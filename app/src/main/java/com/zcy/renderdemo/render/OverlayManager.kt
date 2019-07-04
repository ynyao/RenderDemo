package com.zcy.renderdemo.render

import android.graphics.BitmapFactory
import com.zcy.renderdemo.App
import com.zcy.renderdemo.R
import com.zcy.renderdemo.gles.*

class OverlayManager(private var width: Int, private var height: Int)  {


    inner class Overlay {
        var cx: Float = 0f
        var cy: Float = 0f
        var w: Float = 0f
        var h: Float = 0f
        var textureId: Int = -1
        var id: Long = 0
        var type = -1
        var isSingle: Boolean = true
        var srccx: Float = 0f
        var srccy: Float = 0f
        var width = 0f
        var height = 0f
        var state = 0
    }
    private var stickerId = 0L
    private val overlays = ArrayList<Overlay>()
    init {

        for(i in 0 until 3) {
            val overlay = Overlay()
            var bmp=BitmapFactory.decodeResource(App.getContext().resources,R.mipmap.ic_launcher)
            overlay.textureId = GlUtil.createImageTexture(bmp)
            overlay.cx = 500f+i*40
            overlay.cy = 500f+i*40
            overlay.w = bmp.width.toFloat()
            overlay.h = bmp.height.toFloat()
            overlay.id = i.toLong()
//            overlay.type = type
            GlUtil.checkGlError("add overlay")
            overlays.add(overlay)
        }

    }
    private val overlayRender =
        Sprite2d(ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE))

    private var program2D =
        Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D)

    fun drawOverlay( matrix: FloatArray){
//        Log.d("OVERLAY", "draw overlay texture id : " + overlay.textureId+"overlay.cx: "+overlay.cx+ "overlay.cy" +overlay.cy
//        +"overlay.w:"+overlay.w+"overlay.h:"+overlay.h)
        for(i in 0 until overlays.size) {
            overlayRender.setTexture(overlays[i].textureId)
            overlayRender.setPosition((overlays[i].cx+10*(1+Math.random()*10)).toFloat(),
                (overlays[i].cy+10*(1+Math.random()*10)).toFloat()
            )
            overlayRender.setScale(overlays[i].w, overlays[i].h)
            overlayRender.draw(program2D, matrix)
        }
    }
}