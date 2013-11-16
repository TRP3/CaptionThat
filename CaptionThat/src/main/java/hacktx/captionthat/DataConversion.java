package hacktx.captionthat;

import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Helper class to convert between data formats such as base64, .jpeg, and .mp3.
 */
public class DataConversion {
    public static JSONObject constructPictureJson(Bitmap bitmap) throws JSONException, IOException {
        JSONObject pictureData = new JSONObject();
        pictureData.put("image", Base64.encodeToString(bitmapToByteArr(bitmap), Base64.DEFAULT));
        return pictureData;
    }

    public static byte[] bitmapToByteArr(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static String constructAudioString(String pathname) throws JSONException, IOException {
        File f = new File(pathname);
        return Base64.encodeToString(fileToByteArr(f), Base64.DEFAULT);
    }

    /**
     * Converts a file to a byte array
     * @param file
     * @return
     */
    public static byte[] fileToByteArr(File file) {
        if(file == null)
            return null;

        byte[] picArray = null;
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(file, "r");
            picArray = new byte[(int)f.length()];
            f.read(picArray);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(f != null)
                try {
                    f.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return picArray;
    }
}
