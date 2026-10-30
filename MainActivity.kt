package com.mahim.aisignalpro

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val service = Intent(this, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_START
                    putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
                }
                ContextCompat.startForegroundService(this, service)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var message by remember { mutableStateOf("প্রথমে প্রয়োজনীয় অনুমতি দিন।") }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("MAHIM AI SIGNAL PRO", style = MaterialTheme.typography.headlineSmall)
                    Text("Android 14+ • Manual chart scan • Screen capture")
                    HorizontalDivider()
                    Button(onClick = {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName"))
                            startActivity(i)
                            message = "অনুমতি দিয়ে অ্যাপে ফিরে আসুন।"
                        } else {
                            message = "Floating permission granted."
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("১. Floating permission") }

                    Button(onClick = {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            message = "আগে Floating permission দিন।"
                        } else {
                            startService(Intent(this@MainActivity, FloatingOverlayService::class.java))
                            message = "Floating AI PRO bubble চালু হয়েছে।"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("২. Floating Bubble ON") }

                    Button(onClick = {
                        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        projectionLauncher.launch(manager.createScreenCaptureIntent())
                    }, modifier = Modifier.fillMaxWidth()) { Text("৩. Start Screen Sharing") }

                    Button(onClick = {
                        startService(Intent(this@MainActivity, ScreenCaptureService::class.java).apply {
                            action = ScreenCaptureService.ACTION_STOP
                        })
                        message = "Screen capture বন্ধ করার অনুরোধ পাঠানো হয়েছে।"
                    }, modifier = Modifier.fillMaxWidth()) { Text("Stop Screen Sharing") }

                    Button(onClick = {
                        stopService(Intent(this@MainActivity, FloatingOverlayService::class.java))
                        message = "Floating bubble বন্ধ হয়েছে।"
                    }, modifier = Modifier.fillMaxWidth()) { Text("Floating Bubble OFF") }

                    Text(message, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "গুরুত্বপূর্ণ: Android-এর অনুমতি ছাড়া অন্য অ্যাপের স্ক্রিন দেখা যাবে না। " +
                        "এই প্রথম ধাপে স্ক্রিন ফ্রেম নেওয়ার ব্যবস্থা তৈরি করা হয়েছে; Gemini বিশ্লেষণ পরের ধাপ।",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
