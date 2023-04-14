> 记录 Tomcat 实现原理中的关键逻辑

# Tomcat 架构

## Tomcat顶层架构
![img_1.png](img_1.png)
一个Tomcat中只有一个Server，一个Server可以包含多个Service，一个Service只有一个 Container（Engine），但是可以有多个Connectors。
PS：Engine、Host、Context 都属于 Container，但它们三个是父子关系。

代码层面体现：
- org/apache/catalina/startup/Catalina.java:106 ：一个Catalina只有一个Server。
- org/apache/catalina/core/StandardServer.java:134：一个Server对应多个Service。
- org/apache/catalina/core/StandardService.java:86：一个Service对应一个Engine。
- org/apache/catalina/core/StandardService.java:78：一个Service对应多个Connector。

![img_2.png](img_2.png)

多个Connector和一个Container形成一个Service。

总结下一个Service中各个组件的关联关系以及是如何串联起来的，以下流程都是在启动过程中完成的。
1. 根据 server.xml 完成 service、Engine、connector、 Host的关联关系，其中Engine与connector是平级的，而一个Engine下可能有多个Host。
2. 接着就是初始化Host的工作了，初始化Host很关键的一步就是处理 child（Context），Host 会根据 server.xml 配置文件中Host的appBase属性，扫描appBase目录，然后为appBase下的每一个一级目录创建一个对应的Context。
3. 上面处理每一个Context的过程中，也依然是需要处理每个Context的child（Wrapper），根据每个Context对应的web.xml（每个appBase下的一级目录中对应的web.xml）初始化对应的servlet（wrapper）。


## Connector 架构图
![img_3.png](img_3.png)
代码层面体现：
- org/apache/catalina/connector/Connector.java:243：一个Connector对应一个 ProtocolHandler
- org/apache/coyote/AbstractProtocol.java:74：一个 ProtocolHandler 对应一个 Endpoint
- org/apache/tomcat/util/net/NioEndpoint.java:257：Endpoint中包含一个Executor
- org/apache/tomcat/util/net/NioEndpoint.java:264：Endpoint中包含一个Poller
- org/apache/tomcat/util/net/NioEndpoint.java:270：Endpoint中包含一个Acceptor
- org/apache/tomcat/util/net/AbstractEndpoint.java:1245：新建一个Processor，然后扔到Executor中执行
- org/apache/coyote/AbstractProcessor.java:53：一个Processor对应一个Adapter
- org/apache/catalina/connector/CoyoteAdapter.java:343：在Adapter中调用container处理后续逻辑

## Container 架构
![img_4.png](img_4.png)
> Container用于封装和管理Servlet，以及具体处理Request请求，在Connector内部包含了4个子容器。
4个子容器的作用分别是：
1. `Engine`：引擎，用来管理多个站点，一个Service最多只能有一个Engine；
2. `Host`：代表一个站点，也可以叫虚拟主机，通过配置Host就可以添加站点；
3. `Context`：代表一个应用程序，对应着平时开发的一套程序，或者一个WEB-INF目录以及下面的web.xml文件；
4. `Wrapper`：每一Wrapper封装着一个Servlet；

Engine、Host、Context、Wrapper 都属于 Container，通过ContainerBase的children字段实现父子关系。

## Container 如何处理请求
Container处理请求是使用 `Pipeline-Valve` 管道来处理的！（Valve是阀门之意）

Pipeline-Valve使用的责任链模式和普通的责任链模式有些不同！区别主要有以下两点：
1. 每个Pipeline都有特定的Valve，而且是在管道的最后一个执行，这个Valve叫做BaseValve，BaseValve是不可删除的；
2. 在上层容器的管道的BaseValve中会调用下层容器的管道。

我们知道Container包含四个子容器，而这四个子容器对应的 `BaseValve` 分别在：StandardEngineValve、StandardHostValve、StandardContextValve、StandardWrapperValve。

Pipeline的处理流程图如下：
![img_5.png](img_5.png)
1. Connector在接收到请求后会首先调用最顶层容器的Pipeline来处理，这里的最顶层容器的Pipeline就是EnginePipeline（Engine的管道）；
2. 在Engine的管道中依次会执行EngineValve1、EngineValve2等等，最后会执行StandardEngineValve，在StandardEngineValve中会调用Host管道，然后再依次执行Host的HostValve1、HostValve2等，最后在执行StandardHostValve，然后再依次调用Context的管道和Wrapper的管道，最后执行到StandardWrapperValve。
3. 当执行到 `StandardWrapperValve` 的时候，会在StandardWrapperValve中创建FilterChain，并调用其doFilter方法来处理请求，这个FilterChain包含着我们配置的与请求相匹配的Filter和Servlet，其doFilter方法会依次调用所有的Filter的doFilter方法和Servlet的service方法，这样请求就得到了处理！
4. 当所有的Pipeline-Valve都执行完之后，并且处理完了具体的请求，这个时候就可以将返回的结果交给Connector了，Connector在通过Socket的方式将结果返回给客户端。

### 关于 pipeline
pipleline是一个接口，里面只有三个字段：
- basic：pipeline中最后会调用的的Valve。
- container：该pipeline关联的容器。
- frist：pipeline中第一个关联的Valve。

Pipeline只有一个实现类StandardPipeline。

### 关于 Valve
> Valve的invoke方法最后会调用next.invoke()方法，next的兜底是basicValve（org/apache/catalina/core/StandardPipeline.java:344）。

> 执行EnginePipeline -> HostPipeline -> ContextPipeline -> WrapperPipeline
>
用EnginePipeline详细说明：获取到frist valve，然后沿着valve的next链式执行，最终执行到 StandardEngineValve，在 StandardEngineValve 的invoke方法中会指定一下个容器Host的pipeLine。


参考文章：
- [Tomcat系统架构](https://blog.csdn.net/xlgen157387/article/details/79006434)

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

