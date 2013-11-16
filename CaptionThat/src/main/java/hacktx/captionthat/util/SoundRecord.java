package hacktx.captionthat.util;

/**
 * Created by Andy on 11/16/13.
 */
public class SoundRecord {
    public double minX, minY, maxX, maxY;
    public String path;

    public SoundRecord(double minX, double minY, double maxX, double maxY, String path){ //TODO
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.path = path;
    }

}
