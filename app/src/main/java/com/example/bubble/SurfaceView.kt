package com.example.bubble

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class SurfaceView(context:Context) : GLSurfaceView(context) {

    private val renderer: GameRenderer

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = GameRenderer()

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

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

                val x_org = width * 0.5f
                val y_org = height.toFloat()

                val dx: Float = x_org - x
                val dy: Float = y_org - y

                renderer.mFireAngle = max(min(atan2(dx, dy), 1.0f), -1.0f)
                requestRender()
            }
            MotionEvent.ACTION_UP -> {
                renderer.mGameMode = GameMode.FIRING
                requestRender()
            }
        }

        previousX = x
        previousY = y
        return true
    }


}