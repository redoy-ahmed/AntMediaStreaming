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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
    private var bluetoothEnabled = false
    private lateinit var remoteRenderer: SurfaceViewRenderer

    var isPlaying by mutableStateOf(false)
    var statusText by mutableStateOf("Disconnected")
    var statusColor by mutableStateOf(Color.Red)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        remoteRenderer = SurfaceViewRenderer(this)

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
                Toast.makeText(context, "Play permissions are not granted.", Toast.LENGTH_LONG)
                    .show()
            }
        }

        Column(
            modifier = Modifier
                .background(color = Color.Black)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                AndroidView(
                    factory = {
                        remoteRenderer
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                Text(if (isPlaying) "Stop" else "Play")
            }
        }
    }

    private fun startStopStream() {
        if (webRTCClient == null) {
            createWebRTCClient()
        }

        if (!webRTCClient!!.isStreaming(ServerInfo.STREAM_ID)) {
            webRTCClient!!.play(ServerInfo.STREAM_ID)
            statusText = "Connecting..."
            statusColor = Color.Blue
        } else {
            webRTCClient!!.stop(ServerInfo.STREAM_ID)
            statusText = "Disconnected"
            statusColor = Color.Red
        }
    }

    private fun createWebRTCClient() {
        webRTCClient = IWebRTCClient.builder()
            .addRemoteVideoRenderer(remoteRenderer)
            .setServerUrl(ServerInfo.SERVER_URL)
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
                isPlaying = true
            }

            override fun onPlayFinished(streamId: String) {
                super.onPlayFinished(streamId)
                statusText = "Streaming stopped"
                statusColor = Color.Red
                isPlaying = false
            }
        }
    }

    @Composable
    @Preview
    fun PreviewPlayScreen() {
        PlayScreen()
    }
}
