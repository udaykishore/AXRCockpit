package com.ukp.axrcockpit

import android.content.Context
import android.hardware.usb.*
import com.ukp.axrcockpit.util.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder

object UsbAdbManager {
    private val logger = Logger.get(UsbAdbManager::class.java)

    fun sendAdbTcpIpCommand(context: Context): Map<String, String> {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = manager.deviceList
        var deviceIp: String? = null
        val phoneIp = getPhoneIp() ?: "Unknown"
        var rawOutput = "No data"
        var statusMessage = "No ADB device found on cable."
        
        val publicKey = try {
            (AdbConnectionManager.getInstance(context) as AdbConnectionManager).publicKeyBase64
        } catch (e: Exception) {
            "Error reading local key: ${e.message}"
        }

        for (device in deviceList.values) {
            for (i in 0 until device.interfaceCount) {
                val usbIface = device.getInterface(i)
                if (usbIface.interfaceClass == 255 &&
                    usbIface.interfaceSubclass == 66 &&
                    usbIface.interfaceProtocol == 1) {
                    
                    if (!manager.hasPermission(device)) {
                        return mapOf(
                            "phoneIp" to phoneIp,
                            "deviceIp" to "None",
                            "rawOutput" to "Permission denied for USB device.",
                            "message" to "Permission required.",
                            "publicKey" to publicKey
                        )
                    }

                    val connection = manager.openDevice(device) ?: return mapOf(
                        "phoneIp" to phoneIp,
                        "deviceIp" to "None",
                        "rawOutput" to "Failed to open USB device.",
                        "message" to "Device open failure.",
                        "publicKey" to publicKey
                    )
                    connection.claimInterface(usbIface, true)

                    var inEndpoint: UsbEndpoint? = null
                    var outEndpoint: UsbEndpoint? = null

                    for (j in 0 until usbIface.endpointCount) {
                        val ep = usbIface.getEndpoint(j)
                        if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (ep.direction == UsbConstants.USB_DIR_IN) {
                                inEndpoint = ep
                            } else {
                                outEndpoint = ep
                            }
                        }
                    }

                    if (inEndpoint != null && outEndpoint != null) {
                        statusMessage = "Endpoints found. Handshake sent."
                        
                        // Step 1: Send CNXN
                        val cnxnData = "host::\u0000".toByteArray()
                        val cnxnHeader = createAdbHeader(AdbHeader.CMD_CNXN, 0x01000000, 4096, cnxnData)
                        
                        connection.bulkTransfer(outEndpoint, cnxnHeader, cnxnHeader.size, 1000)
                        connection.bulkTransfer(outEndpoint, cnxnData, cnxnData.size, 1000)

                        // Read response
                        val buffer = ByteArray(inEndpoint.maxPacketSize)
                        connection.bulkTransfer(inEndpoint, buffer, buffer.size, 1000)

                        // Step 2: Send OPEN for shell command to get IP
                        val command = "shell:ip addr show\u0000".toByteArray()
                        val openHeader = createAdbHeader(AdbHeader.CMD_OPEN, 1, 0, command)
                        
                        connection.bulkTransfer(outEndpoint, openHeader, openHeader.size, 1000)
                        connection.bulkTransfer(outEndpoint, command, command.size, 1000)

                        // Read OKAY response for OPEN (24 bytes)
                        connection.bulkTransfer(inEndpoint, buffer, buffer.size, 1000)
                        
                        // Read WRTE response containing data
                        val dataBuffer = ByteArray(4096)
                        val bytesRead = connection.bulkTransfer(inEndpoint, dataBuffer, dataBuffer.size, 2000)
                        
                        if (bytesRead > 24) {
                            val dataLengthBytes = ByteBuffer.wrap(dataBuffer, 12, 4).order(ByteOrder.LITTLE_ENDIAN).int
                            val output = String(dataBuffer, 24, minOf(dataLengthBytes, bytesRead - 24))
                            rawOutput = output
                            
                            val isIpv6 = phoneIp.contains(":")
                            val subnetPrefix = if (isIpv6) {
                                phoneIp.substringBeforeLast(":") + ":"
                            } else {
                                phoneIp.substringBeforeLast(".") + "."
                            }
                            
                            val ipMatchesV4 = Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").findAll(output)
                            val ipMatchesV6 = Regex("([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|::([0-9a-fA-F]{1,4}:){0,7}[0-9a-fA-F]{1,4}").findAll(output)
                            
                            var fallbackIpv4: String? = null
                            var fallbackIpv6: String? = null
                            
                            for (match in ipMatchesV6) {
                                val ip = match.value
                                if (ip != "::1" && !ip.startsWith("fe80")) {
                                    if (ip.startsWith(subnetPrefix)) {
                                        deviceIp = ip
                                        break
                                    }
                                    if (fallbackIpv6 == null) {
                                        fallbackIpv6 = ip
                                    }
                                }
                            }
                            
                            if (deviceIp == null) {
                                for (match in ipMatchesV4) {
                                    val ip = match.value
                                    if (ip != "127.0.0.1" && !ip.startsWith("0.")) {
                                        if (ip.startsWith(subnetPrefix)) {
                                            deviceIp = ip
                                            break
                                        }
                                        if (fallbackIpv4 == null) {
                                            fallbackIpv4 = ip
                                        }
                                    }
                                }
                            }
                            
                            if (deviceIp == null) {
                                deviceIp = fallbackIpv6 ?: fallbackIpv4
                            }
                        }

                        // Step 3: Send OPEN tcpip:45555
                        val tcpipCmd = "tcpip:45555\u0000".toByteArray()
                        val tcpipHeader = createAdbHeader(AdbHeader.CMD_OPEN, 2, 0, tcpipCmd)
                        
                        connection.bulkTransfer(outEndpoint, tcpipHeader, tcpipHeader.size, 1000)
                        connection.bulkTransfer(outEndpoint, tcpipCmd, tcpipCmd.size, 1000)
                        
                        connection.releaseInterface(usbIface)
                        connection.close()
                        
                        statusMessage = "Completed. tcpip:45555 command sent."
                        
                        return mapOf(
                            "phoneIp" to phoneIp,
                            "deviceIp" to (deviceIp ?: "None"),
                            "rawOutput" to rawOutput,
                            "message" to statusMessage,
                            "publicKey" to publicKey
                        )
                    }
                }
            }
        }
        
