package com.cby.netease_network;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.cby.networklib.annotation.Network;
import com.cby.networklib.type.NetType;
import com.cby.networklib.utils.Constants;
import com.cby.networklib.utils.NetworkManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NetworkManager.getDefault().registerObserver(this);
    }

    @Network(netType = NetType.AUTO)    // 指定监听的网络类型
    public void network(NetType netType) {
        switch (netType) {
            case WIFI:
                Log.d(Constants.TAG, "WIFI");
                Toast.makeText(MainActivity.this, "当前网络类型 wifi", Toast.LENGTH_LONG).show();
                break;
            case CMNET:
            case CMWAP:
                Log.d(Constants.TAG, "CM");
                Toast.makeText(MainActivity.this, "当前网络类型 CMNET/CMWAP", Toast.LENGTH_LONG).show();
                break;
            case NONE:
                Log.d(Constants.TAG, "没有网络");
                Toast.makeText(MainActivity.this, "没有网络", Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkManager.getDefault().unRegisterObserver(this);
    }
}
