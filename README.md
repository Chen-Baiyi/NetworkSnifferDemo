# 即时网络监听架构

#### 介绍

- 继承官方 API ConnectivityManager.NetworkCallback，实现网络状态即时监听
- 使用 EventBus 核心技术原理进行数据交互

 **网络监听使用场景** 
![输入图片说明](https://images.gitee.com/uploads/images/2019/0220/110436_69a851ae_1682002.png "屏幕截图.png")

 **工具到架构演变**

 **工具缺点：** 
 
1. 先判断网络状态，再做其他
1. 无法即时监听网络变化
1. 多处订阅监听，无法同时接收
1. 某方法只想监听WIFI或GPRS，不好处理

 **广播缺点** 
![输入图片说明](https://images.gitee.com/uploads/images/2019/0220/111225_fdd7929c_1682002.png "屏幕截图.png")


#### 软件架构