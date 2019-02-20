package com.cby.networklib.type;

public enum NetType {
    // 有网络，包括 wifi/gprs
    AUTO(0),
    // wifi
    WIFI(1),
    // 手机
    CMWAP(2),
    // 主要PC/笔记本/PDA
    CMNET(3),
    // 没有网络
    NONE(-1);

    private int value = 0;

    NetType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
