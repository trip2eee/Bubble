package com.example.bubble

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Random
import kotlin.concurrent.timer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.max
import kotlin.math.PI

enum class Mode{
    READY,
    FIRING,
    EXPLODING,
    FALLING,
    GAMEOVER
}
class GameView(context: Context, attrs: AttributeSet) : View(context, attrs), View.OnTouchListener {

    private val cellCols = 8
    private val cellRows = 20

    private val gapY = Bubble.diameter*sin(PI.toFloat() / 3f)
    private val gapX = Bubble.diameter*cos(PI.toFloat() / 3f)
    private val canvasWidth = cellCols*Bubble.diameter
    private val canvasHeight = (((cellRows - 1) * gapY)) + (Bubble.radius * 2f)

    private var canvasLeft = 0f
    private var canvasTop = 0f
    private var fireAngle = 0f
    private val maxFireAngle = PI.toFloat() / 4.0f // Maximum firing angle: 45 degrees
    private val fireSpeed = 50f
    private var fireDx = 0f
    private var fireDy = 0f

    private var gameMode: Mode = Mode.READY
    private lateinit var firedBubble : Bubble

    private val listBubble = ArrayList<Bubble>()
    private val listBubbleType = ArrayList<Int>()
    private val random = Random()

    init {
        listBubbleType.add(Color.rgb(255, 0, 0))
        listBubbleType.add(Color.rgb(0, 255, 0))
        listBubbleType.add(Color.rgb(0, 0, 255))
        listBubbleType.add(Color.rgb(255, 255, 0))

        listBubbleType.add(Color.rgb(200, 200, 200))
        listBubbleType.add(Color.rgb(50, 50, 50))

        this.setOnTouchListener(this)

        val timer = timer(period = 100){
            timerEvent()
        }
        createStage()
    }

    fun createStage(){
        listBubble.clear()

        // 1st row
        for(i in 0..7){
            val type = random.nextInt(listBubbleType.count())
            var bubble = Bubble(Bubble.radius + i*Bubble.radius*2, Bubble.radius, listBubbleType[type])
            listBubble.add(bubble)
        }

    }
    override fun onDraw(canvas: Canvas?){
        super.onDraw(canvas)

        val paint = Paint()
        paint.color = Color.YELLOW
        paint.textSize = 50f

        canvas?.drawText("Score", 50f, 50f, paint)

        canvasLeft = (this.width -canvasWidth) * 0.5f
        canvasTop = (this.height -canvasHeight) * 0.5f

        // boundary rect.
        paint.color = Color.DKGRAY
        canvas?.drawRect(canvasLeft, canvasTop, canvasLeft + canvasWidth, canvasTop + canvasHeight, paint)

        for(i in 0 until listBubble.count()){
            listBubble[i].draw(canvas, canvasLeft, canvasTop)
        }

        // draw firing direction.
        if(Mode.READY == gameMode) {
            paint.color = Color.RED
            paint.strokeWidth = 5f
            val firingArrowSize = 100f
            val fx0 = this.width * 0.5f
            val fy0 = canvasTop + canvasHeight
            val fx1 = fx0 - firingArrowSize * sin(fireAngle)
            val fy1 = fy0 - firingArrowSize * cos(fireAngle)
            canvas?.drawLine(fx0, fy0, fx1, fy1, paint)
        }
    }

    fun timerEvent(){
        if(Mode.FIRING == gameMode){

            var nextX = firedBubble.getX() - fireDx
            var nextY = firedBubble.getY() - fireDy

            if((nextX-Bubble.radius) <= 0)
            {
                fireDx *= -1f
                nextX = Bubble.radius

            }else if((nextX+Bubble.radius) >= canvasWidth)
            {
                fireDx *= -1f
                nextX = canvasWidth - Bubble.radius - 1f
            }

            // test for collision.
            // If out of screen
            if(nextY < 0){
                listBubble.remove(firedBubble); // remove from buffer.
                gameMode = Mode.READY
            }
            else
            {
                // for each bubble, perform collision test
                for(i in 0 until listBubble.count()){
                    if(firedBubble != listBubble[i]){
                        val dist = listBubble[i].computeDistance(nextX, nextY)
                        if(dist < Bubble.diameter){
                            gameMode = Mode.READY;

                            // if fired bubble is on the left of the bubble.
                            if(nextX <= listBubble[i].getX()){
                                nextX = listBubble[i].getX() - gapX
                            }else{
                                nextX = listBubble[i].getX() + gapX
                            }
                            nextY = listBubble[i].getY() + gapY
                            break
                        }
                    }
                }

                // compute the next position.
            }

            firedBubble.setX(nextX)
            firedBubble.setY(nextY)
            invalidate()
        }
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {

        val x = event?.getX() as Float
        val y = event?.getY() as Float

        if(event?.action == MotionEvent.ACTION_DOWN){


        }else if(event?.action == MotionEvent.ACTION_MOVE){
            // compute firing angle.
            val fx0 = this.width * 0.5f
            val fy0 = canvasTop + canvasHeight
            val dx = fx0 - x
            val dy = fy0 - y
            fireAngle = atan2(dx, dy)
            fireAngle = min(maxFireAngle, fireAngle)
            fireAngle = max(-maxFireAngle, fireAngle)

            this.invalidate()
        }else if(event?.action == MotionEvent.ACTION_UP){
            // If ready mode
            if(Mode.READY == gameMode) {
                val color_bubble = random.nextInt(listBubbleType.count())

                val fx0 = canvasWidth * 0.5f
                val fy0 = canvasHeight
                val bubble = Bubble(fx0, fy0, listBubbleType[color_bubble])
                firedBubble = bubble
                fireDx = fireSpeed * sin(fireAngle)
                fireDy = fireSpeed * cos(fireAngle)
                listBubble.add(bubble)

                gameMode = Mode.FIRING
            }
        }

        return true
    }

}