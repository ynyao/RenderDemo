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

import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer


/**
 * Some OpenGL utility functions.
 */
object GlUtil {
    val TAG = "Grafika"

    /** Identity matrix for general use.  Don't modify or life will get weird.  */
    val IDENTITY_MATRIX: FloatArray

    init {
        IDENTITY_MATRIX = FloatArray(16)
        Matrix.setIdentityM(IDENTITY_MATRIX, 0)
    }

    private val SIZEOF_FLOAT = 4

    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            Log.e("GLUTIL","Could not load shader $vertexSource  .")
            return 0
        }
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) {
            Log.e("GLUTIL","Could not load shader $fragmentSource  .")
            return 0
        }

        var program = GLES20.glCreateProgram()
        checkGlError("glCreateProgram")
        if (program == 0) {
            Log.e("GLUTIL","Could not create program")
        }
        GLES20.glAttachShader(program, vertexShader)
        checkGlError("glAttachShader")
        GLES20.glAttachShader(program, pixelShader)
        checkGlError("glAttachShader")
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e("GLUTIL","Could not link program.")
            Log.e("GLUTIL","Could not link program  ${GLES20.glGetProgramInfoLog(program)}")
//            LogUtils.e(GLES20.glGetProgramInfoLog(program))
            GLES20.glDeleteProgram(program)
            program = 0
        }
        return program
    }

    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        checkGlError("glCreateShader action=" + shaderType)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("GLUTIL","Could not compile shader $shaderType")
            Log.e("GLUTIL",GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = 0
        }
        return shader
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            val msg = op + ": glError 0x" + Integer.toHexString(error)
//            LogUtils.e(msg)
            throw RuntimeException(msg)
        }
    }

    /**
     * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
     * could not be found, but does not set the GL error.
     *
     *
     * Throws a RuntimeException if the location is invalid.
     */
    fun checkLocation(location: Int, label: String) {
        if (location < 0) {
            throw RuntimeException("Unable to locate '$label' in program")
        }
    }

    /**
     * Creates a texture from raw data.
     *
     * @param data Image data, in a "direct" ByteBuffer.
     * @param width Texture width, in pixels (not bytes).
     * @param height Texture height, in pixels.
     * @param format Image data format (use constant appropriate for glTexImage2D(), e.g. GL_RGBA).
     * @return Handle to texture.
     */
    fun createImageTexture(data: ByteBuffer, width: Int, height: Int, format: Int): Int {
        val textureHandles = IntArray(1)
        val textureHandle: Int

        GLES20.glGenTextures(1, textureHandles, 0)
        textureHandle = textureHandles[0]
        GlUtil.checkGlError("glGenTextures")

        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)

        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR)
        GlUtil.checkGlError("loadImageTexture")

        // Load the data from the buffer into the texture handle.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, /*level*/ 0, format,
                width, height, /*border*/ 0, format, GLES20.GL_UNSIGNED_BYTE, data)
        GlUtil.checkGlError("loadImageTexture")

        return textureHandle
    }

    fun createImageTexture(bitmap: Bitmap): Int{
        val width = bitmap.width
        val height = bitmap.height
        val ib = IntBuffer.allocate(width * height)
        bitmap.copyPixelsToBuffer(ib)
        ib.rewind()

        // allocate a GL texture
        val textureHandles = IntArray(1)
        val textureHandle: Int

        GLES20.glGenTextures(1, textureHandles, 0)
        textureHandle = textureHandles[0]
        GlUtil.checkGlError("glGenTextures")

        // copy texture data and generate mipmap
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib)
        GlUtil.checkGlError("overlay glTexImage2D")
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GlUtil.checkGlError("overlay glTexParameteri min")
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GlUtil.checkGlError("overlay glTexParameteri mag")

        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GlUtil.checkGlError("overlay glGenerateMipmap")

