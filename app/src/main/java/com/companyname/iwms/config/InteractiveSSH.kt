package com.companyname.iwms.config

// Add required imports
import com.jcraft.jsch.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.*


class InteractiveSSH(
    private val hostname: String = "",
    private val username: String = "",
    private val password: String = "",
    private val port: Int = 0
) {

    // Define scope at class level
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var session: Session
    private lateinit var channel: ChannelShell
    private lateinit var output: BufferedReader
    private lateinit var input: PrintStream

    private val responseChannel = Channel<String>(Channel.BUFFERED)
    private var isReading = true

    companion object {
        private const val TIMEOUT = 5000L
        private const val BUFFER_SIZE = 1024
    }

    suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            println(port)
            println(username)
            println(password)

            println("Connecting to $hostname...")

            val jsch = JSch()
            session = jsch.getSession(username, hostname, port)

            // Configure session
            session.setPassword(password)
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)

            session.connect(TIMEOUT.toInt())
            //println("Session connected!")

            // Initialize shell channel
            channel = session.openChannel("shell") as ChannelShell

            // Setup I/O streams
            output = BufferedReader(InputStreamReader(channel.inputStream))
            input = PrintStream(channel.outputStream, true)

            println("Establishing shell channel...")
            channel.connect(TIMEOUT.toInt())
            println("Shell channel established!")

            // Start response reader
            startResponseReader()

            // Wait for initial response
            delay(1000)
            println("Initial connection response:")

        } catch (e: Exception) {
            println("Connection error: ${e.message}")
            disconnect()
            throw e
        }
    }

    private fun startResponseReader() {
        scope.launch {
            try {
                val buffer = CharArray(BUFFER_SIZE)
                while (isActive && isReading && isConnected()) {
                    if (output.ready()) {
                        val length = output.read(buffer)
                        if (length > 0) {
                            val response = String(buffer, 0, length)
                            responseChannel.send(response)
                            print(response)
                        }
                    }
                    delay(50)
                }
            } catch (e: CancellationException) {
                println("Response reader cancelled")
            } catch (e: Exception) {
                println("Error reading response: ${e.message}")
            } finally {
                responseChannel.close()
            }
        }
    }

    suspend fun executeCommand(command: String) = withContext(Dispatchers.IO) {
        if (!isConnected()) {
            throw IOException("SSH connection is not established")
        }

        println("\nExecuting command: $command")
        input.println(command)
        input.flush()

        // Wait for command to complete
        delay(5000)
    }

    fun getResponseFlow(): Flow<String> = flow {
        for (response in responseChannel) {
            val menuList = parseMenu(response)
            menuList.forEachIndexed { index, item ->
                println("MY DATA${index + 1}. $item")  // Display the menu in a numbered format
            }
            emit(response)
        }
    }

    fun isConnected(): Boolean {
        return ::session.isInitialized &&
                session.isConnected &&
                ::channel.isInitialized &&
                channel.isConnected
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        isReading = false
        scope.cancel() // Cancel all coroutines

        if (::channel.isInitialized) {
            channel.disconnect()
        }
        if (::session.isInitialized) {
            session.disconnect()
        }
        println("Disconnected from SSH server")
    }
}
fun removeEscapeSequences(input: String): String {
    // Regex to match escape sequences like `[5;31H` and other control characters
    val regex = Regex("""[\x1B\x08]\[[;?0-9]*[a-zA-Z]""")
    // Remove escape sequences and control characters
    return regex.replace(input, "").replace("\u0008", "")
}

fun parseMenu(input: String): List<String> {
    val cleanedInput = removeEscapeSequences(input)
    val regex = Regex("""\d+\)\s*(.*)""") // Pattern to match "1) Item 1", "2) Item 2", etc.
    val items = mutableListOf<String>()

    // Split by newlines and parse each line for the menu item
    cleanedInput.split("\n").forEach { line ->
        val match = regex.find(line.trim()) // Find the pattern
        match?.groups?.get(1)?.value?.let { item ->
            items.add(item)
        }
    }

    Config.menu1=items.toMutableSet()
    return items
}

// Main function demonstrating usage
fun main() = runBlocking {

    val ssh = InteractiveSSH()

    try {
        // Connect to SSH server
        ssh.connect()

        // Launch response collector
        val responseJob = launch {
            ssh.getResponseFlow().collect { response ->
                print(response)
                val menuList = parseMenu(response)
                menuList.forEachIndexed { index, item ->
                    println("MY DATA${index + 1}. $item")  // Display the menu in a numbered format
                }
            }
        }

        // Interactive command loop
        withContext(Dispatchers.IO) {
            val scanner = Scanner(System.`in`)
            while (true) {
                print("\nEnter command (or 'exit' to quit): ")
                val command = scanner.nextLine()

                if (command.equals("exit", ignoreCase = true)) {
                    break
                }

                try {
                    ssh.executeCommand(command)
                    delay(8000)
                } catch (e: Exception) {
                    println("Error executing command: ${e.message}")
                }
            }
        }

        responseJob.cancelAndJoin()

    } catch (e: Exception) {
        println("Error: ${e.message}")
    } finally {
        ssh.disconnect()
    }
}

