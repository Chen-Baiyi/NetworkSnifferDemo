package com.cby.networklib.callback;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.cby.networklib.type.NetType;
import com.cby.networklib.utils.NetworkManager;

import static com.cby.networklib.utils.Constants.TAG;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
    static NetType netType = NetType.AUTO;

    /**
     * 网络连接
     *
     * @param network
     */
    @Override
    public void onAvailable(Network network) {
        super.onAvailable(network);
        Log.d(TAG, "网络已连接");
        //
        netType = NetType.AUTO;
        NetworkManager.getDefault().post(netType);
    }

    /**
     * 网络断开
     * 官方文档，生硬断开（如手动断开网络），该方法可能不会回调。【建议使用 onLost(Network network)】
     *
     * @param network
     * @param maxMsToLive
     */
    @Override
    public void onLosing(Network network, int maxMsToLive) {
        super.onLosing(network, maxMsToLive);
    }

    /**
     * 网络断开
     *
     * @param network
     */
    @Override
    public void onLost(Network network) {
        super.onLost(network);
        Log.d(TAG, "网络已中断");
        //
        netType = NetType.NONE;
        NetworkManager.getDefault().post(netType);
    }

    /**
     * 网络变更【该方法可能会回调多次，需做处理】
     *
     * @param network
     * @param networkCapabilities
     */
    @Override
    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities);
        NetType type;
        if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                type = NetType.WIFI;
            } else {
                type = NetType.AUTO;
            }
            post(type);
        }
    }

    private void post(NetType type) {
        if (netType.getValue() != type.getValue()) {
            netType = type;
            NetworkManager.getDefault().post(netType);
        }
    }
}
