/* Copyright 2011 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */

package com.elmkom.grbl.cnc;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.elmkom.grbl.cnc.R;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monitors a single {@link UsbSerialDriver} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity extends Activity implements OnTouchListener,Runnable {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();
    private static final int REQUEST_CODE = 6384;
    private static Uri gcodeFile = null;
    private ArrayList<String> lines = null;
    private int lineCount = 0;
    private boolean playing = false;
    private ImageButton mModeButton;
    private ImageButton mRunButton;
    private byte[] replyData = new byte[256];
    private int replyIndex = 0;
    private boolean polling = false;

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialDriver)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialDriver sDriver = null;

    private TextView mTitleTextView;
 //   private TextView mDumpTextView;
 //   private ScrollView mScrollView;
    private EditText mInput;
    private Button mSend;
   // MyRenderer myRenderer;
    MyGLSurfaceView glSurface;
    private View view;
    
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(final Exception e) {
            SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String msg = "serial error "+e.toString();
                    SerialConsoleActivity.this.updateReceivedData(msg.getBytes());
                }
            });
          //  mDumpTextView.append("error "+e.toString());
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            
            SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SerialConsoleActivity.this.updateReceivedData(data);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("cnc","on create");
        setContentView(R.layout.serial_console);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        view = findViewById(R.id.toplayout);
        view.setBackgroundColor(Color.RED);
        view.invalidate();

        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
