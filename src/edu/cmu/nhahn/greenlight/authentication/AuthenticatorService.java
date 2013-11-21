package edu.cmu.nhahn.greenlight.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticatorService extends Service {

	 @SuppressWarnings("unused")
	private static final String TAG = "AccountAuthenticatorService";
	 private static GreenlightAuthenticator sAccountAuthenticator = null;
	
	public AuthenticatorService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IBinder onBind(Intent intent) {
		IBinder ret = null;
		if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
			ret = getAuthenticator().getIBinder();
		return ret;
	}

	private GreenlightAuthenticator getAuthenticator() {
		if (sAccountAuthenticator == null)
			sAccountAuthenticator = new GreenlightAuthenticator(this);
		return sAccountAuthenticator;
	}

}
