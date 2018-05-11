package ca.uoguelph.pspenler.maptracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class AccelerometerHandler implements SensorEventListener{
    private Sensor accelerometer;
    private SensorManager manager;

    AccelerometerHandler(Context context){
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        try {
            accelerometer = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        DatabasePool.getDb().insertAccelData(event.values[0], event.values[1], event.values[2]);
        Log.d("ACCEL", Float.toString(event.values[0]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Unused
    }

    public void close(){
        manager.unregisterListener(this);
    }
}
