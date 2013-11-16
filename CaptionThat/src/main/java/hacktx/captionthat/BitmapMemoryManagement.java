package hacktx.captionthat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class BitmapMemoryManagement {
    /**
     * Helps manage memory by resizing bitmaps that are too large. 
     * Otherwise, it acts just like the BitmapFactory.decodeFile() 
     * method. 
     * @param bitmapPath the path of the bitmap to decode. 
     * @return the resized bitmap. 
     */
    public static Bitmap decodeBitmapFromFile(String bitmapPath, Context ctx) {
        // First decode with inJustDecodeBounds=true to check dimensions 
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(bitmapPath, options);

        // Calculate inSampleSize 
        options.inSampleSize = calculateInSampleSize(options, ctx);
        Log.d("BitmapMemoryManagement", "inSampleSize: " + options.inSampleSize);

        // Decode bitmap with inSampleSize set 
        options.inJustDecodeBounds = false;
        Bitmap b = BitmapFactory.decodeFile(bitmapPath, options);
        Log.d("BitmapMemoryManagement", "New height: " + options.outHeight
                + " New width: " + options.outWidth);
        return b;
    }

    /**
     * Decode an immutable bitmap from a byte array. 
     * @param data the byte[] to decode. 
     * @return
     */
    public static Bitmap decodeByteArray(byte[] data, Context ctx) {
        // First decode with inJustDecodeBounds=true to check dimensions 
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize 
        options.inSampleSize = calculateInSampleSize(options, ctx);
        Log.d("decodeByteArray", "inSampleSize: " + options.inSampleSize);

        // Decode bitmap with inSampleSize set 
        options.inJustDecodeBounds = false;
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        Log.d("decodeByteArray", "New height: " + options.outHeight
                + " New width: " + options.outWidth);
        return b;
    }

    /**
     * A helper method that helps calculate the size 
     * of a resized bitmap. 
     * @param options
     * @return the largest ratio that guarantees a final 
     * image with both dimensions less than or equal to 
     * the requested height and width. 
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, Context ctx) {
        // Raw height and width of image 
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        Log.d("BitmapMemoryManagement", "Height: " + height + " Width: " + width);

        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int MAX_WIDTH = size.x;
        int MAX_HEIGHT = size.y;

        if (height > MAX_HEIGHT || width > MAX_WIDTH) {

            // Calculate ratios of height and width to requested height and width 
            final int heightRatio = Math.round((float) height / (float) MAX_HEIGHT);
            final int widthRatio = Math.round((float) width / (float) MAX_WIDTH);

            inSampleSize = heightRatio > widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }
}