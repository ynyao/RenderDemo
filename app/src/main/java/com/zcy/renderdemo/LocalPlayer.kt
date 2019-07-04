package com.zcy.renderdemo

import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoRendererEventListener

class LocalPlayer {

    private val TAG = "ExoPlayer"
    var player:SimpleExoPlayer?=null
    var mCallback :PreviewDataCallback?=null
    private var mSurface: SurfaceTexture? = null
    private var lastPlayPosition = 0L
    private var uri: Uri? = null
    fun setupPlayUrl(uri: Uri) {
        this.uri = uri
    }

    fun seekToBegin() {
        if (player == null) return
        player?.seekTo(0)
    }

    fun open(streamId: Int, surface: SurfaceTexture?,
             callback: PreviewDataCallback) {
        open(streamId, surface, callback, false)
    }

    fun open(streamId: Int, surface: SurfaceTexture?,
             callback: PreviewDataCallback, useSoft: Boolean) {
        mCallback = callback
        mSurface = surface
        setUpSimpleExoPlayer(surface!!, useSoft)
//        mStreamId = streamId
    }

    fun setVolumePlayState(isPlayer: Boolean) {
        if (isPlayer) {
            player?.setVolume(0.6f)
        } else {
            player?.setVolume(0f)
        }
    }

    private fun setUpSimpleExoPlayer(surfaceTexture: SurfaceTexture, useSoft: Boolean) {
        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        val videoSource = buildMediaSource(uri!!, null, null, null)
        val defaultAllocator = DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE)
        val loadControl = DefaultLoadControl(defaultAllocator, 1000, 3000, 500, 500, -1, true)
        // SimpleExoPlayer
        if (useSoft) {
//            val renderersFactory = SimpleRenderersFactory(App.getContext(),
//                null, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
//            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, loadControl)
        } else {
            player = ExoPlayerFactory.newSimpleInstance(App.getContext(), trackSelector, loadControl)
        }
        // Prepare the player with the source.
        player?.prepare(videoSource)
        player?.setVolume(0f)
        player?.repeatMode = Player.REPEAT_MODE_ONE
        player?.shuffleModeEnabled = true
        player?.addVideoListener(object : SimpleExoPlayer.VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            }

