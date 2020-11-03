package com.zcy.renderdemo.mediator

import android.content.Context
import android.view.View
import com.zcy.renderdemo.App
import com.zcy.renderdemo.NaviController
import com.zcy.renderdemo.WindowsScene
import com.zcy.renderdemo.render.RenderThread

class Director private constructor(context: Context): Mediator() {
        companion object {
            val instance = SingletonHolder.holder
        }

        private object SingletonHolder {
            val holder = Director(App.getContext())
        }

    var renderThread: RenderThread? = null
    var windowsScene: WindowsScene? = null
    var naviController: NaviController? = null

    init {
        windowsScene = WindowsScene(context, this)
        renderThread = RenderThread(this)
        renderThread?.start()
    }

    fun setNavi(layout: View) {
        naviController = NaviController(layout, this)
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

    fun getMainView() :View?{
        return windowsScene?.getCameraView()
    }

    fun pause() {
        renderThread?.sendPause()
    }

    fun goSmall(isSmall:Boolean){
        if(!isSmall){
            naviController=null
        }
        renderThread?.goSmallScreen(isSmall)
    }
}