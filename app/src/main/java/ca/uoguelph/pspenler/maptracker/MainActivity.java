package ca.uoguelph.pspenler.maptracker;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {

    private static int isConfigured = 0;
    private static Configuration configuration;

    private Button experimentButton;
    private Button finishExperimentButton;

    private int requestCode = 0x0;
    private int grantResults[] = null;
    private static boolean hasInternet;

    private static String mapPath = "";

    private ProgressBar progressBar;
    private Dialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configuration = new Configuration();
        DatabasePool.startDatabase(this);
        DatabasePool.deleteDb(this);

        experimentButton = findViewById(R.id.experimentButton);
        finishExperimentButton = findViewById(R.id.finishExperimentButton);

        experimentButton.setVisibility(View.GONE);
        finishExperimentButton.setVisibility(View.GONE);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        onRequestPermissionsResult(requestCode, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, grantResults);
    }

    public void launchConfiguration(View view) {
        Intent intent = new Intent(this, ConfigureActivity.class);
        intent.putExtra("configObject", configuration);
        startActivityForResult(intent, 1);
    }

    public void launchExperiment(View view) {
        if (isConfigured == 1) {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("configObject", configuration);
            intent.putExtra("mapPath", mapPath);
            startActivityForResult(intent, 2);
        } else {
            Toast.makeText(MainActivity.this, "Must configure before experiment", Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                isConfigured = 1;
                configuration = data.getParcelableExtra("configObject");
                experimentButton.setVisibility(View.VISIBLE);
                finishExperimentButton.setVisibility(View.VISIBLE);
                mapPath = "";
            }
        }
        if (requestCode == 2) {
            if (resultCode == RESULT_CANCELED) {
                finishExperimentButton.setVisibility(View.GONE);
            } else if (resultCode == RESULT_OK) {
                mapPath = data.getStringExtra("mapPath");
            }
        }
    }

    public void finishExperiment(View view) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);

        builder.setTitle("Finish experiment")
                .setMessage("This will finish the experiment and save the experimental data. Continue?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        sendData();
                    }
                })
                .setNegativeButton(android.R.string.no, null);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_dark)));
        dialog.show();
    }

    @Override // android recommended class to handle permissions
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d("permission", "granted");
                } else {
                    // permission denied
                    Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                    onDestroy();
                }
            }
        }
    }

    public static boolean hasInternetAccess(final String url) {
        hasInternet = false;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection urlc = (HttpURLConnection) new URL(url).openConnection();
                    urlc.setRequestProperty("User-Agent", "Android");
                    urlc.setRequestProperty("Connection", "close");
                    urlc.setConnectTimeout(1500);
                    urlc.connect();
                    hasInternet = urlc.getResponseCode() == 204 && urlc.getContentLength() <= 0;
                    Log.d("HASINTERNET", Integer.toString(urlc.getContentLength()));
                    urlc.disconnect();
                } catch (IOException e) {
                    Log.e("INTERNET ACCESS", "Error checking internet connection", e);
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return hasInternet;
    }

    @Override
    protected void onDestroy() {
        if (mapPath != null && !mapPath.equals("")) {
            new File(Uri.parse(mapPath).getPath()).delete();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (mapPath != null && !mapPath.equals("")) {
            new File(Uri.parse(mapPath).getPath()).delete();
        }
        super.onStop();
    }

    public void sendData() {
        if (configuration.getResultsServer().equals("")) {
            // Do local saving

        }
        progressDialog = new Dialog(this, R.style.Theme_AppCompat_Dialog_Alert);
        progressDialog.setTitle("Uploading data");
        progressDialog.setContentView(R.layout.dialog_upload);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_dark)));
        progressBar = progressDialog.getWindow().findViewById(R.id.uploadDataProgress);
        progressBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.SRC_IN);
        progressDialog.show();
        try {
            new UploadData().execute(configuration.getResultsServer() + "/" + URLEncoder.encode(configuration.getName(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    class UploadData extends AsyncTask<String, String, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(String... urls) {
            int resultCode = 0;
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("Experiment Name", configuration.getName());
                jsonParam.put("SubmissionDatetime", getDatetime());
                jsonParam.put("Configuration File", configuration.getConfigFile());
                jsonParam.put("Beacon Label", configuration.getBeaconLabel());
                jsonParam.put("Beacon Height", configuration.getBeaconHeight());
                jsonParam.put("PositionLog", DatabasePool.getDb().JSONPositionArray());
                jsonParam.put("AccelerometerData", DatabasePool.getDb().JSONAccelerometerArray());
                jsonParam.put("CompassData", DatabasePool.getDb().JSONCompassArray());

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                byte[] stringBytes = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
                ByteArrayInputStream is = new ByteArrayInputStream(stringBytes);

                int maxBufferSize = 4096;
                byte[] buffer = new byte[maxBufferSize];
                long length = stringBytes.length;
                int bytesRead;
                long total = 0;

                while ((bytesRead = is.read(buffer, 0, maxBufferSize)) > 0) {
                    total += bytesRead;
                    publishProgress("" + (int) ((total * 100) / length));
                    os.write(buffer, 0, bytesRead);
                }

                os.flush();
                os.close();

                resultCode = conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return resultCode;
        }


        protected void onProgressUpdate(String... progress) {
            progressBar.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            progressDialog.dismiss();

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle)
                    .setTitle("Upload Result")
                    .setMessage("")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton("Ok", null);
            AlertDialog uploadDialog = builder.create();
            uploadDialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_dark)));
            uploadDialog.show();

            uploadDialog.setIcon(android.R.drawable.ic_dialog_info);
            if (resultCode == 201) {
                DatabasePool.finishDb(getBaseContext());
                uploadDialog.setMessage("Success!");
                if ((mapPath != null) && !mapPath.equals("")) {
                    new File(Uri.parse(mapPath).getPath()).delete();
                }
                experimentButton.setVisibility(View.GONE);
                finishExperimentButton.setVisibility(View.GONE);
            } else if (resultCode == 409) {
                uploadDialog.setMessage("This experiment name already exists. Please change the name in the configuration");
            } else if (resultCode == 404 || resultCode == 0) {
                if (!hasInternetAccess("http://clients3.google.com/generate_204")) {
                    uploadDialog.setMessage("No internet connection. Please check network settings");
                } else if (configuration.getName().contains("/")) {
                    uploadDialog.setMessage("The experiment name is invalid. Please remove any '/' characters from the experiment name");
                } else {
                    uploadDialog.setMessage("Invalid results server");
                }
            } else if (resultCode == 412) {
                uploadDialog.setMessage("The submission has empty or invalid values and cannot be accepted. Please check the configuration");
            } else {
                uploadDialog.setMessage("RESULT CODE: " + Integer.toString(resultCode));
            }
        }

        private String getDatetime() {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.CANADA);
            dateFormat.setTimeZone(TimeZone.getDefault());
            return dateFormat.format(new Date());
        }

    }
}
