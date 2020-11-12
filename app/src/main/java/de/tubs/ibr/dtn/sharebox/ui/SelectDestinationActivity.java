package de.tubs.ibr.dtn.sharebox.ui;

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bazaarvoice.jackson.rison.RisonFactory;
import com.bazaarvoice.jackson.rison.RisonGenerator;
import com.bazaarvoice.jackson.rison.RisonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.tubs.ibr.dtn.api.Node;
import de.tubs.ibr.dtn.api.SingletonEndpoint;
import de.tubs.ibr.dtn.sharebox.R;
import de.tubs.ibr.dtn.sharebox.SyncEIDActivity;
import de.tubs.ibr.dtn.sharebox.data.EIDDao;
import de.tubs.ibr.dtn.sharebox.data.EIDDatabase;
import de.tubs.ibr.dtn.sharebox.data.EIDEntity;

public class SelectDestinationActivity extends Activity {
    Button button_send;
    EditText text;
    Intent intent;

    EIDDatabase db;
    EIDDao dao;

    Button button_test;
    Button button_test2;
    Button button_test3;
    Button button_test4;
    EditText text2;

    DBTask task;

    final String TAG = "SelectDestination";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_destination);

        db = Room.databaseBuilder(getApplicationContext(), EIDDatabase.class, "eid-database").build();
        dao = db.eiddao();

        button_send = (Button)findViewById(R.id.select_destination_button_send);
        text = (EditText) findViewById(R.id.select_destination_edittext);

        button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent();
                // Create destination Node
                Node n = new Node();
                n.endpoint = new SingletonEndpoint(text.getText().toString());
                n.type = "NODE_DISCOVERED";
                // Put Node into Intent
                intent.putExtra(de.tubs.ibr.dtn.Intent.EXTRA_NODE, n);
                // Return to ShareWithActivity
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        text2 = (EditText) findViewById(R.id.select_destination_edittext2);
        button_test = (Button)findViewById(R.id.select_destination_button_test);
        button_test2 = (Button)findViewById(R.id.select_destination_button_test2);
        button_test3 = (Button)findViewById(R.id.select_destination_button_test3);
        button_test4 = (Button)findViewById(R.id.select_destination_button_test4);

        button_test.setOnClickListener(testListener);
        button_test2.setOnClickListener(testListener);
        button_test3.setOnClickListener(testListener);
        button_test4.setOnClickListener(testListener);
    }

    View.OnClickListener testListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.select_destination_button_test:
                    Log.d(TAG, "DB:1: Insert");
                    new DBTask(db, dao, "test" + text2.getText().toString()).execute(1);
                    break;
                case R.id.select_destination_button_test2:
                    Log.d(TAG, "DB:2: getAll");
                    new DBTask(db, dao).execute(2);
                    break;
                case R.id.select_destination_button_test3:
                    Log.d(TAG, "DB:3: deleteAll");
                    new DBTask(db, dao).execute(3);
                    break;
                case R.id.select_destination_button_test4:
                    Log.d(TAG, "Click4");
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    class DBTask extends AsyncTask<Integer, Integer, Integer> {

        EIDDatabase db;
        EIDDao dao;
        String text;

        public DBTask(EIDDatabase db, EIDDao dao){
            super();
            this.db = db;
            this.dao = dao;
        }
        public DBTask(EIDDatabase db, EIDDao dao, String text){
            super();
            this.db = db;
            this.dao = dao;
            this.text = text;
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            switch (integers[0]) {
                case 1:
                    EIDEntity testUser = new EIDEntity();
                    testUser.slackWorkspaceId = "work_space_id";
                    testUser.slackUserId = text;
                    testUser.Eid = "eid";
                    testUser.slackUseName = "name";
                    try {
                        dao.insert(testUser);
                    } catch (Exception e) {
                        Log.d(TAG, "Already exist");
                    }
                    break;
                case 2:
                    List<EIDEntity> list = dao.getAll();
                    for (EIDEntity user: list) {
                        Log.d(TAG, "ユーザー");
                        //Log.d(TAG, user.toString());
                        System.out.println(user.slackWorkspaceId + ":" + user.slackUserId + ":"
                                + user.Eid + ":" + user.slackUseName);
                    }
                    break;
                case 3:
                    dao.deleteAll();
                    break;
                default:
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            Log.d(TAG, "Finish async task!");
        }
    }
}
