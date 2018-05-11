package ca.uoguelph.pspenler.maptracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class BitmapLoader extends Thread{

    private String imageUri;
    private Context context;
    private Handler mHandler;
    private String errorMsg = "";

    BitmapLoader(String imageUri, Context context, Handler mHandler){
        super();
        this.imageUri = imageUri;
        this.context = context;
        this.mHandler = mHandler;
    }

    @Override
    public void run() {
        Bitmap bitmap = null;
        String type = imageUri.substring(0, Math.min(imageUri.length(), 4));
        try{
            if(type.equals("file")){
                Uri uri = Uri.parse(imageUri);
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            }else if (type.equals("http")){
                URL url = new URL(imageUri);
                bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            }else{

            }
        } catch(MalformedURLException e){
            errorMsg = "Incorrect map image URL";
        } catch(IOException e){
            errorMsg = "Map image does not exist";
        }

        Message msg = mHandler.obtainMessage();
        Bundle b = new Bundle(1);

        if(errorMsg.equals("")) {
            b.putParcelable("bitmap", bitmap);
            msg.what = 1;
        }else{
            b.putString("errorMsg", errorMsg);
            msg.what = 2;
        }

        msg.setData(b);
        mHandler.dispatchMessage(msg);
    }
}
