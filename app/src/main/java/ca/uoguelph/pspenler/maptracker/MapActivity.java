package ca.uoguelph.pspenler.maptracker;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

interface AsyncResponse {
    void processFinish(String output);
}

public class MapActivity extends AppCompatActivity implements AsyncResponse {

    private Configuration config;
    private MapImageView mImageView;

    private String mapPath;

    private static Dialog progressDialog;
    private ProgressBar progressBar;

    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mImageView = findViewById(R.id.mapImageView);
        getSupportActionBar().setTitle("Experiment: unpaused");

        findViewById(R.id.pause_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPaused = !isPaused;
                isPaused = mImageView.setPaused(isPaused);
                mImageView.displayToast("Paused: " + (isPaused ? "yes" : "no"));
                getSupportActionBar().setTitle("Experiment: " + (isPaused ? "paused" : "unpaused"));
                FloatingActionButton fab = findViewById(R.id.pause_button);
                if (!isPaused) {
                    fab.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    fab.setImageResource(android.R.drawable.ic_media_play);
                }

            }
        });

        config = getIntent().getParcelableExtra("configObject");
        mapPath = getIntent().getStringExtra("mapPath");
        initMapView();
    }

    private void initMapView() {
        String type = config.getImagePath().substring(0, Math.min(config.getImagePath().length(), 4));

        if (type.equals("http") && (mapPath == null || mapPath.equals(""))) {
            progressDialog = new Dialog(this, R.style.Theme_AppCompat_Dialog_Alert);
            progressDialog.setTitle("Uploading data");
            progressDialog.setContentView(R.layout.dialog_upload);
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_dark)));
            progressBar = progressDialog.getWindow().findViewById(R.id.uploadDataProgress);
            progressBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.colorAccent), android.graphics.PorterDuff.Mode.SRC_IN);
            TextView title = progressDialog.findViewById(R.id.uploadDataTitle);
            title.setText("Downloading Map");
            progressDialog.show();

            DownloadMapImage downloader = new DownloadMapImage();
            downloader.delegate = this;
            downloader.execute(config.getImagePath(), "file://" + Environment.getExternalStorageDirectory().toString() + "/Documents/maptracker.png");
        } else {
            if (type.equals("file")) {
                mapPath = config.getImagePath();
            }
            initImageView();
        }
    }

    private void initImageView() {
        try {
            mImageView.setImageUri(mapPath, config.getLandmarks());
        } catch (NullPointerException e) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void processFinish(String output) {
        String type = output.substring(0, Math.min(config.getImagePath().length(), 4));
        if (type.equals("file")) {
            mapPath = output;
            initImageView();
        } else {
            Toast.makeText(this, output, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        mImageView.closeSensorMonitors();
        progressDialog.dismiss();
        Intent intent = new Intent();
        intent.putExtra("mapPath", mapPath);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        mImageView.closeSensorMonitors();
        progressDialog.dismiss();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mImageView.closeSensorMonitors();
        progressDialog.dismiss();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mImageView.openSensorMonitors();
        super.onResume();
    }

    class DownloadMapImage extends AsyncTask<String, Integer, String> {
        String filepath = "";
        AsyncResponse delegate = null;

        @Override
        protected String doInBackground(String... strings) {
            int count;
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(), 8192);
                filepath = strings[1];

                // Output stream

                File fpath = new File(filepath.substring(filepath.indexOf(":///")+3));

                File dirname = new File(fpath.getParent() + "/");
                dirname.mkdirs();
                OutputStream output = new FileOutputStream(Uri.parse(filepath).getPath());
                byte data[] = new byte[1024];

                long total = 0;
                long length = connection.getContentLength();
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress((int) ((total * 100) / length));
                    output.write(data, 0, count);
                }
                // flushing output
                output.flush();
                output.close();
                input.close();
                connection.disconnect();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            return "Could not load map, check configuration file";
            }

            return filepath;
        }

        protected void onProgressUpdate(Integer... progress) {
            progressBar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            progressDialog.dismiss();
            delegate.processFinish(s);
        }
    }
}
