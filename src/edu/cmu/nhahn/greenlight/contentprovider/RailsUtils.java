package edu.cmu.nhahn.greenlight.contentprovider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.nhahn.greenlight.R;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class RailsUtils {

	public static void jsonAdapt(String type, JSONObject params,
			String key, ContentValues values) {
		try{
			if(type == null)
				return;
			else if(type.equals("boolean"))
				params.put(key,values.getAsBoolean(key));
			else if (type.equals("date"))
				params.put(key,values.getAsInteger(key));
			else if (type.equals("datetime"))
				params.put(key,values.getAsInteger(key));
			else if (type.equals("decimal"))
				params.put(key,values.getAsDouble(key));
			else if (type.equals("float"))
				params.put(key,values.getAsFloat(key));
			else if (type.equals("integer"))
				params.put(key,values.getAsInteger(key));
			else if (type.equals("references"))
				params.put(key,values.getAsInteger(key));
			else if (type.equals("string"))
				params.put(key,values.getAsString(key));
			else if (type.equals("text"))
				params.put(key,values.getAsString(key));
			else if (type.equals("time"))
				params.put(key,values.getAsInteger(key));
			else if (type.equals("timestamp"))
				params.put(key,values.getAsInteger(key));
			else if (type.equals("binary"))
				params.put(key,values.getAsByteArray(key));
		} catch (JSONException e)
		{
			//Just leave this un-handled... 
		}
	}
	
	public static void cvAdapt(String type, JSONObject obj,
			String key, ContentValues values) {
		try{
			if (type == null)
				return;
			if(type.equals("boolean"))
				values.put(key, obj.getBoolean(key));
			else if (type.equals("date"))
				values.put(key, obj.getLong(key));
			else if (type.equals("datetime"))
				values.put(key, obj.getLong(key));
			else if (type.equals("decimal"))
				values.put(key, obj.getDouble(key));
			else if (type.equals("float"))
				values.put(key, obj.getDouble(key));
			else if (type.equals("integer"))
				values.put(key, obj.getInt(key));
			else if (type.equals("references"))
				values.put(key, obj.getInt(key));
			else if (type.equals("string"))
				values.put(key, obj.getString(key));
			else if (type.equals("text"))
				values.put(key, obj.getString(key));
			else if (type.equals("time"))
				values.put(key, obj.getLong(key));
			else if (type.equals("timestamp"))
				values.put(key, obj.getLong(key));
			else if (type.equals("binary"))
				values.put(key, (byte[])obj.get(key));
		} catch (JSONException e)
		{
			//Just leave this un-handled... 
		}
	}
	
	public static String dbTypeResolve(String type)
	{
		if(type.equals("boolean"))
			return "integer";
		else if (type.equals("date"))
			return "integer";
		else if (type.equals("datetime"))
			return "integer";
		else if (type.equals("decimal"))
			return "real";
		else if (type.equals("float"))
			return "real";
		else if (type.equals("integer"))
			return "integer";
		else if (type.equals("references"))
			return "integer";
		else if (type.equals("string"))
			return "text";
		else if (type.equals("text"))
			return "text";
		else if (type.equals("time"))
			return "integer";
		else if (type.equals("timestamp"))
			return "integer";
		else if (type.equals("binary"))
			return "blob";
		else
			return null;
	}

	public static String getRequest(String root, String path, Map<String,String> params) throws ClientProtocolException, IOException
	{
		Uri.Builder uri = Uri.parse("http://"+root).buildUpon().path(path);
		if(params != null)
			for(String param : params.keySet())
			{
				uri.appendQueryParameter(param, params.get(param));
			}
		
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(uri.build().toString());
		HttpResponse response;
        // setup the request headers
        get.setHeader("Accept", "application/json");
        get.setHeader("Content-Type", "application/json");
        response = client.execute(get);
        return convertStreamToString(response.getEntity().getContent());
	}
	
	public static JSONArray postRequest(String root, String path, String json) throws ClientProtocolException, IOException
	{
		DefaultHttpClient client = new DefaultHttpClient();
		Uri.Builder uri = Uri.parse("http://"+root).buildUpon().path(path);

		HttpPost post = new HttpPost(uri.build().toString());
		HttpResponse response;
        // setup the request headers
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(json));
        response = client.execute(post);
        String out = convertStreamToString(response.getEntity().getContent());
        try{
        	return new JSONArray(out);
        } catch (JSONException e)
        {
        	try{
        		JSONArray ret = new JSONArray();
        		ret.put(new JSONObject(out));
        		return ret;
        	} catch (Exception e1)
        	{
        		Log.e("JSONProblem", e1.getMessage(), e1);
        		return new JSONArray();
        	}
        }
	}
	
	public static String convertStreamToString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),1024);
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                inputStream.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }
	
	public static Map<String,String> buildFields(String model, SQLiteDatabase db)
	{
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		Map<String,String> map = new LinkedHashMap<String,String>();
		builder.setTables(RailsCacheTable.TABLE_SCHEMAS);
		String[] args = {model};
		Cursor c = builder.query(db, null, RailsCacheTable.COLUMN_MODEL + "= ?", args, null, null, null);
		while(c.moveToNext())
		{
			String str1 = c.getString(c.getColumnIndex(RailsCacheTable.COLUMN_FIELD));
			String str2 = c.getString(c.getColumnIndex(RailsCacheTable.COLUMN_DATATYPE));
			map.put(str1, str2);
		}
		return map;
	}
	
	public static Map<String,RailsAssociation> buildAssociations(String model, SQLiteDatabase db)
	{
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		Map<String,RailsAssociation> map = new LinkedHashMap<String,RailsAssociation>();
		builder.setTables(RailsCacheTable.TABLE_ASSOCIATIONS);
		String[] args = {model};
		Cursor c = builder.query(db, null, RailsCacheTable.COLUMN_MODEL + "= ?", args, null, null, null);
		while(c.moveToNext())
		{
			RailsAssociation aso = new RailsAssociation(c);
			map.put(aso.getName(), aso);
		}
		return map;
	}

	public static void networkError(Context context, OnClickListener onClickListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(R.string.network_error).setTitle("Connection Error");
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		builder.setPositiveButton(R.string.retry,onClickListener);
		AlertDialog dialog = builder.create();
		dialog.show();
		
	}
	
}
