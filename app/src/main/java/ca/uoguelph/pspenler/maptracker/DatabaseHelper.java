package ca.uoguelph.pspenler.maptracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public final class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "experiment_data.db";
    public static final int VERSION = 3;
    private static final String LANDMARK_TABLE_NAME = "position_log";
    private static final String ACCELEROMETER_TABLE_NAME = "acceleration_log";
    private static final String COMPASS_TABLE_NAME = "compass_log";

    private static final String DATETIME = "datetime";
    private static final String REALX = "realX";
    private static final String REALY = "realY";
    private static final String PAUSE = "paused";

    private static final String REALXA = "realXAcc";
    private static final String REALYA = "realYAcc";
    private static final String REALZA = "realZAcc";



    private static final String AZIMUTH = "azimuth";
    private static final String MAGFIELD = "magneticField";

    DatabaseHelper(Context context, String dbname) { super(context, dbname, null, VERSION); }

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + LANDMARK_TABLE_NAME + "(" + DATETIME + " TEXT, " + REALX + " FLOAT, " + REALY + " FLOAT, " + PAUSE + " INTEGER)");
        db.execSQL("create table " + ACCELEROMETER_TABLE_NAME + "(" + DATETIME + " TEXT, " + REALXA + " FLOAT, " + REALYA + " FLOAT, " + REALZA + " FLOAT)");
        db.execSQL("create table " + COMPASS_TABLE_NAME + "(" + DATETIME + " TEXT, " + AZIMUTH + " FLOAT, " + MAGFIELD + " FLOAT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LANDMARK_TABLE_NAME);
        onCreate(db);
    }

    private String getDatetime() {
        final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        return dateTimeFormatter.format(new Date());
    }


    public boolean insertLandmarkData(float realX, float realY) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATETIME, getDatetime());
        contentValues.put(REALX, realX);
        contentValues.put(REALY, realY);
        contentValues.put(PAUSE, -1);
        long result = db.insert(LANDMARK_TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public boolean insertLandmarkPause(boolean pause) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATETIME, getDatetime());
        contentValues.put(REALX, 0.0);
        contentValues.put(REALY, 0.0);
        contentValues.put(PAUSE, pause ? 1 : 0);
        long result = db.insert(LANDMARK_TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public boolean insertAccelData(float realX, float realY, float realZ) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATETIME, getDatetime());
        contentValues.put(REALXA, realX);
        contentValues.put(REALYA, realY);
        contentValues.put(REALZA, realZ);
        long result = db.insert(ACCELEROMETER_TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public boolean insertCompassData(float azimuth, float magneticField) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATETIME, getDatetime());
        contentValues.put(AZIMUTH, azimuth);
        contentValues.put(MAGFIELD, magneticField);
        long result = db.insert(COMPASS_TABLE_NAME, null, contentValues);
        return result != -1;

    }

    public JSONArray JSONPositionArray() {
        JSONArray ja = new JSONArray();
        try {
            SQLiteDatabase db = DatabasePool.getDb().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + LANDMARK_TABLE_NAME, null);
            while (cursor.moveToNext()) {
                JSONObject jo = new JSONObject();
                jo.put("Datetime", cursor.getString(0));

                JSONArray da = new JSONArray();
                da.put(cursor.getFloat(1));
                da.put(cursor.getFloat(2));

                jo.put("Data", da);
                jo.put("Paused", cursor.getInt(3));

                ja.put(jo);
            }
            cursor.close();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ja;
    }

    public JSONArray JSONAccelerometerArray() {
        JSONArray ja = new JSONArray();
        try {
            SQLiteDatabase db = DatabasePool.getDb().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + ACCELEROMETER_TABLE_NAME, null);
            while (cursor.moveToNext()) {
                JSONObject jo = new JSONObject();
                jo.put("Datetime", cursor.getString(0));

                JSONArray da = new JSONArray();
                da.put(cursor.getFloat(1));
                da.put(cursor.getFloat(2));
                da.put(cursor.getFloat(3));

                jo.put("Data", da);

                ja.put(jo);
            }
            cursor.close();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ja;
    }

    public JSONArray JSONCompassArray() {
        JSONArray ja = new JSONArray();
        try {
            SQLiteDatabase db = DatabasePool.getDb().getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + COMPASS_TABLE_NAME, null);
            while (cursor.moveToNext()) {
                JSONObject jo = new JSONObject();
                jo.put("Datetime", cursor.getString(0));

                JSONArray da = new JSONArray();
                da.put(cursor.getFloat(1));
                da.put(cursor.getFloat(2));

                jo.put("Data", da);

                ja.put(jo);
            }
            cursor.close();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ja;
    }

}
