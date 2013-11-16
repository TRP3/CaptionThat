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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;

import hacktx.captionthat.util.SoundRecord;

public class Draw extends Activity {
    DrawingView dv;
    private Paint mPaint;
    private float startX, startY, endY, endX;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dv = new DrawingView(this);
        setContentView(R.layout.activity_draw);
        ((LinearLayout)findViewById(R.id.activityDraw)).addView(dv);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Bitmap b = BitmapMemoryManagement.decodeBitmapFromFile(extras.getString("path"), this);
            Drawable d = new BitmapDrawable(getResources(), b);
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

        ArrayList<SoundRecord> SoundList = new ArrayList<SoundRecord>();

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
                SoundList.add(rec);
            }
        } else if (requestCode == PICK_FROM_RECORDER) {
            soundPath = mSoundCaptureUri.getPath();
            // Todo SoundPoool test
        }
        else { //default sound
            soundPath = null;
        }

        Button b = new Button(this);
        b.setText("Done");
        ((LinearLayout)findViewById(R.id.activityDraw)).addView(b);
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

}