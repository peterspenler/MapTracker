package ca.uoguelph.pspenler.maptracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class CompassHandler implements SensorEventListener {
    private Sensor magnetometer;
    private Sensor rotationV;
    private SensorManager manager;


    private boolean paused = false;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    CompassHandler(Context context) {
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        try {
            magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        if (manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
            try {
                rotationV = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
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
        float[] rotationMatrix = new float[9];
        float[] orientation = new float[3];
        float azimuth;
        float magneticField = 0.0f;
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            azimuth = (float) (Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360) % 360;
            DatabasePool.getDb().insertCompassData(azimuth, magneticField);
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
            magneticField = (float) Math.sqrt(mLastMagnetometer[0] * mLastMagnetometer[0] + mLastMagnetometer[1] * mLastMagnetometer[1] + mLastMagnetometer[2] * mLastMagnetometer[2]);
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rotationMatrix, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuth = (float) (Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360) % 360;
            DatabasePool.getDb().insertCompassData(azimuth, magneticField);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Unused
    }

    public void close() {
        manager.unregisterListener(this);
    }

    public void open() {
        if (magnetometer != null) {
            manager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (rotationV != null) {
            manager.registerListener(this, rotationV, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
}