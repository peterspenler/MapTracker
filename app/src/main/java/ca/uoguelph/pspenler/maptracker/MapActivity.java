package ca.uoguelph.pspenler.maptracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;

public class MapActivity extends AppCompatActivity {

    private Configuration config;
    private Bitmap mapImage;
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
