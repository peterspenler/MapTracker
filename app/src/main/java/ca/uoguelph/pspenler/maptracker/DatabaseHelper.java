package ca.uoguelph.pspenler.maptracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DatabaseHelper extends SQLiteOpenHelper{
    private static final String DATABASE_NAME = "experiment_data.db";
    private static final String LANDMARK_TABLE_NAME = "position_log";
    private static final String ACCELEROMETER_TABLE_NAME = "acceleration_log";

    private static final String DATETIME = "datetime";
    private static final String REALX = "realX";
    private static final String REALY = "realY";

    private static final String REALXA = "realXAcc";
    private static final String REALYA = "realYAcc";
    private static final String REALZA = "realZAcc";


    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + LANDMARK_TABLE_NAME + "(" + DATETIME + " TEXT, " + REALX + " INTEGER, " + REALY + " INTEGER)");
        db.execSQL("create table " + ACCELEROMETER_TABLE_NAME + "(" + DATETIME + " TEXT, " + REALXA + " FLOAT, " + REALYA + " FLOAT, " + REALZA + " FLOAT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LANDMARK_TABLE_NAME);
        onCreate(db);
    }

    private String getDatetime(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.CANADA);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }

    public boolean insertLandmarkData(int realX, int realY){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATETIME, getDatetime());
        contentValues.put(REALX, realX);
        contentValues.put(REALY, realY);
        long result = db.insert(LANDMARK_TABLE_NAME, null, contentValues);
        if(result == -1){
            return false;
        }
        return true;
    }

    public boolean insertAccelData(float realX, float realY, float realZ){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATETIME, getDatetime());
        contentValues.put(REALXA, realX);
        contentValues.put(REALYA, realY);
        contentValues.put(REALZA, realZ);
        long result = db.insert(ACCELEROMETER_TABLE_NAME, null, contentValues);
        if(result == -1){
            return false;
        }
        return true;
    }

    public void finishDatabse(Configuration config, int fileNum){
        Uri uri;
        File file;
        String tableName = LANDMARK_TABLE_NAME;
        for(int i = 0; i < 2; i++) {
            if (fileNum == 0) {
                uri = Uri.parse("file:///storage/emulated/0/Documents/" + config.getName() + "_" + config.getBeaconLabel() + "_<" + tableName + ">.csv");
                file = new File(uri.getPath());
            } else {
                uri = Uri.parse("file:///storage/emulated/0/Documents/" + config.getName() + "_" + config.getBeaconLabel() + "_<" + tableName + ">" + Integer.toString(fileNum) + ".csv");
                file = new File(uri.getPath());
            }
            try {
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                SQLiteDatabase db = DatabasePool.getDb().getReadableDatabase();
                Cursor curCSV = db.rawQuery("SELECT * FROM " + tableName, null);
                csvWrite.writeNext(curCSV.getColumnNames());
                while (curCSV.moveToNext()) {
                    if(i == 0) {
                        String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2)};
                        csvWrite.writeNext(arrStr, false);
                    }
                    if(i == 1){
                        String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3)};
                        csvWrite.writeNext(arrStr, false);
                    }
                }
                csvWrite.close();
                curCSV.close();
            } catch (Exception sqlEx) {
                Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
            }
            tableName = ACCELEROMETER_TABLE_NAME;
        }
    }
}
