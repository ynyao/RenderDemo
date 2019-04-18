/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zcy.renderdemo.gles
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import com.zcy.renderdemo.utils.L
import java.nio.FloatBuffer

/**
 * GL program and supporting functions for textured 2D shapes.
 */
class Texture2dProgram
/**
 * Prepares the program in the current EGL context.
 */
(
        /**
         * Returns the program type.
         */
        val programType: ProgramType) {

    enum class ProgramType {
        TEXTURE_2D,
        TEXTURE_DINT_YADIF,
        TEXTURE_EXT,
        TEXTURE_EXT_BOB,
        TEXTURE_EXT_BLEND,
        TEXTURE_EXT_BW,
        TEXTURE_EXT_FILT,
        TEXTURE_GREENSCREEN_EXT,
        TEXTURE_GREENSCREEN
    }

    // Handles to the GL program and various components of it.
    private var mProgramHandle: Int = 0
    private val muMVPMatrixLoc: Int
    private val muTexMatrixLoc: Int
    private var muKernelLoc: Int = 0
    private var muTexOffsetLoc: Int = 0
    private var muColorAdjustLoc: Int = 0
    private val maPositionLoc: Int
    private val maTextureCoordLoc: Int

    private var mTextureTarget: Int = 0

    private var useBlending = true
    private val mKernel = FloatArray(KERNEL_SIZE)
    private var mTexOffset: FloatArray? = null
    private var mColorAdjust: Float = 0.toFloat()

    private var mKeyUseGreenFilter: Int = 0
    private var mKeyColorSmoothness:Float = 61.0f
    private var mKeyColorSimilarity:Float = 500.0f

    private var mKeyUseGreenFilterLoc: Int = -1
    private var mKeyColorSmoothnessLoc:Int = -1
    private var mKeyColorSimilarityLoc:Int = -1

    private var mChromaKey:FloatArray? = FloatArray(4)
    private var mChromaKeyLoc:Int= -1

    private var mPixelSize:FloatArray? = FloatArray(2)
    private var mPixelSizeLoc:Int= -1

    private var mChromaSpill:Float = 100.0f
    private var mChromaSpillLoc:Int= -1

    private var mOddLoc: Int = -1
    private var odd: Int = -1

    private var previewFrameLoc = -1
    private var mPreviewFrame = -1
//    private var fieldorderLoc = -1
//    private var mFieldOrder = -1
    private var textureLoc = -1

    init {

        when (programType) {
            Texture2dProgram.ProgramType.TEXTURE_2D -> {
                mTextureTarget = GLES30.GL_TEXTURE_2D
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D)
            }
            Texture2dProgram.ProgramType.TEXTURE_EXT -> {
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT)
            }
            Texture2dProgram.ProgramType.TEXTURE_EXT_BOB -> {
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_I2P_BOB)
            }
            Texture2dProgram.ProgramType.TEXTURE_EXT_BW -> {
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BW)
            }
            Texture2dProgram.ProgramType.TEXTURE_EXT_FILT -> {
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILT)
            }

            Texture2dProgram.ProgramType.TEXTURE_GREENSCREEN -> {
                mTextureTarget = GLES30.GL_TEXTURE_2D
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_GREEN_SCREEN)
            }

            Texture2dProgram.ProgramType.TEXTURE_GREENSCREEN_EXT -> {
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_GREEN_SCREEN_EXT)
            }

            Texture2dProgram.ProgramType.TEXTURE_EXT_BLEND -> {
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_I2P_BLEND)
            }

            Texture2dProgram.ProgramType.TEXTURE_DINT_YADIF -> {
                mTextureTarget = GLES30.GL_TEXTURE_2D
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_OBS_DINT_YADIF)
            }
            else -> throw RuntimeException("Unhandled type " + programType)
        }
        if (mProgramHandle == 0) {
            throw RuntimeException("Unable to create program")
        }
        L.d("Created program $mProgramHandle ($programType)")

        // get locations of attributes and uniforms

        maPositionLoc = GLES30.glGetAttribLocation(mProgramHandle, "aPosition")
        GlUtil.checkLocation(maPositionLoc, "aPosition")
        maTextureCoordLoc = GLES30.glGetAttribLocation(mProgramHandle, "aTextureCoord")
        GlUtil.checkLocation(maTextureCoordLoc, "aTextureCoord")
        muMVPMatrixLoc = GLES30.glGetUniformLocation(mProgramHandle, "uMVPMatrix")
        GlUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix")
        muTexMatrixLoc = GLES30.glGetUniformLocation(mProgramHandle, "uTexMatrix")
        GlUtil.checkLocation(muTexMatrixLoc, "uTexMatrix")
        muKernelLoc = GLES30.glGetUniformLocation(mProgramHandle, "uKernel")
        if (muKernelLoc < 0) {
            // no kernel in this one
            muKernelLoc = -1
            muTexOffsetLoc = -1
            muColorAdjustLoc = -1
        } else {
            // has kernel, must also have tex offset and color adj
            muTexOffsetLoc = GLES30.glGetUniformLocation(mProgramHandle, "uTexOffset")
            GlUtil.checkLocation(muTexOffsetLoc, "uTexOffset")
            muColorAdjustLoc = GLES30.glGetUniformLocation(mProgramHandle, "uColorAdjust")
            GlUtil.checkLocation(muColorAdjustLoc, "uColorAdjust")

            // initialize default values
            setKernel(floatArrayOf(0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f), 0f)
            setTexSize(256, 256)
        }

        mKeyColorSimilarityLoc = GLES20.glGetUniformLocation(mProgramHandle, "uSimilarity")
        if(mKeyColorSimilarityLoc >= 0) {
            mKeyColorSmoothnessLoc = GLES20.glGetUniformLocation(mProgramHandle, "uSmoothness")
            GlUtil.checkLocation(mKeyColorSmoothnessLoc, "uSmoothness")

            mChromaKeyLoc = GLES20.glGetUniformLocation(mProgramHandle, "uChromaKey")
            mPixelSizeLoc = GLES20.glGetUniformLocation(mProgramHandle, "uPixelSize")
            mChromaSpillLoc = GLES20.glGetUniformLocation(mProgramHandle, "uSpill")
        }
        mOddLoc = GLES20.glGetUniformLocation(mProgramHandle, "odd")
