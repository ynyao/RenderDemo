package com.zcy.renderdemo

import android.graphics.Rect
import android.view.View
import com.zcy.renderdemo.mediator.Director
import com.zcy.renderdemo.render.Colleague
import com.zcy.renderdemo.render.SceneSurfaceRender
import kotlinx.android.synthetic.main.activity_main.view.*


/**
 * Navigation Area ,Added Stream ,Switching Functions
 */
class NaviController(private val naviLayout: View, private val director : Director):Colleague(director){
    var sceneRender: SceneSurfaceRender?=null

    init {
        if(director.renderThread?.sceneSurfaceRender!=null) {
            sceneRender = director.renderThread?.sceneSurfaceRender
            naviLayout.direct_textureView.surfaceTextureListener = sceneRender
            naviLayout.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                val lp = naviLayout.direct_textureView.layoutParams
                val w = right - left
                val h = bottom - top
                if (w != lp.width || h != lp.height) {
                    lp.width = w
                    lp.height = h
                    naviLayout.direct_textureView.layoutParams = lp
                }
            }
            naviLayout.requestLayout()
        }
    }

    fun addCameraStream(){
        sceneRender?.addStreamTexture(0, genDirectRect(naviLayout.ttv_preview))
    }

    fun addLocalStream(){
        sceneRender?.addStreamTexture(1, genDirectRect(naviLayout.local_video_preview))
    }


    private fun genDirectRect(v: View): Rect {
        val p = v.parent as View
        val vl = 0//v.left + p.left
        val vt = 0//v.top + p.top
        return generateDirectRect(v, vl, vt)
    }

    /**
     *  Generate the Rect of the Views in the window
     */
    private fun generateDirectRect(v: View, l: Int, t: Int): Rect {
        val rect =Rect(v.left,v.top,v.right,v.bottom)
        rect.left += l
        rect.top += t
        rect.right += l
        rect.bottom += t
        return rect
    }


    init{

    }
}