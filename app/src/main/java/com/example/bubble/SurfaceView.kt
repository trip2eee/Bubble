package com.example.bubble

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import java.nio.ByteBuffer
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class SurfaceView(context:Context) : GLSurfaceView(context) {

    private val mRenderer: GameRenderer

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)


        val texFont = BitmapFactory.decodeResource(context.resources, R.drawable.font)
        var bufferTextureNumbers = ByteBuffer.allocate(texFont.byteCount)
        texFont.copyPixelsToBuffer(bufferTextureNumbers)

        var bufferTextureNumbersFloat : MutableList<Float> = arrayListOf()
        for(i in 0 until texFont.byteCount){
            val v : Float = bufferTextureNumbers[i].toUByte().toFloat() / 255.0f
            bufferTextureNumbersFloat.add(v)
        }


        mRenderer = GameRenderer(texFont)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer)

        // Render the view only when there is a change in the drawing data
        //renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    private val TOUCH_SCALE_FACTOR: Float = 180.0f / 320f
    private var previousX: Float = 0f
    private var previousY: Float = 0f

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        val x: Float = e.x
        val y: Float = e.y

        when (e.action) {
            MotionEvent.ACTION_MOVE -> {

                val origin = mRenderer.computeOriginInImage()
                // origin
                // x: -1 ~ 1
                // y: h/w ~ -h/w

                val x_org = (origin[0] + 1.0f) * 0.5f * width.toFloat()
                val y_org = (height.toFloat() - origin[1] * width.toFloat() * 0.5f)

                val dx: Float = x_org - x
                val dy: Float = y_org - y

                mRenderer.mFireAngle = max(min(atan2(dx, dy), 1.0f), -1.0f)
                requestRender()
            }
            MotionEvent.ACTION_UP -> {
                mRenderer.screenTabEvent()
                requestRender()
            }
        }

        previousX = x
        previousY = y
        return true
    }


}