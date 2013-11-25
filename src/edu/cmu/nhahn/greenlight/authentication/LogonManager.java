package edu.cmu.nhahn.greenlight.authentication;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import edu.cmu.nhahn.greenlight.R;
import edu.cmu.nhahn.greenlight.contentprovider.RailsProvider;
import edu.cmu.nhahn.greenlight.contentprovider.RailsUtils;

public class LogonManager extends AccountAuthenticatorActivity {

    public static final String PARAM_AUTHTOKEN_TYPE = "account";  
    public static final String PARAM_USER_PASS = "password";
	private AccountManager accMgr;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logon_manager);  
		Log.v("authenticator", "got to add account");

    	accMgr = AccountManager.get(getApplicationContext());  

    }
    
    public void onSubmitClick(final View v){
    	final EditText username = ((EditText) findViewById(R.id.username));
    	final EditText password = ((EditText) findViewById(R.id.password));
        new AsyncTask<Void, Void, Intent>() {
            @Override
            protected Intent doInBackground(Void... params) {
                Bundle auth;
                final Intent res = new Intent();
				try {
					auth = authenticate(username.getText().toString(), password.getText().toString(), "", v);
				} catch (Exception e) {
					return null;
				}
                if (auth.getString(AccountManager.KEY_AUTHTOKEN) == null)
                	return res;
                
                res.putExtra(AccountManager.KEY_ACCOUNT_NAME, username.getText().toString());
                res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, GreenlightAuthenticator.ACCOUNT_TYPE);
                res.putExtra(AccountManager.KEY_AUTHTOKEN, auth.getString(AccountManager.KEY_AUTHTOKEN));
                res.putExtra(PARAM_USER_PASS, password.getText().toString());
                return res;
            }
            @Override
            protected void onPostExecute(Intent intent) {
            	if(intent == null)
            	{
            		RailsUtils.networkError(LogonManager.this,  new DialogInterface.OnClickListener() {
            			@Override
            			public void onClick(DialogInterface dialog, int which) {
            				onSubmitClick(v);
            			}
            		});
            	} else if (!intent.hasExtra(AccountManager.KEY_AUTHTOKEN))
            	{
            		TextView message = (TextView) findViewById(R.id.login_message);
    				message.setText("Invalid username or password");
            		return;
            	}
            	else 
            		finishLogin(intent);
            }
        }.execute();
    }
    
    private void finishLogin(Intent intent) {
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
        if (getIntent().getBooleanExtra(GreenlightAuthenticator.PARAM_CREATE, false)) {
        	
            String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
        	String accountType = this.getIntent().getStringExtra(PARAM_AUTHTOKEN_TYPE);  
        	if (accountType == null) {  
        		accountType = GreenlightAuthenticator.ACCOUNT_TYPE;  
        	}
        	
            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
        	accMgr.addAccountExplicitly(account, accountPassword, null);
        	accMgr.setAuthToken(account, PARAM_AUTHTOKEN_TYPE, authtoken);
        } else {
        	accMgr.setPassword(account, accountPassword);
        }
        
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        	//TODO Disable the preference
        }
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }
        
    public static Bundle authenticate(String username, String password, String authTokenType, View activity) throws JSONException, ClientProtocolException, IOException
    {
    	JSONObject user = new JSONObject();
    	JSONObject info = new JSONObject();
    	Bundle bundle = new Bundle();
    	info.put("password", password);
    	info.put("email", username);
    	user.put("user", info);
    	JSONObject retVal = RailsUtils.postRequest(RailsProvider.root, "/api/v1/sessions", user.toString()).optJSONObject(0);
    	if (retVal.has("success"))
    		bundle.putString(AccountManager.KEY_AUTHTOKEN, retVal.getJSONObject("data").getString("auth_token"));

    	return bundle;
    }

}

