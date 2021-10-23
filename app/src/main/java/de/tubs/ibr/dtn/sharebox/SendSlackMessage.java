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
import java.util.concurrent.CountDownLatch;

import de.tubs.ibr.dtn.sharebox.data.EIDDao;
import de.tubs.ibr.dtn.sharebox.data.EIDDatabase;
import de.tubs.ibr.dtn.sharebox.data.EIDEntity;
import de.tubs.ibr.dtn.sharebox.ui.DestinationRecycleViewAdapter;
import de.tubs.ibr.dtn.sharebox.ui.DestinationRowData;

public class SendSlackMessage extends AsyncTask<Integer,Integer,Integer> {
    final String TAG = "SendSlackMessage";
    //githubにアップロード時に削除
    final String SLACK_APP_TOKEN = "";

    // final int ASYNC_GET_ALL = 1;
    // final int ASYNC_DELETE_ALL = 2;
    final int ASYNC_POST_SEND = 3;
    final int ASYNC_POST_DOWNLOAD = 4;

    //Slack通知外部出力(検証用)
    final String SLACK_APP_URL = "https://slack-dtn-test.glitch.me";
    int noticeflag = 0;
    String bunid = null;
    //


    // List<EIDEntity> dbList;

    EIDDatabase db;
    EIDDao dao;
    String id;
    String eid;
    // CountDownLatch countDownLatch;
    String sendeid;
    String myeid;
    String message;


/*
    public SendSlackMessage(EIDDatabase db, EIDDao dao, List<EIDEntity> dbList){
        // deleteAll
        super();
        this.db = db;
        this.dao = dao;
        this.dbList = dbList;
    }
    public SendSlackMessage(EIDDatabase db, EIDDao dao, CountDownLatch countDownLatch, List<EIDEntity> dbList){
        // getAll
        super();
        this.db = db;
        this.dao = dao;
        this.countDownLatch = countDownLatch;
        this.dbList = dbList;
    }

 */
    /*
    変数の説明
    dao:eidからslackのユーザーネーム,idを取得するため必要
    sendslackid:メッセージを送る相手に対してslackapiのconversation.openを使うために必要
    message:受け取ったファイル名を送信メッセージに追加するため
    sendeid:ファイルを送ってきた相手のEID,daoを用いて誰が送ってきたかを識別する
    myeid:自分のEID,自分が受け取ったことを伝えるため自分の名前を取得する
    */
    public SendSlackMessage(EIDDao dao, String message, String sendeid,String myeid,String bunid){
        //ファイルを受け取ったことを通知する(POST4)
        super();
        //Log.d(TAG,"受信コンストラクタ");
        this.dao = dao;
        this.message = message;
        this.sendeid = sendeid;
        this.myeid = myeid;
        //Slack(検証用)
        this.bunid = bunid;
    }

    public SendSlackMessage(EIDDatabase db, EIDDao dao, String destinationId, String eid, String message){
        // ファイルを送信したことを通知する (POST3)
        super();
        //Log.d(TAG,"送信コンストラクタ");
        this.db = db;
        this.dao = dao;
        this.id = destinationId;
        this.eid = eid;
        this.message = message;
    }

    @Override
    protected Integer doInBackground(Integer... integers) {
        Log.d(TAG,"doInBackground");

        switch (integers[0]) {
            /*
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

             */
            case ASYNC_POST_SEND:
                String sendname = dao.searchFromEid(eid).slackUseName;
                //(検証用)
                noticeflag=ASYNC_POST_SEND;
                //
                ConversationOpen(id,sendname);
                break;
            case ASYNC_POST_DOWNLOAD:
                String myname = dao.searchFromEid(myeid).slackUseName;
                String sendslackid = dao.searchFromEid(sendeid).slackUserId;

                //(検証用)
                noticeflag=ASYNC_POST_DOWNLOAD;
                //
                ConversationOpen(sendslackid, myname);
                break;
        }

        return null;
    }

    protected void ConversationOpen(String senduserid,String name){
        //Log.d(TAG,"conversationsopen");
        // get ID of channel between destination and DTN app
        String result = post("https://slack.com/api/conversations.open",
                "token=" + SLACK_APP_TOKEN +
                        "&users=" + senduserid);

        // send message for Slack api
        String channelId = "";
        //Log.d(TAG,"channelIdtrycatch");
        try {
            ObjectMapper mapper = new ObjectMapper();
            //convert JSON string to Map
            channelId = ((Map)mapper.readValue(result, Map.class).get("channel")).get("id").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Log.d(TAG,"postmessage");
        result = post("https://slack.com/api/chat.postMessage",
                "token=" + SLACK_APP_TOKEN +
                        "&channel=" + channelId +
                        "&text=" + convertToOiginal(name) + "が" + message);
        //Log.d(TAG, "## " + result + " ##");
        String result2 = post2(noticeflag);
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
            //(検証用)
            noticeflag=0;
            //
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

    //Slack通知の時間を外部出力(検証用)
    protected String post2(int noticeflag){
        final int TIMEOUT_MILLIS = 0;
        final StringBuffer sb = new StringBuffer("");

        HttpURLConnection httpConn = null;
        BufferedReader br = null;
        InputStream is = null;
        InputStreamReader isr = null;

        try {
            URL url = new URL(SLACK_APP_URL + "/notice");
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
            switch (noticeflag) {
                case ASYNC_POST_SEND:
                    /*
                    送信通知の外部出力(sendがreceiveに対してbunidのバンドルを送った)
                    bunid:送ったバンドルのid
                     */
                    ps.print("send=" + eid +
                            "&receive=" + id +
                            "&message=" + message +
                            "&noticeflag=" + noticeflag);
                    ps.close();
                    break;
                case ASYNC_POST_DOWNLOAD:
                    /*
                    送信通知の外部出力(sendがreceiveに対してbunidのバンドルを受け取った)
                    bunid:受け取ったバンドルのid
                     */
                    ps.print("send=" + sendeid +
                            "&receive=" + myeid +
                            "&bunid=" + bunid +
                            "&noticeflag=" + noticeflag);
                    ps.close();
                    break;
            }


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
