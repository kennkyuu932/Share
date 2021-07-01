package de.tubs.ibr.dtn.sharebox.ui;

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import de.tubs.ibr.dtn.DTNService;
import de.tubs.ibr.dtn.api.Node;
import de.tubs.ibr.dtn.api.SingletonEndpoint;
import de.tubs.ibr.dtn.sharebox.DtnService;
import de.tubs.ibr.dtn.sharebox.R;
import de.tubs.ibr.dtn.sharebox.SendSlackMessage;
import de.tubs.ibr.dtn.sharebox.data.EIDDao;
import de.tubs.ibr.dtn.sharebox.data.EIDDatabase;
import de.tubs.ibr.dtn.sharebox.data.EIDEntity;

import de.tubs.ibr.dtn.sharebox.ui.DestinationRecycleViewAdapter;
import de.tubs.ibr.dtn.sharebox.ui.DestinationRowData;
import android.support.v7.widget.LinearLayoutManager;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SelectDestinationActivity extends Activity {

    EIDDatabase db;
    EIDDao dao;

    List<EIDEntity> dbList;

    RecyclerView rv;
    DestinationRecycleViewAdapter adapter;

    final String TAG = "SelectDestination";
    final int ASYNC_GET_ALL = 1;
    final int ASYNC_DELETE_ALL = 2;
    final int ASYNC_POST = 3;

    private EditText enterfilename;

    private String fnamehint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_select_destination);
        setTitle("宛先選択");

        enterfilename = findViewById(R.id.EnterFileName);

        Intent f = getIntent();
        fnamehint = f.getStringExtra("fnameHint");
        enterfilename.setHint(fnamehint);
        Log.d(TAG,fnamehint);

        // get database
        db = Room.databaseBuilder(getApplicationContext(), EIDDatabase.class, "eid-database").build();
        dao = db.eiddao();

        // get user list from DB
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new AsyncTasks(db, dao, countDownLatch).execute(ASYNC_GET_ALL);
        try {
            countDownLatch.await();
        } catch (Exception e) {
            Log.e(TAG, "await error!!!");
            e.printStackTrace();
        }

        // set user list view
        rv = (RecyclerView)findViewById(R.id.destination_recycler_view);
        adapter = new DestinationRecycleViewAdapter(this.createData(dbList), this){
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
                //SendSlackMessageクラスでSlackでメッセージを送る
                String message = GetHintFilename() + "を渡したいらしい";
                new SendSlackMessage(db, dao, id, getIntent().getStringExtra("eid"),message).execute(ASYNC_POST);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.select_destination, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_delete_destination) {
            new AsyncTasks(db, dao).execute(ASYNC_DELETE_ALL);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
    protected String GetHintFilename(){
        String sharefilename = enterfilename.getText().toString();
        if(sharefilename.isEmpty()){
            sharefilename = enterfilename.getHint().toString();
        }
        return sharefilename;
    }

    class AsyncTasks extends AsyncTask<Integer, Integer, Integer> {

        EIDDatabase db;
        EIDDao dao;
        String id;
        String eid;
        CountDownLatch countDownLatch;

        public AsyncTasks(EIDDatabase db, EIDDao dao){
            // deleteAll
            super();
            this.db = db;
            this.dao = dao;
        }
        public AsyncTasks(EIDDatabase db, EIDDao dao, CountDownLatch countDownLatch){
            // getAll
            super();
            this.db = db;
            this.dao = dao;
            this.countDownLatch = countDownLatch;
        }
        /*
        public AsyncTasks(EIDDatabase db, EIDDao dao, String destinationId, String eid){
            // POST
            super();
            this.db = db;
            this.dao = dao;
            this.id = destinationId;
            this.eid = eid;
        }

         */

        @Override
        protected Integer doInBackground(Integer... integers) {
            switch (integers[0]) {
                case ASYNC_DELETE_ALL:
                    dao.deleteAll();
                    dbList = dao.getAll();
                    Log.d(TAG, "DB:deleteAll");
                    return ASYNC_DELETE_ALL;
                case ASYNC_GET_ALL:
                    dbList = dao.getAll();
                    Log.d(TAG, "DB:getAll");
                    countDownLatch.countDown();
                    return ASYNC_GET_ALL;
                /*
                case ASYNC_POST:
                    String name = dao.searchFromEid(eid).slackUseName;
                    String sharefilename = enterfilename.getText().toString();


                    if(sharefilename.isEmpty()){
                        sharefilename = enterfilename.getHint().toString();
                    }

                    // get ID of channel between destination and DTN app
                    String result = post("https://slack.com/api/conversations.open",
                        "token=" + SLACK_APP_TOKEN +
                                "&users=" + id);

                    // send message for Slack api
                    String channelId = "";
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        //convert JSON string to Map
                        channelId = ((Map)mapper.readValue(result, Map.class).get("channel")).get("id").toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    result = post("https://slack.com/api/chat.postMessage",
                            "token=" + SLACK_APP_TOKEN +
                                    "&channel=" + channelId +
                                    "&text=" + convertToOiginal(name) + " が" + sharefilename + "を渡したいらしい...．");
                    Log.d(TAG,"#########");
                    Log.d(TAG, result);
                    Log.d(TAG,"#########");
                    return ASYNC_POST;
                default:
                    break;

                 */
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            switch (integer) {
                case ASYNC_DELETE_ALL:
                    adapter.setList(createData(dbList));
                    adapter.notifyDataSetChanged();
            }
            Log.d(TAG, "Finish async task!");
        }


        /*
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
                Log.d(TAG, "POST message END!");
            }

            return sb.toString();
        }
        */
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
