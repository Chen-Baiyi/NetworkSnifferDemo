package com.cby.networklib.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import com.cby.networklib.callback.NetworkCallbackImpl;
import com.cby.networklib.annotation.Network;
import com.cby.networklib.bean.MethodManager;
import com.cby.networklib.type.NetType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cby.networklib.utils.Constants.TAG;

public class NetworkManager {
    /**
     * volatile 的特殊规则就是：
     * <p>
     * read、load、use动作必须连续出现。
     * assign、store、write动作必须连续出现。
     * 所以，使用volatile变量能够保证:
     * <p>
     * 每次读取前必须先从主内存刷新最新的值。
     * 每次写入后必须立即同步回主内存当中。
     * 也就是说，volatile关键字修饰的变量看到的随时是自己的最新值。
     * <p>
     * 防止指令重排
     * 在基于偏序关系的Happens-Before内存模型中，指令重排技术大大提高了程序执行效率，但同时也引入了一些问题。
     */
    private static volatile NetworkManager instance = null;
    private Application application;
    private ConnectivityManager connMgr;
    private NetworkCallbackImpl networkCallback;
    private Map<Object, List<MethodManager>> networkList;

    public static NetworkManager getDefault() {
        if (instance == null) {
            synchronized (NetworkManager.class) {
                if (instance == null) {
                    instance = new NetworkManager();
                }
            }
        }
        return instance;
    }

    private NetworkManager() {
    }

    public Application getApplication() {
        if (application == null) {
            throw new RuntimeException("NetworkManager uninitialized");
        }
        return application;
    }

    /**
     * 初始化并注册网络监听
     *
     * @param application
     */
    @SuppressLint("MissingPermission")
    public void init(Application application) {
        this.application = application;
        networkList = new HashMap<>();

        // 开启网络监听
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            networkCallback = new NetworkCallbackImpl();
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            NetworkRequest request = builder.build();
            connMgr = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connMgr != null) {
                connMgr.registerNetworkCallback(request, networkCallback);
            }
        }
    }

    /**
     * 注销网络监听
     */
    public void unregisterNetworkCallback() {
        if (connMgr != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                connMgr.unregisterNetworkCallback(networkCallback);
            }
        }
    }


    /**
     * post
     *
     * @param netType 网络类型 NetType
     */
    public void post(NetType netType) {
        Set<Object> set = networkList.keySet();
        // 获取 activity 对象
        for (Object getter : set) {
            // 获取 getter 中所有注解的方法
            List<MethodManager> methodList = networkList.get(getter);
            if (methodList != null) {
                // 遍历
                for (final MethodManager method : methodList) {
                    // 匹配参数类型
                    if (method.getType().isAssignableFrom(netType.getClass())) {
                        switch (method.getNetType()) {
                            case AUTO:
                                invoke(method, getter, netType);
                                break;
                            case WIFI:
                                if (netType == NetType.WIFI || netType == NetType.NONE) {
                                    invoke(method, getter, netType);
                                }
                                break;
                            case CMWAP:
                                if (netType == NetType.CMWAP || netType == NetType.NONE) {
                                    invoke(method, getter, netType);
                                }
                                break;
                            case CMNET:
                                if (netType == NetType.CMNET || netType == NetType.NONE) {
                                    invoke(method, getter, netType);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 通知注解方法
     *
     * @param method  要执行的方法 network()
     * @param getter  activity
     * @param netType 网络类型
     */
    private void invoke(MethodManager method, Object getter, NetType netType) {
        Method execute = method.getMethod();
        try {
            execute.invoke(getter, netType);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 注册观察者
     *
     * @param obj activity / fragment 对象
     */
    public void registerObserver(Object obj) {
        // 获取 activity/fragment 中所有的网络监听注解方法
        List<MethodManager> methodList = networkList.get(obj);
        if (methodList == null) {   // 不为空，表示已注册过
            // 开始添加方法，【反射】
            methodList = findAnnotationMethod(obj);
            networkList.put(obj, methodList);
        }
    }

    /**
     * 通过反射获取 activity / fragment 中使用 Network 注解的方法
     *
     * @param obj activity / fragment
     * @return
     */
    private List<MethodManager> findAnnotationMethod(Object obj)  {
        List<MethodManager> methodList = new ArrayList<>();

        Class<?> clazz = obj.getClass();
        // 获取类 clazz 中的所有方法
        Method[] methods = clazz.getMethods();
        // 遍历
        for (Method method : methods) {
            // 获取方法的注解
            Network network = method.getAnnotation(Network.class);
            if (network == null) {
                continue;
            }
            // 方法返回值校验
            Type returnType = method.getGenericReturnType();
            if (!"void".equals(returnType.toString())) {
                throw new RuntimeException(method.getName() + "返回值类型必须是void！");
            }
            // 参数校验
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                throw new RuntimeException(method.getName() + "有且只有一个参数！");
            }

            MethodManager manager = new MethodManager(parameterTypes[0], network.netType(), method);
            methodList.add(manager);
        }
        return methodList;
    }

    /**
     * 注销观察者
     *
     * @param obj activity / fragment 对象
     */
    public void unRegisterObserver(Object obj) {
        if (!networkList.isEmpty()) {
            networkList.remove(obj);
        }
        Log.d(TAG, obj.getClass().getName() + "注销成功");
    }

    /**
     * 注销所以观察者
     */
    public void unRegisterAllObserver() {
        if (!networkList.isEmpty()) {
            networkList.clear();
        }
        networkList = null;
        Log.d(TAG, "注销所有监听成功");
    }
}
