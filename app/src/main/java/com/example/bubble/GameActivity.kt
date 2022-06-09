package com.example.bubble

import android.app.Activity
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


//class GameActivity : AppCompatActivity() {
class GameActivity : Activity() {

    private lateinit var gLView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Create a GLSurfaceView instance and set it as the ContentView for this Activity.
        gLView = SurfaceView(this)
        setContentView(gLView)


    }
}


