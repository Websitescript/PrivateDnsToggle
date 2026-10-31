package com.privatedns.toggle

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(PrivateDns.read(app))
    val state: StateFlow<DnsState> = _state.asStateFlow()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    init {
        PrivateDns.register(app, observer)
    }

    fun refresh() {
        _state.value = PrivateDns.read(getApplication())
        DnsWidgetProvider.updateAll(getApplication())
    }

    fun toggle() {
        val s = _state.value
        PrivateDns.setEnabled(getApplication(), !s.isOn)
        refresh()
    }

    override fun onCleared() {
        PrivateDns.unregister(getApplication(), observer)
    }
}
