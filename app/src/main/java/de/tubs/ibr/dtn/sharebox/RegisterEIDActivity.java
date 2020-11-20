package de.tubs.ibr.dtn.sharebox;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.bazaarvoice.jackson.rison.RisonFactory;
import com.bazaarvoice.jackson.rison.RisonGenerator;
import com.bazaarvoice.jackson.rison.RisonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import de.tubs.ibr.dtn.sharebox.data.EIDDao;
import de.tubs.ibr.dtn.sharebox.data.EIDDatabase;
import de.tubs.ibr.dtn.sharebox.data.EIDEntity;
import de.tubs.ibr.dtn.sharebox.ui.SelectDestinationActivity;
import de.tubs.ibr.dtn.sharebox.ui.ShareWithActivity;

public class RegisterEIDActivity extends Activity {
    private static final String TAG = "RegisterEIDActivity";

    private DtnService mService = null;
    private Boolean mBound = false;

    final String SLACK_APP_URL = "https://4e44be149ab5.ngrok.io";

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((DtnService.LocalBinder)service).getService();
            String eid = mService.getClientEndpoint();

            String rawData = getIntent().getData().getQuery();

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

            String id = data.get("id").toString();
            String team_id = data.get("team_id").toString();

            Log.d(TAG, eid);

            new RegisterTask(eid, id, team_id).execute(0);

            Toast.makeText(RegisterEIDActivity.this, "Register!", Toast.LENGTH_SHORT).show();
            finish();
        }
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBound = false;
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();

        if (intent == null) {
            // if there is no intent, quit directly
            finish();
        } else {
            if (!mBound) {
                bindService(new Intent(this, DtnService.class), mConnection, Context.BIND_AUTO_CREATE);
                mBound = true;
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        super.onDestroy();
    }

    class RegisterTask extends AsyncTask<Integer, Integer, Integer> {

        String eid;
        String id;
        String team_id;

        public RegisterTask(String eid, String id, String team_id){
            super();
            this.eid = eid;
            this.id = id;
            this.team_id = team_id;
        }

        @Override
        protected Integer doInBackground(Integer... integers) {

            final int TIMEOUT_MILLIS = 0;
            final StringBuffer sb = new StringBuffer("");

            HttpURLConnection httpConn = null;
            BufferedReader br = null;
            InputStream is = null;
            InputStreamReader isr = null;

            try {
                URL url = new URL(SLACK_APP_URL + "/android");
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
                ps.print("id=" + id +
                        "&eid=" + eid +
                        "&team_id=" + team_id);
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
                    Log.d(TAG, sb.toString());
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
                Log.d(TAG, "POST message END!!!!!");
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
