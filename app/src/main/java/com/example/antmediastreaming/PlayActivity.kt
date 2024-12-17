package com.example.antmediastreaming

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener
import io.antmedia.webrtcandroidframework.api.IWebRTCClient
import io.antmedia.webrtcandroidframework.api.IWebRTCListener
import org.webrtc.SurfaceViewRenderer

class PlayActivity : ComponentActivity() {

    private var webRTCClient: IWebRTCClient? = null
    private var streamId by mutableStateOf("streamId_JmvO8hEmT")
    private var bluetoothEnabled = false

    private val serverURL: String = "wss://test.antmedia.io:5443/WebRTCAppEE/websocket"

    var statusText by mutableStateOf("Disconnected")
    var statusColor by mutableStateOf(Color.Red)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PlayScreen()
        }
    }

    @Composable
    fun PlayScreen() {
        val context = LocalContext.current

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                startStopStream()
            } else {
                Toast.makeText(context, "Play permissions are not granted.", Toast.LENGTH_LONG).show()
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Status Text
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Stream ID Input
                var inputStreamId by remember { mutableStateOf(streamId) }
                OutlinedTextField(
                    value = inputStreamId,
                    onValueChange = { inputStreamId = it },
                    label = { Text("Stream ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                // WebRTC SurfaceViewRenderer Placeholder
                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            init(null, null)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                )

                // Start/Stop Button
                Button(
                    onClick = {
                        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            launcher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        } else {
                            startStopStream()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start/Stop")
                }

                // Send Message Button
                Button(
                    onClick = { showSendDataChannelMessageDialog() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Message via Data Channel")
                }
            }
        }
    }

    private fun startStopStream() {
        if (webRTCClient == null) {
            createWebRTCClient()
        }

        if (!webRTCClient!!.isStreaming(streamId)) {
            webRTCClient!!.play(streamId)
            statusText = "Connecting..."
            statusColor = Color.Blue
        } else {
            webRTCClient!!.stop(streamId)
            statusText = "Disconnected"
            statusColor = Color.Red
        }
    }

    private fun createWebRTCClient() {
        webRTCClient = IWebRTCClient.builder()
            .setServerUrl(serverURL)
            .setActivity(this)
            .setBluetoothEnabled(bluetoothEnabled)
            .setVideoCallEnabled(false)
            .setWebRTCListener(createWebRTCListener())
            .build()
    }

    private fun createWebRTCListener(): IWebRTCListener {
        return object : DefaultWebRTCListener() {
            override fun onPlayStarted(streamId: String) {
                super.onPlayStarted(streamId)
                statusText = "Streaming started"
                statusColor = Color.Green
            }

            override fun onPlayFinished(streamId: String) {
                super.onPlayFinished(streamId)
                statusText = "Streaming stopped"
                statusColor = Color.Red
            }
        }
    }

    private fun showSendDataChannelMessageDialog() {
        // Show dialog to send data via DataChannel (not implemented in Compose for simplicity)
    }

    @Composable
    @Preview
    fun PreviewPlayScreen(){
        PlayScreen()
    }
}
