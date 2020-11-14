package de.tubs.ibr.dtn.sharebox.ui;

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import de.tubs.ibr.dtn.api.Node;
import de.tubs.ibr.dtn.api.SingletonEndpoint;
import de.tubs.ibr.dtn.sharebox.R;
import de.tubs.ibr.dtn.sharebox.SyncEIDActivity;
import de.tubs.ibr.dtn.sharebox.data.EIDDao;
import de.tubs.ibr.dtn.sharebox.data.EIDDatabase;
import de.tubs.ibr.dtn.sharebox.data.EIDEntity;

import de.tubs.ibr.dtn.sharebox.ui.DestinationRecycleViewAdapter;
import de.tubs.ibr.dtn.sharebox.ui.DestinationRowData;
import android.support.v7.widget.LinearLayoutManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SelectDestinationActivity extends Activity {
    //Button button_send;
    //EditText text;
    Intent intent;

    EIDDatabase db;
    EIDDao dao;

    Button button_test;
    Button button_test2;
    Button button_test3;
    Button button_test4;
    EditText text2;

    DBTask task;

    List<EIDEntity> dbList;
    String ownId;

    final String TAG = "SelectDestination";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_destination);

        db = Room.databaseBuilder(getApplicationContext(), EIDDatabase.class, "eid-database").build();
        dao = db.eiddao();

        //button_send = (Button)findViewById(R.id.select_destination_button_send);
        //text = (EditText) findViewById(R.id.select_destination_edittext);
        /*
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
        */

        // test buttons
        text2 = (EditText) findViewById(R.id.select_destination_edittext2);
        button_test = (Button)findViewById(R.id.select_destination_button_test);
        button_test2 = (Button)findViewById(R.id.select_destination_button_test2);
        button_test3 = (Button)findViewById(R.id.select_destination_button_test3);
        button_test4 = (Button)findViewById(R.id.select_destination_button_test4);

        button_test.setOnClickListener(testListener);
        button_test2.setOnClickListener(testListener);
        button_test3.setOnClickListener(testListener);
        button_test4.setOnClickListener(testListener);

        // user list
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new DBTask(db, dao, countDownLatch).execute(4);
        try {
            countDownLatch.await();
        } catch (Exception e) {
            Log.e(TAG, "await error!!!");
            e.printStackTrace();
        }

        RecyclerView rv = (RecyclerView)findViewById(R.id.destination_recycler_view);
        DestinationRecycleViewAdapter adapter = new DestinationRecycleViewAdapter(this.createData(dbList), this){
            @Override
            void onViewClick(View v, int position) {
                Intent intent = new Intent();
                // Create destination Node
                Node n = new Node();
                String eid = list.get(position).getEid();
                n.endpoint = new SingletonEndpoint(eid);
                n.type = "NODE_DISCOVERED";
                // Put Node into Intent
                intent.putExtra(de.tubs.ibr.dtn.Intent.EXTRA_NODE, n);
                Toast.makeText(activityContext, eid, Toast.LENGTH_SHORT).show();
                // Return to ShareWithActivity
                setResult(RESULT_OK, intent);

                String id = list.get(position).getUserId();
                // Send message in channel 'DTN app'
                new DBTask(db, dao, id, getIntent().getStringExtra("eid")).execute(5);

                finish();
            }
        };

        LinearLayoutManager llm = new LinearLayoutManager(this);

        rv.setHasFixedSize(true);
        rv.setLayoutManager(llm);
        rv.setAdapter(adapter);

        DividerItemDecoration itemDecoration = new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        rv.addItemDecoration(itemDecoration);
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
                    Log.d(TAG, "4: POST message!!!!!");
                    //CountDownLatch countDownLatch = new CountDownLatch(1);
                    //new DBTask(eid).execute(5);
                    //try {
                    //    countDownLatch.await();
                    //} catch (Exception e){
                    //    Log.e(TAG, e.getClass().toString());
                    //    e.printStackTrace();
                    //}
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
        String text2;
        CountDownLatch countDownLatch;

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
        public DBTask(EIDDatabase db, EIDDao dao, CountDownLatch countDownLatch){
            super();
            this.db = db;
            this.dao = dao;
            this.countDownLatch = countDownLatch;
        }
        public DBTask(String destinationId){
            super();
            this.text = destinationId;
        }
