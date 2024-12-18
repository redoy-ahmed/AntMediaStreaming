package com.example.antmediastreaming

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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

class PlayActivity : ComponentActivity() {
    private lateinit var primaryStream: StreamInfo

    private val streams = mutableStateListOf<StreamInfo>()

    private var bluetoothEnabled = false

    var isPlaying by mutableStateOf(false)
    var statusText by mutableStateOf("Disconnected")
    var statusColor by mutableStateOf(Color.Red)

    private var showDialog by mutableStateOf(false)
    private var messageText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        primaryStream = StreamInfo(
            streamId = ServerInfo.STREAM_ID,
            webRTCClient = null,
            surfaceViewRenderer = SurfaceViewRenderer(this)
        )

        setContent {
            PlayScreen()
        }
    }

    private fun addSecondaryWebRTCStream(streamId: String = ServerInfo.STREAM_ID) {
        if (streams.size > 3) return

        val renderer = SurfaceViewRenderer(this)

        streams.add(
            StreamInfo(
                surfaceViewRenderer = renderer,
                webRTCClient = null,
                streamId = streamId
            )
        )

        startStopStream(index = streams.lastIndex)
    }

    private fun stopWebRtcStream(index: Int) {
        startStopStream(index = index)

        streams.removeAt(index)
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
            Row {
                IconButton(
                    onClick = {
                        enterPIPMode()
                    }
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Text(
                    text = statusText,
                    textAlign = TextAlign.End,
                    color = statusColor,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .weight(1f)
                )
            }


            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.Green)
                ) {
                    AndroidView(
                        factory = {
                            primaryStream.surfaceViewRenderer
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(streams) { index, item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                                .background(color = Color.Red)
                        ) {
                            AndroidView(
                                factory = {
                                    item.surfaceViewRenderer
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            IconButton(
                                modifier = Modifier.align(Alignment.TopEnd),
                                onClick = {
                                    stopWebRtcStream(index = index)
                                }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

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

            Button(
                onClick = {
                    addSecondaryWebRTCStream()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add a stream")
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
        val webRTCClient = primaryStream.webRTCClient

        if (webRTCClient != null && webRTCClient.isDataChannelEnabled) {

            val buffer = ByteBuffer.wrap(message.toByteArray(StandardCharsets.UTF_8))
            val buf = DataChannel.Buffer(buffer, false)
            webRTCClient.sendMessageViaDataChannel(ServerInfo.STREAM_ID, buf)

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
        val streamId = primaryStream.streamId
        val renderer = primaryStream.surfaceViewRenderer

        if (primaryStream.webRTCClient == null) {
            primaryStream = primaryStream.copy(webRTCClient = createWebRTCClient(renderer))
        }

        primaryStream.webRTCClient?.let {
            if (!it.isStreaming(streamId)) {
                it.play(streamId)
                statusText = "Connecting..."
                statusColor = Color.Blue
            } else {
                it.stop(streamId)
                statusText = "Disconnected"
                statusColor = Color.Red
            }
        }

    }

    private fun startStopStream(index: Int) {
        val streamId = streams[index].streamId
        val renderer = streams[index].surfaceViewRenderer

        if (streams[index].webRTCClient == null) {
            streams[index] = streams[index].copy(webRTCClient = createWebRTCClient(renderer))
        }

        streams[index].webRTCClient?.let {
            if (!it.isStreaming(streamId)) {
                it.play(streamId)
            } else {
                it.stop(streamId)
            }
        }
    }


    private fun createWebRTCClient(renderer: SurfaceViewRenderer) = IWebRTCClient.builder()
        .addRemoteVideoRenderer(renderer)
        .setServerUrl(ServerInfo.PLAY_URL)
        .setActivity(this)
        .setBluetoothEnabled(bluetoothEnabled)
        .setVideoCallEnabled(false)
        .setWebRTCListener(createWebRTCListener())
        .build()


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


    private fun enterPIPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16))
                .build()


            enterPictureInPictureMode(params)
        } else {
            enterPictureInPictureMode()
        }
    }

    @Composable
    @Preview
    fun PreviewPlayScreen() {
        PlayScreen()
    }
}
