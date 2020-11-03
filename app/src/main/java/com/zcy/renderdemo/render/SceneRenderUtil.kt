package com.zcy.renderdemo.render

import android.opengl.GLES20
import android.opengl.Matrix
import com.zcy.renderdemo.gles.*

class SceneRenderUtil {

    private var viewWidth = RenderThread.mWindowSurfaceWidth
    private var viewHeight = RenderThread.mWindowSurfaceHeight

    private val mainDrawable =
        ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE)
    private val mainRect = Sprite2d(mainDrawable)

    private val cropDrawable =
        ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE_CROP)
    private val cropRect = Sprite2d(cropDrawable)

    private var mFullScreen: FullFrameRect? = null

    private var mTexProgram: Texture2dProgram? = null
    private var greenProgramExt: Texture2dProgram? = null

    private var mTexProgramDeint: Texture2dProgram? = null

    private val glMatrixUtil = GLMatrixUtil()

    fun setSceneViewPort(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, viewWidth, viewHeight)
    }

    fun initGL() {
        mTexProgram =
                Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT)
        mTexProgramDeint =
                Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT_BOB)
        greenProgramExt =
                Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_GREENSCREEN_EXT)
        mFullScreen = FullFrameRect(mTexProgram!!)
    }

    fun releaseGL() {
        mTexProgram?.release()
        mTexProgram = null
        mTexProgramDeint?.release()
        mTexProgramDeint = null
        greenProgramExt?.release()
        greenProgramExt = null
        mFullScreen?.release(false)
    }

    fun drawStreamScene(textureId: Int, drawType: Int, w: Int = 0, h: Int = 0, rect: RenderRect?, greenFilter: Boolean, interlace: Boolean = false, smooth: Float, similarity: Float, projection: FloatArray) {
        val renderItem = RendererItem(textureId, rect, greenFilter, interlace, smooth, similarity, projection)
        if (rect == null) {
            renderItem.rect = RenderRect(
                w.toFloat(),
                h.toFloat(),
                (w / 2).toFloat(),
                (h / 2).toFloat()
            )
        }

        renderItem.width = w
        renderItem.height = h
        if (drawType == RENDERER_TYPE_PDF) {
            drawPdf(renderItem)
        } else {
            val sprite: Sprite2d
            /*if(interlace){
                sprite = fullRect
            }else */if(drawType == RENDERER_TYPE_480){
                sprite = cropRect
            }else{
                sprite = mainRect
            }
            drawSceneItem(sprite, renderItem)
        }
    }

    private fun drawSceneItem(sprite: Sprite2d, scene: RendererItem){
        GlUtil.checkGlError("scene draw start")
        sprite.rotation = 90f
        sprite.setTexture(scene.textureId)
        sprite.setScale(scene.rect!!.h, scene.rect!!.w)
        sprite.setPosition(scene.rect!!.cx, scene.rect!!.cy)


        val program: Texture2dProgram
        if (!scene.interlace && scene.greenFilter) {
            program = /*if(scene.interlace) greenProgram!! else*/ greenProgramExt!!
            program.setUseBlending(false)
            program.setKeyColorSmoothness(scene.greenSmooth)
            program.setKeyColorSimilarity(scene.greenSimilarity)
        } else {
            program = if(scene.interlace) mTexProgramDeint!! else mTexProgram!!
        }
        draw(sprite, program, scene.projection)
    }

    private fun draw(rect: Sprite2d, program: Texture2dProgram, projection: FloatArray) {
        rect.draw(program, projection)
    }

    private fun computeItemSceleWidthAndHeifght(pw: Int, ph: Int, sw: Int, sh: Int): FloatArray?{
        if(pw == 0 || ph == 0 || sw == 0 || sh == 0){
            return null
        }

        val w: Float
        val h: Float
        if ((pw.toFloat() / ph.toFloat()) > (sw.toFloat() / sh.toFloat())) {
            w = sw.toFloat()
            val scale = sw.toFloat() / pw.toFloat()
            h = ph.toFloat() * scale
        } else {
            h = sh.toFloat()
            val scale = sh.toFloat() / ph.toFloat()
            w = pw.toFloat() * scale
        }

        val ret = FloatArray(2)
        ret[0] = w
        ret[1] = h
        return ret
    }

    private fun drawPdf(scene: RendererItem) {
        mFullScreen?.program = mTexProgram
        val pw = scene.width
        val ph = scene.height
        val sw = scene.rect!!.w.toInt()
        val sh = scene.rect!!.h.toInt()

        val scale = computeItemSceleWidthAndHeifght(pw, ph, sw, sh)
        scale?: return

        val w = scale[0]
        val h = scale[1]

        glMatrixUtil.setPosition(scene.rect!!.cx, scene.rect!!.cy)
        glMatrixUtil.setScale(w / 2, h / 2)
        val matrix = glMatrixUtil.generateMVPMatrix(scene.projection)
        Matrix.scaleM(matrix, 0, 1f, -1f, 0f)

        mFullScreen?.drawFrame(scene.textureId, matrix, GlUtil.IDENTITY_MATRIX)
    }

    open class RenderRect(val w: Float, val h: Float, val cx: Float, val cy: Float)

    inner class RendererItem(val textureId: Int, var rect: RenderRect?, val greenFilter: Boolean, val interlace: Boolean = false, val greenSmooth: Float, val greenSimilarity: Float, val projection: FloatArray){
        var width: Int = 0
        var height: Int = 0
    }

    companion object {
        val RENDERER_TYPE_PDF = 0
        val RENDERER_TYPE_576 = 1
        val RENDERER_TYPE_480 = 2
        val RENDERER_TYPE_NORMAL = 3
    }


}