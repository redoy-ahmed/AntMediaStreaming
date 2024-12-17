package com.example.antmediastreaming

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener
import io.antmedia.webrtcandroidframework.api.IWebRTCClient
import io.antmedia.webrtcandroidframework.api.IWebRTCListener
import org.webrtc.SurfaceViewRenderer

class PublishActivity : ComponentActivity() {

    private var webRTCClient: IWebRTCClient? = null
    private var bluetoothEnabled = false
    private var initBeforeStream = false

    private val serverURL: String = "wss://antmedia.workuplift.com/live/websocket"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PublishScreen()
        }
    }

    @Composable
    fun PublishScreen() {
        var streamId by remember { mutableStateOf("streamId_JmvO8hEmT") }
        var isStreaming by remember { mutableStateOf(false) }
        var statusText by remember { mutableStateOf("Disconnected") }
        val context = LocalContext.current

        val cameraPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    createWebRTCClient()
                } else {
                    Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }

        LaunchedEffect(Unit) {
            if (initBeforeStream) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    createWebRTCClient()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            } else {
                createWebRTCClient()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
              AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            init(null, null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = streamId,
                onValueChange = { streamId = it },
                label = { Text("Stream ID") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    startStopStream(streamId) { streaming ->
                        isStreaming = streaming
                        statusText = if (streaming) "Live" else "Disconnected"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isStreaming) "Stop" else "Start")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusText,
                color = when (statusText) {
                    "Live" -> Color.Green
                    "Reconnecting" -> Color.Blue
                    else -> Color.Red
                },
                modifier = Modifier.padding(8.dp)
            )
        }
    }

    private fun createWebRTCClient() {
        webRTCClient = IWebRTCClient.builder()
            .setLocalVideoRenderer(getSurfaceViewRenderer())
            .setServerUrl(serverURL)
            .setActivity(this)
            .setInitiateBeforeStream(initBeforeStream)
            .setBluetoothEnabled(bluetoothEnabled)
            .setWebRTCListener(createWebRTCListener())
            .build()
    }

    private fun getSurfaceViewRenderer(): SurfaceViewRenderer {
        return SurfaceViewRenderer(this).apply {
            init(null, null)
        }
    }

    private fun startStopStream(streamId: String, onStreamToggle: (Boolean) -> Unit) {
        webRTCClient?.let {
            if (!it.isStreaming(streamId)) {
                Log.i("PublishActivity", "Calling publish start")
                it.publish(streamId)
                onStreamToggle(true)
            } else {
                Log.i("PublishActivity", "Calling publish stop")
                it.stop(streamId)
                onStreamToggle(false)
            }
        }
    }

    private fun createWebRTCListener(): IWebRTCListener {
        return object : DefaultWebRTCListener() {
            override fun onPublishStarted(streamId: String) {
                super.onPublishStarted(streamId)
                Log.i("PublishActivity", "Publish started")
            }

            override fun onPublishFinished(streamId: String) {
                super.onPublishFinished(streamId)
                Log.i("PublishActivity", "Publish finished")
            }
        }
    }

    @Composable
    @Preview
    fun PreviewPublishScreen(){
        PublishScreen()
    }
}
