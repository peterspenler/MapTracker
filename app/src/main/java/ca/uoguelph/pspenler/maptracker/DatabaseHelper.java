package ca.uoguelph.pspenler.maptracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
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
    private static final String LANDMARK_TABLE_NAME = "landmark_table";
    private static final String ACCELEROMETER_TABLE_NAME = "accelerometer_table";

    private static final String DATETIME = "datetime";
    private static final String REALX = "realX";
    private static final String REALY = "realY";

    public static final String REALXA = "realXAcc";
    public static final String REALYA = "realYAcc";
    public static final String REALZA = "realZAcc";


    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + LANDMARK_TABLE_NAME + "(DATETIME TEXT PRIMARY KEY, REALX INTEGER, REALY INTEGER)");
        db.execSQL("create table " + ACCELEROMETER_TABLE_NAME + "(DATETIME TEXT PRIMARY KEY, REALXA INTEGER, REALYA INTEGER, REALZA INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LANDMARK_TABLE_NAME);
        onCreate(db);
    }

    public boolean insertLandmarkData(int realX, int realY){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.CANADA);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = dateFormat.format(new Date());
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATETIME, date);
        contentValues.put(REALX, realX);
        contentValues.put(REALY, realY);
        long result = db.insert(LANDMARK_TABLE_NAME, null, contentValues);
        if(result == -1){
            return false;
        }

        Cursor cursor = db.rawQuery("select * from " + LANDMARK_TABLE_NAME, null);
        if(cursor != null && cursor.moveToFirst()) {
            do {
                Log.d("DATETIME", cursor.getString(cursor.getColumnIndex("DATETIME")));
                Log.d("REALX", cursor.getString(cursor.getColumnIndex("REALX")));
                Log.d("REALY", cursor.getString(cursor.getColumnIndex("REALY")));


            } while (cursor.moveToNext());
        }
        cursor.close();
        return true;
    }

    public void finishDatabse(Uri uri){
        File file = new File(uri.getPath());
        try
        {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = DatabasePool.getDb().getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM " + LANDMARK_TABLE_NAME,null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while(curCSV.moveToNext())
            {
                String arrStr[] ={curCSV.getString(0),curCSV.getString(1), curCSV.getString(2)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
        }
        catch(Exception sqlEx)
        {
            Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
        }
    }
}
