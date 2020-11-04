package de.tubs.ibr.dtn.sharebox.data;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import de.tubs.ibr.dtn.api.BundleID;
import de.tubs.ibr.dtn.sharebox.ui.DownloadAdapter;
import de.tubs.ibr.dtn.sharebox.ui.PackageFileAdapter;

@SuppressLint("SimpleDateFormat")
public class Database {

    private final static String TAG = "Database";
    
    public final static String NOTIFY_DATABASE_UPDATED = "de.tubs.ibr.dtn.sharebox.DATABASE_UPDATED"; 
 
    private DBOpenHelper mHelper = null;
    private SQLiteDatabase mDatabase = null;
    private Context mContext = null;
    
    private static final String DATABASE_NAME = "transmissions";
    public static final String[] TABLE_NAMES = { "download", "files" };
    
    private static final int DATABASE_VERSION = 3;
    
    // Database creation sql statement
    private static final String DATABASE_CREATE_DOWNLOAD = 
            "CREATE TABLE " + TABLE_NAMES[0] + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Download.SOURCE + " TEXT NOT NULL, " +
                Download.DESTINATION + " TEXT NOT NULL, " +
                Download.TIMESTAMP + " TEXT, " +
                Download.LIFETIME + " INTEGER NOT NULL, " +
                Download.LENGTH + " INTEGER NOT NULL, " +
                Download.STATE + " INTEGER, " +
                Download.BUNDLE_ID + " TEXT NOT NULL" +
            ");";
    
    private static final String DATABASE_CREATE_FILES = 
            "CREATE TABLE " + TABLE_NAMES[1] + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                PackageFile.DOWNLOAD + " INTEGER NOT NULL, " +
                PackageFile.FILENAME + " TEXT NOT NULL, " +
                PackageFile.LENGTH + " INTEGER NOT NULL " +
            ");";
    
    public SQLiteDatabase getDB() {
        return mDatabase;
    }
    
    private class DBOpenHelper extends SQLiteOpenHelper {
        