//        fieldorderLoc = GLES20.glGetUniformLocation(mProgramHandle, "field_order")
        previewFrameLoc = GLES20.glGetUniformLocation(mProgramHandle, "previous_texture")
        if(previewFrameLoc >= 0){
            textureLoc = GLES20.glGetUniformLocation(mProgramHandle, "sTexture")
            mPixelSizeLoc = GLES20.glGetUniformLocation(mProgramHandle, "uPixelSize")
        }

    }

    /**
     * Releases the program.
     *
     *
     * The appropriate EGL context must be current (i.e. the one that was used to create
     * the program).
     */
    fun release() {
        L.d("deleting program " + mProgramHandle)
        GLES30.glDeleteProgram(mProgramHandle)
        mProgramHandle = -1
    }

    /**
     * Configures the convolution filter values.
     *
     * @param values Normalized filter values; must be KERNEL_SIZE elements.
     */
    fun setKernel(values: FloatArray, colorAdj: Float) {
        if (values.size != KERNEL_SIZE) {
            throw IllegalArgumentException("Kernel size is " + values.size +
                    " vs. " + KERNEL_SIZE)
        }
        System.arraycopy(values, 0, mKernel, 0, KERNEL_SIZE)
        mColorAdjust = colorAdj
        //Log.d(TAG, "filt kernel: " + Arrays.toString(mKernel) + ", adj=" + colorAdj);
    }

    /**
     * Sets the size of the texture.  This is used to find adjacent texels when filtering.
     */
    fun setTexSize(width: Int, height: Int) {
        val rw = 1.0f / width
        val rh = 1.0f / height

        // Don't need to create a new array here, but it's syntactically convenient.
        mTexOffset = floatArrayOf(-rw, -rh, 0f, -rh, rw, -rh, -rw, 0f, 0f, 0f, rw, 0f, -rw, rh, 0f, rh, rw, rh)
        //Log.d(TAG, "filt size: " + width + "x" + height + ": " + Arrays.toString(mTexOffset));
    }

    fun setUseBlending(blending: Boolean) {
        useBlending = blending
    }

    fun setKeyColorSmoothness(smoothness: Float) {
        if(smoothness > 0) {
            mKeyColorSmoothness = smoothness
        }
    }

    fun setKeyColorSimilarity(similarity: Float) {
        if(similarity > 0) {
            mKeyColorSimilarity = similarity
        }
    }

    fun setChromaSpill(spill: Float) {
        mChromaSpill = spill
    }

    fun setChromaKey(key: FloatArray) {
        mChromaKey = key
    }

    fun setPixelSize(size: FloatArray) {
        mPixelSize = size
    }

