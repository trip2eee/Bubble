package com.example.bubble

import android.opengl.GLES30

class Particle : Bubble() {
    override val mVertexCoords = floatArrayOf(
        -0.013635f, 0.483420f, -0.888825f, 0.292166f, 0.099052f, -0.961172f, -0.089589f, 0.090364f, -1.000517f,
    )
    override val mVertexNormal = floatArrayOf(
        0.093400f, 0.255400f, -0.962300f, 0.093400f, 0.255400f, -0.962300f, 0.093400f, 0.255400f, -0.962300f,
    )

    override val mFragmentShaderCode =
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

    override val mVertexShaderCode =
                "#version 300 es\n" +
                "uniform mat4 uMVPMatrix;" +
                "uniform vec4 vScale;" +
                "uniform float fTime;" +

                "layout(location = 0) in vec4 vPosition;" +
                "layout(location = 1) in vec3 vNormal;" +

                "uniform vec4 vInstanceColors[100];" +
                "uniform vec4 vInstancePositions[100];" +
                "vec4 vInstanceAxis[1000];" +
                "float fInstanceAngle[1000];" +
                "vec4 vGravity = vec4(0.0, -0.005, 0.0, 0.0);" +
                "out vec4 transVertexNormal;" +
                "out vec4 vColor;" +
                "void main() {" +
                "  float fInstanceID = float(gl_InstanceID); " +
                "  float fx = fInstanceID / 10.0 * 2.0 * 3.14; " +
                "  fInstanceAngle[gl_InstanceID] = sin(fInstanceID / 10.0 * 3.0) * 180.0; " +
                "  float fSpeed = 0.01;  " +
                "  vInstanceAxis[gl_InstanceID]  = vec4(cos(fx), sin(fx), sin(fx), 0.0); " +
                "  gl_Position = uMVPMatrix * ((vPosition*vScale) + vInstancePositions[gl_InstanceID / 10] + (vInstanceAxis[gl_InstanceID]*fSpeed*fTime) + (vGravity*fTime));" +
                "  transVertexNormal = normalize(uMVPMatrix * vec4(vNormal, 0.0));" +
                "  vColor = vInstanceColors[gl_InstanceID / 10];" +
                "}"

    fun draw(mvpMatrix: FloatArray, scale: Float, time: Float) {
        // Add program to OpenGL ES environment
        GLES30.glUseProgram(mProgram)

        GLES30.glGetUniformLocation(mProgram, "fTime").also {
            GLES30.glUniform1f(it, time)
        }

        // get handle to shape's transformation matrix
        GLES30.glGetUniformLocation(mProgram, "uMVPMatrix").also {
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

        // get handle to fragment shader's vColor member
        GLES30.glGetUniformLocation(mProgram, "vInstanceColors").also {
            // Set color for drawing the triangle
            GLES30.glUniform4fv(it, mNumInstances, mInstanceColors.toFloatArray(), 0)
        }

        mScale = floatArrayOf(scale, scale, scale, 1.0f)
        GLES30.glGetUniformLocation(mProgram, "vScale").also {
            GLES30.glUniform4fv(it, 1, mScale, 0)
        }

        // Draw the triangle
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, mVertexCount, mNumInstances * 10)

        // Disable vertex array
        GLES30.glDisableVertexAttribArray(mPositionHandle)
        GLES30.glDisableVertexAttribArray(mVertexNormalHandle)
    }

}