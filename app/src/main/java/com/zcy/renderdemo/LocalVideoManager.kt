package com.zcy.renderdemo

import android.net.Uri
import com.zcy.renderdemo.render.RenderThread

class LocalVideoManager(renderThread: RenderThread) {

    var previewWidth=2160
    var previewHeight=1080
    var player:LocalPlayer?=null

    init {
        player= LocalPlayer()
        player?.setupPlayUrl(Uri.parse("file:///android_asset/input.mp4"))


        val sft = renderThread?.cameraScene?.generateTexture(1)//textureView.surfaceTexture
        sft?.setDefaultBufferSize(previewWidth, previewHeight)
        player?.open(0,sft,object:LocalPlayer.PreviewDataCallback{
            override fun onPreviewFrame(data: ByteArray?) {

            }
        })
    }



}