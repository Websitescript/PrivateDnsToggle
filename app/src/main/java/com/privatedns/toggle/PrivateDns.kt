package com.privatedns.toggle

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.provider.Settings
import android.widget.Toast

enum class DnsMode { OFF, AUTOMATIC, HOSTNAME, UNKNOWN }

data class DnsState(val mode: DnsMode, val hostname: String?, val canWrite: Boolean) {
    val isOn get() = mode == DnsMode.AUTOMATIC || mode == DnsMode.HOSTNAME
    val statusLabel get() = when (mode) {
        DnsMode.OFF -> "OFF"
        DnsMode.UNKNOWN -> "?"
        else -> "ON"
    }
    val modeLabel get() = when (mode) {
        DnsMode.OFF -> "Off"
        DnsMode.AUTOMATIC -> "Automatic"
        DnsMode.HOSTNAME -> "Private DNS provider hostname"
        DnsMode.UNKNOWN -> "Unknown (set by your ROM)"
    }
    val providerLabel get() = when (mode) {
        DnsMode.HOSTNAME -> hostname?.takeIf { it.isNotBlank() } ?: "Not set"
        DnsMode.AUTOMATIC -> "Automatic"
        DnsMode.OFF -> "Off"
        DnsMode.UNKNOWN -> "Unavailable"
    }
}

object PrivateDns {
    private const val KEY_MODE = "private_dns_mode"
    private const val KEY_SPEC = "private_dns_specifier"
    private const val PREFS = "dns_prefs"
    private const val LAST_ON = "last_on_mode"

    /** Reading these Global settings needs no permission. */
    fun read(ctx: Context): DnsState {
        val cr = ctx.contentResolver
        val raw = try { Settings.Global.getString(cr, KEY_MODE) } catch (_: Exception) { "?" }
        val spec = try { Settings.Global.getString(cr, KEY_SPEC) } catch (_: Exception) { null }
        val mode = when (raw) {
            null, "opportunistic" -> DnsMode.AUTOMATIC // AOSP default when unset
            "off" -> DnsMode.OFF
            "hostname" -> DnsMode.HOSTNAME
            else -> DnsMode.UNKNOWN
        }
        return DnsState(mode, spec, canWrite(ctx))
    }

    fun canWrite(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /**
     * Changes the real system setting, then re-reads it. Returns true only if the system
     * now reports the requested state. Never claims success otherwise.
     */
    fun setEnabled(ctx: Context, enable: Boolean): Boolean {
        if (!canWrite(ctx)) return false
        val cr = ctx.contentResolver
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        try {
            val cur = read(ctx)
            val target = if (!enable) {
                if (cur.isOn) prefs.edit().putString(LAST_ON, if (cur.mode == DnsMode.HOSTNAME) "hostname" else "opportunistic").apply()
                "off"
            } else {
                val spec = Settings.Global.getString(cr, KEY_SPEC)
                val last = prefs.getString(LAST_ON, null)
                if (!spec.isNullOrBlank() && last != "opportunistic") "hostname" else "opportunistic"
            }
            Settings.Global.putString(cr, KEY_MODE, target)
        } catch (_: SecurityException) {
            return false
        } catch (_: Exception) {
            return false
        }
        return read(ctx).isOn == enable
    }

    fun register(ctx: Context, observer: ContentObserver) {
        listOf(KEY_MODE, KEY_SPEC).forEach {
            ctx.contentResolver.registerContentObserver(Settings.Global.getUriFor(it), false, observer)
        }
    }

    fun unregister(ctx: Context, observer: ContentObserver) {
        ctx.contentResolver.unregisterContentObserver(observer)
    }

    /** Candidates in priority order. The first one this ROM accepts wins. */
    private fun settingsIntents(): List<Intent> = listOf(
        // AOSP / Pixel and ROMs that keep the stock activity name
        Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$PrivateDnsSettingsActivity")),
        // Network/connection settings page (ColorOS: Connection & sharing -> Private DNS)
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )

    fun openSettings(ctx: Context): Boolean {
        settingsIntents().forEachIndexed { index, intent ->
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                if (index > 0) {
                    Toast.makeText(ctx, "Opened settings - look for Private DNS (ColorOS: Connection & sharing)", Toast.LENGTH_LONG).show()
                }
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }
}
