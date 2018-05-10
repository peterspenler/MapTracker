package ca.uoguelph.pspenler.maptracker;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Configuration implements Parcelable {
    private String experimentName;
    private String configFile;
    private String resultsServer;
    private String beaconLabel;
    private String imagePath;
    private ArrayList<Landmark> landmarks;
    private int beaconHeight;
    private int validConfig;

    Configuration(){
        experimentName = "";
        configFile = "";
        resultsServer = "";
        beaconLabel = "";
        imagePath = "";
        landmarks = null;
        beaconHeight = 0;
        validConfig = 0;
    }

    public void initConfig(String name, String confFile, String server, String label, String height) throws Exception{
        //Checks that experiment name is not empty
        if(name.equals("")) {
            throw new Exception("Experiment name cannot be empty");
        }else {
            experimentName = name;
        }

        //Checks that config file exists

        try{
            loadConfig(confFile);
            configFile = confFile;
        }catch(Exception e){
            throw new Exception(e.getMessage());
        }

        resultsServer = server;

        //Checks that beacon label is not empty
        if(label.equals("")) {
            throw new Exception("Beacon label cannot be empty");
        }else {
            beaconLabel = label;
        }

        //Checks that beacon height is an integer
        try{
            beaconHeight = Integer.parseInt(height);
        }
        catch(Exception e){
            throw new Exception("Beacon height is not an integer");
        }

        validConfig = 1;
    }

    public int isValid(){
        return validConfig;
    }

    public String getName(){
        return experimentName;
    }

    public String getConfigFile(){
        return configFile;
    }

    public String getResultsServer(){
        return resultsServer;
    }

    public String getBeaconLabel(){
        return beaconLabel;
    }

    public int getBeaconHeight(){
        return beaconHeight;
    }

    public String getImagePath() {
        return imagePath;
    }

    public ArrayList<Landmark> getLandmarks() {
        return landmarks;
    }

    private void setLandmarks(ArrayList<Landmark> landmarks) {
        this.landmarks = landmarks;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    private void loadConfig(String config) throws Exception{
        Uri uri = Uri.parse(config);
        File file = new File(uri.getPath());
        StringBuilder text = new StringBuilder();

        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line = br.readLine()) != null){
                text.append(line);
                text.append("\n");
            }
            br.close();

            String JSONData = text.toString();
            JSONObject reader = new JSONObject(JSONData);
            this.experimentName = reader.getString("Title");
            this.imagePath = reader.getString("ImagePath");

            JSONArray landmarksJSON = reader.getJSONArray("Landmarks");
            ArrayList<Landmark> landmarks = new ArrayList<>();
            for(int i = 0; i < landmarksJSON.length(); i++){
                JSONObject l = landmarksJSON.getJSONObject(i);
                String label = l.getString("Label");
                int XDisplayLoc = l.getInt("XDisplayLoc");
                int YDisplayLoc = l.getInt("YDisplayLoc");
                int XLoc = l.getInt("XLoc");
                int YLoc = l.getInt("YLoc");
                landmarks.add(new Landmark(label, XDisplayLoc, YDisplayLoc, XLoc, YLoc, i));
            }

            this.setLandmarks(landmarks);

        } catch (FileNotFoundException e) {
            Log.e("FILE NOT FOUND", e.getMessage());
            throw new Exception("Config file does not exist");
        } catch (IOException e) {
            throw new Exception("Config file IO error");
        } catch (JSONException e) {
            throw new Exception("Config file is not valid JSON");
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.experimentName);
        dest.writeString(this.configFile);
        dest.writeString(this.resultsServer);
        dest.writeString(this.beaconLabel);
        dest.writeString(this.imagePath);
        dest.writeList(this.landmarks);
        dest.writeInt(this.beaconHeight);
        dest.writeInt(this.validConfig);
    }

    protected Configuration(Parcel in) {
        this.experimentName = in.readString();
        this.configFile = in.readString();
        this.resultsServer = in.readString();
        this.beaconLabel = in.readString();
        this.imagePath = in.readString();
        this.landmarks = new ArrayList<>();
        in.readList(this.landmarks, Landmark.class.getClassLoader());
        this.beaconHeight = in.readInt();
        this.validConfig = in.readInt();
    }

    public static final Creator<Configuration> CREATOR = new Creator<Configuration>() {
        @Override
        public Configuration createFromParcel(Parcel source) {
            return new Configuration(source);
        }

        @Override
        public Configuration[] newArray(int size) {
            return new Configuration[size];
        }
    };
}