//        mDumpTextView = (TextView) findViewById(R.id.consoleText);
//        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        mInput = (EditText) findViewById(R.id.editText1);
        
        mModeButton = (ImageButton) findViewById(R.id.mode);
        mModeButton.setTag("0");

        mRunButton = (ImageButton) findViewById(R.id.run);
        mRunButton.setTag("run");
        
        ((ImageButton) findViewById(R.id.plusx)).setOnTouchListener(this);
        ((ImageButton) findViewById(R.id.plusy)).setOnTouchListener(this);
        ((ImageButton) findViewById(R.id.plusz)).setOnTouchListener(this);
        ((ImageButton) findViewById(R.id.minx)).setOnTouchListener(this);
        ((ImageButton) findViewById(R.id.miny)).setOnTouchListener(this);
        ((ImageButton) findViewById(R.id.minz)).setOnTouchListener(this);
        

        
        TextView.OnEditorActionListener exampleListener = new TextView.OnEditorActionListener()
        {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) 
            {
                if (actionId == EditorInfo.IME_NULL  
                   && event.getAction() == KeyEvent.ACTION_DOWN) { 
                    String data = mInput.getText().toString()+"\n";
                    sendMsg(data);
                    mInput.setText("");
                }
                return true;
             }
        };

        mInput.setOnEditorActionListener(exampleListener);

        //Get hold of our GLSurfaceView
        glSurface = (MyGLSurfaceView) findViewById(R.id.glSurfaceView);

        
        Log.d("cnc","on create finished");
 
    }

    private void initSurface()
    {
        glSurface.initRenderer();
        if(gcodeFile !=null)
        {
            
            lineCount = 0;
            lines = null;
            
            try {
                lines = getStringFromFile(gcodeFile.getPath());
                
                mTitleTextView.setText(gcodeFile.getLastPathSegment()+ " "+lineCount+"/"+lines.size());
                glSurface.getRenderer().addData(lines);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
         
        }
        
    }
    public boolean onTouch( View view , MotionEvent theMotion ) {
        String msg = null;
        switch ( theMotion.getAction() ) {
        case MotionEvent.ACTION_DOWN: 
            switch(view.getId()){
                case R.id.miny:
                    msg = "G91 G0 Y -100\n";
                    break;
                case R.id.plusy:
                    msg = "G91 G0 Y 100\n";
                    break;
                case R.id.minx:
                    msg = "G91 G0 X -100\n";
                    break;
                case R.id.plusx:
                    msg = "G91 G0 X 100\n";
                    break;
                case R.id.minz:
                    msg = "G91 G0 Z -100\n";
                    break;
                case R.id.plusz:
                    msg = "G91 G0 Z 100\n";
                    break; 
            }
            break;
        case MotionEvent.ACTION_UP: 
            msg = "!\n";
            break;
        }
        if(msg != null)
            sendMsg(msg);
        if("!\n".equals(msg))
            sendReset();
        return true;
    }  
    
    public void button_click(View view)
    {
        String msg = null;
        switch(view.getId()){
            /*
            case R.id.miny:
                msg = "G91 G0 Y -10\n";
                break;
            case R.id.plusy:
                msg = "G91 G0 Y 10\n";
                break;
            case R.id.minx:
                msg = "G91 G0 X -10\n";
                break;
            case R.id.plusx:
                msg = "G91 G0 X 10\n";
                break;
            case R.id.minz:
                msg = "G91 G0 Z -10\n";
                break;
            case R.id.plusz:
                msg = "G91 G0 Z 10\n";
                break;  
                */
            case R.id.home:
                msg = "$H\n";
                break;  
            case R.id.alarm:
                msg = "$X\n";
                break;  
            case R.id.zero:
                msg = "G10 L20 P1 X0 Y0 Z0\n";
                break; 
            case R.id.run:
                if("run".equals(mRunButton.getTag()))
                {
                    mRunButton.setImageResource(R.drawable.pause);
                    mRunButton.setTag("pause");
                    playing = true;
                    if(lineCount == 0)
                    {
                        nextLine();
                    }
                    else
                        msg = "~\n";
                    
 
                }
                else
                {
                    mRunButton.setImageResource(R.drawable.play);
                    mRunButton.setTag("run");
                    playing = false;
                    msg = "!\n";
                }
                break;
            case R.id.mode:
                if("0".equals(mModeButton.getTag()))
                {
                    glSurface.setMode(1);
                    mModeButton.setTag("1");
                    mModeButton.setImageResource(R.drawable.move);
                }
                else if("1".equals(mModeButton.getTag()))
                {
                    glSurface.setMode(2);
                    mModeButton.setTag("2");
                    mModeButton.setImageResource(R.drawable.rotate);
                }
                else if("2".equals(mModeButton.getTag()))
                {
                    glSurface.setMode(0);
                    mModeButton.setTag("0");
                    mModeButton.setImageResource(R.drawable.scale);
                }
                break;
        }
        if(msg != null)
        {
            sendMsg(msg);
        }

        
    }
    
    private void sendReset()
    {
        byte[] msg = {0x18};
        if(mSerialIoManager != null)
        {
             mSerialIoManager.writeAsync(msg);
        }
        
    }
    
    private synchronized void sendMsg(final String msg)
    {
        if(mSerialIoManager != null)
        {
            //if(!msg.startsWith("?"))
              //  mInput.setText(msg.trim());
             mSerialIoManager.writeAsync(msg.getBytes());
        }
        else
        {
            if(!msg.startsWith("?"))
                mInput.setText(msg.trim());
            if(lines != null)
            {
                if(lineCount < lines.size())
                {
                    lineCount++;
                    mTitleTextView.setText(gcodeFile.getLastPathSegment()+ " "+lineCount+"/"+lines.size());
                }
            }
        }
    }
    private void nextLine()
    {
        if(lines != null)
        {
            if(lineCount < lines.size())
            {
                while(lines.get(lineCount).startsWith("(") || lines.get(lineCount).startsWith("%"))
                {
                    lineCount++;
                    if(lineCount == lines.size())
                    endOfFile();
                    return;
                }
                sendMsg(lines.get(lineCount).replaceAll("\\s+","")+"\n");
                lineCount++;
                mTitleTextView.setText(gcodeFile.getLastPathSegment()+ " "+lineCount+"/"+lines.size());
            }
            else
            {
                endOfFile();
            }
        }
    }
    
    private void endOfFile()
    {
        mRunButton.setImageResource(R.drawable.play);
        mRunButton.setTag("run");
        playing = false;
        lineCount = 0;
        
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sDriver != null) {
            try {
                sDriver.close();
            } catch (IOException e) {
                // Ignore.
            }
            sDriver = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initSurface();
        Log.d(TAG, "Resumed, sDriver=" + sDriver);
        if (sDriver == null) {
            if(gcodeFile != null)
                mTitleTextView.setText(gcodeFile.getLastPathSegment()+ " "+lineCount+"/"+lines.size());
            else
                mTitleTextView.setText("No serial device.");
                
        } else {
            try {
                sDriver.open();
                sDriver.setParameters(9600, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
                //mDumpTextView.append("device opened at 9600 baud\n");
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText("Error opening device: " + e.getMessage()+"\n");
                try {
                    sDriver.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sDriver = null;
                return;
            }
            if(gcodeFile != null)
                mTitleTextView.setText(gcodeFile.getLastPathSegment()+ " "+lineCount+"/"+lines.size());
            else
                mTitleTextView.setText(sDriver.getClass().getSimpleName());
            
        }
        onDeviceStateChange();
    }

    private void stopIoManager() {
        stopPoller();
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
      //      mDumpTextView.append("stopped");
        }
    }

    private void startIoManager() {
        if (sDriver != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
            mExecutor.submit(mSerialIoManager);
            startPoller();
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }
    
    public void run()
    {
        while(polling)
        {
            sendMsg("?");
            try{
                Thread.sleep(1000);
            }catch(Exception e){}
        }
    }

    private void startPoller()
    {
        polling = true;
        Thread t = new Thread(this);
        t.start();
        
    }
    
    private void stopPoller()
    {
        polling = false;
        
    }
    private void updateStatus(String reply)
    {
       // String reply = "<Alarm,MPos:0.000,0.000,0.000,WPos:-51.000,-31.000,31.000>\n";

        if(reply.indexOf("to unlock")>=0)
            sendMsg("$X\n");

        if(reply.startsWith("<"))
        {
            if(reply.indexOf("Alarm") >=0)
            {
                view.setBackgroundColor(Color.RED);
            }
            else
            {
                view.setBackgroundColor(Color.CYAN);
            }
            view.invalidate();
            
            String[] tokens = reply.split("[,:<>]");

            if(tokens.length >=9)
            {
                float wx = Float.parseFloat(tokens[7]);
                float wy = Float.parseFloat(tokens[8]);
                float wz = Float.parseFloat(tokens[9]);
                glSurface.updateTool(wx, wy, wz);
                if(!playing)
                    mTitleTextView.setText("X="+wx+" Y="+wy+" Z="+wz);
            }
        }
        else
            mInput.setText(reply.trim());

        
    }

    private void updateReceivedData(byte[] data) {


        for(int i =0;i< data.length;i++)
        {
            if(replyIndex > replyData.length-1)
                replyIndex = 0;
             replyData[replyIndex++] = data[i];
             if(data[i] == '\n')
             {
                 String reply = new String(replyData,0,replyIndex);
                 replyIndex  = 0;
                 if(reply.indexOf("ok") >= 0 || reply.indexOf("error")>=0)
                 {
                     mInput.setText(reply.trim());
                     if(playing)
                         nextLine();
                 }
                 else
                     updateStatus(reply);
             }
         }
    }

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialDriver driver,Uri uri) {
        sDriver = driver;
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
        
        gcodeFile = uri;
    }
    
    private void showChooser() {
        // Use the GET_CONTENT intent from the utility class
        Intent target = FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(
                target, "CNC");
        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            // The reason for the existence of aFileChooser
            Log.e(TAG,e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        Log.i(TAG, "Uri = " + uri.toString());
                        try {
                            // Get the file path from the URI
                            //final String path = FileUtils.getPath(this, uri);
                            Toast.makeText(SerialConsoleActivity.this,
                                    "File Selected: " + uri, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e("FileSelectorTestActivity", "File select error", e);
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    public static ArrayList<String> convertStreamToArray(InputStream is) throws Exception {
        ArrayList<String> list = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = reader.readLine()) != null) {
            list.add(line+"\n");
        }
        reader.close();
        return list;
    }

    public static ArrayList<String> getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        ArrayList<String> ret = convertStreamToArray(fin);
        //Make sure you close all streams.
        fin.close();        
        return ret;
    }
}
