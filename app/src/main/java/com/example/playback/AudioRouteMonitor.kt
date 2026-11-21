package com.example.playback

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.model.AudioOutputRoute
import com.example.model.AudioOutputRouteType

/**
 * Hardware-backed audio output monitor tracking live Bluetooth & Wired headphone connections
 * using Android AudioManager.AudioDeviceCallback + headset/Bluetooth state broadcasts.
 */
object AudioRouteMonitor {

  fun queryActiveOutputRoute(context: Context): AudioOutputRoute {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      ?: return AudioOutputRoute()

    val outputs = runCatching { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull()
      ?: return AudioOutputRoute()

    // 1. Prioritize Bluetooth A2DP / BLE Audio / SCO / Hearing Aid
    val btDevice =
      outputs.firstOrNull { info ->
        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
          info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
          (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (info.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
              info.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)) ||
          (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            info.type == AudioDeviceInfo.TYPE_BLE_BROADCAST) ||
          info.type == AudioDeviceInfo.TYPE_HEARING_AID
      }
    if (btDevice != null) {
      val name =
        resolveBluetoothDeviceName(context, btDevice.productName?.toString())
      return AudioOutputRoute(type = AudioOutputRouteType.BLUETOOTH, deviceName = name)
    }

    // 2. Wired Headphones / Headset / USB Headset / Analog Line
    val wiredDevice =
      outputs.firstOrNull { info ->
        info.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
          info.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
          info.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
          info.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
          info.type == AudioDeviceInfo.TYPE_LINE_ANALOG
      }
    if (wiredDevice != null) {
      val name = wiredDevice.productName?.toString()?.trim().orEmpty()
      return AudioOutputRoute(type = AudioOutputRouteType.WIRED_HEADPHONES, deviceName = name)
    }

    // Fallback check for older APIs or emulated wired state
    @Suppress("DEPRECATION")
    if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
      return AudioOutputRoute(type = AudioOutputRouteType.BLUETOOTH, deviceName = "")
    }
    @Suppress("DEPRECATION")
    if (audioManager.isWiredHeadsetOn) {
      return AudioOutputRoute(type = AudioOutputRouteType.WIRED_HEADPHONES, deviceName = "")
    }

    return AudioOutputRoute(type = AudioOutputRouteType.SPEAKER, deviceName = "")
  }

  @SuppressLint("MissingPermission")
  private fun resolveBluetoothDeviceName(context: Context, fallbackName: String?): String {
    val cleanFallback = fallbackName?.trim().orEmpty()
    if (cleanFallback.isNotEmpty() && !cleanFallback.equals(Build.MODEL, ignoreCase = true)) {
      return cleanFallback
    }
    val hasBtPermission =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    if (!hasBtPermission) return cleanFallback

    return runCatching {
      val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
      val adapter = btManager?.adapter ?: return@runCatching cleanFallback
      val bonded = adapter.bondedDevices
      bonded?.firstOrNull { dev ->
        runCatching {
          val method = dev.javaClass.getMethod("isConnected")
          (method.invoke(dev) as? Boolean) == true
        }.getOrDefault(false)
      }?.name?.trim()?.takeIf { it.isNotEmpty() } ?: cleanFallback
    }.getOrDefault(cleanFallback)
  }

  class Registration(
    private val context: Context,
    private val audioManager: AudioManager?,
    private val callback: AudioDeviceCallback?,
    private val receiver: BroadcastReceiver?,
  ) {
    fun unregister() {
      if (audioManager != null && callback != null) {
        runCatching { audioManager?.unregisterAudioDeviceCallback(callback) }
      }
      if (receiver != null) {
        runCatching { context.unregisterReceiver(receiver) }
      }
    }
  }

  fun register(
    context: Context,
    onBecomingNoisy: () -> Unit = {},
    onRouteChanged: (AudioOutputRoute) -> Unit,
  ): Registration {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val handler = Handler(Looper.getMainLooper())

    fun syncNow() {
      val route = queryActiveOutputRoute(context)
      AudioOutputRouteBus.updateRoute(route)
      onRouteChanged(route)
    }

    syncNow()

    val deviceCallback =
      object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
          syncNow()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
          syncNow()
        }
      }

    audioManager?.registerAudioDeviceCallback(deviceCallback, handler)

    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
          when (intent?.action) {
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
              onBecomingNoisy()
              syncNow()
            }
            AudioManager.ACTION_HEADSET_PLUG,
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
              syncNow()
            }
          }
        }
      }

    val filter =
      IntentFilter().apply {
        addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        addAction(AudioManager.ACTION_HEADSET_PLUG)
        addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
      }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      context.registerReceiver(receiver, filter)
    }

    return Registration(
      context = context,
      audioManager = audioManager,
      callback = deviceCallback,
      receiver = receiver,
    )
  }
}
