package com.companyname.iwms.config

import com.companyname.iwms.model.Environment

class SSHConnectionManager {
    var sshConnection: InteractiveSSH? = null

    suspend fun connect(environment: Environment) {
        sshConnection?.disconnect()
        sshConnection = InteractiveSSH(
            hostname = environment.host,
            username = environment.username,
            password = environment.password,
            port = environment.port.toInt()
        )
        sshConnection?.connect()
    }

    suspend fun disconnect() {
        sshConnection?.disconnect()
        sshConnection = null
    }
}
