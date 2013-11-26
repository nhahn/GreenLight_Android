package edu.cmu.nhahn.greenlight;

import java.util.concurrent.ExecutionException;

import edu.cmu.nhahn.greenlight.contentprovider.RailsCursor;
import edu.cmu.nhahn.greenlight.contentprovider.RailsProvider;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.ToggleButton;

public class SettingsDialog extends DialogFragment {
	
	public final static String ROOM_ID = "room";
	public final static String SWITCH_CHANGED = "switch";
	public final static String LEVEL_CHANGED = "level";
	
	public final static String SWITCH_CHANGED_ID = "switch_id";
	public final static String LEVEL_CHANGED_ID = "level_id";
	
	private LevelTask lt;
	private DimmerTask dt;
	private View v;
	
	private boolean is_on;
	private int is_on_id;
	
	private int level;
	private int level_id;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	   
	    lt = new LevelTask();
	    dt = new DimmerTask();
	    level = 50;
	    is_on = false;
	}
    
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    // Get the layout inflater
	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    v = inflater.inflate(R.layout.settings_dialog, null);
	    
	    
	    lt.execute(getArguments().getString(ROOM_ID));
	    dt.execute(getArguments().getString(ROOM_ID));
	   
        ((SeekBar) v.findViewById(R.id.lightLevel)).setMax(100);

 	    try {
			dt.get();
	 	    lt.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
 	    
	    builder.setView(v)

	    // Add action buttons
            .setPositiveButton(R.string.update,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	Intent i = new Intent();
                        	
                        	int seek = ((SeekBar) v.findViewById(R.id.lightLevel)).getProgress();
                        	boolean sw = ((ToggleButton) v.findViewById(R.id.lightButton)).isChecked();
                        	if (seek != level) {
                        		i.putExtra(LEVEL_CHANGED, seek);
                        		i.putExtra(LEVEL_CHANGED_ID, level_id);
                        	}
                        	if (is_on != sw) {
                        		i.putExtra(SWITCH_CHANGED, sw);
                        		i.putExtra(SWITCH_CHANGED_ID, is_on_id);
                        	}
                        		
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, i);
                        }
                    }
            )
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, getActivity().getIntent());
                }
            });     
            
        
	    return builder.create();
	}
	
	 private class DimmerTask extends AsyncTask<String, Void, Cursor> {
	     protected Cursor doInBackground(String... id) {
	 		Uri mDataUrl = Uri.parse("content://"+RailsProvider.AUTHORITY+"/Room");
	 		String[] selectionArgs = new String[2];
	 		selectionArgs[0] = "integer:"+id[0];
	 		selectionArgs[1] = "symbol:room_dimmers";
	 		
	        return  getActivity().getContentResolver().query(
	        		mDataUrl,
	        		null,
	        		"find_by_id(?).current_room_dimmer.dimmer.includes(?)", 
	        		selectionArgs, null);
	     }

	     protected void onProgressUpdate(Void... progress) {
	     }

	     protected void onPostExecute(Cursor c) {
	    	 if (c == null)
	    		 return;
	    	 RailsCursor cursor = ((RailsCursor) ((CursorWrapper) c).getWrappedCursor());
	    	 cursor.moveToFirst();
	    	 is_on = cursor.getBoolean(cursor.getColumnIndex("is_on"));
	    	 is_on_id = c.getInt(c.getColumnIndex("id"));
	    	 RailsCursor asso = cursor.getAssociation("room_dimmers");
	    	 asso.moveToFirst();
	    	 while(true) {
	    		 if(asso.getString(asso.getColumnIndex("end_date")).equals("null"))
	    	    	 level_id = asso.getInt(asso.getColumnIndex("id"));
	    		 if (!asso.moveToNext())
	    			 break;
	    	 }
	 	    ((ToggleButton) v.findViewById(R.id.lightButton)).setChecked(is_on);
	     }
	 }
	 
	 private class LevelTask extends AsyncTask<String, Void, Cursor> {
	     protected Cursor doInBackground(String... id) {
	 		Uri mDataUrl = Uri.parse("content://"+RailsProvider.AUTHORITY+"/Room");
	 		String[] selectionArgs = new String[1];
	 		selectionArgs[0] = "integer:"+id[0];
	 		
	        return  getActivity().getContentResolver().query(
	        		mDataUrl,
	        		null,
	        		"find_by_id(?).current_room_dimmer_setting", 
	        		selectionArgs, null);
	     }

	     protected void onProgressUpdate(Void... progress) {
	     }

	     protected void onPostExecute(Cursor c) {
	    	 if (c == null)
	    		 return;
	    	 
	    	 c.moveToFirst();
	    	 level = c.getInt(c.getColumnIndex("value"));
		    ((SeekBar) v.findViewById(R.id.lightLevel)).setProgress(level);
	     }
	 }
}
