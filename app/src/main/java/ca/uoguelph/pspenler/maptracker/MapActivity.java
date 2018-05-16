package ca.uoguelph.pspenler.maptracker;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

interface AsyncResponse {
    void processFinish(String output);
}

public class MapActivity extends AppCompatActivity implements AsyncResponse{

    private Configuration config;
    private MapImageView mImageView;
    private String errorMsg;

    private String mapPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mImageView = findViewById(R.id.mapImageView);

        config = getIntent().getParcelableExtra("configObject");
        mapPath = getIntent().getStringExtra("mapPath");
        initMapView();
    }

    private void initMapView(){
        String type = config.getImagePath().substring(0, Math.min(config.getImagePath().length(), 4));

        if(type.equals("http") && (mapPath == null || mapPath.equals(""))){
                DownloadMapImage downloader = new DownloadMapImage();
                downloader.delegate = this;
                Log.d("FILEPATH", "file://" + Environment.getExternalStorageDirectory().toString() + "/Documents/map2.png");
                downloader.execute(config.getImagePath(), "file://" + Environment.getExternalStorageDirectory().toString() + "/Documents/map2.png");
        } else{
            if(type.equals("file")) {
                mapPath = config.getImagePath();
            }
            initImageView();
        }
    }

    private void initImageView(){
        Log.d("INITIMAGEVIEW", "CALLED");
        try {
            mImageView.setImageUri(mapPath, config.getLandmarks());
        }catch (NullPointerException e){
            errorMsg = "Map image could not be loaded, Please check configuration";
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void processFinish(String output) {
        String type = output.substring(0, Math.min(config.getImagePath().length(), 4));
        if(type.equals("file")){
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
        Intent intent = new Intent();
        intent.putExtra("mapPath", mapPath);
        setResult(RESULT_OK, intent);
        finish();
    }

    static class DownloadMapImage extends AsyncTask<String, Void, String>{
        String filepath = "";
        public AsyncResponse delegate = null;

        @Override
        protected String doInBackground(String... strings) {
            int count;
            try {
                URL url = new URL(strings[0]);
                URLConnection connection = url.openConnection();
                connection.connect();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),8192);
                filepath = strings[1];

                // Output stream
                //Uri uri = Uri.parse(filepath).getPath();
                OutputStream output = new FileOutputStream(Uri.parse(filepath).getPath());
                byte data[] = new byte[1024];
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
                // flushing output
                output.flush();
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
                return "Could not load map, check configuration file";
            }

            return filepath;
        }

        @Override
        protected void onPostExecute(String s) {
            delegate.processFinish(s);
        }
    }
}
