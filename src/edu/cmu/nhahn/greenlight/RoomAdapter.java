package edu.cmu.nhahn.greenlight;

import java.util.ArrayList;

import edu.cmu.nhahn.greenlight.contentprovider.RailsCursor;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filter;
import android.widget.TextView;

public class RoomAdapter extends CursorAdapter {
	private RailsCursor mCursor;
	@SuppressWarnings("unused")
	private Context mContext;
	private final LayoutInflater mInflater;
	private String filter;
	private ArrayList<Integer> hidden;

	public RoomAdapter(Context context, final Cursor c) {
		super(context, c, 0);
		mInflater=LayoutInflater.from(context);
		mContext=context;
		mCursor = ((RailsCursor) ((CursorWrapper) c).getWrappedCursor());
		hidden = new ArrayList<Integer>();
		filter = "";
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		RailsCursor c = (RailsCursor) ((CursorWrapper) cursor).getWrappedCursor();
		Cursor asso = c.getAssociation("building");
		asso.moveToFirst();

		String abr = asso.getString(asso.getColumnIndex("abbreviation"));
		String number = cursor.getString(cursor.getColumnIndex("room_num"));

		TextView building=(TextView)view.findViewById(android.R.id.text1);
		building.setText(abr);

		TextView num=(TextView)view.findViewById(android.R.id.text2);
		num.setText(number);
		
	}

	@Override
	public int getCount() {
	    return super.getCount() - hidden.size();
	}
	
	@Override
	public Object getItem(int position)
	{
	    for (Integer hiddenIndex : hidden) {
	        if(hiddenIndex <= position)
	            position++;
	    }

	    return super.getItem(position);
	}
	
	public View getView (int position, View convertView, ViewGroup parent) {
		for (Integer hiddenIndex : hidden) {
			if(hiddenIndex <= position)
				position++;
		}

		return super.getView(position, convertView, parent);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final View view=mInflater.inflate(R.layout.list_item_custom,parent,false); 
		return view;
	}
	
	@Override
	public void notifyDataSetChanged(){
		int counter = 0;
		hidden = new ArrayList<Integer>();
		mCursor.moveToFirst();
		
		while(true) {
			Cursor asso = mCursor.getAssociation("building");
			asso.moveToFirst();
	
			String abr = asso.getString(asso.getColumnIndex("abbreviation"));
			String number = mCursor.getString(mCursor.getColumnIndex("room_num"));
	
			
			if(!(abr + " " + number).toLowerCase().contains(filter.toLowerCase()))
			{
				hidden.add(counter);
			}
	
			counter++;
			if (!mCursor.moveToNext())
				break;
		}
		
		super.notifyDataSetChanged();
	}

	@Override
	public Filter getFilter()
	{
		return new Filter(){

			@Override
			protected FilterResults performFiltering(CharSequence arg0) {
				filter = arg0.toString();
				return new Filter.FilterResults();
			}

			@Override
			protected void publishResults(CharSequence arg0,
					FilterResults arg1) {
				notifyDataSetChanged();		
			}

		};
	}

}