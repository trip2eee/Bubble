package com.example.bubble

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class TextObject(texFont: Bitmap) {

    private val COORDS_PER_VERTEX = 4

    private val mFragmentShaderCode =
        "#version 300 es\n" +
                "precision mediump float;" +
                "uniform sampler2D textureObject;" +
                "out vec4 fragColor;" +
                "in vec4 vColor;" +
                "in vec2 vTextureCoord;" +
                "void main() {" +
                "  vec4 texColor = texture(textureObject, vTextureCoord);" +
                "  fragColor = vec4(texColor[0]*vColor[0], texColor[1]*vColor[1], texColor[2]*vColor[2], texColor[0]);" +
                "}"

    private val mVertexShaderCode =
        "#version 300 es\n" +
                "uniform mat4 uMVPMatrix;" +
                "layout(location = 0) in vec4 vPosition;" +
                "uniform vec4 vInstanceColors[10];" +
                "uniform vec4 vInstancePositions[10];" +
                "uniform vec4 vOrigin;" +
                "out vec4 vColor;" +
                "out vec2 vTextureCoord;" +
                "void main() {" +
                "  vec4 vScaledPosition = vec4(vPosition[0], vPosition[1], 0.0, 1.0);" +
                "  gl_Position = uMVPMatrix * (vScaledPosition + vInstancePositions[gl_InstanceID] + vOrigin);" +
                "  vColor = vInstanceColors[gl_InstanceID];" +
                "  vTextureCoord = vec2(vPosition[2], vPosition[3]);" +
                "}"


    // Use to access and set the view transformation
    private var vPMatrixHandle: Int = 0
    private val mTexFont: Bitmap = texFont

    private var mProgram: Int
    private var mTextureHandle = IntBuffer.allocate(1)

    var buff_texture = FloatArray(100 * 100 * 3)


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
        GLES30.glUseProgram(mProgram)

        // generate fake texture
        for(i in 0 until 100*100){
            buff_texture[i * 3 + 0] = 0.0f
            buff_texture[i * 3 + 1] = 1.0f
            buff_texture[i * 3 + 2] = 0.0f
        }

        GLES30.glGenTextures(1, mTextureHandle)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureHandle[0])


        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, mTexFont, 0)
        var temp_texture = FloatBuffer.wrap(buff_texture)
//        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGB32F, 100, 100, 0, GLES30.GL_RGB, GLES30.GL_FLOAT, temp_texture)
//        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)


        mTexFont.recycle()  // TODO: to check.
    }

    var mNumInstances: Int = 1

    // Set color with red, green, blue and alpha (opacity) values
    var mInstanceColors = floatArrayOf(
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
    )
    var mInstancePositions = floatArrayOf(
        0.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 0.0f,
    )

    var mOrigin = floatArrayOf(
        0.0f, 0.0f, 0.0f, 0.0f
    )

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
    private var mTextureBuffer: Int = 0
    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    fun draw(mvpMatrix: FloatArray, text:String, x:Float, y:Float, z:Float, w:Float, h:Float, color:FloatArray) {

        var vertexCoords : MutableList<Float> = arrayListOf()
        mInstanceColors[0] = color[0]
        mInstanceColors[1] = color[1]
        mInstanceColors[2] = color[2]
        mInstanceColors[3] = color[3]

        // x, y, u, v
        // Vertex coordinates (x,y)
        // 1. (0, 0)
        //    (0,-1)   (1,-1)
        // 2. (0, 0)   (1, 0)
        //             (1,-1)
        // Texture coordinates (u, v)
        // 1. (0, 1)
        //    (0, 0)    (1,0)
        // 2. (0, 1)    (1,1)
        //              (1,0)
        //

        for(idxChar in 0 until text.length){
            val code:Int = text[idxChar].code
            val idx = code - 32
            val row:Float = (idx / 32).toFloat()
            val col:Float = (idx % 32).toFloat()

            val charWidth: Float = 1.0f/32.0f
            val charHeight: Float = 1.0f / 3.0f

            // triangle 1
            vertexCoords.add(idxChar.toFloat() * w)
            vertexCoords.add(0.0f)
            vertexCoords.add(col * charWidth)
            vertexCoords.add(row * charHeight)

            vertexCoords.add(idxChar.toFloat() * w + w)
            vertexCoords.add(-h)
            vertexCoords.add(col * charWidth + charWidth)
            vertexCoords.add(row * charHeight + charHeight)

            vertexCoords.add(idxChar.toFloat() * w)
            vertexCoords.add(-h)
            vertexCoords.add(col * charWidth)
            vertexCoords.add(row * charHeight + charHeight)

            // triangle 2
            vertexCoords.add(idxChar.toFloat() * w)
            vertexCoords.add(0.0f)
            vertexCoords.add(col * charWidth)
            vertexCoords.add(row * charHeight)

            vertexCoords.add(idxChar.toFloat() * w + w)
            vertexCoords.add(0.0f)
            vertexCoords.add(col * charWidth + charWidth)
            vertexCoords.add(row * charHeight)

            vertexCoords.add(idxChar.toFloat() * w + w)
            vertexCoords.add(-h)
            vertexCoords.add(col * charWidth + charWidth)
            vertexCoords.add(row * charHeight + charHeight)
        }
        val vertexCount:Int = vertexCoords.size / COORDS_PER_VERTEX

        val vertexBuffer: FloatBuffer =
            // (number of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(vertexCoords.size * 4).run {
                // use the device hardware's native byte order
                order(ByteOrder.nativeOrder())
                // create a floating point buffer from the ByteBuffer
                asFloatBuffer().apply {
                    put(vertexCoords.toFloatArray())		// add the coordinates to the FloatBuffer
                    position(0)	// set the buffer to read the first coordinate
                }
            }

        // Add program to OpenGL ES environment
        GLES30.glUseProgram(mProgram)

        mOrigin[0] = x
        mOrigin[1] = y
        mOrigin[2] = z

        // get handle to vertex shader's vOrigin member
        mOriginHandle = GLES30.glGetUniformLocation(mProgram, "vOrigin").also {
            // Set color for drawing the triangle
            GLES30.glUniform4fv(it, 1, mOrigin, 0)
        }

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureHandle[0])

        mTextureBuffer = GLES30.glGetUniformLocation(mProgram, "textureObject").also {
            // Set color for drawing the triangle
            GLES30.glUniform1i(it, 0)
        }

        // get handle to shape's transformation matrix
        vPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix").also{
            GLES30.glUniformMatrix4fv(it, 1, false, mvpMatrix, 0)
        }

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition").also {
            GLES30.glEnableVertexAttribArray(it)
            GLES30.glVertexAttribPointer(it, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, vertexStride, vertexBuffer)
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
//        GLES30.glDisableVertexAttribArray(mInstancePositionsHandle)
//        GLES30.glDisableVertexAttribArray(mColorHandle)

    }

    fun setPosition(origin: FloatArray){
        mOrigin = origin
    }
}



