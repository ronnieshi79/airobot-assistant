package com.airobot.core.system

import com.airobot.core.system.model.AiRobot
import com.airobot.core.system.model.SystemInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SystemInfoTest {

    @Test
    fun testAiRobotDefaults() {
        val robot = AiRobot()
        assertEquals("小美", robot.roleName)
        assertEquals("ANDROID_CANVAS", robot.characterType)
        assertEquals("", robot.personality)
        assertEquals("火山模型", robot.voiceModel)
        assertEquals("小叶,小宁", robot.wakeWords)
    }

    @Test
    fun testSystemInfoActiveRoleIndex() {
        val sysInfo1 = SystemInfo(activeRoleIndex = 0, clientId = "test-client")
        val sysInfo2 = sysInfo1.copy(activeRoleIndex = 1)
        val sysInfo3 = sysInfo1.copy(activeRoleIndex = 0)

        assertEquals(0, sysInfo1.activeRoleIndex)
        assertEquals(1, sysInfo2.activeRoleIndex)

        // Test equals and hashCode overrides
        assertEquals(sysInfo1, sysInfo3)
        assertNotEquals(sysInfo1, sysInfo2)
        assertEquals(sysInfo1.hashCode(), sysInfo3.hashCode())
        assertNotEquals(sysInfo1.hashCode(), sysInfo2.hashCode())
    }
}
