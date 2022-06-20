package com.example.bubble

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

enum class GameMode{
    INITIALIZE,
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

    private lateinit var mBubbleObjects: BubbleObject
    private lateinit var mProjectileObject: ProjectileObject
    private lateinit var mTargetingLine: TargetingLine

    var mBubblePositions: MutableList<Float> = arrayListOf()
    var mBubbleColors: MutableList<Int> = arrayListOf()
    var mBubbleColorsRGB : MutableList<Float> = arrayListOf()
    var mExplodingCondition : MutableList<Float> = arrayListOf()

    var mNextColor: Int = 0

    private val random = Random()
    @Volatile
    var mFireAngle = 0.0f
    var mProjectileAngle = 0.0f

    @Volatile
    var mGameMode : GameMode = GameMode.INITIALIZE

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

        mBubbleObjects = BubbleObject()
        mBubbleObjects.initialize()

        mProjectileObject = ProjectileObject()
        mProjectileObject.initialize()

        mTargetingLine = TargetingLine()
    }

    fun colorCodeToRGB(color: Int): FloatArray {
        var colorRGB = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)

        if(color == 0) {
            colorRGB = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 0.1f)
        }else if(color == 1) {
            colorRGB = floatArrayOf(0.76953125f, 0.63671875f, 0.22265625f, 0.1f)
        }else if(color == 2) {
            colorRGB = floatArrayOf(0.63671875f, 0.22265625f, 0.76953125f, 0.1f)
        }else if(color == 3) {
            colorRGB = floatArrayOf(0.22265625f, 0.63671875f, 0.76953125f, 0.1f)
        }
        return colorRGB
    }

    fun setReadyState(){
        mGameMode = GameMode.READY
        mProjectileObject.mInstancePositions[0] = 0.0f
        mProjectileObject.mInstancePositions[1] = mMinWorldY

        mNextColor = random.nextInt(4)
        val colorRGB = colorCodeToRGB(mNextColor)
        mProjectileObject.mInstanceColors[0] = colorRGB[0]
        mProjectileObject.mInstanceColors[1] = colorRGB[1]
        mProjectileObject.mInstanceColors[2] = colorRGB[2]
    }

    fun checkExplodingCondition(idxStart:Int) {

        var stackIndex :MutableList<Int> = arrayListOf()

        // mark conditions
        mExplodingCondition.clear()
        for(i in 0 until mBubblePositions.count()/4){

        }
        // push
        stackIndex.add(idxStart)

        while(stackIndex.isNotEmpty()){

            val idxCur = stackIndex[0]
            stackIndex.removeAt(0)


        }

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

        val velocity = 0.02f
        val worldWidth : Float = mMaxWorldX - mMinWorldX
        val cols = 8
        val rows = 12
        val bubbleSize :Float = (worldWidth * 0.5f) / cols.toFloat()

        if(GameMode.INITIALIZE == mGameMode){
            for(i in 0 until cols){
                mBubblePositions.add(mMinWorldX + bubbleSize + 2.0f*bubbleSize*i.toFloat())
                mBubblePositions.add(mMaxWorldY - bubbleSize)
                mBubblePositions.add(0.0f)
                mBubblePositions.add(0.0f)

                val color = random.nextInt(4)
                mBubbleColors.add(color)

                val colorRGB = colorCodeToRGB(color)
                mBubbleColorsRGB.add(colorRGB[0])
                mBubbleColorsRGB.add(colorRGB[1])
                mBubbleColorsRGB.add(colorRGB[2])
                mBubbleColorsRGB.add(colorRGB[3])

            }
            mBubbleObjects.mInstancePositions = mBubblePositions.toFloatArray()
            mBubbleObjects.mInstanceColors = mBubbleColorsRGB.toFloatArray()
            mBubbleObjects.mNumInstances = cols

            setReadyState()
        }

        if(GameMode.FIRING == mGameMode){
            val vx = -velocity * sin(mProjectileAngle)
            val vy = velocity * cos(mProjectileAngle)

            mProjectileObject.mInstancePositions[0] += vx
            mProjectileObject.mInstancePositions[1] += vy

            // Test boundary.
            if((mMinWorldX+bubbleSize) > mProjectileObject.mInstancePositions[0] || mProjectileObject.mInstancePositions[0] > (mMaxWorldX-bubbleSize)) {
                mProjectileAngle *= -1.0f
            }

            if(mProjectileObject.mInstancePositions[1] > mMaxWorldY){
                setReadyState()
            }

            // Test collision with other bubbles
            val curX = mProjectileObject.mInstancePositions[0]
            val curY = mProjectileObject.mInstancePositions[1]
            var minSqrDist = 1000.0f
            var minBubbleX = 0.0f
            var minBubbleY = 0.0f
            val diameter = bubbleSize * 2.0f
            for(idxBubble in 0 until mBubbleObjects.mNumInstances){
                val bubbleX = mBubblePositions[idxBubble * 4 + 0]
                val bubbleY = mBubblePositions[idxBubble * 4 + 1]
                val dx = curX - bubbleX
                val dy = curY - bubbleY
                val sqrDist = (dx*dx) + (dy*dy)
                if((sqrDist < (diameter*diameter)) && (sqrDist < minSqrDist)){
                    minSqrDist = sqrDist
                    minBubbleX = bubbleX
                    minBubbleY = bubbleY
                }
            }

            if(minSqrDist <= diameter*diameter){
                val newX = if(curX < minBubbleX){
                    minBubbleX - (diameter * cos(60.0f * PI.toFloat() / 180.0f))
                }else{
                    minBubbleX + (diameter * cos(60.0f * PI.toFloat() / 180.0f))
                }
                val newY = minBubbleY - (diameter * sin(60.0f * PI.toFloat() / 180.0f))

                mBubblePositions.add(newX)
                mBubblePositions.add(newY)
                mBubblePositions.add(0.0f)
                mBubblePositions.add(0.0f)

                mBubbleColors.add(mNextColor)
                val colorRGB = colorCodeToRGB(mNextColor)
                mBubbleColorsRGB.add(colorRGB[0])
                mBubbleColorsRGB.add(colorRGB[1])
                mBubbleColorsRGB.add(colorRGB[2])
                mBubbleColorsRGB.add(colorRGB[3])

                mBubbleObjects.mInstancePositions = mBubblePositions.toFloatArray()
                mBubbleObjects.mInstanceColors = mBubbleColorsRGB.toFloatArray()
                mBubbleObjects.mNumInstances ++

                mGameMode = GameMode.EXPLODING
            }
        }
        else if(GameMode.EXPLODING == mGameMode){
            checkExplodingCondition(mBubblePositions.count()-1)

            mGameMode = GameMode.FALLING
        }
        else if(GameMode.FALLING == mGameMode) {
            setReadyState()
        }

        if(GameMode.READY == mGameMode) {
            mProjectileAngle = mFireAngle

            val startY = mMaxWorldY - (rows.toFloat() * (bubbleSize*2.0f))

            mTargetingLine.draw(vPMatrix, mFireAngle)
        }

        mBubbleObjects.draw(vPMatrix, bubbleSize)
        mProjectileObject.draw(vPMatrix, bubbleSize)

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


