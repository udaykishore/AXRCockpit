package com.ukp.axrcockpit.util

import java.io.BufferedReader
import java.io.InputStreamReader

object ShellUtil {
    fun runCommand(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            readProcessOutput(process)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun runSuCommand(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            readProcessOutput(process)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun readProcessOutput(process: Process): String {
        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
        }
        BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append("Error: ").append(line).append("\n")
            }
        }
        process.waitFor()
        return output.toString().trim()
    }
}