//    fun setFieldOrder(fieldOrder: Int){
//        mFieldOrder = fieldOrder
//    }

    fun setPreviewFrame(previewFrame: Int){
        mPreviewFrame = previewFrame
    }

    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix The 4x4 projection matrix.
     * @param vertexBuffer Buffer with vertex position data.
     * @param firstVertex Index of first vertex to use in vertexBuffer.
     * @param vertexCount Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride Width, in bytes, of the position data for each vertex (often
     * vertexCount * sizeof(float)).
     * @param texMatrix A 4x4 transformation matrix for texture coords.  (Primarily intended
     * for use with SurfaceTexture.)
     * @param texBuffer Buffer with vertex texture data.
     * @param texStride Width, in bytes, of the texture data for each vertex.
     */
    fun draw(mvpMatrix: FloatArray, vertexBuffer: FloatBuffer, firstVertex: Int,
             vertexCount: Int, coordsPerVertex: Int, vertexStride: Int,
             texMatrix: FloatArray, texBuffer: FloatBuffer, textureId: Int, texStride: Int) {
        GlUtil.checkGlError("draw start")

        if(useBlending){
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        }else{
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        }
//        GLES30.glDisable(GLES30.GL_BLEND)


        // Select the program.
        GLES30.glUseProgram(mProgramHandle)
        GlUtil.checkGlError("glUseProgram")

        // Set the texture.
        if(previewFrameLoc >= 0){
            GLES20.glUniform1i(textureLoc, 0)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(mTextureTarget, textureId)
            GlUtil.checkGlError("bind textureLoc")

            GLES20.glUniform1i(previewFrameLoc, 1)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(mTextureTarget, mPreviewFrame)
            GlUtil.checkGlError("bind previewFrameLoc  glBindTexture")

            setPixelSize(floatArrayOf(1.0f / 1920, 1.0f / 1080))
            GLES20.glUniform2fv(mPixelSizeLoc, 1, mPixelSize, 0)
        }else{
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(mTextureTarget, textureId)
        }

        // Copy the model / view / projection matrix over.
        GLES30.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GlUtil.checkGlError("glUniformMatrix4fv")

        // Copy the texture transformation matrix over.
        GLES30.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0)
        GlUtil.checkGlError("glUniformMatrix4fv")

        // Enable the "aPosition" vertex attribute.
        GLES30.glEnableVertexAttribArray(maPositionLoc)
        GlUtil.checkGlError("glEnableVertexAttribArray")

        // Connect vertexBuffer to "aPosition".
        GLES30.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
                GLES30.GL_FLOAT, false, vertexStride, vertexBuffer)
        GlUtil.checkGlError("glVertexAttribPointer")

        // Enable the "aTextureCoord" vertex attribute.
        GLES30.glEnableVertexAttribArray(maTextureCoordLoc)
        GlUtil.checkGlError("glEnableVertexAttribArray")

        // Connect texBuffer to "aTextureCoord".
        GLES30.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES30.GL_FLOAT, false, texStride, texBuffer)
        GlUtil.checkGlError("glVertexAttribPointer")

        // Populate the convolution kernel, if present.
        if (muKernelLoc >= 0) {
            GLES30.glUniform1fv(muKernelLoc, KERNEL_SIZE, mKernel, 0)
            GLES30.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0)
            GLES30.glUniform1f(muColorAdjustLoc, mColorAdjust)
        }

        if (mKeyColorSmoothnessLoc >= 0 && mKeyColorSmoothness > 0 && mKeyColorSimilarity > 0) {
            GLES20.glUniform1f(mKeyColorSmoothnessLoc, mKeyColorSmoothness / 1000)
            GLES20.glUniform1f(mKeyColorSimilarityLoc, mKeyColorSimilarity / 1000)
        }

        if (mPixelSizeLoc >= 0 && mChromaSpillLoc >= 0) {
            setPixelSize(floatArrayOf(1.0f / 1920, 1.0f / 1080))
            setChromaKey((floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)))

            GLES20.glUniform4fv(mChromaKeyLoc, 1, mChromaKey, 0)
            GLES20.glUniform2fv(mPixelSizeLoc, 1, mPixelSize, 0)
            GLES20.glUniform1f(mChromaSpillLoc, mChromaSpill / 1000)
            GlUtil.checkGlError("glUniform2fv")
        }

        if(mOddLoc >= 0){
            if(odd == -1 || odd == 0){
                odd = 1
            }else if(odd == 1){
                odd = 0
            }
            GLES20.glUniform1i(mOddLoc, odd)
        }

