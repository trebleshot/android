package org.monora.uprotocol.client.android.model

import android.net.MacAddress
import android.net.wifi.ScanResult
import android.net.wifi.WifiNetworkSuggestion
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.core.util.ObjectsCompat
import kotlinx.parcelize.Parcelize

@Parcelize
class NetworkDescription(var ssid: String, var bssid: String?, var password: String?) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (other is NetworkDescription) {
            return bssid == other.bssid || (bssid == null && ssid == other.ssid)
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return ObjectsCompat.hash(ssid, bssid, password)
    }

    @RequiresApi(29)
    fun toNetworkSuggestion(): WifiNetworkSuggestion {
        val builder: WifiNetworkSuggestion.Builder = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setIsAppInteractionRequired(true)
        password?.let { builder.setWpa2Passphrase(it) }
        bssid?.let { builder.setBssid(MacAddress.fromString(it)) }

        return builder.build()
    }
}