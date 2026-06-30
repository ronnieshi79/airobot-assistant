package com.airobot.assistant.assembly
import com.airobot.airbot.domain.model.AirbotServiceSubState

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import com.airobot.airbot.viewmodel.InteractionType
import com.airobot.framework.layout.BottomFooter
import com.airobot.airbot.domain.model.CharacterType
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.animation.core.Spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.theme.RobotThemeMode
import com.airobot.assistant.ui.comp.BackgroundDecorations
import com.airobot.assistant.settings.AiRobotDialog
import com.airobot.framework.layout.RobotTopBar
import com.airobot.framework.layout.SystemDrawer
import com.airobot.framework.layout.DrawerMenuItemData
import com.airobot.assistant.settings.AiRobotConfig
import com.airobot.assistant.settings.RoleConfig
import com.airobot.assistant.settings.SystemAuth
import com.airobot.assistant.assembly.APP_SUPPORTED_OVERLAYS
import androidx.compose.ui.res.stringResource
import com.airobot.airbot.domain.model.ConversationSubState
import com.airobot.airbot.domain.model.RobotState
import com.airobot.airbot.viewmodel.RobotUiState
import com.airobot.airbot.viewmodel.RobotVisualState
import com.airobot.assistant.viewmodel.MainShellViewModel
import com.airobot.airbot.viewmodel.ConversationViewModel
import com.airobot.framework.theme.StatusAmber
import com.airobot.framework.theme.StatusCyan
import com.airobot.framework.theme.StatusEmerald
import com.airobot.framework.theme.StatusRed
import com.airobot.features.aiserv.viewmodel.OverlayViewModel
import com.airobot.framework.util.LanguageMode

