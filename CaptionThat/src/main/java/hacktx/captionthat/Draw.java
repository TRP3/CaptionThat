package hacktx.captionthat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hacktx.captionthat.util.SoundRecord;

public class Draw extends Activity {
    DrawingView dv;
    private Paint mPaint;
    private float startX, startY, endY, endX;
    List<SoundRecord> records = new ArrayList<SoundRecord>();
    Bitmap bitmap;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dv = new DrawingView(this);
        setContentView(R.layout.activity_draw);
        ((LinearLayout)findViewById(R.id.activityDraw)).addView(dv);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            bitmap = BitmapMemoryManagement.decodeBitmapFromFile(extras.getString("path"), this);
            Drawable d = new BitmapDrawable(getResources(), bitmap);
            dv.setBackground(d);
        }
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
    }

    public class DrawingView extends View {
        private Bitmap mBitmap;
        private Path mPath;
        private Paint   mBitmapPaint;
        Context context;
        private Paint circlePaint;
        private Path circlePath;

        public DrawingView(Context c) {
            super(c);
            context=c;
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
            circlePaint = new Paint();
            circlePath = new Path();
            circlePaint.setAntiAlias(true);
            circlePaint.setColor(Color.BLUE);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeJoin(Paint.Join.MITER);
            circlePaint.setStrokeWidth(4f);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w > 0 && h > 0) {
                mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawBitmap( mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath( mPath,  mPaint);
            canvas.drawPath( circlePath,  circlePaint);
            canvas.drawRect(Math.min(startX, endX), Math.min(startY, endY), Math.max(startX, endX),
                            Math.max(startY, endY), mPaint);
        }

        private static final float TOUCH_TOLERANCE = 4;

        private void touch_start(float x, float y) {
            startX = endX = x;
            startY = endY = y;
        }
        private void touch_move(float x, float y) {
            float dx = Math.abs(x - startX);
            float dy = Math.abs(y - startY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                endX = x;
                endY = y;
            }
        }
        private void touch_up() {
            browseForSound();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
            return true;
        }
    }

    private String soundPath;

    //sound stuff
    private Uri mSoundCaptureUri;
    private static final int PICK_FROM_RECORDER = 1;
    private static final int PICK_FROM_FILE = 2;

    private void browseForSound() {
        final String [] items = new String [] {"New Recording", "Existing Recording"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String> (this,
                R.layout.list_item, items);
        AlertDialog.Builder builder	= new AlertDialog.Builder(
                new ContextThemeWrapper(this, R.style.AlertDialogCustom));

        builder.setTitle("Select Sound");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                    File file = new File(Environment.getExternalStorageDirectory(),
                            "tmp_sound_" + String.valueOf
                                    (System.currentTimeMillis()) + ".wav");
                    mSoundCaptureUri = Uri.fromFile(file);

                    try {
                        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                                mSoundCaptureUri);
                        intent.putExtra("return-data", true);

                        startActivityForResult(intent, PICK_FROM_RECORDER);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    dialog.cancel();
                } else if (item == 1) {
                    Intent intent = new Intent();

                    intent.setType("audio/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);

                    startActivityForResult(Intent.createChooser(intent,
                            "Complete action using"), PICK_FROM_FILE);
                }
            }
        } );
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;


        if (requestCode == PICK_FROM_FILE) {
            mSoundCaptureUri = data.getData();
            soundPath = getRealPathFromURI(mSoundCaptureUri); //from external apps

            if (soundPath == null)
                soundPath = mSoundCaptureUri.getPath(); //from File Manager


            if (soundPath != null) {
                double minX = Math.min(startX, endX);
                double minY = Math.min(startY, endY);
                double maxX = Math.max(startX, endX);
                double maxY = Math.max(startY, endY);
                SoundRecord rec = new SoundRecord(minX, minY, maxX, maxY, soundPath);
                records.add(rec);
            }
        } else if (requestCode == PICK_FROM_RECORDER) {
            soundPath = mSoundCaptureUri.getPath();
            // Todo SoundPoool test
        }
        else { //default sound
            soundPath = null;
        }

    }

    @SuppressWarnings("deprecation")
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Audio.Media.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.draw_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.done:
                System.out.println("Pressed done");
                new RetreiveFeedTask(bitmap, records).execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}

class RetreiveFeedTask extends AsyncTask<String, Void, Integer> {

    private Exception exception;
    Bitmap bitmap;
    List<SoundRecord> recs;

    protected Integer doInBackground(String... urls) {
        try {
            uploadNewImageBitmap();
        } catch (Exception e) {
            this.exception = e;
        }finally {
            return 1;
        }
    }

    protected void onPostExecute(Integer feed) {
        System.out.println("Got to onPostExecute");
        // TODO: check this.exception
        // TODO: do something with the feed
    }

    public RetreiveFeedTask (Bitmap b, List<SoundRecord> r){
        bitmap = b;
        recs = r;
    }

    public Boolean uploadNewImageBitmap() throws JSONException, IOException {
        Boolean success = true;
        JSONObject obj = DataConversion.constructPictureJson(bitmap);
        int i = 0;
        for(SoundRecord rec : recs){
            JSONObject obje = new JSONObject();
            obje.put("sound", DataConversion.constructAudioString(rec.path));
            obje.put("tx", rec.minX);
            obje.put("ty", rec.minY);
            obje.put("bx", rec.maxX);
            obje.put("by", rec.maxY);
            obj.accumulate("sounds", obje);
            System.out.println("JSON Record object: " + i + " :  " + obje.toString());
            int len = obj.toString().length();
            for(int im = 0; im < len; im += 100)
                System.out.println("JSON Complete object: " + i+im + " :  " + obj.toString().substring(im, Math.min(im+100, len)));
        }
        DefaultHttpClient httpClient = new DefaultHttpClient();
        System.out.println("Step 1 WHOO HOO!!! BANANA");
        System.out.println("obj: " + obj.toString());
        System.out.println("Step 2 WHOO HOO!!! BANANA");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        System.out.println("Step 3 WHOO HOO!!! BANANA");
        final String NEW_IMAGE_URL = "http://captionthat.herokuapp.com/images.json"; // TODO: Make this a public variable.
        System.out.println("Step 4 WHOO HOO!!! BANANA");
        HttpPost postMethod = new HttpPost(NEW_IMAGE_URL);
        System.out.println("Step 5 WHOO HOO!!! BANANA");
        postMethod.setEntity(new StringEntity(obj.toString()));
        System.out.println("Step 6 WHOO HOO!!! BANANA");
        postMethod.setHeader("Accept", "application/json");
        System.out.println("Step 7 WHOO HOO!!! BANANA");
        postMethod.setHeader("Content-type", "application/json");
        System.out.println("Step 8 WHOO HOO!!! BANANA");
        postMethod.setHeader("Data-type", "json");
        System.out.println("Step 9 WHOO HOO!!! BANANA");
        try{
            System.out.println("Step 10 WHOO HOO!!! BANANA");
            httpClient.execute(postMethod, responseHandler);
            System.out.println("Step 11 WHOO HOO!!! BANANA");
        } catch (org.apache.http.client.HttpResponseException error){
            Log.d("Uploader Class Error", "Error code: " + error.getStatusCode());
            Log.d("Uploader Class Error", "Error message: " + error.getMessage());
            success = false;
        }
        //Log.d("server resposne", response);
        System.out.println("Success:  " + success);
        return success;
    }

}