/*
        public DBTask(CountDownLatch countDownLatch){
            super();
            this.countDownLatch = countDownLatch;
        }
*/
        public DBTask(){
            super();
        }
        public DBTask(EIDDatabase db, EIDDao dao, String destinationId, String eid){
            super();
            this.db = db;
            this.dao = dao;
            this.text = destinationId;
            this.text2 = eid;
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
                case 4:
                    dbList = dao.getAll();
                    countDownLatch.countDown();
                    break;

                case 5:
                    String name = dao.searchFromEid(text2).slackUseName;

                    String result = post("https://slack.com/api/conversations.open",
                        "token=xoxb-660008667248-1489147300195-r95c0sR806MWYmzryCOa3Ptj" +
                                "&users=" + text);
                    String channelId = "";
                    try {
                        ObjectMapper mapper = new ObjectMapper();

                        //convert JSON string to Map
                        channelId = ((Map)mapper.readValue(result, Map.class).get("channel")).get("id").toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    result = post("https://slack.com/api/chat.postMessage",
                            "token=xoxb-660008667248-1489147300195-r95c0sR806MWYmzryCOa3Ptj" +
                                    "&channel=" + channelId +
                                    "&text=" + convertToOiginal(name) + " が何か渡したいらしい...．");
                    Log.d(TAG, result);

                    /*
                    post("https://slack.com/api/chat.postMessage",
                            "token=xoxb-660008667248-1489147300195-r95c0sR806MWYmzryCOa3Ptj" +
                                    "&channel=D01E665K4ES" +
                                    "&text=送ったよ");
                    */
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

        protected String post(String strUrl, String param){
            final int TIMEOUT_MILLIS = 0;
            final StringBuffer sb = new StringBuffer("");

            HttpURLConnection httpConn = null;
            BufferedReader br = null;
            InputStream is = null;
            InputStreamReader isr = null;

            try {
                URL url = new URL(strUrl);
                httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setConnectTimeout(TIMEOUT_MILLIS);
                httpConn.setReadTimeout(TIMEOUT_MILLIS);
                httpConn.setRequestMethod("POST");
                httpConn.setUseCaches(false);
                httpConn.setDoOutput(true);
                httpConn.setDoInput(true);

                OutputStream os = httpConn.getOutputStream();
                final boolean autoFlash = true;
                PrintStream ps = new PrintStream(os, autoFlash, "UTF-8");
                ps.print(param);
                ps.close();

                final int responseCode = httpConn.getResponseCode();
                Log.d(TAG, "responseCode: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "HTTP OK!!");
                    is = httpConn.getInputStream();
                    isr = new InputStreamReader(is, "UTF-8");
                    br = new BufferedReader(isr);
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                } else {
                    // If responseCode is not HTTP_OK
                }

            } catch (Exception e) {
                Log.e(TAG, e.getClass().toString());
                e.printStackTrace();
            } finally {
                //countDownLatch.countDown();
                if (br != null) try { br.close(); } catch (IOException e) { }
                if (isr != null) try { isr.close(); } catch (IOException e) { }
                if (is != null) try { is.close(); } catch (IOException e) { }
                if (httpConn != null) httpConn.disconnect();
                Log.d(TAG, "4: POST message END!!!!!");
            }

            return sb.toString();
        }
    }

    private List<DestinationRowData> createData(List<EIDEntity> dbList) {

        List<DestinationRowData> dataset = new ArrayList<>();
        for (EIDEntity d: dbList) {
            DestinationRowData data = new DestinationRowData();
            data.setTeamId(d.slackWorkspaceId);
            data.setUserId(d.slackUserId);
            data.setEid(d.Eid);
            data.setRealName(convertToOiginal(d.slackUseName));

            dataset.add(data);
        }
        return dataset;
    }

    private static String convertToOiginal(String unicode) {
        String[] codeStrs = unicode.split("\\\\u");
        String result = "";
        for (int i = 1; i < codeStrs.length; i++) {
            result = result + String.valueOf(Character.toChars(Integer.parseInt(codeStrs[i])));
        }
        return result;
    }
}
