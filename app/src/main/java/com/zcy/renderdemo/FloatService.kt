package com.zcy.renderdemo

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import com.zcy.renderdemo.mediator.Director


/**
 * Created by zcy on 2019-08-19.
 */
class FloatService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showFloatingWindow()
        return super.onStartCommand(intent, flags, startId)
    }

    // 新建悬浮窗控件
    var layout :ViewGroup ?= null

    private fun showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            // 获取WindowManager服务
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            layout = LayoutInflater.from(this).inflate(R.layout.layout_floatview, null) as ViewGroup?


            // 设置LayoutParam
            val layoutParams = WindowManager.LayoutParams()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
            }
            layoutParams.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            layoutParams.format = PixelFormat.RGBA_8888
            layoutParams.width = 480
            layoutParams.height = layoutParams.width * 9 / 16
            layoutParams.x = 300
            layoutParams.y = 300

            layout?.setOnTouchListener(FloatingOnTouchListener(layoutParams))
            layout?.addView(Director.instance.getMainView())
            // 将悬浮窗控件添加到WindowManager
            windowManager.addView(layout, layoutParams)
        }
    }

    override fun onDestroy() {
        // 获取WindowManager服务
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.removeView(layout)
        super.onDestroy()
    }


    private inner class FloatingOnTouchListener(var layoutParams: WindowManager.LayoutParams) :
        View.OnTouchListener {
        private var x: Int = 0
        private var y: Int = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (floatingOnGestureListener == null) {
                floatingOnGestureListener = FloatingOnGestureListener(view, layoutParams)
            }
            if (mGestureDetector == null) {
                mGestureDetector = GestureDetector(this@FloatService, floatingOnGestureListener)
            }
            return mGestureDetector!!.onTouchEvent(event)
        }
    }

    var mGestureDetector: GestureDetector? = null
    var floatingOnGestureListener: FloatingOnGestureListener? = null

    inner class FloatingOnGestureListener(
        var view: View?,
        var layoutParams: WindowManager.LayoutParams
    ) : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            Director.instance.pause()
            Director.instance.goSmall(false)
            if(Director.instance.getMainView()?.parent!=null){
                (Director.instance.getMainView()?.parent as ViewGroup).removeView(Director.instance.getMainView())
            }
            var intent = Intent(this@FloatService, MainActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            stopSelf()
            return super.onSingleTapConfirmed(e)
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            return super.onDoubleTap(e)
        }

        var x = 0
        var y = 0

        override fun onDown(e: MotionEvent?): Boolean {
            x = e?.rawX!!.toInt()
            y = e.rawY.toInt()
            return super.onDown(e)
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val movedX = e2.rawX.toInt() - x
            val movedY = e2.rawY.toInt() - y
            x = e2.rawX.toInt()
            y = e2.rawY.toInt()
            val layoutParams = layoutParams
            layoutParams.x = layoutParams.x + movedX
            layoutParams.y = layoutParams.y + movedY
            // 获取WindowManager服务
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            // 更新悬浮窗控件布局
            windowManager.updateViewLayout(view, layoutParams)
            return false
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return false
        }

    }
}