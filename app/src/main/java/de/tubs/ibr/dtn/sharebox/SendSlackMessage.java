package de.tubs.ibr.dtn.sharebox;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
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

import de.tubs.ibr.dtn.sharebox.data.EIDDao;
import de.tubs.ibr.dtn.sharebox.data.EIDDao_Impl;
import de.tubs.ibr.dtn.sharebox.data.EIDDatabase;
import de.tubs.ibr.dtn.sharebox.data.EIDEntity;
import de.tubs.ibr.dtn.sharebox.ui.DestinationRecycleViewAdapter;
import de.tubs.ibr.dtn.sharebox.ui.DestinationRowData;

public class SendSlackMessage extends AsyncTask<Integer,Integer,Integer> {
    final String TAG = "SendSlackMessage";
    //githubにアップロード時に削除
    final String SLACK_APP_TOKEN = "";

    EIDDao dao;
    String id;
    String sendeid;
    String myeid;
    String message;


    public SendSlackMessage(EIDDao dao, String destinationId, String message, String sendeid,String myeid){
        super();
        Log.d(TAG,"コンストラクタ");
        this.dao = dao;
        this.id = destinationId;
        this.message = message;
        this.sendeid = sendeid;
        this.myeid = myeid;
    }

    @Override
    protected Integer doInBackground(Integer... integers) {
        Log.d(TAG,"doInBackground");
        Log.d(TAG,message);


        /*
        String name = dao.searchFromEid(myeid).slackUseName;


        Log.d(TAG,"conversationsopen");
        // get ID of channel between destination and DTN app
        String result = post("https://slack.com/api/conversations.open",
                "token=" + SLACK_APP_TOKEN +
                        "&users=" + id);

        // send message for Slack api
        String channelId = "";
        Log.d(TAG,"channelIdtrycatch");
        try {
            ObjectMapper mapper = new ObjectMapper();
            //convert JSON string to Map
            channelId = ((Map)mapper.readValue(result, Map.class).get("channel")).get("id").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG,"postmessage");
        result = post("https://slack.com/api/chat.postMessage",
                "token=" + SLACK_APP_TOKEN +
                        "&channel=" + channelId +
                        "&text=" + convertToOiginal(name) + "が" + message);
        Log.d(TAG,"#########");
        Log.d(TAG, result);
        Log.d(TAG,"#########");
        */
        return null;
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
            Log.d(TAG, "POST message END!");
        }

        return sb.toString();
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
