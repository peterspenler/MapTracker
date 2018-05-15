package ca.uoguelph.pspenler.maptracker;

import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class ResultsPoster {

    private static int resultCode = 0;

    public static int post(final String resultsServer, final Configuration config) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(resultsServer);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("Experiment Name", config.getName());
                    jsonParam.put("SubmissionDatetime", getDatetime());
                    jsonParam.put("Configuration File", config.getConfigFile());
                    jsonParam.put("Beacon Label", config.getBeaconLabel());
                    jsonParam.put("Beacon Height", config.getBeaconHeight());
                    jsonParam.put("PositionLog", DatabasePool.getDb().JSONPositionArray());
                    jsonParam.put("AccelerometerData", DatabasePool.getDb().JSONAccelerometerArray());
                    jsonParam.put("CompassData", DatabasePool.getDb().JSONCompassArray());

                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();


                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    resultCode = conn.getResponseCode();
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        while(thread.isAlive());

        return resultCode;
    }

    private static String getDatetime(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.CANADA);
        dateFormat.setTimeZone(TimeZone.getDefault());
        return dateFormat.format(new Date());
    }
}
