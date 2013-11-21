package edu.cmu.nhahn.greenlight;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.app.ListFragment;
import android.widget.SimpleCursorAdapter;
import edu.cmu.nhahn.greenlight.contentprovider.RailsCursor;
import edu.cmu.nhahn.greenlight.contentprovider.RailsProvider;

/**
 * A list fragment representing a list of Rooms. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link RoomDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class RoomListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>{

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sDummyCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	
	private static final int URL_LOADER = 1;
		
	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(String id);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(String id) {
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public RoomListFragment() {
	}

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onResume() {
		getLoaderManager().restartLoader(URL_LOADER, null, this);
		super.onResume();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_room_list,
				container, false);
		
		EditText search = (EditText) rootView.findViewById(R.id.searchBox);
		search.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable arg0) {
				
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {			
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				((SimpleCursorAdapter) getListAdapter()).getFilter().filter(s);
			}
			
		});
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		Cursor c = ((SimpleCursorAdapter) listView.getAdapter()).getCursor(); 
		mCallbacks.onItemSelected(c.getString(c.getColumnIndex("id")));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderID, Bundle arg1) {
		
		Uri mDataUrl = Uri.parse("content://"+RailsProvider.AUTHORITY+"/Room");
		
		switch (loaderID) {
		case URL_LOADER:
			// Returns a new CursorLoader
			return new CursorLoader(
					getActivity(),   // Parent activity context
					mDataUrl,        // Table to query
					null,     // Projection to return
					null,            // No selection clause
					new String[0],            // No selection arguments
					""             // Default sort order
					);
		default:
			// An invalid id was passed in
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, final Cursor c) {
		String[] bindFrom = {"room_name"};
		int[] bindTo = {android.R.id.text1};
		
		if(c != null)
		{
			SimpleCursorAdapter adapt = new SimpleCursorAdapter(getActivity().getApplicationContext(),
					R.layout.list_item_custom,
				c, bindFrom, bindTo, 0);
			setListAdapter(adapt);
			adapt.setFilterQueryProvider(new FilterQueryProvider() {

				@Override
				public Cursor runQuery(CharSequence cs) {
					RailsCursor cursor = ((RailsCursor) ((CursorWrapper) c).getWrappedCursor());
					Uri mDataUrl = Uri.parse("content://"+RailsProvider.AUTHORITY+"/Room/cache/"+cursor.getUID());
					String[] item = {cs.toString()};
					return getActivity().getContentResolver().query(mDataUrl, null, "room_name LIKE %?%", item, null);
				}
				
			});
			
		}
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub
		
	}
}
