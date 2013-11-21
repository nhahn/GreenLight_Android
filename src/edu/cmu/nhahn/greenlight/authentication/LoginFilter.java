package edu.cmu.nhahn.greenlight.authentication;

import android.app.Application;
import android.content.Intent;
import android.text.TextUtils;

public class LoginFilter extends Application {

	private String token;
	private String account;

	public LoginFilter() {
	}

	public String getToken() {
		if(TextUtils.isEmpty(token))
		{
			Intent intent = new Intent(this.getApplicationContext(),AccountPicker.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(intent);
			return token;
		} else 
			return token;
	}
	
	public String getAccount(){
		return account;
	}
	
	protected void setToken(String token) {
		this.token = token;
	}
	
	protected void setAccount(String account) {
		this.account = account;
	}
	
	//TODO implement this if we have a token error
	public boolean refreshToken() {
		return false;
	}
	
}


