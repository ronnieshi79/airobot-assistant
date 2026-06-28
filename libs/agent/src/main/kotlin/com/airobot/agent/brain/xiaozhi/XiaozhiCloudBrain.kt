package com.airobot.agent.brain.xiaozhi

import android.util.Log
import com.airobot.agent.brain.AiBrain
import com.airobot.agent.brain.BrainState
import com.airobot.agent.skills.SkillManager
import com.airobot.agent.skills.SkillResult
import com.airobot.core.comm.NetCommEvent
import com.airobot.core.comm.NetCommService
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Xiaozhi cloud AI proxy — acts as the AiBrain implementation for the
 * Xiaozhi cloud agent. Handles MCP JSON-RPC protocol, dispatches tool calls
 * to the local SkillManager, and drives BrainState from cloud events.
 */
@Singleton
class XiaozhiCloudBrain @Inject constructor(
    private val netCommService: NetCommService,
    private val skillManager: SkillManager,
    private val gson: Gson
) : AiBrain {

    companion object {
        private const val TAG = "XiaozhiCloudBrain"
        private const val MCP_PROTOCOL_VERSION = "2024-11-05"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _brainState = MutableStateFlow(BrainState.IDLE)
    override val brainState: StateFlow<BrainState> = _brainState.asStateFlow()

    init {
        scope.launch {
            netCommService.events.collect { event ->
                when (event) {
                    is NetCommEvent.TextMessage -> {
                        scope.launch { handleMessage(event.json) }
                    }

                    else -> { /* ignored */
                    }
                }
            }
        }
        Log.d(TAG, "XiaozhiCloudBrain initialized, listening for events")
    }

    /**
     * Parse incoming JSON text and route to the appropriate handler.
     */
    private fun handleMessage(jsonStr: String) {
        try {
            val json = gson.fromJson(jsonStr, JsonObject::class.java)
            val type = json.get("type")?.asString ?: return
            val sessionId = json.get("session_id")?.asString

            when (type) {
                "mcp" -> {
                    val payload = json.getAsJsonObject("payload") ?: return
                    handleMcpPayload(payload, sessionId)
                }

                "tts" -> {
                    val state = json.get("state")?.asString
                    when (state) {
                        "start" -> _brainState.value = BrainState.SPEAKING
                        "stop" -> _brainState.value = BrainState.IDLE
                    }
                }

                "stt" -> _brainState.value = BrainState.THINKING
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: ${jsonStr.take(200)}", e)
        }
    }

    /**
     * Handle MCP JSON-RPC 2.0 requests from the cloud agent.
     */
    private fun handleMcpPayload(payload: JsonObject, sessionId: String?) {
        val method = payload.get("method")?.asString ?: return
        // Preserve original id as JsonElement to support string/number/null
        val id = payload.get("id") ?: return

        Log.d(TAG, "MCP request: method=$method, id=$id, sessionId=$sessionId")

        when (method) {
            "initialize" -> handleInitialize(id, sessionId)
            "tools/list" -> handleToolsList(id, sessionId)
            "tools/call" -> handleToolsCall(id, payload.getAsJsonObject("params"), sessionId)
            else -> {
                Log.w(TAG, "Unknown MCP method: $method")
                sendMcpError(id, -32601, "Method not found: $method", sessionId)
            }
        }
    }

    /**
     * Respond to MCP initialize handshake with protocol version and capabilities.
     */
    private fun handleInitialize(id: JsonElement, sessionId: String?) {
        val result = JsonObject().apply {
            addProperty("protocolVersion", MCP_PROTOCOL_VERSION)
            add("capabilities", JsonObject().apply {
                add("tools", JsonObject())
            })
            add("serverInfo", JsonObject().apply {
                addProperty("name", "AiRobotClock")
                addProperty("version", "1.0.0")
            })
        }
        sendMcpResult(id, result, sessionId)
        Log.d(TAG, "MCP initialize handshake completed")
    }

    /**
     * Respond with all registered tool schemas.
     */
    private fun handleToolsList(id: JsonElement, sessionId: String?) {
        val schemas = skillManager.getAllSchemas()
        val result = JsonObject().apply {
            add("tools", schemas)
        }
        sendMcpResult(id, result, sessionId)
        Log.d(TAG, "MCP tools/list: returned ${schemas.size()} tools")
    }

    /**
     * Dispatch a tool call to the SkillManager and send the result back.
     */
    private fun handleToolsCall(id: JsonElement, params: JsonObject?, sessionId: String?) {
        val name = params?.get("name")?.asString
        if (name == null) {
            sendMcpError(id, -32602, "Missing 'name' in params", sessionId)
            return
        }

        val argsObj = params.getAsJsonObject("arguments")
        val argsMap = mutableMapOf<String, Any>()
        argsObj?.entrySet()?.forEach { entry ->
            val value = entry.value
            argsMap[entry.key] = when {
                value.isJsonPrimitive -> {
                    val prim = value.asJsonPrimitive
                    when {
                        prim.isNumber -> prim.asNumber
                        prim.isBoolean -> prim.asBoolean
                        else -> prim.asString
                    }
                }

                else -> value.toString()
            }
        }

        Log.d(TAG, "MCP tools/call: name=$name, args=$argsMap")

        scope.launch {
            val skillResult = skillManager.dispatchToolCall(name, argsMap)
            val isError = skillResult is SkillResult.Failure
            val text = when (skillResult) {
                is SkillResult.Success -> skillResult.message
                is SkillResult.Failure -> skillResult.reason
            }

            val result = JsonObject().apply {
                add(
                    "content", gson.toJsonTree(
                        listOf(
                            mapOf("type" to "text", "text" to text)
                        )
                    )
                )
                addProperty("isError", isError)
            }
            sendMcpResult(id, result, sessionId)
            Log.d(TAG, "MCP tools/call result: name=$name, isError=$isError, text=$text")
        }
    }

    /**
     * Send a successful MCP JSON-RPC response via raw WebSocket text.
     */
    private fun sendMcpResult(id: JsonElement, result: JsonObject, sessionId: String?) {
        val response = JsonObject().apply {
            sessionId?.let { addProperty("session_id", it) }
            addProperty("type", "mcp")
            add("payload", JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                add("id", id)
                add("result", result)
            })
        }
        netCommService.sendRawText(gson.toJson(response))
    }

    /**
     * Send an MCP JSON-RPC error response.
     */
    private fun sendMcpError(id: JsonElement, code: Int, message: String, sessionId: String?) {
        val response = JsonObject().apply {
            sessionId?.let { addProperty("session_id", it) }
            addProperty("type", "mcp")
            add("payload", JsonObject().apply {
                addProperty("jsonrpc", "2.0")
                add("id", id)
                add("error", JsonObject().apply {
                    addProperty("code", code)
                    addProperty("message", message)
                })
            })
        }
        netCommService.sendRawText(gson.toJson(response))
        Log.w(TAG, "MCP error response: code=$code, message=$message")
    }

    override fun wakeUp() {
        _brainState.value = BrainState.LISTENING
        netCommService.startListening("auto")
        Log.d(TAG, "wakeUp: switched to LISTENING")
    }

    override fun sleep() {
        _brainState.value = BrainState.IDLE
        netCommService.stopListening()
        Log.d(TAG, "sleep: switched to IDLE")
    }
}
