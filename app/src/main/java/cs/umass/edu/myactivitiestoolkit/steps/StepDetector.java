package cs.umass.edu.myactivitiestoolkit.steps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.sql.Time;
import java.util.ArrayList;

import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.processing.Filter;

/**
 * This class is responsible for detecting steps from the accelerometer sensor.
 * All {@link OnStepListener step listeners} that have been registered will
 * be notified when a step is detected.
 */
public class StepDetector implements SensorEventListener {
    /** Used for debugging purposes. */
    @SuppressWarnings("unused")
    private static final String TAG = StepDetector.class.getName();

    /** Maintains the set of listeners registered to handle step events. **/
    private ArrayList<OnStepListener> mStepListeners;
    private Filter filter = new Filter(1);
    //.5 seconds
    private double minTime = 1000000*1000*.5;
    private long maxTime =1000000*1000*2;
    private long lastStepped = 0;

    /**
     * The number of steps taken.
     */
    private int stepCount;

    public StepDetector(){
        mStepListeners = new ArrayList<>();
        stepCount = 0;
    }

    /**
     * Registers a step listener for handling step events.
     * @param stepListener defines how step events are handled.
     */
    public void registerOnStepListener(final OnStepListener stepListener){
        mStepListeners.add(stepListener);
    }

    /**
     * Unregisters the specified step listener.
     * @param stepListener the listener to be unregistered. It must already be registered.
     */
    public void unregisterOnStepListener(final OnStepListener stepListener){
        mStepListeners.remove(stepListener);
    }

    /**
     * Unregisters all step listeners.
     */
    public void unregisterOnStepListeners(){
        mStepListeners.clear();
    }

    /**
     * Here is where you will receive accelerometer readings, buffer them if necessary
     * and run your step detection algorithm. When a step is detected, call
     * {@link #onStepDetected(long, float[])} to notify all listeners.
     *
     * Recall that human steps tend to take anywhere between 0.5 and 2 seconds.
     *
     * @param event sensor reading
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG,"In OnSensorChanged in StepDetector");
        Log.i(TAG,event.sensor.getType()+"");

        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
        Log.i(TAG,"After If In OnSensorChanged in StepDetector");
            //TODO: Detect steps! Call onStepDetected(...) when a step is detected.
           long currTime = event.timestamp;
            //If it hasn't been our minimum threshold
            Log.i(TAG,"current Time: "+currTime);
            Log.i(TAG,"last Stepped: "+lastStepped);
            if(((currTime-lastStepped) >= minTime )){
                lastStepped = currTime;
                double [] hold = filter.getFilteredValues(event.values);
                float [] someValues = new float[hold.length];
                    for (int i = 0; i < hold.length; i++) {
                        someValues[i]=(float)hold[i];
                    }

                onStepDetected(currTime,someValues);
            }




        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // do nothing
    }

    /**
     * This method is called when a step is detected. It updates the current step count,
     * notifies all listeners that a step has occurred and also notifies all listeners
     * of the current step count.
     */
    private void onStepDetected(long timestamp, float[] values){
        stepCount++;
        for (OnStepListener stepListener : mStepListeners){
            stepListener.onStepDetected(timestamp, values);
            stepListener.onStepCountUpdated(stepCount);
        }
    }
}
