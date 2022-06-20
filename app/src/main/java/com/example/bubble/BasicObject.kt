package com.example.bubble

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


open class BasicObject {
    // number of coordinates per vertex in this array
    private val COORDS_PER_VERTEX = 3
    open val mVertexCoords = floatArrayOf(
        -0.013635f, 0.483420f, -0.888825f, 0.292166f, 0.099052f, -0.961172f, -0.089589f, 0.090364f, -1.000517f,
    )
    open val mVertexNormal = floatArrayOf(
        0.093400f, 0.255400f, -0.962300f, 0.093400f, 0.255400f, -0.962300f, 0.093400f, 0.255400f, -0.962300f,
    )

    open var mNumInstances: Int = 1

    // Set color with red, green, blue and alpha (opacity) values
    open var mInstanceColors = floatArrayOf(
        0.63671875f, 0.76953125f, 0.22265625f, 0.1f,
    )
    open var mInstancePositions = floatArrayOf(
        0.0f,  -1.0f, 0.0f, 0.0f,
    )
    open var mScale = floatArrayOf(
        0.05f, 0.05f, 0.05f, 1.0f,
    )

    private val mFragmentShaderCode =
        "#version 300 es\n" +
                "precision mediump float;" +
                "out vec4 fragColor;" +
                "in vec4 vColor;" +
                "in vec4 transVertexNormal;" +
                "void main() {" +
                "  vec4 diffuseLightIntensity = vec4(1.0, 1.0, 1.0, 1.0);" +
                "  vec4 inverseLightDirection = normalize(vec4(0.0, 1.0, 1.0, 0.0));" +
                "  float normalDotLight = max(0.0, dot(transVertexNormal, inverseLightDirection));" +
                "  fragColor = vColor;" +
                "  fragColor += normalDotLight * vColor * diffuseLightIntensity;" +
                "  clamp(fragColor, 0.0, 1.0);" +
                "}"

    private val mVertexShaderCode =
    // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "#version 300 es\n" +
                "uniform mat4 uMVPMatrix;" +
                "uniform vec4 vScale;" +
                "layout(location = 0) in vec4 vPosition;" +
                "layout(location = 1) in vec3 vNormal;" +

                "uniform vec4 vInstanceColors[100];" +
                "uniform vec4 vInstancePositions[100];" +
                "out vec4 transVertexNormal;" +
                "out vec4 vColor;" +
                "void main() {" +
                // the matrix must be included as a modifier of gl_Position
                // Note that the uMVPMatrix factor *must be first* in order
                // for the matrix multiplication product to be correct.
//                "  vec4 vScale = vec4(0.05, 0.05, 0.05, 1.0);" +
                "  gl_Position = uMVPMatrix * ((vPosition*vScale) + vInstancePositions[gl_InstanceID]);" +
                "  transVertexNormal = normalize(uMVPMatrix * vec4(vNormal, 0.0));" +
                "  vColor = vInstanceColors[gl_InstanceID];" +
                "}"

    // Use to access and set the view transformation
    private var vPMatrixHandle: Int = 0
    private var mProgram: Int = 0

    private var mVertexBuffer: FloatBuffer? = null
    private var mNormalBuffer: FloatBuffer? = null

    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var mVertexNormalHandle: Int = 0
    private var mInstancePositionsHandle: Int = 0
    private var mScaleHandle: Int = 0

    private val mVertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    private var mVertexCount: Int = 0

    fun initialize() {
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


        mVertexBuffer =
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

        mNormalBuffer =
            // (number of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(mVertexNormal.size * 4).run {
                // use the device hardware's native byte order
                order(ByteOrder.nativeOrder())
                // create a floating point buffer from the ByteBuffer
                asFloatBuffer().apply {
                    put(mVertexNormal)		// add the coordinates to the FloatBuffer
                    position(0)	// set the buffer to read the first coordinate
                }
            }

        mVertexCount = mVertexCoords.size / COORDS_PER_VERTEX

    }

    fun draw(mvpMatrix: FloatArray, scale: Float) {

        // Add program to OpenGL ES environment
        GLES30.glUseProgram(mProgram)

        // get handle to shape's transformation matrix
        vPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix")

        // Pass the projection and view transformation to the shader
        GLES30.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition").also {
            GLES30.glEnableVertexAttribArray(it)
            GLES30.glVertexAttribPointer(it, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, mVertexStride, mVertexBuffer)
        }

        mInstancePositionsHandle = GLES30.glGetUniformLocation(mProgram, "vInstancePositions").also {
            GLES30.glUniform4fv(it, mNumInstances, mInstancePositions, 0)
        }

        mVertexNormalHandle = GLES30.glGetAttribLocation(mProgram, "vNormal").also {
            GLES30.glEnableVertexAttribArray(it)
            GLES30.glVertexAttribPointer(it, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, 0, mNormalBuffer)
        }


        // get handle to fragment shader's vColor member
        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vInstanceColors").also {
            // Set color for drawing the triangle
            GLES30.glUniform4fv(it, mNumInstances, mInstanceColors, 0)
        }

        mScale = floatArrayOf(scale, scale, scale, 1.0f)
        mScaleHandle = GLES30.glGetUniformLocation(mProgram, "vScale").also {
            GLES30.glUniform4fv(it, 1, mScale, 0)
        }

        // Draw the triangle
//		GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, mVertexCount, mNumInstances)

        // Disable vertex array
        GLES30.glDisableVertexAttribArray(mPositionHandle)
        GLES30.glDisableVertexAttribArray(mInstancePositionsHandle)
        GLES30.glDisableVertexAttribArray(mColorHandle)
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

}