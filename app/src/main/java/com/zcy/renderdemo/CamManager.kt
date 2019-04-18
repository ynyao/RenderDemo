package com.zcy.renderdemo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import com.zcy.renderdemo.gles.RenderThread2
import java.util.*

class CamManager(internal var context: Activity) {

    internal var previewWidth = 1080
    internal var previewHeight = 1920
    internal lateinit var textureView: TextureView
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var mBackgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var mBackgroundHandler: Handler? = null

    var isFirst=true;
    internal var time = System.currentTimeMillis()
    internal var textureListener: TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            //当SurefaceTexture可用的时候，设置相机参数并打开相机
            renderThread?.sendSurfaceAvailable(surface,width,height)

            doOpenCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            Log.i("delta time", "delta :" + (System.currentTimeMillis() - time))
            time = System.currentTimeMillis()
        }
    }

    var renderThread:RenderThread2 ?=null
    init {
        renderThread= RenderThread2()
        renderThread!!.start()
        initView()
        openCam()
    }



    private fun openCam() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)

    }

    private fun initView() {
        textureView = context.findViewById(R.id.ttv_preview)
        textureView.surfaceTextureListener = textureListener

    }

    private fun doOpenCamera() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = manager.cameraIdList[0]//这个可能会有很多个，但是通常都是两个，第一个是后置，第二个是前置；
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
//mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.i(TAG, "onOpened")
                    createCameraPreview(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.i(TAG, "onDisconnected")
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "onError -> $error")
                    camera.close()
                }
            }, renderThread?.cameraScene?.handler)//这个指定其后台运行，如果直接UI线程也可以，直接填null；
            Log.i(TAG, "open Camera $cameraId")
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    protected fun createCameraPreview(cameraDevice: CameraDevice?) {
        try {
            if (null == cameraDevice) {
                Log.i(TAG, "updatePreview error, return")
                return
            }
            //            setUpImageReader();
            val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

            val sft = renderThread?.cameraScene?.generateCamTexture()//textureView.surfaceTexture
            sft?.setDefaultBufferSize(previewWidth, previewHeight)
            val textureSurface = Surface(sft)


            //            Surface imageSurface = imageReader.getSurface();
            captureRequestBuilder.addTarget(textureSurface)
            //captureRequestBuilder.addTarget(recorderSurface);
            //            captureRequestBuilder.addTarget(imageSurface);
            val surfaceList = Arrays.asList(textureSurface)//, imageSurface);
            cameraDevice.createCaptureSession(surfaceList, object : CameraCaptureSession.StateCallback() {
                //配置要接受图像的surface
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    try {
                        cameraCaptureSession.setRepeatingRequest(
                            captureRequestBuilder.build(),
                            null,
                            mBackgroundHandler
                        )//成功配置后，便开始进行相机图像的监听
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }

                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Toast.makeText(context, "Configuration change", Toast.LENGTH_LONG).show()
                }
            }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    companion object {

        var TAG = "CameraWrapper"
    }

}
