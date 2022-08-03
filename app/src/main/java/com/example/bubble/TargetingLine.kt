package com.example.bubble

import android.opengl.GLES30
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.tan

class TargetingLine {
    private val COORDS_PER_VERTEX = 3

    private val mFragmentShaderCode =
        "#version 300 es\n" +
        "precision mediump float;" +
        "out vec4 fragColor;" +
        "in vec4 vColor;" +
        "void main() {" +
        "  fragColor = vColor;" +
        "}"

    private val mVertexShaderCode =
        "#version 300 es\n" +
        "uniform mat4 uRotationMatrix;" +
        "uniform mat4 uMVPMatrix;" +
        "layout(location = 0) in vec4 vPosition;" +
        "uniform vec4 vInstanceColors[10];" +
        "uniform vec4 vInstancePositions[10];" +
        "uniform vec4 vOrigin;" +
        "out vec4 vColor;" +
        "void main() {" +
        "  vec4 vScale = vec4(0.04, 0.04, 0.04, 1.0);" +
        "  vec4 vScaledPosition = vPosition * vScale;" +
        "  gl_Position = uMVPMatrix * ((uRotationMatrix*vScaledPosition) + (uRotationMatrix*vInstancePositions[gl_InstanceID] + vOrigin));" +
        "  vColor = vInstanceColors[gl_InstanceID];" +
        "}"


    // Use to access and set the view transformation
    private var vPMatrixHandle: Int = 0
    private var vRotationMatrixHandle: Int = 0

    private var mProgram: Int

    init {
        val vertexShader: Int = loadShader(GLES30.GL_VERTEX_SHADER, mVertexShaderCode)
        val fragmentShader: Int = loadShader(GLES30.GL_FRAGMENT_SHADER, mFragmentShaderCode)

        mProgram = GLES30.glCreateProgram().also {
            // add the vertex shader to program
            GLES30.glAttachShader(it, vertexShader)
            // add the fragment shader to program
            GLES30.glAttachShader(it, fragmentShader)
            // create OpenGL ES program executables
            GLES30.glLinkProgram(it)
        }

    }

    var mVertexCoords = floatArrayOf(
         0.0f,  1.0f, 0.0f,
         2.0f/tan(60.0f * PI.toFloat() / 180.0f), -1.0f, 0.0f,
         -2.0f/tan(60.0f * PI.toFloat() / 180.0f), -1.0f, 0.0f,
    )

    var mNumInstances: Int = 4

    // Set color with red, green, blue and alpha (opacity) values
    val mInstanceColors = floatArrayOf(
        0.0f, 0.0f, 1.0f, 1.0f,
        0.5f, 1.0f, 0.5f, 1.0f,
        1.0f, 1.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
    )
    var mInstancePositions = floatArrayOf(
        0.0f, 0.2f, 0.0f, 0.0f,
        0.0f, 0.4f, 0.0f, 0.0f,
        0.0f, 0.6f, 0.0f, 0.0f,
        0.0f, 0.8f, 0.0f, 0.0f,
    )

    var mOrigin = floatArrayOf(
        0.0f, 0.0f, 0.0f, 0.0f
    )

    private var mRotationMatrix = FloatArray(16)

    private var mVertexBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(mVertexCoords.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())
            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                put(mVertexCoords)		// add the coordinates to the FloatBuffer
                position(0)	// set the buffer to read the first coordinate
            }
        }

    fun loadShader(type: Int, shaderCode: String): Int {

        // create a vertex shader type (GLES30.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES30.GL_FRAGMENT_SHADER)
        return GLES30.glCreateShader(type).also { shader ->

            // add the source code to the shader and compile it
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)

            val log = GLES30.glGetShaderInfoLog(shader)
            print("shader compile")
            print(log)
        }
    }

    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mInstancePositionsHandle: Int = 0
    private var mOriginHandle: Int = 0

    private val vertexCount: Int = mVertexCoords.size / COORDS_PER_VERTEX
    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    fun draw(mvpMatrix: FloatArray, angle:Float) {

        Matrix.setRotateM(mRotationMatrix, 0, angle * 180.0f / PI.toFloat(), 0.0f, 0.0f, 1.0f)

        // Add program to OpenGL ES environment
        GLES30.glUseProgram(mProgram)

        vRotationMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uRotationMatrix").also{
            GLES30.glUniformMatrix4fv(it, 1, false, mRotationMatrix, 0)
        }

        // get handle to vertex shader's vOrigin member
        mOriginHandle = GLES30.glGetUniformLocation(mProgram, "vOrigin").also {
            // Set color for drawing the triangle
            GLES30.glUniform4fv(it, 1, mOrigin, 0)
        }

        // get handle to shape's transformation matrix
        vPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix").also{
            GLES30.glUniformMatrix4fv(it, 1, false, mvpMatrix, 0)
        }

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition").also {
            GLES30.glEnableVertexAttribArray(it)
            GLES30.glVertexAttribPointer(it, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, vertexStride, mVertexBuffer)
        }

        mInstancePositionsHandle = GLES30.glGetUniformLocation(mProgram, "vInstancePositions").also {
            GLES30.glUniform4fv(it, mNumInstances, mInstancePositions, 0)
        }

        // get handle to fragment shader's vColor member
        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vInstanceColors").also {
            // Set color for drawing the triangle
            GLES30.glUniform4fv(it, mNumInstances, mInstanceColors, 0)
        }


        // Draw the triangle
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, vertexCount, mNumInstances)

        // Disable vertex array
        GLES30.glDisableVertexAttribArray(mPositionHandle)
        GLES30.glDisableVertexAttribArray(mInstancePositionsHandle)
        GLES30.glDisableVertexAttribArray(mColorHandle)
    }

    fun setOrigin(origin: FloatArray){
        mOrigin = origin
    }
}

