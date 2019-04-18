package com.zcy.renderdemo.gles

class TextureRender {

    private val TAG = "TextureRender"

    private var program2D: Texture2dProgram ? = null
    private var greenscreenProgram: Texture2dProgram? = null
    private var mFullScreen: FullFrameRect? = null
    private var mWidth: Float = 0f
    private var mHeight:Float = 0f
    private var mPosX: Float = 0f
    private var mPosY: Float = 0f

    private val glMatrixUtil = GLMatrixUtil()

    init {
        program2D = Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D)
        mFullScreen = FullFrameRect(program2D!!)
    }

    private fun createGreenscreenProgram(){
        if(greenscreenProgram == null){
            greenscreenProgram = Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_GREENSCREEN)
        }
    }



    fun setUseGreenFilter(smooth: Float, similarity: Float){
        createGreenscreenProgram()
        greenscreenProgram?.setUseBlending(false)
        greenscreenProgram?.setKeyColorSmoothness(smooth)
        greenscreenProgram?.setKeyColorSimilarity(similarity)
        mFullScreen?.program = greenscreenProgram
    }

    fun setUseTexture2D(){
        program2D?.setUseBlending(true)
        mFullScreen?.program = program2D
    }

    fun setRegin(posX: Float, posY: Float, width: Float, height: Float){
        mWidth = width
        mHeight = height
        mPosX = posX
        mPosY = posY
    }

    fun drawTexture(textureId: Int){
        mFullScreen?.drawFrame(textureId, GlUtil.IDENTITY_MATRIX)
        GlUtil.checkGlError("hw draw done")
    }

    fun drawTexture(textureId: Int, projectionMatrix: FloatArray){
        glMatrixUtil.setPosition(mPosX, mPosY)
        glMatrixUtil.setScale(mWidth/2, mHeight/2)
        mFullScreen?.drawFrame(textureId, glMatrixUtil.generateMVPMatrix(projectionMatrix), GlUtil.IDENTITY_MATRIX)
        GlUtil.checkGlError("hw draw done")
    }

    fun drawTexture2(textureId: Int, projectionMatrix: FloatArray){
        glMatrixUtil.setPosition(mPosX, mPosY)
        glMatrixUtil.setScale(mWidth/2, mHeight/2)
        mFullScreen?.drawFrame(textureId, GlUtil.IDENTITY_MATRIX, glMatrixUtil.generateMVPMatrix(projectionMatrix))
        GlUtil.checkGlError("hw draw done")
    }

    fun release(){

        if (mFullScreen != null) {
            mFullScreen?.release(false)
            mFullScreen = null
        }

        if (program2D != null) {
            program2D?.release()
            program2D = null
        }

        if (greenscreenProgram != null) {
            greenscreenProgram?.release()
            greenscreenProgram = null
        }

    }

}