package com.zcy.renderdemo.gles

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Created by quqiuzhu on 30/08/2017.
 */

class ScaledDrawable2d (shape: Drawable2d.Prefab) : Drawable2d(shape) {

    private var mTweakedTexCoordArray: FloatBuffer? = null
    private var mScale = 1.0f
    private var mRecalculate: Boolean = false


    init {
        mRecalculate = true
    }

    /**
     * Set the scale factor.
     */
    fun setScale(scale: Float) {
        if (scale < 0.0f || scale > 1.0f) {
            throw RuntimeException("invalid scale " + scale)
        }
        mScale = scale
        mRecalculate = true
    }

    /**
     * Returns the array of texture coordinates.  The first time this is called, we generate
     * a modified version of the array from the parent class.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    override //Log.v(TAG, "Scaling to " + mScale);
            // Texture coordinates range from 0.0 to 1.0, inclusive.  We do a simple scale
            // here, but we could get much fancier if we wanted to (say) zoom in and pan
            // around.
    var texCoordArray: FloatBuffer?
        get() {
            if (mRecalculate) {
                val parentBuf = super.texCoordArray
                val count = parentBuf!!.capacity()

                if (mTweakedTexCoordArray == null) {
                    val bb = ByteBuffer.allocateDirect(count * SIZEOF_FLOAT)
                    bb.order(ByteOrder.nativeOrder())
                    mTweakedTexCoordArray = bb.asFloatBuffer()
                }
                val fb = mTweakedTexCoordArray
                val scale = mScale
                for (i in 0..count - 1) {
                    var fl = parentBuf.get(i)
                    fl = (fl - 0.5f) * scale + 0.5f
                    fb!!.put(i, fl)
                }

                mRecalculate = false
            }

            return mTweakedTexCoordArray
        }
        set(value: FloatBuffer?) {
            super.texCoordArray = value
        }

    companion object {
        private val TAG = "ScaledDrawable2d"

        private val SIZEOF_FLOAT = 4
    }
}