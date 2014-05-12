package com.pulp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.londatiga.fsq.FoursquareApp;
import net.londatiga.fsq.FoursquareApp.FsqAuthListener;
import net.londatiga.fsq.FsqVenue;
import net.londatiga.fsq.NearbyAdapter;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.pulp.R;
import com.pulp.R.id;
import com.pulp.R.layout;
import com.facebook.*;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.model.OpenGraphAction;
import com.facebook.model.OpenGraphObject;
import com.foursquare.android.nativeoauth.FoursquareOAuth;
import com.foursquare.android.nativeoauth.model.AuthCodeResponse;


public class ShareStatusUpdateFragment extends Fragment implements /**AccessTokenRequestListener, UserInfoRequestListener,*/ LocationListener {

	public static final String debugTag = "BYI";
	private View rootView;
	EditText statusTextBox;
	String statusText;
	private static final String STATUS_BACKUP = "status backup";
	
	
	
	//FACEBOOK Variables
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
	private boolean pendingPublishReauthorization = false;
	
	
	//TwitterVariables
	// Preference Constants
	static String TWITTER_CONSUMER_KEY;
	static String TWITTER_CONSUMER_SECRET;
    static String PREFERENCE_NAME = "twitter_oauth";
    
    //UserToken Twitter
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    public static String TwitterLoggedInPref = "TwitterLoggedIn";
    
   
    public static String TWITTER_CALLBACK_URL = "oauth://com.Pulp";
    public static String URL_PARAMETER_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
 
    
    private static Twitter twitter;
    private static RequestToken requestToken;
	
    
    public static SharedPreferences mSharedPreferences;
	
	//Foursquare Variables
   //Native Foursquare variables
    //Activity Result Constants
    final int REQUEST_CODE_FSQ_CONNECT = 200;
    final int REQUEST_CODE_FSQ_TOKEN_EXCHANGE = 201;
    final int BROWSE_GALLERY = 1;
    final int TAKE_PHOTO = 2;
    
    //Lorenz FourSquare Tutorial
    private FoursquareApp myFourSquareApp;
    public static final String CLIENT_ID = "23RCTKA5RLMIAZ0TTHJVLV4UPZ5BR3Y4LC4NUVGO4UBKXCCU";
    public static final String CLIENT_SECRET = "YHXFVDSD3NR0MM2VHI42BQVCYEVGR1WWNKBYZK0K4C0Q1S04";
	public static ArrayList<FsqVenue> myNearbyList;
	private ListView myListView;
	private NearbyAdapter myNearbyAdapter;
	public static String foursquareAccessToken;
	public static String foursquareLoggedIn = "LogIn";
	
	//User Token Foursquare
	static final String PREF_KEY_FOURSQUARE_TOKEN = "foursquare_token";
	static final String PREF_KEY_FOURSQUARE_SECRET = "foursquare_secret";
	
	//Foursquare get GPS location and get nearby venues
	public String bestProvider;
	public Geocoder geocoder;
	LocationManager lm;
	LocationListener locationListener;
	List<Address> user;
	double lat; //= Double.valueOf(latitude);
	double lon; //= Double.valueOf(longitude);
	
	//Foursquare Venue
	static public String foursquareVenueClickedID;
	static public String foursquareVenueClickedName = "nothing";
	//  NOT BEING USED String checkInVenue;
	TextView venueTextView;
	TextView venueCheckInLabelView;
	Boolean isCheckingIntoFoursquare;
	
	//Switches and buttons
    Switch facebookSwitch;
    Switch twitterSwitch;
    Button foursquareButton;
    
