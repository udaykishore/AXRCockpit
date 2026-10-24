package com.ukp.axrcockpit

import com.ukp.axrcockpit.util.ShellUtil
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellUtilTest {
    @Test
    fun testRunCommandEcho() {
        val output = ShellUtil.runCommand("echo 'Hello World'")
        assertTrue(output.contains("Hello World"))
    }

    @Test
    fun testRunCommandInvalid() {
        val output = ShellUtil.runCommand("invalid_command_that_does_not_exist")
        assertTrue(output.contains("Error:"))
    }
}
