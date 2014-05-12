package com.pulp;

import java.util.List;

import com.pulp.R;
import com.pulp.R.id;
import com.pulp.R.layout;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;

/**
 * An activity representing a single Action detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ActionListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link ActionDetailFragment}.
 */
public class ShareOptionLauncherShell extends FragmentActivity {
	
	//Key for what fragment to launch
	public static final String SHARE_ID = "TEST";
	public final String debugTag = "BYI";
	public static Boolean venueSelected;
	
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_detail);
        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
       
        venueSelected = false;
      
        
        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
		if (savedInstanceState == null) {
			String shareOptionSelected;	
			
			Bundle extras = getIntent().getExtras();		
			if(!(extras == null)){
			shareOptionSelected = extras.getString("ShareOptionChosen");
			venueSelected = extras.getBoolean("VenueSelected");
			}	else{
				shareOptionSelected = "Location";
			}
			
			
			if(MainActivity.fromMainActivity){			
			// Grab the Share ID passed from the ShareOptionListFragment.class  if it is null (from the URL) set default to RetrieveLocation
			extras = getIntent().getExtras();		
			if(!(extras == null)){
			shareOptionSelected = extras.getString("ShareOptionChosen");						
			}
			else
			{shareOptionSelected = "Location";}
			if(shareOptionSelected == null){
				shareOptionSelected = "RetrieveLocation";
			}
			}else if(venueSelected) 
			{		
				shareOptionSelected = "Status";
			}
			else if(true) {
				shareOptionSelected = "RetrieveLocation";
			}
			
			shareOptionSwitcher(shareOptionSelected);
		
			//shareOptionSwitcher("Status");
		}

	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpTo(this, new Intent(this, MainActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    
    public void shareOptionSwitcher(String option){
    	
    	if (option.equals("Status")) {
			ShareStatusUpdateFragment fragment = new ShareStatusUpdateFragment();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.action_detail_container, fragment).commit();
		}


		else if (option.equals("Picture")) {

			Log.d(debugTag, option);

		}
    	
		else if (option.equals("Internet")) {
			
			ShareInternetFragment fragment = new ShareInternetFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.action_detail_container, fragment).commit();
			
		}
		else if (option.equals("Location")) {
			
			ShareLocationFragment fragment = new ShareLocationFragment();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.action_detail_container, fragment).commit();

		}
    	
		else if (option.equals("FoursquareVenueList")){
			
			Log.d("BYI", "FoursquareVenueList Fragment Should Launch here");
			
			FoursquareVenueListFragment fragment = new FoursquareVenueListFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.action_detail_container, fragment).commit();

		}
    	
		else if (option.equals("RetrieveLocation")) {
			
			Log.d("BYI", "Retrieve Location");
			Uri data = getIntent().getData();
			String scheme = data.getScheme();
			String host = data.getHost();
			List<String> params = data.getPathSegments();
			String uniqueKey = params.get(0);
			
			Log.d("BYI", uniqueKey);
			
			Bundle bundle = new Bundle();
			bundle.putString("uniqueKey", uniqueKey);

			ShareLocationFragment fragment = new ShareLocationFragment();
			fragment.setArguments(bundle);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.action_detail_container, fragment).commit();

		}
    	
    }
    
    
    
    
    
}
