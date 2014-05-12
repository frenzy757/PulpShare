package com.pulp;

import java.util.ArrayList;

import net.londatiga.fsq.FsqVenue;
import net.londatiga.fsq.NearbyAdapter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.pulp.R;
import com.pulp.R.id;
import com.pulp.R.layout;

public class FoursquareVenueListFragment extends Fragment {
	
	private View rootView;
	private ArrayList<FsqVenue> fourSquareVenueArray;
	private NearbyAdapter myNearbyAdapter;
	private ListView myListView;
	
	
	
    public FoursquareVenueListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
	
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
	
    	rootView = inflater.inflate(R.layout.foursquare_venue_list, container, false);
    	myNearbyAdapter = new NearbyAdapter(getActivity());
    	
    	fourSquareVenueArray = new ArrayList<FsqVenue>();
    	fourSquareVenueArray = ShareStatusUpdateFragment.getList();
    	
    	myListView = (ListView) rootView.findViewById(R.id.foursquare_venue_listview);
    	
    	ShareStatusUpdateFragment.foursquareVenueClickedID = null;
    	
    	
    	
    	
    	
    	myNearbyAdapter.setData(fourSquareVenueArray);
    	myListView.setAdapter(myNearbyAdapter);
    	
    	
    	
    	myListView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				Log.d("BYI", "ListClicked Index: " + arg2);
				Log.d("BYI", "Foursquare Venue Clicked: " + fourSquareVenueArray.get(arg2).name);
				ShareStatusUpdateFragment.foursquareVenueClickedID = fourSquareVenueArray.get(arg2).id;
				ShareStatusUpdateFragment.foursquareVenueClickedName= fourSquareVenueArray.get(arg2).name; 
				
				Intent i = new Intent(getActivity(), ShareOptionLauncherShell.class);
                i.putExtra("ShareOptionChosen", "Status");
                i.putExtra("VenueSelected", true);
                startActivity(i);
				
			}
    		
    	});
    	
    	
    	
    	return rootView;
    	
    	
    }
    
    
    
    
    
	
}
