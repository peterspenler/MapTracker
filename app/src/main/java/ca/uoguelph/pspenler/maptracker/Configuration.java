package ca.uoguelph.pspenler.maptracker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Configuration implements Parcelable {
    private String experimentName;
    private String configName;
    private String configFile;
    private String resultsServer;
    private String beaconLabel;
    private String imagePath;
    private ArrayList<Landmark> landmarks;
    private float beaconHeight;
    private int validConfig;
    private int configLoaded;
    private String errorMsg = "";

    Configuration() {
        experimentName = "";
        configFile = "";
        resultsServer = "";
        beaconLabel = "";
        imagePath = "";
        landmarks = null;
        beaconHeight = 0;
        validConfig = 0;
    }

    public void initConfig(String name, String confFile, String server, String label, String height) throws Exception {
        //Checks that experiment name is not empty
        String error = "";
        if (name.equals("")) {
            error = "Experiment name cannot be empty";
        } else {
            if (name.contains("/")) {
                error = "Experiment name cannot contain '/' characters";
            }
            experimentName = name;
        }

        //Checks that config file exists
        loadConfig(confFile);
        if (configLoaded == 2) {
            error = errorMsg;
        }
        configFile = confFile;

        //Checks that the results server is valid
        if (server.equals("")) {
            error = "Results server address cannot be empty";
        } else if (!MainActivity.hasInternetAccess(server, true)) {
            error = "Inputted results server not available";
        }

        resultsServer = server;

        //Checks that beacon label is not empty
        if (label.equals("")) {
            error = "Beacon label cannot be empty";
        } else {
            beaconLabel = label;
        }

        //Checks that beacon height is an float
        try {
            beaconHeight = Float.parseFloat(height);
        } catch (Exception e) {
            error = "Beacon height is not an valid number";
        }

        if (error != "") {
            throw new Exception(error);
        }
        validConfig = 1;
    }

    private void loadConfig(String config) {
        configLoaded = 0;

        @SuppressLint("HandlerLeak")
        final Handler mHandler = new Handler() {
            public void handleMessage(Message msg) {
                Bundle b;
                if (msg.what == 1) {
                    b = msg.getData();

                    configName = b.getString("configName");
                    imagePath = b.getString("imagePath");
                    landmarks = b.getParcelableArrayList("landmarks");
                    configLoaded = 1;
                }
                if (msg.what == 2) {
                    b = msg.getData();

                    errorMsg = b.getString("errorMsg");
                    configLoaded = 2;
                }
                super.handleMessage(msg);
            }
        };

        Thread thread = new WebThread(mHandler, config);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return experimentName;
    }

    public String getConfigFile() {
        return configFile;
    }

    public String getResultsServer() {
        return resultsServer;
    }

    public String getBeaconLabel() {
        return beaconLabel;
    }

    public float getBeaconHeight() {
        return beaconHeight;
    }

    public String getImagePath() {
        return imagePath;
    }

    public ArrayList<Landmark> getLandmarks() {
        return landmarks;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.experimentName);
        dest.writeString(this.configName);
        dest.writeString(this.configFile);
        dest.writeString(this.resultsServer);
        dest.writeString(this.beaconLabel);
        dest.writeString(this.imagePath);
        dest.writeList(this.landmarks);
        dest.writeFloat(this.beaconHeight);
        dest.writeInt(this.validConfig);
    }

    protected Configuration(Parcel in) {
        this.experimentName = in.readString();
        this.configName = in.readString();
        this.configFile = in.readString();
        this.resultsServer = in.readString();
        this.beaconLabel = in.readString();
        this.imagePath = in.readString();
        this.landmarks = new ArrayList<>();
        in.readList(this.landmarks, Landmark.class.getClassLoader());
        this.beaconHeight = in.readFloat();
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
