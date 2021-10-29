package de.tubs.ibr.dtn.sharebox.ui;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;
import de.tubs.ibr.dtn.sharebox.DtnService;
import de.tubs.ibr.dtn.sharebox.R;
import de.tubs.ibr.dtn.sharebox.data.Database;
import de.tubs.ibr.dtn.sharebox.data.PackageFile;
import de.tubs.ibr.dtn.sharebox.data.Utils;

public class PackageFileListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int FILE_LOADER_ID = 1;
    
    @SuppressWarnings("unused")
    private final String TAG = "PackageFileListFragment";
    
    private CursorAdapter mAdapter = null;
    private DtnService mService = null;
    private Boolean mBound = false;
    
    private AbsListView.MultiChoiceModeListener mMultiChoiceListener = null;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((DtnService.LocalBinder)service).getService();
            
            // initialize the loaders
            getLoaderManager().initLoader(FILE_LOADER_ID,  null, PackageFileListFragment.this);
        }

        public void onServiceDisconnected(ComponentName name) {
            getLoaderManager().destroyLoader(FILE_LOADER_ID);
            mService = null;
        }
    };
    
    /**
     * Create a new instance of PackageFileListFragment
     */
    public static PackageFileListFragment newInstance(Long downloadId) {
    	PackageFileListFragment f = new PackageFileListFragment();
    	
        // Supply buddyId input as an argument.
        Bundle args = new Bundle();
        if (downloadId != null) args.putLong("download_id", downloadId);
        f.setArguments(args);
        
        return f;
    }
    
    @SuppressLint({
            "NewApi", "InlinedApi"
    })
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setEmptyText(getActivity().getResources().getString(R.string.list_no_files));
        
        // enable context menu
        registerForContextMenu(getListView());
        
        // enable action bar selection
        if (Build.VERSION.SDK_INT >= 11) {
            mMultiChoiceListener = new AbsListView.MultiChoiceModeListener() {

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId())
                    {
                    case R.id.itemDelete:
                        SparseBooleanArray selected = getListView().getCheckedItemPositions();
                        int cntChoice = getListView().getCount();
                        for (int i = 0; i < cntChoice; i++) {
                            if (selected.get(i)) {
                                View view = getListView().getChildAt(i);
                                if (view instanceof PackageFileItem) {
                                    PackageFileItem pfi = (PackageFileItem)view;
                                    PackageFile pf = pfi.getObject();

                                    Intent deleteIntent = new Intent(getActivity(), DtnService.class);
                                    deleteIntent.setAction(DtnService.DELETE_FILE_INTENT);
                                    deleteIntent.putExtra(DtnService.EXTRA_KEY_FILE_ID, pf.getId());
                                    getActivity().startService(deleteIntent);
                                }
                            }
                        }
                        mode.finish();
                        return true;
                        
                    default:
                        return true;
                    }
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = getActivity().getMenuInflater();
                    inflater.inflate(R.menu.item_menu, menu);

                    String title = getResources().getQuantityString(R.plurals.listitem_multi_subtitle, 1, 1);
                    mode.setTitle(R.string.listitem_multi_title);
                    mode.setSubtitle(title);
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return true;
                }

                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                    int selectCount = getListView().getCheckedItemCount();
                    String title = getResources().getQuantityString(R.plurals.listitem_multi_subtitle, selectCount, selectCount);
                    mode.setSubtitle(title);
                }
            };
            
            getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            getListView().setMultiChoiceModeListener(mMultiChoiceListener);
        }
        
        mAdapter = new PackageFileAdapter(getActivity(), null, new PackageFileAdapter.ColumnsMap());
        setListAdapter(mAdapter);
        
        // Start out with a progress indicator.
        setListShown(false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.item_menu, menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        
        if (info.targetView instanceof PackageFileItem) {
            PackageFileItem pfi = (PackageFileItem)info.targetView;
            PackageFile pf = pfi.getObject();

            switch (item.getItemId())
            {
            case R.id.itemDelete:
            	Database db = mService.getDatabase();
                db.remove(pf);
                return true;
                
            default:
                return super.onContextItemSelected(item);
            }
        }
    
        return super.onContextItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	PackageFileItem item = (PackageFileItem)v;
        if (item != null) {
        	File f = item.getObject().getFile();

        	/*
        	Intent newIntent = new Intent(android.content.Intent.ACTION_VIEW);
        	newIntent.setDataAndType(Uri.fromFile(f), Utils.getMimeType(f.getAbsolutePath()));
        	newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        	 */
            Intent newIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            newIntent.setType("image/*");
            newIntent.addCategory(Intent.CATEGORY_OPENABLE);
        	try {
        	    Log.d(TAG,"ファイルの保存先のパス " + f.getAbsolutePath());
        	    startActivity(newIntent);//startActivity(newIntent)→
        	} catch (android.content.ActivityNotFoundException e) {
        	    Toast.makeText(getActivity(), getString(R.string.hint_no_handler_for_type), Toast.LENGTH_LONG).show();
        	}
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBound = false;
    }

    @Override
    public void onDestroy() {
        if (mBound) {
            getLoaderManager().destroyLoader(FILE_LOADER_ID);
            getActivity().unbindService(mConnection);
            mBound = false;
        }
        
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        if (!mBound) {
            getActivity().bindService(new Intent(getActivity(), DtnService.class), mConnection, Context.BIND_AUTO_CREATE);
            mBound = true;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new PackageFileLoader(getActivity(), mService, getArguments().getLong("download_id", -1));
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
        
        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }
}
