package com.philips.lighting.huegames;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

/**
 * MyApplicationActivity - The starting point for creating your own Hue App.  
 * Currently contains a simple view with a button to change your lights to random colours.  Remove this and add your own app implementation here! Have fun!
 * 
 * @author SteveyO
 *
 */
public class MyApplicationActivity extends Activity {
    private PHHueSDK phHueSDK;
    private static final int MAX_HUE=65535;
    public static final String TAG = "QuickStart";
    private Timer myTimer;
    ListView listView = null;


    public final class controlFrame
    {
        private final int lightIndex;
        private final int hue;
        private final int bri;
        private final int transitionTime;
        private final int upTime;

        public controlFrame(int lightIndex, int hue, int bri, int transitionTime, int upTime)
        {
            this.lightIndex = lightIndex;
            this.hue = hue;
            this.bri = bri;
            this.transitionTime = transitionTime;
            this.upTime = upTime;
        }

        public int getLightIndex()
        {
            return lightIndex;
        }

        public int getHue() {
            return hue;
        }

        public int getBri() {
            return bri;
        }

        public int getTransitionTime() {
            return transitionTime;
        }

        public int getUpTime() {
            return upTime;
        }
    }

    private ArrayList<controlFrame> rawFrames = new ArrayList<controlFrame>();
    private ArrayList<ArrayList> allFrames = new ArrayList<ArrayList>();
    private ArrayList<Integer> newFrameStartTime = new ArrayList<Integer>();
    private ArrayList<Integer> timerState = new ArrayList<Integer>();
    private ArrayList<Integer> nextFrameIndex = new ArrayList<Integer>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        phHueSDK = PHHueSDK.create();
        listView = (ListView) findViewById(R.id.listView);

        rawFrames.add(new controlFrame(1, 0, 0, 0, 1000));
        //rawFrames.add(new controlFrame(1, 64202, 155, 5000, 5000));
        rawFrames.add(new controlFrame(2, 0, 0, 0, 1000));
        //rawFrames.add(new controlFrame(2, 64202, 155, 1000, 1000));
        rawFrames.add(new controlFrame(3, 25718, 1, 1000, 1000));
        rawFrames.add(new controlFrame(3, 64202, 155, 1000, 1000));

