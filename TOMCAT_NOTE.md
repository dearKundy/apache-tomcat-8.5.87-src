> 记录 Tomcat 实现原理中的关键逻辑


# connector 核心参数
- `acceptCount`
    - `使用位置`：org/apache/tomcat/util/net/NioEndpoint.java:226
    - `作用`：实质上就是 ServerSocket 的 backLog 参数。
- `minSpareThreads`
    - `使用位置`：org/apache/tomcat/util/net/AbstractEndpoint.java:1054
    - `作用`：线程池（实际处理网络数据逻辑的线程池）的 corePoolSize 参数。
- `maxThreads`
    - `使用位置`：org/apache/tomcat/util/net/AbstractEndpoint.java:1054
    - `作用`：线程池（实际处理网络数据逻辑的线程池）的 maximumPoolSize 参数。
- `maxConnections`
  - `使用位置`：org/apache/tomcat/util/net/Acceptor.java:117
  - `作用`：Acceptor能同时接受的最大连接数。当当前socket连接超过maxConnections的时候，Acceptor线程自己会阻塞等待，等连接降下去之后，才去处理Accept队列的下一个连接

参考文章：
- [Tomcat调优及acceptCount、maxConnections与maxThreads参数的含义和关系](https://blog.csdn.net/z69183787/article/details/128817991)

