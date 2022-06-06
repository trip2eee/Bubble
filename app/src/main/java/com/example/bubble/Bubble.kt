package com.example.bubble

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sqrt

class Bubble (private var cx:Float, private var cy:Float, private val color:Int){

    companion object {
        const val radius: Float = 60f
        const val diameter = radius * 2f
    }

    private val red = Color.red(color);
    private val green = Color.green(color);
    private val blue = Color.blue(color);

    fun getColor():Int {
        return color;
    }

    fun getX():Float{
        return cx;
    }
    fun getY():Float{
        return cy;
    }
    fun setX(x:Float){
        cx = x;
    }
    fun setY(y:Float){
        cy = y;
    }

    public var left: Bubble? = null
    public var right: Bubble? = null

    fun draw(canvas: Canvas?, left: Float, top: Float) {
        val paint = Paint();
        paint.style = Paint.Style.STROKE;

        for( i in 0..10){
            val deltaR = i * 0.5f;
            val deltaColor = i * 20

            paint.color = Color.rgb(Math.min(255, red + deltaColor), Math.min(255, green + deltaColor), Math.min(255, blue + deltaColor));
            canvas?.drawCircle(cx+left, cy+top, radius -  deltaR, paint)
        }
    }

    // this method computes distance from this bubble to the other bubble.
    fun computeDistance(otherX:Float, otherY:Float): Float {
        val dx = cx - otherX
        val dy = cy - otherY

        val dist = sqrt((dx*dx) + (dy*dy))
        return dist
    }
}

