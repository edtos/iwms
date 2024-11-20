package com.companyname.iwms.config

import android.util.Log
import com.jcraft.jsch.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.*

class InteractiveSSH(
    private val hostname: String,
    private val username: String,
    private val password: String,
    private val port: Int
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var session: Session
    private lateinit var channel: ChannelShell
    private lateinit var output: BufferedReader
    private lateinit var input: PrintStream

    private val responseChannel = Channel<String>(Channel.BUFFERED)
    private var isReading = true

    companion object {
        private const val TIMEOUT = 1000L
        private const val BUFFER_SIZE = 1024
    }

    suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            Log.d("InteractiveSSH", "Connecting to $hostname...")
            val jsch = JSch()
            session = jsch.getSession(username, hostname, port).apply {
                setPassword(password)
                setConfig("StrictHostKeyChecking", "no")
            }

            session.connect(TIMEOUT.toInt())
            Log.d("InteractiveSSH", "Connected to SSH server")

            channel = session.openChannel("shell") as ChannelShell
            output = BufferedReader(InputStreamReader(channel.inputStream))
            input = PrintStream(channel.outputStream, true)

            channel.connect(TIMEOUT.toInt())
            Log.d("InteractiveSSH", "Channel connected, ready to execute commands")

            startResponseReader()

        } catch (e: Exception) {
            Log.e("InteractiveSSH", "SSH connection error: ${e.message}")
            disconnect()
            throw e
        }
    }

    suspend fun executeCommand(command: String) = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            throw IOException("SSH connection is not established")
        }

        Log.d("InteractiveSSH", "Executing command: $command")
        input.println(command)
        input.flush()

        // Collecting responses asynchronously from the flow
        try {
            withTimeout(TIMEOUT) {
                getResponseFlow().collect { response ->
                    Log.d("InteractiveSSH", "Received response: $response")
                    responseChannel.send(response)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("InteractiveSSH", "Command response timeout exceeded: ${e.message}")
        } catch (e: Exception) {
            Log.e("InteractiveSSH", "Error while executing command: ${e.message}")
        }
    }
    fun getResponseFlow(): Flow<String> = flow {
        try {
            while (isReading && isConnected()) {
                val response = responseChannel.receive()

                if (response.contains("Another active session", ignoreCase = true) ||
                    response.contains("Invalid Login", ignoreCase = true) ||
                    response.contains("1)")) {

                    Log.d("InteractiveSSH", "Emitting relevant response: $response")
                    emit(response)
                }
            }
        } catch (e: Exception) {
            Log.e("InteractiveSSH", "Error in response flow: ${e.message}")
        }
    }
    private fun startResponseReader() {
        scope.launch {
            try {
                val buffer = CharArray(BUFFER_SIZE)
                val responseBuilder = StringBuilder()

                while (isActive && isReading && isConnected()) {
                    if (output.ready()) {
                        val length = output.read(buffer)
                        if (length > 0) {
                            val response = String(buffer, 0, length)
                            responseChannel.send(response)  // Send response to channel
                            Log.d("InteractiveSSH", "Response read: $response")
                        }
                    }
                }

                if (responseBuilder.isNotEmpty()) {
                    Log.d("InteractiveSSH", "Final aggregated response: ${responseBuilder.toString()}")
                }
            } catch (e: Exception) {
                Log.e("InteractiveSSH", "Error reading response: ${e.message}")
            } finally {
                responseChannel.close()  // Close the response channel after processing
            }
        }
    }

    fun isConnected(): Boolean = ::session.isInitialized && session.isConnected && ::channel.isInitialized && channel.isConnected

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        isReading = false
        scope.cancel() // Cancel all coroutines

        if (::channel.isInitialized) {
            channel.disconnect()
        }
        if (:: session.isInitialized) {
            session.disconnect()
        }
        Log.d("InteractiveSSH", "Disconnected from SSH server")
    }

}