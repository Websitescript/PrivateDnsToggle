package com.privatedns.toggle

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * Best-effort widget sync: JobScheduler fires when the Private DNS settings change,
 * even if the app process is not running. OEM battery managers may delay it.
 */
class DnsChangeJobService : android.app.job.JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        DnsWidgetProvider.updateAll(applicationContext)
        schedule(applicationContext)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        private const val JOB_ID = 4242

        fun schedule(ctx: Context) {
            val scheduler = ctx.getSystemService(JobScheduler::class.java)
            val ids = AppWidgetManager.getInstance(ctx)
                .getAppWidgetIds(ComponentName(ctx, DnsWidgetProvider::class.java))
            if (ids.isEmpty()) {
                scheduler.cancel(JOB_ID)
                return
            }
            val builder = JobInfo.Builder(JOB_ID, ComponentName(ctx, DnsChangeJobService::class.java))
            listOf("private_dns_mode", "private_dns_specifier").forEach {
                builder.addTriggerContentUri(
                    JobInfo.TriggerContentUri(
                        Settings.Global.getUriFor(it),
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
                    )
                )
            }
            builder.setTriggerContentUpdateDelay(300)
            builder.setTriggerContentMaxDelay(1500)
            scheduler.schedule(builder.build())
        }
    }
}
