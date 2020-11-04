package de.tubs.ibr.dtn.sharebox.ui;

import java.io.Serializable;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import de.tubs.ibr.dtn.api.Node;
import de.tubs.ibr.dtn.api.SingletonEndpoint;
import de.tubs.ibr.dtn.sharebox.DtnService;

public class ShareWithActivity extends FragmentActivity {
    
    private static final String TAG = "ShareWithActivity";
    private static final int SELECT_NEIGHBOR = 1;

    // private static final String TEST_EID = "dtn://android-vaio2.dtn/sharebox";
    
    private DtnService mService = null;
    private Boolean mBound = false;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((DtnService.LocalBinder)service).getService();

            PendingIntent pi = mService.getSelectNeighborIntent();
            try {
				startIntentSenderForResult(pi.getIntentSender(), SELECT_NEIGHBOR, null, 0, 0, 0);
			} catch (SendIntentException e1) {
				// error
				e1.printStackTrace();
			}
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    
    private void onNeighborSelected(Node n) {
        String endpoint = n.endpoint.toString();
        
        if (endpoint.startsWith("ipn:")) {
            endpoint = endpoint + ".4066896964";
        } else {
            endpoint = endpoint + "/sharebox";
        }

        Log.d(TAG, "Neighbor selected: " + endpoint);
        SingletonEndpoint destination = new SingletonEndpoint(endpoint);
        
        // intent of the share request
        Intent intent = getIntent();
        
        // close the activity is there is no intent
        if (intent == null) return;
        
        // add selected endpoint and forward the intent to the DtnService
        intent.setClass(this, DtnService.class);
        intent.putExtra(de.tubs.ibr.dtn.Intent.EXTRA_ENDPOINT, (Serializable)destination);
        startService(intent);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (SELECT_NEIGHBOR == requestCode) {
            if ((data != null) && data.hasExtra(de.tubs.ibr.dtn.Intent.EXTRA_NODE)) {
                Node n = data.getParcelableExtra(de.tubs.ibr.dtn.Intent.EXTRA_NODE);
                onNeighborSelected(n);
            }
        }
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBound = false;
    }
    
    @Override
    public void onDestroy() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        
        super.onDestroy();
    }
    
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        Intent intent = getIntent();
        
        if (intent == null) {
        	// if there is no intent, quit directly
        	finish();
        }
        else if (intent.hasExtra(de.tubs.ibr.dtn.Intent.EXTRA_ENDPOINT)) {
        	// forward intent directly if the destination is already specified
            intent.setClass(this, DtnService.class);
            startService(intent);
            finish();
        }
        else {
	        if (!mBound) {
	            bindService(new Intent(this, DtnService.class), mConnection, Context.BIND_AUTO_CREATE);
	            mBound = true;
	        }
        }
    }
    
}
