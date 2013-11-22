package edu.cmu.nhahn.greenlight.contentprovider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

//Abstract cache interface for rails objects
public class RailsCacheTable {
	
	public static final String TABLE_URI = "models";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_MODEL = "model";
	public static final String COLUMN_UPDATE = "update_time";
	
	
	public static final String TABLE_SCHEMAS = "schemas";
	public static final String COLUMN_FIELD = "field";
	public static final String COLUMN_DATATYPE = "datatype";
	
	public static final String TABLE_ASSOCIATIONS = "associations";
	public static final String COLUMN_TYPE = "association_type";
	public static final String COLUMN_PRIMARY_KEY = "pk";
	public static final String COLUMN_FOREIGN_KEY = "fk";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_CLASS = "class";


	private static final String DATABASE_CREATE_URI = "create table "
			+ TABLE_URI
			+ "("
			+ COLUMN_ID + " integer primary key autoincrement,"
			+ COLUMN_MODEL + " text not null, " 
			+ COLUMN_UPDATE + " integer default 0"
			+ ");";
	
	private static final String DATABASE_CREATE_SCHEMAS = "create table "
			+TABLE_SCHEMAS
			+ "("
			+ COLUMN_ID + " integer primary key autoincrement,"
			+ COLUMN_MODEL + " text not null, " 
			+ COLUMN_FIELD + " text not null,"
			+ COLUMN_DATATYPE + " text not null"
			+ ");"
			;
	
	private static final String DATABASE_CREATE_ASSOCIATIONS = "create table "
			+ TABLE_ASSOCIATIONS
			+ "("
			+ COLUMN_ID + " integer primary key autoincrement,"
			+ COLUMN_MODEL + " text not null, " 
			+ COLUMN_TYPE + " text not null,"
			+ COLUMN_PRIMARY_KEY + " text not null,"
			+ COLUMN_FOREIGN_KEY + " text not null,"
			+ COLUMN_NAME + " text not null,"
			+ COLUMN_CLASS + " text not null"		
			+ ");"
			;
	
	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE_SCHEMAS);
		database.execSQL(DATABASE_CREATE_URI);
		database.execSQL(DATABASE_CREATE_ASSOCIATIONS);

		JSONArray arr;
		try {
			arr = RailsCacheHelper.uris(RailsProvider.root);
			for(int i = 0; i < arr.length(); i++)
			{
				ContentValues values = new ContentValues();
				values.put(RailsCacheTable.COLUMN_MODEL, arr.getString(i));
				database.insert(RailsCacheTable.TABLE_URI, null, values);
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(RailsCacheTable.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		List<String> tables = new ArrayList<String>();
		Cursor cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE type='table';", null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
		    String tableName = cursor.getString(1);
		    if (!tableName.equals("android_metadata") &&
		            !tableName.equals("sqlite_sequence"))
		        tables.add(tableName);
		    cursor.moveToNext();
		}
		cursor.close();

		for(String tableName:tables) {
		    database.execSQL("DROP TABLE IF EXISTS " + tableName);
		}

		onCreate(database);
	}
	
	@SuppressWarnings("unchecked")
	public static synchronized void importSchema(SQLiteDatabase database, String jsonschema, String model)
	{
		try {
			String sql = "";
			JSONObject schema = new JSONObject(jsonschema);
			
			sql += "create table " + model + "(";
			JSONObject fieldList = schema.getJSONObject("schema");
			Iterator<String> it = fieldList.keys();
			while(it.hasNext())
			{
				JSONObject field = fieldList.getJSONObject((String)it.next());
				ContentValues cv = new ContentValues();
				cv.put(COLUMN_MODEL, model);
				cv.put(COLUMN_FIELD, field.getString("name"));
				cv.put(COLUMN_DATATYPE, field.getString("type"));
				sql += field.getString("name") + " " + RailsUtils.dbTypeResolve(field.getString("type"));
				if (field.getBoolean("primary"))
					sql += " primary key";
				sql += ",";
				database.insert(TABLE_SCHEMAS, null, cv);
			}
			sql = sql.substring(0, sql.length()-1) + ")";
			database.execSQL(sql);
			
			JSONArray associations = schema.getJSONArray("associations");
			for (int i = 0; i < associations.length(); i++)
			{
				JSONObject obj = associations.getJSONObject(i);
				ContentValues cv = new ContentValues();
				cv.put(COLUMN_MODEL, model);
				cv.put(COLUMN_TYPE, obj.getString("type"));
				cv.put(COLUMN_PRIMARY_KEY, obj.getString("primary_key"));
				cv.put(COLUMN_FOREIGN_KEY, obj.getString("foreign_key"));
				cv.put(COLUMN_CLASS, obj.getString("class"));
				cv.put(COLUMN_NAME, obj.getString("name"));
				database.insert(TABLE_ASSOCIATIONS, null, cv);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block 
			// error importing schema
			e.printStackTrace();
		}
	}
	
}
