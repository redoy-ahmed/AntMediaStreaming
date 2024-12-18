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
import androidx.compose.foundation.layout.PaddingValues
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

    private var webRTCClient: IWebRTCClient? = null
    private val webRTCClients = mutableStateListOf<IWebRTCClient?>()
    private val remoteRenderers = mutableStateListOf<SurfaceViewRenderer>()

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
            PlayScreen()
        }
    }

    private fun addWebRTCStream() {
        if (remoteRenderers.size > 3) return

        val renderer = SurfaceViewRenderer(this)
        remoteRenderers.add(renderer)
        webRTCClients.add(null)

        startStopStream(index = webRTCClients.lastIndex)
    }

    private fun stopWebRtcStream(index: Int) {
        startStopStream(index = index)

        remoteRenderers.removeAt(index)
        webRTCClients.removeAt(index)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.Green)
                ) {
                    AndroidView(
                        factory = {
                            remoteRenderer
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
                    itemsIndexed(remoteRenderers) { index, item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                                .background(color = Color.Red)
                        ) {
                            AndroidView(
                                factory = {
                                    item
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
                    addWebRTCStream()
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
        if (webRTCClient != null && webRTCClient!!.isDataChannelEnabled) {

            val buffer = ByteBuffer.wrap(message.toByteArray(StandardCharsets.UTF_8))
            val buf = DataChannel.Buffer(buffer, false)
            webRTCClient!!.sendMessageViaDataChannel(ServerInfo.STREAM_ID_PLAY_TEST, buf)

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

        if (!webRTCClient!!.isStreaming(ServerInfo.STREAM_ID_PLAY_TEST)) {
            webRTCClient!!.play(ServerInfo.STREAM_ID_PLAY_TEST)
            statusText = "Connecting..."
            statusColor = Color.Blue
        } else {
            webRTCClient!!.stop(ServerInfo.STREAM_ID_PLAY_TEST)
            statusText = "Disconnected"
            statusColor = Color.Red
        }
    }

    private fun startStopStream(index: Int) {
        if (webRTCClients[index] == null) {
            createWebRTCClient(index)
        }

        if (!webRTCClients[index]!!.isStreaming(ServerInfo.STREAM_ID_PLAY_TEST)) {
            webRTCClients[index]!!.play(ServerInfo.STREAM_ID_PLAY_TEST)
            /*statusText = "Connecting..."
            statusColor = Color.Blue*/
        } else {
            webRTCClients[index]!!.stop(ServerInfo.STREAM_ID_PLAY_TEST)

            /*statusText = "Disconnected"
            statusColor = Color.Red*/
        }
    }


    private fun createWebRTCClient() {
        webRTCClient = IWebRTCClient.builder()
            .addRemoteVideoRenderer(remoteRenderer)
            .setServerUrl(ServerInfo.SERVER_URL_PLAY_TEST)
            .setActivity(this)
            .setBluetoothEnabled(bluetoothEnabled)
            .setVideoCallEnabled(false)
            .setWebRTCListener(createWebRTCListener())
            .build()
    }

    private fun createWebRTCClient(index: Int) {
        val renderer = remoteRenderers[index]
        webRTCClients[index] = IWebRTCClient.builder()
            .addRemoteVideoRenderer(renderer)
            .setServerUrl(ServerInfo.SERVER_URL_PLAY_TEST)
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
