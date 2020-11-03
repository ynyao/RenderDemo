package com.zcy.renderdemo.render

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView

/**
 *  multi-stream surfacetexture callback
 */
class SceneSurfaceRender(private val render: RenderThread) : TextureView.SurfaceTextureListener {

//    private var scene: ShareStreamRender? = null
    private var sceneId: Int = -1
    init {
    }
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        Log.d("SceneSurfaceRender" ,"onSurfaceTextureAvailable: "+surface.toString())
        render.sendSurfaceAvailable(sceneId,surface,width,height)
    }

    fun initSubScene(surface: SurfaceTexture?, width: Int, height: Int) {
        sceneId = render.subSceneRender!!.registerSubScene()
        render.subSceneRender?.sendSurfaceAvailable(surface!!, sceneId, width, height)

    }

    fun addStreamTexture(streamId: Int, rect: Rect?, isDrawOnce: Boolean = false) {
        if (render.subSceneRender != null) {
            render.subSceneRender!!.addStreamTextureInfo(sceneId, /*textureId, */streamId, rect, isDrawOnce)
        }

    }


//    fun removeStreamTexture(streamId: Int) {
//        if (scene != null) {
//            scene!!.removeStreamTextureInfo(sceneId, streamId)
//        }
//    }
//
//    fun pause() {
//        scene?.sendSurfacePause(sceneId)
//    }
//
//    fun resume() {
//        scene?.sendSurfaceResume(sceneId)
//    }


}