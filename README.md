# 即时网络监听架构

#### 介绍

- 继承官方 API ConnectivityManager.NetworkCallback，实现网络状态即时监听
- 使用 EventBus 核心技术原理进行数据交互

 **网络监听使用场景** 
1. 下载文件（表情包、更新apk、补丁包等）
1. 图片浏览（图片列表、原图展示、gif 等）
1. 适配播放（短视频、MTV、电影电视剧等）
1. 接口请求

 **工具到架构演变**

 **工具缺点：** 
 
1. 先判断网络状态，再做其他
1. 无法即时监听网络变化
1. 多处订阅监听，无法同时接收
1. 某方法只想监听WIFI或GPRS，不好处理

 **广播缺点** 
![输入图片说明](https://images.gitee.com/uploads/images/2019/0220/111225_fdd7929c_1682002.png "屏幕截图.png")


#### 软件架构

 **网络监听实现** 

```

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
```

```
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
```

 **EventBus 核心技术原理实现数据交互的实现**

- 注解
```
/**
 * 作用、目标：{ElementType.FIELD 在属性之上； ElementType.METHOD 在方法之上；ElementType.TYPE 在类、接口之上}
 * jvm 在 {RetentionPolicy.CLASS 源码；RetentionPolicy.SOURCE 编译期；RetentionPolicy.RUNTIME 运行时} ，通过反射获取注解的值
 */
@Target(ElementType.METHOD)             // 作用、目标在方法之上
@Retention(RetentionPolicy.RUNTIME)     // jvm 在运行时，通过反射获取注解的值
public @interface Network {

    NetType netType() default NetType.AUTO; // 默认为 NetType.AUTO
}
```
- 注解方法实现

```
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
```

- 实体类，用于保留符合要求的注解方法

```
/**
 * 保留符合要求的网络监听注解方法，封装类
 */
public class MethodManager {

    /**
     *   @Network(netType = NetType.AUTO)
     *     public void network(NetType netType) {
     *         switch (netType) {
     *             case WIFI:
     *                 Log.d(Constants.TAG, "WIFI");
     *                 break;
     *             case CMNET:
     *             case CMWAP:
     *                 Log.d(Constants.TAG, "CM");
     *                 break;
     *             case NONE:
     *                 Log.d(Constants.TAG, "没有网络");
     *                 break;
     *         }
     *     }
     */
    // 参数类型 NetType netType
    private Class<?> type;
    // 网络类型 netType = NetType.AUTO
    private NetType netType;
    // 要执行的方法 network()
    private Method method;

    public MethodManager(Class<?> type, NetType netType, Method method) {
        this.type = type;
        this.netType = netType;
        this.method = method;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public NetType getNetType() {
        return netType;
    }

    public void setNetType(NetType netType) {
        this.netType = netType;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}

```

- 观察者注册与注销

```
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
```

- post 数据到对应的注解方法

```
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
```