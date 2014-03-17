
package com.elmkom.grbl.cnc;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;




import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;



public class MyRenderer implements Renderer {
    Lines line = new Lines();
    private ArrayList<String> data = null;
    float mAngle = 0;
    float mScale = 0.5f;
    float mDeltax = 0;
    float mDeltay = 0;
    float mDeltaz = 0;
    int mMode = 0;
    float[] mProjectionMatrix = new float[16];
    float[] mViewMatrix = new float[16];
    float[] mMVPMatrix = new float[16];
    int mMVPMatrixHandle;
    float lastTouchX;
    float lastTouchY;
    
    final int RED = 0;
    final int GREEN = 1;
    final int BLUE = 2;
    final int WHITE = 3;
    final int MAXCOLOR = 4;
    
    final int SCALE = 0;
    final int TRANS = 1;
    final int ROTATE = 2;
    
    float lastX = 0;
    float lastY = 0;
    float lastZ = 0;
    String cmd = "";
    float wx = 0;
    float wy = 0;
    float wz = 0;
    private float toInch = 0.0393701f;
    private float toMm = 1f;
    private float conversion = toMm;

    
    public void setMode(int mode)
    {
        mMode = mode;
    }
    public void move(float dx, float dy, float dz)
    {
        mDeltax += dx;
        mDeltay += dy;
        mDeltaz += dz;
    }
    
    public void touch(MotionEvent event)
    {
        if( event.getAction() == MotionEvent.ACTION_DOWN ) {
            lastTouchX = event.getX();
            lastTouchY = event.getY();
        }else if( event.getAction() == MotionEvent.ACTION_MOVE ) {
            float dx = (event.getX() - lastTouchX) / 30.0f;
            float dy = (event.getY() - lastTouchY) / 30.0f;
            
            if(mMode == SCALE)
            {
                if(dx > 0)
                    mScale *= 1.01f;
                else if(dx < 0)
                    mScale *= 0.99f;
            }
            else if(mMode == TRANS)
            {
                if(Math.abs(dx) > Math.abs(dy))
                {
                    if(dx > 0)
                        move(0.02f,0,0);
                    else if(dx < 0)
                        move(-0.02f,0,0);
                }
                else
                {
                    if(dy > 0)
                        move(0,-0.02f,0);
                    else if(dy < 0)
                        move(0,0.02f,0);
                }

            }
            else if(mMode == ROTATE)
            {
                if(dy > 0)
                    mAngle -= 0.5f;
                else if(dy < 0)
                    mAngle += 0.5f;

            }

        }
    }
    @Override
    public void onDrawFrame(GL10 gl) {

        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        line.draw();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.d("cnc","surface changed");
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, ratio, -ratio, -1, 1, 3, 15);
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -7, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    public void updateTool(float wx,float wy,float wz)
    {
        wx *= conversion;
        wy *= conversion;
        wz *= conversion;
        
        line.clear(WHITE);
        line.addLine(-1, WHITE, wx-0.5f, wy, wz, wx+0.5f, wy, wz);
        line.addLine(-1, WHITE, wx, wy-0.5f, wz, wx, wy+0.5f, wz);
        line.fix(WHITE);
    }
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        
        Log.d("cnc","surface create");
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        
        line = new Lines();
        

