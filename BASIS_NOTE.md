> 记录阅读 Tomcat 过程中需要的基础知识


# connector 核心参数
- `acceptCount`
    - `使用位置`：org/apache/tomcat/util/net/NioEndpoint.java:226
    - `作用`：实质上就是 ServerSocket 的 backLog 参数。
- `minSpareThreads`
    - `使用位置`：org/apache/tomcat/util/net/AbstractEndpoint.java:1054
    - `作用`：线程池的 corePoolSize 参数。【具体是什么线程池晚点告诉你】
- `maxThreads`
    - `使用位置`：org/apache/tomcat/util/net/AbstractEndpoint.java:1054
    - `作用`：线程池的 maximumPoolSize 参数。【具体是什么线程池晚点告诉你】
- `maxConnections`



