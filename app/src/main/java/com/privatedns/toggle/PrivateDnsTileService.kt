package com.privatedns.toggle

import android.app.PendingIntent
import android.content.Intent
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class PrivateDnsTileService : TileService() {

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refreshTile()
            DnsWidgetProvider.updateAll(applicationContext)
        }
    }

    override fun onTileAdded() = refreshTile()

    override fun onStartListening() {
        PrivateDns.register(this, observer)
        refreshTile()
    }

    override fun onStopListening() {
        PrivateDns.unregister(this, observer)
    }

    override fun onClick() {
        val state = PrivateDns.read(this)
        if (state.canWrite) {
            val ok = PrivateDns.setEnabled(this, !state.isOn)
            refreshTile()
            DnsWidgetProvider.updateAll(this)
            if (!ok) openSettings()
        } else {
            openSettings()
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val s = PrivateDns.read(this)
        tile.state = if (s.isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.subtitle = if (s.isOn) s.providerLabel else "Off"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_dns)
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun openSettings() {
        val intent = Intent(this, OpenDnsSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
