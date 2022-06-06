package com.example.bubble

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


//class GameActivity : AppCompatActivity() {
class GameActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)


        val gameView:GameView = findViewById(R.id.viewGame);


    }
}


