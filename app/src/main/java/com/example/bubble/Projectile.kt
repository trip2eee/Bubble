package com.example.bubble

class Projectile : Bubble() {

    override var mNumInstances: Int = 1

    // Set color with red, green, blue and alpha (opacity) values
    override var mInstanceColors = floatArrayOf(
        0.63671875f, 0.76953125f, 0.22265625f, 0.1f,
    )
    override var mInstancePositions = floatArrayOf(
        0.0f, -1.0f, 0.0f, 0.0f,
    )
}

