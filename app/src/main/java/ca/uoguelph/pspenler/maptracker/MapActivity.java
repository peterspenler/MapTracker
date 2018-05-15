package ca.uoguelph.pspenler.maptracker;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;


public class MapActivity extends AppCompatActivity {

    private Configuration config;
    private MapImageView mImageView;
    private Uri uri;
    private int imageLoaded = 0;
    private String errorMsg;
    private Bitmap mapImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mImageView = findViewById(R.id.mapImageView);

        config = getIntent().getParcelableExtra("configObject");
        initMapView();
    }

    private void initMapView(){
        imageLoaded = 0;

        @SuppressLint("HandlerLeak") final Handler mHandler = new Handler(){

            public void handleMessage(Message msg) {
                Bundle b;
                if(msg.what == 1){
                    b = msg.getData();

                    mapImage = b.getParcelable("bitmap");

                    imageLoaded = 1;
                }
                if(msg.what == 2){
                    b = msg.getData();
                    errorMsg = b.getString("errorMsg");

                    imageLoaded = 2;
                }
                super.handleMessage(msg);
            }
        };

        Thread thread = new BitmapLoader(config.getImagePath(),this,mHandler);
        thread.start();
        while(thread.isAlive());

        if(imageLoaded == 2){
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            config.invalidate();
            setResult(RESULT_CANCELED);
            finish();
        }
        try {
            mImageView.setImageUri(mapImage, config.getLandmarks());
        }catch (NullPointerException e){
            Toast.makeText(this, "Map image does not exist, check configuration file", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        mImageView.closeSensorMonitors();
        setResult(RESULT_OK);
        finish();
    }
}
