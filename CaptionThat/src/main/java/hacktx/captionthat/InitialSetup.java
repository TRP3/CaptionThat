package hacktx.captionthat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class InitialSetup extends Activity {
    private static final String PREFERENCES = "preferences";
    public static final String IMAGE_FILENAME = "team_pic.jpg";
    private String teamName, teamPicPath, prevTeamPicPath;
    private int teamNum;
    //image stuff
    private Uri mImageCaptureUri;
    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_FILE = 2;
    private static final int DEFAULT_IMAGE = 3;
    Bitmap bitmap = null;
    AlertDialog dialog;
    private int numImagesRetrieved = 0;
    private long lastTimestamp;
    private static final int NUM_IMAGES_PER_PAGE = 10;
    private static final String SERVER_URL = "http://captionthat2.herokuapp.com";
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.browser:
                dialog.show();
                return true;
            case R.id.refresh:
                refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void refresh() {
        setContentView(R.layout.home_screen);
        lastTimestamp = System.currentTimeMillis() / 1000;
        numImagesRetrieved = 0;
        new LoadImagesTask().execute();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("InitialSetup", "onCreate");
        refresh();
        // Begin listening for user to browse for picture.
        browseForPicture();
    }
    class LoadImagesTask extends AsyncTask<String, Void, LinkedList<Pair>> {
        ProgressBar progressBar;
        protected void onPreExecute() {
            showProgressSpinner();
        }
        protected LinkedList<Pair> doInBackground(String... urls) {
            try {
                JSONArray jsonArray = getPageOfImages();
                LinkedList<Pair> imagePairs = new LinkedList<Pair>();
                for(int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    imagePairs.add(new Pair(jsonObject.getInt("id"),
                            getBitmapFromUrl(jsonObject.getString("url"))));
                }
                return imagePairs;
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
                return null;
            }
        }
        protected void onPostExecute(LinkedList<Pair> imagePairs) {
            hideProgressSpinner();
            try {
                addImages(imagePairs);
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println(e);
            }
        }
        private void showProgressSpinner() {
            progressBar = new ProgressBar(InitialSetup.this);
            ((LinearLayout)findViewById(R.id.homePage)).addView(progressBar);
        }
        private void hideProgressSpinner() {
            ((LinearLayout)findViewById(R.id.homePage)).removeView(progressBar);
        }
    }
    /**
     * Tell the OS to expect orientation change.
     */
    @Override
    public void onConfigurationChanged(Configuration  newConfig) {
        super.onConfigurationChanged(newConfig);
        refresh();
        Log.d("InitialSetup", "Configuration changed.");
    }
    private void browseForPicture() {
        final String [] items = new String [] {"New Image", "Existing Image", "Default Image"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String> (this,
                R.layout.list_item,items);
        AlertDialog.Builder builder	= new AlertDialog.Builder(
                new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle("Select Image");
        builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int item ) {
                if (item == 0) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File file = new File(Environment.getExternalStorageDirectory(),
                            "tmp_avatar_" + String.valueOf
                                    (System.currentTimeMillis()) + ".jpg");
                    mImageCaptureUri = Uri.fromFile(file);
                    try {
                        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                                mImageCaptureUri);
                        intent.putExtra("return-data", true);
                        startActivityForResult(intent, PICK_FROM_CAMERA);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    dialog.cancel();
                } else if (item == 1){
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent,
                            "Complete action using"), PICK_FROM_FILE);
                } else { //default image
                    teamPicPath = null;
                    bitmap = null;
                }
            }
        } );
        dialog = builder.create();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        if (requestCode == PICK_FROM_FILE) {
            mImageCaptureUri = data.getData();
            teamPicPath = getRealPathFromURI(mImageCaptureUri); //from Gallery
            if (teamPicPath == null)
                teamPicPath = mImageCaptureUri.getPath(); //from File Manager
            if (teamPicPath != null)
                bitmap 	= BitmapMemoryManagement
                        .decodeBitmapFromFile(teamPicPath, this);
        } else if (requestCode == PICK_FROM_CAMERA) {
            teamPicPath	= mImageCaptureUri.getPath();
            bitmap  = BitmapMemoryManagement.decodeBitmapFromFile(teamPicPath, this);
        }
        else { //default image
            teamPicPath = null;
            bitmap = null;
        }
        resizeBitmap();
        Intent i = new Intent(getApplicationContext(), Draw.class);
        i.putExtra("path", teamPicPath);
        startActivity(i);

    }

    @SuppressWarnings("deprecation")
    private String getRealPathFromURI(Uri contentUri) {
        String [] proj 	= {MediaStore.Images.Media.DATA};
        Cursor cursor 	= managedQuery( contentUri, proj, null, null,null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
    /**
     * Resizes the bitmap so that it is within the 2048 x 2048 limit.
     */
    private void resizeBitmap() {
        //avoid null pointer exceptions - can't resize nonexistent bitmap
        if(bitmap == null)
            return;
        final double MAX_DIMENSION_ALLOWED = 2048.0;
        double maxDimension = Math.max(bitmap.getHeight(), bitmap.getWidth());
        if(maxDimension > MAX_DIMENSION_ALLOWED) {
            double scale = (double) ((double) MAX_DIMENSION_ALLOWED / (double) maxDimension);
            bitmap = Bitmap.createScaledBitmap(bitmap,
                    (int) (bitmap.getWidth() * scale),
                    (int) (bitmap.getHeight() * scale), true);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_initial_setup, menu);
        return true;
    }
    private void addImages(LinkedList<Pair> imagePairs) throws JSONException {
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int dim = Math.min(size.x, size.y);
        int numPics = (int)Math.ceil(size.x*2/(float)dim);
        for(int i = 0; i < imagePairs.size(); i += numPics) {
            List<Pair> list = new ArrayList<Pair>();
            for(int j = 0; j < numPics && i + j < imagePairs.size(); j++){
                list.add(imagePairs.get(i + j));
                numImagesRetrieved++;
            }
            addLine(list, size.x);
        }
        Button loadMoreImages = new Button(this);
        loadMoreImages.setText("Load more images");
        loadMoreImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((LinearLayout) findViewById(R.id.homePage)).removeView(view);
                new LoadImagesTask().execute();
            }
        });
        ((LinearLayout)findViewById(R.id.homePage)).addView(loadMoreImages);
    }
    private void addLine(List<Pair> imageList, int screenWidth){
        LinearLayout sideBySideImages = new LinearLayout(this);
        sideBySideImages.setOrientation(LinearLayout.HORIZONTAL);
        final int widthScale = screenWidth / imageList.size();
        int count = 0;
        for(Pair imagePair : imageList){
            Bitmap image = imagePair.image;
            if(image == null) continue;
            int posX = (count++%NUM_IMAGES_PER_PAGE * widthScale);
            double ratio = (double)Math.max(image.getHeight(), image.getWidth())/(double)widthScale;
            double scaledWidth = image.getWidth()/ratio;
            double scaledHeight = image.getHeight()/ratio;
            ImageView pic = new ImageView(this);
            pic.setAdjustViewBounds(true);
            pic.setMaxHeight((int) scaledHeight);
            pic.setMaxWidth((int) scaledWidth);
            pic.setImageBitmap(image);
            pic.setPadding(5, 5, 5, 5);
            pic.setOnClickListener(new MyLovelyOnClickListener(imagePair.id));
            sideBySideImages.addView(pic);
        }
        ((LinearLayout)findViewById(R.id.homePage)).addView(sideBySideImages);
    }

    public class MyLovelyOnClickListener implements View.OnClickListener
    {
        int id;
        public MyLovelyOnClickListener(int id) {
            this.id = id;
        }
        @Override
        public void onClick(View v) {
            new GetSoundsForImageTask().execute(id);
        }
    };

    public static Bitmap getBitmapFromUrl(String src) {
        try {
            URL url = new URL(SERVER_URL + src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
            return null;
        }
    }
    public JSONArray getPageOfImages() {
        String url = SERVER_URL + "/images.json?offset=" + numImagesRetrieved
                + "&count=" +  NUM_IMAGES_PER_PAGE + "&start_time=" + lastTimestamp;
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (String line = null; (line = reader.readLine()) != null;) {
                builder.append(line).append("\n");
            }
            JSONTokener tokener = new JSONTokener(builder.toString());
            JSONArray finalResult = new JSONArray(tokener);
            return finalResult;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
        return null;
    }
    public JSONObject getSoundsForImage(int id) {
        lastTimestamp = System.currentTimeMillis();
        String url = SERVER_URL + "/images/" + id + ".json";
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (String line = null; (line = reader.readLine()) != null;) {
                builder.append(line).append("\n");
            }
            JSONTokener tokener = new JSONTokener(builder.toString());
            JSONObject finalResult = new JSONObject(tokener);
            System.out.println(finalResult);
            return finalResult;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
        return null;
    }
    class GetSoundsForImageTask extends AsyncTask<Integer, Void, JSONObject> {
        protected JSONObject doInBackground(Integer... id) {
            return getSoundsForImage(id[0]);
        }
        protected void onPostExecute(JSONObject sounds) {
            double[] minX =  new double[0];
            double[] minY =  new double[0];
            double[] maxX =  new double[0];
            double[] maxY =  new double[0];
            String[] path = new String[0];
            String image = "";
            byte[] picArray = new byte[10];
            Random rand = new Random();
            try {
                JSONArray arr = sounds.getJSONArray("sounds_attributes");
                int size = arr.length();
                minX = new double[size];
                minY= new double[size];
                maxX = new double[size];
                maxY = new double[size];
                path = new String[size];
                for (int j = size-1; j>=0; --j) {
                    JSONObject ob = arr.getJSONObject(j);
                    minX[j] = ob.getDouble("tx");
                    minY[j] = ob.getDouble("ty");
                    maxX[j] = ob.getDouble("bx");
                    maxY[j] = ob.getDouble("by");
                    String audio = ob.getString("sound");
                    String str = "";
                    for (int k = 0; k < 50; k++)
                        str += (char)rand.nextInt()%26 + 'a';
                    RandomAccessFile f = null;
                    File fi = null;
                    try {
                        fi = new File(str+".wav");
                        f = new RandomAccessFile(fi, "rw");
                        picArray = audio.toString().getBytes();
                        f.write(picArray);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    path[j] = ob.getString(getFilesDir().getAbsolutePath()+str);
                    image = sounds.getString("image");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(image != null)
                picArray = image.toString().getBytes();

            String str = "";
            for (int k = 0; k < 50; k++)
                str += (char)rand.nextInt()%26 + 'a';
            RandomAccessFile f = null;
            try {
                f = new RandomAccessFile(new File(str+".jpg"), "rw");
                picArray = new byte[(int)f.length()];
                f.write(picArray);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Intent i = new Intent(getApplicationContext(), FullscreenActivity.class);
            i.putExtra("image", getFilesDir().getAbsolutePath()+str);
            i.putExtra("minX", minX);
            i.putExtra("minY", minY);
            i.putExtra("maxX", maxX);
            i.putExtra("maxY", maxY);
            i.putExtra("path", path);
            i.putExtra("bool", true);
            startActivity(i);
        }
    }
}
class Pair{
    int id;
    Bitmap image;
    protected Pair(int id, Bitmap image){
        this.id = id;
        this.image = image;
    }
}