        return mapOf(
            "phoneIp" to phoneIp,
            "deviceIp" to "None",
            "rawOutput" to "No ADB device found on hardware layers.",
            "message" to statusMessage,
            "publicKey" to publicKey
        )
    }

    private fun getPhoneIp(): String? {
        try {
            var ipv4: String? = null
            var ipv6: String? = null
            val en = java.net.NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (inetAddress.isLoopbackAddress) continue
                    if (inetAddress is java.net.Inet6Address) {
                        ipv6 = inetAddress.hostAddress
                    } else if (inetAddress is java.net.Inet4Address) {
                        ipv4 = inetAddress.hostAddress
                    }
                }
            }
            return ipv6 ?: ipv4
        } catch (ex: Exception) {
            logger.w("Error getting phone IP", ex)
        }
        return null
    }

    private fun createAdbHeader(command: Int, arg0: Int, arg1: Int, data: ByteArray): ByteArray {
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(command)
        header.putInt(arg0)
        header.putInt(arg1)
        header.putInt(data.size)
        header.putInt(calculateChecksum(data))
        header.putInt(command.inv()) // Magic
        return header.array()
    }

    private fun calculateChecksum(data: ByteArray): Int {
        var sum = 0L
        for (b in data) {
            sum += (b.toInt() and 0xFF)
        }
        return sum.toInt()
    }

    object AdbHeader {
        const val CMD_CNXN = 0x4e584e43 // "CNXN"
        const val CMD_OPEN = 0x4e45504f // "OPEN"
    }
}
