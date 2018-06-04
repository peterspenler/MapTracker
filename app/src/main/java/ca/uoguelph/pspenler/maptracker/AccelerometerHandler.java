package ca.uoguelph.pspenler.maptracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AccelerometerHandler implements SensorEventListener {
    private Sensor accelerometer;
    private SensorManager manager;

    private boolean paused = false;

    AccelerometerHandler(Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        try {
            accelerometer = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        open();
    }


    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (paused) {
            return;
        }
        DatabasePool.getDb().insertAccelData(event.values[0], event.values[1], event.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Unused
    }

    public void close() {
        manager.unregisterListener(this);
    }

    public void open() {
        if (accelerometer != null) {
            manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
}
