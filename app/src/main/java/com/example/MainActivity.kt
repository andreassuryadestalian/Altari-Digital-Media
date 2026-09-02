package com.example

import android.os.Bundle
import coil.ImageLoader
import coil.Coil
import coil.decode.ImageDecoderDecoder
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.features.dashboard.DashboardScreen
import com.example.presentation.PresentationServer
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var presentationServer: PresentationServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageLoader = ImageLoader.Builder(this)
            .components { 
                if (Build.VERSION.SDK_INT >= 28) { 
                    add(ImageDecoderDecoder.Factory()) 
                } else { 
                    add(coil.decode.GifDecoder.Factory()) 
                } 
            }
            .build()
        Coil.setImageLoader(imageLoader)
        
        // Prevent background thread or server exceptions from crashing the Android application process
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("MainActivity", "Uncaught exception in thread ${thread.name}", throwable)
            if (thread == android.os.Looper.getMainLooper().thread) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        presentationServer = PresentationServer(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(server = presentationServer)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            presentationServer.webServer.stop()
        } catch (_: Throwable) {}
    }
}
