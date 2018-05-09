package ca.uoguelph.pspenler.maptracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.io.IOException;

public class MapImageView extends android.support.v7.widget.AppCompatImageView{

    private Bitmap mBitmap;
    private int mImageWidth;
    private int mImageHeight;

    private final static float minZoom = 1.f;
    private final static float maxZoom = 3.f;
    private float scaleFactor = 1;
    private float oScaleFactor = 1;
    private ScaleGestureDetector scaleGestureDetector;

    private final static int NONE = 0;
    private final static int PAN = 1;
    private final static int ZOOM = 2;
    private int eventState;

    private float startX = 0;
    private float startY = 0;
    private float translateX = 0;
    private float translateY = 0;
    private float prevTranslateX = 0;
    private float prevTranslateY = 0;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor = ((detector.getScaleFactor() - 1) / 10) + oScaleFactor;
            scaleFactor = Math.max(minZoom, Math.min(maxZoom, scaleFactor));
            //invalidate();
            //requestLayout();
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
                eventState = PAN;
                startX = event.getX() - prevTranslateX;
                startY = event.getY() - prevTranslateY;
                break;
            case MotionEvent.ACTION_UP:
                prevTranslateX = translateX;
                prevTranslateY = translateY;
                eventState = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                translateX = event.getX() - startX;
                translateY = event.getY() - startY;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                eventState = ZOOM;
                break;
        }

        scaleGestureDetector.onTouchEvent(event);

        if((eventState == PAN) || (eventState == ZOOM)){
            invalidate();
            requestLayout();
        }

        oScaleFactor = scaleFactor;
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int height = displayMetrics.heightPixels;

        canvas.save();
        canvas.scale(scaleFactor, scaleFactor);
        canvas.translate(translateX/scaleFactor, translateY/scaleFactor);
        canvas.drawBitmap(mBitmap, 0, 0, null);
        canvas.restore();
        Log.d("CANVAS LOG", "canvas:" + Integer.toString(canvas.getHeight()) + " screen:" + getResources().getDisplayMetrics().heightPixels);
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

    public void setImageUri(Uri uri){
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
            float aspectRatio = (float) bitmap.getHeight() / (float) bitmap.getWidth();
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            mImageWidth = displayMetrics.widthPixels;
            mImageHeight = Math.round(mImageWidth * aspectRatio);
            //TODO, MOVE TO BACKGROUND THREAD
            mBitmap = Bitmap.createScaledBitmap(bitmap, mImageWidth, mImageHeight, false);
            invalidate();
            requestLayout();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
