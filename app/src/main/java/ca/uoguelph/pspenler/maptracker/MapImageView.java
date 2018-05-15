package ca.uoguelph.pspenler.maptracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

public class MapImageView extends android.support.v7.widget.AppCompatImageView{

    private Bitmap mBitmap;
    private int mImageWidth;
    private int mImageHeight;

    private final static float minZoom = 1.f;
    private final static float maxZoom = 3.f;
    private float scaleFactor = 1;
    private float oScaleFactor = 1;
    private float imageScale = 1;
    private ScaleGestureDetector scaleGestureDetector;
    private AccelerometerHandler accelHandler;
    private CompassHandler compassHandler;

    private final static int NONE = 0;
    private final static int PAN = 1;
    private final static int ZOOM = 2;
    private int eventState;

    private static final int MAX_CLICK_DURATION = 130; //Change tap length
    private static final int MAX_TAP_DISTANCE = 30; //Change distance sensitivity
    private long startClickTime;

    private float startX = 0;
    private float startY = 0;
    private float translateX = 0;
    private float translateY = 0;
    private float prevTranslateX = 0;
    private float prevTranslateY = 0;

    Matrix scaleMatrix = new Matrix();
    ArrayList<Landmark> points;
    int numPoints = 0;
    Paint pointPaint;
    int touchMod = 0;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Float detectedScaleFactor = detector.getScaleFactor();
            if(oScaleFactor == 1) {
                scaleFactor = (detectedScaleFactor - 1) + oScaleFactor;
            }else{
                scaleFactor = 3 * (detectedScaleFactor - 1) + oScaleFactor;
            }
            scaleFactor = Math.max(minZoom, Math.min(maxZoom, scaleFactor));

            translateX = prevTranslateX + ((scaleFactor - oScaleFactor) * -1 * ((scaleGestureDetector.getFocusX() - prevTranslateX)/oScaleFactor));
            translateY = prevTranslateY + ((scaleFactor - oScaleFactor) * -1 * ((scaleGestureDetector.getFocusY() - prevTranslateY)/oScaleFactor));

            return super.onScale(detector);
        }
    }

    public MapImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction() & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:
                startClickTime = Calendar.getInstance().getTimeInMillis();
                if(eventState != ZOOM) {
                    eventState = PAN;
                    startX = event.getX() - prevTranslateX;
                    startY = event.getY() - prevTranslateY;
                }
                break;
            case MotionEvent.ACTION_UP:
                prevTranslateX = translateX;
                prevTranslateY = translateY;
                eventState = NONE;

                long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                if(clickDuration < MAX_CLICK_DURATION) {

                    int pointX;
                    int pointY;
                    float dist;
                    float canvasScale = scaleFactor/imageScale;
                    int closeID = -1;
                    float closeDist = 999999999;

                    for(int i = 0; i < points.size(); i++){
                        pointX = (int)prevTranslateX + (int)(canvasScale * points.get(i).getXDisplayLoc());
                        pointY = (int)prevTranslateY + (int)(canvasScale * points.get(i).getYDisplayLoc());
                        dist = (float) Math.sqrt(Math.pow((pointX - event.getX()), 2) + Math.pow((pointY - event.getY() - touchMod), 2));
                        if((dist < closeDist)&&(dist < MAX_TAP_DISTANCE)){
                            closeDist = dist;
                            closeID = points.get(i).getId();
                        }
                    }

                    if(closeID != -1){
                        Toast.makeText(getContext(), "Added point " + Integer.toString(closeID), Toast.LENGTH_SHORT).show();
                        DatabasePool.getDb().insertLandmarkData(points.get(closeID).getXLoc(), points.get(closeID).getYLoc());
                        if(accelHandler == null) {
                            accelHandler = new AccelerometerHandler(getContext());
                        }
                        if(compassHandler == null){
                            compassHandler = new CompassHandler(getContext());
                        }
                    }

                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(eventState != ZOOM) {
                    translateX = event.getX() - startX;
                    translateY = event.getY() - startY;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                eventState = ZOOM;
                oScaleFactor = scaleFactor;
                break;
        }
        scaleGestureDetector.onTouchEvent(event);

        if((eventState == PAN) || (eventState == ZOOM)){
            invalidate();
            requestLayout();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        scaleMatrix.setTranslate(imageScale * translateX/scaleFactor, imageScale * translateY/scaleFactor);
        scaleMatrix.postScale(scaleFactor/imageScale, scaleFactor/imageScale);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(mBitmap, 0, 0, null);
        for(int i = 0; i < numPoints; i++) {
            canvas.drawCircle(points.get(i).getXDisplayLoc(), points.get(i).getYDisplayLoc(), 20, pointPaint);
        }
        canvas.restore();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int imageWidth = MeasureSpec.getSize(widthMeasureSpec);
        int imageHeight = MeasureSpec.getSize(heightMeasureSpec);
        int scaledWidth = Math.round(mImageWidth * scaleFactor);
        int scaledHeight = Math.round(mImageHeight * scaleFactor);

        setMeasuredDimension(Math.min(imageWidth, scaledWidth), Math.min(imageHeight, scaledHeight));
    }

    public void setImageUri(Bitmap bitmap, ArrayList<Landmark> p) throws NullPointerException{

        float aspectRatio = (float) bitmap.getHeight() / (float) bitmap.getWidth();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        mImageWidth = displayMetrics.widthPixels;
        mImageHeight = Math.round(mImageWidth * aspectRatio);

        //TODO, MOVE TO BACKGROUND THREAD
        mBitmap = Bitmap.createBitmap(bitmap);
        imageScale = mBitmap.getWidth() / mImageWidth;

        translateY = prevTranslateY = (displayMetrics.heightPixels / 2) - (mImageHeight /2) + 60;

        invalidate();
        requestLayout();

        points = p;
        numPoints = points.size();
        pointPaint = new Paint();
        pointPaint.setColor(getResources().getColor(R.color.colorAccent));

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP){
            int actionBarHeight = 0;
            TypedValue tv = new TypedValue();
            if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
            }
            touchMod = 65 + actionBarHeight;
        }
    }

    public void closeSensorMonitors(){
        if(accelHandler != null) {
            accelHandler.close();
        }
        if(compassHandler != null) {
            compassHandler.close();
        }
        accelHandler = null;
        compassHandler = null;
    }
}
