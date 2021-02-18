package org.monora.uprotocol.client.android.protocol

import android.os.Build
import org.monora.coolsocket.core.session.ActiveConnection
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.protocol.ConnectionFactory
import java.net.InetAddress

class DefaultConnectionFactory : ConnectionFactory {
    override fun enableCipherSuites(
        supportedCipherSuites: Array<out String>?,
        enabledCipherSuiteList: MutableList<String>?
    ) {
        if (Build.VERSION.SDK_INT >= 20) {
            enabledCipherSuiteList?.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384")
            enabledCipherSuiteList?.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256")
        }
    }

    override fun openConnection(address: InetAddress?): ActiveConnection = CommunicationBridge.openConnection(address)
}