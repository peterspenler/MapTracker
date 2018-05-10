package ca.uoguelph.pspenler.maptracker;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

public class MapActivity extends AppCompatActivity {

    private Configuration config;
    private MapImageView mImageView;
    private Uri uri;
    private ArrayList<LandmarkXY> points;

    private int cursorX;
    private int cursorY;
    private int actionBarHeight;

    int canvasX;
    int canvasY;
    float canvasScale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mImageView = findViewById(R.id.mapImageView);

        config = getIntent().getParcelableExtra("configObject");
        uri = Uri.parse(config.getImagePath());
        pinchZoomPan();

        //Get actionbar height
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
            //Log.d("ACTIONBAR", Integer.toString(actionBarHeight));
        }

        final ImageView cursor = findViewById(R.id.cursor);
        cursor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int height = cursor.getHeight();
                int width = cursor.getWidth();
                int x = cursor.getLeft();
                int y = cursor.getTop();
                Log.e("CURSOR POS", "X:" + Integer.toString(x + (width / 2)) + " Y:" + Integer.toString(y + (height / 2) + 65 + actionBarHeight));
                cursorX = x + (width / 2);
                cursorY = y + (height / 2) + 65 + actionBarHeight;
            }
        });
    }

    private void pinchZoomPan(){
        points = createPoints(config.getLandmarks());
        mImageView.setImageUri(uri, points);
    }

    public void addDataPoint(View view) {
        canvasX = (int) mImageView.getCanvasX();
        canvasY = (int) mImageView.getCanvasY();
        canvasScale = mImageView.getScaleFactor() / mImageView.getImageScaleFator();
        Log.d("CANVAS",  "X:" + Integer.toString(canvasX) + " Y:" + Integer.toString(canvasY) + " S;" + Float.toString(canvasScale));

        int pointX;
        int pointY;
        float dist;
        int closeID = -1;
        float closeDist = 999999999;

        for(int i = 0; i < points.size(); i++){
            pointX = canvasX + (int)(canvasScale * points.get(i).getX());
            pointY = canvasY + (int)(canvasScale * points.get(i).getY());
            Log.d("POINTS", "Point:" + Integer.toString(i) + " X:" + Integer.toString(pointX) + " Y:" + Integer.toString(pointY)+ " X:" + Integer.toString(cursorX) + " Y:" + Integer.toString(cursorY));
            dist = (float) Math.sqrt(Math.pow((pointX - cursorX), 2) + Math.pow((pointY - cursorY), 2));
            Log.e("DISTANCE", Integer.toString(i) + ": " + Float.toString(dist) + " CMP:" + Float.toString(50 * canvasScale));
            if((dist < closeDist)&&(dist < 25)){
                closeDist = dist;
                closeID = points.get(i).getId();
            }
        }

        if(closeID != -1){
            Toast.makeText(this, "Added point " + Integer.toString(closeID), Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "No point found", Toast.LENGTH_SHORT).show();
        }
    }

    private ArrayList<LandmarkXY> createPoints(ArrayList<Landmark> l){
        ArrayList<LandmarkXY> points = new ArrayList<>();
        for(int i = 0; i < l.size(); i++) {
            points.add(new LandmarkXY(l.get(i).getXDisplayLoc(), l.get(i).getYDisplayLoc(), l.get(i).getId()));
        }
        return points;
    }
}
