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
import java.nio.FloatBuffer

/**
 * Base class for stuff we like to draw.
 */
open class Drawable2d
/**
 * Prepares a drawable from a "pre-fabricated" shape definition.
 *
 *
 * Does no EGL/GL operations, so this can be done at any time.
 */
(private val mPrefab: Prefab?) {


    /**
     * Returns the array of vertices.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    var vertexArray: FloatBuffer? = null
        private set
    /**
     * Returns the array of texture coordinates.
     *
     *
     * To avoid allocations, this returns internal state.  The caller must not modify it.
     */
    open var texCoordArray: FloatBuffer? = null
    /**
     * Returns the number of vertices stored in the vertex array.
     */
    var vertexCount: Int = 0
    /**
     * Returns the number of position coordinates per vertex.  This will be 2 or 3.
     */
    var coordsPerVertex: Int = 0
    /**
     * Returns the width, in bytes, of the data for each vertex.
     */
    var vertexStride: Int = 0
    /**
     * Returns the width, in bytes, of the data for each texture coordinate.
     */
    val texCoordStride: Int

    /**
     * Enum values for constructor.
     */
    enum class Prefab {
        TRIANGLE, RECTANGLE, FULL_RECTANGLE, RECTANGLE_CROP, RECTANGLE_2D
    }

    init {
        when (mPrefab) {
            Drawable2d.Prefab.TRIANGLE -> {
                vertexArray = TRIANGLE_BUF
                texCoordArray = TRIANGLE_TEX_BUF
                coordsPerVertex = 2
                vertexStride = coordsPerVertex * SIZEOF_FLOAT
                vertexCount = TRIANGLE_COORDS.size / coordsPerVertex
            }
            Drawable2d.Prefab.RECTANGLE -> {
                vertexArray = RECTANGLE_BUF
                texCoordArray = RECTANGLE_TEX_BUF
                coordsPerVertex = 2
                vertexStride = coordsPerVertex * SIZEOF_FLOAT
                vertexCount = RECTANGLE_COORDS.size / coordsPerVertex
            }
            Drawable2d.Prefab.RECTANGLE_CROP -> {
                vertexArray = RECTANGLE_BUF
                texCoordArray = RECTANGLE_TEX_CROP_BUF
                coordsPerVertex = 2
                vertexStride = coordsPerVertex * SIZEOF_FLOAT
                vertexCount = RECTANGLE_COORDS.size / coordsPerVertex
            }
            Drawable2d.Prefab.RECTANGLE_2D -> {
                vertexArray = RECTANGLE_BUF
                texCoordArray = FULL_RECTANGLE_TEX_BUF
                coordsPerVertex = 2
                vertexStride = coordsPerVertex * SIZEOF_FLOAT
                vertexCount = RECTANGLE_COORDS.size / coordsPerVertex
            }
            Drawable2d.Prefab.FULL_RECTANGLE -> {
                vertexArray = FULL_RECTANGLE_BUF
                texCoordArray = FULL_RECTANGLE_TEX_BUF
                coordsPerVertex = 2
                vertexStride = coordsPerVertex * SIZEOF_FLOAT
                vertexCount = FULL_RECTANGLE_COORDS.size / coordsPerVertex
            }
            else -> throw RuntimeException("Unknown shape " + mPrefab)
        }
        texCoordStride = 2 * SIZEOF_FLOAT
    }

    fun cropTexCoordArray(w: Int, h: Int, cl: Int, ct: Int, cw: Int, ch: Int){
        val l = cl.toFloat()
        val r = (cl+cw).toFloat()
        val t = ct.toFloat()
        val b = (ct + ch).toFloat()
        val textBuf = floatArrayOf(l/w, t/h,
                                   r/w, t/h,
                                   l/w, b/h,
                                   r/w, b/h
        )
        texCoordArray = GlUtil.createFloatBuffer(textBuf)
    }

    override fun toString(): String {
        return if (mPrefab != null) {
            "[Drawable2d: $mPrefab]"
        } else {
            "[Drawable2d: ...]"
        }
    }

    companion object {
        private val SIZEOF_FLOAT = 4

        /**
         * Simple equilateral triangle (1.0 per side).  Centered on (0,0).
         */
        private val TRIANGLE_COORDS = floatArrayOf(0.0f, 0.577350269f, // 0 top
                -0.5f, -0.288675135f, // 1 bottom left
                0.5f, -0.288675135f    // 2 bottom right
        )
        private val TRIANGLE_TEX_COORDS = floatArrayOf(0.5f, 0.0f, // 0 top center
                0.0f, 1.0f, // 1 bottom left
                1.0f, 1.0f)// 2 bottom right
        private val TRIANGLE_BUF = GlUtil.createFloatBuffer(TRIANGLE_COORDS)
        private val TRIANGLE_TEX_BUF = GlUtil.createFloatBuffer(TRIANGLE_TEX_COORDS)

        /**
         * Simple square, specified as a triangle strip.  The square is centered on (0,0) and has
         * a size of 1x1.
         *
         *
         * Triangles are 0-1-2 and 2-1-3 (counter-clockwise winding).
         */
        private val RECTANGLE_COORDS = floatArrayOf(-0.5f, -0.5f, // 0 bottom left
                0.5f, -0.5f, // 1 bottom right
                -0.5f, 0.5f, // 2 top left
                0.5f, 0.5f)// 3 top right
//        private val RECTANGLE_TEX_COORDS = floatArrayOf(0.0f, 1.0f, // 0 bottom left
//                1.0f, 1.0f, // 1 bottom right
//                0.0f, 0.0f, // 2 top left
//                1.0f, 0.0f      // 3 top right
//        )

        private val RECTANGLE_TEX_COORDS = floatArrayOf(1.0f, 1.0f, // 0 bottom left
            1.0f, 0.0f, // 1 bottom right
            0.0f, 1.0f, // 2 top left
            0.0f, 0.0f      // 3 top right
        )
        private val RECTANGLE_TEX_CROP_COORDS = floatArrayOf(0.0f, 500.0f/576.0f, // 0 bottom left
                1.0f, 500.0f/576.0f, // 1 bottom right
                0.0f, 20.0f/576.0f, // 2 top left
                1.0f, 20.0f/576.0f      // 3 top right
        )

        private val RECTANGLE_BUF = GlUtil.createFloatBuffer(RECTANGLE_COORDS)
        private val RECTANGLE_TEX_BUF = GlUtil.createFloatBuffer(RECTANGLE_TEX_COORDS)
        private val RECTANGLE_TEX_CROP_BUF = GlUtil.createFloatBuffer(RECTANGLE_TEX_CROP_COORDS)


        /**
         * A "full" square, extending from -1 to +1 in both dimensions.  When the model/view/projection
         * matrix is identity, this will exactly cover the viewport.
         *
         *
         * The texture coordinates are Y-inverted relative to RECTANGLE.  (This seems to work out
         * right with external textures from SurfaceTexture.)
         */
        private val FULL_RECTANGLE_COORDS = floatArrayOf(-1.0f, -1.0f, // 0 bottom left
                1.0f, -1.0f, // 1 bottom right
                -1.0f, 1.0f, // 2 top left
                1.0f, 1.0f)// 3 top right
        private val FULL_RECTANGLE_TEX_COORDS = floatArrayOf(0.0f, 0.0f, // 0 bottom left
                1.0f, 0.0f, // 1 bottom right
                0.0f, 1.0f, // 2 top left
                1.0f, 1.0f      // 3 top right
        )
        private val FULL_RECTANGLE_BUF = GlUtil.createFloatBuffer(FULL_RECTANGLE_COORDS)
        private val FULL_RECTANGLE_TEX_BUF = GlUtil.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS)
    }
}
