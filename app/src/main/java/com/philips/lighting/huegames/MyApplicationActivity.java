package com.philips.lighting.huegames;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

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
        Button randomButton;
        randomButton = (Button) findViewById(R.id.buttonRand);
        randomButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                randomLights();
            }

        });

        myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethod();
            }

        }, 0, 100);

        rawFrames.add(new controlFrame(1, 25718, 1, 500, 500));
        rawFrames.add(new controlFrame(1, 64202, 155, 500, 0));
        rawFrames.add(new controlFrame(2, 25718, 1, 1000, 1000));
        rawFrames.add(new controlFrame(2, 64202, 155, 1000, 1000));
        rawFrames.add(new controlFrame(3, 25718, 1, 1000, 1000));
        rawFrames.add(new controlFrame(3, 64202, 155, 1000, 1000));

        processRawFrames(rawFrames);
        int i = 0;

    }

    private void TimerMethod()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.
        ArrayList<controlFrame> framesToSend = getNewFrames();
        for(controlFrame frame:framesToSend) {
            Log.w(TAG, "LightToSend: " + frame.getLightIndex());
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
                //bridge.updateLightState(allLights.get(i), lightState, listener);
                //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
                bridge.updateLightState(allLights.get(frame.getLightIndex()-1), lightState);
                //Log.w(TAG,"light " + i + "color " + String.valueOf(lightState.getHue()) + " trans " + String.valueOf(lightState.getTransitionTime()));
                Log.w(TAG, "Sent: " + frame.getLightIndex());
            }
        }

       /* for (int i = 0; i < allLights.size(); i++) {
            PHLightState lightState = new PHLightState();

            //Only send frames if there is frame to send
            if (i < frames.size()) {
                if (frames.get(i).getHue() != 0) {
                    lightState.setHue(frames.get(i).getHue());
                }
                if (frames.get(i).getBri() != 0) {
                    lightState.setBrightness(frames.get(i).getBri());
                }
                lightState.setTransitionTime(frames.get(i).getTransitionTime()/100);
                // To validate your lightstate is valid (before sending to the bridge) you can use:
                // String validState = lightState.validateState();
                //bridge.updateLightState(allLights.get(i), lightState, listener);
                //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
                bridge.updateLightState(allLights.get(i), lightState);
                //Log.w(TAG,"light " + i + "color " + String.valueOf(lightState.getHue()) + " trans " + String.valueOf(lightState.getTransitionTime()));
                Log.w(TAG, "Sent: " + i+1);
            }
        }*/
    }

    public void processRawFrames(ArrayList<controlFrame> rawFrames) {

        int numberOfLights = getNumberOfLights(rawFrames);

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
                framesToSend.add(frame.get(nextFrameIndex.get(lightIndex)));

                refreshTimerData(lightIndex, frame.get(nextFrameIndex.get(lightIndex)));
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

    public void randomLights() {
        PHBridge bridge = phHueSDK.getSelectedBridge();

        List<PHLight> allLights = bridge.getResourceCache().getAllLights();
        Random rand = new Random();

        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setHue(rand.nextInt(MAX_HUE));


            // To validate your lightstate is valid (before sending to the bridge) you can use:  
            // String validState = lightState.validateState();
            bridge.updateLightState(light, lightState, listener);
            //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
        }
    }
    // If you want to handle the response from the bridge, create a PHLightListener object.
    PHLightListener listener = new PHLightListener() {
        
        @Override
        public void onSuccess() {

        }
        
        @Override
        public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
           Log.w(TAG, "Light has updated");
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
}
