package com.example.bubble

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class GameMode{
    READY,
    FIRING,
    EXPLODING,
    FALLING,
    GAMEOVER
}

class GameRenderer : GLSurfaceView.Renderer {

    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private val vPMatrix = FloatArray(16)

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private lateinit var mTriangle: Triangle
    private lateinit var mSquare: Square2
    private lateinit var mSphere: Sphere
    private lateinit var mTargetingLine: TargetingLine

    @Volatile
    var angle = 0.0f

    @Volatile
    var mGameMode : GameMode = GameMode.READY

    private val mMaxWorldY = 1.0f
    private val mMinWorldY = -1.0f
    private var mMaxWorldX = 1.0f
    private var mMinWorldX = -1.0f

    /**
     * This method isc alled once to set up the view's OpenGL ES environment.
     * */
    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // Set the background frame color
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        mTriangle = Triangle()
        mSquare = Square2()
        mSphere = Sphere()
        mTargetingLine = TargetingLine()
    }

    /**
     * This method is called for each redraw of the view.
     * */
    override fun onDrawFrame(unused: GL10?) {
        val scratch = FloatArray(16)
//        var rotationMatrix = FloatArray(16)

        // Redraw background color
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)


        val time = SystemClock.uptimeMillis() % 4000L
        // Create a rotation transformation for the triangle
//        val time = SystemClock.uptimeMillis() % 4000L

//        var rotationMatrix = FloatArray(16)
//        Matrix.setRotateM(rotationMatrix, 0, angle, 0f, 0f, -1.0f)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
//        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0)

        val velocity = 0.02f

        if(GameMode.FIRING == mGameMode){
            val vx = -velocity * sin(angle)
            val vy = velocity * cos(angle)

            mSphere.instancePositions[0] += vx
            mSphere.instancePositions[1] += vy

            if(mMinWorldX > mSphere.instancePositions[0] || mSphere.instancePositions[0] > mMaxWorldX) {
                angle *= -1.0f
            }

            if(mSphere.instancePositions[1] > 1.0f){
                mSphere.instancePositions[0] = 0.0f
                mSphere.instancePositions[1] = -1.0f
                mGameMode = GameMode.READY
            }
        }

        mSphere.draw(vPMatrix)

        if(GameMode.READY == mGameMode) {
            mTargetingLine.draw(vPMatrix, angle)
        }
    }

    /**
     * Called if the geometry of the view changes, for example whe the device's screen orientation changes.
     * */
    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {

        val viewWidth = min(width, height)
        val viewHeight = max(width, height)

        GLES30.glViewport(0, 0, viewWidth, viewHeight)

        val ratio: Float = viewWidth.toFloat() / viewHeight.toFloat()

        mMinWorldX = -ratio
        mMaxWorldX = ratio

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

}