/**
 * 机器人服务主屏幕
 * Web原型对应: App.tsx
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppMainScreen(
    themeMode: RobotThemeMode = RobotThemeMode.DARK,
    languageMode: LanguageMode = LanguageMode.CHINESE,
    onToggleTheme: () -> Unit = {},
    onLanguageChange: (LanguageMode) -> Unit = {},
    mainShellViewModel: MainShellViewModel = hiltViewModel(),
    conversationViewModel: ConversationViewModel = hiltViewModel(),
    overlayViewModel: OverlayViewModel = hiltViewModel()
) {
    // 权限管理
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    )

    // 从 MainShellViewModel 收集一级状态
    val robotState by mainShellViewModel.robotState.collectAsState()
    val errorMessage by mainShellViewModel.errorMessage.collectAsState()
    val showActivationDialog by mainShellViewModel.showActivationDialog.collectAsState()
    val activationCode by mainShellViewModel.activationCode.collectAsState()
    val mainVoiceLevel by mainShellViewModel.voiceLevel.collectAsState()
    val systemInfo by mainShellViewModel.systemInfo.collectAsState()
    val allCharacters by mainShellViewModel.allCharacters.collectAsState()
    val activeCharacter by mainShellViewModel.activeCharacter.collectAsState()
    val characterType = CharacterType.fromString(activeCharacter?.characterType ?: "ANDROID_CANVAS")

    // 从 ConversationViewModel 收集交互状态
    val convAudioLevel by conversationViewModel.audioLevel.collectAsState()
    val currentRoundUserText by conversationViewModel.currentRoundUserText.collectAsState()
    val currentRoundAiText by conversationViewModel.currentRoundAiText.collectAsState()

    // 组合音量等级：对话时用对话VM的，非对话时用主VM的
    val audioLevel = if (robotState is RobotState.Conversation) convAudioLevel else mainVoiceLevel

    // 从 OverlayViewModel 收集弹窗状态
    val activeOverlay by overlayViewModel.activeOverlay.collectAsState()

    // 本地UI状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 机器人UI状态
    var robotUiState by remember { mutableStateOf(RobotUiState()) }
    var currentCardIndex by remember { mutableIntStateOf(0) }

    // 服务卡片定义 (由 Hilt Singleton 注入的 RecommendationEngine 计算出的动态列表)
    val serviceCards by overlayViewModel.getRecommendedCards(APP_SUPPORTED_OVERLAYS).collectAsState(initial = emptyList())
    val currentCard = serviceCards.getOrNull(currentCardIndex) ?: serviceCards.firstOrNull()
    val currentStatusTip = currentCard?.let { stringResource(id = it.statusTipResId) } ?: ""

    // 自动轮播逻辑
    LaunchedEffect(serviceCards.size, robotUiState.isInteracting) {
        if (serviceCards.isNotEmpty() && !robotUiState.isInteracting) {
            while (true) {
                delay(10000L)
                currentCardIndex = (currentCardIndex + 1) % serviceCards.size
            }
        }
    }

    // 机器人UI状态汇总同步 - 整合多个来源，避免竞态覆盖
    LaunchedEffect(
        robotState,
        currentRoundUserText,
        currentRoundAiText,
        activeOverlay,
        currentCard // currentCard 随 currentCardIndex 变化
    ) {
        val visualState = when (val s = robotState) {
            is RobotState.Offline -> RobotVisualState.SLEEPING
            is RobotState.Initializing -> RobotVisualState.THINKING
            is RobotState.Connecting -> RobotVisualState.THINKING
            is RobotState.Unauthorized -> RobotVisualState.IDLE
            is RobotState.Ready -> RobotVisualState.IDLE
            is RobotState.Conversation -> when (s.subState) {
                ConversationSubState.LISTENING -> RobotVisualState.LISTENING
                ConversationSubState.THINKING -> RobotVisualState.THINKING
                ConversationSubState.SPEAKING -> RobotVisualState.SPEAKING
            }
            is RobotState.FunctionService -> RobotVisualState.IDLE // Or keep original logic if you have access to substate
        }

        robotUiState = robotUiState.copy(
            visualState = visualState,
            isConnected = robotState !is RobotState.Offline,
            currentUserMsg = currentRoundUserText,
            currentAiMsg = currentRoundAiText,
            interactionType = if (activeOverlay.isNotEmpty()) InteractionType.CARD else InteractionType.CHAT,
            statusTip = currentStatusTip
        )
    }

    // 请求权限
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // 初始化音频系统 (当权限获得后)
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            mainShellViewModel.initAudioService()
        }
    }

    // 监听唤醒事件
    LaunchedEffect(Unit) {
        mainShellViewModel.wakeupEvent.collect {
            conversationViewModel.startConversation()
        }
    }

    // 机器人水平位移移动动画
    // 当 isCardMode 为 true (点击卡片展开) 时，机器人滑向左侧 (bias 0.04f)
    // 否则保持在中间 (bias 0.5f)
    val robotHorizontalBias by animateFloatAsState(
        targetValue = if (robotUiState.isCardMode) 0.04f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "robotSlide"
    )

    val drawerMenuItems = listOf(
        DrawerMenuItemData(Icons.Default.Lock, "系统认证", "系统认证信息") { SystemAuth() },
        DrawerMenuItemData(Icons.Default.Person, "角色管理", "角色管理") {
            RoleConfig(
                characters = allCharacters,
                activeCharacter = activeCharacter,
                onRoleSelected = { roleName -> mainShellViewModel.updateActiveRole(roleName) }
            )
        },
        DrawerMenuItemData(Icons.Default.Settings, "Ai智能体", "Ai智能体配置") { AiRobotConfig() }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SystemDrawer(
                menuItems = drawerMenuItems,
                languageMode = languageMode,
                onClose = { scope.launch { drawerState.close() } },
                onToggleTheme = onToggleTheme,
                onLanguageChange = onLanguageChange
            )
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            RobotTheme.colors.backgroundGradientStart,
                            RobotTheme.colors.backgroundGradientEnd
                        )
                    )
                )
        ) {
            BackgroundDecorations()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    // 为了防止状态栏遮挡，顶部留出一定空间
            ) {
                val stateText = when (robotState) {
                    is RobotState.Offline -> "OFFLINE"
                    is RobotState.Initializing -> "INITIALIZING"
                    is RobotState.Unauthorized -> "UNAUTHORIZED"
                    is RobotState.Connecting -> "CONNECTING"
                    is RobotState.Ready -> "READY"
                    is RobotState.Conversation -> "CONVERSATION"
                    is RobotState.FunctionService -> "SERVICE MODE"
                }

                val stateColor = when (robotState) {
                    is RobotState.Offline -> StatusRed
                    is RobotState.Initializing -> StatusAmber
                    is RobotState.Unauthorized -> StatusRed
                    is RobotState.Connecting -> StatusCyan
                    is RobotState.Ready -> StatusCyan
                    else -> StatusEmerald
                }

                RobotTopBar(
                    stateText = stateText,
                    stateColor = stateColor,
                    errorMessage = errorMessage,
                    onLogoClick = { scope.launch { drawerState.open() } },
                    roleName = activeCharacter?.roleName ?: "AETHER"
                )

                // ErrorBanner 迁移到 TopBar 中，此处移除

                // 中心内容区域 - 使用 ConstraintLayout 精确控制相对位置
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    val (airobotRef, featureScreensRef) = createRefs()

                    // 1. Airobot 组件组合
                    AirobotScreen(
                        robotHorizontalBias = robotHorizontalBias,
                        robotVisualState = robotUiState.visualState,
                        characterType = characterType,
                        roleName = activeCharacter?.roleName ?: "AETHER",
                        audioLevel = audioLevel,
                        isConnected = robotUiState.isConnected,
                        isTimerActive = activeOverlay == "overlay_timer",
                        isTimerPaused = false, // Timer pause state is managed inside timer overlay now
                        currentRoundAiText = currentRoundAiText,
                        onStartListening = {
                            if (permissionsState.allPermissionsGranted) {
                                robotUiState = robotUiState.copy(
                                    interactionType = InteractionType.CHAT,
                                    currentUserMsg = null,
                                    currentAiMsg = null
                                )
                                conversationViewModel.startConversation()
                            }
                        },
                        onStopListening = { conversationViewModel.stopAutoConversation() },
                        onInterruptSpeak = { conversationViewModel.interrupt() },
                        onTimerControl = { /* Timer control moved to overlay */ },
                        onCommandClick = { command ->
                            if (permissionsState.allPermissionsGranted) {
                                robotUiState = robotUiState.copy(
                                    interactionType = InteractionType.CHAT,
                                    currentUserMsg = command,
                                    currentAiMsg = null
                                )
                                conversationViewModel.startConversation()
                            }
                        },
                        onBubbleClose = {
                            robotUiState = robotUiState.copy(
                                visualState = RobotVisualState.IDLE,
                                currentUserMsg = null,
                                currentAiMsg = null
                            )
                            conversationViewModel.interrupt()
                        },
                        modifier = Modifier.constrainAs(airobotRef) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        }
                    )

                    // 2. 右侧卡片区域
                    Box(
                        modifier = Modifier
                            .constrainAs(featureScreensRef) {
                                if (robotUiState.isCardMode) {
                                    end.linkTo(parent.end, margin = 48.dp)
                                    width = Dimension.value(600.dp)
                                } else {
                                    end.linkTo(parent.end, margin = 64.dp)
                                    width = Dimension.value(260.dp)
                                }
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                height = Dimension.fillToConstraints
                            },
                        contentAlignment = if (robotUiState.isCardMode) Alignment.Center else Alignment.CenterEnd
                    ) {
                        FeatureScreens(
                            isCardMode = robotUiState.isCardMode,
                            serviceCards = serviceCards,
                            currentCardIndex = currentCardIndex,
                            onPageChanged = { currentCardIndex = it },
                            statusTip = robotUiState.statusTip,
                            activeOverlay = activeOverlay,
                            onCardClick = { card ->
                                val targetCard = if (serviceCards.contains(card)) card else serviceCards.getOrNull(currentCardIndex) ?: card
                                robotUiState = robotUiState.copy(
                                    interactionType = InteractionType.CARD,
                                    visualState = RobotVisualState.LISTENING,
                                    currentUserMsg = null,
                                    currentAiMsg = null
                                )
                                overlayViewModel.showOverlay(targetCard.overlayTag)
                                if (permissionsState.allPermissionsGranted) {
                                    conversationViewModel.startConversation()
                                }
                            },
                            onCloseOverlay = {
                                robotUiState = robotUiState.copy(
                                    visualState = RobotVisualState.IDLE
                                )
                                overlayViewModel.hideOverlay()
                                conversationViewModel.interrupt()
                            },
                            onWakeupAirobot = {
                                conversationViewModel.startConversation()
                            }
                        )
                    }
                }
            }

            // 底部页脚
            BottomFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )

            // 激活弹窗
            if (showActivationDialog && activationCode != null) {
                AiRobotDialog(
                    activationCode = activationCode!!,
                    onConfirm = { mainShellViewModel.onActivationConfirmed() },
                    onDismiss = { /* Optionally handle dismissal, but usually activation is required */ }
                )
            }
        }
    }
}