    //Photo
    private Uri image;
    byte[] imageByteArray;
    
	
	/**This is a flag to see if we should call the getPermissions() request.  
	 * If you do it twice in the same instance it errors out.  Create flag, onCreateView set it to 0
	 * After first status update to facebook happens, set to 1.  If 1, don't call getPermissions()
	*/
	Integer getPermissionFlag;
	
	
	
	
	 /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ShareStatusUpdateFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
      
      
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.status_update_main, container, false);
        
        getPermissionFlag = 0;
        
        myNearbyList = new ArrayList<FsqVenue>();
        myNearbyAdapter = new NearbyAdapter(getActivity());
        facebookSwitch = (Switch) rootView.findViewById(R.id.share_to_facebook_switch);
        twitterSwitch = (Switch) rootView.findViewById(R.id.share_to_twitter_switch);
        foursquareButton = (Button) rootView.findViewById(R.id.share_to_foursquare_button);
        venueTextView = (TextView) rootView.findViewById(R.id.venue_chosen);
        venueCheckInLabelView = (TextView) rootView.findViewById(R.id.venue_chosen_label);
        statusTextBox = (EditText) rootView.findViewById(R.id.status_text);
       
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        image = null;
        
       
        getActivity().getActionBar().setTitle("Share My Status");
        
        
        
        
       
        
        
        //Checks to see if a backup text exists and if so, put that into the text box
        String status_backup_text;
        status_backup_text = mSharedPreferences.getString(STATUS_BACKUP, "nothing");
        
        if(status_backup_text.equals("nothing")){
        }else{
        	statusTextBox.setText(status_backup_text);
        }
        
        
        if(foursquareVenueClickedName.equals("nothing")){
        	venueCheckInLabelView.setVisibility(View.GONE);
        	isCheckingIntoFoursquare = false;
        }else{
        	venueTextView.setText(foursquareVenueClickedName);
        	venueCheckInLabelView.setVisibility(View.VISIBLE);
        	isCheckingIntoFoursquare = true;
        }
        
        
       
        
       
        /**
         * CODE BELOW IS FOR LOGGING INTO FACEBOOK
         * 
         * 
         * 
         * 
         */
        
 
  	  //start Facebook login
  		Session.openActiveSession(getActivity(), this, true, new Session.StatusCallback(){
  			
  			
  			
  			@Override
  			public void call(Session session, SessionState state, Exception exception){
  				
  				if (session.isOpened()){
  					
  					Request.newMeRequest(session, new Request.GraphUserCallback(){
  					
  						
  						@Override
  						public void onCompleted(GraphUser user, Response response){
  							
  							if (user != null) {
  								Log.d("BYI", user.getName() + " From Facebook Log In");
  								}	
  						}
  					}).executeAsync();	
  				}
  			}
  			
  			
  		});
  		

        /**
         * CODE ABOVE IS FOR LOGGING INTO FACEBOOK
         * 
         * 
         * 
         * 
         */
        
        
        
  		
  		
  		/** BELOW THIS IS THE TWITTER CODE
  		 * 
  		 * 
  		 * checking to see if a token exists, if it does then do init control, else nothing
  		 */
  	
  		
  		TWITTER_CONSUMER_KEY = getActivity().getString(R.string.consumer_key);
  		TWITTER_CONSUMER_SECRET = getActivity().getString(R.string.consumer_secret);
  		
  		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
  		Boolean twitterLoggedIn;
  		twitterLoggedIn = mSharedPreferences.getBoolean(TwitterLoggedInPref, false);
  		
  		//This if else forces login if app is launched from scratch
  		//if(MainActivity.fromMainActivity){
  		//	twitterLogIn();
  			
  		//}else{
 	
  		if(twitterLoggedIn){
  			
  			initControl();  		
  			
  		}else{
  		
        twitterLogIn();
  		}
  		
  		/**
  		TWITTER_CONSUMER_KEY = getActivity().getString(R.string.consumer_key);
  		TWITTER_CONSUMER_SECRET = getActivity().getString(R.string.consumer_secret);
  		
  		
  		Boolean twitterLoggedIn;
  		
  		Log.d("BYI" , "Twitter Token: " + mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, null));
  		
  		
  		twitterLoggedIn = mSharedPreferences.getBoolean(TwitterLoggedInPref, false);
  		
  		//This if else forces login if app is launched from scratch
  		//if(MainActivity.fromMainActivity){
  		//	twitterLogIn();
  			
  		//}else{
 	
  		String testString = mSharedPreferences.getString(PREF_KEY_FOURSQUARE_TOKEN, "nothing");
  		if(testString.equals("nothing")){
  			
  			Log.d("BYI", "Test true");
  		}else{
  			Log.d("BYI", "Test false");
  			
  		}
  		
  		
  		if(twitterLoggedIn){	
  			initControl();  		
  		}else{
        
  		}
  		*/
  		//}
        /**ABOVE IS THE TWITTER CODE
         * 
         * 
         * 
         */
        

  		
  		
  		/**
  		 * CODE BELOW IS FOR FOURSQUARE 
  		 *
  		 * 
  		 * 
  		
  		 */
  		
  		Boolean foursquareLogIn = mSharedPreferences.getBoolean(foursquareLoggedIn, false);
  		//Lorenz FourSquare Method
  		myFourSquareApp = new FoursquareApp(getActivity(), CLIENT_ID, CLIENT_SECRET);
  		
  		/**
  		 * New native way to log into foursquare
  		if(!foursquareLogIn){
  			foursquareNativeConnect();
  		}else{
  			//pass token to 	
  			
  		String fsqToken = mSharedPreferences.getString(foursquareAccessToken, null);	
  		Log.d("BYI", "Foursquare Token From Pulp: " + fsqToken);
  		myFourSquareApp.getAccessTokenFromPulp(fsqToken);
  		
  		}
  		*/
  		
  		FsqAuthListener listener = new FsqAuthListener() {

			@Override
			public void onSuccess() {
				// TODO Auto-generated method stub
				Log.d("BYI", "Foursquare User Name: " + myFourSquareApp.getUserName());
				Log.d("BYI", "Foursquare Has Token: " + myFourSquareApp.hasAccessToken());	
			}

			@Override
			public void onFail(String error) {
				// TODO Auto-generated method stub
				Log.d("BYI", "Foursquare Login Failed");
			}  			
  		};
  		
  		myFourSquareApp.setListener(listener);
  		
  		
  		
  		/**  Old way to log into foursquare using a web view.  Couldnt do facebook so using new native one
  		//check to see if foursquare is already logged in*/
  		if (!myFourSquareApp.hasAccessToken()) {
  			myFourSquareApp.authorize();
  		}
  		
  		
  		
  	  		  		
  		
  		/**
  		 * CODE ABOVE IS FOR FOURSQUARE 
  		 *
  		 * 
  		 * 
  		 */
  		
        
        //adding button/switch listeners
        addShareButtonListener();
        addFoursquareButtonListener();
        addTwitterSwitchListener();
        addPhotoShareListener();
        
        return rootView;
        
        
       
    }
    
    
    public void getUserLocation(){
    	
    	backupStatusText();
    	
    	
    	user = null;
    	lm = (LocationManager) getActivity().getSystemService(getActivity().LOCATION_SERVICE);
    	Criteria criteria = new Criteria();
    	criteria.setAccuracy(Criteria.ACCURACY_COARSE);
    	
    	locationListener = new LocationListener(){

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				//location = lm.getLastKnownLocation(bestProvider);
				
		    
				if(location.getAccuracy() < 100){
				
		    	Log.d("BYI", "Location Accuracy is: " +location.getAccuracy());
		    	Log.d("BYI", "Location Best Provider is: " + bestProvider);
		    	Log.d("BYI", "Location Provider is: " + location.getProvider());
		    	getNearbyLocationsFourSquare(location);
		    	
				lm.removeUpdates(locationListener);
				}
			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
				
			}
    		
    	};
    	
    	bestProvider = lm.getBestProvider(criteria, true);
    	
    	Log.d("BYI", "Providers: " + lm.getProviders(true));
    	
    	lm.requestLocationUpdates(bestProvider, 0, 0, locationListener);
    	lm.requestLocationUpdates(lm.NETWORK_PROVIDER, 0, 0, locationListener);
    	lm.requestLocationUpdates(lm.GPS_PROVIDER, 0, 0, locationListener);
    	
    }
    
    
    
    public void getNearbyLocationsFourSquare(Location userLocation){
    	
 
    	if (userLocation == null){
    		Log.d("BYI", "Foursquare: User Locatin not found");
    	} else{
    		geocoder = new Geocoder(getActivity());
    			try{
    				user = geocoder.getFromLocation(userLocation.getLatitude(), userLocation.getLongitude(), 1);
    				lat = (double)user.get(0).getLatitude();
    				lon = (double)user.get(0).getLongitude();
    				
    				Log.d("BYI", "Location from GPS Lat: " + lat + " - Long - " + lon);
    				
    				Toast.makeText(getActivity(),"GPS LOCATION IS: " + lat + " - Long - " + lon, Toast.LENGTH_LONG).show();
    				
    				//Here is the old code but put within the get location.  If this doesnt work, might have to make a new method that
    				//specifically gets location then pass it to getNearbyLocationsFourSquare() method.
    				new Thread(){
    		    		
    		    		public void run(){
    		    			int what = 0;
    		    			
    		    			try {
    		    				myNearbyList = myFourSquareApp.getNearby(lat, lon);
    		    			} catch (Exception e){
    		    				what = 1;
    		    				e.printStackTrace();
    		    				Log.d("BYI", "FourSquare getting Venues Error: " + e);
    		    			
    		    			}
    		    			mHandler.sendMessage(mHandler.obtainMessage(what));
    		    		}
    		    	}.start();
    				
    				
    			}catch (Exception e){
    				e.printStackTrace();
    			}
    	
    	}
  
    	lm.removeUpdates(this);
    	
 
    	
    	
    }
    
    
    private Handler mHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		
    		
    		if (msg.what == 0) {
    			if (myNearbyList.size() == 0) {
    				Log.d("BYI", "FourSquare: NO LOCATIONS NEAR BY");
    				return;
    			}
    			
    			
    			//Logic to start shareoptionlaunchershell activity pass parameter to open foursquare venue list fragment
    			
    			Intent detailIntent = new Intent(getActivity(), ShareOptionLauncherShell.class);
                detailIntent.putExtra("ShareOptionChosen", "FoursquareVenueList");
                //Log.d(debugTag, ShareOptionLauncherActivity.SHARE_ID);
                //Log.d(debugTag, shareOptions[position]);
                startActivity(detailIntent);
    			
                //will set the nearby list in this fragment, want to start a new fragment
    			//myNearbyAdapter.setData(myNearbyList);    			
    			//myListView.setAdapter(myNearbyAdapter);
    			
    		
    		} else {
    			Log.d("BYI", "FourSquare: Failed to get any locations");
    		}
    	}
    };
   
    public static ArrayList<FsqVenue> getList(){
    	return myNearbyList;
    }
    

    @Override
   	public void onActivityResult(int requestCode, int resultCode, Intent data){
   		super.onActivityResult(requestCode, resultCode, data);
   		Session.getActiveSession().onActivityResult(getActivity(),requestCode, resultCode, data);
   		
   		switch (requestCode){
   		//native foursquare authentication code
   		case REQUEST_CODE_FSQ_CONNECT:
   		 onCompleteConnect(resultCode, data);
   			//AuthCodeResponse codeResponse = FoursquareOAuth.getAuthCodeFromResult(resultCode, data);
   			break;		
   		//native foursquare authentication code
   		case REQUEST_CODE_FSQ_TOKEN_EXCHANGE:
   			onCompleteTokenExchange(resultCode, data);
   			break;   			
   		//Sharing Photo
   		case TAKE_PHOTO:
   			Log.d("BYI", "OnActivityResult: Take Photo");
   			if (image == null){
   				Log.d("BYI", "No Image Selected from Camera");
   			}
   			else{
   				setPhoto(image.getPath());
   				convertUriToByteArray(image);   				
   			}
   			
   			break;   			
   		//Sharing Photo
   		case BROWSE_GALLERY:
   			
   			Log.d("BYI", "OnActivityResult: Browse Gallery");
   			
   			String[] columns = { MediaStore.Images.Media.DATA };

			Uri imageUri = data.getData();

			Cursor cursor = getActivity().getContentResolver().query(imageUri, columns, null,
					null, null);

			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(columns[0]);
			String imagePath = cursor.getString(columnIndex);

			cursor.close();

			image = Uri.parse(imagePath);
			
			if(image == null){
			Log.d("BYI", "No Image Selected from Gallery");
			}
			else{
				setPhoto(image.getPath());
				
				//Need this in order to make the file path correct from browsing the gallery
				Uri imageFromGallery = Uri.parse("file://" + imagePath);
					
				convertUriToByteArray(imageFromGallery);  				
   			}
   			break;   			
   		}
   		
   		
   	}
    
    
    private void twitterLogIn(){
		MainActivity.fromMainActivity = false;    	
    	if (true){
    		//getActivity();
			//Authenticate user with Twitter
    		new TwitterAuthenticateTask().execute();
    	}  	
    }
	
    
    
    //Button Listener to Share Status
    private void addShareButtonListener(){
    	Button shareButton = (Button) rootView.findViewById(R.id.share_status_button);
    	
    	shareButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub			
				Log.d(debugTag, "Share Button Has Been Clicked");	    	
				shareStatus();
				
				
			}
		});    	    	
    }
    
    
    //Foursquare Button Listener
    private void addFoursquareButtonListener(){
    	foursquareButton.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				getUserLocation();
				
				
				
			}
    		
    	});
    	
    }
    
    //Twitter switch listener
    private void addTwitterSwitchListener(){
    	twitterSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
			
				/** 
				 * Twitter logging in tied to switch
				TWITTER_CONSUMER_KEY = getActivity().getString(R.string.consumer_key);
		  		TWITTER_CONSUMER_SECRET = getActivity().getString(R.string.consumer_secret);
		  		
		  		
		  		Boolean twitterLoggedIn;
		  		
		  		Log.d("BYI" , "Twitter Token: " + mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, null));
		  		
		  		
		  		twitterLoggedIn = mSharedPreferences.getBoolean(TwitterLoggedInPref, false);
		  		
		  		//This if else forces login if app is launched from scratch
		  		//if(MainActivity.fromMainActivity){
		  		//	twitterLogIn();
		  			
		  		//}else{
		  	
		  		
		  		if(twitterLoggedIn){	
		  			initControl();  		
		  		}else{
		        twitterLogIn();
		  		}
				*/
				
			}
    		
    	});
    	
    }
    
    //Button Listener to Share Status
    private void addPhotoShareListener(){
    	Button sharePhotoButton = (Button) rootView.findViewById(R.id.share_picture_button);
    	
    	sharePhotoButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub			
				Log.d(debugTag, "Share Button Has Been Clicked");	    	
				
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				View v2 = getLayoutInflater(null).inflate(R.layout.take_photo_browse_gallery,null);
				builder.setTitle("Would you like to take a photo or select one from your gallery");
				builder.setView(v2);
				final AlertDialog dlg = builder.create();
				dlg.show();
				
				Button takePhoto = (Button) dlg.findViewById(R.id.take_photo);
				Button browseGallery = (Button) dlg.findViewById(R.id.browse_gallery);

				takePhoto.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						Log.d(debugTag, "Take Photo Option Chosen");
						dlg.dismiss();
						// Invoke the camera activity.
						takePhoto();
					}
				});

				browseGallery.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						// Browse the gallery.
						Log.d(debugTag, "Browse Gallery Option Chosen");
						dlg.dismiss();
						browseGallery();
					}
				});			
			}
		});    	    	
    }
    
    
    private void takePhoto() {

		File picturesDirectory = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File imageFile = new File(picturesDirectory, "share_photo");

		Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
		image = Uri.fromFile(imageFile);
		startActivityForResult(i, TAKE_PHOTO);
		
	}

    
    private void browseGallery() {
		Intent i = new Intent(Intent.ACTION_PICK,
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(i, BROWSE_GALLERY);
	}

    public void convertUriToByteArray(Uri img){
    	try {
    		//This is where the Filenot found exception happens for browse photo
			InputStream iStream = getActivity().getContentResolver().openInputStream(img);
			
			ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];
			
			int len = 0;
			try {
				while ((len = iStream.read(buffer)) != -1){
					byteBuffer.write(buffer, 0, len);
				}
				
				imageByteArray = byteBuffer.toByteArray();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d("BYI", "Error IOException converting to bytearray: " + e);
			}	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d("BYI", "Error FileNotFound: " + e);
		}
    Log.d("BYI", "Conversion to Byte Array seems to have worked");
    }
    
    //Sets preview of photo you are sharing
    public void setPhoto(String pathOfImage){
    	
    	ImageView imageView = (ImageView) rootView.findViewById(R.id.picture_to_share);
    	if(image != null){
    		imageView.setImageURI(Uri.parse(pathOfImage));
    	}else
    	{
    		Log.d("BYI", "Putting Picutre Into Image View Failed: No Picture");
    	}
    	
    }
    
    
    //Method that will execute the posting tasks on social networks - need to make sure there is no space for the last character
    private void shareStatus(){
    	
    	// Get the status update text
		
		
		statusText = null;
		statusText = statusTextBox.getText().toString();
		Log.d("BYI", statusText);

		
	//Check to see status text is blank	
    if(statusText.matches("")){
    	//if blank toast
    	Toast.makeText(getActivity(), "No Status In Text Box!", Toast.LENGTH_LONG).show();		
    }else{
	//Status text box is not empty
    	
		
    //Get the values of all the social network switches	
    
    Log.d(debugTag,"Facebook Switch is: " + Boolean.toString(facebookSwitch.isChecked())); 	
    Log.d(debugTag,"Twitter Switch is: " + Boolean.toString(twitterSwitch.isChecked())); 
    
    
   if(!isCheckingIntoFoursquare){ 
    	
  //Post to Facebook if facebookSwitch.isChecked()
    if(facebookSwitch.isChecked()){
    PostOnWallFaceBook postWallFacebook = new PostOnWallFaceBook();
    postWallFacebook.execute();
    
    //DOES NOT WORK
    	//testPublishStory();
    }
    
  //Post to Twitter if twitterSwitch.isChecked() 
    if(twitterSwitch.isChecked()){
    	PostOnTwitter postTwitter = new PostOnTwitter();
    	postTwitter.execute(statusText);  
    }	
    
   }else{
	//Foursquare switch is checked...use foursquare API to post to twitter and facebook if needed [NEED TO FIX THIS]
	   
	   String broadcast;
	   
	   if(facebookSwitch.isChecked() && twitterSwitch.isChecked()){
		   //broadcast = "public,facebook,twitter";   
		   broadcast = "public,twitter";
		    PostOnWallFaceBook postWallFacebook = new PostOnWallFaceBook();
		    postWallFacebook.execute();
	   } 
	   
	   else if(!facebookSwitch.isChecked() && twitterSwitch.isChecked()){
		   broadcast = "public,twitter";		   
	   }
	   
	   else if(facebookSwitch.isChecked() && !twitterSwitch.isChecked()){
		    //broadcast = "public,facebook";
		    broadcast = "public";
		    PostOnWallFaceBook postWallFacebook = new PostOnWallFaceBook();
		    postWallFacebook.execute();
	   } else{
		   broadcast = "public";
	   }
	   
	   try{
		   if(image == null){
			   myFourSquareApp.postCheckIn(statusText.replaceAll(" ", "%20"), broadcast,foursquareVenueClickedID,"nothing");
		   }else{
			   myFourSquareApp.postCheckIn(statusText.replaceAll(" ", "%20"), broadcast,foursquareVenueClickedID,image.getPath());
		   }
	   }catch (Exception e){
		   Log.d("BYI", "Foursquare post checkin error: " + e);
		   Log.d("BYI", "Foursquare post checkin error: " + broadcast);
		   Log.d("BYI", "Foursquare post checkin error: " + foursquareVenueClickedID);
		   
	   }
	   
   }

    }
    
    //Set switches and text box back to beginning, switches need to be auto on as per user preference
    //This should be in on post post successful
    //in the future, might want to make the share text in the background, and once ALL posts are successful then toast, this is artificial
    statusTextBox.setText("");
    facebookSwitch.setChecked(false);
    twitterSwitch.setChecked(false);
    venueTextView.setText("");
    Toast.makeText(getActivity(), "Post Successful!", Toast.LENGTH_LONG).show();
    statusText = "";
    foursquareVenueClickedName = "nothing";
    foursquareVenueClickedID = "nothing";
    
    //end class
    }
    
    
    private void backupStatusText(){
    	
    	SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(STATUS_BACKUP, statusTextBox.getText().toString());
        editor.commit();
    	
    }
    
    
    
    //DOES not post to timeline.  Need to fix and submit for approval.  it is set up however for when we are ready
    public void testPublishStory() {
    	
    	
    	try{
    		
    		Session session = Session.getActiveSession();
    		
    		
    		//Only run if getPermissionFlag = 0
			if(getPermissionFlag == 0){	
			List<String> permissions = session.getPermissions();
			
			if(!isSubsetOf (PERMISSIONS, permissions)){				
				 pendingPublishReauthorization = true;
		        Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(getActivity(), PERMISSIONS);
		        session.requestNewPublishPermissions(newPermissionsRequest);
			}
			}
    		
    		 
		        JSONObject book = new JSONObject();
		        book.put("title", "a status");
		        //book.put("location", "https://foursquare.com/v/somewhere-loud/5179ef7fe4b076127bf04922");
		       // book.put("url", "https://foursquare.com/v/somewhere-loud/5179ef7fe4b076127bf04922");
		        
		        
		        //JSONObject data = new JSONObject();
		        //data.put("isbn", "1234567890");
		        //book.put("data", data);
		        Bundle objectParams = new Bundle();
		        
		        objectParams.putString("object", book.toString());
		        
		        
		        Request.Callback objectCallback = new Request.Callback() {

                    @Override
                    public void onCompleted(Response response) {
                            // Log any response error
                    	try{
                    	final String objectId = response.getGraphObject().getInnerJSONObject().getString("id");
                    	Bundle params2 = new Bundle();
                    	//params2.putString("book", objectId);
                    	//params2.putString("venue", objectId);
                    	params2.putString("status", objectId);
                    	params2.putString("place", "https://foursquare.com/v/somewhere-loud/5179ef7fe4b076127bf04922");
                    	params2.putString("message", statusText);
                    	Request.Callback callback2 = new Request.Callback(){
						@Override
						public void onCompleted(Response response) {
							// TODO Auto-generated method stub
							try{
								String actionId = response.getGraphObject().getInnerJSONObject().getString("id");
								Log.d("BYI", "Action ID from facebook post:" + actionId);
							}catch (Exception e){
								e.printStackTrace();
							}					
						}
                    	};
                    	
                    	//Request request2 = new Request(Session.getActiveSession(), "me/books.reads",params2,HttpMethod.POST,callback2);
                    	//Request request2 = new Request(Session.getActiveSession(), "me/pulpsharehub:at",params2,HttpMethod.POST,callback2);
                    	Request request2 = new Request(Session.getActiveSession(), "me/pulpsharehub:post",params2,HttpMethod.POST,callback2);
                    	RequestAsyncTask task2 = new RequestAsyncTask(request2);
                    	task2.execute();
                    	}	
                    	catch (JSONException e){
                    		e.printStackTrace();
                    	}
                         
                    }
        };
		        
		        //Request objectRequest = new Request(session,"me/objects/books.book", objectParams, HttpMethod.POST,objectCallback);
		        //Request objectRequest = new Request(session,"me/objects/pulpsharehub:venue", objectParams, HttpMethod.POST,objectCallback);
		        Request objectRequest = new Request(session,"me/objects/pulpsharehub:status", objectParams, HttpMethod.POST,objectCallback);
		        RequestAsyncTask task1 = new RequestAsyncTask(objectRequest);
		        task1.execute();
             
		        }
		        catch (JSONException e){
		        	
		        }
    	
    	
    	
    	
    }
    
    
    public class PostOnWallFaceBook extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			Session session = Session.getActiveSession();
			if (session != null){	
				//Check for publish permissions
				//Only run if getPermissionFlag = 0
				if(getPermissionFlag == 0){	
				List<String> permissions = session.getPermissions();
				
				if(!isSubsetOf (PERMISSIONS, permissions)){				
					 pendingPublishReauthorization = true;
			        Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(getActivity(), PERMISSIONS);
			        session.requestNewPublishPermissions(newPermissionsRequest);
				}
				}

				  Bundle postParams = new Bundle();
			        //postParams.putString("name", "Hello This is a Test");
			        //postParams.putString("caption", "Trying this code out");
			        //postParams.putString("privacy", "friends");
			        //postParams.putString("description", "The Facebook SDK for Android makes it easier and faster to develop Facebook integrated Android apps.");
			        //postParams.putString("link", "https://foursquare.com/v/somewhere-loud/5179ef7fe4b076127bf04922");
			       //postParams.putString("place",  "http://www.yelp.com/biz/christa-wonderful-market-san-francisco");
			       //postParams.putString("url", "https://foursquare.com/v/somewhere-loud/5179ef7fe4b076127bf04922");
			     // postParams.putString("title", "HI");
			       
			        if(isCheckingIntoFoursquare){
			        postParams.putString("message",  statusText + "\n" + "\r\n"+ "I am at:");
			        postParams.putString("link", "https://foursquare.com/v/" + foursquareVenueClickedID);
			       
			        }else{
			        	postParams.putString("message",statusText);
			        	
			        }
			        
			        //Check to see if there is an image to post -- if checking into foursquare, cannot post picture with check in
			        if(!isCheckingIntoFoursquare){
			        if (image != null){
			        postParams.putByteArray("picture", imageByteArray);
			        }
			        }
			        
			        
			        /**
			         * This code will put a link next to the "share and comment" buttons for facebook
			         *
			        JSONStringer actions;
			        try{
			        	
			        	actions = new JSONStringer().object().key("name").value("CLICKTEST").key("link").value("https://foursquare.com/v/somewhere-loud/5179ef7fe4b076127bf04922").endObject();
			        	postParams.putString("actions", actions.toString());
			        	
			        }catch (JSONException e){
			        	
			        }
			        
			        */
			        
			     
			       	        
				Request.Callback callback = new Request.Callback() {
					
					public void onCompleted(Response response){
		
						//Log.d("BYI", "Facebook Error: " + response.getError());
						
						JSONObject graphResponse = response
															.getGraphObject()
															.getInnerJSONObject();
						
						String postId = null;
						
						try {
							postId = graphResponse.getString("id");
							
							
							
						} catch (JSONException e){
							
							Log.d("BYI", "Facebook Error " + e.getMessage());
							
							
						}
								
						FacebookRequestError error = response.getError();
						
						if (error != null) {
							
						} else {
						}
						
				
						}
							
						
					};
					
					// Use me/feed without photo me/photos for picture
					
					Request request;
					//Check to see if there is an image to post
					if(isCheckingIntoFoursquare){
						request = new Request(session, "me/feed", postParams, HttpMethod.POST, callback);
					}else
			        if (image != null){
			        request = new Request(session, "me/photos", postParams, HttpMethod.POST, callback);
			        
			        }else{
			        request = new Request(session, "me/feed", postParams, HttpMethod.POST, callback);

			        }
					
				    request.executeAndWait();
					
				    //request.executeBatchAndWait();
				   
			       
					
					
					
					//request.executeAsync() code for executing async in a UI thread (not a fragment);
					/**
					//Old way to execute task without do in background, used do in background so I can use the on post execute method
					RequestAsyncTask task = new RequestAsyncTask(request);
					task.execute();
					*/
					
				}
	
			return null;
		}
		
		
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			Log.d("BYI", "POST: -- " + statusText + " -- SUCCESSFULL from Pulp");
			Toast.makeText(getActivity(), "Post Successful!", Toast.LENGTH_LONG).show();
			
			//Set getPermissionFlag to 1 after complete.  
			//If another status is shared in the same instance, it will not run getPermissions()
			getPermissionFlag = 1;
		}
    
    }
    
    
    //Method to determine whether the app has the permissions from Facebook or not
    private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
	    for (String string : subset) {
	        if (!superset.contains(string)) {
	            return false;
	        }
	    }
	    return true;
	}
    
    
    
    
    
    //TwitterLogInAsync
    class TwitterAuthenticateTask extends AsyncTask<String, String, RequestToken> {

		@Override
		protected void onPostExecute(RequestToken requestToken){
			
			if (!requestToken.equals(null)){
			Editor editor = mSharedPreferences.edit();
			editor.putBoolean(TwitterLoggedInPref, true);
			editor.commit();
			}
			
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL()));
			startActivity(intent);
			
		}
    	

    	@Override
		protected RequestToken doInBackground(String... params) {
			// TODO Auto-generated method stub
    		return TwitterUtil.getInstance().getRequestToken();
		} 	
    }
    
    
    public final static class TwitterUtil {
    	 
        private RequestToken requestToken = null;
        private TwitterFactory twitterFactory = null;
        private Twitter twitter;
     
        public TwitterUtil() {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
            configurationBuilder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
            configurationBuilder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
            Configuration configuration = configurationBuilder.build();
            twitterFactory = new TwitterFactory(configuration);
            twitter = twitterFactory.getInstance();
        }
     
        public TwitterFactory getTwitterFactory()
        {
            return twitterFactory;
        }
     
        public void setTwitterFactory(AccessToken accessToken)
        {
            twitter = twitterFactory.getInstance();
        }
     
        public Twitter getTwitter()
        {
            return twitter;
        }
        public RequestToken getRequestToken() {
            if (requestToken == null) {
                try {
                    requestToken = twitterFactory.getInstance().getOAuthRequestToken(TWITTER_CALLBACK_URL);                    
                } catch (TwitterException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            return requestToken;
        }
     
       static TwitterUtil instance = new TwitterUtil();
     
        public static  TwitterUtil getInstance() {
            return instance;
        }
     
     
        public void reset() {
            instance = new TwitterUtil();
        }
    }
    
  
    
    private void initControl() {
        Uri uri = getActivity().getIntent().getData();
        if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
            String verifier = uri.getQueryParameter(URL_PARAMETER_TWITTER_OAUTH_VERIFIER);
            new TwitterGetAccessTokenTask().execute(verifier);
        } else
            new TwitterGetAccessTokenTask().execute("");
    }
    
    
    class TwitterGetAccessTokenTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPostExecute(String userName) {
            //Log.d("BYI", userName);
        }

        @Override
        protected String doInBackground(String... params) {

            Twitter twitter = TwitterUtil.getInstance().getTwitter();
            RequestToken requestToken = TwitterUtil.getInstance().getRequestToken();
            if (!false) {
                try {

                    twitter4j.auth.AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, params[0]);
                   
                   //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                    editor.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
                    editor.putBoolean(TwitterLoggedInPref, true);
                    editor.commit();
                    
                    Log.d("BYI", "TWITTER TOKEN: " + mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN,null));
                    Log.d("BYI", "TWITTER SECRET: " + mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET,null));
                    Log.d("BYI", "TWITTER USER: " + twitter.showUser(accessToken.getUserId()).getName());
                    
                    return twitter.showUser(accessToken.getUserId()).getName();
                    
                    
              
                    
                } catch (TwitterException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            } else {
            	
            	Log.d("BYI", "NO TOKEN FOUND");
            
               // SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                //String accessTokenString = sharedPreferences.getString(ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN, "");
                //String accessTokenSecret = sharedPreferences.getString(ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN_SECRET, "");
                //AccessToken accessToken = new AccessToken(accessTokenString, accessTokenSecret);
               
            	/**
            	try {
                    TwitterUtil.getInstance().setTwitterFactory(accessToken);
                    return TwitterUtil.getInstance().getTwitter().showUser(accessToken.getUserId()).getName();
                } catch (TwitterException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                
                */
            }

            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    
    class PostOnTwitter extends AsyncTask <String, String, Boolean>{

    	@Override
    	protected void onPostExecute(Boolean result){
    		if (result){
    			
    			Log.d("BYI", "TWEET SUCCESSFULL");
    			
    		} else{
    			
    			Log.d("BYI", "TWEET FAILED");
    		}    		    		
    	}
    	
    	
		@Override
		protected Boolean doInBackground(String... params) {
			// TODO Auto-generated method stub
			
			try {
				
				String accessTokenString = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN,"");
				String accessTokenSecret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");
				
				if (!accessTokenString.equals(null) && !accessTokenSecret.equals(null)){			
					twitter4j.auth.AccessToken accessToken = new twitter4j.auth.AccessToken(accessTokenString, accessTokenSecret);					
					if(image == null){
					Log.d("BYI", "STATUS TO TWEET: " + params[0]);					
					twitter4j.Status status = TwitterUtil.getInstance().getTwitterFactory().getInstance(accessToken).updateStatus(params[0]);
					}else{
					// TEST FOR PHOTO
					StatusUpdate statusPhoto = new StatusUpdate(params[0]);
					statusPhoto.setMedia(new File(image.getPath()));
					twitter4j.Status status = TwitterUtil.getInstance().getTwitterFactory().getInstance(accessToken).updateStatus(statusPhoto);
					}	
					return true;
					
				}
				
			} catch (TwitterException e){
				
				e.printStackTrace();
				Log.d("BYI", "Twitter Error: " + e.getCause());
				Log.d("BYI", "Twitter Error: " + e.getExceptionCode());
				
			}
			
			return false;
			
		}
	
    }


	

	

	
	
	/** Code below is the overrides for the locationmanager implementation i dont think we need this anymore
	 *
	 */
	
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		location = lm.getLastKnownLocation(bestProvider);
    	
    	Log.d("BYI", "Location Accuracy is: " +location.getAccuracy());
    	Log.d("BYI", "Location Best Provider is: " + bestProvider);
    	getNearbyLocationsFourSquare(location);
		
	}

	
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	/**Code above is for the locationmanager implementation
	 * 
	 * 
	 * 
	 */
    
  
    
    //Native Foursquare authentication
	public void foursquareNativeConnect(){
		
		Intent i = FoursquareOAuth.getConnectIntent(getActivity(), CLIENT_ID);
 
        // If the device does not have the Foursquare app installed, we'd
        // get an intent back that would open the Play Store for download.
        // Otherwise we start the auth flow.
        if (FoursquareOAuth.isPlayStoreIntent(i)) {
            startActivity(i);
        } else {
            startActivityForResult(i, REQUEST_CODE_FSQ_CONNECT);
        }

	}
	 private void onCompleteConnect(int resultCode, Intent data) {
	        AuthCodeResponse codeResponse = FoursquareOAuth.getAuthCodeFromResult(resultCode, data);
	        Exception exception = codeResponse.getException();
	        
	        if (exception == null) {
	            // Success.
	            String code = codeResponse.getCode();
	            performTokenExchange(code);

	        } else {
	            Log.d("BYI", "Foursquare Native Error: " + exception.toString());
	        }
	    }    
	
	 private void performTokenExchange(String code) {
	        Intent intent = FoursquareOAuth.getTokenExchangeIntent(getActivity(), CLIENT_ID, CLIENT_SECRET, code);
	        startActivityForResult(intent, REQUEST_CODE_FSQ_TOKEN_EXCHANGE);
	    }
	
	  private void onCompleteTokenExchange(int resultCode, Intent data) {
	        com.foursquare.android.nativeoauth.model.AccessTokenResponse tokenResponse = FoursquareOAuth.getTokenFromResult(resultCode, data);
	        Exception exception = tokenResponse.getException();
	        
	        if (exception == null) {
	            String accessTokenFSQ = tokenResponse.getAccessToken();
	            // Success.
	           Log.d("BYI", "Foursquare Access Token from Native: " + accessTokenFSQ);
	           
	           Editor editor = mSharedPreferences.edit();
				editor.putBoolean(foursquareLoggedIn, true);
				editor.putString(foursquareAccessToken, accessTokenFSQ);
				editor.commit();
	            
	            // Persist the token for later use. In this example, we save
	            // it to shared prefs.
	            
	            myFourSquareApp.getAccessTokenFromPulp(accessTokenFSQ);
	            // Refresh UI.
	            
	            
	        } else {
	           Log.d("BYI", "Foursquare Token Exchange Error: " + exception.toString());
	        }
	    }
    
}


	
	

