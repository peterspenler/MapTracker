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

    public static void finishDb(Configuration config, Context context){
        int fileExists = 1;
        String fileStr = "file:///storage/emulated/0/Documents/" + config.getName() + "_" + config.getBeaconLabel() + "_position_log.csv";
        Uri uri = Uri.parse(fileStr);
        while (fileExists != 0){
            File file = new File(uri.getPath());
            if (file.exists()) {
                fileStr = "file:///storage/emulated/0/Documents/" + config.getName() + "_" + config.getBeaconLabel() + "_position_log" + Integer.toString(fileExists) + ".csv";
                fileExists++;
            }else{
                break;
            }
            uri = Uri.parse(fileStr);
        }
        experimentDb.finishDatabse(config, fileExists - 1);
        context.deleteDatabase(experimentDb.getDatabaseName());
        startDatabase(context);
    }
}
