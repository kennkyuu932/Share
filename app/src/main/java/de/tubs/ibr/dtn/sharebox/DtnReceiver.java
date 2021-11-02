package de.tubs.ibr.dtn.sharebox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DtnReceiver extends BroadcastReceiver {
    private String TAG = "DtnReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG,"onReceive");
        //Log.d(TAG,context.toString());
        
        if (action.equals(de.tubs.ibr.dtn.Intent.RECEIVE))
        {
            // We received a notification about a new bundle and
            // wake-up the local PingService to received the bundle.
            Intent i = new Intent(context, DtnService.class);
            i.setAction(de.tubs.ibr.dtn.Intent.RECEIVE);
            Log.d(TAG,"IntentactionReceive");
            context.startForegroundService(i);//エラー  startForegroundService()を使ってみる?→VAIOでの実行が出来なくなる.VAIOの時はstartService(i)
        }
        else if (action.equals(de.tubs.ibr.dtn.Intent.STATUS_REPORT))
        {
            // We received a status report about a bundle and
            // wake-up the local PingService to process this report.
            Intent i = new Intent(context, DtnService.class);
            i.setAction(DtnService.REPORT_DELIVERED_INTENT);
            i.putExtra("source", intent.getParcelableExtra("source"));
            i.putExtra("bundleid", intent.getParcelableExtra("bundleid"));
            Log.d(TAG,"IntentStatusReport");
            context.startForegroundService(i);// startForegroundService()に変更
        }
        else if (action.equals(de.tubs.ibr.dtn.Intent.STATE))
        {
            String state = intent.getStringExtra("state");
            if (state.equals("ONLINE")) {
                // trigger session registration
                Intent i = new Intent(context, DtnService.class);
                i.setAction(de.tubs.ibr.dtn.Intent.RECEIVE);
                Log.d(TAG,"IntentState");
                context.startForegroundService(i);// startForegroundService()に変更
            }
        }
    }

}
