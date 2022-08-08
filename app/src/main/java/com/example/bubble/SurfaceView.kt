package com.example.bubble

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class SurfaceView(context:Context) : GLSurfaceView(context) {

    private val mRenderer: GameRenderer

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        mRenderer = GameRenderer(context)

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

                val normX = (x - width.toFloat()*0.5f) / height.toFloat() * 2.0f
                val normY = (height.toFloat() * 0.5f - y) / height.toFloat() * 2.0f
                mRenderer.screenTouchMoveEvent(normX, normY)
                requestRender()
            }
            MotionEvent.ACTION_UP -> {

                val normX = (x - width.toFloat()*0.5f) / height.toFloat() * 2.0f
                val normY = (height.toFloat() * 0.5f - y) / height.toFloat() * 2.0f
                mRenderer.screenTouchUpEvent(normX, normY)
                requestRender()
            }
        }

        previousX = x
        previousY = y
        return true
    }


}