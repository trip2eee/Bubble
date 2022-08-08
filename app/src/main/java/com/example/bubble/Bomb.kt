package com.example.bubble

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import com.example.bubble.models.BombModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class Bomb (texBomb: Bitmap) : Bubble() {

    override val mVertexCoords = BombModel.mVertexCoords
    override val mVertexNormal = BombModel.mVertexNormal
    private  val mVertexTexture = BombModel.mVertexTexture

    private val mTexBomb: Bitmap = texBomb
    private var mTextureHandle = IntBuffer.allocate(1)
    protected var mTextureCoordBuffer: FloatBuffer? = null
    protected var mTextureCoordHandle: Int = 0

    override var mNumInstances: Int = 1

    // Set color with red, green, blue and alpha (opacity) values
    override var mInstanceColors : MutableList<Float> = arrayListOf(
        0.63671875f, 0.76953125f, 0.22265625f, 0.1f,
    )
    override var mInstancePositions : MutableList<Float> = arrayListOf(
        0.0f,  0.0f, 0.0f, 0.0f,
        0.0f,  0.0f, 0.0f, 0.0f,
        0.0f,  0.0f, 0.0f, 0.0f,
    )
    override var mScale = floatArrayOf(
        0.05f, 0.05f, 0.05f, 1.0f,
    )

    override val mFragmentShaderCode =
        "#version 300 es\n" +
                "uniform sampler2D textureObject;" +
                "out vec4 fragColor;" +
                "in vec2 vTextureCoord;" +
                "in vec4 transVertexNormal;" +
                "void main() {" +
                "  vec4 diffuseLightIntensity = vec4(1.0, 1.0, 1.0, 1.0);" +
                "  vec4 inverseLightDirection = normalize(vec4(0.0, 0.0, 1.0, 0.0));" +
                "  float normalDotLight = max(0.0, dot(transVertexNormal, inverseLightDirection));" +
                "  vec4 vColorLight = vec4(0.5f, 0.5f, 0.5f, 1.0f);" +
                "  fragColor = texture(textureObject, vTextureCoord);" +
                "  fragColor += normalDotLight * vColorLight * diffuseLightIntensity;" +
                "  clamp(fragColor, 0.0, 1.0);" +
                "}"

    override val mVertexShaderCode =
        "#version 300 es\n" +
                "uniform mat4 uMVPMatrix;" +
                "uniform vec4 vScale;" +
                "layout(location = 0) in vec4 vPosition;" +
                "layout(location = 1) in vec3 vNormal;" +
                "layout(location = 2) in vec2 vTexCoord;" +

                "uniform vec4 vInstancePositions[100];" +
                "out vec4 transVertexNormal;" +
                "out vec2 vTextureCoord;" +
                "void main() {" +

                "  gl_Position = uMVPMatrix * ((vPosition*vScale) + vInstancePositions[gl_InstanceID]);" +
                "  transVertexNormal = normalize(uMVPMatrix * vec4(vNormal, 0.0));" +
                "  vTextureCoord = vTexCoord;" +
                "}"

    override fun initialize() {
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

        GLES30.glUseProgram(mProgram)

        GLES30.glGenTextures(1, mTextureHandle)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureHandle[0])

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, mTexBomb, 0)

        mTexBomb.recycle()  // To reclaim texture memory as soon as possible

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

        mTextureCoordBuffer = ByteBuffer.allocateDirect(mVertexTexture.size * 4).run {
                // use the device hardware's native byte order
                order(ByteOrder.nativeOrder())
                // create a floating point buffer from the ByteBuffer
                asFloatBuffer().apply {
                    put(mVertexTexture)		// add the coordinates to the FloatBuffer
                    position(0)	// set the buffer to read the first coordinate
                }
            }

        mVertexCount = mVertexCoords.size / COORDS_PER_VERTEX

    }

    override fun draw(mvpMatrix: FloatArray, scale: Float) {

        // Add program to OpenGL ES environment
        GLES30.glUseProgram(mProgram)

        // get handle to shape's transformation matrix
        GLES30.glGetUniformLocation(mProgram, "uMVPMatrix").also {
            // Pass the projection and view transformation to the shader
            GLES30.glUniformMatrix4fv(it, 1, false, mvpMatrix, 0)
        }

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition").also {
            GLES30.glEnableVertexAttribArray(it)
            GLES30.glVertexAttribPointer(it, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, mVertexStride, mVertexBuffer)
        }

        GLES30.glGetUniformLocation(mProgram, "vInstancePositions").also {
            GLES30.glUniform4fv(it, mNumInstances, mInstancePositions.toFloatArray(), 0)
        }

        mVertexNormalHandle = GLES30.glGetAttribLocation(mProgram, "vNormal").also {
            GLES30.glEnableVertexAttribArray(it)
            GLES30.glVertexAttribPointer(it, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, 0, mNormalBuffer)
        }

        mTextureCoordHandle = GLES30.glGetAttribLocation(mProgram, "vTexCoord").also {
            GLES30.glEnableVertexAttribArray(it)
            GLES30.glVertexAttribPointer(it, 2, GLES30.GL_FLOAT, false, 0, mTextureCoordBuffer)
        }

        mScale = floatArrayOf(scale, scale, scale, 1.0f)
        GLES30.glGetUniformLocation(mProgram, "vScale").also {
            GLES30.glUniform4fv(it, 1, mScale, 0)
        }

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureHandle[0])
        GLES30.glGetUniformLocation(mProgram, "textureObject").also {
            // Set color for drawing the triangle
            GLES30.glUniform1i(it, 0)
        }

        // Draw the triangle
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, mVertexCount, mNumInstances)

        // Disable vertex array
        GLES30.glDisableVertexAttribArray(mPositionHandle)
        GLES30.glDisableVertexAttribArray(mVertexNormalHandle)
        GLES30.glDisableVertexAttribArray(mTextureCoordHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {

        // create a vertex shader type (GLES30.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES30.GL_FRAGMENT_SHADER)
        return GLES30.glCreateShader(type).also { shader ->

            // add the source code to the shader and compile it
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)

            val log = GLES30.glGetShaderInfoLog(shader)
            Log.d("[Shader]", "Compile Log:")
            Log.d("[Shader]", log)
        }
    }
}