package com.zzh.ethernetstaticip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

// add by zzh for check net changed listener
public class MainFragmentReceiver extends BroadcastReceiver {
    private final static String TAG = "MainFragReceiver";
    public static final int NETSTATUS_INAVAILABLE = 0;
    public static final int NETSTATUS_WIFI = 1;
    public static final int NETSTATUS_MOBILE = 2;
    public static final int NETSTATUS_ETHERNET = 3;
    public static int netStatus = 0;
    // fragment listener
    FragmentListener fragmentListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "MainFragReceiver intent=" + intent);
        if (intent == null || TextUtils.isEmpty(intent.getAction())) {
            Log.e(TAG, "MainFragReceiver process failed" + intent);
            return;
        }

        String action = intent.getAction();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            processNetStateChanged(context);
        }
    }

    private void processNetStateChanged(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobileNetInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifiNetInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo ethernetInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        NetworkInfo allNetInfo = cm.getActiveNetworkInfo();

        boolean needJudge = false;
        if (allNetInfo == null) {
            needJudge = true;
        } else {
            if (allNetInfo.isConnected() || allNetInfo.isConnectedOrConnecting()) {
                needJudge = true;
            } else {
                netStatus = NETSTATUS_INAVAILABLE;
            }
        }
        if (needJudge) {
            if (ethernetInfo != null && ethernetInfo.isConnected() && ethernetInfo.isAvailable()) {
                netStatus = NETSTATUS_ETHERNET;
            } else if (wifiNetInfo != null && wifiNetInfo.isConnected() && wifiNetInfo.isAvailable()) {
                netStatus = NETSTATUS_WIFI;
            } else if (mobileNetInfo != null && (mobileNetInfo.isConnected() && mobileNetInfo.isAvailable())) {
                netStatus = NETSTATUS_MOBILE;
            } else {
                netStatus = NETSTATUS_INAVAILABLE;
            }
        }
        if (fragmentListener != null) {
            fragmentListener.getNetState(netStatus);
        }
    }



    public interface FragmentListener {
        void getNetState(int state);
    }

    public void setFragmentListener(FragmentListener fragmentListener) {
        this.fragmentListener = fragmentListener;
    }
}