            override fun onRenderedFirstFrame() {
                mCallback?.onPreviewFrame(ByteArray(1))
            }
        })
        player?.addAudioDebugListener(object : AudioRendererEventListener {
            override fun onAudioEnabled(counters: DecoderCounters?) {
//                AudioSource.notifyAudioStatusChanged(true, mStreamId)
                Log.d(TAG, "onAudioEnabled")
            }

            override fun onAudioSessionId(audioSessionId: Int) {
//                AudioData.getInstance().setSessionId(mStreamId, audioSessionId)
                Log.d(TAG, "onAudioSessionId    ###  $audioSessionId")
            }

            override fun onAudioDecoderInitialized(decoderName: String?, initializedTimestampMs: Long, initializationDurationMs: Long) {
                Log.d(TAG, "onAudioDecoderInitialized")
            }

            override fun onAudioInputFormatChanged(format: Format?) {
//                AudioSource.setFormat(mStreamId, format)
            }

            override fun onAudioSinkUnderrun(bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
                Log.d(TAG, "onAudioSinkUnderrun")
            }

            override fun onAudioDisabled(counters: DecoderCounters?) {
                Log.e(TAG, "onAudioDisabled  ")
//                AudioSource.notifyAudioStatusChanged(false, mStreamId)
            }
        })

        player?.setVideoSurface(Surface(surfaceTexture))
        player?.addListener(object : Player.EventListener {
            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
                Log.e(TAG, "onTimelineChanged timeline:$timeline")
//                AudioData.getInstance().onTimelineChanged(mStreamId)
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}

            override fun onLoadingChanged(isLoading: Boolean) {}

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                /** playbackState 说明
                 * Player.STATE_IDLE = 1
                 * Player.STATE_BUFFERING = 2
                 * Player.STATE_READY = 3
                 * Player.STATE_ENDED = 4
                 */
                Log.e(TAG, "onPlayerStateChanged: $playWhenReady playbackState:$playbackState")
//                AudioData.getInstance().onPlayStateChanged(mStreamId, playbackState)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                Log.e(TAG, "onRepeatModeChanged repeatMode:$repeatMode")
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                Log.e(TAG, "onShuffleModeEnabledChanged shuffleModeEnabled:$shuffleModeEnabled")
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                lastPlayPosition = player?.currentPosition ?: 0
                Log.e(TAG, "onPlayerError error:$error")
                close(false)
                mCallback ?: return
                Log.e(TAG, "onPlayerError error , reopen ... ")
//                if (error?.cause is UnrecognizedInputFormatException && mStreamId == LOCAL_VIDEO_STREAM_ID) {
//                    val deleteSource = DeleteSourceEvent(mStreamId)
//                } else {
//                    open(mStreamId, mSurface, mCallback!!, false)
//                }
//                player!!.prepare(videoSource)
//                player!!.playWhenReady = true
            }

            override fun onPositionDiscontinuity(reason: Int) {
//                AudioData.getInstance().onPositionDiscontinuity(mStreamId)
                Log.e(TAG, "onPositionDiscontinuity reason:$reason")
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                Log.e(TAG, "onPlaybackParametersChanged playbackParameters:$playbackParameters")
            }

            override fun onSeekProcessed() {
                Log.e(TAG, "onSeekProcessed")
//                AudioData.getInstance().onSeekProcessed(mStreamId)
            }

        })
        player?.addVideoDebugListener(object : VideoRendererEventListener {
            override fun onVideoEnabled(counters: DecoderCounters?) {
                Log.e(TAG, "onVideoEnabled : ${counters?.decoderInitCount}")
            }

            override fun onVideoDecoderInitialized(decoderName: String?, initializedTimestampMs: Long, initializationDurationMs: Long) {
                Log.e(TAG, "onVideoDecoderInitialized : $decoderName   $initializedTimestampMs   $initializationDurationMs")
            }

            override fun onVideoInputFormatChanged(format: Format?) {
                Log.e(TAG, "onVideoInputFormatChanged : ${format.toString()}  ")
            }

            override fun onDroppedFrames(count: Int, elapsedMs: Long) {
                Log.e(TAG, "onDroppedFrames : $count   $elapsedMs ")
            }

            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                Log.e(TAG, "onVideoSizeChanged :  w  $width   h  $height   r   $unappliedRotationDegrees  ratio  : $pixelWidthHeightRatio  ")

            }

            override fun onRenderedFirstFrame(surface: Surface?) {
                Log.e(TAG, "onRenderedFirstFrame :  surface ${surface?.isValid} ")
            }

            override fun onVideoDisabled(counters: DecoderCounters?) {
                Log.e(TAG, "onVideoDisabled : ${counters?.decoderInitCount}")
            }
        })

        player?.playWhenReady = true
        player?.seekTo(lastPlayPosition)
    }

    private fun buildMediaSource(
        uri: Uri,
        overrideExtension: String?,
        handler: Handler?,
        listener: MediaSourceEventListener?): MediaSource {

        val mediaDataSourceFactory = buildDataSourceFactory(true)
        val type = if (TextUtils.isEmpty(overrideExtension))
            Util.inferContentType(uri)
        else
            Util.inferContentType("." + overrideExtension!!)
        when (type) {
            C.TYPE_DASH -> return DashMediaSource.Factory(
                DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                buildDataSourceFactory(false))
                .createMediaSource(uri)
            C.TYPE_SS -> return SsMediaSource.Factory(
                DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                buildDataSourceFactory(false))
                .createMediaSource(uri)
            C.TYPE_HLS -> return HlsMediaSource.Factory(mediaDataSourceFactory)
                .createMediaSource(uri)
            C.TYPE_OTHER -> {
//                return if (uri.toString().startsWith("rtmp")) {
//                    Log.d("TYPE_OTHER", "TYPE_RTMP")
//                    val rtmpDataSourceFactory = RtmpDataSourceFactory()
//                    val extractorsFactory = DefaultExtractorsFactory()
//                    val videoSource = ExtractorMediaSource(uri,
//                        rtmpDataSourceFactory, extractorsFactory, null, null)
//                    videoSource
//                } else {
//                    Log.d("TYPE_OTHER", "TYPE_OTHER")
                    ExtractorMediaSource.Factory(mediaDataSourceFactory)
                        .createMediaSource(uri)
//                }
            }
            else -> {
                throw IllegalStateException("Unsupported action: " + type)
            }
        }
        return ExtractorMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(uri)
    }

    private fun buildDataSourceFactory(useBandwidthMeter: Boolean): DataSource.Factory {
        val bandwidthMeter = if (useBandwidthMeter) DefaultBandwidthMeter() else null

        return DefaultDataSourceFactory(App.getContext(), bandwidthMeter,
            DefaultHttpDataSourceFactory("xxx", bandwidthMeter)
        )
    }

    fun previewSuccess() {}

    fun pause(reset:Boolean){
        if(reset) {
            player?.seekTo(0)
        }
        lastPlayPosition = player?.currentPosition ?: 0
        player?.playWhenReady = false
    }
    fun resume(){
        player?.playWhenReady = true
    }

    fun close(isRelease: Boolean = true) {
        player?.release()
        if (!isRelease) return
        player = null
        mCallback = null
        mSurface = null
        lastPlayPosition = 0
    }

    /**
     * An interface which wraps
     * [android.hardware.Camera.PreviewCallback].
     */
    interface PreviewDataCallback {
        fun onPreviewFrame(data: ByteArray?)
    }

}