package ca.uoguelph.pspenler.maptracker;

import android.os.Parcel;
import android.os.Parcelable;

public class Landmark implements Parcelable {
    private String label;
    private int XDisplayLoc;
    private int YDisplayLoc;
    private int XLoc;
    private int YLoc;
    private int id;

    Landmark(String label, int XDisplayLoc, int YDisplayLoc, int XLoc, int YLoc, int id){
        this.label = label;
        this.XDisplayLoc = XDisplayLoc;
        this.YDisplayLoc = YDisplayLoc;
        this.XLoc = XLoc;
        this.YLoc = YLoc;
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public int getXDisplayLoc(){
        return XDisplayLoc;
    }

    public int getYDisplayLoc() {
        return YDisplayLoc;
    }

    public int getXLoc() {
        return XLoc;
    }

    public int getYLoc() {
        return YLoc;
    }

    public int getId() { return id; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.label);
        dest.writeInt(this.XDisplayLoc);
        dest.writeInt(this.YDisplayLoc);
        dest.writeInt(this.XLoc);
        dest.writeInt(this.YLoc);
        dest.writeInt(this.id);
    }

    protected Landmark(Parcel in) {
        this.label = in.readString();
        this.XDisplayLoc = in.readInt();
        this.YDisplayLoc = in.readInt();
        this.XLoc = in.readInt();
        this.YLoc = in.readInt();
        this.id = in.readInt();
    }

    public static final Parcelable.Creator<Landmark> CREATOR = new Parcelable.Creator<Landmark>() {
        @Override
        public Landmark createFromParcel(Parcel source) {
            return new Landmark(source);
        }

        @Override
        public Landmark[] newArray(int size) {
            return new Landmark[size];
        }
    };
}
