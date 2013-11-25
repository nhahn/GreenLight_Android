package edu.cmu.nhahn.greenlight.contentprovider;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

public class RailsAssociation {

	private String name;
	private String type;
	private String klass;
	private String pk;
	private String fk;
	private boolean loaded;
	
	public RailsAssociation(Cursor c) {
		name = c.getString(c.getColumnIndex(RailsCacheTable.COLUMN_NAME));
		type = c.getString(c.getColumnIndex(RailsCacheTable.COLUMN_TYPE));
		klass = c.getString(c.getColumnIndex(RailsCacheTable.COLUMN_CLASS));
		pk = c.getString(c.getColumnIndex(RailsCacheTable.COLUMN_PRIMARY_KEY));
		fk = c.getString(c.getColumnIndex(RailsCacheTable.COLUMN_FOREIGN_KEY));
	}
	
	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getKlass() {
		return klass;
	}

	public String getPrivateKey() {
		return pk;
	}

	public String getForeignKey() {
		return fk;
	}
	
	public boolean isLoaded(){
		return loaded;
	}
	
	public boolean isArray(){
		return type.equals("has_many");
	}
	
	public RailsCursor performQuery(RailsCacheHelper database, Cursor c) throws JSONException, ClientProtocolException, IOException {
		//check cache
		SQLiteDatabase db = database.getReadableDatabase();
		if (loaded)
		{
			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(this.klass);
			if (type.equals("belongs_to"))
			{
				String[] args = {c.getString(c.getColumnIndex(fk))};
				Cursor cur = queryBuilder.query(db, null, "id = ?", args, null, null, null);
				return new RailsCursor(this.klass, database,cur, new String[0]);
			}
			else if (type.equals("has_many") || type.equals("has_one"))
			{
				String[] args = {c.getString(c.getColumnIndex(pk))};
				Cursor cur = queryBuilder.query(db, null, fk + " = ?", args, null, null, null);
				return new RailsCursor(this.klass, database,cur, new String[0]);
			}
			return null;
		} else
		{
			String[] args = new String[1];
			if (type.equals("belongs_to")) {
				args[0] = "string:id = "+c.getString(c.getColumnIndex(fk));
			} else if (type.equals("has_many") || type.equals("has_one")){
				args[0] = "string:"+ fk + " = " + c.getString(c.getColumnIndex(pk));
			} else {
				return null;
			}
			
			String model = getKlass();
			String query = RailsCacheHelper.composeQuery(model,"where(?)",args).toString();
			String uid = RailsCacheHelper.generateCacheUID(model, "where(?)", args);
			return new RailsCursor(database,RailsUtils.postRequest(RailsProvider.root,"api/v1/query.json",query),new String[0], uid);
		}
		
	}

	public void setLoaded(boolean b) {
		loaded = b;
	}

}
