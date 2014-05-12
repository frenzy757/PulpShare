package com.pulp;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import com.pulp.R;
import com.pulp.R.id;
import com.pulp.R.layout;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.google.cloud.backend.core.CloudBackend;
import com.google.cloud.backend.core.CloudBackendFragment;
import com.google.cloud.backend.core.CloudCallbackHandler;
import com.google.cloud.backend.core.CloudEntity;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
//import android.support.v4.widget.SearchViewCompatIcs.MySearchView;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;


/**
 * An activity representing a list of Actions. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ActionDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ActionListFragment} and the item details
 * (if present) is a {@link ActionDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link ActionListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class MainActivity extends FragmentActivity
        implements ShareOptionListFragment.Callbacks {

    /**
     * mTwoPane is the variable that determines whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    public static boolean mTwoPane;
    public static final String debugTag = "BYI";
    public static boolean fromMainActivity;
    
    
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_list);
        
     
      
        
        
        fromMainActivity = true;
        

        if (findViewById(R.id.action_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ShareOptionListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.action_list))
                    .setActivateOnItemClick(true);
        }
        


            try {
                PackageInfo info = getPackageManager().getPackageInfo(
                        "com.facebook.samples.loginhowto", 
                        PackageManager.GET_SIGNATURES);
                for (Signature signature : info.signatures) {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    Log.d("BYI", Base64.encodeToString(md.digest(), Base64.DEFAULT));
                    }
            } catch (NameNotFoundException e) {

            } catch (NoSuchAlgorithmException e) {}

            
       
        
    	
        
        
    }

    /**
     * right now it is not used and the logic for launching a new activity resides in the ShareOptionListFragment class
     * Callback method from {@link ShareOptionListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     
    */
    
    
    @Override
    public void onItemSelected(String id) {
      
    	/** comment out for testing
    	 * 
    	if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ActionDetailFragment.ARG_ITEM_ID, id);
            ActionDetailFragment fragment = new ActionDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.action_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ShareOptionLauncherActivity.class);
            //detailIntent.putExtra(ActionDetailFragment.ARG_ITEM_ID, id);
            detailIntent.putExtra(ActionDetailFragment.ARG_ITEM_ID, id);
            //Toast.makeText(this, "ID", Toast.LENGTH_LONG).show();
            Log.d(debugTag, ActionDetailFragment.ARG_ITEM_ID);
            Log.d(debugTag, id);
            startActivity(detailIntent);
        }
        
        */
    }
    
   
    
    
    // Callback test method
    public void testMethod(){
    	Log.d(debugTag, "Test Method Works");	
    }
    
    
   
    
    
    
    
   
   
 
    
    
    
}
