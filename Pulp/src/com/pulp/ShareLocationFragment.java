package com.pulp;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.cloud.backend.core.CloudBackend;
import com.google.cloud.backend.core.CloudBackendAsync;
import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;
import com.google.cloud.backend.core.CloudQuery;
import com.google.cloud.backend.core.CloudQuery.Scope;
import com.google.cloud.backend.core.Filter;



import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ShareLocationFragment extends Fragment {

	//General stuff
	public static final String debugTag = "BYI";
	private View rootView;
	Button shareLocationButton;
	Button trackLocationButton;
	Button centerMapButton;
	Button endTrackingLocationButton;
	public static SharedPreferences mSharedPreferences;
	
	
	//GPS and location tracking stuff
	public String bestProvider;
	public Geocoder geocoder;
	LocationManager lm;
	LocationListener locationListener;
	List<Address> user;
	double lat; //= Double.valueOf(latitude);
	double lon; //= Double.valueOf(longitude);
	String mapsURL = "http://maps.google.com/?q=";
	String mapURLOneTimeTracking;
	String shareIDContinuousTrackingURL;
	Boolean sessionIDCreated;
	Boolean gpsSignalReceived;
	PolylineOptions mapLineOptions;
	Marker mapMarker;
	double cycleCount; //tracks the cycles so we can remove the map markers when we are retrieving locations
	Boolean autoCenterMap;
	double defaultZoomLevel = 16;
	String locationMessage;
	private static final String LOCATION_MESSAGE = "message";
	private static final String RANDOM_SESSION_ID = "Random Session ID";
	
	//Timer Tracking Stuff
	private static final String TIME_TO_TRACK = "time to track location";
	int timerCount;
	String randSessionID;
	int trackingInterval;  //in miliseconds
	int hourTimer;
	int minuteTimer;
	TimerCount tCount;
	Boolean keepCycling; //test variable to see if we can keep GPS on if it is false then we will keep the GPS on, else we turn it off
	
	//SupportMapFragment m;
	GoogleMap gMap;
	
	
	  //Cloud DB Stuff
    private CloudBackendFragment mProcessingFragment;
    private FragmentManager mFragmentManager;
    private static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";
	
    //Notification Bar Stuff
    NotificationCompat.Builder mBuilder;
    NotificationManager mNotificationManager;
    
    
    
	public ShareLocationFragment(){
		
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	}

	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
		rootView = inflater.inflate(R.layout.share_location_main, container, false);
		shareLocationButton = (Button) rootView.findViewById(R.id.share_location_button);
		trackLocationButton = (Button) rootView.findViewById(R.id.track_location_button);
		centerMapButton = (Button) rootView.findViewById(R.id.center_map_button);
		endTrackingLocationButton = (Button) rootView.findViewById(R.id.end_location_share_button);
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		cycleCount = 0;
		keepCycling = false;
		autoCenterMap = true;
		

		getActivity().getActionBar().setTitle("Share Location");
		
		
		//ActionBar actionBar = getActivity().getActionBar();
		//actionBar.setDisplayHomeAsUpEnabled(false);
		
		//getting the unique ID from the shared URL
		Bundle shareBundle = this.getArguments();
		if (!(shareBundle == null)){
			randSessionID = "\"" + shareBundle.getString("uniqueKey") + "\"";
			Log.d("byi", "retreieved unique session ID from the URL is (from on create): "  + randSessionID);
			RetrieveFromCloud rFC = new RetrieveFromCloud();	
			rFC.execute();
		}
		
		
		sessionIDCreated = false;
		gpsSignalReceived = false;
			
		gMap = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map_view)).getMap();
		
		
		
		
		
		addShareLocationButtonListener();
		addTrackLocationButtonListener();
		addEndTrackLocationButtonListener();
		addCenterMapButtonListener();
		addMarkerDragListener();
		
		
		//making buttons invisible until location is found
		shareLocationButton.setVisibility(View.GONE);
		trackLocationButton.setVisibility(View.GONE);
		endTrackingLocationButton.setVisibility(View.GONE);
		centerMapButton.setVisibility(View.GONE);
		
		getUserLocation(0);
		return rootView;
		
	}
	
	// Button Listener to Share Location One Time - for now it initiates the received location
	private void addShareLocationButtonListener() {
		shareLocationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d(debugTag, "Share Location Button Has Been Clicked");
				sendLocation(mapURLOneTimeTracking,"everything");
				
			}
		});
	}

	// Button Listener to Track Location
	private void addTrackLocationButtonListener() {
		trackLocationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d(debugTag, "Track Location Button Has Been Clicked");

				CheckID checkID = new CheckID();
				checkID.execute();

			}
		});
	}
	
	// Button Listener to Stop Track Location
	private void addEndTrackLocationButtonListener() {
		endTrackingLocationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d(debugTag, "End Location Button Has Been Clicked");
				// need end timer button to put this in there.
				tCount.endTimer();				
			}
		});
	}

	private void addCenterMapButtonListener(){
		
		centerMapButton.setOnClickListener(new View.OnClickListener() {		
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				reCenterMap();
			}
		});
		
	}
	
	private void addMapMovedListener(){
		
		gMap.setOnCameraChangeListener(new OnCameraChangeListener() {

			@Override
			public void onCameraChange(CameraPosition arg0) {
				// TODO Auto-generated method stub

				float zoomLevel = gMap.getCameraPosition().zoom;
				Log.d("BYI", "map position has changed with lat long of: Lat "
						+ arg0.target.latitude + " + Lon "
						+ arg0.target.longitude);

				userMovedMap(zoomLevel, arg0.target.latitude,
						arg0.target.longitude);

			}
		});
		
		
	}

	private void addMarkerDragListener(){
		
		gMap.setOnMarkerDragListener(new OnMarkerDragListener() {

			@Override
			public void onMarkerDragStart(Marker arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onMarkerDragEnd(Marker arg0) {
				// TODO Auto-generated method stub
					lat = arg0.getPosition().latitude;
					lon = arg0.getPosition().longitude;
			}

			@Override
			public void onMarkerDrag(Marker arg0) {
				// TODO Auto-generated method stub

			}
		});
		
		
	}
	// creates the UI to select tracking attributes
	public void launchTrackingAttributes(final String s) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// LayoutInflater inflater2 = null;
		View v2 = getActivity().getLayoutInflater().inflate(
				R.layout.track_location_attributes, null);
		builder.setView(v2);
		final AlertDialog dlg = builder.create();
		dlg.show();
		
		
		
		Button trackLocationButtonEmail = (Button) dlg.findViewById(R.id.start_tracking_button_email);
		Button trackLocationButtonText = (Button) dlg.findViewById(R.id.start_tracking_button_text);
		final EditText locationText = (EditText) dlg.findViewById(R.id.location_text);
		
		
		trackLocationButtonEmail.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				locationMessage = locationText.getEditableText().toString();				
				Editor editor = mSharedPreferences.edit();
				editor.putString(LOCATION_MESSAGE, locationMessage);
				editor.commit();
				
				// TODO Auto-generated method stub
				dlg.dismiss();
				launchNotification(true);
				Log.d("byi", "Share EMAIL URL is: " + s) ;	
				tCount = new TimerCount();
				tCount.initializeTimer(hourTimer, minuteTimer);
				trackLocation(trackingInterval);
				keepCycling = true;
				getUserLocation(1);
				sendLocation(s,"email");
			}
		});
		
		trackLocationButtonText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				locationMessage = locationText.getEditableText().toString();
				Editor editor = mSharedPreferences.edit();
				editor.putString(LOCATION_MESSAGE, locationMessage);
				editor.commit();
				
				// TODO Auto-generated method stub
				dlg.dismiss();
				launchNotification(true);
				Log.d("byi", "Share TEXT URL is: " + s) ;	
				tCount = new TimerCount();
				tCount.initializeTimer(hourTimer, minuteTimer);
				trackLocation(trackingInterval);
				keepCycling = true;
				getUserLocation(1);
				sendLocation(s,"text");
			}
		});

		
		trackingInterval = 3000;
		
		/**
		 * DISABLING USER SEEK BAR STUFF
		final SeekBar intervalSeekBar = (SeekBar) dlg
				.findViewById(R.id.interval_seek_bar);
		final TextView intervalText = (TextView) dlg
				.findViewById(R.id.interval_text);
		intervalSeekBar.setMax(10);
		intervalSeekBar.setProgress(5);
		intervalText
				.setText(Integer.toString(intervalSeekBar.getProgress() * 2));
		trackingInterval = intervalSeekBar.getProgress() * 2000;
		
		//Disabling the user ability to set the interval
	
		
		
		intervalSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						// TODO Auto-generated method stub

						if (progress >= 0 && progress <= seekBar.getMax()) {

							String progressString = String
									.valueOf(progress * 2);
							intervalText.setText(progressString);
							seekBar.setSecondaryProgress(progress);

							trackingInterval = progress * 2000;

						}
					}
				});
**/
		NumberPicker hourPicker = (NumberPicker) dlg
				.findViewById(R.id.hour_picker);
		final NumberPicker minutePicker = (NumberPicker) dlg
				.findViewById(R.id.minute_picker);
		hourPicker.setMaxValue(24);
		hourPicker.setMinValue(0);
		minutePicker.setMaxValue(59);
		if (hourTimer > 0){
		minutePicker.setMinValue(0);}
		else{
			minutePicker.setMinValue(1);
		}

		hourPicker.setOnValueChangedListener(new OnValueChangeListener() {

			@Override
			public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				Log.d("BYI", "Hour Picked is: " + arg2);
				hourTimer = arg2;
				if (hourTimer > 0){
					minutePicker.setMinValue(0);}
					else{
						minutePicker.setMinValue(1);
					}
			}

		});

		minutePicker.setOnValueChangedListener(new OnValueChangeListener() {

			@Override
			public void onValueChange(NumberPicker arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				Log.d("BYI", "Minute Picked is: " + arg2);
				minuteTimer = arg2;
			}

		});

	}

	public void launchNotification(Boolean displayNotification){
		
	
		//NotificationCompat.Builder 
		if(displayNotification){
		mBuilder =  new NotificationCompat.Builder(getActivity())
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("Pulp Location Share")
		.setContentText("Sharing Location Now");
		
		
		Intent resultIntent = new Intent(getActivity(), ShareOptionLauncherShell.class);
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(getActivity());
		stackBuilder.addParentStack(ShareOptionLauncherShell.class);
		stackBuilder.addNextIntent(resultIntent);
		int requestID = (int) System.currentTimeMillis();
		PendingIntent resultPendingIntent = PendingIntent.getActivity(getActivity(), requestID, resultIntent, 0);
		
		mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setOngoing(true);
		//NotificationManager 
        
        mNotificationManager = (NotificationManager) getActivity()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, mBuilder.build());
		}
		else{
			mNotificationManager.cancel(0);
		}
	}
	
	//Get user's location, drop a pin on the map, and then launch the method to store location in cloud DB
	public void getUserLocation(final int flag) {
		
		user = null;
		lm = (LocationManager) getActivity().getSystemService(
				getActivity().LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		mapURLOneTimeTracking = null;

		locationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				// TODO Auto-generated method stub
				// location = lm.getLastKnownLocation(bestProvider);
				int flag2 = flag;
				if (keepCycling){
					flag2 = 1;
				}else{
					flag2 = 0;
				}
				if (location.getAccuracy() < 100) {
					gpsSignalReceived = true;
					Log.d("BYI",
							"Location Accuracy is: " + location.getAccuracy());
					Log.d("BYI", "Location Best Provider is: " + bestProvider);
					Log.d("BYI",
							"Location Provider is: " + location.getProvider());
					Log.d("BYI", "Lat: " + location.getLatitude());
					Log.d("BYI", "Lon: " + location.getLongitude());

					lat = location.getLatitude();
					lon = location.getLongitude();

					mapURLOneTimeTracking = mapsURL + lat + "," + lon;

			
					//shareLocationButton.setVisibility(View.VISIBLE);
					//trackLocationButton.setVisibility(View.VISIBLE);
					
					LatLng objLatLon = new LatLng(lat, lon);
					
					if(autoCenterMap){
						Log.d("BYI","autocenterMap flag = " + autoCenterMap.toString());
					gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
							objLatLon, 16));
					gMap.animateCamera(CameraUpdateFactory.zoomTo(16), 2000,
							null);
					}


					if (flag2 == 1) { //repeating
						//mapLineOptions.add(objLatLon);
						//mapLineOptions.color(Color.BLUE);
						//gMap.addPolyline(mapLineOptions);
						mapMarker.remove();
						mapMarker = gMap.addMarker(new MarkerOptions().position(objLatLon)
								.title("You are here").draggable(true));	
						mapMarker.showInfoWindow();
						//Test to see if GPS on is better - result removed initiateFragment 
						//decided to separate the GPS loop from the google cloud loop
						//initiateFragments("send");
						
					} else{ //first time app is opened
						lm.removeUpdates(locationListener);
						//mapLineOptions = new PolylineOptions();
						//mapLineOptions.add(objLatLon);
						//mapLineOptions.color(Color.BLUE);
						//gMap.addPolyline(mapLineOptions);
						mapMarker = gMap.addMarker(new MarkerOptions().position(objLatLon)
								.title("You are here").draggable(true));
						mapMarker.showInfoWindow();
						shareLocationButton.setVisibility(View.VISIBLE);
						trackLocationButton.setVisibility(View.VISIBLE);
						centerMapButton.setVisibility(View.VISIBLE);
						addMapMovedListener();
					}

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

		// Test code to see if running this on the main UI thread will work
		getActivity().runOnUiThread(new Runnable() {

			public void run() {
				lm.requestLocationUpdates(bestProvider, 0, 0, locationListener);
				lm.requestLocationUpdates(lm.NETWORK_PROVIDER, 0, 0,
						locationListener);
				lm.requestLocationUpdates(lm.GPS_PROVIDER, 0, 0,
						locationListener);
			}
		});
	}

	//Will see if the user has zoomed in/out or moved the map so that we won't move the map for them
	public void userMovedMap(float zoomLevel, double latMap, double lonMap){
		
		float lowerBoundZoom = 16f *1f;
		float upperBoundZoom = 16f * 1f;
		double lowerLatLonBound = 0.99999;
		double upperLatLonBound = 1.00001;
		latMap = Math.abs(latMap);
		lonMap = Math.abs(lonMap);
		
		if((zoomLevel < lowerBoundZoom || zoomLevel > upperBoundZoom) 
				|| (latMap < Math.abs(lat) * lowerLatLonBound || latMap > Math.abs(lat) * upperLatLonBound) 
				|| (lonMap < Math.abs(lon) * lowerLatLonBound || lonMap > Math.abs(lon) * upperLatLonBound))
		{
			autoCenterMap = false;	
			Log.d("BYI", "autocenter is now FALSE");
			//Log.d("byi", "Zoom Map: " + zoomLevel +"GPS: 16");
			//Log.d("byi", "Lat Map: " + latMap +"GPS: " + lat);
			//Log.d("byi", "Long Map: " + lonMap +"GPS: " + lon);
		}
		else{
			autoCenterMap = true;
			Log.d("BYI", "autocenter is now TRUE");
		}
		
	}
	
	public void reCenterMap(){
		
		autoCenterMap = true;
		LatLng latLong = new LatLng(lat,lon);
		gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
				latLong, 16));
		gMap.animateCamera(CameraUpdateFactory.zoomTo(16), 8000,
				null);
		
		
	}
	
	public void moveMapCamera(LatLng latLong){
		//LatLng objLatLon = new LatLng(lat, lon);
		
		if(autoCenterMap){
		gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
				latLong, 16));
		gMap.animateCamera(CameraUpdateFactory.zoomTo(16), 2000,
				null);
		}
		
		if(cycleCount == 0){
		mapMarker = gMap.addMarker(new MarkerOptions().position(latLong)
				.title(locationMessage).draggable(true));
		mapMarker.showInfoWindow();
		mapLineOptions = new PolylineOptions();
		mapLineOptions.add(latLong);
		mapLineOptions.color(Color.BLUE);
		gMap.addPolyline(mapLineOptions);
		
		}
		else {
			
		mapMarker.remove();	
		mapLineOptions.add(latLong);
		mapLineOptions.color(Color.BLUE);
		gMap.addPolyline(mapLineOptions);	
		mapMarker = gMap.addMarker(new MarkerOptions().position(latLong)
				.title(locationMessage).draggable(true));
		mapMarker.showInfoWindow();
		}
		
		
		cycleCount++;
		
	}
	
	//Method that will launch the one time sharing of a location
	private void sendLocation(String url, String via) {
		if (via.equals("email")) {
			// final int STATIC_RESULT = 2;
			Intent i = new Intent(Intent.ACTION_SEND);
			// i.setType("text/plain");
			i.setType("message/rfc822");
			i.putExtra(Intent.EXTRA_TEXT, url);
			// i.putExtra(android.content.Intent.EXTRA_SUBJECT,"I am sending you my location from Pulp");
			i.putExtra(Intent.EXTRA_SUBJECT,
					"I am sending you my location from Pulp");
			// startActivityForResult(Intent.createChooser(i,
			// "SELECT APPLICATION"), STATIC_RESULT);
			startActivity(Intent.createChooser(i, "SELECT APPLICATION"));
		} else if (via.equals("text")) {
			
			//NEED SOMETHING HERE
			Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
			//i.putExtra(Intent.EXTRA_TEXT, url);
			i.putExtra("sms_body", url);
			//startActivity(Intent.createChooser(i, "SELECT APPLCATION"));
			getActivity().startActivity(i);
			//startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"
               //   )));
			
		} else {
			// final int STATIC_RESULT = 2;
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_TEXT, url);
			// i.putExtra(android.content.Intent.EXTRA_SUBJECT,"I am sending you my location from Pulp");
			i.putExtra(Intent.EXTRA_SUBJECT,
					"I am sending you my location from Pulp");
			// startActivityForResult(Intent.createChooser(i,
			// "SELECT APPLICATION"), STATIC_RESULT);
			startActivity(Intent.createChooser(i, "SELECT APPLICATION"));
		}
		
		
	}

	/**
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent){
		super.onActivityResult(requestCode, resultCode, intent);
		tCount = new TimerCount();
		tCount.initializeTimer(hourTimer, minuteTimer);
		trackLocation(trackingInterval);
	}**/

	// Timer based location tracking i is passed from the button press with the
	// interval i
	// Will fire the method to track location and input to data base after a
	// delay of i milliseconds
	public void trackLocation(int i) {

		Timer t = new Timer();
		Log.d("BYI", "Timer interval set for: " + Integer.toString(i));

		t.schedule(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				// pass back the flag of 1 to get user location so it initiates
				// the timer again
				
				//TEST TO SEE IF KEEPING THE GPS ON IS BETTER
				initiateFragments("send");
				//getUserLocation(1);
			}
		}, i);
	}
	
	//Timer interval to launch task to pull location
	public void trackSentLocation(int i) {

		Timer t = new Timer();
		Log.d("BYI", "Timer interval set for: " + Integer.toString(i));

		t.schedule(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				// pass back the flag of 1 to get user location so it initiates
				// the timer again
				initiateFragments("retrieve");
			}
		}, i);
	}

	//Countdown timer for posting location
	public class TimerCount {
		CountDownTimer countDownTimer;

		// Background timer that will cause the trackLocation loop to end
		public void initializeTimer(int hourTime, int minuteTime) {
			
			shareLocationButton.setVisibility(View.GONE);
			trackLocationButton.setVisibility(View.GONE);
			endTrackingLocationButton.setVisibility(View.VISIBLE);
			
			int milisecondsToRun;
			milisecondsToRun = hourTime * 60 * 60 * 1000 + minuteTime * 60
					* 1000;
			countDownTimer = new CountDownTimer(milisecondsToRun, 1000) {
				
				@Override
				public void onFinish() {
					// TODO Auto-generated method stub
					// Since timer count > 2 in the onpostexecute method in the
					// tracking method, will cancel tracking
					timerCount = 5;
					keepCycling = false;
					Log.d("BYI", "Timer Finished");
					launchNotification(false);
					shareLocationButton.setVisibility(View.VISIBLE);
					trackLocationButton.setVisibility(View.VISIBLE);
					endTrackingLocationButton.setVisibility(View.GONE);

				}

				@Override
				public void onTick(long arg0) {
					// TODO Auto-generated method stub
					timerCount = 1;
					Log.d("BYI", "Timer Tick");

				}

			}.start();
		}

		public void endTimer() {
			keepCycling = false;
			countDownTimer.cancel();
			launchNotification(false);
			timerCount = 5;
			shareLocationButton.setVisibility(View.VISIBLE);
			trackLocationButton.setVisibility(View.VISIBLE);
			endTrackingLocationButton.setVisibility(View.GONE);
			gMap.clear();
		}

	}

	//Initializes the post to cloud stuff
	private void initiateFragments(String s) {

		/**
		 * FragmentTransaction fragmentTransaction =
		 * mFragmentManager.beginTransaction();
		 * 
		 * // Check to see if we have retained the fragment which handles //
		 * asynchronous backend calls mProcessingFragment =
		 * (CloudBackendFragment) mFragmentManager.
		 * findFragmentByTag(PROCESSING_FRAGMENT_TAG); // If not retained (or
		 * first time running), create a new one if (mProcessingFragment ==
		 * null) { mProcessingFragment = new CloudBackendFragment();
		 * mProcessingFragment.setRetainInstance(true);
		 * fragmentTransaction.add(mProcessingFragment,
		 * PROCESSING_FRAGMENT_TAG); }
		 **/

		mProcessingFragment = new CloudBackendFragment();
		mProcessingFragment.setRetainInstance(true);
		// cloud stuff
		mFragmentManager = getActivity().getFragmentManager();
		FragmentTransaction fragmentTransaction = mFragmentManager
				.beginTransaction();
		fragmentTransaction.add(mProcessingFragment, PROCESSING_FRAGMENT_TAG);
		// sendCloudData();

		if (s.equals("send")){
		PostToCloud pTC = new PostToCloud();
		pTC.execute();
		}
		else if(s.equals("checkID")){
			
		}
		else{ //retrieve		
		RetrieveFromCloud retrieveCloud = new RetrieveFromCloud();
		retrieveCloud.execute();
		
		}
	
		}
	
	//will check to see if an ID is created if not it will create a session ID
	public class CheckID extends AsyncTask<Void, Void, Void> {
		CloudBackend cb = new CloudBackend();
		CloudEntity newPost = new CloudEntity("Location");
		Boolean uniqueIDSet = true;
		ProgressDialog dialog;
		
		@Override
		protected void onPreExecute() {		
			dialog = ProgressDialog.show(getActivity(), "Creating your unique session ID", "Please wait...", true);
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub

			// Time is counted as GMT which is 8 hours ahead of PST
			

			/**
			 *randSessionID = mSharedPreferences.getString(RANDOM_SESSION_ID,"exist");
			if (!randSessionID.equals("exist")) {
				// saved preferences exists do nothing since we already have the
				// random session ID
				Log.d("BYI", "Retreived saved random session ID: "
						+ randSessionID);

			} else { **/
				
				Random r = new Random();
				
				randSessionID = "\"" + Integer.toString(r.nextInt(9000000)) + "\"";

				// Checks to see if we have an entity with that random session in the DB
				// id if so then in our on post execute we will redo this
				
				try{
				
				CloudQuery cq = new CloudQuery("Location");

				// filter and query the DB
				cq.setFilter(Filter.eq("session_id", randSessionID));
				cq.setLimit(10);
				List<CloudEntity> results = cb.list(cq);
	
				if (results.isEmpty()) {
					Log.d("BYI", "No entity with Session_ID = " + randSessionID);
					shareIDContinuousTrackingURL = "http://com.pulp.share/"+randSessionID.replaceAll("\"","");
					uniqueIDSet = true;
					
				} else {
					newPost = results.get(0);
					Log.d("BYI", "Rand Session ID Exists, must create another one");
					uniqueIDSet = false;
				}
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

				
				Log.d("BYI",
						"No saved pref exists, creating new rand session ID: "
								+ randSessionID + " session ID URL is: " + shareIDContinuousTrackingURL);

				Editor editor = mSharedPreferences.edit();
				editor.putString(RANDOM_SESSION_ID, randSessionID);
				editor.commit();	

			//}

			return null;
		}

		
		
		
		@Override
		protected void onPostExecute(Void v) {
			
			if(uniqueIDSet){
				Log.d("BYI", "SESSION ID FINE START TRACKING");
				dialog.dismiss();
				
			launchTrackingAttributes(shareIDContinuousTrackingURL);
			}
			else{
			Log.d("BYI", "MUST CREATE A NEW SESSION ID");
			CheckID checkID = new CheckID();
			checkID.execute();
			}
		}
		
		

	}

	//Posts location to cloud DB
	public class PostToCloud extends AsyncTask<Void, Void, Void> {
		CloudBackend cb = new CloudBackend();
		CloudEntity newPost = new CloudEntity("Location");

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub

			// Time is counted as GMT which is 8 hours ahead of PST

			// setID() doesnt work yet
			// newPost.setId("TEST_ID");
			// If there is a saved random number from saved prefs use it
			// else create a random number for the session ID

			// randSessionID = mSharedPreferences.getInt(RANDOM_SESSION_ID, -1);
			randSessionID = mSharedPreferences.getString(RANDOM_SESSION_ID,
					"exist");
			if (!randSessionID.equals("exist")) {
				// saved preferences exists do nothing since we already have the
				// random session ID
				Log.d("BYI", "Retreived saved random session ID: "
						+ randSessionID);

			} else {
				// no saved preferences exists - calculate new random one
				Random r = new Random();
				
				
				randSessionID = "\"" + Integer.toString(r.nextInt(1000000))
						+ "\"";
				//FOr testing use a static randSessionID
				//randSessionID = "test";
				
				Log.d("BYI",
						"No saved pref exists, creating new rand session ID: "
								+ randSessionID);

				Editor editor = mSharedPreferences.edit();
				editor.putString(RANDOM_SESSION_ID, randSessionID);
				editor.commit();

			}
			
			
			locationMessage = mSharedPreferences.getString(LOCATION_MESSAGE,
					"");
			

			try {

				/**
				 * 
				 * 
				 * This code will update an existing CloudQuery until google
				 * fixes the setID() feature... with setID working, we can set
				 * to a random sequence that we generate on this app when we
				 * update with the random sequence generated on this app, if it
				 * doesnt exist in the data base we will create new else if it
				 * exists we will update for now this is the only way to do it
				 * 
				 * also the basis of receiving updated locations
				 * */
				CloudQuery cq = new CloudQuery("Location");

				// filter and query the DB
				cq.setFilter(Filter.eq("session_id", randSessionID));
				cq.setLimit(10);
				List<CloudEntity> results = cb.list(cq);

				// Checks to see if we have an entity with that random session
				// id if so then
				// Get the entity and save as newPost2 for updating.
				
				
				if (results.isEmpty()) {
					Log.d("BYI", "No entity with Session_ID = " + randSessionID);
				} else {
					newPost = results.get(0);
					Log.d("BYI", "Got entity, Key is: "
							+ results.get(0).getId());
				}
				
				newPost.put("lat", lat);
				newPost.put("lon", lon);
				newPost.put("session_id", randSessionID);
				newPost.put("message", locationMessage);
				cb.update(newPost);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/**
			 * 
			 * THis code is to set up the continuous query CloudBackendAsync
			 * cloudBackEndAsync = new CloudBackendAsync(getActivity());
			 * cloudBackEndAsync.list(cloudQuery, new
			 * CloudCallbackHandler<List<CLoudEntity>>(){
			 * 
			 * public void onComplete(List<CloudEntity> results){ //here we
			 * could update the UI with results
			 * 
			 * } });
			 **/

			return null;
		}

		@Override
		protected void onPostExecute(Void v) {

			if (timerCount < 2) {
				Editor editor = mSharedPreferences.edit();
				editor.putString(RANDOM_SESSION_ID, randSessionID);
				editor.putInt(TIME_TO_TRACK, timerCount);
				trackLocation(trackingInterval);
			} else {
				Log.d("BYI", "Timer Done");
				timerCount = 0;
				keepCycling = false;
				// Clear saved timer count and random session ID
				Editor editor = mSharedPreferences.edit();
				editor.remove(RANDOM_SESSION_ID);
				editor.remove(TIME_TO_TRACK);
				editor.commit();

				DeleteCloudEntity deleteEntity = new DeleteCloudEntity();
				deleteEntity.execute(newPost);
			}
		}

	}

	
	//deletes the entry with locations after the timer has expired
	public class DeleteCloudEntity extends AsyncTask<CloudEntity, Void, Void> {

		CloudBackend cb2 = new CloudBackend();
		CloudEntity ce2 = new CloudEntity("location");

		@Override
		protected Void doInBackground(CloudEntity... arg0) {
			// TODO Auto-generated method stub

			ce2 = arg0[0];
			try {
				cb2.delete(ce2);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			Log.d("BYI", "Deleted Cloud Entity");
		}
	}
	
	//Method to retrieve location data from another user and place pin on map forced method does not use continuous query
	public class RetrieveFromCloud extends AsyncTask<Void, Void, Void> {
		CloudBackend cb = new CloudBackend();
		CloudEntity newPost = new CloudEntity("Location");
		LatLng objLatLon;

		@Override
		protected Void doInBackground(Void... params) {
				

			// Time is counted as GMT which is 8 hours ahead of PST
/**
 * Edited out which saves session IDs, will need later to store this in saved instance state
			
			randSessionID = mSharedPreferences.getString(RANDOM_SESSION_ID,
					"exist");
			if (!randSessionID.equals("exist")) {
				// saved preferences exists do nothing since we already have the
				// random session ID
				Log.d("BYI", "Retreived saved random session ID: "
						+ randSessionID);

			} else {
				// no saved preferences exists - calculate new random one
				Random r = new Random();
				randSessionID = "\"" + Integer.toString(r.nextInt(1000000))
						+ "\"";

				// randSessionID = "Hello";
				Log.d("BYI",
						"No saved pref exists, creating new rand session ID: "
								+ randSessionID);

				Editor editor = mSharedPreferences.edit();
				editor.putString(RANDOM_SESSION_ID, randSessionID);
				editor.commit();

			}
**/
			try {

				/**
				 * 
				 * 
				 * This code will update an existing CloudQuery until google
				 * fixes the setID() feature... with setID working, we can set
				 * to a random sequence that we generate on this app when we
				 * update with the random sequence generated on this app, if it
				 * doesnt exist in the data base we will create new else if it
				 * exists we will update for now this is the only way to do it
				 * 
				 * also the basis of receiving updated locations
				 * */
				CloudQuery cq = new CloudQuery("Location");
				//randSessionID = "test";
				// filter and query the DB
				cq.setFilter(Filter.eq("session_id", randSessionID));
				cq.setLimit(10);
				//cq.setScope(Scope.FUTURE_AND_PAST);
				//cq.setSubscriptionDurationSec(20);
				List<CloudEntity> results = cb.list(cq);

				// Checks to see if we have an entity with that random session
				// id if so then
				// Get the entity and save as newPost2 for updating.
				if (results.isEmpty()) {
					Log.d("BYI", "No entity to track with Session_ID = " + randSessionID);
					timerCount = 5;
				} else {
					newPost = results.get(0);
					Log.d("BYI", "Lat retreived from DB is :" + results.get(0).get("lat"));
					Log.d("BYI", "Lat retreived from DB is :" + results.get(0).get("lon"));
					Log.d("BYI", "Got entity, Key is: " + results.get(0).getId());
					timerCount = 1;
					
					lat = Double.parseDouble(results.get(0).get("lat").toString());
					lon = Double.parseDouble(results.get(0).get("lon").toString());
					locationMessage = results.get(0).get("message").toString();					
					objLatLon = new LatLng(lat, lon);
					
					
	
				}

				/**
				 * Old code to update the lat and lon of a cloud entity
				newPost.put("lat", lat);
				newPost.put("lon", lon);
				newPost.put("session_id", randSessionID);
				cb.update(newPost);
				 **/
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/**
			 * 
			 * THis code is to set up the continuous query CloudBackendAsync
			 * cloudBackEndAsync = new CloudBackendAsync(getActivity());
			 * cloudBackEndAsync.list(cloudQuery, new
			 * CloudCallbackHandler<List<CLoudEntity>>(){
			 * 
			 * public void onComplete(List<CloudEntity> results){ //here we
			 * could update the UI with results
			 * 
			 * } });
			 **/

			return null;
		}

		@Override
		protected void onPostExecute(Void v) {

			if (timerCount < 2) {
				if(objLatLon==null){
				//do nothing
				}else{
				moveMapCamera(objLatLon);
				}
				trackSentLocation(2500);
			} else {
				Log.d("BYI", "Timer Done");
				timerCount = 0;
				cycleCount = 0;
			}
			
		}

	}

	//Continuous query...doesnt work only returns once then never again
	public void queryLocations(){
		
		CloudBackend cb = new CloudBackend();
		CloudEntity newPost = new CloudEntity("Location");
		CloudBackendAsync cloudBackendAsync = new CloudBackendAsync(getActivity());
		randSessionID = "test";
		
		CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
            @Override
            public void onComplete(List<CloudEntity> results) {
            	if (results.isEmpty()) {
					Log.d("BYI", "No entity to track with Session_ID = " + randSessionID);
				} else {
					CloudEntity newPost = new CloudEntity("Location");
					newPost = results.get(0);
					Log.d("BYI", "Lat retreived from DB is :" + results.get(0).get("lat"));
					Log.d("BYI", "Got entity, Key is: "
							+ results.get(0).getId());
				}
            }
 
            @Override
            public void onError(IOException e) {
                
            }
        };
 
        CloudQuery cq = new CloudQuery("Location");
        cq.setLimit(50);
        cq.setFilter(Filter.eq("session_id", randSessionID));
        cq.setScope(Scope.FUTURE_AND_PAST);
        cq.setSubscriptionDurationSec(200000);
        cloudBackendAsync.list(cq,handler);
		
		
		
	}

}