        for(float i = 0;i<1;i+= 0.1f)
        {
            line.addLine(-1,RED,-i, -i, 0, i, -i, 0);
            line.addLine(-1,GREEN,-i, i, 0, i, i, 0);
            line.addLine(-1,BLUE,-i, -i, 0, -i, i, 0);
            line.addLine(-1,RED, i, -i, 0, i, i, 0);
        }
        updateTool(0,0,0);
        //line.addLine(-1,RED,0,0,0,0.5f,0,0);
       // line.addLine(-1,BLUE,0,0,0,0,1,0);
//        line.addLine(0,0,0,0,0,1);
        //line.fix();
        if(data != null)
            renderData(data);
        else
            for(int c = 0;c<MAXCOLOR;c++)
            {
                line.fix(c);
            }
        
    }

    public static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }
    
    public void addData(ArrayList<String> data)
    {
        this.cmd = "";
        this.data = data;
        lastX = 0;
        lastY = 0;
        lastZ = 0;
    }
    

    
    private void renderData(ArrayList<String> data)
    {
        for(int i = 0;i<data.size();i++)
        {
            renderData(i);
        }
        for(int c = 0;c<MAXCOLOR;c++)
        {
            line.fix(c);
        }
    }
    
    private void renderData(int i)
    {
        int step = i;
        float x = lastX;
        float y = lastY;
        float z = lastZ;

        float I = Float.NaN;
        float J = Float.NaN;
        double R = 0;

        if (data.get(i).startsWith("("))
            return;

        String[] tokens = data.get(i).split(" ");
        for (int t = 0; t < tokens.length; t++)
        {
            String token = tokens[t];
            // Log.d("cnc",token);
            if (token.startsWith("G"))
                cmd = token;
            else if (token.startsWith("X"))
                x = Float.parseFloat(token.substring(1));
            else if (token.startsWith("Y"))
                y = Float.parseFloat(token.substring(1));
            else if (token.startsWith("Z"))
                z = -Float.parseFloat(token.substring(1));
            else if (token.startsWith("I"))
                I = Float.parseFloat(token.substring(1));
            else if (token.startsWith("J"))
                J = Float.parseFloat(token.substring(1));
            else if (token.startsWith("R"))
                R = Double.parseDouble(token.substring(1));
            
            if("G20".equals(cmd)) conversion = toInch;
            if("G21".equals(cmd)) conversion = toMm;
            

        }

        if (cmd.equals(""))
            return;

        if (x != lastX || y != lastY || z != lastZ)
        {

            if (cmd.equals("G0"))
                line.addLine(step,BLUE, lastX, lastY, lastZ, x, y, z);
            else if (cmd.equals("G1"))
                line.addLine(step,GREEN, lastX, lastY, lastZ, x, y, z);
            else if (cmd.equals("G2") || cmd.equals("G3"))
            {
                boolean isCW = cmd.equals("G2");
                if (Float.isNaN(I) && Float.isNaN(J)) { // todo only supports
                                                        // relative
                    float[] center = convertRToCenter(lastX, lastY, x, y, R, false, isCW);
                    generatePointsAlongArcBDring(step, RED, lastX, lastY, lastZ, x, y, z, center[0],
                            center[1], isCW, R, 5);
                }
                else
                {
                    generatePointsAlongArcBDring(step,RED, lastX, lastY, lastZ, x, y, z, I + lastX, J
                            + lastY, isCW, R, 5);
                }
            }

            lastX = x;
            lastY = y;
            lastZ = z;

        }
            
        
    }
    
    private float[] convertRToCenter(float sx,float sy, float ex, float ey, double radius, boolean absoluteIJK, boolean clockwise) {
        double R = radius;
        float cx;
        float cy;
        
        // This math is copied from GRBL in gcode.c
        double x = ex - sx;
        double y = ey - sy;

        double h_x2_div_d = 4 * R*R - x*x - y*y;
        if (h_x2_div_d < 0) { System.out.println("Error computing arc radius."); }
        h_x2_div_d = (-Math.sqrt(h_x2_div_d)) / Math.hypot(x, y);

        if (clockwise == false) {
            h_x2_div_d = -h_x2_div_d;
        }

        // Special message from gcoder to software for which radius
        // should be used.
        if (R < 0) {
            h_x2_div_d = -h_x2_div_d;
            // TODO: Places that use this need to run ABS on radius.
            radius = -radius;
        }

        double offsetX = 0.5*(x-(y*h_x2_div_d));
        double offsetY = 0.5*(y+(x*h_x2_div_d));

        if (!absoluteIJK) {
            cx = (float)(sx + offsetX);
            cy = (float)(sy + offsetY);
        } else {
            cx = (float)offsetX;
            cy = (float)offsetY;
        }
//        Log.d("cnc","R = "+R+" sx = "+sx+" sy = "+sy+" cx = "+cx+" cy = "+cy+" ex = "+ex+" ey = "+ey);       
        float[] center = new float[2];
        center[0] = cx;
        center[1] = cy;
        return center;
    }
    
    private double getAngle(float sx, float sy, float ex, float ey) {
        double deltaX = ex - sx;
        double deltaY = ey - sy;

        double angle = 0.0;

        if (deltaX != 0) { // prevent div by 0
            // it helps to know what quadrant you are in
            if (deltaX > 0 && deltaY >= 0) {  // 0 - 90
                angle = Math.atan(deltaY/deltaX);
            } else if (deltaX < 0 && deltaY >= 0) { // 90 to 180
                angle = Math.PI - Math.abs(Math.atan(deltaY/deltaX));
            } else if (deltaX < 0 && deltaY < 0) { // 180 - 270
                angle = Math.PI + Math.abs(Math.atan(deltaY/deltaX));
            } else if (deltaX > 0 && deltaY < 0) { // 270 - 360
                angle = Math.PI * 2 - Math.abs(Math.atan(deltaY/deltaX));
            }
        }
        else {
            // 90 deg
            if (deltaY > 0) {
                angle = Math.PI / 2.0;
            }
            // 270 deg
            else {
                angle = Math.PI * 3.0 / 2.0;
            }
        }
      
        return angle;
    }  
    private void generatePointsAlongArcBDring(int step, int color, float sx, float sy, float sz, float ex, float ey, float ez, float cx, float cy, boolean isCw, double R, int arcResolution) {
        double radius = R;
        double sweep;

        // Calculate radius if necessary.
        if (radius == 0) {
            radius = Math.sqrt(Math.pow(sx - cx, 2.0) + Math.pow(sy - cy, 2.0));
        }
//        Log.d("cnc","R1 = "+R+" radius = "+radius+" cx = "+cx+" cy = "+cy);
        // Calculate angles from center.
        double startAngle = getAngle(cx, cy, sx,sy);
        double endAngle = getAngle(cx,cy, ex,ey);
                
        // Fix semantics, if the angle ends at 0 it really should end at 360.
        if (endAngle == 0) {
                endAngle = Math.PI * 2;
        }

        // Calculate distance along arc.
        if (!isCw && endAngle < startAngle) {
            sweep = ((Math.PI * 2 - startAngle) + endAngle);
        } else if (isCw && endAngle > startAngle) {
            sweep = ((Math.PI * 2 - endAngle) + startAngle);
        } else {
            sweep = Math.abs(endAngle - startAngle);
        }
        
        generatePointsAlongArcBDring(step,color,sx,sy,sz, ex,ey,ez, cx,cy, isCw, radius, startAngle, endAngle, sweep, arcResolution);
    }

    /**
     * Generates the points along an arc including the start and end points.
     */
    private void generatePointsAlongArcBDring(int step, int color,float sx, float sy, float sz, float ex, float ey, float ez, float cx, float cy, boolean isCw, double radius, 
            double startAngle, double endAngle, double sweep, int numPoints) {

 
        double angle;
        float x = ex;
        float y = ey;
        float z = ez;
        float lastX = sx;
        float lastY = sy;
        float lastZ = sz;
                
        double zIncrement = (ez - sz) / numPoints;
        for(int i=0; i<=numPoints; i++)
        {
            if (isCw) {
                angle = (startAngle - i * sweep/numPoints);
            } else {
                angle = (startAngle + i * sweep/numPoints);
            }

            if (angle >= Math.PI * 2) {
                angle = angle - Math.PI * 2;
            }

            x = (float) (Math.cos(angle) * radius + cx);
            y = (float) (Math.sin(angle) * radius + cy);
            z += zIncrement;
            
            line.addLine(step, color,lastX,lastY,lastZ,x,y,z);
            lastX = x;
            lastY = y;
            lastZ = z;
            
        }
    }
    
    class Lines {

        Object[] lineData = new Object[MAXCOLOR];
        
        
        final String vertexShaderCode =  
                "uniform mat4 uMVPMatrix;" +
                "uniform mat4 uRotate;" +
                "attribute vec4 vPosition;\n"+
                "void main()\n" +
                "{\n" +
                 "gl_Position = uMVPMatrix * vPosition;\n"+  
                "}";
        
        
        final String fragmentShaderCode = "precision mediump float;"
                + "uniform vec4 vColor;" + "void main() {"
                + "  gl_FragColor = vColor;" + "}";

        FloatBuffer[] vertexBuffer = new FloatBuffer[MAXCOLOR];
        int vertexCount[] = new int[MAXCOLOR];
        int mProgram;
        int mPositionHandle;
        int mColorHandle;

        // number of coordinates per vertex in this array
        final int COORDS_PER_VERTEX = 3;
        float lineCoords[] = new float[6];

        final int vertexStride = COORDS_PER_VERTEX * 4; // bytes per vertex
        // Set color with red, green, blue and alpha (opacity) values
        float color[][] = {
                {0.63671875f, 0, 0, 1.0f},
                {0.63671875f, 0.76953125f, 0.22265625f, 1.0f},
                {0, 0, 1, 1.0f},
                {1,1,1,1}
                
        };

        public Lines() {

            for(int i = 0;i<MAXCOLOR;i++)
                lineData[i] = new ArrayList<float[]>();
            
            // prepare shaders and OpenGL program
            int vertexShader = MyRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                    vertexShaderCode);
            int fragmentShader = MyRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                    fragmentShaderCode);

            mProgram = GLES20.glCreateProgram(); // create empty OpenGL Program
            Log.d("cnc","mprogram = "+mProgram);
            GLES20.glAttachShader(mProgram, vertexShader); // add the vertex
                                                           // shader
                                                           // to program
            GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment
                                                             // shader to
                                                             // program
            GLES20.glLinkProgram(mProgram); // create OpenGL program executables
            
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            Log.d("cnc","mMVPMatrixHandle = "+mMVPMatrixHandle);
        }

        public void clear(int c)
        {
            ((ArrayList<float[]>)lineData[c]).clear();
        }
        public void addLine(int step, int color,float x1, float y1, float z1, float x2, float y2, float z2)
        {
            float points[] = new float[6];
          
            points[0] = x1;
            points[1] = y1;
            points[2] = z1;
            points[3] = x2;
            points[4] = y2;
            points[5] = z2;
            ArrayList<float[]> list = (ArrayList<float[]>) lineData[color];
            list.add(points);

        }
        
        public void fix(int c)
        {
            ArrayList<float[]> list = (ArrayList<float[]>) lineData[c];
            lineCoords = new float[list.size() * 6];
            for (int i = 0; i < list.size(); i++) {
                for (int p = 0; p < 6; p++)
                {
                    lineCoords[i * 6 + p] = list.get(i)[p];
                }

            }
            vertexCount[c] = lineCoords.length / COORDS_PER_VERTEX;
            // initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(
                    // (number of coordinate values * 4 bytes per float)
                    lineCoords.length * 4);
            // use the device hardware's native byte order
            bb.order(ByteOrder.nativeOrder());

            // create a floating point buffer from the ByteBuffer
            vertexBuffer[c] = bb.asFloatBuffer();
            vertexBuffer[c].put(lineCoords);
            vertexBuffer[c].position(0);
            
        }
        
        private void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e("cnc", op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }
        
        public void draw() {

            vertexBuffer[RED].position(0);

            float[] mRotationMatrix = new float[16];
             float [] scratch = new float[16];
             float [] scaleM = new float[16];
             float [] transM = new float[16];
             //mAngle+=0.5f;
             
             Matrix.setIdentityM(transM,0);
             Matrix.translateM(transM,0, mDeltax, mDeltay, mDeltaz);
             Matrix.setIdentityM(mRotationMatrix,0);
             Matrix.setIdentityM(scaleM,0);
             Matrix.scaleM(scaleM, 0, mScale, mScale, mScale);
             Matrix.setRotateM(mRotationMatrix, 0, mAngle, 1.0f, 0, 0);
             
             Matrix.multiplyMM(scratch, 0, scaleM, 0, mRotationMatrix, 0);
             Matrix.multiplyMM(scratch, 0, transM, 0, scratch, 0);
             Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, scratch, 0);
            // Log.d("cnc","angle "+scratch[0]+" "+scratch[1]+" "+scratch[2]+" "+scratch[4]);          

             // Add program to OpenGL environment
             GLES20.glUseProgram(mProgram);
             checkGlError("glUseProgram");     

            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, scratch, 0);
            checkGlError("glUniformMatrix4fv muMVPMatrixHandle");     

            // get handle to vertex shader's vPosition member
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            checkGlError("glGetAttribLocation");     

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            checkGlError("glEnableVertexAttribArray");     

            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            
            for(int i = 0;i<MAXCOLOR;i++)
            {
                // Prepare the triangle coordinate data
                GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false, vertexStride, vertexBuffer[i]);
                checkGlError("glVertexAttribPointer");     



                // Set color for drawing the triangle
                GLES20.glUniform4fv(mColorHandle, 1, color[i], 0);
                checkGlError("glUniform4fv");     

                // Draw the triangle
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount[i]);
                checkGlError("glDrawArrays");
            }

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle);
            checkGlError("glDisableVertexAttribArray");     
            
            
        }

    }

}
