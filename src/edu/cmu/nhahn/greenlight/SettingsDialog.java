package edu.cmu.nhahn.greenlight;

import java.util.concurrent.ExecutionException;

import edu.cmu.nhahn.greenlight.contentprovider.RailsCursor;
import edu.cmu.nhahn.greenlight.contentprovider.RailsProvider;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
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
	
	private LevelTask lt;
	private DimmerTask dt;
	private View v;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	   
	    lt = new LevelTask();
	    dt = new DimmerTask();
	    
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
                            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getActivity().getIntent());
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
	 		String[] selectionArgs = new String[1];
	 		selectionArgs[0] = "integer:"+id[0];
	 		
	        return  getActivity().getContentResolver().query(
	        		mDataUrl,
	        		null,
	        		"Room.find_by_id(?).current_room_dimmer.dimmer", 
	        		selectionArgs, null);
	     }

	     protected void onProgressUpdate(Void... progress) {
	     }

	     protected void onPostExecute(Cursor c) {
	    	 if (c == null)
	    		 return;
	    	 RailsCursor cursor = ((RailsCursor) ((CursorWrapper) c).getWrappedCursor());
	    	 cursor.moveToFirst();
	 	    ((ToggleButton) v.findViewById(R.id.lightButton)).setChecked(cursor.getBoolean(cursor.getColumnIndex("is_on")));
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
	        		"Room.find_by_id(?).current_room_dimmer_setting", 
	        		selectionArgs, null);
	     }

	     protected void onProgressUpdate(Void... progress) {
	     }

	     protected void onPostExecute(Cursor c) {
	    	 if (c == null)
	    		 return;
	    	 
	    	 c.moveToFirst();
		    ((SeekBar) v.findViewById(R.id.lightLevel)).setProgress(c.getInt(c.getColumnIndex("value")));
	     }
	 }
}