//        if(fieldorderLoc >= 0){
//            GLES20.glUniform1i(fieldorderLoc, odd)
//
//        }

        // Draw the rect.
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, firstVertex, vertexCount)
        GlUtil.checkGlError("glDrawArrays")

        // Done -- disable vertex array, texture, and program.
        GLES30.glDisableVertexAttribArray(maPositionLoc)
        GLES30.glDisableVertexAttribArray(maTextureCoordLoc)
        GLES30.glDisable(GLES30.GL_BLEND)

        GLES30.glBindTexture(mTextureTarget, 0)
        GLES30.glUseProgram(0)
    }

    companion object {
        private val TAG = GlUtil.TAG

        // Simple vertex shader, used for all programs.
        private val VERTEX_SHADER =
                "uniform mat4 uMVPMatrix;\n" +
                        "uniform mat4 uTexMatrix;\n" +
                        "attribute vec4 aPosition;\n" +
                        "attribute vec4 aTextureCoord;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "void main() {\n" +
                        "    gl_Position = uMVPMatrix * aPosition;\n" +
                        "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                        "}\n"

        // Simple fragment shader for use with "normal" 2D textures.
        private val FRAGMENT_SHADER_2D =
                "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform sampler2D sTexture;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                        "}\n"

        private val FRAGMENT_SHADER_EXT_I2P_BOB =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "    float isodd = mod(vTextureCoord.y, 2.0);\n" +
                        "    vec4 result;\n" +
                        "    float unit = 1.0/1080.0;\n" +
                        "    if(isodd < 0.0){\n" +
                        "       vec2 srcCoord = vec2(vTextureCoord.x, (vTextureCoord.y + unit)/2.0);\n" +
                        "       vec4 evenfield = texture2D(sTexture, vec2(srcCoord.x, srcCoord.y + unit));\n" +
                        "       vec4 oddfield = texture2D(sTexture, srcCoord);\n" +
                        "       result = mix(evenfield, oddfield, 0.5);\n" +
                        "    }else{\n" +
                        "       vec2 srcCoord = vec2(vTextureCoord.x, vTextureCoord.y/2.0);\n" +
                        "       vec4 evenfield = texture2D(sTexture, srcCoord);\n" +
                        "       vec4 oddfield = texture2D(sTexture, vec2(srcCoord.x, srcCoord.y - unit));\n" +
                        "       result = mix(evenfield, oddfield, 0.5);\n" +
                        "    }\n" +
                        "    gl_FragColor = result;\n" +
                        "}\n"

        private val FRAGMENT_SHADER_EXT_I2P_BLEND =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "    float isodd = mod(vTextureCoord.y, 2.0);\n" +
                        "    vec4 result;\n" +
                        "    float unit = 1.0/1080.0;\n" +
                        "    if(isodd > 0.0){\n" +
                        "       vec4 evenfield = texture2D(sTexture, vec2(vTextureCoord.x, vTextureCoord.y + unit));\n" +
                        "       vec4 oddfield = texture2D(sTexture, vTextureCoord);\n" +
                        "       result = mix(evenfield, oddfield, 0.5);\n" +
                        "    }else{\n" +
                        "       vec4 evenfield = texture2D(sTexture, vTextureCoord);\n" +
                        "       vec4 oddfield = texture2D(sTexture, vec2(vTextureCoord.x, vTextureCoord.y - unit));\n" +
                        "       result = mix(evenfield, oddfield, 0.5);\n" +
                        "    }\n" +
                        "    gl_FragColor = result;\n" +
                        "}\n"

