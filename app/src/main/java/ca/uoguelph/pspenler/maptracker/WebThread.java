package ca.uoguelph.pspenler.maptracker;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class WebThread extends Thread {

    private Handler handler;
    private String configUri;

    WebThread(Handler handler, String configUri) {
        super();
        this.handler = handler;
        this.configUri = configUri;
    }

    @Override
    public void run() {

        String configName = "";
        String imagePath = "";
        ArrayList<Landmark> landmarks = null;
        String errorMsg = "";

        String type = configUri.substring(0, Math.min(configUri.length(), 4));
        StringBuilder text = new StringBuilder();
        BufferedReader br;

        try {
            if (type.equals("file")) {
                Uri uri = Uri.parse(configUri);
                br = new BufferedReader(new FileReader(new File(uri.getPath())));
            } else if (type.equals("http")) {
                URL url = new URL(configUri);
                br = new BufferedReader(new InputStreamReader(url.openStream()));

            } else {
                throw new Exception("Config file must be web or local address");
            }

            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
            br.close();

            String JSONData = text.toString();
            JSONObject reader = new JSONObject(JSONData);
            configName = reader.getString("Title");
            imagePath = reader.getString("ImagePath");

            JSONArray landmarksJSON = reader.getJSONArray("Landmarks");
            landmarks = new ArrayList<>();
            for (int i = 0; i < landmarksJSON.length(); i++) {
                JSONObject l = landmarksJSON.getJSONObject(i);
                String label = l.getString("Label");
                int XDisplayLoc = l.getInt("XDisplayLoc");
                int YDisplayLoc = l.getInt("YDisplayLoc");
                float XLoc = (float) l.getDouble("XLoc");
                float YLoc = (float) l.getDouble("YLoc");
                landmarks.add(new Landmark(label, XDisplayLoc, YDisplayLoc, XLoc, YLoc, i));
            }

        } catch (FileNotFoundException e) {
            Log.e("FILE NOT FOUND", e.getMessage());
            errorMsg = "Config file does not exist";
        } catch (MalformedURLException e) {
            Log.e("EXCEPTION", "MALFORMED URL");
            Log.e("URL NOT FOUND", e.getMessage());
            errorMsg = "Config file URL does not exist";
        } catch (IOException e) {
            errorMsg = "Config file IO error";
        } catch (JSONException e) {
            errorMsg = "Config file is not valid JSON";
        } catch (Exception e) {
            errorMsg = e.getMessage();
        }

        Bundle b = new Bundle();
        Message msg = handler.obtainMessage();

        if (errorMsg.equals("")) {
            b.putString("configName", configName);
            b.putString("imagePath", imagePath);
            b.putParcelableArrayList("landmarks", landmarks);
            msg.what = 1;
        } else {
            b.putString("errorMsg", errorMsg);
            msg.what = 2;
        }

        msg.setData(b);
        handler.dispatchMessage(msg);
    }
}
