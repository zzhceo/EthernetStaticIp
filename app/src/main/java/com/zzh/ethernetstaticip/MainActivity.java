package com.zzh.ethernetstaticip;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zzh.ethernetstaticip.receiver.MainFragmentReceiver;
import com.zzh.ethernetstaticip.utils.IpGetUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    TextView tvIp;
    Button btnDhcp;
    Button btnStatic;
    private MainFragmentReceiver fragReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fragReceiver = new MainFragmentReceiver();
        registerBroadcastReceiver();
        initView();
    }

    private void initView() {
        tvIp = findViewById(R.id.tv_ip);
        btnDhcp = findViewById(R.id.btn_set_dhcp_ip);
        btnStatic = findViewById(R.id.btn_set_static_ip);
        btnDhcp.setOnClickListener(this);
        btnStatic.setOnClickListener(this);
        String ip = IpGetUtil.getIpAddress(MainActivity.this);
        tvIp.setText(getString(R.string.ip_address, ip));
    }

    @Override
    protected void onStart() {
        super.onStart();
        tvIp.setText(getString(R.string.ip_address, IpGetUtil.getIpAddress(this)));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.btn_set_dhcp_ip:
                boolean success = IpGetUtil.setEthernetIP(MainActivity.this, "DHCP",
                        "", "", "", "", "");
                if (success)
                    rebootSystem();
                break;
            case R.id.btn_set_static_ip:
                boolean success2 = IpGetUtil.setEthernetIP(MainActivity.this, "STATIC",
                        "192.168.2.168", "255.255.255.0",
                        "192.168.2.1", "4.4.4.4", "114.114.114.114");
                if (success2)
                    rebootSystem();
                break;
        }
    }

    public void rebootSystem() {
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        pm.reboot(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplicationContext().unregisterReceiver(fragReceiver);
    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        // for net state changed
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        if (fragReceiver != null) {
            getApplicationContext().registerReceiver(fragReceiver, intentFilter);
            setBroadcastReceiverListener();
            Log.i(TAG, "registerBroadcastReceiver finished");
        }
    }

    private void setBroadcastReceiverListener() {
        fragReceiver.setFragmentListener(new MainFragmentReceiver.FragmentListener() {
            @Override
            public void getNetState(int state) {
                Log.i(TAG, "getNetState state=" + state);
                if (state > MainFragmentReceiver.NETSTATUS_INAVAILABLE) {
                    String ip = IpGetUtil.getIpAddress(MainActivity.this);
                    tvIp.setText(getString(R.string.ip_address, ip));
                } else {
                    tvIp.setText(getString(R.string.ip_address, "没有网络"));
                }
            }
        });
    }
}
