package ca.uoguelph.pspenler.maptracker;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class DatabasePool {
    private static final String tempdir = "temp";
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

    public static boolean moveDb(Context ctx, String name) {
        experimentDb.close();

        // Rename (move)
        File src = ctx.getDatabasePath(experimentDb.getDatabaseName());
        File dest = new File(ctx.getDir(tempdir, Context.MODE_PRIVATE), name);
        Log.d("DB", String.format("Moving %s -> %s", src.getPath(), dest.getPath()));
        boolean res = src.renameTo(dest);
        startDatabase(ctx);
        return res;

    }

    public static List<File> listFailedSends(Context ctx) {
        File f = ctx.getDir(tempdir, Context.MODE_PRIVATE);
        Log.d("DB", "File path" + f.getPath());
        File[] files = f.listFiles();
        Log.d("files not null", String.format("File is null %b", files != null));
        if (files != null) {
            return new ArrayList<>(Arrays.asList(files));
        }
        return new ArrayList<>();
    }
}