//        private val FRAGMENT_SHADER_EXT_I2P_BLEND =
//                "#extension GL_OES_EGL_image_external : require\n" +
//                        "precision mediump float;\n" +
//                        "varying vec2 vTextureCoord;\n" +
//                        "uniform int odd; \n" +
//                        "uniform samplerExternalOES sTexture;\n" +
//                        "void main() {\n" +
//                        "    float isodd = mod(vTextureCoord.y, 2.0);\n" +
//                        "    vec4 result;\n" +
//                        "    float unit = 1.0/1200.0;\n" +
//                        "    if(odd > 0){\n" +
//                        "        if(isodd < 0.0){\n" +
//                        "           vec2 srcCoord = vec2(vTextureCoord.x, (vTextureCoord.y + unit)/2.0);\n" +
//                        "           result = texture2D(sTexture, srcCoord);\n" +
//                        "        }else{\n" +
//                        "           result = vec4(1.0, 1.0, 1.0, 0.0);\n" +
//                        "        }\n" +
//                        "    }else{\n" +
//                        "        if(isodd < 0.0){\n" +
//                        "           result = vec4(1.0, 1.0, 1.0, 0.0);\n" +
//                        "        }else{\n" +
//                        "           vec2 srcCoord = vec2(vTextureCoord.x, vTextureCoord.y/2.0);\n" +
//                        "           result = texture2D(sTexture, srcCoord);\n" +
//                        "        }\n" +
//                        "    }\n" +
//                        "    gl_FragColor = result;\n" +
//                        "}\n"

        // Simple fragment shader for use with external 2D textures (e.g. what we get from
        // SurfaceTexture).
        private val FRAGMENT_SHADER_EXT =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                        "}\n"

        // Fragment shader that converts color to black & white with a simple transformation.
        private val FRAGMENT_SHADER_EXT_BW =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                        "    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
                        "    gl_FragColor = vec4(color, color, color, 1.0);\n" +
                        "}\n"
        private val FRAGMENT_SHADER_GREEN_SCREEN_EXT =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision highp float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +

                        "uniform vec4 uChromaKey;\n" +
                        "uniform vec2 uPixelSize;\n" +
                        "uniform float uSmoothness;\n" +
                        "uniform float uSimilarity;\n" +
                        "uniform float uSpill;\n" +
                        "uniform int uUseFilter;\n" +

                        "mat4 yuv_mat = mat4(0.182586,  0.614231,  0.062007, 0.062745,\n" +
                        "                    -0.100644, -0.338572,  0.439216, 0.501961,\n" +
                        "                    0.439216, -0.398942, -0.040274, 0.501961,\n" +
                        "                    0.000000,  0.000000,  0.000000, 1.000000);\n" +

                        "vec4 chroma_key;" +

                        "vec4 SampleTexture(vec2 uv)\n" +
                        "{\n" +
                        "    return texture2D(sTexture, uv);\n" +
                        "}\n" +

                        "float saturate(float x)\n" +
                        "{\n" +
                        "    return clamp(x, 0.0, 1.0);\n" +
                        "}\n" +

                        "vec3 saturate(vec3 x)\n" +
                        "{\n" +
                        "    return clamp(x, vec3(0.0, 0.0, 0.0), vec3(1.0, 1.0, 1.0));\n" +
                        "}\n" +

                        "float GetChromaDist(vec3 rgb)\n" +
                        "{\n" +
                        "    vec4 yuvx = vec4(rgb.rgb, 1.0) * yuv_mat;\n" +
                        "    return distance(chroma_key.yz, yuvx.yz);\n" +
                        "}\n" +

                        "float GetBoxFilteredChromaDist(vec3 rgb, vec2 texCoord)\n" +
                        "{\n" +
                        "   float distVal = GetChromaDist(rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord-uPixelSize).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord-vec2(uPixelSize.x, 0.0)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord-vec2(uPixelSize.x, -uPixelSize.y)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord-vec2(0.0, uPixelSize.y)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord+vec2(0.0, uPixelSize.y)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord+vec2(uPixelSize.x, -uPixelSize.y)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord+vec2(uPixelSize.x, 0.0)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord+uPixelSize).rgb);\n" +
                        "   return distVal / 9.0;\n" +
                        "}\n" +

                        "vec4 ProcessChromaKey(vec4 rgba, vec2 uv)\n" +
                        "{\n" +
                        "   float chromaDist = GetBoxFilteredChromaDist(rgba.rgb, uv);\n" +
                        "   float baseMask = chromaDist - uSimilarity;\n" +
                        "   float fullMask = pow(saturate(baseMask / uSmoothness), 1.5);\n" +
                        "   float spillVal = pow(saturate(baseMask / uSpill), 1.5);\n" +
                        "   rgba.a *= fullMask;\n" +
                        "   float desat = (rgba.r * 0.2126 + rgba.g * 0.7152 + rgba.b * 0.0722);\n" +
                        "   rgba.rgb = saturate(vec3(desat, desat, desat)) * (1.0 - spillVal) + rgba.rgb * spillVal;\n" +
                        "   return rgba;\n" +
                        "}\n" +

                        "void main() {\n" +
                        "   vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                        "   gl_FragColor = ProcessChromaKey(tc, vTextureCoord);\n" +
                        "}\n"

        private val FRAGMENT_SHADER_GREEN_SCREEN =
                "precision highp float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform sampler2D sTexture;\n" +

                        "uniform vec4 uChromaKey;\n" +
                        "uniform vec2 uPixelSize;\n" +
                        "uniform float uSmoothness;\n" +
                        "uniform float uSimilarity;\n" +
                        "uniform float uSpill;\n" +
                        "uniform int uUseFilter;\n" +

                        "mat4 yuv_mat = mat4(0.182586,  0.614231,  0.062007, 0.062745,\n" +
                        "                    -0.100644, -0.338572,  0.439216, 0.501961,\n" +
                        "                    0.439216, -0.398942, -0.040274, 0.501961,\n" +
                        "                    0.000000,  0.000000,  0.000000, 1.000000);\n" +

                        "vec4 chroma_key;" +

                        "vec4 SampleTexture(vec2 uv)\n" +
                        "{\n" +
                        "    return texture2D(sTexture, uv);\n" +
                        "}\n" +

                        "float saturate(float x)\n" +
                        "{\n" +
                        "    return clamp(x, 0.0, 1.0);\n" +
                        "}\n" +

                        "vec3 saturate(vec3 x)\n" +
                        "{\n" +
                        "    return clamp(x, vec3(0.0, 0.0, 0.0), vec3(1.0, 1.0, 1.0));\n" +
                        "}\n" +

                        "float GetChromaDist(vec3 rgb)\n" +
                        "{\n" +
                        "    vec4 yuvx = vec4(rgb.rgb, 1.0) * yuv_mat;\n" +
                        "    return distance(chroma_key.yz, yuvx.yz);\n" +
                        "}\n" +

                        "float GetBoxFilteredChromaDist(vec3 rgb, vec2 texCoord)\n" +
                        "{\n" +
                        "   float distVal = GetChromaDist(rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord-uPixelSize).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord-vec2(uPixelSize.x, 0.0)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord-vec2(uPixelSize.x, -uPixelSize.y)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord-vec2(0.0, uPixelSize.y)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord+vec2(0.0, uPixelSize.y)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord+vec2(uPixelSize.x, -uPixelSize.y)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord+vec2(uPixelSize.x, 0.0)).rgb);\n" +
                        "   distVal += GetChromaDist(SampleTexture(texCoord+uPixelSize).rgb);\n" +
                        "   return distVal / 9.0;\n" +
                        "}\n" +

                        "vec4 ProcessChromaKey(vec4 rgba, vec2 uv)\n" +
                        "{\n" +
                        "   float chromaDist = GetBoxFilteredChromaDist(rgba.rgb, uv);\n" +
                        "   float baseMask = chromaDist - uSimilarity;\n" +
                        "   float fullMask = pow(saturate(baseMask / uSmoothness), 1.5);\n" +
                        "   float spillVal = pow(saturate(baseMask / uSpill), 1.5);\n" +
                        "   rgba.a *= fullMask;\n" +
                        "   float desat = (rgba.r * 0.2126 + rgba.g * 0.7152 + rgba.b * 0.0722);\n" +
                        "   rgba.rgb = saturate(vec3(desat, desat, desat)) * (1.0 - spillVal) + rgba.rgb * spillVal;\n" +
                        "   return rgba;\n" +
                        "}\n" +

                        "void main() {\n" +
                        "   vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                        "   gl_FragColor = ProcessChromaKey(tc, vTextureCoord);\n" +
                        "}\n"


        // Fragment shader with a convolution filter.  The upper-left half will be drawn normally,
        // the lower-right half will have the filter applied, and a thin red line will be drawn
        // at the border.
        //
        // This is not optimized for performance.  Some things that might make this faster:
        // - Remove the conditionals.  They're used to present a half & half view with a red
        //   stripe across the middle, but that's only useful for a demo.
        // - Unroll the loop.  Ideally the compiler does this for you when it's beneficial.
        // - Bake the filter kernel into the shader, instead of passing it through a uniform
        //   array.  That, combined with loop unrolling, should reduce memory accesses.
        val KERNEL_SIZE = 9
        private val FRAGMENT_SHADER_EXT_FILT =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "#define KERNEL_SIZE " + KERNEL_SIZE + "\n" +
                        "precision highp float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "uniform float uKernel[KERNEL_SIZE];\n" +
                        "uniform vec2 uTexOffset[KERNEL_SIZE];\n" +
                        "uniform float uColorAdjust;\n" +
                        "void main() {\n" +
                        "    int i = 0;\n" +
                        "    vec4 sum = vec4(0.0);\n" +
                        "    if (vTextureCoord.x < vTextureCoord.y - 0.005) {\n" +
                        "        for (i = 0; i < KERNEL_SIZE; i++) {\n" +
                        "            vec4 texc = texture2D(sTexture, vTextureCoord + uTexOffset[i]);\n" +
                        "            sum += texc * uKernel[i];\n" +
                        "        }\n" +
                        "    sum += uColorAdjust;\n" +
                        "    } else if (vTextureCoord.x > vTextureCoord.y + 0.005) {\n" +
                        "        sum = texture2D(sTexture, vTextureCoord);\n" +
                        "    } else {\n" +
                        "        sum.r = 1.0;\n" +
                        "    }\n" +
                        "    gl_FragColor = sum;\n" +
                        "}\n"

        private val FRAGMENT_SHADER_OBS_DINT_YADIF =