        populateListView(listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                Object o = listView.getItemAtPosition(position);

                startAnimation(o.toString());


            }
        });

    }

    private void TimerMethod()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.
        ArrayList<controlFrame> framesToSend = getNewFrames();

        //debug
        for(controlFrame frame:framesToSend) {
            //Log.w(TAG, "LightToSend: " + frame.getLightIndex());
        }

        sendFrames(framesToSend);


        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(Timer_Tick);
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {

            //This method runs in the same thread as the UI.

            //Do something to the UI thread here

        }
    };

    public void startAnimation(String fileName) {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/Hue animations/" + fileName);
        dir.mkdirs();

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(dir);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        ArrayList<controlFrame> tmp = new ArrayList<controlFrame>();
        try {
            tmp = readJsonStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        processRawFrames(tmp);

        if(myTimer != null) myTimer.cancel();

        myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethod();
            }

        }, 0, 100);
    }

    public void populateListView(ListView listView) {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/Hue animations");
        dir.mkdirs();

        File[] filelist = dir.listFiles();
        String[] theNamesOfFiles = new String[filelist.length];
        for (int i = 0; i < theNamesOfFiles.length; i++) {
            theNamesOfFiles[i] = filelist[i].getName();
        }
        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, theNamesOfFiles);
        listView.setAdapter(itemsAdapter);
    }

    public ArrayList<controlFrame> readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        ArrayList<controlFrame> frames = new ArrayList<controlFrame>();
        try {
            reader.beginArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        while (reader.hasNext()) {
            controlFrame message = gson.fromJson(reader, controlFrame.class);
            frames.add(message);
        }
        try {
            reader.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return frames;
    }

    public void sendFrames(ArrayList<controlFrame> frames) {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        List<PHLight> allLights = bridge.getResourceCache().getAllLights(); //TODO - Lights are not in order

        for(controlFrame frame:frames){
            if(frame.getLightIndex() <= allLights.size()) {
                PHLightState lightState = new PHLightState();

                if (frame.getHue() != 0) {
                    lightState.setHue(frame.getHue());
                }
                if (frame.getBri() != 0) {
                    lightState.setBrightness(frame.getBri());
                }
                lightState.setTransitionTime(frame.getTransitionTime()/100);
                // To validate your lightstate is valid (before sending to the bridge) you can use:
                // String validState = lightState.validateState();
                //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
                bridge.updateLightState(allLights.get(frame.getLightIndex()-1), lightState, listener);

                //debug
                //Log.w(TAG, "Sent: " + frame.getLightIndex());
            }
        }

    }

    public void processRawFrames(ArrayList<controlFrame> rawFrames) {

        int numberOfLights = getNumberOfLights(rawFrames);
        allFrames.clear();
        newFrameStartTime.clear();
        timerState.clear();
        nextFrameIndex.clear();

        //intitialise allFrames and timer arrays
        for (int i = 0;i < numberOfLights; i++) {
            allFrames.add(new ArrayList<controlFrame>());
            newFrameStartTime.add(0);
            timerState.add(0);
            nextFrameIndex.add(0);
        }

        //separate frames by lights
        for(controlFrame frame:rawFrames) {
            int lightIndex = frame.getLightIndex();
            allFrames.get(lightIndex-1).add(frame);
        }
    }

    public int getNumberOfLights(ArrayList<controlFrame> rawFrames) {
        int numberOfLights = 0;
        for(controlFrame frame:rawFrames) {
            if(frame.getLightIndex() > numberOfLights) numberOfLights = frame.getLightIndex();
        }
        return numberOfLights;
    }

   public ArrayList<controlFrame> getNewFrames() { //returns the frames that are scheduled for time
       ArrayList<controlFrame> framesToSend = new ArrayList<controlFrame>();
       int lightIndex = 0;
        for(ArrayList<controlFrame> frame:allFrames) {
            if(timerState.get(lightIndex) == newFrameStartTime.get(lightIndex)) {

                if(frame.size() != 0) {
                    framesToSend.add(frame.get(nextFrameIndex.get(lightIndex)));
                    refreshTimerData(lightIndex, frame.get(nextFrameIndex.get(lightIndex)));
                }


            }
            //Increment timerState
            timerState.set(lightIndex, timerState.get(lightIndex) + 1);

            lightIndex++;
        }
       return framesToSend;
    }

    public void refreshTimerData(int lightIndex, controlFrame frameToSend) {
        timerState.set(lightIndex, 0); //reset timer

        //set next frame's start time
        int nextStart = frameToSend.getTransitionTime()/100 + frameToSend.getUpTime()/100;
        newFrameStartTime.set(lightIndex, nextStart);

        //increment nextFrameIndex, if there's no more, start it over (loop)
        if(nextFrameIndex.get(lightIndex) == allFrames.get(lightIndex).size()-1) {
            nextFrameIndex.set(lightIndex, 0);
        } else {
            nextFrameIndex.set(lightIndex, nextFrameIndex.get(lightIndex) + 1);
        }
    }

    // If you want to handle the response from the bridge, create a PHLightListener object.
    PHLightListener listener = new PHLightListener() {
        
        @Override
        public void onSuccess() {

        }
        
        @Override
        public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
           //Log.w(TAG, "Light has updated");
        }
        
        @Override
        public void onError(int arg0, String arg1) {

        }

        @Override
        public void onReceivingLightDetails(PHLight arg0) {}

        @Override
        public void onReceivingLights(List<PHBridgeResource> arg0) {}

        @Override
        public void onSearchComplete() {}
    };
    
    @Override
    protected void onDestroy() {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        if (bridge != null) {
            
            if (phHueSDK.isHeartbeatEnabled(bridge)) {
                phHueSDK.disableHeartbeat(bridge);
            }
            
            phHueSDK.disconnect(bridge);
            super.onDestroy();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_myapplicationactivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.refresh_list) {
            populateListView(listView);
            if(myTimer != null) {
                myTimer.cancel();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
