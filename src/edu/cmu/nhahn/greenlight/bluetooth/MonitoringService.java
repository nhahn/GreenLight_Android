package edu.cmu.nhahn.greenlight.bluetooth;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.MonitorNotifier;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

import edu.cmu.nhahn.greenlight.R;
import edu.cmu.nhahn.greenlight.RoomDetailFragment;
import edu.cmu.nhahn.greenlight.RoomListActivity;

public class MonitoringService extends Service implements IBeaconConsumer{
	
    protected static final String TAG = "RangingActivity";
    private static boolean running = false;
	private IBeaconManager iBeaconManager;
    IBinder mBinder;      // interface for clients that bind
    private Region region;
    private HashMap<Integer,Integer> curNotifications;
    private static int counter;
    
    @Override
    public void onCreate() {
    	SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
    	    	
    	if (sharedPrefs.getBoolean("prefLocateGreenlight", false) || running)
    		stopSelf();
    	else
    	{
    		iBeaconManager = IBeaconManager.getInstanceForApplication(this);
        	region = new Region("greenlightbeacon", getString(R.string.proximityuuid), null, null);
    		iBeaconManager.bind(this);
    		curNotifications = new HashMap<Integer,Integer>();
    		counter = 0;
    	}
     }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        running = true;
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return mBinder;
    }

    @Override
    public void onDestroy() {
        try {
        	if (region != null)
        		iBeaconManager.stopMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {   }
        iBeaconManager.unBind(this);
        running = false;
    }

	@Override
	public void onIBeaconServiceConnect() {
        iBeaconManager.setMonitorNotifier(new MonitorNotifier() {
        @Override
        public void didEnterRegion(Region region) {
        }

        @Override
        public void didExitRegion(Region region) {
        }

        @Override
        public void didDetermineStateForRegion(int state, Region region) {  
        	if (state == MonitorNotifier.OUTSIDE) {
        		Log.v("bluetooth", "I no longer see" + region.getMinor());
        		NotificationManager mNotificationManager =
        				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        		for (Entry<Integer,Integer> e : curNotifications.entrySet())
        			mNotificationManager.cancel(e.getValue());
        	}
        }


        });
        
        iBeaconManager.setRangeNotifier(new RangeNotifier() {
            @Override 
            public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) { 
            	HashMap<Integer,Integer> newHash = new HashMap<Integer,Integer>();
            	
            	for(IBeacon beacon : iBeacons) {
            		if(!curNotifications.containsKey(beacon.getMinor())) {
            			NotificationCompat.Builder mBuilder = 
            					new NotificationCompat.Builder(getApplicationContext())
            			.setSmallIcon(R.drawable.ic_launcher)
            			.setContentTitle("Nearby GreenLight Switch")
            			.setContentText("");

            			Intent intent = new Intent(getApplicationContext(),RoomListActivity.class);
            			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            			intent.putExtra(RoomDetailFragment.ARG_ROOM_ID, "" + beacon.getMinor());

            			PendingIntent resultPendingIntent =
            					PendingIntent.getActivity(getApplicationContext(), 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            			mBuilder.setContentIntent(resultPendingIntent);

            			NotificationManager mNotificationManager =
            					(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            			mNotificationManager.notify(++counter, mBuilder.build());  
            			newHash.put(beacon.getMinor(), counter);
            		} else
            			newHash.put(beacon.getMinor(), curNotifications.get(beacon.getMinor()));
            	}
            	
            	curNotifications = newHash;
            }
            
        });

        try {
            iBeaconManager.startMonitoringBeaconsInRegion(region);
            iBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {   }
		
	}

}
