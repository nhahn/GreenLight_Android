package edu.cmu.nhahn.greenlight.bluetooth;

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

import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.MonitorNotifier;
import com.radiusnetworks.ibeacon.Region;

import edu.cmu.nhahn.greenlight.R;
import edu.cmu.nhahn.greenlight.RoomDetailFragment;
import edu.cmu.nhahn.greenlight.RoomListActivity;

public class MonitoringService extends Service implements IBeaconConsumer{
	
    protected static final String TAG = "RangingActivity";
    private static boolean running = false;
    private int mNotificationID = 123;
	private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);
    IBinder mBinder;      // interface for clients that bind
    private Region region;
    
    @Override
    public void onCreate() {
    	SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
    	    	
    	if (!sharedPrefs.getBoolean("prefLocateGreenlight", false) || running)
    		stopSelf();
    	else
    	{
        	region = new Region("greenlightbeacon", getString(R.string.proximityuuid), null, null);
    		iBeaconManager.bind(this);
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
        	
          NotificationCompat.Builder mBuilder = 
        		  new NotificationCompat.Builder(getApplicationContext())
          		  .setSmallIcon(R.drawable.ic_launcher)
          		  .setContentTitle("Nearby GreenLight Switch")
          		  .setContentText("");
        	
          Intent intent = new Intent(getApplicationContext(),RoomListActivity.class);
          intent.putExtra(RoomDetailFragment.ARG_ROOM_ID, region.getMinor());
          
          PendingIntent resultPendingIntent =
        		  PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
          mBuilder.setContentIntent(resultPendingIntent);
          
          NotificationManager mNotificationManager =
        		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
          
          mNotificationManager.notify(mNotificationID, mBuilder.build());
        }

        @Override
        public void didExitRegion(Region region) {
                Log.v("bluetooth", "I no longer see" + region.getMinor());
                NotificationManager mNotificationManager =
            		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                
                mNotificationManager.cancel(mNotificationID);
        }

        @Override
        public void didDetermineStateForRegion(int state, Region region) {     
        }


        });

        try {
            iBeaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {   }
		
	}

}
