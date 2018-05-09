package ca.uoguelph.pspenler.maptracker;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MapActivity extends AppCompatActivity {

    private Configuration config;
    private MapImageView mImageView;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mImageView = findViewById(R.id.mapImageView);

        config = getIntent().getParcelableExtra("configObject");
        uri = Uri.parse(config.getImagePath());
        pinchZoomPan();
    }

    private void pinchZoomPan(){
        mImageView.setImageUri(uri);
    }

}
