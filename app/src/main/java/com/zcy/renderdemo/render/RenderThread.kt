package com.zcy.renderdemo.render

import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.zcy.renderdemo.CamManager
import com.zcy.renderdemo.LocalVideoManager
import com.zcy.renderdemo.gles.EglCore
import com.zcy.renderdemo.gles.GlUtil
import com.zcy.renderdemo.gles.WindowSurface
import com.zcy.renderdemo.mediator.Mediator
import com.zcy.renderdemo.utils.L

/**
 * 渲染线程，绘制主画面和各种副画面
 *
 */
class RenderThread(mediator: Mediator) : HandlerThread("RenderThread") {

    var cameraScene: CameraScene?= null
    var mediator: Mediator? = mediator

    var handler: Handler? = null
    private var mEglCore: EglCore? = null
    private var mTextureRender: TextureRender? = null
    private var mainFrameBuffer: GLFrameBuffer? = null
    private var drawInternal: Long = 1000 / 45

    private var mOverlayManager: OverlayManager? = null

    var mWindowSurfaceWidth = 1080
    var mWindowSurfaceHeight = 1920
    var surfaceState=0x00
    var camSurfaceAvailable=0x01
    var subSurfaceAvailable=0x10

    fun sendSurfaceAvailable(holder: SurfaceTexture?, width: Int, height: Int) {
        surfaceState = surfaceState or camSurfaceAvailable
        handler?.sendMessage(handler?.obtainMessage(MSG_SURFACE_AVAILABLE, width, height, holder))
    }

    fun sendSurfaceAvailable(sceneId:Int,holder: SurfaceTexture?, width: Int, height: Int) {
        surfaceState = surfaceState or subSurfaceAvailable
        handler?.sendMessage(handler?.obtainMessage(MSG_SUB_SURFACE_AVAILABLE, width, height, holder))
        if(surfaceState== 0x11)
            handler?.sendMessage(handler?.obtainMessage(MSG_REDRAW, timestamp))
    }

    var streamList=ArrayList<Int>()
    var currentStream=0
    // Orthographic projection matrix.
    private val mDisplayProjectionMatrix = FloatArray(16)
    private var startTime = 0L

    private var mWindowSurface: WindowSurface? = null
    var subSceneRender: ShareStreamRender? = null
    var sceneSurfaceRender: SceneSurfaceRender? = null

    init {
        sceneSurfaceRender = SceneSurfaceRender(this)
    }

    @Volatile
    private var timestamp = 0L

    override fun onLooperPrepared() {
        super.onLooperPrepared()
        handler = object : Handler(looper) {
            override // runs on RenderThread
            fun handleMessage(msg: Message) {
                val what = msg.what

                when (what) {
                    MSG_SURFACE_AVAILABLE -> surfaceAvailable(msg.obj as SurfaceTexture, msg.arg1, msg.arg2)
                    MSG_SUB_SURFACE_AVAILABLE -> subsurfaceAvailable(msg.obj as SurfaceTexture, msg.arg1, msg.arg2)
                    MSG_OPEN_CAM ->CamManager(this@RenderThread)
                    MSG_OPEN_LOCAL_STREAM -> LocalVideoManager(this@RenderThread)
                    MSG_REDRAW -> {
                        val current = System.currentTimeMillis()
                        if (current - startTime >= 1 * 1000) {
                            startTime = current
                        }
                        draw(System.currentTimeMillis())
                        subSceneRender?.drawSubScene()

                        sendEmptyMessageDelayed(MSG_REDRAW, drawInternal)
                    }
                }

            }
        }
        // Prepare EGL and open the camera before we start handling messages.
        mEglCore = EglCore(null, EglCore.FLAG_RECORDABLE)
        mEglCore?.makeCurrent(null, null)
        mTextureRender = TextureRender()
        mOverlayManager = OverlayManager(mWindowSurfaceWidth, mWindowSurfaceHeight)
        mainFrameBuffer = GlUtil.prepareFrameBuffer(1080, 2160)

        cameraScene= CameraScene()
        cameraScene?.prepareGl()
        cameraScene?.setShareEglContext(shareEglContext())
        cameraScene?.start()

        subSceneRender = ShareStreamRender(this)
        subSceneRender?.prepareLooper(looper,mEglCore!!)
    }


