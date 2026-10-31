package com.privatedns.toggle

import android.app.Activity
import android.os.Bundle

class OpenDnsSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrivateDns.openSettings(this)
        finish()
    }
}
