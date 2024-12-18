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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener
import io.antmedia.webrtcandroidframework.api.IWebRTCClient
import io.antmedia.webrtcandroidframework.api.IWebRTCListener
import org.webrtc.DataChannel
import org.webrtc.SurfaceViewRenderer
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

class FloatingPlayActivity : ComponentActivity() {

    private var webRTCClient: IWebRTCClient? = null
    private var bluetoothEnabled = false
    private lateinit var remoteRenderer: SurfaceViewRenderer

    var isPlaying by mutableStateOf(false)
    var statusText by mutableStateOf("Disconnected")
    var statusColor by mutableStateOf(Color.Red)

    private var showDialog by mutableStateOf(false)
    private var messageText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        remoteRenderer = SurfaceViewRenderer(this)

        setContent {
            FloatingPlayScreen()
        }
    }

    @Composable
    fun FloatingPlayScreen() {
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
                DraggableFloatingVideo()
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

            Spacer(modifier = Modifier.height(8.dp))

            if (isPlaying) {
                Button(
                    onClick = {
                        showDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Message")
                }
            }

            if (showDialog) {
                MessageDialog(
                    onSend = { sendMessage(it) },
                    onDismiss = { showDialog = false }
                )
            }
        }
    }

    private fun sendMessage(message: String) {
        if (webRTCClient != null && webRTCClient!!.isDataChannelEnabled) {

            val buffer = ByteBuffer.wrap(message.toByteArray(StandardCharsets.UTF_8))
            val buf = DataChannel.Buffer(buffer, false)
            webRTCClient!!.sendMessageViaDataChannel(ServerInfo.STREAM_ID, buf)

            Toast.makeText(this, "Message sent: $message", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Data Channel not Available", Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    fun MessageDialog(onSend: (String) -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Send Message") },
            text = {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Enter your message") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSend(messageText)
                    messageText = ""
                    onDismiss()
                }) {
                    Text("Send")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
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
            .setServerUrl(ServerInfo.PLAY_URL)
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
    fun DraggableFloatingVideo() {
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        val screenWidth by remember { mutableFloatStateOf(500f) }
        val screenHeight by remember { mutableFloatStateOf(500f) }
        var boxWidth by remember { mutableFloatStateOf(0f) }
        var boxHeight by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .onGloballyPositioned { layoutCoordinates ->
                    boxWidth = layoutCoordinates.size.width.toFloat()
                    boxHeight = layoutCoordinates.size.height.toFloat()
                }
                .offset {
                    IntOffset(
                        x = offsetX.roundToInt(),
                        y = offsetY.roundToInt()
                    )
                }
                .size(100.dp, 200.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x)
                            .coerceIn(screenWidth / 2, screenWidth - boxWidth)
                        offsetY = (offsetY + dragAmount.y)
                            .coerceIn(0f, screenHeight - boxHeight)
                    }
                }
        ) {
            AndroidView(
                factory = {
                    remoteRenderer
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @Composable
    @Preview
    fun PreviewFloatingPlayScreen() {
        FloatingPlayScreen()
    }
}
