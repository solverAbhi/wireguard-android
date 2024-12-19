// TunnelUtils.kt
package com.abhi.vpn.activity
import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import android.util.Log
import com.abhi.vpn.Application
import com.abhi.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TunnelUtlis {
    private const val TAG = "TunnelUtils"

    private fun getManagedConfig(context: Context): Bundle? {
        try {
            val restrictionsManager = context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
            val restrictions = restrictionsManager.applicationRestrictions
            Log.i(TAG, "RestrictionsManager available: $restrictionsManager")
            Log.i(TAG, "Application Restrictions: $restrictions")
            return restrictions
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing RestrictionsManager", e)
            return null
        }
    }

    fun createManagedTunnel(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val configBundle = getManagedConfig(context)
                val testKey = configBundle?.getString("test_key")
                Log.i(TAG, "Test key: $testKey")

                if (configBundle == null || configBundle.isEmpty) {
                    Log.e(TAG, "No Managed App Configuration available")
                    return@launch
                }

                val privateKey = configBundle.getString("interface_private_key")
                val address = configBundle.getString("interface_address")
                val dns = configBundle.getString("interface_dns")
                val publicKey = configBundle.getString("peer_public_key")
                val presharedKey = configBundle.getString("peer_preshared_key")
                val allowedIPs = configBundle.getString("peer_allowed_ips")
                val endpoint = configBundle.getString("peer_endpoint")

                if (privateKey.isNullOrBlank() || publicKey.isNullOrBlank() || endpoint.isNullOrBlank()) {
                    Log.e(TAG, "Required fields missing in Managed App Configuration")
                    return@launch
                }

                val vpnConfig = """
                    [Interface]
                    PrivateKey = $privateKey
                    Address = $address
                    DNS = $dns
                    [Peer]
                    PublicKey = $publicKey
                    PresharedKey = $presharedKey
                    AllowedIPs = $allowedIPs
                    Endpoint = $endpoint
                """.trimIndent()

                val config = Config.parse(vpnConfig.byteInputStream())
                val tunnelManager = Application.getTunnelManager()
                val tunnel = tunnelManager.create("ManagedVPN", config)
                Log.i(TAG, "Tunnel created: ${tunnel.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create tunnel: ${e.message}", e)
            }
        }
    }
}
