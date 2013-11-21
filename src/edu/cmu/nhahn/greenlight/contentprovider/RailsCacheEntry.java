package edu.cmu.nhahn.greenlight.contentprovider;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;

public class RailsCacheEntry {

	private String model;
	private String query;
	private String[] loadedAssociations;
	private int[] ids;
	
	public RailsCacheEntry(String model, String query, String[] loadedAssociations, int[] ids) {
		this.model = model;
		this.query = query;
		this.loadedAssociations = loadedAssociations;
		this.ids = ids;
	}
	 
	public String getModel() {
		return model;
	}

	public String getQuery() {
		return query;
	}

	public String[] getLoadedAssociations() {
		return loadedAssociations;
	}

	public int[] getIds() {
		return ids;
	}

	public RailsCursor performQuery(RailsCacheHelper db)
	{
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		SQLiteDatabase database = db.getWritableDatabase();
		queryBuilder.setTables(this.model);
		String inClause = "[";
		
		for (int i : ids)
			if(i > 0)
				inClause += i + ",";
		
		inClause = inClause.substring(0, inClause.length()-1) + "]";
		
		Cursor cur = queryBuilder.query(database, null, "id in " + inClause, null, null, null, null);
		return new RailsCursor(this.model,db,cur,this.loadedAssociations);
	}
	
	public RailsCursor performQuery(RailsCacheHelper db, String selection, String[] selectionArgs)
	{
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		SQLiteDatabase database = db.getWritableDatabase();
		queryBuilder.setTables(this.model);
		String inClause = "[";
		
		for (int i : ids)
			if(i > 0)
				inClause += i + ",";
		
		inClause = inClause.substring(0, inClause.length()-1) + "]";
		if(!TextUtils.isEmpty(selection))
			inClause += "AND" + selection;
		Cursor cur = queryBuilder.query(database, null, "id in " + inClause, selectionArgs, null, null, null);
		return new RailsCursor(this.model,db,cur,this.loadedAssociations);
	}
}
