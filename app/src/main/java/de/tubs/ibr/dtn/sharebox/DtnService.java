package de.tubs.ibr.dtn.sharebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.preference.PreferenceManager;
import android.util.Log;

import de.tubs.ibr.dtn.SecurityService;
import de.tubs.ibr.dtn.SecurityUtils;
import de.tubs.ibr.dtn.Services;
import de.tubs.ibr.dtn.api.Block;
import de.tubs.ibr.dtn.api.Bundle;
import de.tubs.ibr.dtn.api.BundleID;
import de.tubs.ibr.dtn.api.DTNClient;
import de.tubs.ibr.dtn.api.DTNClient.Session;
import de.tubs.ibr.dtn.api.DTNIntentService;
import de.tubs.ibr.dtn.api.DataHandler;
import de.tubs.ibr.dtn.api.EID;
import de.tubs.ibr.dtn.api.GroupEndpoint;
import de.tubs.ibr.dtn.api.Registration;
import de.tubs.ibr.dtn.api.ServiceNotAvailableException;
import de.tubs.ibr.dtn.api.SessionDestroyedException;
import de.tubs.ibr.dtn.api.SingletonEndpoint;
import de.tubs.ibr.dtn.api.TransferMode;
import de.tubs.ibr.dtn.sharebox.data.Database;
import de.tubs.ibr.dtn.sharebox.data.Download;
import de.tubs.ibr.dtn.sharebox.data.Download.State;
import de.tubs.ibr.dtn.sharebox.data.PackageFile;
import de.tubs.ibr.dtn.sharebox.data.TruncatedInputStream;
import de.tubs.ibr.dtn.sharebox.data.Utils;

public class DtnService extends DTNIntentService {

    // This TAG is used to identify this class (e.g. for debugging)
    private static final String TAG = "DtnService";
    
    // mark a specific bundle as delivered
    public static final String MARK_DELIVERED_INTENT = "de.tubs.ibr.dtn.sharebox.MARK_DELIVERED";
    
    // process a status report
    public static final String REPORT_DELIVERED_INTENT = "de.tubs.ibr.dtn.sharebox.REPORT_DELIVERED";
    
    // download or rejet a bundle
    public static final String ACCEPT_DOWNLOAD_INTENT = "de.tubs.ibr.dtn.sharebox.ACCEPT_DOWNLOAD";
    public static final String REJECT_DOWNLOAD_INTENT = "de.tubs.ibr.dtn.sharebox.REJECT_DOWNLOAD";

    // delete files or downloads
    public static final String DELETE_ALL_INTENT = "de.tubs.ibr.dtn.sharebox.DELETE_ALL_INTENT";

    public static final String DELETE_FILE_INTENT = "de.tubs.ibr.dtn.sharebox.DELETE_FILE_INTENT";
    public static final String EXTRA_KEY_FILE_ID = "fileid";

    public static final String DELETE_DOWNLOAD_INTENT = "de.tubs.ibr.dtn.sharebox.DELETE_DOWNLOAD_INTENT";
    public static final String EXTRA_KEY_DOWNLOAD_ID = "downloadid";
    
    // local endpoint
    public static final String SHAREBOX_APP_ENDPOINT = "sharebox";
    
    // group EID of this app
    public static final GroupEndpoint SHAREBOX_GROUP_EID = new GroupEndpoint("dtn://broadcast.dtn/sharebox");
    
    public static final String EXTRA_KEY_BUNDLE_ID = "bundleid";
    public static final String EXTRA_KEY_SOURCE = "source";
    public static final String EXTRA_KEY_LENGTH = "length";
    public static final String EXTRA_KEY_LIFETIME = "lifetime";
    public static final String EXTRA_KEY_AUTO_ACCEPT = "auto-accept";

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    
    // The communication with the DTN service is done using the DTNClient
    private DTNClient.Session mSession = null;

    // Security API provided by IBR-DTN
    private SecurityService mSecurityService = null;
    private boolean mSecurityBound = false;
    
    // should be set to true if a download is requested 
    private Boolean mIsDownloading = false;
    
