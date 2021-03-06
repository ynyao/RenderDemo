package com.zcy.renderdemo

import android.net.Uri
import com.zcy.renderdemo.render.RenderThread

class LocalVideoManager(renderThread: RenderThread) {

    var previewWidth=RenderThread.mWindowSurfaceWidth
    var previewHeight=RenderThread.mWindowSurfaceHeight
    var player:LocalPlayer?=null

    init {
        player= LocalPlayer()
        player?.setupPlayUrl(Uri.parse("file:///android_asset/input.mp4"))


        val sft = renderThread?.mainDisplay?.generateTexture(1)//textureView.surfaceTexture
        sft?.setDefaultBufferSize(previewWidth, previewHeight)
        player?.open(0,sft,object:LocalPlayer.PreviewDataCallback{
            override fun onPreviewFrame(data: ByteArray?) {

            }
        })
    }



}