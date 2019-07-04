package com.zcy.renderdemo.mediator

import android.content.Context
import android.view.View
import com.zcy.renderdemo.NaviController
import com.zcy.renderdemo.WindowsScene
import com.zcy.renderdemo.render.RenderThread

class Director(context:Context):Mediator(){

    var renderThread: RenderThread?=null
    var windowsScene: WindowsScene?=null
    var naviController:NaviController?=null

    init {
        windowsScene= WindowsScene(context,this)
        renderThread=RenderThread(this)
        renderThread?.start()
    }

    fun setNavi(layout: View) {
        naviController = NaviController(layout,this)
    }

    fun addCameraStream() {
        renderThread?.openCam()
        naviController?.addCameraStream()
    }

    fun addLocalStream() {
        renderThread?.openLocalStream()
        naviController?.addLocalStream()
    }

    fun switchStream() {
        renderThread?.switchStream()
    }
}