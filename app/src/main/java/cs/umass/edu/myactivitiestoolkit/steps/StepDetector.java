package cs.umass.edu.myactivitiestoolkit.steps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.processing.Filter;
import edu.umass.cs.MHLClient.sensors.SensorReading;

/**
 * This class is responsible for detecting steps from the accelerometer sensor.
 * All {@link OnStepListener step listeners} that have been registered will
 * be notified when a step is detected.
 */
public class StepDetector implements SensorEventListener {
    /**
     * Used for debugging purposes.
     */
    @SuppressWarnings("unused")
    private static final String TAG = StepDetector.class.getName();

    /**
     * Maintains the set of listeners registered to handle step events.
     **/
    private ArrayList<OnStepListener> mStepListeners;
    private Filter filter = new Filter(3.0f);
    private LinkedList<Float []> buffer = new LinkedList<>();
    private LinkedList<Long> timeStampBuffer = new LinkedList<>();
    //.5 seconds
    private double window = 1000000 * 1000*.5;
    private long lastStepped = 0;
    private int bufferCounter = 0;
    final private int maxBufferCount = 30;
    private boolean bufferFilled = false;
    float minX, minY, minZ, maxX, maxY, maxZ;
    private float dynThreshold;
    private Float lastWindowVal;
    private boolean buffAccuFirstTime = false;
    private boolean x,y,z;


    /**
     * The number of steps taken.
     */
    private int stepCount;

    public StepDetector() {
        mStepListeners = new ArrayList<>();
        stepCount = 0;
        minX = minY = minZ = maxX = maxY = maxZ = 0;
        x=y=z=false;
    }

    /**
     * Registers a step listener for handling step events.
     *
     * @param stepListener defines how step events are handled.
     */
    public void registerOnStepListener(final OnStepListener stepListener) {
        mStepListeners.add(stepListener);
    }

    /**
     * Unregisters the specified step listener.
     *
     * @param stepListener the listener to be unregistered. It must already be registered.
     */
    public void unregisterOnStepListener(final OnStepListener stepListener) {
        mStepListeners.remove(stepListener);
    }

    /**
     * Unregisters all step listeners.
     */
    public void unregisterOnStepListeners() {
        mStepListeners.clear();
    }



    public float [] calcMags(List<Float[]> buffer){
        float [] ret = new float[buffer.size()];

        Iterator<Float[]> iterator = buffer.iterator();
        int i = 0;
        while(iterator.hasNext()){
           Float [] tmp =  iterator.next();
            ret[i] = (float) Math.sqrt((double)(Math.pow(tmp[0],2)+Math.pow(tmp[1],2)+Math.pow(tmp[2],2)));
            i++;
        }

        return ret;

    }

    public float [] getSlopes(float [] rise, long [] run){
        float [] ret = new float[rise.length];
        int x1=0;
        int x2=1;
        for(;x2<rise.length;x2++){
            Log.i(TAG,"run[X2]: "+run[x2]+" run[X1]: "+run[x1]);
            float deltaX = run[x2]-run[x1];
            float deltaY = rise[x2]-run[x1];
            ret[x1] = (deltaY/deltaX);
            x1++;
            Log.i(TAG,"deltaX: "+deltaX+" deltaY: "+deltaY);
            Log.i(TAG,"dy/dx = "+ret[x1-1]);
        }
        return ret;
    }

    public float[] getDerivativesOfTheSlopes(float []slopes){
        float [] ret = new float[slopes.length];

        for (int i=0; i<slopes.length; i++){
            ret[i] =(float) (-Math.sin(slopes[i]));
            Log.i(TAG,"derivative = "+ret[i]);
        }

        return ret;
    }

    public float[] floatConversion(Float[] from){
        float [] to = new float[from.length];

        for (int i=0; i<from.length; i++){
            to[i] = from[i];
        }
        return to;
    }

    public Float[] oFloatConversion(float[] from){
        Float [] to = new Float[from.length];
        for (int i=0; i<from.length; i++){
            to[i] = from[i];
        }
        return to;
    }

    /**
     * Here is where you will receive accelerometer readings, buffer them if necessary
     * and run your step detection algorithm. When a step is detected, call
     * {@link #onStepDetected(long, float[])} to notify all listeners.
     * <p>
     * Recall that human steps tend to take anywhere between 0.5 and 2 seconds.
     *
     * @param event sensor reading
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "In OnSensorChanged in StepDetector");
        Log.i(TAG, event.sensor.getType() + "");


        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if(buffer.size()<maxBufferCount) {
                Float[] values = new Float[3];
                for (int i = 0; i < values.length; i++) {
                    values[i] = event.values[i];
                }
                long curTimeStamp = (long) ((double) event.timestamp / Constants.TIMESTAMPS.NANOSECONDS_PER_MILLISECOND);

                buffer.add(values);
                timeStampBuffer.add(curTimeStamp);
            } else if (buffer.size() == maxBufferCount){
//                float [] fv = filter.getFilteredValues(calcMags(buffer));
                LinkedList<Float []>fv = new LinkedList<>();
                for (int i=0; i<buffer.size(); i++){
                    fv.add(oFloatConversion(filter.getFilteredValues(floatConversion(buffer.get(i)))));
                }

                float[] mags = calcMags(fv);
               Iterator<Long> tmp =  timeStampBuffer.iterator();
                long [] timeStamps = new long[timeStampBuffer.size()];
                int iterator = 0;
                while (tmp.hasNext()){
                    Long val = tmp.next();
                    timeStamps[iterator] = val;
                }
                float [] derivatives = getDerivativesOfTheSlopes(getSlopes(mags,timeStamps));
                //starts off being negative;
                boolean negative;
                if(derivatives[0] >= 0 ){
                    negative=false;
                }else {
                    negative = true;
                }

                for (int i=1; i<derivatives.length; i+=2){
                    if(derivatives[i]>=0 && negative){
                        Log.i(TAG,"it was negative");
                        negative =false;
                        float [] tm = new float[3];
                        Float [] t = buffer.get(i);
                        for (int j=0; j<3; j++){
                            tm[j] = t[j];
                        }
                        onStepDetected(timeStampBuffer.get(i).longValue(),tm);
                    } else{
                        Log.i(TAG,"it is negative: "+derivatives[i]);

                        negative = true;
                    }

                }
                buffer.clear();
                timeStampBuffer.clear();
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
    private void onStepDetected(long timestamp, float[] values) {
        stepCount++;
        for (OnStepListener stepListener : mStepListeners) {
            stepListener.onStepDetected(timestamp, values);
            stepListener.onStepCountUpdated(stepCount);
        }
    }
}
