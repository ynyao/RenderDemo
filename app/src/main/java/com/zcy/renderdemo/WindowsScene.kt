package com.zcy.renderdemo

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.TextureView
import com.zcy.renderdemo.mediator.Director
import com.zcy.renderdemo.render.Colleague
import com.zcy.renderdemo.utils.L

/**
 * 主显示区域画面
 * @property renderThread RenderThread?
 * @property cameraPreview WindowPreview?
 * @constructor
 */
class WindowsScene(context: Context, private val director: Director) :Colleague(director){

    private var cameraPreview: WindowPreview? = null

    init {
        cameraPreview=WindowPreview(context)
    }


    /**
     * 显示屏幕可以确定是 可以控制主显示区域的大小，比如说放大缩小全屏等
     */
    inner class WindowPreview(context : Context) :TextureView(context) ,TextureView.SurfaceTextureListener{
        init {
            this.surfaceTextureListener = this
        }
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return true
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
//            renderThread?.sendSurfaceAvailable(surface,width,height)
            director.renderThread?.sendSurfaceAvailable(surface,width,height)
            L.i("onSurfaceTextureAvailable","windowsScene available")
        }

    }

    fun getCameraView(): WindowPreview? {
        return cameraPreview
    }
}