    fun shareEglContext(): EGLContext {
        return mEglCore!!.shareContext()
    }

    /**
     * Draws the hdmiScene and submits the buffer.
     */
    private fun draw(time: Long) {
        val t1 = System.currentTimeMillis()
        GlUtil.checkGlError("draw start")

        var drawLiveBuffer = false
        drawMainTexture(drawLiveBuffer)
        mWindowSurface!!.makeCurrent()
        mTextureRender?.setUseTexture2D()
        mTextureRender?.drawTexture(mainFrameBuffer!!.frameBufferTextureId)


        GLES30.glFinish()
        val swap = mWindowSurface!!.swapBuffers()
        if (!swap) {
            L.e(TAG, "preview render swap buffer failed ")
            GlUtil.checkGlError("preview render swap buffer failed")
            return
        }
        val t4 = System.currentTimeMillis()
        val d3 = t4 - t1

    }

    private fun drawMainTexture(drawLiveBuffer: Boolean) {
        var fbo = mainFrameBuffer

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo!!.framebuffer)

        GLES30.glClearColor(0.176f, 0.184f, 0.2f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        cameraScene?.drawStreamScene(mWindowSurfaceWidth, mWindowSurfaceHeight, currentStream, null, false, 0f, 0f, mDisplayProjectionMatrix, true)
        drawExtra()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    private fun drawExtra() {

        //绘制贴纸列表
//        mOverlayManager!!.drawOverlaysWithoutHide(mDisplayProjectionMatrix)
        mOverlayManager?.drawOverlay(mDisplayProjectionMatrix)
    }


    /**
     * Handles the surface-created previewCallback from SurfaceView.  Prepares GLES and the Surface.
     */
    private fun surfaceAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {

        if (mWindowSurface != null) {
            mWindowSurface?.release()
        }
        mWindowSurface = WindowSurface(mEglCore!!, surfaceTexture!!)
        mWindowSurface!!.makeCurrent()

        mWindowSurfaceWidth = 1080 //width//width/3*4
        mWindowSurfaceHeight =1810 //height
        Log.i(TAG, "width:$mWindowSurfaceWidth height:$mWindowSurfaceHeight")

        GLES30.glViewport(0, 0, mWindowSurfaceWidth, mWindowSurfaceHeight)

        Matrix.orthoM(mDisplayProjectionMatrix, 0, 0f, mWindowSurfaceWidth*1.0f, 0f, mWindowSurfaceHeight*1.0f, -1f, 1f)
        if(surfaceState== 0x11)
        handler?.sendMessage(handler?.obtainMessage(MSG_REDRAW, timestamp))

    }

    private fun subsurfaceAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
        sceneSurfaceRender?.initSubScene(surfaceTexture,width,height)
    }


    companion object {
        public val TAG = "RenderThread"
        // Messages
        private val MSG_SURFACE_AVAILABLE = 0
        private val MSG_SUB_SURFACE_AVAILABLE = 1
        private val MSG_OPEN_CAM=2
        private val MSG_REDRAW = 3
        private val MSG_OPEN_LOCAL_STREAM=4
    }

    /**
     * Releases most of the GL resources we currently hold (anything allocated by
     * surfaceAvailable()).
     *
     *
     * Does not release EglCore.
     */
    private fun releaseGl() {
//        GlUtil.checkGlError("releaseGl start")

        if (mWindowSurface != null) {
            mWindowSurface!!.release()
            mWindowSurface = null
        }

        if (mTextureRender != null) {
            mTextureRender!!.release()
            mTextureRender = null
        }

        if (mainFrameBuffer != null) {
            GlUtil.releaseFrameBuffer(mainFrameBuffer!!)
            mainFrameBuffer = null
        }
        GlUtil.checkGlError("releaseGl done")

        mEglCore!!.makeNothingCurrent()
    }

    fun switchStream() {
        currentStream=(currentStream+1) % streamList.size
    }

    fun openCam() {
        handler?.sendMessage(handler?.obtainMessage(MSG_OPEN_CAM))
        streamList?.add(0)
        currentStream=0
    }

    fun openLocalStream(){
        handler?.sendMessage(handler?.obtainMessage(MSG_OPEN_LOCAL_STREAM))
        streamList?.add(1)
        currentStream=1
    }


}