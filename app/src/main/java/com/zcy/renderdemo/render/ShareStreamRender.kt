package com.zcy.renderdemo.render

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Looper
import android.util.Log
import com.zcy.renderdemo.gles.EglCore
import com.zcy.renderdemo.gles.GlUtil
import com.zcy.renderdemo.gles.WindowSurface
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

/**
 * 初始化设置第二显示区域画面的状态
 * @property sceneRender CameraScene
 */
class ShareStreamRender(private val renderThread: RenderThread) :
    Runnable /*(private var shareContext: EGLContext)*//* : HandlerThread("ShareStreamRender")*/ {
    override fun run() {
    }

    private var mEglCore: EglCore? = null

    private val subRenderScenes = HashMap<Int, SubRenderScene>()
    private val subRenderSceneStatus = ConcurrentHashMap<Int, SubRenderSceneInfo>()
    private var subSceneId = -1

    private var shareContext: EGLContext? = null

    @Volatile
    private var mSurfaceDestoryed = false
    private val LOG_DEBUGGER = true

    private fun log(log: String) {
        if (LOG_DEBUGGER) {
            Log.d("SUB_RENDERER", log)
        }
    }

    init {

    }


    fun prepareLooper(looper: Looper, eglCore: EglCore) {
        mEglCore = eglCore
    }

    /**
     * 注册副显示区域 可以有多个副显示区域 投屏 ，副显示区 ，选择区等
     * @return Int
     */
    fun registerSubScene(): Int {
        subSceneId++
        val subScene = SubRenderScene(subSceneId)
        val subSceneStatus = SubRenderSceneInfo(subSceneId)
        subRenderSceneStatus[subSceneId] = subSceneStatus
        subRenderScenes[subScene.id] = subScene
        return subSceneId
    }

    fun setSceneSpecial(subSceneId: Int) {
        subRenderSceneStatus[subSceneId] ?: return
    }

    fun unregisterSubScene(subSceneId: Int) {
        subRenderSceneStatus.remove(subSceneId)
    }

    /**
     *
     * 初始化副显示区域
     * @param info SurfaceInfo
     * @param sceneId Int
     */
    private fun initSubScene(info: SurfaceInfo, sceneId: Int) {
        val subScene = subRenderScenes[sceneId]
        subScene ?: return
        log("initSubScene    w : ${info.width}  h   :    ${info.height}  ")
        subScene.surfaceInfo = info
        if (info.surface != null) {
            subScene.windowSurface = WindowSurface(mEglCore!!, info.surface!!)
            Matrix.orthoM(
                subScene.projectionMatrix,
                0,
                0f,
                subScene.windowSurface!!.width.toFloat(),
                0f,
                subScene.windowSurface!!.height.toFloat(),
                -1f,
                1f
            )
        }
    }

    private fun surfaceTextureAvailable(info: SurfaceInfo, sceneId: Int) {
        initSubScene(info, sceneId)
//        handler?.sendMessage(handler?.obtainMessage(MSG_DRAW))
    }

    private fun surfaceTextureChange(info: SurfaceInfo, sceneId: Int) {
        val subScene = subRenderScenes[sceneId]
        subScene ?: return
        log("surfaceTextureChange    w : ${info.width}  h   :    ${info.height}  ")

        subScene.surfaceInfo.width = info.width
        subScene.surfaceInfo.height = info.height
    }


    private fun release() {
        mSurfaceDestoryed = true
        subRenderScenes.forEach { _, scene ->
            scene.windowSurface?.release()
        }

        subRenderScenes.clear()
        subRenderSceneStatus.clear()
    }


    //    private var startTime = 0L
    fun drawSubScene() {
        if (mSurfaceDestoryed) {
            return
        }

        subRenderScenes.forEach { _, scene ->
            val sceneInfo = subRenderSceneStatus[scene.id]
            if (sceneInfo != null) {
                if (!sceneInfo.surfaceDestoryed &&
                    scene.windowSurface != null &&
                    !sceneInfo.pause
                ) {
                    scene.windowSurface?.makeCurrent()

                    val surfaceInfo = scene.surfaceInfo
                    //                    GLES30.glViewport(0, 0, scene.width, scene.height)
                    GLES30.glClearColor(0.176f, 0.184f, 0.2f, 1.0f)
                    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
//                    sceneRender.setSceneViewPort(scene.width, scene.height)
                    scene.scenes.forEach {
                        //                        if(it.textureId != -1){
                        val filter = it.filter
                        if (it.rect != null) {
                            val cx = it.rect.centerX().toFloat()
                            val cy = surfaceInfo.height - it.rect.centerY().toFloat()
                            val w = it.rect.width().toFloat()
                            val h = it.rect.height().toFloat()
                            val r = SceneRenderUtil.RenderRect(w, h, cx, cy)
                            renderThread?.cameraScene?.drawStreamScene(
                                surfaceInfo.width,
                                surfaceInfo.height,
                                it.streamId,
                                r,
                                filter.useGreenFilter,
                                filter.smooth,
                                filter.similarity,
                                scene.projectionMatrix
                            )
                            //                                mTextureRender!!.setRegin(cx, cy, w, h)
                            //                                mTextureRender!!.drawTexture(it.textureId, scene.projectionMatrix)
                        } else {
                            renderThread?.cameraScene?.drawStreamScene(
                                surfaceInfo.width,
                                surfaceInfo.height,
                                it.streamId,
                                null,
                                filter.useGreenFilter,
                                filter.smooth,
                                filter.similarity,
                                scene.projectionMatrix
                            )
                            //                                mTextureRender!!.drawTexture(it.textureId)
                        }
                    }
                    if (sceneInfo.isDrawOnce) {
                        sceneInfo.pause = true
                    }
                    val swap = scene.windowSurface!!.swapBuffers()

                    if (!swap) {
                        Log.d(TAG, "share scene render swap buffer failed id")
                        GlUtil.checkGlError("scene render swap buffer failed")
                        sceneInfo.surfaceDestoryed = true
                    }

                }
            }
        }


    }


    /**
     *
     * 通知 副显示区surface可用
     * @param surface SurfaceTexture
     * @param sceneId Int
     * @param width Int
     * @param height Int
     */
    fun sendSurfaceAvailable(surface: SurfaceTexture, sceneId: Int, width: Int, height: Int) {
        subRenderSceneStatus[sceneId] ?: return
        val info = SurfaceInfo()
        info.surface = surface
        info.width = width
        info.height = height
        surfaceTextureAvailable(info,sceneId)
        log("sendSurfaceAvailable    w : ${info.width}  h   :    ${info.height}  ")
    }

    fun sendSurfacePause(sceneId: Int) {
        subRenderSceneStatus[sceneId] ?: return
        subRenderSceneStatus[sceneId]!!.pause = true
    }

    fun sendSurfaceResume(sceneId: Int) {
        subRenderSceneStatus[sceneId] ?: return
        subRenderSceneStatus[sceneId]!!.pause = false
    }

    fun sendSurfaceChanged(sceneId: Int, width: Int, height: Int) {
        subRenderSceneStatus[sceneId] ?: return
        val info = SurfaceInfo()
        info.width = width
        info.height = height
        log("sendSurfaceChanged    w : ${info.width}  h   :    ${info.height}  ")
    }

    fun sendSurfaceDestroyed(sceneId: Int) {
        subRenderSceneStatus[sceneId] ?: return
        subRenderSceneStatus[sceneId]?.surfaceDestoryed = true
    }


    private fun addStreamTexture(sceneId: Int, streamId: Int, rect: Rect?) {
        subRenderScenes[sceneId] ?: return
        removeStreamTexture(sceneId, streamId)
        val info = StreamTextureInfo(streamId, rect)
        subRenderScenes[sceneId]!!.scenes.add(info)
    }

    fun addStreamTextureInfo(sceneId: Int, streamId: Int, rect: Rect?, isDrawOnce: Boolean = false) {
        subRenderSceneStatus[sceneId] ?: return
        subRenderSceneStatus[sceneId]?.isDrawOnce = isDrawOnce
        addStreamTexture(sceneId,streamId,rect)
    }

    fun removeStreamTextureInfo(sceneId: Int, streamId: Int) {
        subRenderSceneStatus[sceneId] ?: return
    }

    private fun removeStreamTexture(sceneId: Int, streamId: Int) {
        subRenderScenes[sceneId] ?: return
        val find = subRenderScenes[sceneId]!!.scenes.filter {
            it.streamId == streamId
        }

        if (find.isNotEmpty()) {
            subRenderScenes[sceneId]!!.scenes.removeAll(find)
        }
    }


    companion object {
        val TAG = "SceneRender"

    }


    inner class SubRenderScene(val id: Int) {
        var windowSurface: WindowSurface? = null
        var surfaceInfo = SurfaceInfo()

        val projectionMatrix = FloatArray(16)
        val scenes = ArrayList<StreamTextureInfo>()
    }

    inner class SurfaceInfo {
        var surface: SurfaceTexture? = null
        var width: Int = 0
        var height: Int = 0
    }

    inner class SubRenderSceneInfo(val id: Int) {
        var pause: Boolean = false
        @Volatile
        var surfaceDestoryed: Boolean = false
        var isDrawOnce = false
    }

    class StreamTextureInfo(/*val textureId: Int,*/
        val streamId: Int,
        val rect: Rect?
    ) {
        var filter = StreamTextureFilter()
        var isDrawOnce = false
    }

    class StreamTextureFilter {
        var useGreenFilter = false
        var smooth: Float = 0f
        var similarity: Float = 0f
    }


}