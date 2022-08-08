package com.example.bubble

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*
import android.content.Context
import android.graphics.BitmapFactory

enum class GameMode{
    INITIALIZE,
    READY,
    FIRING,
    EXPLODING,
    FALLING,
    GAME_OVER,
    LEVEL_CLEAR,
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
    NUM_COLORS,
    BOMB,
}

class GameRenderer(context: Context) : GLSurfaceView.Renderer {

    // Model View Projection Matrix
    private val mPMatrix = FloatArray(16)

    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)

    private lateinit var mBubbleObjects: Bubble
    private lateinit var mTargetingLine: TargetingLine
    private lateinit var mParticleObjects: Particle
    private lateinit var mTextObject: TextObject
    private lateinit var mBombObject: Bomb

    private var mBubblePositions: MutableList<Float> = arrayListOf()
    private var mBubbleTypes: MutableList<Int> = arrayListOf()
    private var mBubbleColorsRGB : MutableList<Float> = arrayListOf()
    private var mBubbleStatus : MutableList<BubbleStatus> = arrayListOf()

    private var mProjectilePositions: MutableList<Float> = arrayListOf(
        0.0f, 0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 0.0f, 1.0f,
    )

    private var mProjectileColorsRGB : MutableList<Float> = arrayListOf(
        0.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 0.0f,
    )

    private var mExplodingPositions: MutableList<Float> = arrayListOf()
    private var mExplodingColorsRGB : MutableList<Float> = arrayListOf()

    private val random = Random()
    private var mCurType: Int = random.nextInt(BubbleType.NUM_COLORS.ordinal)
    private var mCurTypeTemp = mCurType
    private var mNextType: Int = random.nextInt(BubbleType.NUM_COLORS.ordinal)

    private var mFireAngle = 0.0f
    private var mProjectileAngle = 0.0f
    private var mEventTime = 0.0f
    private var mGameMode : GameMode = GameMode.INITIALIZE

    private val mAlignAngle = 60.0f * PI.toFloat() / 180.0f
    private val mTopMarginY = 0.15f
    private val mDimPosition = 4
    private val mDimColor = 4

    private val mMinBubblesToExplode = 3
    private val mCols = 8
    private val mRows = 12

    private val mVelocity = 0.02f

    private var mMaxWorldY = 1.0f
    private var mMinWorldY = -1.0f
    private var mMaxWorldX = 1.0f
    private var mMinWorldX = -1.0f
    private var mWorldWidth : Float = mMaxWorldX - mMinWorldX
    private var mBubbleRadius = (mWorldWidth * 0.5f) / mCols.toFloat()
    private var mBubbleDiameter = mBubbleRadius * 2.0f
    private var mContactThreshold = mBubbleDiameter * 0.9f
    private var mWorldHeight : Float = mBubbleRadius + (sin(mAlignAngle)*mBubbleDiameter*mRows)
    private var mGameOverHeight : Float = mBubbleRadius + (sin(mAlignAngle)*mBubbleDiameter*(mRows-1.0f))

    private var mOrigin : FloatArray = computeOrigin()

    private val mTexFont : Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.font)
    private val mTexBomb : Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.texture_bomb)
    private var mScore: Int = 0
    private var mLevel: Int = 1
    private var mNumBombs: Int = 3

    /**
     * This method isc alled once to set up the view's OpenGL ES environment.
     * */
    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        // Set the background frame color
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)       // Ignore back face
        GLES30.glFrontFace(GLES30.GL_CW)        // Front face: Clockwise

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        mBubbleObjects = Bubble()
        mBubbleObjects.initialize()

        mParticleObjects = Particle()
        mParticleObjects.initialize()

        mTargetingLine = TargetingLine()

        mTextObject = TextObject(mTexFont)

        mBombObject = Bomb(mTexBomb)
        mBombObject.initialize()

    }

    private fun computeOrigin(): FloatArray{
        return floatArrayOf(0f, mMaxWorldY - mWorldHeight, 0f, 0f)
    }

    private fun bubbleTypeToRGB(color: Int): FloatArray {
        var colorRGB = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
        val alpha = 0.8f
        if(color == BubbleType.GREEN.ordinal) {
            colorRGB = floatArrayOf(0.53671875f, 0.76953125f, 0.22265625f, alpha)
        }else if(color == BubbleType.RED.ordinal) {
            colorRGB = floatArrayOf(0.76953125f, 0.63671875f, 0.22265625f, alpha)
        }else if(color == BubbleType.BLUE.ordinal) {
            colorRGB = floatArrayOf(0.63671875f, 0.22265625f, 0.76953125f, alpha)
        }else if(color == BubbleType.YELLOW.ordinal) {
            colorRGB = floatArrayOf(0.22265625f, 0.63671875f, 0.76953125f, alpha)
        }else if(color == BubbleType.BOMB.ordinal) {
            colorRGB = floatArrayOf(1.0f, 0.0f, 0.0f, alpha)
        }

        return colorRGB
    }

    private fun checkGameOver(){
        // find the bubble at the lowest position.
        var lowestY = 0.0f

        for(idxBubble in 0 until mBubblePositions.count()/mDimPosition){
            val bubbleY = mBubblePositions[idxBubble * mDimPosition + 1]
            if(bubbleY < lowestY){
                lowestY = bubbleY
            }
        }

        if(lowestY <= (mMaxWorldY - mGameOverHeight + mBubbleRadius*0.1f)){
            mGameMode = GameMode.GAME_OVER

            for(idxBubble in 0 until mBubblePositions.count()/mDimPosition){
                mBubbleColorsRGB[idxBubble * mDimColor + 0] = 0.5f
                mBubbleColorsRGB[idxBubble * mDimColor + 1] = 0.5f
                mBubbleColorsRGB[idxBubble * mDimColor + 2] = 0.5f
                mBubbleColorsRGB[idxBubble * mDimColor + 3] = 0.5f
            }
        }
    }

    private fun checkStageClear(){
        if(mBubblePositions.size == 0){
            mGameMode = GameMode.LEVEL_CLEAR
        }
    }

    private fun setReadyState(){
        checkGameOver()
        if(GameMode.GAME_OVER != mGameMode){
            checkStageClear()
            if(GameMode.LEVEL_CLEAR != mGameMode) {
                mGameMode = GameMode.READY

                mProjectilePositions[0] = mOrigin[0]
                mProjectilePositions[1] = mOrigin[1]
                mProjectilePositions[2] = 0.0f
                mProjectilePositions[3] = 0.0f

                mProjectilePositions[4] = mOrigin[0]
                mProjectilePositions[5] = mOrigin[1] - mBubbleDiameter - 0.05f
                mProjectilePositions[6] = 0.0f
                mProjectilePositions[7] = 0.0f

                mCurType = mNextType
                mNextType = random.nextInt(BubbleType.NUM_COLORS.ordinal)

                mProjectileColorsRGB.clear()
                val colorNextRGB = bubbleTypeToRGB(mCurType)
                mProjectileColorsRGB.addAll(colorNextRGB.toTypedArray())

                val colorNextNextRGB = bubbleTypeToRGB(mNextType)
                mProjectileColorsRGB.addAll(colorNextNextRGB.toTypedArray())

            }
        }
    }

    private fun checkExplodingCondition(idxStart:Int) {

        mExplodingColorsRGB = arrayListOf()
        mExplodingPositions = arrayListOf()

        var numToExplode = 1    // The number of bubbles to be exploded including the current bubble.
        val numBubbles = mBubblePositions.count() / mDimPosition

        // Initialize bubble conditions to be idle
        mBubbleStatus.clear()
        for (i in 0 until numBubbles) {
            mBubbleStatus.add(BubbleStatus.IDLE)
        }

        if(BubbleType.NUM_COLORS.ordinal > mCurType) {
            val stackIndex: MutableList<Int> = arrayListOf()
            val contactThres = mBubbleRadius * 2.1f

            mBubbleStatus[idxStart] = BubbleStatus.EXPLODING
            stackIndex.add(idxStart)    // push
            while (stackIndex.isNotEmpty()) {
                val idxCur = stackIndex[0]  // pop
                stackIndex.removeAt(0)

                val curX = mBubblePositions[idxCur * mDimPosition + 0]
                val curY = mBubblePositions[idxCur * mDimPosition + 1]

                for (idxSearch in 0 until numBubbles) {
                    if (BubbleStatus.IDLE == mBubbleStatus[idxSearch] &&
                        mBubbleTypes[idxCur] == mBubbleTypes[idxSearch]
                    ) {
                        val dx = mBubblePositions[idxSearch * mDimPosition + 0] - curX
                        val dy = mBubblePositions[idxSearch * mDimPosition + 1] - curY
                        val sqrDist = (dx * dx) + (dy * dy)
                        if (sqrDist <= contactThres * contactThres) {
                            stackIndex.add(idxSearch)       // push
                            mBubbleStatus[idxSearch] = BubbleStatus.EXPLODING
                            numToExplode++
                        }
                    }
                }
            }
            // If the number of bubbles to be exploded is less than the threshold, ignore.
            if(numToExplode < mMinBubblesToExplode){
                numToExplode = 0
            }
        }else{
            // if Bomb
            val curX = mProjectilePositions[0]
            val curY = mProjectilePositions[1]

            for (idxSearch in 0 until numBubbles) {
                if (BubbleStatus.IDLE == mBubbleStatus[idxSearch]) {
                    val dx = mBubblePositions[idxSearch * mDimPosition + 0] - curX
                    val dy = mBubblePositions[idxSearch * mDimPosition + 1] - curY
                    val sqrDist = (dx * dx) + (dy * dy)
                    val mBombRange = mBubbleDiameter * 1.5f
                    if (sqrDist <= mBombRange*mBombRange) {
                        mBubbleStatus[idxSearch] = BubbleStatus.EXPLODING
                        numToExplode++
                    }
                }
            }
        }

        // Exploding condition
        if(numToExplode >= 1){
            increaseScore(numToExplode)
            // If the current bubble is a bomb, add particles for the bomb.
            if(BubbleType.BOMB.ordinal == mCurType) {
                val colorBomb = bubbleTypeToRGB(mCurType)
                mExplodingPositions.add(mProjectilePositions[0])
                mExplodingPositions.add(mProjectilePositions[1])
                mExplodingPositions.add(mProjectilePositions[2])
                mExplodingPositions.add(mProjectilePositions[3])
                mExplodingColorsRGB.addAll(colorBomb.toTypedArray())
            }

            // remove bubbles with exploding state.
            for(i in numBubbles-1 downTo 0){
                if(BubbleStatus.EXPLODING == mBubbleStatus[i]){
                    for(idxCoord in 0 until mDimPosition){
                        mExplodingPositions.add(mBubblePositions[i * mDimPosition + idxCoord])
                        mExplodingColorsRGB.add(mBubbleColorsRGB[i * mDimPosition + idxCoord])
                    }
                    for(idxCoord in (mDimPosition-1) downTo 0){
                        mBubblePositions.removeAt(i * mDimPosition + idxCoord)
                        mBubbleColorsRGB.removeAt(i * mDimPosition + idxCoord)
                    }
                    mBubbleTypes.removeAt(i)
                }
            }
        }else{
            for(i in 0 until numBubbles) {
                mBubbleStatus[i] = BubbleStatus.IDLE
            }
        }
    }

    /**
     * This method increases score by s. It also increases the number of bombs at every 10 points.
     * */
    private fun increaseScore(s:Int){
        for(i in 0 until s) {
            mScore++
            if(mScore % 10 == 0){
                mNumBombs++
            }
        }
    }
    private fun checkFalling(){
        // mark conditions
        val numBubbles = mBubblePositions.count()/mDimPosition
        mBubbleStatus.clear()

        val labeled : MutableList<Int> = arrayListOf()

        for(i in 0 until numBubbles){
            mBubbleStatus.add(BubbleStatus.IDLE)
            labeled.add(0)
        }

        val contactThres = mBubbleRadius*2.1f
        val stackIndex :MutableList<Int> = arrayListOf()
        var label = 0

        for(i in 0 until numBubbles) {
            // if not labeled
            if(labeled[i] == 0) {
                label ++
                labeled[i] = label
                stackIndex.add(i)    // push
                var maxY = mBubblePositions[i * mDimPosition + 1]

                while (stackIndex.isNotEmpty()) {
                    val idxCur = stackIndex[0]  // pop
                    stackIndex.removeAt(0)

                    val curX = mBubblePositions[idxCur * mDimPosition + 0]
                    val curY = mBubblePositions[idxCur * mDimPosition + 1]

                    for (idxSearch in 0 until numBubbles) {
                        if (0 == labeled[idxSearch]) {
                            val dx = mBubblePositions[idxSearch * mDimPosition + 0] - curX
                            val dy = mBubblePositions[idxSearch * mDimPosition + 1] - curY
                            val sqrDist = (dx * dx) + (dy * dy)
                            if (sqrDist <= contactThres * contactThres) {
                                stackIndex.add(idxSearch)       // push
                                labeled[idxSearch] = label
                                maxY = max(maxY, mBubblePositions[idxSearch * mDimPosition + 1])
                            }
                        }
                    }
                }

                if(maxY < (mMaxWorldY - (mBubbleRadius * 1.01f))){
                    for(idxCheck in 0 until numBubbles) {
                        if(label == labeled[idxCheck]){
                            mBubbleStatus[idxCheck] = BubbleStatus.FALLING
                            increaseScore(1)
                        }
                    }
                }
            }
        }
    }

    /**
     * This method returns the index of the bubble collides or overlaps with the bubble at (curX, curY).
     * */
    private fun checkCollision(curX: Float, curY: Float, contactThreshold: Float) : Int{
        var idxCollide = -1

        // Test collision with other bubbles
        var closestSqrDist = 1000.0f    // The distance to the closest bubble.
        val numBubbles = mBubblePositions.count()/mDimPosition

        for(idxBubble in 0 until numBubbles){
            val bubbleX = mBubblePositions[idxBubble * mDimPosition + 0]
            val bubbleY = mBubblePositions[idxBubble * mDimPosition + 1]
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

                var valid = true
                for(idxTest in 0 until numBubbles){
                    val testDX = mBubblePositions[idxTest * mDimPosition + 0] - newX
                    val testDY = mBubblePositions[idxTest * mDimPosition + 1] - newY
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

        // Redraw background color
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)

        if(GameMode.INITIALIZE == mGameMode){

            var x = mBubbleRadius
            var y = mBubbleRadius
            for(i in 0 until mLevel){
                mBubblePositions.add(mMinWorldX + x)
                mBubblePositions.add(mMaxWorldY - y)
                mBubblePositions.add(0.0f)
                mBubblePositions.add(0.0f)

                x += mBubbleDiameter
                val eps = 0.001
                if(x >= (mMaxWorldX-mMinWorldX)+mBubbleRadius - eps){
                    x = mBubbleDiameter
                    y += mBubbleDiameter * sin(mAlignAngle)
                }else if(x >= (mMaxWorldX-mMinWorldX) - eps){
                    x = mBubbleRadius
                    y += mBubbleDiameter * sin(mAlignAngle)
                }

                val color = i % BubbleType.NUM_COLORS.ordinal
                mBubbleTypes.add(color)

                val colorRGB = bubbleTypeToRGB(color)
                mBubbleColorsRGB.addAll(colorRGB.toTypedArray())
            }

            mBubbleObjects.mInstancePositions.clear()
            mBubbleObjects.mInstanceColors.clear()
            mBubbleObjects.mInstancePositions.addAll(mBubblePositions)
            mBubbleObjects.mInstanceColors.addAll(mBubbleColorsRGB)
            mBubbleObjects.mNumInstances = mCols

            setReadyState()
        }

        if(GameMode.FIRING == mGameMode){
            val vx = -mVelocity * sin(mProjectileAngle)
            val vy = mVelocity * cos(mProjectileAngle)

            mProjectilePositions[0] += vx
            mProjectilePositions[1] += vy

            // Test boundary.
            if((mMinWorldX+mBubbleRadius) > mProjectilePositions[0] || mProjectilePositions[0] > (mMaxWorldX-mBubbleRadius)) {
                // If normal bubble, bounce
                if(BubbleType.NUM_COLORS.ordinal > mCurType) {
                    mProjectileAngle *= -1.0f
                }else{
                    // if bomb, explode
                    mGameMode = GameMode.EXPLODING
                    mEventTime = 0.0f
                }
            }

            val curX = mProjectilePositions[0]
            val curY = mProjectilePositions[1]

            // If the bubble is out of world.
            if((curY + mBubbleRadius) >= mMaxWorldY){
                // If normal bubble
                if(BubbleType.NUM_COLORS.ordinal > mCurType) {
                    val newX = floor(curX / mBubbleDiameter) * mBubbleDiameter + mBubbleRadius
                    val newY = mMaxWorldY - mBubbleRadius

                    mBubblePositions.add(newX)
                    mBubblePositions.add(newY)
                    mBubblePositions.add(0.0f)
                    mBubblePositions.add(0.0f)

                    mBubbleTypes.add(mCurType)
                    val colorRGB = bubbleTypeToRGB(mCurType)
                    mBubbleColorsRGB.addAll(colorRGB.toTypedArray())
                }
                mGameMode = GameMode.EXPLODING
                mEventTime = 0.0f

            }else{
                // Test collision with other bubbles
                val idxCollide = checkCollision(curX, curY, mContactThreshold)
                if(idxCollide >= 0){
                    if(BubbleType.NUM_COLORS.ordinal > mCurType) {
                        val closestBubbleX = mBubblePositions[idxCollide * mDimPosition + 0]
                        val closestBubbleY = mBubblePositions[idxCollide * mDimPosition + 1]

                        val newX = if(curX < closestBubbleX){
                            closestBubbleX - (mBubbleDiameter * cos(mAlignAngle))
                        }else{
                            closestBubbleX + (mBubbleDiameter * cos(mAlignAngle))
                        }

                        val newY = if(curY > closestBubbleY) {
                            closestBubbleY + (mBubbleDiameter * sin(mAlignAngle))
                        }else{
                            closestBubbleY - (mBubbleDiameter * sin(mAlignAngle))
                        }

                        mBubblePositions.add(newX)
                        mBubblePositions.add(newY)
                        mBubblePositions.add(0.0f)
                        mBubblePositions.add(0.0f)

                        mBubbleTypes.add(mCurType)
                        val colorRGB = bubbleTypeToRGB(mCurType)
                        mBubbleColorsRGB.addAll(colorRGB.toTypedArray())
                    }

                    mGameMode = GameMode.EXPLODING
                    mEventTime = 0.0f
                }
            }
        }
        else if(GameMode.EXPLODING == mGameMode){
            if(mEventTime < 1.0f) {
                checkExplodingCondition((mBubblePositions.count() / mDimPosition) - 1)
                checkFalling()
            }
            mEventTime += 1.0f

            if((mExplodingPositions.isEmpty()) or (mEventTime >= 50.0f)) {
                mGameMode = GameMode.FALLING
            }
        }
        else if(GameMode.FALLING == mGameMode) {

            var maxY = mMinWorldY - 1.0f
            val numBubbles = mBubblePositions.count()/mDimPosition
            for(i in 0 until numBubbles) {
                if(BubbleStatus.FALLING == mBubbleStatus[i]){
                    mBubblePositions[i * mDimPosition + 1] -= mVelocity * 2.0f
                    val y = mBubblePositions[i * mDimPosition + 1]
                    maxY = max(maxY, y)
                }
            }

            if(maxY < mMinWorldY - mBubbleRadius) {
                // remove falling bubbles.
                for(i in numBubbles-1 downTo 0){
                    if(BubbleStatus.FALLING == mBubbleStatus[i]){
                        for(idxCoord in 3 downTo 0){
                            mBubblePositions.removeAt(i * mDimPosition + idxCoord)
                            mBubbleColorsRGB.removeAt(i * mDimPosition + idxCoord)
                        }
                        mBubbleTypes.removeAt(i)
                    }
                }
                setReadyState()
            }
        }
        mBubbleObjects.mInstancePositions.clear()
        mBubbleObjects.mInstanceColors.clear()
        mBubbleObjects.mInstancePositions.addAll(mBubblePositions)
        mBubbleObjects.mInstanceColors.addAll(mBubbleColorsRGB)
        mBubbleObjects.mNumInstances = mBubblePositions.count() / mDimPosition

        // Bomb selection button.
        mBombObject.mInstancePositions[0] = mOrigin[0] + mBubbleDiameter + 0.1f
        mBombObject.mInstancePositions[1] = mOrigin[1] - mBubbleDiameter - 0.05f
        mBombObject.mNumInstances = 1

        if(GameMode.READY == mGameMode || GameMode.FIRING == mGameMode) {

            if(mCurType < BubbleType.NUM_COLORS.ordinal) {
                mBubbleObjects.mInstancePositions.addAll(mProjectilePositions.toTypedArray())
                mBubbleObjects.mInstanceColors.addAll(mProjectileColorsRGB.toTypedArray())
                mBubbleObjects.mNumInstances += 2
            }else{
                for(i in 0 until mDimPosition){
                    mBubbleObjects.mInstancePositions.add(mProjectilePositions[mDimPosition + i])
                }
                for(i in 0 until mDimColor){
                    mBubbleObjects.mInstanceColors.add(mProjectileColorsRGB[mDimPosition + i])
                }
                mBubbleObjects.mNumInstances ++

                mBombObject.mInstancePositions[4] = mProjectilePositions[0]
                mBombObject.mInstancePositions[5] = mProjectilePositions[1]
                mBombObject.mNumInstances++
            }
            mBombObject.draw(mPMatrix, mBubbleRadius)
            val strNumBomb = "%d".format(mNumBombs)
            val widthNumBomb = 0.02f
            val heightNumBomb = 0.05f
            mTextObject.draw(mPMatrix, strNumBomb, mOrigin[0] + mBubbleDiameter * 2.5f, mOrigin[1] - mBubbleDiameter, 0.0f, widthNumBomb, heightNumBomb, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f))
        }

        mBubbleObjects.draw(mPMatrix, mBubbleRadius)

        if(GameMode.READY == mGameMode) {
            mProjectileAngle = mFireAngle
            mTargetingLine.setOrigin(mOrigin)
            mTargetingLine.draw(mPMatrix, mFireAngle)
        }
        else if (GameMode.EXPLODING == mGameMode) {
            mParticleObjects.mInstancePositions = mExplodingPositions
            mParticleObjects.mInstanceColors = mExplodingColorsRGB
            mParticleObjects.mNumInstances = mExplodingPositions.count() / mDimPosition

            mParticleObjects.draw(mPMatrix, mBubbleRadius, mEventTime)
        }

        val widthScore = 0.075f
        val heightScore = 0.15f
        val strScore = "%05d".format(mScore)
        mTextObject.draw(mPMatrix, strScore, mMinWorldX, mMaxWorldY + mTopMarginY, 0.0f, widthScore, heightScore, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f))

        val widthLevel = 0.05f
        val heightLevel = 0.1f
        val strLevel = "Lv.%d".format(mLevel)
        mTextObject.draw(mPMatrix, strLevel, mMinWorldX + widthScore*7.0f, mMaxWorldY + mTopMarginY, 0.0f, widthLevel, heightLevel, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f))

        if(GameMode.GAME_OVER == mGameMode){
            val widthGameOver = 0.075f
            val heightGameOver = 0.15f
            mTextObject.draw(mPMatrix, "Game Over!", -widthGameOver * 5.0f, 0.0f, 0.0f, widthGameOver, heightGameOver, floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f))
        }

        if(GameMode.LEVEL_CLEAR == mGameMode){
            val widthLevelClear = 0.075f
            val heightLevelClear = 0.15f
            mTextObject.draw(mPMatrix, "Level Clear!", -widthLevelClear * 6.0f, 0.0f, 0.0f, widthLevelClear, heightLevelClear, floatArrayOf(0.0f, 0.1f, 1.0f, 1.0f))
        }
    }

    /**
     * Screen touch move event handler
     * */
    fun screenTouchMoveEvent(x:Float, y:Float){

        val origin = computeOrigin()
        val dx: Float = origin[0] - x
        val dy: Float = y - origin[1]

        if(dy > 0) {
            mFireAngle = max(min(atan2(dx, dy), 1.0f), -1.0f)
        }
    }

    /**
     * Screen touch up event handler
     * */
    fun screenTouchUpEvent(x:Float, y:Float){
        if(mGameMode == GameMode.READY) {

            val origin = computeOrigin()
            val dy: Float = y - origin[1]

            if(dy > 0) {
                mGameMode = GameMode.FIRING

                if(BubbleType.BOMB.ordinal == mCurType && mNumBombs > 0){
                    mNumBombs--
                }
            }else{
                // Check if bomb icon is touched.
                val dx_bomb = x - mBombObject.mInstancePositions[0]
                val dy_bomb = y - mBombObject.mInstancePositions[1]
                val sqr_dist_bomb = dx_bomb*dx_bomb + dy_bomb*dy_bomb
                if(sqr_dist_bomb < mBubbleRadius*mBubbleRadius) {
                    // If the current type is not bomb
                    if(BubbleType.BOMB.ordinal != mCurType && mNumBombs > 0) {
                        mCurTypeTemp = mCurType
                        mCurType = BubbleType.BOMB.ordinal
                    }
                }else{
                    mCurType = mCurTypeTemp
                }
            }
        }
        else if(mGameMode == GameMode.LEVEL_CLEAR){
            mLevel += 1
            mGameMode = GameMode.INITIALIZE
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

        val ratio: Float = viewWidth.toFloat() / viewHeight.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method

        // Coordinate
        //              y +1
        //              ^
        //              |
        //  -ratio -----+-----> x +ratio
        //              |
        //              | -1
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f,3f, 7f)

        mMinWorldX = -ratio
        mMaxWorldX = ratio

        mMaxWorldY = 1.0f - mTopMarginY
        mMinWorldY = -1.0f

        mWorldWidth = mMaxWorldX - mMinWorldX
        mBubbleRadius = (mWorldWidth * 0.5f) / mCols.toFloat()
        mBubbleDiameter = mBubbleRadius * 2.0f
        mContactThreshold = mBubbleDiameter * 0.9f
        mWorldHeight = mBubbleRadius + (sin(mAlignAngle)*mBubbleDiameter*mRows)
        mGameOverHeight = mBubbleRadius + (sin(mAlignAngle)*mBubbleDiameter*(mRows-1.0f))

        // update the origin
        mOrigin = computeOrigin()
    }

}


