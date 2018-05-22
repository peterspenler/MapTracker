package ca.uoguelph.pspenler.maptracker;

import android.content.Context;

public final class DatabasePool {

    private static DatabaseHelper experimentDb;

    public static void startDatabase(Context context) {
        experimentDb = new DatabaseHelper(context);
    }

    public static DatabaseHelper getDb() {
        return experimentDb;
    }

    public static void finishDb(Context context) {
        context.deleteDatabase(experimentDb.getDatabaseName());
        startDatabase(context);
    }

    public static void deleteDb(Context context) {
        context.deleteDatabase(experimentDb.getDatabaseName());
        startDatabase(context);
    }
}
