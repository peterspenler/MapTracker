package ca.uoguelph.pspenler.maptracker;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static int isConfigured = 0;
    private static Configuration configuration;

    private Button experimentButton;
    private Button finishExperimentButton;

    private int requestCode;
    private int grantResults[];
    private static boolean hasInternet;

    private static String mapPath = "";

    private ProgressBar progressBar;
    private Dialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        configuration = new Configuration();
        DatabasePool.startDatabase(this);
        DatabasePool.deleteDb(this);

        experimentButton = findViewById(R.id.experimentButton);
        finishExperimentButton = findViewById(R.id.finishExperimentButton);

        experimentButton.setVisibility(View.GONE);
        finishExperimentButton.setVisibility(View.GONE);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        onRequestPermissionsResult(requestCode,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, grantResults);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_configure) {
            // Handle the camera action
        } else if (id == R.id.nav_experiment) {
            Toast.makeText(getApplicationContext(), "Started the gallery", Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void launchConfiguration(View view) {
        Intent intent = new Intent(this, ConfigureActivity.class);
        intent.putExtra("configObject", configuration);
        startActivityForResult(intent, 1);
    }

    public void launchExperiment(View view) {
        if(isConfigured == 1){
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("configObject", configuration);
            intent.putExtra("mapPath", mapPath);
            startActivityForResult(intent, 2);
        } else{
            Toast.makeText(MainActivity.this, "Must configure before experiment", Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            if(resultCode == RESULT_OK){
                isConfigured = 1;
                configuration = data.getParcelableExtra("configObject");
                experimentButton.setVisibility(View.VISIBLE);
                finishExperimentButton.setVisibility(View.VISIBLE);
                mapPath = "";
            }
        }
        if(requestCode == 2){
            if(resultCode == RESULT_CANCELED){
                //experimentButton.setVisibility(View.GONE);
                finishExperimentButton.setVisibility(View.GONE);
            } else if(resultCode == RESULT_OK){
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
                        sendToServer();
                    }})
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
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission
                    Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                    //app cannot function without this permission for now so close it...
                    onDestroy();
                }
            }
        }
    }

    public void sendToServer(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogStyle).setMessage("Saving to server...").setIcon(android.R.drawable.ic_menu_send).setPositiveButton("Ok", null).setTitle("");
        AlertDialog uploadDialog = builder.create();
        uploadDialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_dark)));
        uploadDialog.show();

        try {
            int resultCode = ResultsPoster.post(configuration.getResultsServer() + "/" + URLEncoder.encode(configuration.getName(), "UTF-8"), configuration); //Saves to server
            uploadDialog.setTitle("Upload Result");
            uploadDialog.setIcon(android.R.drawable.ic_dialog_info);
            if(resultCode == 201) {
                DatabasePool.finishDb(configuration, getBaseContext(), 1);
                uploadDialog.setMessage("Success!");
                if((mapPath != null) && !mapPath.equals("")) {
                    new File(Uri.parse(mapPath).getPath()).delete();
                }
                experimentButton.setVisibility(View.GONE);
                finishExperimentButton.setVisibility(View.GONE);
            } else if(resultCode == 409) {
                uploadDialog.setMessage("This experiment name already exists. Please change the name in the configuration");
            } else if((resultCode == 404) || (resultCode == 0)){
                if(!hasInternetAccess("http://clients3.google.com/generate_204")){
                    uploadDialog.setMessage("No internet connection. Please check network settings");
                } else if(configuration.getName().contains("/")){
                    uploadDialog.setMessage("The experiment name is invalid. Please remove any '/' characters from the experiment name");
                } else{
                    uploadDialog.setMessage("Invalid results server");
                }
                uploadDialog.setMessage("The experiment name is invalid. Please change the name in the configuration");
            } else if(resultCode == 412){
                uploadDialog.setMessage("The submission has empty or invalid values and cannot be accepted. Please check the configuration");
            } else{
                uploadDialog.setMessage("RESULT CODE: " + Integer.toString(resultCode));
            }
        }catch(UnsupportedEncodingException e){
            Toast.makeText(MainActivity.this, "Encoding error, check experiment name", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public static boolean hasInternetAccess(final String url) {
        hasInternet = false;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection urlc = (HttpURLConnection) (new URL(url).openConnection());
                    urlc.setRequestProperty("User-Agent", "Android");
                    urlc.setRequestProperty("Connection", "close");
                    urlc.setConnectTimeout(1500);
                    urlc.connect();
                    hasInternet = (urlc.getResponseCode() == 204 && urlc.getContentLength() <= 0);
                    Log.d("HASINTERNET", Integer.toString(urlc.getContentLength()));
                } catch (IOException e) {
                    Log.e("INTERNET ACCESS", "Error checking internet connection", e);
                }
            }
        });
        thread.start();
        while(thread.isAlive());
        return hasInternet;
    }

    @Override
    protected void onDestroy() {
        if((mapPath != null) && !mapPath.equals("")){
            new File(Uri.parse(mapPath).getPath()).delete();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if((mapPath != null) && !mapPath.equals("")){
            new File(Uri.parse(mapPath).getPath()).delete();
        }
        super.onStop();
    }

    //EXPERIMENTAL CODE BELOW HERE
    public void sendData(View view){
        progressDialog = new Dialog(this, R.style.Theme_AppCompat_Dialog_Alert);
        progressDialog.setTitle("Uploading data");

        /*
        RelativeLayout layout = new RelativeLayout(dialog.getContext());
        progressBar = new ProgressBar(dialog.getContext());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100,100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(progressBar,params);
        progressBar.setVisibility(View.VISIBLE);
    */
        progressDialog.setContentView(R.layout.dialog_upload);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.background_dark)));
        progressBar = progressDialog.getWindow().findViewById(R.id.uploadDataProgress);
        progressDialog.show();
        new UploadData().execute();
    }

    class UploadData extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //showDialog(0);
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            try {

                int lengthOfFile = 100;

                for(int total = 0; total < 101; total++) {
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lengthOfFile));
                    Thread.sleep(100);
                }

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            progressBar.setProgress(Integer.parseInt(progress[0]));
            //Toast.makeText(getApplicationContext(), progress[0],Toast.LENGTH_SHORT);
            Log.d("PROGRESS", progress[0]);
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            progressDialog.hide();
        }

    }
}
