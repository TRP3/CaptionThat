package hacktx.captionthat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;

import hacktx.captionthat.util.SoundRecord;
import hacktx.captionthat.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    Bitmap bitmap;
    SoundRecord[] sounds;
    SoundPool pool;
    int[] ids;
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        Bundle extras = getIntent().getExtras();
        if(!extras.getBoolean("bool")){
            String bitPath = extras.getString("image");
        bitmap = BitmapMemoryManagement.decodeBitmapFromFile(bitPath, this);
        }  else
            bitmap = (Bitmap)extras.get("image");
        double[] minX = extras.getDoubleArray("minX");
        double[] minY = extras.getDoubleArray("minY");
        double[] maxX = extras.getDoubleArray("maxX");
        double[] maxY = extras.getDoubleArray("maxY");
        String[] path = extras.getStringArray("path");
        sounds = new SoundRecord[path.length];
        for(int i = 0; i < path.length; i++){
            sounds[i] = new SoundRecord(minX[i], minY[i], maxX[i], maxY[i], path[i]);
        }
        findViewById(R.id.imageView).setBackground(new BitmapDrawable(getResources(), bitmap));
        ids = new int[sounds.length];
        pool = new SoundPool(sounds.length, AudioManager.STREAM_MUSIC, 0);
        for(int i = 0; i < sounds.length; i++){
            if(pool == null)
                break;
            ids[i] = pool.load(sounds[i].path, 0);
        }



        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
    }



    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            float x = event.getX() / width;
            float y = event.getY() / height;
            for(int i = 0; i < sounds.length; i++) {
                SoundRecord sound = sounds[i];
                if (x >= sound.minX && x <= sound.maxX && y >= sound.minY && y <= sound.maxY){
                    pool.play(ids[i], 1, 1, 0, 0, 1);
                }
            }
        }
        return true;
    }

}