        public DBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE_DOWNLOAD);
            db.execSQL(DATABASE_CREATE_FILES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(DBOpenHelper.class.getName(),
                    "Upgrading database from version " + oldVersion + " to "
                            + newVersion + ", which will destroy all old data");
            
            for (String table : TABLE_NAMES) {
                db.execSQL("DROP TABLE IF EXISTS " + table);
            }
            onCreate(db);
        }
    };
    
    public Database(Context context) throws SQLException
    {
        mHelper = new DBOpenHelper(context);
        mDatabase = mHelper.getWritableDatabase();
        mContext = context;
    }
    
    public void notifyDataChanged() {
        Intent i = new Intent(NOTIFY_DATABASE_UPDATED);
        mContext.sendBroadcast(i);
    }
    
    public void close()
    {
        mDatabase.close();
        mHelper.close();
    }
    
    public Download getLatestPending() {
        Download ret = null;
               
        try {
            Cursor cur = mDatabase.query(
                    Database.TABLE_NAMES[0], 
                    DownloadAdapter.PROJECTION, 
                    Download.STATE + " = ?", 
                    new String[] { String.valueOf(Download.State.PENDING.getValue()) },
                    null, null, Download.TIMESTAMP + " DESC", "0, 1");
            
            if (cur.moveToNext())
            {
                ret = new Download(mContext, cur, new DownloadAdapter.ColumnsMap());
            }
            
            cur.close();
        } catch (Exception e) {
            // download not found
            Log.e(TAG, "get() failed", e);
        }
        
        return ret;
    }
    
    public Download getDownload(BundleID id) {
        Download d = null;
        
        try {
            Cursor cur = mDatabase.query(Database.TABLE_NAMES[0], DownloadAdapter.PROJECTION, Download.BUNDLE_ID + " = ?", new String[] { id.toString() }, null, null, null, "0, 1");
            
            if (cur.moveToNext())
            {
                d = new Download(mContext, cur, new DownloadAdapter.ColumnsMap());
            }
            
            cur.close();
        } catch (Exception e) {
            // download not found
            Log.e(TAG, "get() failed", e);
        }
        
        return d;
    }
    
    public Download getDownload(Long downloadId) {
        Download d = null;
        
        try {
            Cursor cur = mDatabase.query(Database.TABLE_NAMES[0], DownloadAdapter.PROJECTION, Download.ID + " = ?", new String[] { downloadId.toString() }, null, null, null, "0, 1");
            
            if (cur.moveToNext())
            {
                d = new Download(mContext, cur, new DownloadAdapter.ColumnsMap());
            }
            
            cur.close();
        } catch (Exception e) {
            // download not found
            Log.e(TAG, "get() failed", e);
        }
        
        return d;
    }

    public List<Download> getDownloads() {
        LinkedList<Download> downloads = new LinkedList<Download>();

        try {
            Cursor cur = mDatabase.query(Database.TABLE_NAMES[0], DownloadAdapter.PROJECTION, null, null, null, null, null);

            while (cur.moveToNext())
            {
                downloads.add(new Download(mContext, cur, new DownloadAdapter.ColumnsMap()));
            }

            cur.close();
        } catch (Exception e) {
            // error
            Log.e(TAG, "getDownloads() failed", e);
        }

        return downloads;
    }

    public PackageFile getFile(Long fileId) {
        PackageFile ret = null;

        try {
            Cursor cur = mDatabase.query(Database.TABLE_NAMES[1], PackageFileAdapter.PROJECTION, PackageFile.ID + " = ?", new String[] { fileId.toString() }, null, null, null);

            if (cur.moveToNext())
            {
                ret = new PackageFile(mContext, cur, new PackageFileAdapter.ColumnsMap());
            }

            cur.close();
        } catch (Exception e) {
            // error
            Log.e(TAG, "getFile() failed", e);
        }

        return ret;
    }
    
    public List<PackageFile> getFiles(Long downloadId) {
        LinkedList<PackageFile> files = new LinkedList<PackageFile>();
        
        try {
            Cursor cur = mDatabase.query(Database.TABLE_NAMES[1], PackageFileAdapter.PROJECTION, PackageFile.DOWNLOAD + " = ?", new String[] { downloadId.toString() }, null, null, null);
            
            while (cur.moveToNext())
            {
                files.add( new PackageFile(mContext, cur, new PackageFileAdapter.ColumnsMap()) );
            }
            
            cur.close();
        } catch (Exception e) {
            // error
            Log.e(TAG, "getFiles() failed", e);
        }
        
        return files;
    }
    
    public Long put(Download d) {
        ContentValues values = new ContentValues();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        values.put(Download.SOURCE, d.getSource());
        values.put(Download.DESTINATION, d.getDestination());
        values.put(Download.TIMESTAMP, dateFormat.format(d.getTimestamp()));
        values.put(Download.LIFETIME, d.getLifetime());
        values.put(Download.LENGTH, d.getLength());
        values.put(Download.STATE, d.getState().getValue());
        
        // write bundle id to bundle_id
        values.put(Download.BUNDLE_ID, d.getBundleId().toString());
        
        // store the message in the database
        Long id = mDatabase.insert(Database.TABLE_NAMES[0], null, values);
        
        notifyDataChanged();
        
        return id;
    }
    
    public Long put(BundleID bundle_id, File f) {
        ContentValues values = new ContentValues();
        
        Download d = getDownload(bundle_id);
        
        if (d == null) return null;

        values.put(PackageFile.DOWNLOAD, d.getId());
        values.put(PackageFile.FILENAME, f.getAbsolutePath());
        values.put(PackageFile.LENGTH, f.length());
        
        // store the message in the database
        Long id = mDatabase.insert(Database.TABLE_NAMES[1], null, values);
        
        notifyDataChanged();
        
        // add new file to the media library
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
        
        return id;
    }
    
    public Integer getPending() {
        Integer ret = 0;
        
        try {
            Cursor cur = mDatabase.query(
                    Database.TABLE_NAMES[0],
                    new String[] { "COUNT(*)" }, 
                    Download.STATE + " = ?", 
                    new String[] { String.valueOf( Download.State.PENDING.getValue() ) },
                    null, null, null);

            if (cur.moveToNext())
            {
                ret = cur.getInt(0);
            }
            
            cur.close();
        } catch (Exception e) {
            // error
            Log.e(TAG, "getPending() failed", e);
        }
        
        return ret;
    }
    
    public void setState(Long id, Download.State state) {
        ContentValues values = new ContentValues();

        values.put(Download.STATE, state.getValue());

        // update message data
        mDatabase.update(Database.TABLE_NAMES[0], values, "_id = ?", new String[]{String.valueOf(id)});

        notifyDataChanged();
    }
    
    public void remove(PackageFile pf) {
    	File f = pf.getFile();

    	// delete from database
    	mDatabase.delete(Database.TABLE_NAMES[1], PackageFile.ID + " = ?", new String[]{pf.getId().toString()});
    	
    	// delete from storage
    	f.delete();

        // removed file from media library
        removeFromMediaStore(f);
    	
    	notifyDataChanged();
    }
    
    public void remove(BundleID id) {
        Download d = getDownload(id);
        if (d == null) return;
        remove(d.getId());
    }
    
    public void remove(Long downloadId) {
        List<PackageFile> files = getFiles(downloadId);

        // delete the files
        for (PackageFile pf : files) {
            File f = pf.getFile();

            // delete from storage
            f.delete();

            // removed file from media library
            removeFromMediaStore(f);
        }

        // delete all files from database
        mDatabase.delete(Database.TABLE_NAMES[1], PackageFile.DOWNLOAD + " = ?", new String[] { downloadId.toString() });

        // delete the download from database
        mDatabase.delete(Database.TABLE_NAMES[0], Download.ID + " = ?", new String[] { downloadId.toString() });

        notifyDataChanged();
    }

    private void removeFromMediaStore(File f) {
        // get content Uri for the deleted file
        Uri[] uris = {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        };

        for (Uri uri : uris) {
            // removed file from media library
            mContext.getContentResolver().delete(uri,
                    MediaStore.MediaColumns.DATA + " = ?", new String[]{f.getAbsolutePath()});
        }
    }
}
