package hacktx.captionthat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import hacktx.captionthat.util.SoundRecord;

public class InitialSetup extends Activity {
	private static final String PREFERENCES = "preferences";
	public static final String IMAGE_FILENAME = "team_pic.jpg";
	
	private String teamName, teamPicPath, prevTeamPicPath;
	private int teamNum;
	
	//image stuff
	private Uri mImageCaptureUri;
	private ImageView mImageView;
	private static final int PICK_FROM_CAMERA = 1;
	private static final int PICK_FROM_FILE = 2;
	private static final int DEFAULT_IMAGE = 3;
	Bitmap bitmap = null;
    AlertDialog dialog;

    private int numImagesRetrieved = 0;
    private static final int NUM_IMAGES_PER_PAGE = 10;
    private static final String SERVER_URL = "http://captionthat.herokuapp.com";

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.browser:
                dialog.show();
                return true;
            case R.id.refresh:
                setContentView(R.layout.choose_picture);
                new LoadImagesTask().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_picture);

		Log.d("InitialSetup", "onCreate");
		
        new LoadImagesTask().execute();
		// Begin listening for user to browse for picture.
		browseForPicture();
	}

    class LoadImagesTask extends AsyncTask<String, Void, LinkedList<Pair>> {
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
            try {
                addImages(imagePairs);
            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println(e);
            }
        }
    }
	
	/**
	 * Tell the OS to expect orientation change.
	 */
	@Override
    public void onConfigurationChanged(Configuration  newConfig) {
      super.onConfigurationChanged(newConfig);
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
					mImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
				}
			}
		} );
		
		dialog = builder.create();

//		((Button) findViewById(R.id.browse_button)).setOnClickListener(new
//				View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//                findViewById(R.id.browse_button).setVisibility(View.GONE);
//				dialog.show();
//			}
//		});
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
        try {
            uploadNewImageBitmap(bitmap);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mImageView.setImageBitmap(bitmap);
	}

    // TODO: Move this to the class that adds sound to the image, and include the sound data.
    public Boolean uploadNewImageBitmap(Bitmap bitmap) throws JSONException, IOException {
        Boolean success = true;
        JSONObject jsonObject = DataConversion.constructPictureJson(bitmap);
        DefaultHttpClient httpClient = new DefaultHttpClient();

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        final String NEW_IMAGE_URL = SERVER_URL + "/images/new";
        HttpPost postMethod = new HttpPost(NEW_IMAGE_URL);
        postMethod.setEntity(new StringEntity(jsonObject.toString()));
        postMethod.setHeader("Accept", "application/json");
        postMethod.setHeader("Content-type", "application/json");
        postMethod.setHeader("Data-type", "json");
        try{
            httpClient.execute(postMethod, responseHandler);
        } catch (org.apache.http.client.HttpResponseException error){
            Log.d("Uploader Class Error", "Error code: "+error.getStatusCode());
            Log.d("Uploader Class Error", "Error message: " + error.getMessage());
            success = false;
        }
        //Log.d("server response", response);
        return success;
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
            }
            addLine(list, size.x);
        }
    }

    private void addLine(List<Pair> imageList, int width){
        LinearLayout sideBySideImages = new LinearLayout(this);
        sideBySideImages.setOrientation(LinearLayout.HORIZONTAL);
        final int widthScale = width / imageList.size();
        int count = 0;

        for(Pair imagePair : imageList){
            Bitmap image = imagePair.image;
            if(image == null) continue;
            int posX = (count++%NUM_IMAGES_PER_PAGE * widthScale);
            double ratio = (double)Math.max(image.getHeight(), image.getWidth())/(double)widthScale;
            double scaledWidth = image.getWidth()/ratio;
            double scaledHeight = image.getHeight()/ratio;
            ImageView pic = new ImageView(this);
            pic.setImageBitmap(image);
            pic.setMaxHeight((int) scaledHeight);
            pic.setMaxHeight((int)scaledWidth);
            pic.setPadding(5, 5, 5, 5);
            pic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO open up picture
                }
            });
            sideBySideImages.addView(pic);
        }
        ((LinearLayout)findViewById(R.id.homePage)).addView(sideBySideImages);
    }

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
        String testUrl = SERVER_URL + "/images.json?offset=0&count=10&start_time=2000000000";
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(testUrl);
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
}


class Pair{
    int id;
    Bitmap image;

    protected Pair(int id, Bitmap image){
        this.id = id;
        this.image = image;
    }
}