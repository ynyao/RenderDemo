package com.zcy.renderdemo.gles

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import java.util.*

class CameraScene: HandlerThread("camera_scene"), SurfaceTexture.OnFrameAvailableListener  {

    private var mEglCore: EglCore? = null
    private var shareContext: EGLContext? = null
    var handler: Handler? = null
    private val renderMap = HashMap<Looper, SceneRenderUtil>()
    private var mOffscreenSurface: OffscreenSurface? = null

    fun setShareEglContext(context: EGLContext) {
        shareContext = context
    }


    override fun onLooperPrepared() {
        super.onLooperPrepared()
        handler = object : Handler(looper) {
            override fun handleMessage(msg: Message?) {
                super.handleMessage(msg)
                val what = msg?.what

                when (what) {
                    MSG_OPEN_CAMERA -> {
                        val streamId = msg.obj as Int
                        val log = "open camera $streamId"
                    }

                    MSG_CLOSE_CAMERA -> {
                        val log = "close stream ${msg.arg1}"
                    }

                    MSG_QUIT -> {
                        handler?.removeCallbacksAndMessages(null)
                        release()
                        quitSafely()
                        handler = null
                        val log = "camera scene thread stop................"
                    }
                }

            }
        }
        mEglCore = EglCore(shareContext, EglCore.FLAG_RECORDABLE)
        mOffscreenSurface = OffscreenSurface(mEglCore!!, 1, 1)
        mOffscreenSurface!!.makeCurrent()
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
//        glInitializer = true
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
        var textureId = textureMap[streamId] ?: return 0
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
        if (surfaceTexture == surfaceTex)
            surfaceTexture?.updateTexImage()
    }

    var surfaceTex: SurfaceTexture? = null

    @SuppressLint("UseSparseArrays")
    var textureMap = HashMap<Int, Int>()//id,textureid   camId 0

    fun generateCamTexture(): SurfaceTexture {
        var textureId = GlUtil.createTextureObject()
        var sft = SurfaceTexture(textureId)
        surfaceTex = sft
        textureMap[0] = textureId
        sft.setOnFrameAvailableListener(this)
        return sft
    }

    private fun release() {
        Log.d("RELEASE_LEAK", "camera scene release")
//        closePdf()
//        for(i in 0 until cameraScenes.size) {
//            val it = cameraScenes[i]
//            it ?: continue
//            releaseScene(it)
//        }
//        cameraScenes.clear()
//
//        mDestroyed = true
//
        if (mOffscreenSurface != null) {
            mOffscreenSurface!!.release()
            mOffscreenSurface = null
        }
//        mPdfProgram?.release()
//        mPdfProgram = null
        mEglCore?.release()
    }

    fun sendOpenCamera(streamId: Int, width: Int, height: Int) {
            handler?.obtainMessage(MSG_OPEN_CAMERA, width, height, streamId)?.sendToTarget()
    }

    companion object {
        val TAG = "SceneRender"

        // Messages
        private val MSG_OPEN_CAMERA = 0
        private val MSG_CLOSE_CAMERA = 1
        private val MSG_QUIT = 3

    }
}