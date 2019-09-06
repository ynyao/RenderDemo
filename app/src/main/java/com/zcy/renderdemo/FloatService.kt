package com.zcy.renderdemo

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*


/**
 * Created by zcy on 2019-08-19.
 */
class FloatService :Service() {

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showFloatingWindow()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            // 获取WindowManager服务
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // 新建悬浮窗控件
            var layout=LayoutInflater.from(this).inflate(R.layout.layout_floatview,null)

            // 设置LayoutParam
            val layoutParams = WindowManager.LayoutParams()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
            }
            layoutParams.format = PixelFormat.RGBA_8888
            val dm = resources.displayMetrics
            layoutParams.width = dm.widthPixels
            layoutParams.height = layoutParams.width *9/16
            layoutParams.x = 300
            layoutParams.y = 300

            layout.setOnTouchListener(FloatingOnTouchListener(layoutParams))
            // 将悬浮窗控件添加到WindowManager
            windowManager.addView(layout, layoutParams)
        }
    }


    private inner class FloatingOnTouchListener(var layoutParams : WindowManager.LayoutParams) : View.OnTouchListener {
        private var x: Int = 0
        private var y: Int = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.rawX.toInt()
                    y = event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val nowX = event.rawX.toInt()
                    val nowY = event.rawY.toInt()
                    val movedX = nowX - x
                    val movedY = nowY - y
                    x = nowX
                    y = nowY
                    val layoutParams = layoutParams
                    layoutParams.x = layoutParams.x + movedX
                    layoutParams.y = layoutParams.y + movedY
                    // 获取WindowManager服务
                    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    // 更新悬浮窗控件布局
                    windowManager.updateViewLayout(view, layoutParams)
                }
                else -> {
                }
            }
            return false
        }
    }

    private inner class FloatingOnGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return super.onSingleTapUp(e)
        }



    }
}