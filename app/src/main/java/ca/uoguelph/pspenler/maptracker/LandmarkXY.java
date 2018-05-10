package ca.uoguelph.pspenler.maptracker;

import android.os.Parcel;
import android.os.Parcelable;

public class LandmarkXY implements Parcelable {
    private int x;
    private int y;
    private int id;

    LandmarkXY(int x, int y, int id){
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.x);
        dest.writeInt(this.y);
        dest.writeInt(this.id);
    }

    protected LandmarkXY(Parcel in) {
        this.x = in.readInt();
        this.y = in.readInt();
        this.id = in.readInt();
    }

    public static final Parcelable.Creator<LandmarkXY> CREATOR = new Parcelable.Creator<LandmarkXY>() {
        @Override
        public LandmarkXY createFromParcel(Parcel source) {
            return new LandmarkXY(source);
        }

        @Override
        public LandmarkXY[] newArray(int size) {
            return new LandmarkXY[size];
        }
    };
}
