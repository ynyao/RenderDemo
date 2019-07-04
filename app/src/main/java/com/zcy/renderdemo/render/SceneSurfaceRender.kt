package com.zcy.renderdemo.render

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.view.TextureView

/**
 *包括多路流的surfacetexture的回调
 */
class SceneSurfaceRender(private val render: RenderThread) : TextureView.SurfaceTextureListener {

//    private var scene: ShareStreamRender? = null
    private var sceneId: Int = -1
    private val delayStreamTextures = ArrayList<ShareStreamRender.StreamTextureInfo>()
    init {
//        this.scene=scene
    }
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
//        scene?.sendSurfaceChanged(sceneId, width, height)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
//        scene?.sendSurfaceDestroyed(sceneId)
//        scene?.unregisterSubScene(sceneId)
//        scene = null
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        render.sendSurfaceAvailable(sceneId,surface,width,height)
    }

    fun initSubScene(surface: SurfaceTexture?, width: Int, height: Int) {
        sceneId = render.subSceneRender!!.registerSubScene()
//        render.subSceneRender!!.setSceneSpecial(sceneId)
//        delayStreamTextures.forEach {
//            render.subSceneRender?.addStreamTextureInfo(sceneId, /*it.textureId, */it.streamId, it.rect, it.isDrawOnce)
//        }
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