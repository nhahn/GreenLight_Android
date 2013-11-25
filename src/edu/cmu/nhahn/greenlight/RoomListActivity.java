package edu.cmu.nhahn.greenlight;

import edu.cmu.nhahn.greenlight.R;
import edu.cmu.nhahn.greenlight.bluetooth.MonitoringService;
import edu.cmu.nhahn.greenlight.contentprovider.RailsProvider;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;

/**
 * An activity representing a list of Rooms. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link RoomDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link RoomListFragment} and the item details (if present) is a
 * {@link RoomDetailFragment}.
 * <p>
 * This activity also implements the required {@link RoomListFragment.Callbacks}
 * interface to listen for item selections.
 */
public class RoomListActivity extends Activity implements
		RoomListFragment.Callbacks {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_room_list);

		startService(new Intent(this,MonitoringService.class));
		
		if (findViewById(R.id.room_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			((RoomListFragment) getFragmentManager().findFragmentById(
					R.id.room_list)).setActivateOnItemClick(true);
		}
		
		if(getIntent().getStringExtra(RoomDetailFragment.ARG_ROOM_ID) != null)
		{
			lookupItem(getIntent().getStringExtra(RoomDetailFragment.ARG_ROOM_ID));
		}

		// TODO: If exposing deep links into your app, handle intents here.
	}

	public void lookupItem(String id) {
		new LookupIdTask().execute(id);
	}
	
	
	 private class LookupIdTask extends AsyncTask<String, Void, Cursor> {
	     protected Cursor doInBackground(String... id) {
	 		Uri mDataUrl = Uri.parse("content://"+RailsProvider.AUTHORITY+"/Dimmer");
	 		String[] selectionArgs = new String[1];
	 		selectionArgs[0] = "integer:"+id[0];
	 		
	        return  getContentResolver().query(
	        		mDataUrl,
	        		null,
	        		"find_by_id(?).current_room_dimmer.room", 
	        		selectionArgs, null);
	     }

	     protected void onProgressUpdate(Void... progress) {
	     }

	     protected void onPostExecute(Cursor c) {
	    	 if (c == null)
	    		 return;
	    	 
	    	 c.moveToFirst();
	    	 onItemSelected(c.getString(c.getColumnIndex("id")));
	     }
	 }
	
	/**
	 * Callback method from {@link RoomListFragment.Callbacks} indicating that
	 * the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(String id) {
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putString(RoomDetailFragment.ARG_ROOM_ID, id);
			RoomDetailFragment fragment = new RoomDetailFragment();
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction()
					.replace(R.id.room_detail_container, fragment).commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, RoomDetailActivity.class);
			detailIntent.putExtra(RoomDetailFragment.ARG_ROOM_ID, id);
			startActivity(detailIntent);
		}
	}
}
