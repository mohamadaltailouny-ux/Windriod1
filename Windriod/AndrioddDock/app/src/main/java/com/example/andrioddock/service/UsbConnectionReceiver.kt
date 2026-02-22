package com.example.andrioddock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.util.Log

class UsbConnectionReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "UsbConnectionReceiver"
        private var usbStateCallback: ((Boolean) -> Unit)? = null
        fun setUsbStateCallback(callback: ((Boolean) -> Unit)?) { usbStateCallback = callback }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED, "android.hardware.usb.action.USB_STATE" -> {
                val connected = intent.getBooleanExtra("connected", false)
                val configured = intent.getBooleanExtra("configured", false)
                Log.d(TAG, "USB State: connected=$connected, configured=$configured")
                usbStateCallback?.invoke(connected && configured)
            }
        }
    }
}
