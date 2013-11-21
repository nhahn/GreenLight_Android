package edu.cmu.nhahn.greenlight.contentprovider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class RailsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -953812713049292758L;
	private JSONObject response;

	public RailsException(JSONObject server) throws JSONException
	{
		super(server.getString("message"));
		this.response = server;
	}
	
	public static boolean validateError(JSONObject response)
	{
		try {
			return !response.getBoolean("validated");
		} catch(JSONException e)
		{
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map<String,String> validationErrors()
	{
		HashMap<String,String> map = new HashMap<String,String>();
		if (validateError(this.response))
			return map;
		
		try {
			JSONObject validations = response.getJSONObject("validations");
			Iterator<String> it = validations.keys();
			while(it.hasNext())
			{
				String key = it.next();
				map.put(key, validations.getString(key));
			}
		} catch (JSONException e) {
			//TODO log this
		}
		return map;
	}
}
