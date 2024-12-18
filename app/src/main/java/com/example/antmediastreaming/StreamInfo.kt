package com.example.antmediastreaming

import io.antmedia.webrtcandroidframework.api.IWebRTCClient
import org.webrtc.SurfaceViewRenderer

data class StreamInfo(
    val streamId: String,
    val webRTCClient: IWebRTCClient? = null,
    val surfaceViewRenderer: SurfaceViewRenderer,
)