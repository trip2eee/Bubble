package com.example.bubble

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
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

enum class BubbleStatus{
    IDLE,
    EXPLODING,
    FALLING
}

enum class BubbleType {
    GREEN,
    RED,
    BLUE,
    YELLOW,
    NUM_COLORS
}

class GameRenderer : GLSurfaceView.Renderer {

    // Model View Projection Matrix
    private val mPMatrix = FloatArray(16)

    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)

    private lateinit var mBubbleObjects: BubbleObject
    private lateinit var mProjectileObject: ProjectileObject
    private lateinit var mTargetingLine: TargetingLine
    private lateinit var mParticleObjects: ParticleObject

    var mBubblePositions: MutableList<Float> = arrayListOf()
    var mBubbleColors: MutableList<Int> = arrayListOf()
    var mBubbleColorsRGB : MutableList<Float> = arrayListOf()
    var mBubbleStatus : MutableList<BubbleStatus> = arrayListOf()

    var mExplodingPositions: MutableList<Float> = arrayListOf()
    var mExplodingColorsRGB : MutableList<Float> = arrayListOf()

    var mNextColor: Int = 0

    private val random = Random()
    @Volatile
    var mFireAngle = 0.0f
    var mProjectileAngle = 0.0f
    var mEventTime = 0.0f

    @Volatile
    var mGameMode : GameMode = GameMode.INITIALIZE

    private var mMaxWorldY = 1.0f
    private var mMinWorldY = -1.0f
    private val mMaxWorldX = 1.0f
    private val mMinWorldX = -1.0f
    private val mTopMarginY = 0.3f
    private val mDIM_POSITION = 4

    private val mMinBubblesToExplode = 3
    private val mCols = 8
    private val mRows = 12

    private val mVelocity = 0.04f
    private val mWorldWidth : Float = mMaxWorldX - mMinWorldX
    private val mBubbleRadius = (mWorldWidth * 0.5f) / mCols.toFloat()
    private val mBubbleDiameter = mBubbleRadius * 2.0f
    private val mAlignAngle = 60.0f * PI.toFloat() / 180.0f
    private val mWorldHeight : Float = mBubbleRadius + (sin(mAlignAngle)*mBubbleDiameter*mRows)
    private val mGameOverHeight : Float = mBubbleRadius + (sin(mAlignAngle)*mBubbleDiameter*(mRows-1.0f))
    private var mOrigin : FloatArray = computeOrigin()

    /**
     * This method isc alled once to set up the view's OpenGL ES environment.
     * */
    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // Set the background frame color
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)       // Ignore back face
        GLES30.glFrontFace(GLES30.GL_CW)        // Front face: Clockwise

        mBubbleObjects = BubbleObject()
        mBubbleObjects.initialize()

        mProjectileObject = ProjectileObject()
        mProjectileObject.initialize()

        mParticleObjects = ParticleObject()
        mParticleObjects.initialize()

        mTargetingLine = TargetingLine()
    }

    fun computeOrigin(): FloatArray{
        return floatArrayOf(0f, mMaxWorldY - mWorldHeight, 0f, 0f)
    }

    fun colorCodeToRGB(color: Int): FloatArray {
        var colorRGB = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)

        if(color == BubbleType.GREEN.ordinal) {
            colorRGB = floatArrayOf(0.53671875f, 0.76953125f, 0.22265625f, 1.0f)
        }else if(color == BubbleType.RED.ordinal) {
            colorRGB = floatArrayOf(0.76953125f, 0.63671875f, 0.22265625f, 1.0f)
        }else if(color == BubbleType.BLUE.ordinal) {
            colorRGB = floatArrayOf(0.63671875f, 0.22265625f, 0.76953125f, 1.0f)
        }else if(color == BubbleType.YELLOW.ordinal) {
            colorRGB = floatArrayOf(0.22265625f, 0.63671875f, 0.76953125f, 1.0f)
        }

        return colorRGB
    }

    fun checkGameOver(){
        // find the bubble at the lowest position.
        var lowestY = 0.0f

        for(idxBubble in 0 until mBubblePositions.count()/mDIM_POSITION){
            val bubbleY = mBubblePositions[idxBubble * 4 + 1]
            if(bubbleY < lowestY){
                lowestY = bubbleY
            }
        }

        if(lowestY <= (mMaxWorldY - mGameOverHeight + mBubbleRadius*0.1f)){
            mGameMode = GameMode.GAMEOVER

            for(idxBubble in 0 until mBubblePositions.count()/mDIM_POSITION){
                mBubbleColorsRGB[idxBubble * 4 + 0] = 0.5f
                mBubbleColorsRGB[idxBubble * 4 + 1] = 0.5f
                mBubbleColorsRGB[idxBubble * 4 + 2] = 0.5f
                mBubbleColorsRGB[idxBubble * 4 + 3] = 0.5f
            }
        }
    }

    fun setReadyState(){

        checkGameOver()
        if(GameMode.GAMEOVER != mGameMode) {
            mGameMode = GameMode.READY
            mProjectileObject.mInstancePositions[0] = mOrigin[0]
            mProjectileObject.mInstancePositions[1] = mOrigin[1]

            mNextColor = random.nextInt(BubbleType.NUM_COLORS.ordinal)
            val colorRGB = colorCodeToRGB(mNextColor)
            mProjectileObject.mInstanceColors[0] = colorRGB[0]
            mProjectileObject.mInstanceColors[1] = colorRGB[1]
            mProjectileObject.mInstanceColors[2] = colorRGB[2]
        }
    }

    fun checkExplodingCondition(idxStart:Int) {

        mExplodingColorsRGB = arrayListOf()
        mExplodingPositions = arrayListOf()

        var stackIndex :MutableList<Int> = arrayListOf()
        // mark conditions
        val numBubbles = mBubblePositions.count()/mDIM_POSITION
        mBubbleStatus.clear()
        for(i in 0 until numBubbles){
            mBubbleStatus.add(BubbleStatus.IDLE)
        }

        var numClustered: Int = 1
        val contactThres = mBubbleRadius*2.1f

        mBubbleStatus[idxStart] = BubbleStatus.EXPLODING
        stackIndex.add(idxStart)    // push
        while(stackIndex.isNotEmpty()){
            val idxCur = stackIndex[0]  // pop
            stackIndex.removeAt(0)

            val curX = mBubblePositions[idxCur * mDIM_POSITION + 0]
            val curY = mBubblePositions[idxCur * mDIM_POSITION + 1]

            for(idxSearch in 0 until numBubbles) {
                if (BubbleStatus.IDLE == mBubbleStatus[idxSearch] &&
                    mBubbleColors[idxCur] == mBubbleColors[idxSearch]) {
                    val dx = mBubblePositions[idxSearch * mDIM_POSITION + 0] - curX
                    val dy = mBubblePositions[idxSearch * mDIM_POSITION + 1] - curY
                    val sqrDist = (dx * dx) + (dy * dy)
                    if (sqrDist <= contactThres * contactThres) {
                        stackIndex.add(idxSearch)       // push
                        mBubbleStatus[idxSearch] = BubbleStatus.EXPLODING
                        numClustered++
                    }
                }
            }
        }
        if(numClustered < mMinBubblesToExplode){
            for(i in 0 until numBubbles) {
                mBubbleStatus[i] = BubbleStatus.IDLE
            }
        }else{
            // remove bubbles with exploding state.
            for(i in numBubbles-1 downTo 0){
                if(BubbleStatus.EXPLODING == mBubbleStatus[i]){
                    for(idxCoord in 0 until 4){
                        mExplodingPositions.add(mBubblePositions[i * mDIM_POSITION + idxCoord])
                        mExplodingColorsRGB.add(mBubbleColorsRGB[i * mDIM_POSITION + idxCoord])
                    }
                    for(idxCoord in 3 downTo 0){
                        mBubblePositions.removeAt(i * mDIM_POSITION + idxCoord)
                        mBubbleColorsRGB.removeAt(i * mDIM_POSITION + idxCoord)
                    }
                    mBubbleColors.removeAt(i)
                }
            }
        }
    }

    fun checkFalling(){
        // mark conditions
        val numBubbles = mBubblePositions.count()/mDIM_POSITION
        mBubbleStatus.clear()

        var labeled : MutableList<Int> = arrayListOf()

        for(i in 0 until numBubbles){
            mBubbleStatus.add(BubbleStatus.IDLE)
            labeled.add(0)
        }

        val contactThres = mBubbleRadius*2.1f
        var stackIndex :MutableList<Int> = arrayListOf()
        var label = 0

        for(i in 0 until numBubbles) {
            // if not labeled
            if(labeled[i] == 0) {
                label ++
                labeled[i] = label
                stackIndex.add(i)    // push
                var maxY = mBubblePositions[i * mDIM_POSITION + 1]

                while (stackIndex.isNotEmpty()) {
                    val idxCur = stackIndex[0]  // pop
                    stackIndex.removeAt(0)

                    val curX = mBubblePositions[idxCur * mDIM_POSITION + 0]
                    val curY = mBubblePositions[idxCur * mDIM_POSITION + 1]

                    for (idxSearch in 0 until numBubbles) {
                        if (0 == labeled[idxSearch]) {
                            val dx = mBubblePositions[idxSearch * mDIM_POSITION + 0] - curX
                            val dy = mBubblePositions[idxSearch * mDIM_POSITION + 1] - curY
                            val sqrDist = (dx * dx) + (dy * dy)
                            if (sqrDist <= contactThres * contactThres) {
                                stackIndex.add(idxSearch)       // push
                                labeled[idxSearch] = label
                                maxY = max(maxY, mBubblePositions[idxSearch * mDIM_POSITION + 1])
                            }
                        }
                    }
                }

                if(maxY < (mMaxWorldY - (mBubbleRadius * 1.01f))){
                    for(idxCheck in 0 until numBubbles) {
                        if(label == labeled[idxCheck]){
                            mBubbleStatus[idxCheck] = BubbleStatus.FALLING
                        }
                    }
                }
            }
        }

    }

    /**
     * This method returns the index of the bubble collides or overlaps with the bubble at (curX, curY).
     * */
    fun checkCollision(curX: Float, curY: Float, contactThreshold: Float) : Int{
        var idxCollide = -1

        // Test collision with other bubbles
        val curX = mProjectileObject.mInstancePositions[0]
        val curY = mProjectileObject.mInstancePositions[1]
        var closestSqrDist = 1000.0f    // The distance to the closest bubble.
        val numBubbles = mBubblePositions.count()/mDIM_POSITION

        for(idxBubble in 0 until numBubbles){
            val bubbleX = mBubblePositions[idxBubble * 4 + 0]
            val bubbleY = mBubblePositions[idxBubble * 4 + 1]
            val dx = curX - bubbleX
            val dy = curY - bubbleY
            val sqrDist = (dx*dx) + (dy*dy)
            if((sqrDist <= (contactThreshold*contactThreshold)) && (sqrDist < closestSqrDist)){

                val newX = if(curX < bubbleX){
                    bubbleX - (mBubbleDiameter * cos(mAlignAngle))
                }else{
                    bubbleX + (mBubbleDiameter * cos(mAlignAngle))
                }
                val newY = if(curY > bubbleY) {
                    bubbleY + (mBubbleDiameter * sin(mAlignAngle))
                }else{
                    bubbleY - (mBubbleDiameter * sin(mAlignAngle))
                }

                var valid : Boolean = true
                for(idxTest in 0 until numBubbles){
                    val testDX = mBubblePositions[idxTest * 4 + 0] - newX
                    val testDY = mBubblePositions[idxTest * 4 + 1] - newY
                    val testDist = testDX*testDX + testDY*testDY
                    if(testDist < contactThreshold*contactThreshold){
                        valid = false
                    }
                }
                if(valid) {
                    closestSqrDist = sqrDist
                    idxCollide = idxBubble
                }
            }
        }

        return idxCollide
    }

    /**
     * This method is called for each redraw of the view.
     * */
    override fun onDrawFrame(unused: GL10?) {
        val scratch = FloatArray(16)

        // Redraw background color
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)

        if(GameMode.INITIALIZE == mGameMode){
            for(i in 0 until mCols){
                mBubblePositions.add(mMinWorldX + mBubbleRadius + mBubbleDiameter*i.toFloat())
                mBubblePositions.add(mMaxWorldY - mBubbleRadius)
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
            mBubbleObjects.mNumInstances = mCols

            setReadyState()
        }

        if(GameMode.FIRING == mGameMode){
            val vx = -mVelocity * sin(mProjectileAngle)
            val vy = mVelocity * cos(mProjectileAngle)

            mProjectileObject.mInstancePositions[0] += vx
            mProjectileObject.mInstancePositions[1] += vy

            // Test boundary.
            if((mMinWorldX+mBubbleRadius) > mProjectileObject.mInstancePositions[0] || mProjectileObject.mInstancePositions[0] > (mMaxWorldX-mBubbleRadius)) {
                mProjectileAngle *= -1.0f
            }

            var newX = -100f
            var newY = -100f
            val curX = mProjectileObject.mInstancePositions[0]
            val curY = mProjectileObject.mInstancePositions[1]

            // If the bubble is out of world.
            if((curY + mBubbleRadius) >= mMaxWorldY){

                newX = floor(curX / mBubbleDiameter) * mBubbleDiameter + mBubbleRadius
                newY = mMaxWorldY - mBubbleRadius

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

                mGameMode = GameMode.EXPLODING
                mEventTime = 0.0f

            }else{
                // Test collision with other bubbles
                val contactThreshold = mBubbleDiameter * 0.9f
                val occupancyThreshold = mBubbleDiameter * 1.0f

                val idxCollide = checkCollision(curX, curY, contactThreshold)
                if(idxCollide >= 0){
                    val closestBubbleX = mBubblePositions[idxCollide * 4 + 0]
                    val closestBubbleY = mBubblePositions[idxCollide * 4 + 1]

                    if(curX < closestBubbleX){
                        newX = closestBubbleX - (mBubbleDiameter * cos(mAlignAngle))
                    }else{
                        newX = closestBubbleX + (mBubbleDiameter * cos(mAlignAngle))
                    }

                    if(curY > closestBubbleY) {
                        newY = closestBubbleY + (mBubbleDiameter * sin(mAlignAngle))
                    }else{
                        newY = closestBubbleY - (mBubbleDiameter * sin(mAlignAngle))
                    }

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

                    mGameMode = GameMode.EXPLODING
                    mEventTime = 0.0f
                }
            }

        }
        else if(GameMode.EXPLODING == mGameMode){
            if(mEventTime < 1.0f) {
                checkExplodingCondition((mBubblePositions.count() / mDIM_POSITION) - 1)
                checkFalling()
            }
            mEventTime += 1.0f

            if((mExplodingPositions.isEmpty()) or (mEventTime >= 50.0f)) {
                mGameMode = GameMode.FALLING
            }
        }
        else if(GameMode.FALLING == mGameMode) {

            var maxY = mMinWorldY - 1.0f
            val numBubbles = mBubblePositions.count()/mDIM_POSITION
            for(i in 0 until numBubbles) {
                if(BubbleStatus.FALLING == mBubbleStatus[i]){
                    mBubblePositions[i * mDIM_POSITION + 1] -= mVelocity * 2.0f
                    val y = mBubblePositions[i * mDIM_POSITION + 1]
                    maxY = max(maxY, y)
                }
            }

            if(maxY < mMinWorldY - mBubbleRadius) {
                // remove falling bubbles.
                for(i in numBubbles-1 downTo 0){
                    if(BubbleStatus.FALLING == mBubbleStatus[i]){
                        for(idxCoord in 3 downTo 0){
                            mBubblePositions.removeAt(i * mDIM_POSITION + idxCoord)
                            mBubbleColorsRGB.removeAt(i * mDIM_POSITION + idxCoord)
                        }
                        mBubbleColors.removeAt(i)
                    }
                }

                setReadyState()
            }
        }

        mBubbleObjects.mInstancePositions = mBubblePositions.toFloatArray()
        mBubbleObjects.mInstanceColors = mBubbleColorsRGB.toFloatArray()
        mBubbleObjects.mNumInstances = mBubblePositions.count() / mDIM_POSITION
        mBubbleObjects.draw(mPMatrix, mBubbleRadius)

        if(GameMode.READY == mGameMode) {
            mProjectileAngle = mFireAngle
            val startY = mMaxWorldY - (mRows.toFloat() * mBubbleDiameter)

            mTargetingLine.setOrigin(mOrigin)
            mTargetingLine.draw(mPMatrix, mFireAngle)
        }
        else if (GameMode.EXPLODING == mGameMode) {
            mParticleObjects.mInstancePositions = mExplodingPositions.toFloatArray()
            mParticleObjects.mInstanceColors = mExplodingColorsRGB.toFloatArray()
            mParticleObjects.mNumInstances = mExplodingPositions.count() / mDIM_POSITION

            mParticleObjects.draw(mPMatrix, mBubbleRadius, mEventTime)
        }

        if(GameMode.READY == mGameMode || GameMode.FIRING == mGameMode) {
            mProjectileObject.draw(mPMatrix, mBubbleRadius)
        }

    }

    /**
     * This method fires a bubble if the game is in ready mode.
     * */
    fun fireIfReady(){
        if(mGameMode == GameMode.READY) {
            mGameMode = GameMode.FIRING
        }
    }

    /**
     * This method computes the normalized image coordinate of the origin.
     * */
    fun computeOriginInImage(): FloatArray{
        val origin = computeOrigin()
        val x = origin[0]
        val y = origin[1] + mTopMarginY
        val z = origin[2]

        val wu = mPMatrix[0]*x + mPMatrix[1]*y + mPMatrix[2]*z  + mPMatrix[3]
        val wv = mPMatrix[4]*x + mPMatrix[5]*y + mPMatrix[6]*z  + mPMatrix[7]
        val  w = mPMatrix[8]*x + mPMatrix[9]*y + mPMatrix[10]*z + mPMatrix[11]

        val u = wu/w
        val v = wv/w

        return floatArrayOf(u, v)
    }

    /**
     * Called if the geometry of the view changes, for example whe the device's screen orientation changes.
     * */
    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {

        val viewWidth = min(width, height)
        val viewHeight = max(width, height)

        GLES30.glViewport(0, 0, viewWidth, viewHeight)

        val ratio: Float = viewHeight.toFloat() / viewWidth.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method

        // Coordinate
        //          y
        //          ^
        //          |
        //  -1 -----+-----> x +1
        //          |

        Matrix.frustumM(mProjectionMatrix, 0, -1f, 1f, -ratio, ratio, 3f, 7f)

        mMinWorldY = -ratio
        mMaxWorldY = ratio - mTopMarginY

        // update the origin
        mOrigin = computeOrigin()
    }

}


