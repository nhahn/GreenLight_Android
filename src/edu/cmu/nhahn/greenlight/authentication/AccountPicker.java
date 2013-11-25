package edu.cmu.nhahn.greenlight.authentication;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import edu.cmu.nhahn.greenlight.R;

public class AccountPicker extends ListActivity {

	private AccountManager mAccountManager;
	private Account[] accounts;
	private Context mContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.acount_picker);
		mContext = this.getApplicationContext();
	    mAccountManager = AccountManager.get(this);
	    

		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		String[] accounts = getAccountNames();
		if (accounts.length < 1)
		{
			mAccountManager.addAccount(GreenlightAuthenticator.ACCOUNT_TYPE, 
					LogonManager.PARAM_AUTHTOKEN_TYPE, 
					null, null, this, new OnTokenAcquired(), null);
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String> (this,
				android.R.layout.simple_list_item_1, accounts);
		setListAdapter(adapter);
	}
	
	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		Account account = accounts[position];
		Bundle options = new Bundle();
		mAccountManager.getAuthToken(account, 
				LogonManager.PARAM_AUTHTOKEN_TYPE, 
				options, 
				(Activity) this, 
				(AccountManagerCallback<Bundle>)new OnTokenAcquired(), null);
	}

	
	private String[] getAccountNames() {
	    accounts = mAccountManager.getAccountsByType(
	            GreenlightAuthenticator.ACCOUNT_TYPE);
	    String[] names = new String[accounts.length];
	    for (int i = 0; i < names.length; i++) {
	        names[i] = accounts[i].name;
	    }
	    return names;
	}
	
	private class OnTokenAcquired implements AccountManagerCallback<Bundle> {

		@Override
		public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    // User input required
                    mContext.startActivity(intent);
                } else {
                   ((LoginFilter) getApplicationContext()).setToken(bundle.getString(AccountManager.KEY_AUTHTOKEN));
                   ((LoginFilter) getApplicationContext()).setAccount(bundle.getString(AccountManager.KEY_ACCOUNT_NAME));
                   synchronized(getApplicationContext())
                   {
                	   getApplicationContext().notify();
                   }
                   finish();
                }
            } catch (OperationCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }			
	}
		
}

