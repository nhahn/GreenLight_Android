package edu.cmu.nhahn.greenlight.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MonitoringStartupReciever extends BroadcastReceiver {

	public MonitoringStartupReciever() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		// TODO Auto-generated method stub
		
		Intent myIntent = new Intent(arg0, MonitoringService.class);
		arg0.startService(myIntent);
	}

}