//                "#extension GL_OES_EGL_image_external : require\n" +
                "precision highp float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform sampler2D sTexture;\n" +
                "\n" +
                "uniform sampler2D previous_texture;\n" +
//                "uniform int field_order;\n" +
                "uniform vec2 uPixelSize;\n" +
                "\n" +
                "vec2 select(vec2 texel, vec2 pos)\n" +
                "{\n" +
                "    return vec2(texel + pos);\n" +
                "}\n" +
                "\n" +
                "vec4 load_at_prev(vec2 texel, float x, float y)\n" +
                "{\n" +
                "    return texture2D(previous_texture, select(texel, vec2(x, y)));\n" +
                "}\n" +
                "\n" +
                "vec4 load_at_image(vec2 texel, float x, float y)\n" +
                "{\n" +
                "    return texture2D(sTexture, select(texel, vec2(x, y)));\n" +
                "}\n" +
                "\n" +
                "vec4 load_at(vec2 texel, float x, float y, int field)\n" +
                "{\n" +
                "    if(field == 0)\n" +
                "        return load_at_image(texel, x, y);\n" +
                "    else\n" +
                "        return load_at_prev(texel, x, y);\n" +
                "}\n" +
                "\n" +

                "vec4 texel_at_blend(vec2 texel, int field)\n" +
                "{\n" +
                "   return (load_at_image(texel, 0.0, 0.0) + load_at_image(texel, 0.0, uPixelSize.y)) / 2.0;\n" +
                "}\n" +
                "\n" +
                "vec4 texel_at_blend_2x(vec2 texel, int field)\n" +
                "{\n" +
                "       return (load_at_image(texel, 0.0, 0.0) +\n" +
                "               load_at_image(texel, 0.0, uPixelSize.y)) / 2.0;\n" +
                "}\n" +
                "\n" +
                "vec4 texel_at_linear(vec2 texel, int field)\n" +
                "{\n" +
                "    float isodd = mod(texel.y, 2.0);\n" +
                "    int of = 0;\n" +
                "    if(isodd < 0.0){\n" +
                "       of = 0;\n" +
                "    }else{\n" +
                "       of = 1;\n" +
                "    }\n" +
                "   if (of == field)\n" +
                "       return load_at_image(texel, 0.0, 0.0);\n" +
                "   return (load_at_image(texel, 0.0, -uPixelSize.y) + load_at_image(texel, 0.0, uPixelSize.y)) / 2.0;\n" +
                "}\n" +
                "\n" +
                "vec4 texel_at_linear_2x(vec2 texel, int field)\n" +
                "{\n" +
                "   return texel_at_linear(texel, field);\n" +
                "}" +

                "  vec4 yadif_avg(vec2 texel, float x_off, float y_off)\n" +
                "  {\n" +
                "\n" +
                "      return ((load_at_prev(texel, x_off, y_off) + load_at_image(texel, x_off, y_off))/2.0);\n" +
                "  }\n" +
                "  float compare(float a, float b, float c){\n" +
                "      if(a > b + c){\n" +
                "           a = b + c; \n" +
                "      }else if(a < b - c){\n" +
                "           a = b - c;\n" +
                "      }\n" +
                "      return a;\n" +
                "  }\n" +
                "\n" +
                "  vec4 texel_at_yadif(vec2 texel, int field, int mode0)\n" +
                "  {\n" +
                "\n" +
                "    float isodd = mod(vTextureCoord.y, 2.0);\n" +
                "    int of = 0;\n" +
                "    if(isodd < 0.0){\n" +
                "       of = 1;\n" +
                "    }else{\n" +
                "       of = 0;\n" +
                "    }\n" +
                "      if(of == field)\n" +
                "        return load_at(texel, 0.0, 0.0, field);\n" +
                "      vec4 c = load_at(texel, 0.0, uPixelSize.y, field);\n" +
                "      vec4 d = yadif_avg(texel, 0.0, 0.0);\n" +
                "      vec4 e = load_at(texel, 0.0, -uPixelSize.y, field);\n" +
                "\n" +
                "      vec4 temporal_diff0 = (abs(load_at_prev(texel,  0.0, 0.0)      -     load_at_image(texel, 0.0, 0.0)))      / 2.0;\n" +
                "      vec4 temporal_diff1 = (abs(load_at_prev(texel,  0.0, uPixelSize.y) - c) + abs(load_at_prev(texel,  0.0, -uPixelSize.y) - e)) / 2.0;\n" +
                "      vec4 temporal_diff2 = (abs(load_at_image(texel, 0.0, uPixelSize.y) - c) + abs(load_at_image(texel, 0.0, -uPixelSize.y) - e)) / 2.0;\n" +
                "      vec4 diff = max(temporal_diff0, max(temporal_diff1, temporal_diff2));\n" +
                "\n" +
                "      vec4 spatial_pred = mix(c, e, 0.5);\n" +

                "  if (mode0!=0) {\n" +
                "    vec4 b = yadif_avg(texel, 0.0, 2.0*uPixelSize.y);\n" +
                "    vec4 f = yadif_avg(texel, 0.0, -2.0*uPixelSize.y);\n" +
                "\n" +
                "    vec4 max_ = max(d - e, max(d - c, min(b - c, f - e)));\n" +
                "    vec4 min_ = min(d - e, min(d - c, max(b - c, f - e)));\n" +
                "\n" +
                "    diff = max(diff, max(min_, -max_));\n" +
                "  } else {\n" +
                "    diff = max(diff, max(min(d - e, d - c), -max(d - e, d - c)));\n" +
                "  }\n" +
                "\n" +
                "  { \n" +
                "    spatial_pred.r = compare(spatial_pred.r, d.r, diff.r);\n" +
                "  }\n" +
                "\n" +
                "  { \n" +
                "    spatial_pred.g = compare(spatial_pred.g, d.g, diff.g);\n" +
                "  }\n" +
                "\n" +
                "  { \n" +
                "    spatial_pred.b = compare(spatial_pred.b, d.b, diff.b);\n" +
                "  }\n" +
                "\n" +
                "  { \n" +
                "    spatial_pred.a = compare(spatial_pred.a, d.a, diff.a);\n" +
                "  }\n" +
                "\n" +
                "  return spatial_pred;\n" +
                "}" +

                "void main() {\n" +
                "    gl_FragColor = texel_at_yadif(vTextureCoord, 0, 1);\n" +
                "}\n"
    }
}
