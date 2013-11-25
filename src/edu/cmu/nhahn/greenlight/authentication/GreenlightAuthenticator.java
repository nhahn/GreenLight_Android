package edu.cmu.nhahn.greenlight.authentication;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

public class GreenlightAuthenticator extends AbstractAccountAuthenticator {

	private Context context;
	public final static String ACCOUNT_TYPE = "edu.cmu.nhahn.greenlight.authentication.Greenlight_Account";
    public static final String PARAM_CREATE = "create";  
    public static final String PARAM_CONFIRM = "confirm";  


	
	public GreenlightAuthenticator(Context context) {
		super(context);
		this.context = context;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response,
			String accountType, String authTokenType,
			String[] requiredFeatures, Bundle options)
			throws NetworkErrorException {
		final Bundle result;
		final Intent intent;
		
		intent = new Intent(this.context, LogonManager.class);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
		intent.putExtra(GreenlightAuthenticator.ACCOUNT_TYPE, accountType);
		intent.putExtra(GreenlightAuthenticator.PARAM_CREATE, true);
		
		result = new Bundle();
		result.putParcelable(AccountManager.KEY_INTENT, intent);
		return result;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response,
			Account account, Bundle options) throws NetworkErrorException {
		if(options != null && options.containsKey(AccountManager.KEY_PASSWORD)) {
			String password = options.getString(AccountManager.KEY_PASSWORD);
			try {
				Bundle auth = LogonManager.authenticate(account.name, password, ACCOUNT_TYPE, null);
				String authToken = auth.getString(AccountManager.KEY_AUTHTOKEN);
				if(!TextUtils.isEmpty(authToken)) {
					Bundle result = new Bundle();
					result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
					final AccountManager am = AccountManager.get(context);
					am.setAuthToken(account, LogonManager.PARAM_AUTHTOKEN_TYPE, authToken);
					return result;
				}
			} catch (Exception e) {
				throw new NetworkErrorException();
			}
		}

		Intent intent = new Intent(this.context, LogonManager.class);
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
		intent.putExtra(PARAM_CONFIRM, true);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response,
			String accountType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {
		
		final AccountManager am = AccountManager.get(context);
		
		String authToken = am.peekAuthToken(account, authTokenType);
		if(TextUtils.isEmpty(authToken)) {
			final String password = am.getPassword(account);
			if (password != null) {
				try {
					Bundle auth = LogonManager.authenticate(account.name, password, authTokenType, null);
					authToken = auth.getString(AccountManager.KEY_AUTHTOKEN);
				} catch (Exception e) {
					throw new NetworkErrorException();
				}
			}
		}
		
	    // If we get an authToken - we return it
	    if (!TextUtils.isEmpty(authToken)) {
	        final Bundle result = new Bundle();
	        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
	        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
	        result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
	        return result;
	    }
	    
	    final Intent intent = new Intent(context, LogonManager.class);
	    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
	    intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);
	    intent.putExtra(GreenlightAuthenticator.ACCOUNT_TYPE, authTokenType);
	    final Bundle bundle = new Bundle();
	    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
	    return bundle;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
        if(ACCOUNT_TYPE.equals(authTokenType)) {
            return "Greenlight";
        }
        return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response,
			Account account, String[] features) throws NetworkErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle options)
			throws NetworkErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) {
	    Bundle result = new Bundle();
	    boolean allowed = true; // or whatever logic you want here
	    result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, allowed);
	    return result;
	}
	

}
