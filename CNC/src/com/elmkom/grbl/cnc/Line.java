package com.elmkom.grbl.cnc;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Line {

private FloatBuffer VertexBuffer;

private final String VertexShaderCode =
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +

        "attribute vec4 vPosition;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  gl_Position = uMVPMatrix * vPosition;" +
        "}";

private final String FragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}";

protected int GlProgram;
protected int PositionHandle;
protected int ColorHandle;
protected int MVPMatrixHandle;

// number of coordinates per vertex in this array
static final int COORDS_PER_VERTEX = 3;
static float LineCoords[] = {
    0.0f, 0.0f, 0.0f,
    1.0f, 0.0f, 0.0f
};

private final int VertexCount = LineCoords.length / COORDS_PER_VERTEX;
private final int VertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

// Set color with red, green, blue and alpha (opacity) values
float color[] = { 0.0f, 0.0f, 0.0f, 1.0f };

public Line(){
    // initialize vertex byte buffer for shape coordinates
    ByteBuffer bb = ByteBuffer.allocateDirect(
            // (number of coordinate values * 4 bytes per float)
            LineCoords.length * 4);
    // use the device hardware's native byte order
    bb.order(ByteOrder.nativeOrder());

    // create a floating point buffer from the ByteBuffer
    VertexBuffer = bb.asFloatBuffer();
    // add the coordinates to the FloatBuffer
    VertexBuffer.put(LineCoords);
    // set the buffer to read the first coordinate
    VertexBuffer.position(0);

/*
    int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VertexShaderCode);
    int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FragmentShaderCode);

    GlProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
    GLES20.glAttachShader(GlProgram, vertexShader);   // add the vertex shader to program
    GLES20.glAttachShader(GlProgram, fragmentShader); // add the fragment shader to program
    GLES20.glLinkProgram(GlProgram);                  // creates OpenGL ES program executables
  */  
  //  createProgram(VertexShaderCode,FragmentShaderCode);

}

public int createProgram(String vertexSource, String fragmentSource) {
    int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
    int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

    int program = GLES20.glCreateProgram();
    if (program != 0) {
        GLES20.glAttachShader(program, vertexShader);
       // checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
       // checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e("shader", "Could not link program: ");
            Log.e("shader", GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}
private int loadShader(int shaderType, String source) {
    int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("shader", "Could not compile shader " + shaderType + ":");
                Log.e("shader", GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
}

public void SetVerts(float v0, float v1, float v2, float v3, float v4, float v5)
{
    LineCoords[0] = v0;
    LineCoords[1] = v1;
    LineCoords[2] = v2;
    LineCoords[3] = v3;
    LineCoords[4] = v4;
    LineCoords[5] = v5;

    VertexBuffer.put(LineCoords);
    // set the buffer to read the first coordinate
    VertexBuffer.position(0);

}

public void SetColor(float red, float green, float blue, float alpha)
{
    color[0] = red;
    color[1] = green;
    color[2] = blue;
    color[3] = alpha;
}

public void draw(float[] mvpMatrix) {
    // Add program to OpenGL ES environment
    GLES20.glUseProgram(GlProgram);

    // get handle to vertex shader's vPosition member
    PositionHandle = GLES20.glGetAttribLocation(GlProgram, "vPosition");

    // Enable a handle to the triangle vertices
    GLES20.glEnableVertexAttribArray(PositionHandle);

    // Prepare the triangle coordinate data
    GLES20.glVertexAttribPointer(PositionHandle, COORDS_PER_VERTEX,
                                 GLES20.GL_FLOAT, false,
                                 VertexStride, VertexBuffer);

    // get handle to fragment shader's vColor member
    ColorHandle = GLES20.glGetUniformLocation(GlProgram, "vColor");

    // Set color for drawing the triangle
    GLES20.glUniform4fv(ColorHandle, 1, color, 0);

    // get handle to shape's transformation matrix
    MVPMatrixHandle = GLES20.glGetUniformLocation(GlProgram, "uMVPMatrix");
   // ArRenderer.checkGlError("glGetUniformLocation");

    // Apply the projection and view transformation
    GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, mvpMatrix, 0);
   // ArRenderer.checkGlError("glUniformMatrix4fv");


    // Draw the triangle
    GLES20.glDrawArrays(GLES20.GL_LINES, 0, VertexCount);

    // Disable vertex array
    GLES20.glDisableVertexAttribArray(PositionHandle);
}
}