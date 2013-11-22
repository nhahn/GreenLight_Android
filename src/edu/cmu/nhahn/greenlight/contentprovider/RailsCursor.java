package edu.cmu.nhahn.greenlight.contentprovider;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

public class RailsCursor extends AbstractCursor {

	private JSONArray json;
	private boolean cache;
	private Cursor cacheCursor;
	private Map<String,String> fieldMap;
	private Map<String,RailsAssociation> associations;
	private Thread save;
	private SQLiteDatabase db;
	private RailsCacheHelper database;
	private String uid;
	
	public RailsCursor(final String model, final RailsCacheHelper database, final JSONArray json, final String[] loadedAssociations, final String uid) {
		super();
		this.json = json;
		cache = false;
		this.db = database.getWritableDatabase();
		this.database = database;
		fieldMap = RailsUtils.buildFields(model,db);
		associations = RailsUtils.buildAssociations(model,db);
		this.uid = uid;
		for(String aso : loadedAssociations) {
			if (!RailsCacheHelper.schemaCheck(db,associations.get(aso).getKlass()))
			{	
				Map<String,String> map = new HashMap<String,String>();
				map.put("model", associations.get(aso).getKlass());
				try {
					RailsCacheTable.importSchema(db, RailsUtils.getRequest(RailsProvider.root, "api/v1/schema.json", map), associations.get(aso).getKlass());
				} catch (ClientProtocolException e) {
					Log.e("RailsProviderQuery", "Client HTTP Error", e);
				} catch (IOException e) {
					Log.e("RailsProviderQuery", "Network IO Error", e);
				}
			}
			associations.get(aso).setLoaded(true);
		}
		save = new Thread(new Runnable() {
			@Override
			public void run() {
				saveToDatabase(database,model,json, fieldMap,loadedAssociations, uid);				
			}

		});
		save.start();
	}
	
	public RailsCursor(final String model, final RailsCacheHelper database, final JSONArray json, final String[] loadedAssociations) {
		super();
		this.json = json;
		cache = false;
		this.db = database.getWritableDatabase();
		this.database = database;
		fieldMap = RailsUtils.buildFields(model,db);
		associations = RailsUtils.buildAssociations(model,db);
		uid = "";
		for(String aso : loadedAssociations) {
			if (!RailsCacheHelper.schemaCheck(db,associations.get(aso).getKlass()))
			{	
				Map<String,String> map = new HashMap<String,String>();
				map.put("model", associations.get(aso).getKlass());
				try {
					RailsCacheTable.importSchema(db, RailsUtils.getRequest(RailsProvider.root, "api/v1/schema.json", map), associations.get(aso).getKlass());
				} catch (ClientProtocolException e) {
					Log.e("RailsProviderQuery", "Client HTTP Error", e);
				} catch (IOException e) {
					Log.e("RailsProviderQuery", "Network IO Error", e);
				}
			}
			associations.get(aso).setLoaded(true);
		}
	}
	
	public void close()
	{
		if (cache)
			cacheCursor.close();
		else
			try {
				save.join();
			} catch (InterruptedException e) {
			}
		super.close();
	}
	
	public RailsCursor(String model, RailsCacheHelper database, Cursor cacheCursor, String[] loadedAssociations)
	{
		super();
		this.cacheCursor = cacheCursor;
		cache = true;
		this.db = database.getWritableDatabase();
		this.database = database;
		fieldMap = RailsUtils.buildFields(model,db);
		associations = RailsUtils.buildAssociations(model,db);
		uid = "";
		for(String aso : loadedAssociations)
			associations.get(aso).setLoaded(true);
	}
	
