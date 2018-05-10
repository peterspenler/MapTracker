package ca.uoguelph.pspenler.maptracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;

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

    Matrix scaleMatrix = new Matrix();
    ArrayList<LandmarkXY> points;
    int numPoints = 0;
    Paint pointPaint;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Float detectedScaleFactor = detector.getScaleFactor();
            //Log.d("SCALE FACTOR", "Current:" + Float.toString(scaleFactor) + " Detected:" + Float.toString(detectedScaleFactor - 1));
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
                //Log.e("PREV TRANSLATES", "X:" + Float.toString(prevTranslateX) + " Y:" + Float.toString(prevTranslateY));
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
            canvas.drawCircle(points.get(i).getX(), points.get(i).getY(), 20, pointPaint);
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

    public void setImageUri(Uri uri, ArrayList<LandmarkXY> p){
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        points = p;
        numPoints = points.size();
        pointPaint = new Paint();
        pointPaint.setColor(getResources().getColor(R.color.point_color));
    }

    public float getCanvasX(){
        return translateX;
    }

    public float getCanvasY(){
        return translateY;
    }

    public float getScaleFactor(){
        return scaleFactor;
    }

    public float getImageScaleFator(){
        return imageScale;
    }

    public float getMaxScale(){
        return imageScale * maxZoom;
    }

}
