package com.zcy.renderdemo.gles

import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.zcy.renderdemo.utils.L

class RenderThread2 : HandlerThread("RenderThread"){

    var cameraScene:CameraScene ?= null

    var handler: Handler? = null
    private var mEglCore: EglCore? = null
    private var mTextureRender: TextureRender? = null
    private var mainFrameBuffer: GLFrameBuffer? = null
    private var drawInternal: Long = 1000 / 45

    private var mOverlayManager: OverlayManager? = null

    var mWindowSurfaceWidth = 1080
    var mWindowSurfaceHeight = 1920

    fun sendSurfaceAvailable(holder: SurfaceTexture?, width: Int, height: Int) {
        handler?.sendMessage(handler?.obtainMessage(MSG_SURFACE_AVAILABLE, width, height, holder))
    }

    // Orthographic projection matrix.
    private val mDisplayProjectionMatrix = FloatArray(16)
    private var startTime = 0L

    private var mWindowSurface: WindowSurface? = null



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
                    MSG_REDRAW -> {
//                        if (mSurfaceDestoryed) {
//                            return
//                        }
                        val current = System.currentTimeMillis()
                        if (current - startTime >= 1 * 1000) {
                            startTime = current
//                            L.d("testPublish",    "preview  drop fps ${dropFps.fps}  drop   total fps   ${dropFps.totalFps}   ")
                            //   L.d("testPublish",    "preview  fps ${fpsCounter.fps}    total fps   ${fpsCounter.totalFps}   ")
//                            dropFps.reset()
//                            if (fpsCounter.totalFps > 35) {
//                                drawInternal += 2
//                            }
//                            if (fpsCounter.totalFps < 30) {
//                                drawInternal -= 10
//                                if (drawInternal < 0) {
//                                    drawInternal = 0
//                                }
//                            }
//                            fpsCounter.reset()
                        }
//                        if(fpsControl.isDropFrame()){
//                            dropFps.count()
//                            dropFps.update()
//                            return
//                        }
//                        draw(timestamp)
                        draw(System.currentTimeMillis())
//                        subSceneRender.drawSubScene()

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
        mainFrameBuffer = GlUtil.prepareFrameBuffer()
        cameraScene= CameraScene()
        cameraScene?.prepareGl()
        cameraScene?.setShareEglContext(shareEglContext())
        cameraScene?.start()

    }



    fun shareEglContext(): EGLContext {
        return mEglCore!!.shareContext()
    }




    /**
     * Draws the hdmiScene and submits the buffer.
     */
    private fun draw(time: Long) {
//        if (mSurfaceDestoryed || mDestroyed) {
//            return
//        }
        val t1 = System.currentTimeMillis()
        GlUtil.checkGlError("draw start")
//        GLES30.glClearColor(0.165f, 0.192f, 0.231f, 1.0f)
//        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

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
//            mSurfaceDestoryed = true
            return
        }
//        recorder!!.setMainTexture(mainFrameBuffer!!.frameBufferTextureId)
//        recorder!!.frameAvailable()
//        if (mEncoding) {
//            if (timestamp == -1L) {
//                // Seeing this after device is toggled off/on with power button.  The
//                // first frame back has a zero timestamp.
//                //
//                // MPEG4Writer thinks this is cause to abort() in native code, so it's very
//                // important that we just ignore the frame.
//                L.w(TAG, "HEY: got SurfaceTexture with timestamp of zero")
//                return
//            }
////                val transform = FloatArray(16)      // TODO - avoid alloc every frame
//            L.d("tetVideoEncode", "hwVideoEncoder  frameAvailable() ")
//            hwVideoEncoder.frameAvailable()
//        }
        val t4 = System.currentTimeMillis()
        val d3 = t4 - t1

    }

    private fun drawMainTexture(drawLiveBuffer: Boolean) {
        var fbo = mainFrameBuffer

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo!!.framebuffer)

        GLES30.glClearColor(0.176f, 0.184f, 0.2f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
//        sceneRender.setSceneViewPort(VideoSource.srcWidth, VideoSource.srcHeight)
        cameraScene?.drawStreamScene(mWindowSurfaceWidth, mWindowSurfaceHeight, 0, null, false, 0f, 0f, mDisplayProjectionMatrix, true)
        drawExtra()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }



    private fun drawExtra() {
//        if (mShowPip && !mHidePip) {
//            drawPip(pipPreviewSource)
//        }

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
//        var textureId = GlUtil.createTextureObject()
//        var sft = SurfaceTexture(textureId)
//        sft.setOnFrameAvailableListener(this)
        mWindowSurface = WindowSurface(mEglCore!!, surfaceTexture!!)
        mWindowSurface!!.makeCurrent()

        mWindowSurfaceWidth = width
        mWindowSurfaceHeight = height

        GLES30.glViewport(0, 0, mWindowSurfaceWidth, mWindowSurfaceHeight)

        Matrix.orthoM(mDisplayProjectionMatrix, 0, 0f, 1080.toFloat(), 0f, 1920.toFloat(), -1f, 1f)
//        startEncoderThread()
//        recorder?.startThread(mEglCore!!.shareContext())
        handler?.sendMessage(handler?.obtainMessage(MSG_REDRAW, timestamp))

    }


    companion object {
        public val TAG = "RenderThread"
        // Messages
        private val MSG_SURFACE_AVAILABLE = 0
        private val MSG_REDRAW = 3
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


}