package com.privatedns.toggle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Widget power button. Only visible when WRITE_SECURE_SETTINGS has been granted. */
class ToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val s = PrivateDns.read(context)
        if (s.canWrite) PrivateDns.setEnabled(context, !s.isOn)
        DnsWidgetProvider.updateAll(context)
    }
}
