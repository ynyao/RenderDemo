package com.zcy.renderdemo.gles

import android.opengl.Matrix

class GLMatrixUtil {

    /**
     * Gets the sprite rotation angle, in degrees.
     */
    /**
     * Sets the sprite rotation angle, in degrees.  Sprite will rotate counter-clockwise.
     */
    // Normalize.  We're not expecting it to be way off, so just iterate.
    var rotation: Float = 0.toFloat()
        set(angle) {
            var angle = angle
            while (angle >= 360.0f) {
                angle -= 360.0f
            }
            while (angle <= -360.0f) {
                angle += 360.0f
            }
            field = angle
            mMatrixReady = false
        }
    /**
     * Returns the sprite scale along the X axis.
     */
    var scaleX: Float = 0.toFloat()
        private set
    /**
     * Returns the sprite scale along the Y axis.
     */
    var scaleY: Float = 0.toFloat()
        private set
    /**
     * Returns the position on the X axis.
     */
    var positionX: Float = 0.toFloat()
        private set
    /**
     * Returns the position on the Y axis.
     */
    var positionY: Float = 0.toFloat()
        private set

    private val mModelViewMatrix: FloatArray
    private var mMatrixReady: Boolean = false

    private val mScratchMatrix = FloatArray(16)

    init {
        mModelViewMatrix = FloatArray(16)
        mMatrixReady = false
    }

    /**
     * Re-computes mModelViewMatrix, based on the current values for rotation, scale, and
     * translation.
     */
    private fun recomputeMatrix() {
        val modelView = mModelViewMatrix

        Matrix.setIdentityM(modelView, 0)
        Matrix.translateM(modelView, 0, positionX, positionY, 0.0f)
        if (rotation != 0.0f) {
            Matrix.rotateM(modelView, 0, rotation, 0.0f, 0.0f, 1.0f)
        }
        Matrix.scaleM(modelView, 0, scaleX, scaleY, 1.0f)
        mMatrixReady = true
    }

    /**
     * Sets the sprite scale (size).
     */
    fun setScale(scaleX: Float, scaleY: Float) {
        this.scaleX = scaleX
        this.scaleY = scaleY
        mMatrixReady = false
    }

    /**
     * Sets the sprite position.
     */
    fun setPosition(posX: Float, posY: Float) {
        positionX = posX
        positionY = posY
        mMatrixReady = false
    }

    /**
     * Returns the model-view matrix.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    private val modelViewMatrix: FloatArray
        get() {
            if (!mMatrixReady) {
                recomputeMatrix()
            }
            return mModelViewMatrix
        }

    fun generateMVPMatrix(projectionMatrix: FloatArray): FloatArray{
        Matrix.multiplyMM(mScratchMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
        return mScratchMatrix
    }

}