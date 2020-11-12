package de.tubs.ibr.dtn.sharebox;

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import de.tubs.ibr.dtn.sharebox.data.EIDDao;
import de.tubs.ibr.dtn.sharebox.data.EIDDatabase;
import de.tubs.ibr.dtn.sharebox.data.EIDEntity;
import de.tubs.ibr.dtn.sharebox.ui.SelectDestinationActivity;

import com.bazaarvoice.jackson.rison.RisonFactory;
import com.bazaarvoice.jackson.rison.RisonGenerator;
import com.bazaarvoice.jackson.rison.RisonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SyncEIDActivity extends Activity {

    EIDDatabase db;
    EIDDao dao;

    String TAG = "SyncEIDActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String rawData = intent.getData().getQuery();
        //Log.d(TAG, data);

        ObjectMapper O_RISON = new ObjectMapper(new RisonFactory().
                enable(RisonGenerator.Feature.O_RISON).
                enable(RisonParser.Feature.O_RISON));
        Map data = null;
        try {
            //Log.d(TAG, "start map");
            data = O_RISON.readValue(rawData, Map.class);
            //Log.d(TAG, O_RISON.writeValueAsString(map));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, data.toString());

        //Log.d(TAG, map.get("eid").getClass().toString());

        //for (Object m: (ArrayList)map.get("eid")){
        //    Log.d(TAG, ((Map)m).get("eid").toString());
        //}

        db = Room.databaseBuilder(getApplicationContext(), EIDDatabase.class, "eid-database").build();
        dao = db.eiddao();

        //Log.d(TAG, "getAll");
        //new SyncEIDActivity.DBTask(db, dao).execute(0);
        String team_id = (String)data.get("team_id");
        ArrayList list = (ArrayList)data.get("users");
        CountDownLatch countDownLatch = new CountDownLatch(list.size());
        for (Object d: list){
            Log.d(TAG, "User info.");
            String id = ((Map)d).get("id").toString();
            String eid = ((Map)d).get("eid").toString();
            String real_name = ((Map)d).get("real_name").toString();
            EIDEntity user = new EIDEntity();
            user.slackWorkspaceId = team_id;
            user.slackUserId = id;
            user.Eid = eid;
            user.slackUseName = real_name;
            new DBTask(db, dao, countDownLatch, user).execute(0);
        }

        try {
            Log.d(TAG, "await");
            countDownLatch.await();
            Toast.makeText(this, "Sync!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Sync!");
        } catch (Exception e) {
            Log.e(TAG, e.getClass().toString());
            e.printStackTrace();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    class DBTask extends AsyncTask<Integer, Integer, Integer> {

        EIDDatabase db;
        EIDDao dao;
        EIDEntity user;

        CountDownLatch countDownLatch;

        public DBTask(EIDDatabase db, EIDDao dao, CountDownLatch countDownLatch,
                      EIDEntity user){
            super();
            this.db = db;
            this.dao = dao;
            this.countDownLatch = countDownLatch;
            this.user = user;
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            // insert
            try {
                dao.insert(user);
            } catch (SQLiteConstraintException e) {
                Log.e(TAG, e.getClass().toString());
                dao.update(user);
                Log.d(TAG, "Already exist. So, update.");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            countDownLatch.countDown();
            Log.d(TAG, "Finish async task!");
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }
    }
}
