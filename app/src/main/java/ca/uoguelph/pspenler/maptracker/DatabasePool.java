package ca.uoguelph.pspenler.maptracker;

import android.content.Context;
import android.net.Uri;

import java.io.File;

public final class DatabasePool {

    private static DatabaseHelper experimentDb;

    public static void startDatabase(Context context){
        experimentDb = new DatabaseHelper(context);
    }

    public static DatabaseHelper getDb(){
        return experimentDb;
    }

    public static void finishDb(String uriStr, Context context){
        int fileExists = 1;
        String fileStr = uriStr + ".csv";
        Uri uri = Uri.parse(fileStr);
        while (fileExists != 0){
            File file = new File(uri.getPath());
            if (file.exists()) {
                fileStr = uriStr + Integer.toString(fileExists) + ".csv";
                fileExists++;
            }else{
                fileExists = 0;
            }
            uri = Uri.parse(fileStr);
        }
        experimentDb.finishDatabse(uri);
        context.deleteDatabase(experimentDb.getDatabaseName());
        startDatabase(context);
    }
}
