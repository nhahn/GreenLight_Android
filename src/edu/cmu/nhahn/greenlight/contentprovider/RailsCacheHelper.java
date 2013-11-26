package edu.cmu.nhahn.greenlight.contentprovider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.nhahn.greenlight.authentication.LoginFilter;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class RailsCacheHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "railscache.db";
	//Map of query strings to model names
	private Map<String,RailsCacheEntry> queryCache;
	
	
	public RailsCacheHelper(Context context,final String root) throws JSONException, IOException, ClientProtocolException {
		super(context, DATABASE_NAME, null, version(root).getInt("version"));
		queryCache = ((LoginFilter) context).getCache();

	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		RailsCacheTable.onCreate(db);
	}
	//TODO when model is first loaded -- check if a table exists. If there
	//has been a change -- wipe it out and create a table
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		RailsCacheTable.onUpgrade(db, oldVersion, newVersion);
	}
	
	/**
	 * Get our rails database version
	 * @param root
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 */
	private static JSONObject version(String root) throws ClientProtocolException, IOException, JSONException
	{
		String response = RailsUtils.getRequest(root,"api/v1/version.json",null);
        return new JSONObject(response);
	}
	
	/**
	 * Return an array of models in our rails server
	 * @param root the root url of the rails application
	 * @return a JSONArray with version information
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONArray uris(String root) throws ClientProtocolException, IOException, JSONException
	{
        return new JSONArray(RailsUtils.getRequest(root,"api/v1/uris.json", null));
	}
	
	/**
	 *  Parse the ContentProvider query so our rails app can process it
	 * @param model
	 * @param query a rails-style relation query (with scopes,etc). Variables are replaced with ? 
	 * 		  and then placed in the selectionArgs array
	 * @param selectionArgs an array that corresponds to the ?s in the query (in order)
	 * @return a JSON string with the encoded information
	 * @throws JSONException
	 */
	public static JSONObject composeQuery(String model,String query, String[] selectionArgs) throws JSONException
	{
		if (selectionArgs == null)
			selectionArgs = new String[0];
		
		JSONObject json = new JSONObject();
		json.put("class",model);
		
		int selectionCounter = 0;
		String[] queryArr = query.split("\\.");
		json.put("include", new JSONArray()); 

		JSONArray scopes = new JSONArray();
		for (int i=0;i<queryArr.length;i++)
		{
			JSONObject scopeObj = new JSONObject();
			String scope = queryArr[i];
			String[] scopeArr = scope.split("\\(");
		    scopeObj.put("scope", scopeArr[0]);
			if (scopeArr.length > 1)
			{
				String params = scopeArr[1].substring(0,scopeArr[1].length()-1);
				//TODO finish this 
				if (scopeArr[0].equals("includes")) {
					JSONArray paramsArr = new JSONArray();
					for(char j : params.toCharArray())
					{
						if(j == '?')
						{	
							JSONObject param = new JSONObject();
							String[] selectionSplit = selectionArgs[selectionCounter].split(":");
							param.put("type", selectionSplit[0]);
							param.put("val", selectionSplit[1]);
							paramsArr.put(param);
							selectionCounter++;
						}
					}
					json.put("include", paramsArr);
				} else {
					JSONArray paramsArr = new JSONArray();
					for(char j : params.toCharArray())
					{
						if(j == '?')
						{	
							JSONObject param = new JSONObject();
							String[] selectionSplit = selectionArgs[selectionCounter].split(":",2);
							param.put("type", selectionSplit[0]);
							param.put("val", selectionSplit[1]);
							paramsArr.put(param);
							selectionCounter++;
						}
					}
					scopeObj.put("params", paramsArr);
				}
			}
			if(!scopeArr[0].equals("includes"))
				scopes.put(scopeObj);
		}
		if(scopes.length() < 1)
		{
			JSONObject scopeObj = new JSONObject();
		    scopeObj.put("scope", "all");
		    scopes.put(scopeObj);
		}
		json.put("scopes",scopes);
		return json;
	}
	
	//We are only going to support non-nested ones for now
	//What we have coming in is [] or just commas
	//Room.includes(:room_dimmers).to_json({include: :room_dimmers}) <= this
	@SuppressWarnings("unused")
	private static JSONArray setupEagerLoading(String string) throws JSONException {
		//Might need this in the future
		//String replacement = string.replaceAll("(:?[a-zA-Z_]+)", "\"$1\"");
		String parsed = string.replace("[", "").replace("]","");
		String[] loadAso = parsed.split(",");
		JSONArray arr = new JSONArray();
		for(String aso : loadAso)
		{
			aso = aso.trim().replace(":", "");
			//Maybe perform a check here
			arr.put(aso);
		}
		return arr;
	}	

	/**
	 * Checks if there are any updates in the database for a particular model. 
	 * @param root the root url of the rails application
	 * @param model the model we want to check
	 * @param db writable access to our cache database
	 * @return true if the cache is stale
	 */
	public synchronized boolean updateCache(String root, String model)
	{
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(model);
		Cursor c = queryBuilder.query(getReadableDatabase(), null, null, null, null, null, "updated_at desc");
		if(!c.moveToFirst())
			return true;
		int lastUpdate = c.getInt(c.getColumnIndex("updated_at"));
		try {				
			Map<String,String> map = new HashMap<String,String>();
			map.put("model", model);
			
			//Check if the schema has ever been imported before TODO do this earlier

			//Check if the cache needs to be refreshed
			JSONObject updateResponse = new JSONObject(RailsUtils.getRequest(root,"api/v1/refresh.json",map));
			if (lastUpdate != updateResponse.getInt("update"))
			{
				return true;
			}
			else
				return false;
			
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true; 
		
	}
	
	protected synchronized boolean checkCache(String model,String root) {
		boolean modelCached = true;
		SQLiteDatabase db = this.getWritableDatabase();
		//Check the status of the cache database
		if (!RailsCacheHelper.schemaCheck(db,model))
		{	
			Map<String,String> map = new HashMap<String,String>();
			map.put("model", model);
			try {
				RailsCacheTable.importSchema(db, RailsUtils.getRequest(root, "api/v1/schema.json", map), model);
			} catch (ClientProtocolException e) {
				Log.e("RailsProviderQuery", "Client HTTP Error", e);
			} catch (IOException e) {
				Log.e("RailsProviderQuery", "Network IO Error", e);
			}
			modelCached = false;
		} else if (this.updateCache(root, model))
		{
			modelCached = false;
			this.clearModelFromCache(model);
		}
		db.close();
		return modelCached;
	}
	
	/**
	 *  Check if we have the schema information for a model in our database
	 * @param model the model we want to check
	 * @param db a readable link to our database
	 * @return if the model schema exists
	 */
	public static synchronized boolean schemaCheck(SQLiteDatabase db, String model)
	{
		Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+model+"'", null);
	    if(cursor!=null) {
	        if(cursor.getCount()>0) {
	            cursor.close();
	            return true;
	        }
	        cursor.close();
	    }
	    return false;
	}
	
	/**
	 * Search for an entry in the cache
	 * @param model the name of the model to lookup
	 * @param query the query to lookup
	 * @param params the params for the query
	 * @return a RailsCacheEntry detailing the cache entry, or null
	 */
	public synchronized RailsCacheEntry cacheSearch (String uid){
		return queryCache.get(uid);
	}
	
	public synchronized boolean clearModelFromCache (String model)
	{
		Iterator<Entry<String, RailsCacheEntry>> it = queryCache.entrySet().iterator();
		while(it.hasNext())
		{
			if (it.next().getValue().getModel().equals(model))
				it.remove();
		}
		return true;
	}
	
	/**
	 * Store a new cache entry
	 * @param model the name of the model to lookup
	 * @param query the query to lookup
	 * @param params the params for the query
	 * @param entry a cacheEntry object that corresponds to the query
	 */
	public synchronized void storeCacheEntry(String uid, RailsCacheEntry entry)
	{
		queryCache.put(uid, entry);
	}

	public static String generateCacheUID(String model, String query, String[] params) {
		String uid = model;
		uid += query.trim();
		for (String param : params)
			uid += param.trim();
		return uid;
	}
	
	/**
	 * Find an object in the cache
	 * @param model the model we are searching in
	 * @param id the id of the object we want to lookup
	 * @return a cursor to the object (empty is there is no object)
	 */
	public synchronized Cursor lookupObject(String model, String id) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(model);
		String[] args = {id};
		return queryBuilder.query(this.getReadableDatabase(), null, "id = ?", args, null, null, null);
	}

	
}
