package com.privatedns.toggle

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews

class DnsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = build(context)
        ids.forEach { manager.updateAppWidget(it, views) }
        DnsChangeJobService.schedule(context)
    }

    override fun onEnabled(context: Context) = DnsChangeJobService.schedule(context)

    override fun onDisabled(context: Context) = DnsChangeJobService.schedule(context)

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DnsWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun build(ctx: Context): RemoteViews {
            val s = PrivateDns.read(ctx)
            return if (Build.VERSION.SDK_INT >= 31) {
                RemoteViews(
                    mapOf(
                        SizeF(100f, 40f) to bind(ctx, R.layout.widget_dns_small, s, false),
                        SizeF(200f, 40f) to bind(ctx, R.layout.widget_dns, s, true)
                    )
                )
            } else {
                bind(ctx, R.layout.widget_dns, s, true)
            }
        }

        private fun bind(ctx: Context, layout: Int, s: DnsState, showProvider: Boolean): RemoteViews {
            val rv = RemoteViews(ctx.packageName, layout)
            rv.setTextViewText(R.id.w_status, s.statusLabel)
            rv.setInt(R.id.w_status, "setBackgroundResource", if (s.isOn) R.drawable.chip_on else R.drawable.chip_off)
            if (showProvider) rv.setTextViewText(R.id.w_provider, s.providerLabel)
            rv.setViewVisibility(R.id.w_toggle, if (s.canWrite) View.VISIBLE else View.GONE)

            val open = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, OpenDnsSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val toggle = PendingIntent.getBroadcast(
                ctx, 1, Intent(ctx, ToggleReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            rv.setOnClickPendingIntent(R.id.w_root, open)
            rv.setOnClickPendingIntent(R.id.w_settings, open)
            rv.setOnClickPendingIntent(R.id.w_toggle, toggle)
            return rv
        }
    }
}