	@SuppressWarnings("unchecked")
	private void saveToDatabase(RailsCacheHelper database, String model,
			JSONArray json, Map<String,String> fieldMap, String[] loadedAssociations, String uid) {
		
		SQLiteDatabase db = database.getWritableDatabase();
		Map<String,Map<String,String>> includesMap = new HashMap<String,Map<String,String>>();
		int[] ids = new int[json.length()];
		for(String aso : loadedAssociations)
			includesMap.put(associations.get(aso).getKlass(), RailsUtils.buildFields(associations.get(aso).getKlass(), db));

		for(int i = 0; i < json.length(); i++)
		{
			JSONObject obj;
			try {
				obj = json.getJSONObject(i);
			} catch (JSONException e) {
				continue;
			}
			Iterator<String> it = obj.keys();
			ContentValues values = new ContentValues();
			while (it.hasNext())
			{
				String key = it.next();
				RailsAssociation aso = associations.get(key);
				if (key.equals("id"))
					ids[i] = obj.optInt(key); 
				if( aso != null)
					if (aso.isArray())
						try {
							saveToDatabase(database,aso.getKlass(),obj.getJSONArray(key),includesMap.get(aso.getKlass()),new String[0],"");
						} catch (JSONException e) {
							Log.e("RailsCacheSaver", "Error processing relation",e);
						}
					else
					{
						try {
							JSONArray arr = new JSONArray();
							arr.put(obj.getJSONObject(key));
							saveToDatabase(database,aso.getKlass(),arr,includesMap.get(aso.getKlass()),new String[0],"");
						} catch (JSONException e) {
							Log.e("RailsCacheSaver", "Error processing relation",e);
						}
					}
				else
					RailsUtils.cvAdapt(fieldMap.get(key), obj, key, values);
			}
			db.insertWithOnConflict(model, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		}
		if (!TextUtils.isEmpty(uid))
			database.storeCacheEntry(uid, new RailsCacheEntry(model, uid, loadedAssociations, ids));
	}
	
	public int[] cacheIds() throws JSONException{
		//If this is in the cache -- it is either a simple request that doesn't
		//need to be manually cached -- or its already in the cache list
		if (!cache)
		{
			int[] retVal = new int[getCount()];
			for(int i = 0; i < getCount(); i++)
				retVal[i] = json.getJSONObject(i).getInt("id");
			return retVal;
		} 
		return new int[0];
	}

	@Override
	public String[] getColumnNames() {
		if (cache)
			return cacheCursor.getColumnNames();
		else
		{
			Set<String> set = fieldMap.keySet();
			String[] retVal = new String[set.size()];
			return set.toArray(retVal);
		}
	}
	
	@Override
	public int getColumnIndex(String columnName){
		if (columnName.equals("_id"))
			return super.getColumnIndex("id");
		else
			return super.getColumnIndex(columnName);
	}

	@Override
	public int getCount() {
		if (cache)
			return cacheCursor.getCount();
		else
			return json.length();
	}

	@Override
	public double getDouble(int column) {
		if (cache) {
			cacheCursor.moveToPosition(this.mPos);
			return cacheCursor.getDouble(column);
		}
		else {
			try {
				return json.getJSONObject(this.mPos).getDouble(getColumnNames()[column]);
			} catch (JSONException e) {
				return 0.0;
			}
		}
	}

	@Override
	public float getFloat(int column) {
		if (cache) {
			cacheCursor.moveToPosition(this.mPos);
			return cacheCursor.getFloat(column);
		}
		else {
			try {
				return (float) json.getJSONObject(this.mPos).getDouble(getColumnNames()[column]);
			} catch (JSONException e) {
				return 0;
			}
		}
	}

	@Override
	public int getInt(int column) {
		if (cache) {
			cacheCursor.moveToPosition(this.mPos);
			return cacheCursor.getInt(column);
		}
		else {
			try {
				return json.getJSONObject(this.mPos).getInt(getColumnNames()[column]);
			} catch (JSONException e) {
				return 0;
			}
		}
	}

	@Override
	public long getLong(int column) {
		if (cache) {
			cacheCursor.moveToPosition(this.mPos);
			return cacheCursor.getInt(column);
		}
		else {
			try {
				return json.getJSONObject(this.mPos).getLong(getColumnNames()[column]);
			} catch (JSONException e) {
				return 0;
			}
		}
	}

	@Override
	public short getShort(int column) {
		if (cache) {
			cacheCursor.moveToPosition(this.mPos);
			return cacheCursor.getShort(column);
		}
		else {
			try {
				return (short) json.getJSONObject(this.mPos).getInt(getColumnNames()[column]);
			} catch (JSONException e) {
				return 0;
			}
		}
	}

	@Override
	public String getString(int column) {
		if (cache) {
			cacheCursor.moveToPosition(this.mPos);
			return cacheCursor.getString(column);
		}
		else {
			try {
				return json.getJSONObject(this.mPos).getString(getColumnNames()[column]);
			} catch (JSONException e) {
				return null;
			}
		}
	}
	
	public Date getDateTime(int column){
		if (cache) {
			cacheCursor.moveToPosition(this.mPos);
			return new Date(cacheCursor.getLong(column));
		} else {
			try {
				return new Date(json.getJSONObject(this.mPos).getLong(getColumnNames()[column]));
			} catch (JSONException e) {
				return null;
			}
		}
	}
	
	public boolean getBoolean(int column){
		if (cache) {
			cacheCursor.moveToPosition(this.mPos);
			return (cacheCursor.getLong(column) < 1)? false: true;
		} else {
			try {
				return json.getJSONObject(this.mPos).getBoolean(getColumnNames()[column]);
			} catch (JSONException e) {
				return false;
			}
		}
	} 
	
	public RailsCursor getAssociation(String association)
	{
		RailsAssociation aso = associations.get(association);
		if (aso == null)
			return null;

		try {
			if(!cache && aso.isLoaded())
				if(aso.isArray())
					return new RailsCursor(aso.getKlass(),database,json.getJSONObject(this.mPos).getJSONArray(aso.getName()),new String[0]);
				else {
					JSONArray arr = new JSONArray();
					arr.put(json.getJSONObject(this.mPos).getJSONObject(aso.getName()));
					return new RailsCursor(aso.getKlass(),database,arr,new String[0]);
				}
			else
				return aso.performQuery(database, this);
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
		return null;
	}

	@Override
	public boolean isNull(int column) {
		if (cache) {
			cacheCursor.moveToPosition(this.mPos);
			return cacheCursor.isNull(column);
		} else {
			try {
				return json.getJSONObject(this.mPos).isNull(getColumnNames()[column]);
			} catch (JSONException e) {
				return true;
			}
		}
	}
	
	public String getUID()
	{
		return uid;
	}
	
	public Map<String,String> getFieldMap()
	{
		return fieldMap;
	}
	
}
