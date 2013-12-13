package hacktx.captionthat.util;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Andy on 11/16/13.
 */
public class SoundRecord implements Parcelable{
    public double minX, minY, maxX, maxY;
    public String path;

    public SoundRecord(double minX, double minY, double maxX, double maxY, String path){ //TODO
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.path = path;
    }

    @Override
    public int describeContents() {
        return (int)(minX + minY + maxX + maxY);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Object[] arr = new Object[5];
        arr[0] = minX;
        arr[1] = minY;
        arr[2] = maxX;
        arr[3] = maxY;
        arr[4] = path;
        dest.writeArray(arr);
    }

    public static final Parcelable.Creator<SoundRecord> CREATOR
            = new Parcelable.Creator<SoundRecord>() {
        public SoundRecord createFromParcel(Parcel in) {
            return new SoundRecord(in);
        }

        public SoundRecord[] newArray(int size) {
            return new SoundRecord[size];
        }


    };
    private SoundRecord(Parcel in) {
        Object[] arr = in.readArray(new ClassLoader() {
            @Override
            protected Class<?> findClass(String className) throws ClassNotFoundException {
                return super.findClass(className);
            }
        });
        minX = (Integer)arr[0];
        minY = (Integer)arr[1];
        maxX = (Integer)arr[2];
        maxY = (Integer)arr[3];
        path = (String)arr[4];
    }
}
