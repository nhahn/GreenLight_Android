package edu.cmu.nhahn.greenlight.contentprovider;

import java.io.IOException;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.nhahn.greenlight.authentication.LoginFilter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

//TODO implement eager loading 
public class RailsProvider extends ContentProvider {

	public final static String root = "nhahn-rails.res.cmu.edu:3000";
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int INDEX = 1;
	private static final int SHOW = 2;
	private static final int CACHE = 3;
	
	private RailsCacheHelper database;
	public static final String AUTHORITY = "edu.cmu.nhahn.greenlight.contentprovider";
	private static Integer setup = 0;
	
	@Override
	public boolean onCreate() {
		return true;
	}
	
	private boolean setupDB(){ 
		//Create the cache database file
		try {
			String token = getApplicationToken();
			if(TextUtils.isEmpty(token))
				return false;
			
			synchronized(setup) {
				database = new RailsCacheHelper(getContext(),root);
			}
			SQLiteDatabase db = database.getReadableDatabase();
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(RailsCacheTable.TABLE_URI);
			Cursor cur = queryBuilder.query(db, null, null, null, null, null, null);
			while(cur.moveToNext())
			{
				String model = cur.getString(cur.getColumnIndex(RailsCacheTable.COLUMN_MODEL));
				sUriMatcher.addURI(AUTHORITY, model,INDEX);
				sUriMatcher.addURI(AUTHORITY, model+ "/cache/*",CACHE);
				sUriMatcher.addURI(AUTHORITY, model+"/#",SHOW);
			}
				
			synchronized(setup) {
				setup.notifyAll();
				setup = 2;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (JSONException e) {
			//TODO figure out errors
			return false;
		}
		return true;
	}
	
	private String getApplicationToken(){
		//Activity starting w .. this is problematic 
		try {
			LoginFilter filter = (LoginFilter) this.getContext();
			return filter.getToken();
		} catch (final ClassCastException e)
		{
			return null;
		}
	}

	@Override
	/**
	 * Takes a rails scope query and performs it. 
	 * Selection: A typical scope query, but all args replaced with ? 
	 * selectionArgs: The ? values
	 * 
	 * Ex: URI -- DimmerReading
	 * 	   for_room(?,?,?)
	 * 	   [room_id, Date.yesterday.to_s, Date.today.to_s]
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 */
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO figure out value portion for this 
		if(!setupDB())
			return null;
		
		if (selectionArgs == null)
			selectionArgs = new String[0];
		if (sortOrder == null)
			sortOrder = "";
		if (selection == null)
			selection = "";
		
		int uriType = sUriMatcher.match(uri);
		String model = uri.getPathSegments().get(0);
		String query = "";
		
		boolean modelCached = database.checkCache(model, root);
		
		//Compose our 
		switch(uriType) {
		case INDEX:
			if (selection.isEmpty())
				selection += "all";					
			break;
		case SHOW:
			String[] args = {"integer:" + uri.getLastPathSegment()};
			selectionArgs = args;
			selection = "find_by_id(?)";
			break;
		case CACHE:
			String uid = uri.getLastPathSegment();
			RailsCacheEntry ent = database.cacheSearch(uid);
			if (ent != null)
				return ent.performQuery(database,selection,selectionArgs);
			else
				return null;
		default:
			return null;
		}
		
		if (modelCached)
		{
			//Make cacheSearch more robust
			RailsCacheEntry ent = database.cacheSearch(RailsCacheHelper.generateCacheUID(model, selection, selectionArgs));
			if (ent != null)
				return ent.performQuery(database);  
		}
		
		String[] includes;
		try {
			JSONObject q = RailsCacheHelper.composeQuery(model,selection,selectionArgs);
			JSONArray arr = q.getJSONArray("include");
			includes = new String[arr.length()];
			for(int i = 0; i < arr.length(); i++)
				includes[i] = arr.getJSONObject(i).getString("val");
			query = q.toString();
		} catch (JSONException e) {
			//Output some message here
			Log.e("RailsProviderQuery", "Error parsing user query", e);
			return null;
		}
		
		String uid = RailsCacheHelper.generateCacheUID(model, selection, selectionArgs);
		
		try {			
			return new RailsCursor(database,RailsUtils.postRequest(root,"api/v1/query.json",query),includes, uid);
		} catch (ClientProtocolException e) {
			Log.e("RailsProviderQuery", "Client HTTP Error", e);
		} catch (IOException e) {
			Log.e("RailsProviderQuery", "Network IO Error", e);
		}
		return null;
	}

	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	/*
	 * TODO we are wrapping exceptions in runtime exceptions here -- remember to handle them
	 * correctly
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
	 */
	public Uri insert(Uri uri, ContentValues values) {
		if(!setupDB())
			return null;
		
		int uriType = sUriMatcher.match(uri);
		JSONObject obj = new JSONObject();
		String model = uri.getPathSegments().get(0);
		JSONObject response = new JSONObject();

		try {
			switch(uriType) {
			case INDEX:
				JSONObject params = new JSONObject();
				obj.put("model", model);
				Map<String,String> fieldMap = RailsUtils.buildFields(model, database.getReadableDatabase());
				for(String key : values.keySet())
				{
					RailsUtils.jsonAdapt(fieldMap.get(key), params, key, values);
				}
				obj.put(model.toLowerCase(), params);
				break;
			}
			
			try {
				response = RailsUtils.postRequest(root,"/api/v1/insert.json", obj.toString());
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				throw new IllegalArgumentException(e);
			} catch (IOException e) {
				// TODO Error contacting server -- can't be saved
				throw new IllegalStateException(e);
			}
			//TODO handle this better
			if(RailsException.validateError(response))
				throw new IllegalArgumentException(new RailsException(response));
		
			return Uri.parse("content://" + AUTHORITY + "/" + model + "/" + response.getJSONObject("object").getInt("id"));
		} catch (JSONException e)
		{
			//TODO error parsing JSON
			throw new IllegalStateException();
		}
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int uriType = sUriMatcher.match(uri);
		JSONObject obj = new JSONObject();
		String model = uri.getPathSegments().get(0);
		JSONObject response = new JSONObject();
		
		try {
			obj.put("id", uri.getLastPathSegment());
			switch(uriType) {
			case SHOW:
				JSONObject params = new JSONObject();
				obj.put("model", model);
				Map<String,String> fieldMap = RailsUtils.buildFields(model, database.getReadableDatabase());
				for(String key : values.keySet())
				{
					RailsUtils.jsonAdapt(fieldMap.get(key), params, key, values);
				}
				obj.put(model.toLowerCase(), params);
				break;
			}
			
			try {
				response = RailsUtils.postRequest(root,"/api/v1/update.json", obj.toString());
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				throw new IllegalArgumentException(e);
			} catch (IOException e) {
				// TODO Error contacting server -- can't be saved
				throw new IllegalStateException(e); 
			}
			//TODO handle this better
			if(RailsException.validateError(response))
				throw new IllegalArgumentException(new RailsException(response));
		
			return response.getBoolean("validate")? 1:0;
		} catch (JSONException e)
		{
			//TODO error parsing JSON
			throw new IllegalStateException();
		}
	}
	

}
