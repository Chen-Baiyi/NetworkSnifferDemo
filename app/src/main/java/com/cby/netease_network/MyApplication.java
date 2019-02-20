package com.cby.netease_network;

import android.app.Application;

import com.cby.networklib.utils.NetworkManager;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NetworkManager.getDefault().init(this);
    }
}
