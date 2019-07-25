package com.zcy.renderdemo.render

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.zcy.renderdemo.gles.EglCore
import com.zcy.renderdemo.gles.GlUtil
import com.zcy.renderdemo.gles.OffscreenSurface
import java.util.*

/**
 * mainview
 *
 */
class MainDisplay: HandlerThread("camera_scene"), SurfaceTexture.OnFrameAvailableListener  {

    private var mEglCore: EglCore? = null
    private var shareContext: EGLContext? = null
    var handler: Handler? = null
    private val renderMap = HashMap<Looper, SceneRenderUtil>()
    private var mOffscreenSurface: OffscreenSurface? = null

    fun setShareEglContext(context: EGLContext) {
        shareContext = context
    }



    @Synchronized
    fun prepareGl(): Boolean {
        val looper = Looper.myLooper()
        looper?: return false
        if(renderMap[looper] != null)
            return true
        val render = SceneRenderUtil()
        render.initGL()
        renderMap[Looper.myLooper()] = render
        return true
    }

    fun drawStreamScene(
        sceneWidth: Int,
        sceneHeight: Int,
        streamId: Int,
        rect: SceneRenderUtil.RenderRect?,
        greenFilter: Boolean,
        smooth: Float,
        similarity: Float,
        projection: FloatArray,
        isSync: Boolean = false
    ): Long {
        val renderUtil = renderMap[Looper.myLooper()]
        renderUtil?.setSceneViewPort(sceneWidth, sceneHeight)
        var textureId = textureIdMap[streamId] ?: return 0
        var w = sceneWidth
        var h = sceneHeight
        renderUtil?.drawStreamScene(
            textureId,
            SceneRenderUtil.RENDERER_TYPE_NORMAL,
            w, h, rect, greenFilter,
            false, smooth,
            similarity, projection
        )

        return System.currentTimeMillis()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        textureMap.forEach { id, texture ->
            if(texture==surfaceTexture){
                surfaceTexture?.updateTexImage()
            }
        }
    }


    @SuppressLint("UseSparseArrays")
    var textureIdMap = HashMap<Int, Int>()//id,textureid   camId 0
    var textureMap = HashMap<Int, SurfaceTexture>()
    var i=0

    fun generateTexture(streamId: Int): SurfaceTexture {
        var textureId = GlUtil.createTextureObject()
        var sft = SurfaceTexture(textureId)
        textureMap[streamId] = sft
        textureIdMap[streamId] = textureId
        sft.setOnFrameAvailableListener(this)
        return sft
    }

    private fun release() {
        Log.d("RELEASE_LEAK", "camera scene release")
//        for(i in 0 until cameraScenes.size) {
//            val it = cameraScenes[i]
//            it ?: continue
//            releaseScene(it)
//        }
//        cameraScenes.clear()
//
//
        if (mOffscreenSurface != null) {
            mOffscreenSurface!!.release()
            mOffscreenSurface = null
        }
        mEglCore?.release()
    }


    companion object {
        val TAG = "SceneRender"

        // Messages
        private val MSG_OPEN_CAMERA = 0
        private val MSG_CLOSE_CAMERA = 1
        private val MSG_QUIT = 3

    }
}