//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap, GLES20.GL_UNSIGNED_BYTE, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        bitmap.recycle()
        return textureHandle
    }

    fun releaseTexture(texture: Int){
        val values = IntArray(1)
        values[0] = texture
        GLES20.glDeleteTextures(1, values, 0)
    }

    fun blitFramebuffer(read: Int, draw: Int, readRect: Rect, drawRect: Rect){
        GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, read)
        GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, draw)
        GLES30.glReadBuffer(GLES30.GL_COLOR_ATTACHMENT0)
        GLES30.glBlitFramebuffer(readRect.left, readRect.top,
                                 readRect.right, readRect.bottom,
                                 drawRect.left, drawRect.top,
                                 drawRect.right, drawRect.bottom,
                                 GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_NEAREST)
        GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, 0)
        GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0)
    }

    /**
     * Creates a texture object suitable for use with this program.
     *
     *
     * On exit, the texture will be bound.
     */
    fun createTextureObject(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GlUtil.checkGlError("glGenTextures")

        val texId = textures[0]
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
        GlUtil.checkGlError("glBindTexture " + texId)

        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_NEAREST.toFloat())
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR.toFloat())
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE)
        GlUtil.checkGlError("glTexParameter")

        return texId
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    fun createFloatBuffer(coords: FloatArray): FloatBuffer {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        val bb = ByteBuffer.allocateDirect(coords.size * SIZEOF_FLOAT)
        bb.order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(coords)
        fb.position(0)
        return fb
    }

    fun prepareFrameBuffer(w: Int = 1920, h: Int = 1080): GLFrameBuffer{
        val fbo = GLFrameBuffer()
        val values = IntArray(1)
        GLES30.glGenTextures(1, values, 0)

        fbo.frameBufferTextureId = values[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fbo.frameBufferTextureId)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, w, h, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_NEAREST.toFloat())
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR.toFloat())
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE)

        // Create framebuffer object and bind it.
        GLES20.glGenFramebuffers(1, values, 0)
        GlUtil.checkGlError("glGenFramebuffers")
        fbo.framebuffer = values[0]    // expected > 0
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo.framebuffer!!)
        GlUtil.checkGlError("glBindFramebuffer ${fbo.framebuffer}")

        // Create a depth buffer and bind it.
//        GLES20.glGenRenderbuffers(1, values, 0)
//        GlUtil.checkGlError("glGenRenderbuffers")
//        fbo.depthBuffer = values[0]    // expected > 0
//        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, fbo.depthBuffer!!)
//        GlUtil.checkGlError("glBindRenderbuffer ${fbo.depthBuffer}")

        // Allocate storage for the depth buffer.
//        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
//                VideoSource.srcWidth, VideoSource.srcHeight)
//        GlUtil.checkGlError("glRenderbufferStorage")

        // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
//        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
//                GLES20.GL_RENDERBUFFER, fbo.depthBuffer!!)
//        GlUtil.checkGlError("glFramebufferRenderbuffer")
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fbo.frameBufferTextureId, 0)
        GlUtil.checkGlError("glFramebufferTexture2D")

        // See if GLES is happy with all this.
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer not complete, status=$status")
        }

        // Switch back to the default framebuffer.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        GlUtil.checkGlError("prepareFramebuffer done")
        return fbo
    }

    fun releaseFrameBuffer(frameBuffer: GLFrameBuffer){
        val values = IntArray(1)
        values[0] = frameBuffer.frameBufferTextureId
        if(values[0] != -1){
            GLES20.glDeleteTextures(1, values, 0)
        }
        values[0] = frameBuffer.framebuffer
        if(values[0] != -1){
            GLES20.glDeleteFramebuffers(1, values, 0)
        }
        values[0] = frameBuffer.depthBuffer
        if(values[0] != -1){
            GLES20.glDeleteRenderbuffers(1, values, 0)
        }

    }

    fun preparePixelBuffer(w: Int, h: Int){
        val buffer = IntArray(1)
        GLES30.glGenBuffers(buffer.size, buffer, 0)
        checkGlError("glGenBuffers")
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, buffer[0])
        val size = w * h * 4
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, size, null, GLES30.GL_DYNAMIC_READ)
        checkGlError("glBufferData")
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0)
    }

    /**
     * Writes GL version info to the log.
     */
    fun logVersionInfo() {
//        LogUtils.d( "vendor  : " + GLES20.glGetString(GLES20.GL_VENDOR))
//        LogUtils.d( "renderer: " + GLES20.glGetString(GLES20.GL_RENDERER))
//        LogUtils.d( "version : " + GLES20.glGetString(GLES20.GL_VERSION))

//        if (false) {
//            val values = IntArray(1)
//            GLES20.glGetIntegerv(GLES20.GL_MAJOR_VERSION, values, 0)
//            val majorVersion = values[0]
//            GLES20.glGetIntegerv(GLES20.GL_MINOR_VERSION, values, 0)
//            val minorVersion = values[0]
//            if (GLES20.glGetError() == GLES20.GL_NO_ERROR) {
//                LogUtils.d( "iversion: $majorVersion.$minorVersion")
//            }
//        }
    }
}// do not instantiate
