package com.pulp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

public class ShareInternetFragment extends Fragment{
	
	public static final String debugTag = "BYI";
	private View rootView;
	Switch shareInternetSwitch;
	public static SharedPreferences mSharedPreferencesInternet;
	private static final String WIFI_ON = "wifi status before hotspot creation";
	private static final String HOTSPOT_NAME = "hotspot name";
	private static final String HOTSPOT_PASSWORD = "password";
	EditText hotspotName;
	EditText hotspotPassword; 
	
	public ShareInternetFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
	
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	rootView = inflater.inflate(R.layout.share_internet_main, container, false);
    	shareInternetSwitch = (Switch) rootView.findViewById(R.id.share_internet_switch);
    	addShareInternetSwitch();
    	mSharedPreferencesInternet = PreferenceManager.getDefaultSharedPreferences(getActivity());
    	
    	String  savedName = mSharedPreferencesInternet.getString(HOTSPOT_NAME, null);
    	String  savedPW = mSharedPreferencesInternet.getString(HOTSPOT_PASSWORD, null);
    	
    	hotspotName = (EditText) rootView.findViewById(R.id.hotspot_name);
    	hotspotName.setText(savedName);
    	hotspotPassword = (EditText) rootView.findViewById(R.id.hotspot_password);
    	hotspotPassword.setText(savedPW);
    	
    	getActivity().getActionBar().setTitle("Share My Internet");
    	
    	WifiManager wifi = (WifiManager) getActivity().getSystemService(getActivity().WIFI_SERVICE);
    	Boolean hotspotOn;
		try
	    {
	        final Method method = wifi.getClass().getDeclaredMethod("isWifiApEnabled");
	        method.setAccessible(true); //in the case of visibility change in future APIs
	        hotspotOn = (Boolean) method.invoke(wifi);
	        shareInternetSwitch.setChecked(hotspotOn);
	    }
	    catch (final Throwable ignored)
	    {
	    }
    	
	
    	return rootView;
    	
    	
    }
    
    public void addShareInternetSwitch(){
    	
    	
    	shareInternetSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){
    		
    	
    		
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				
		
				String name = hotspotName.getText().toString();
				String pw = hotspotPassword.getText().toString();
				
				
				
				
				
				WifiManager wifi = (WifiManager) getActivity().getSystemService(getActivity().WIFI_SERVICE);
				//WifiConfiguration wifi_configuration = null;
				WifiConfiguration wifi_configuration = new WifiConfiguration();
				wifi_configuration.SSID = name;
				wifi_configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
				wifi_configuration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
				wifi_configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				//wifi_configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				wifi_configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
				wifi_configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
				wifi_configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
				wifi_configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
				wifi_configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
				wifi_configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
				
				//for some reason this doesn't work
				//wifi_configuration.wepKeys[0] = "\"" + pw + "\"";
				//wifi_configuration.wepKeys[0] = "\"aaabbb1234\"";
				//wifi_configuration.wepTxKeyIndex = 0;
				
				//adds password
				wifi_configuration.preSharedKey = pw;
				
				
				
				
				
				
				
				
				
				// TODO Auto-generated method stub
				if(isChecked){
					Log.d("BYI", "HOTSPOT SWITCH TRUE");
			
					
					if (pw.length() < 8){
						
						Toast.makeText(getActivity(), "PASSWORD MUST BE AT LEAST 8 CHARACTERS LONG", Toast.LENGTH_LONG).show();
						shareInternetSwitch.setChecked(false);
						
					}
					
					else{//password is 8 or more characters
					
					Editor editor = mSharedPreferencesInternet.edit();
					editor.putBoolean(WIFI_ON, wifi.isWifiEnabled());
					editor.putString(HOTSPOT_NAME, name);
					editor.putString(HOTSPOT_PASSWORD, pw);
					editor.commit();
					
					
					
					
			    	wifi.setWifiEnabled(false);
			    	
			    	try 
			        {
			           //USE REFLECTION TO GET METHOD "SetWifiAPEnabled"
			           Method method =wifi.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
			           method.invoke(wifi, wifi_configuration, true);
			        } 
			        catch (NoSuchMethodException e) 
			        {
			           // TODO Auto-generated catch block
			           e.printStackTrace();
			        } 
			        catch (IllegalArgumentException e) 
			        {
			           // TODO Auto-generated catch block
			           e.printStackTrace();
			        } 
			        catch (IllegalAccessException e) 
			        {
			           // TODO Auto-generated catch block
			           e.printStackTrace();
			        } 
			        catch (InvocationTargetException e) 
			        {
			           // TODO Auto-generated catch block
			           e.printStackTrace();
			        }
			    	
				
					}//for the if else statement to check password length
				}
				else {
					Log.d("BYI", "HOTSPOT SWITCH FALSE");
				
				  	try 
			        {
			           //USE REFLECTION TO GET METHOD "SetWifiAPEnabled"
			           Method method =wifi.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
			           method.invoke(wifi, wifi_configuration, false);
			           
			           boolean  wifi_on_before_hotspot = mSharedPreferencesInternet.getBoolean(WIFI_ON, true);
			           
			           
			           wifi.setWifiEnabled(wifi_on_before_hotspot);
			           
			           
			        } 
			        catch (NoSuchMethodException e) 
			        {
			           // TODO Auto-generated catch block
			           e.printStackTrace();
			        } 
			        catch (IllegalArgumentException e) 
			        {
			           // TODO Auto-generated catch block
			           e.printStackTrace();
			        } 
			        catch (IllegalAccessException e) 
			        {
			           // TODO Auto-generated catch block
			           e.printStackTrace();
			        } 
			        catch (InvocationTargetException e) 
			        {
			           // TODO Auto-generated catch block
			           e.printStackTrace();
			        }
				  	
				  	
					
					
				}
				
			
			
				
			}
    		
    	});
    	
    	
    	
    }
    
    
    @Override
    public void onResume() {
    	// TODO Auto-generated method stub
    	WifiManager wifi = (WifiManager) getActivity().getSystemService(getActivity().WIFI_SERVICE);
    	Boolean hotspotOn;
		try
	    {
	        final Method method = wifi.getClass().getDeclaredMethod("isWifiApEnabled");
	        method.setAccessible(true); //in the case of visibility change in future APIs
	        hotspotOn = (Boolean) method.invoke(wifi);
	        shareInternetSwitch.setChecked(hotspotOn);
	    }
	    catch (final Throwable ignored)
	    {
	    }
    	
    	super.onResume();
    }
    
    
}