    // handle of the notification manager
    NotificationFactory mNotificationFactory = null;
    
    private ServiceError mServiceError = ServiceError.NO_ERROR;
    
    // the database
    private Database mDatabase = null;

    private ServiceConnection mSecurityConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSecurityService = SecurityService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName name) {
            mSecurityService = null;
        }
    };
    
    public DtnService() {
        super(TAG);
    }
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public DtnService getService() {
            return DtnService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    public ServiceError getServiceError() {
        return mServiceError;
    }
    
    public Database getDatabase() {
        return mDatabase;
    }
    
    public PendingIntent getSelectNeighborIntent() {
        // get pending intent for neighbor list
        return getClient().getSelectNeighborIntent();
    }

    // for register EID to slack
    public String getClientEndpoint() {
        return getClient().getEndpoint();
    }

    public boolean isTrusted(Bundle b) {
        // can not check trust relation without the security service
        if (mSecurityService == null) {
            Log.d(TAG, "Security service not available");
            return false;
        }

        if (b.get(Bundle.ProcFlags.DTNSEC_STATUS_VERIFIED)) {
            // check if this peer is trusted
            android.os.Bundle keyinfo = SecurityUtils.getSecurityInfo(mSecurityService, b.getSource().toString());
            if (keyinfo == null) return false;
            if (!keyinfo.containsKey(SecurityUtils.EXTRA_TRUST_LEVEL)) return false;

            // retrieve trust level
            Integer trust_level = keyinfo.getInt(SecurityUtils.EXTRA_TRUST_LEVEL);

            Log.d(TAG, "Trust level is " + trust_level.toString());

            // we trust the peer if the level is above 67 (green)
            return (trust_level > 67);
        } else {
            Log.d(TAG, "Received bundle is not signed");
        }

        return false;
    }
    
    @Override
    protected synchronized void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        
        if (de.tubs.ibr.dtn.Intent.RECEIVE.equals(action))
        {
            // Receive bundle info from the DTN service here
            try {
                // We loop here until no more bundles are available
                // (queryNext() returns false)
                mIsDownloading = false;
                while (mSession.queryInfoNext());
            } catch (SessionDestroyedException e) {
                Log.e(TAG, "Can not query for bundle", e);
            }
        }
        else if (MARK_DELIVERED_INTENT.equals(action))
        {
            // retrieve the bundle ID of the intent
            BundleID bundleid = intent.getParcelableExtra(EXTRA_KEY_BUNDLE_ID);
            
            try {
                // mark the bundle ID as delivered
                mSession.delivered(bundleid);
            } catch (Exception e) {
                Log.e(TAG, "Can not mark bundle as delivered.", e);
            }
        }
        else if (REPORT_DELIVERED_INTENT.equals(action))
        {
            // retrieve the source of the status report
            SingletonEndpoint source = intent.getParcelableExtra(EXTRA_KEY_SOURCE);
            
            // retrieve the bundle ID of the intent
            BundleID bundleid = intent.getParcelableExtra(EXTRA_KEY_BUNDLE_ID);
            
            Log.d(TAG, "Status report received for " + bundleid.toString() + " from " + source.toString());
        }
        else if (REJECT_DOWNLOAD_INTENT.equals(action))
        {           
            // retrieve the bundle ID of the intent
            BundleID bundleid = intent.getParcelableExtra(EXTRA_KEY_BUNDLE_ID);
            
            // delete the pending bundle
            mDatabase.remove(bundleid);
            
            // update pending download notification
            updatePendingDownloadNotification();
            
            // mark the bundle as delivered
            Intent i = new Intent(DtnService.this, DtnService.class);
            i.setAction(MARK_DELIVERED_INTENT);
            i.putExtra(EXTRA_KEY_BUNDLE_ID, bundleid);
            startService(i);
        }
        else if (DELETE_ALL_INTENT.equals(action))
        {
            // get all downloads from the database
            List<Download> downloads = mDatabase.getDownloads();

            // create a delete intent for each of it
            for (Download d : downloads) {
                // skip pending downloads
                if (d.isPending()) continue;

                // queue delete intent
                Intent deleteIntent = new Intent(DtnService.this, DtnService.class);
                deleteIntent.setAction(DtnService.DELETE_DOWNLOAD_INTENT);
                deleteIntent.putExtra(DtnService.EXTRA_KEY_DOWNLOAD_ID, d.getId());
                deleteIntent.putExtra(DtnService.EXTRA_KEY_BUNDLE_ID, d.getBundleId());
                startService(deleteIntent);
            }
        }
        else if (DELETE_DOWNLOAD_INTENT.equals(action))
        {
            Long id = intent.getLongExtra(EXTRA_KEY_DOWNLOAD_ID, 0);

            // retrieve the bundle ID of the intent
            BundleID bundleid = intent.getParcelableExtra(EXTRA_KEY_BUNDLE_ID);

            // delete the download
            mDatabase.remove(id);

            // cancel notifications
            mNotificationFactory.cancelDownload(bundleid);
        }
        else if (DELETE_FILE_INTENT.equals(action))
        {
            Long id = intent.getLongExtra(EXTRA_KEY_FILE_ID, 0);

            // first get the corresponding file
            PackageFile pf = mDatabase.getFile(id);

            // remove the file
            if (pf != null) mDatabase.remove(pf);
        }
        else if (ACCEPT_DOWNLOAD_INTENT.equals(action))
        {
            // retrieve the bundle ID of the intent
            BundleID bundleid = intent.getParcelableExtra(EXTRA_KEY_BUNDLE_ID);
            
            Log.d(TAG, "Download request for " + bundleid.toString());
            
            // mark the download as accepted
            Download d = mDatabase.getDownload(bundleid);
            mDatabase.setState(d.getId(), Download.State.ACCEPTED);

            // get preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            
            // update pending download notification
            updatePendingDownloadNotification();
            
            // show ongoing download notification
            mNotificationFactory.showDownload(d);
            
            try {
                mIsDownloading = true;
                if (mSession.query(bundleid)) {
                    if (prefs.getBoolean("download_notifications", true)) {
                        mNotificationFactory.showDownloadCompleted(d, !intent.getBooleanExtra(EXTRA_KEY_AUTO_ACCEPT, false));
                    } else {
                        mNotificationFactory.cancelDownload(d);
                    }
                } else {
                    // set state to aborted
                    mDatabase.setState(d.getId(), Download.State.ABORTED);
                    
                    mNotificationFactory.showDownloadAborted(d);
                }
            } catch (SessionDestroyedException e) {
                Log.e(TAG, "Can not query for bundle", e);
                
                mNotificationFactory.showDownloadAborted(d);
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
        	// send one or more files as bundle
        	
        	// first check the parameters
        	if (
        			intent.hasExtra(Intent.EXTRA_STREAM) &&
        			intent.hasExtra(de.tubs.ibr.dtn.Intent.EXTRA_ENDPOINT)
        		)
        	{
        		// extract destination and files
        		EID destination = (EID)intent.getSerializableExtra(de.tubs.ibr.dtn.Intent.EXTRA_ENDPOINT);
        		Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        		
        		ArrayList<Uri> uris = new ArrayList<Uri>();
        		uris.add(uri);

                // get default lifetime from preferences
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                Long lifetimeDefault = Long.valueOf(prefs.getString("upload_lifetime", "3600"));

                // create bundle object
                Bundle b = new Bundle();
                b.setDestination(destination);
                b.setLifetime(intent.getLongExtra(EXTRA_KEY_LIFETIME, lifetimeDefault));

                // enable signature if requested
                b.set(Bundle.ProcFlags.DTNSEC_REQUEST_SIGN, prefs.getBoolean("upload_sign", true));

                // enable encryption if requested
                b.set(Bundle.ProcFlags.DTNSEC_REQUEST_ENCRYPT, prefs.getBoolean("upload_encrypt", false));
                
                // forward to common send method
                sendFiles(b, uris);
        	}
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // send one or more files as bundle
            
            // first check the parameters
            if (
                    intent.hasExtra(Intent.EXTRA_STREAM) &&
                    intent.hasExtra(de.tubs.ibr.dtn.Intent.EXTRA_ENDPOINT)
                )
            {
                // extract destination and files
                EID destination = (EID)intent.getSerializableExtra(de.tubs.ibr.dtn.Intent.EXTRA_ENDPOINT);
                ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

                // get default lifetime from preferences
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                Long lifetimeDefault = Long.valueOf(prefs.getString("upload_lifetime", "3600"));

                // create bundle object
                Bundle b = new Bundle();
                b.setDestination(destination);
                b.setLifetime(intent.getLongExtra(EXTRA_KEY_LIFETIME, lifetimeDefault));

                // enable signature if requested
                b.set(Bundle.ProcFlags.DTNSEC_REQUEST_SIGN, prefs.getBoolean("upload_sign", true));

                // enable encryption if requested
                b.set(Bundle.ProcFlags.DTNSEC_REQUEST_ENCRYPT, prefs.getBoolean("upload_encrypt", false));

                // forward to common send method
                sendFiles(b, uris);
            }
        }
    }
    
    private void sendFiles(final Bundle bundle, final ArrayList<Uri> uris) {
        // show upload notification
        mNotificationFactory.showUpload(bundle.getDestination(), uris.size());
        
        try {
            // create a pipe
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            
            // create a new output stream for the data
            final OutputStream targetStream = new AutoCloseOutputStream(pipe[1]);
            
            // create a TarCreator and assign default listener to update progress
            TarCreator creator = new TarCreator(this, targetStream, uris);
            creator.setOnStateChangeListener(new TarCreator.OnStateChangeListener() {
            	
            	private int mLastProgressValue = 0;
                
                @Override
                public void onStateChanged(TarCreator creator, int state, Long bytes) {
                    Log.d(TAG, "TarCreator state changed to " + String.valueOf(state));
                    
                    switch (state) {
                        case -1:
                            // close pipes on error
                            try {
                                pipe[0].close();
                                targetStream.close();
                            } catch (Exception e) {
                                
                            }
                            
                            // change send notification into send failed
                            mNotificationFactory.showUploadAborted(bundle.getDestination());
                            break;
                            
                        case 1:
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DtnService.this);
                            if (prefs.getBoolean("upload_notifications", true)) {
                                // change upload notification into send completed
                                mNotificationFactory.showUploadCompleted(uris.size(), bytes);
                            } else {
                                // hide upload notification
                                mNotificationFactory.cancelUpload();
                            }
                            
                            // close write side on success
                            try {
                                targetStream.close();
                            } catch (Exception e) {
                                
                            }
                            break;
                    }
                }
                
                @Override
                public void onFileProgress(TarCreator creator, String currentFile, int currentFileNum, int maxFiles) {
                    Log.d(TAG, "TarCreator processing file " + currentFile);
                    mLastProgressValue = 0;
                    mNotificationFactory.updateUpload(currentFile, currentFileNum, maxFiles);
                }

				@Override
				public void onCopyProgress(TarCreator creator, long offset, long length) {
		            // scale the download progress to 0..100
		            Double pos = Double.valueOf(offset) / Double.valueOf(length) * 100.0;
		            
		            if (mLastProgressValue < pos.intValue()) {
		                mLastProgressValue = pos.intValue();
		                mNotificationFactory.updateUpload(mLastProgressValue, 100);
		            }
				}
            });

            // create a helper thread
            Thread helper = new Thread(creator);
            
            // start the helper thread
            helper.start();
            
            try {
                // send the data
                mSession.send(bundle, pipe[0]);
            } catch (Exception e) {
                pipe[0].close();
                targetStream.close();
                
                // re-throw the exception to the next catch
                throw e;
            } finally {
                // wait until the helper thread is finished
                helper.join();
            }
        } catch (Exception e) {
            Log.e(TAG, "File send failed", e);
        }
    }
    
    @Override
    public void onSessionConnected(Session session) {
        Log.d(TAG, "Session connected");
        
        // register own data handler for incoming bundles
        session.setDataHandler(mDataHandler);
        
        mSession = session;

        // Establish a connection with the security service
        try {
            Services.SERVICE_SECURITY.bind(this, mSecurityConnection, Context.BIND_AUTO_CREATE);
            mSecurityBound = true;
        } catch (ServiceNotAvailableException e) {
            // Security API not available
        }
    }

    @Override
    public void onSessionDisconnected() {
        if (mSecurityBound) {
            unbindService(mSecurityConnection);
            mSecurityBound = false;
        }

        Log.d(TAG, "Session disconnected");
        
        mSession = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // create notification factory
        mNotificationFactory = new NotificationFactory( this, (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE) );
        
        // create message database
        mDatabase = new Database(this);
        
        // create registration with "sharebox" as endpoint
        // if the EID of this device is "dtn://device" then the
        // address of this app will be "dtn://device/sharebox"
        Registration registration = new Registration(SHAREBOX_APP_ENDPOINT);
        
        // additionally join a group
        registration.add(SHAREBOX_GROUP_EID);
        
        try {
            // initialize the connection to the DTN service
            initialize(registration);
            Log.d(TAG, "Connection to DTN service established.");
        } catch (ServiceNotAvailableException e) {
            // The DTN service has not been found
            Log.e(TAG, "DTN service unavailable. Is IBR-DTN installed?", e);
        } catch (SecurityException e) {
            // The service has not been found
            Log.e(TAG, "The app has no permission to access the DTN service. It is important to install the DTN service first and then the app.", e);
        }
    }

    @Override
    public void onDestroy() {
        // close the database
        mDatabase.close();
        
        super.onDestroy();
    }
    
    private void updatePendingDownloadNotification() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // get the latest pending download
        Download next = mDatabase.getLatestPending();
        
        if (next != null) {
            if (prefs.getBoolean("download_notifications", true)) {
                mNotificationFactory.showPendingDownload(next, mDatabase.getPending());
            }
        } else {
            mNotificationFactory.cancelPending();
        }
    }
    
    /**
     * This data handler is used to process incoming bundles
     */
    private DataHandler mDataHandler = new DataHandler() {

        private Bundle mBundle = null;
        private BundleID mBundleId = null;
        private LinkedList<Block> mBlocks = null;
        
        private ParcelFileDescriptor mWriteFd = null;
        private ParcelFileDescriptor mReadFd = null;
        
        private TarExtractor mExtractor = null;
        private Thread mExtractorThread = null;
        
        private int mLastProgressValue = 0;

        @Override
        public void startBundle(Bundle bundle) {
            // store the bundle header locally
            mBundle = bundle;
            mBundleId = new BundleID(bundle);
        }

        @Override
        public void endBundle() {
            if (mIsDownloading) {
                // mark the bundle as delivered if this was a complete download
                Intent i = new Intent(DtnService.this, DtnService.class);
                i.setAction(MARK_DELIVERED_INTENT);
                i.putExtra(EXTRA_KEY_BUNDLE_ID, mBundleId);
                startService(i);

                // set state to completed
                Download d = mDatabase.getDownload(mBundleId);
                mDatabase.setState(d.getId(), Download.State.COMPLETED);
            } else {
                // create new download object
                Download download_request = new Download(mBundle);
                
                // get payload length
                long len = 0L;
                
                if (mBlocks != null) {
                    for (Block b : mBlocks) {
                        if (b.type == 1) {
                            len += b.length;
                            break;
                        }
                    }
                }
                
                // set payload length
                download_request.setLength(len);
                
                // set request to pending
                download_request.setState(State.PENDING);
                
                // put download object into the database
                Long download_id = mDatabase.put(download_request);

                // get preferences
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DtnService.this);

                // automatically accept the download if the peer is
                // trusted and this option is enabled
                if (prefs.getBoolean("download_accept_trusted", false) && isTrusted(mBundle)) {
                    // automatically start the download
                    Intent acceptIntent = new Intent(DtnService.this, DtnService.class);
                    acceptIntent.setAction(DtnService.ACCEPT_DOWNLOAD_INTENT);
                    acceptIntent.putExtra(EXTRA_KEY_AUTO_ACCEPT, true);
                    acceptIntent.putExtra(DtnService.EXTRA_KEY_BUNDLE_ID, mBundleId);
                    Uri downloadUri = Uri.fromParts("download", download_id.toString(), "");
                    acceptIntent.setData(downloadUri);
                    startService(acceptIntent);
                } else {
                    // update pending download notification
                    updatePendingDownloadNotification();
                }
            }
            
            // free the bundle header
            mBundle = null;
            mBundleId = null;
            mBlocks = null;
        }
        
        @Override
        public TransferMode startBlock(Block block) {
            // we are only interested in payload blocks (type = 1)
            if (block.type == 1) {
                // retrieve payload when downloading
                if (mIsDownloading) {
                    File folder = Utils.getStoragePath();
                    
                    // do not store any data if there is no space to store
                    if (folder == null)
                        return TransferMode.NULL;
                    
                    // create new filedescriptor
                    try {
                        ParcelFileDescriptor[] p = ParcelFileDescriptor.createPipe();
                        mReadFd = p[0];
                        mWriteFd = p[1];
                        
                        InputStream is = new TruncatedInputStream(new FileInputStream(mReadFd.getFileDescriptor()), block.length);
                        
                        // create a new tar extractor
                        mExtractor = new TarExtractor(is, folder);
                        mExtractor.setOnStateChangeListener(mExtractorListener);
                        mExtractorThread = new Thread(mExtractor);
                        mExtractorThread.start();
                        
                        // return FILEDESCRIPTOR mode to received the payload using fd()
                        return TransferMode.FILEDESCRIPTOR;
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Can not create a filedescriptor.", e);
                    } catch (IOException e) {
                        Log.e(TAG, "Can not create a filedescriptor.", e);
                    }
                        
                    return TransferMode.NULL;
                }
                
                if (mBlocks == null) {
                    mBlocks = new LinkedList<Block>();
                }
                
                // store the block data
                mBlocks.push(block);

                return TransferMode.NULL;
            } else {
                // return NULL to discard the payload of this block
                return TransferMode.NULL;
            }
        }

        @Override
        public void endBlock() {
            // reset progress
            mLastProgressValue = 0;
            
            if (mWriteFd != null)
            {
                // close filedescriptor
                try {
                    mWriteFd.close();
                    mWriteFd = null;
                } catch (IOException e) {
                    Log.e(TAG, "Can not close filedescriptor.", e);
                }
            }
            
            if (mExtractorThread != null) {
            	try {
					mExtractorThread.join();
				} catch (InterruptedException e) {
					Log.e(TAG, "interrupted in endBlock()", e);
				}
            	mExtractor = null;
            	mExtractorThread = null;
            }
        }

        @Override
        public ParcelFileDescriptor fd() {
            return mWriteFd;
        }

        @Override
        public void payload(byte[] data) {
            // nothing to do here. 
        }

        @Override
        public void progress(long offset, long length) {
            // if payload is written to a file descriptor, the progress
            // will be announced here
            
            // scale the download progress to 0..100
            Double pos = Double.valueOf(offset) / Double.valueOf(length) * 100.0;
            
            if (mIsDownloading && (mLastProgressValue < pos.intValue())) {
                mLastProgressValue = pos.intValue();
                mNotificationFactory.updateDownload(mBundleId, mLastProgressValue, 100);
            }
        }
        
        private TarExtractor.OnStateChangeListener mExtractorListener = new TarExtractor.OnStateChangeListener() {

            @Override
            public void onStateChanged(TarExtractor extractor, int state) {
                switch (state) {
                    case 1:
                        // successful
                        try {
                            mReadFd.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Can not close filedescriptor.", e);
                        }
                        
                        mReadFd = null;
                        
                        // put files into the database
                        for (File f : extractor.getFiles()) {
                            Log.d(TAG, "Extracted file: " + f.getAbsolutePath());
                            mDatabase.put(mBundleId, f);
                        }
                        break;
                        
                    case -1:
                        // error
                        try {
                            mReadFd.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Can not close filedescriptor.", e);
                        }
                        
                        mReadFd = null;
                        break;
                }
            }
            
        };
    };
}
