package edu.cmu.nhahn.greenlight;

import java.io.IOException;
import java.io.InputStream;
import org.json.JSONException; 

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.SeekBar;
import android.widget.TextView;
import edu.cmu.nhahn.greenlight.contentprovider.RailsCacheHelper;
import edu.cmu.nhahn.greenlight.contentprovider.RailsCursor;
import edu.cmu.nhahn.greenlight.contentprovider.RailsProvider;
import edu.cmu.nhahn.greenlight.contentprovider.RailsUtils;

/**
 * A fragment representing a single Room detail screen. This fragment is either
 * contained in a {@link RoomListActivity} in two-pane mode (on tablets) or a
 * {@link RoomDetailActivity} on handsets.
 */
public class RoomDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ROOM_ID = "item_id";


	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	private static final int URL_LOADER = 1;
	
	private static final int DIALOG_CODE = 100;

	
	public RoomDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ROOM_ID)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			getLoaderManager().initLoader(URL_LOADER, getArguments(), (LoaderCallbacks<Cursor>) this);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    inflater.inflate(R.menu.detail_menu, menu);
	    super.onCreateOptionsMenu(menu,inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	        	showSettingsDialog();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_room_detail,
				container, false);

		/** Setup our graph **/
		WebView graph = (WebView) rootView.findViewById(R.id.chart_view);
		graph.getSettings().setJavaScriptEnabled(true);
		graph.getSettings().setDomStorageEnabled(true);
		
		graph.setWebChromeClient(new WebChromeClient() {
			public boolean onConsoleMessage(ConsoleMessage cm) {
				Log.d("MyApplication", cm.message() + " -- From line "
						+ cm.lineNumber() + " of "
						+ cm.sourceId() );
				return true;
			}
		});
		AssetManager assets = this.getActivity().getAssets();
		try {
			InputStream input = assets.open("chart.html");
			String data = RailsUtils.convertStreamToString(input);
			String[] args = {"integer:"+getArguments().getString(ARG_ROOM_ID),
							 "string:value, recorded_at"
							};
			String data_to_send = RailsCacheHelper.composeQuery("Room", "find_by_id(?).sensor_readings.select(?)", args).toString();
			data = data.replace("URLTOSEND", "http://" +RailsProvider.root+"/api/v1/query.json").replace("DATATOREPLACE",data_to_send);
			graph.loadDataWithBaseURL("http://"+RailsProvider.root, data, "text/html", null, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
		
		return rootView;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderID, Bundle arg1) {
		String room_id = arg1.getString(ARG_ROOM_ID);
		String[] selection = {"symbol:building", "symbol:departments", "string:id="+room_id};
		Uri mDataUrl = Uri.parse("content://"+RailsProvider.AUTHORITY+"/Room");
		
		switch (loaderID) {
		case URL_LOADER:
			// Returns a new CursorLoader
			return new CursorLoader(
					getActivity(),   // Parent activity context
					mDataUrl,        // Table to query
					null,     // Projection to return
					"includes(?, ?).where(?)",            // No selection clause
					selection,            // No selection arguments
					""             // Default sort order
					);
		default:
			// An invalid id was passed in
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
		RailsCursor cursor = ((RailsCursor) ((CursorWrapper) c).getWrappedCursor());
		if (((Cursor) cursor).moveToFirst())
		{
			TextView v = (TextView) getView().findViewById(R.id.room_name);
			v.setText(cursor.getString(cursor.getColumnIndex("official_name")));
			
			v = (TextView) getView().findViewById(R.id.room_department);
			Cursor tmpCursor = (Cursor) cursor.getAssociation("departments");
			if(tmpCursor.moveToFirst())
				v.setText(tmpCursor.getString(tmpCursor.getColumnIndex("name")));
			
			v = (TextView) getView().findViewById(R.id.room_building);
			tmpCursor = (Cursor) cursor.getAssociation("building");
			if(tmpCursor.moveToFirst())
				v.setText(tmpCursor.getString(tmpCursor.getColumnIndex("name")));
			
			v = (TextView) getView().findViewById(R.id.room_number);
			v.setText(cursor.getString(cursor.getColumnIndex("room_num")));
			
			getView().findViewById(R.id.listHeaderProgress).setVisibility(View.GONE);
			getView().findViewById(R.id.room_detail_layout).setVisibility(View.VISIBLE);

		}
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub
		
	}
	
    public void showSettingsDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new SettingsDialog();
        dialog.setTargetFragment(this, DIALOG_CODE);
        
        Bundle args = new Bundle();
        args.putString(SettingsDialog.ROOM_ID, getArguments().getString(ARG_ROOM_ID));
        dialog.setArguments(args);

        dialog.show(getFragmentManager(), "SettingsDialog");
        
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case DIALOG_CODE:

                if (resultCode == Activity.RESULT_OK) {
                    if (data.hasExtra(SettingsDialog.LEVEL_CHANGED))
                    {
                    	new UpdateTask().execute(SettingsDialog.LEVEL_CHANGED,""+data.getIntExtra(SettingsDialog.LEVEL_CHANGED_ID,0), ""+data.getIntExtra(SettingsDialog.LEVEL_CHANGED, 50));
                    }
                    if (data.hasExtra(SettingsDialog.SWITCH_CHANGED))
                    {
                    	new UpdateTask().execute(SettingsDialog.SWITCH_CHANGED,""+data.getIntExtra(SettingsDialog.SWITCH_CHANGED_ID,0), ""+data.getBooleanExtra(SettingsDialog.SWITCH_CHANGED, false));
                    }
                } else if (resultCode == Activity.RESULT_CANCELED){
                    // After Cancel code.
                }

                break;
        }
    }
    
	 private class UpdateTask extends AsyncTask<String, Void, Integer> {
	     protected Integer doInBackground(String... id) {
	    	if(id[0].equals(SettingsDialog.LEVEL_CHANGED))
	    	{
		 		Uri mDataUrl = Uri.parse("content://"+RailsProvider.AUTHORITY+"/DimmerSetting");	
				
		 		ContentValues cv = new ContentValues();
		 		cv.put("room_dimmer_id", id[1]);
		 		cv.put("value", id[2]);
		 		
		        return getActivity().getContentResolver().insert(mDataUrl, cv).hashCode();
	    	} else
	    	{
		 		Uri mDataUrl = Uri.parse("content://"+RailsProvider.AUTHORITY+"/Dimmer/" + id[1]);
		 		
		 		ContentValues cv = new ContentValues();
		 		cv.put("is_on", id[2]);
		 		
		        return getActivity().getContentResolver().update(mDataUrl, cv, "", null);
	    	}

	     }

	     protected void onProgressUpdate(Void... progress) {
	     }

	     protected void onPostExecute(Integer c) {

	     }
	 }
    
}

