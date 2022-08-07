package com.example.bubble

class Bubble : BaseBubble() {

	override var mNumInstances: Int = 4

	// Set color with red, green, blue and alpha (opacity) values
	override var mInstanceColors = floatArrayOf(
		0.63671875f, 0.76953125f, 0.22265625f, 0.1f,
		0.76953125f, 0.63671875f, 0.22265625f, 0.1f,
		0.63671875f, 0.76953125f, 0.22265625f, 0.1f,
		0.63671875f, 0.22265625f, 0.76953125f, 0.1f,
	)
	override var mInstancePositions = floatArrayOf(
		-0.2f, -0.2f, 0.0f, 0.0f,
		+0.2f, -0.2f, 0.0f, 0.0f,
		-0.2f, +0.2f, 0.0f, 0.0f,
		+0.2f, +0.2f, 0.0f, 0.0f,
	)

}


