package ca.uoguelph.pspenler.maptracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class MapImageView extends android.support.v7.widget.AppCompatImageView {

    private Bitmap mBitmap;
    private int mImageWidth;
    private int mImageHeight;

    private final static float minZoom = 0.5f;
    private final static float maxZoom = 6.f;
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

    private static final int MAX_CLICK_DURATION = 100; //Change tap length
    private static final int MAX_TAP_DISTANCE = 300; //Change distance sensitivity
    private long startClickTime;

    private float startX = 0;
    private float startY = 0;
    private float translateX = 0;
    private float translateY = 0;
    private float prevTranslateX = 0;
    private float prevTranslateY = 0;
    private int lastclicked = -1;

    private boolean paused = false;

    Matrix scaleMatrix = new Matrix();
    ArrayList<Landmark> points;
    int numPoints = 0;
    Paint pointPaint;
    Paint pointPaintHigh;
    int touchMod = 0;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Float detectedScaleFactor = detector.getScaleFactor();
            if (oScaleFactor == 1) {
                scaleFactor = (detectedScaleFactor - 1) + oScaleFactor;
            } else {
                scaleFactor = 3 * (detectedScaleFactor - 1) + oScaleFactor;
            }
            scaleFactor = Math.max(minZoom, Math.min(maxZoom, scaleFactor));

            translateX = prevTranslateX + ((scaleFactor - oScaleFactor) * -1 * ((scaleGestureDetector.getFocusX() - prevTranslateX) / oScaleFactor));
            translateY = prevTranslateY + ((scaleFactor - oScaleFactor) * -1 * ((scaleGestureDetector.getFocusY() - prevTranslateY) / oScaleFactor));

            return super.onScale(detector);
        }
    }

    public MapImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
        if (points == null) {
            points = new ArrayList<>(0);
        }
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startClickTime = Calendar.getInstance().getTimeInMillis();
                if (eventState != ZOOM) {
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
                if (clickDuration < MAX_CLICK_DURATION) {
                    if (paused) {
                        displayToast("Paused, unpause to add new entries");
                        break;
                    }
                    int pointX;
                    int pointY;
                    float dist;
                    float canvasScale = scaleFactor / imageScale;
                    int closeID = -1;
                    float closeDist = 999999999;

                    for (int i = 0; i < points.size(); i++) {
                        pointX = (int) prevTranslateX + (int) (canvasScale * points.get(i).getXDisplayLoc());
                        pointY = (int) prevTranslateY + (int) (canvasScale * points.get(i).getYDisplayLoc());
                        dist = (float) Math.sqrt(Math.pow((pointX - event.getX()), 2) + Math.pow((pointY - event.getY() - touchMod), 2));
                        if (dist < closeDist) {
                            closeDist = dist;
                            closeID = points.get(i).getId();
                        }
                    }

                    if (closeID != -1 && closeDist < MAX_TAP_DISTANCE) {
                        displayToast("Added point " + points.get(closeID).getLabel());
                        lastclicked = closeID;

                        // Play sound
                        final MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.button);
                        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                mp.reset();
                                mp.release();

                            }
                        });
                        mp.start();
                        // Redraw
                        this.postInvalidate();

                        DatabasePool.getDb().insertLandmarkData(points.get(closeID).getXLoc(), points.get(closeID).getYLoc());
                        // These are the initializers
                        if (accelHandler == null) {
                            accelHandler = new AccelerometerHandler(getContext());
                        }
                        if (compassHandler == null) {
                            compassHandler = new CompassHandler(getContext());
                        }
                    }

                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (eventState != ZOOM) {
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

        if ((eventState == PAN) || (eventState == ZOOM)) {
            invalidate();
            requestLayout();
        }
        return true;
    }

    Toast lasttoast = null;
    void displayToast(String s) {

        if (lasttoast != null) {
            lasttoast.cancel();
        }
        lasttoast = Toast.makeText(getContext(), s, Toast.LENGTH_SHORT);
        lasttoast.show();

    }

    public boolean setPaused(boolean paused) {
        if (compassHandler != null) {
            compassHandler.setPaused(paused);
        } else {
            return false;
        }
        if (accelHandler != null) {
            accelHandler.setPaused(paused);
        } else {
            return false;
        }
        this.paused = paused;
        DatabasePool.getDb().insertLandmarkPause(paused);
        return paused;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    Paint textPaint = new Paint();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        scaleMatrix.setTranslate(imageScale * translateX / scaleFactor, imageScale * translateY / scaleFactor);
        scaleMatrix.postScale(scaleFactor / imageScale, scaleFactor / imageScale);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(mBitmap, 0, 0, null);

        textPaint.setColor(getResources().getColor(R.color.text_light));
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < numPoints; i++) {
            if (i == lastclicked) {
                canvas.drawCircle(points.get(i).getXDisplayLoc(), points.get(i).getYDisplayLoc(), 20, pointPaintHigh);
            } else {
                canvas.drawCircle(points.get(i).getXDisplayLoc(), points.get(i).getYDisplayLoc(), 20, pointPaint);
            }
            canvas.drawText(points.get(i).getLabel(), points.get(i).getXDisplayLoc(), points.get(i).getYDisplayLoc(), textPaint);
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

    public void setImageUri(String mapPath, ArrayList<Landmark> p) throws NullPointerException {

        try {
            Uri uri = Uri.parse(mapPath);
            mBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        float aspectRatio = (float) mBitmap.getHeight() / (float) mBitmap.getWidth();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        mImageWidth = displayMetrics.widthPixels;
        mImageHeight = Math.round(mImageWidth * aspectRatio);
        imageScale = mBitmap.getWidth() / mImageWidth;
        translateY = prevTranslateY = (displayMetrics.heightPixels / 2) - (mImageHeight / 2) + 60;

        invalidate();
        requestLayout();

        points = p;
        numPoints = points.size();
        pointPaint = new Paint();
        pointPaintHigh = new Paint();
        pointPaint.setColor(getResources().getColor(R.color.colorAccent));
        pointPaintHigh.setColor(getResources().getColor(R.color.colorPrimary));

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            int actionBarHeight = 0;
            TypedValue tv = new TypedValue();
            if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            }
            touchMod = 65 + actionBarHeight;
        }
    }

    public void closeSensorMonitors() {
        if (accelHandler != null) {
            accelHandler.close();
        }
        if (compassHandler != null) {
            compassHandler.close();
        }
    }

    public void openSensorMonitors() {
        if (accelHandler != null) {
            accelHandler.open();
        }
        if (compassHandler != null) {
            compassHandler.open();
        }
    }
}
