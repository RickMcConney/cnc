package com.elmkom.grbl.cnc;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView {

    MyRenderer myrenderer;
public MyGLSurfaceView(Context context) {
    super(context);

}

public MyRenderer getRenderer()
{
    return myrenderer;
}

public void updateTool(float wx,float wy,float wz)
{
    myrenderer.updateTool(wx, wy, wz);
}
public void move(float dx, float dy, float dz)
{
    myrenderer.move(dx, dy, dz);
}

public void setMode(int mode)
{
    myrenderer.setMode(mode);
}

public MyGLSurfaceView(Context context, AttributeSet attribs) {
    super(context, attribs);
   setEGLContextClientVersion(2);
}

public void initRenderer()
{
    myrenderer = new MyRenderer();
    setRenderer(myrenderer);
    // setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
}

@Override
public boolean onTouchEvent(MotionEvent event)
{
    if (event != null)
    {
        myrenderer.touch(event);

        return true;
    }

    return super.onTouchEvent(event);
}

@Override
public void onPause(){
    super.onPause();
}

@Override
public void onResume(){
    super.onResume();
}

}
