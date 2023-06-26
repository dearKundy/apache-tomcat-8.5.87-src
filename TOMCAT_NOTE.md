> 记录 Tomcat 实现原理中的关键逻辑

【never give up，撞墙了就换个思路】

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
用EnginePipeline详细说明：获取到first valve，然后沿着valve的next链式执行，最终执行到 StandardEngineValve，在 StandardEngineValve 的invoke方法中会指定一下个容器Host的pipeLine。


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
  - `作用`：默认值 8*1024，Acceptor能同时接受的最大连接数。当前socket连接超过maxConnections的时候，Acceptor线程自己会阻塞等待，等连接降下去之后，才去处理Accept队列的下一个连接

- Tomcat初始化时就会对serverSocket进行监听地址与端口的设置，这里就包括了 acceptCount的设置，实质上就是设置ServerSocket的队列长度【新的请求进入队列，accept则消费队列】。
- 要理解这几个参数最重要的就是看org.apache.tomcat.util.net.NioEndpoint.startInternal()，其实就是一个独立的acceptor线程（阻塞式的accept【org/apache/tomcat/util/net/Acceptor.java:129】），accept之后就注册 READ 事件到poller中。
- 而核心的线程池【minSpareThreads，maxThreads两个参数就是设置这里的线程池的参数】就是负责消费poller中的读写事件的。所以我目前认为Tomcat的性能瓶颈分析可以从这里入手。
![img_8.png](img_8.png)
- ？？？？TODO：分析线程池，在sleep或者等待IO的时候，线程资源的分配情况。

- socket close 的时候会countDown connectionLimitLatch：getEndpoint().countDownConnection();【org/apache/tomcat/util/net/SocketWrapperBase.java:428】
- 也就是成功accept一个socket，connectionLimitLatch++或者wait，close一个socket connectionLimitLatch--。
- websocket是可以同时接受很多的连接的【因为他大部分时候不占用线程资源，但是占用内存资源】

参考文章：
- [Tomcat调优及acceptCount、maxConnections与maxThreads参数的含义和关系](https://blog.csdn.net/z69183787/article/details/128817991)

# 如何确定某个请求由哪个 Servlet 负责响应？
> 我们使用逆推的方式，看源码大部分情况下都是逆推。 根据我们的基础知识，我们知道最终决定由哪一个 Wrapper 处理我们的请求是在 Request 的 MappingData 对象的 wrapper 字段中的。 所以我们就得看沿着MappingData的wrapper属性逆向网上找，从而确定整个链路。

1. mappingData.wrapper = wrapper.object;：org/apache/catalina/mapper/Mapper.java:1004
2. wrapper从哪来：MappedWrapper wrapper = exactFind(wrappers, path);【org.apache.catalina.mapper.Mapper.MappedWrapper】
    1. 这里是根据path和MappedWrapper对象中的name属性进行匹配的。
3. wrappers从哪来：MappedWrapper[] exactWrappers = contextVersion.exactWrappers;【org/apache/catalina/mapper/Mapper.java:837】
4. contextVersion.exactWrappers从哪来：context.exactWrappers = newWrappers;【org/apache/catalina/mapper/Mapper.java:484】
5. newWrappers是一个数组，里面的元素是怎么构造的：MappedWrapper newWrapper = new MappedWrapper(name, wrapper, jspWildCard, resourceOnly);【org/apache/catalina/mapper/Mapper.java:480】
    1. 在第2步中MappedWrapper对象中的name属性就是在这一步设置进去的。
6. name和wrapper怎么来：
    - wrapper：addWrapper(contextVersion, wrapper.getMapping(), wrapper.getWrapper(), wrapper.isJspWildCard(),wrapper.isResourceOnly());的wrapper.getWrapper()【org/apache/catalina/mapper/Mapper.java:425】
    - name：addWrapper(contextVersion, wrapper.getMapping(), wrapper.getWrapper(), wrapper.isJspWildCard(),wrapper.isResourceOnly());的wrapper.getMapping()【org/apache/catalina/mapper/Mapper.java:425】
7. wrappers 从哪来：
    1. addWrappers(newContextVersion, wrappers);【org/apache/catalina/mapper/Mapper.java:277】
    2. prepareWrapperMappingInfo(context, (Wrapper) container, wrappers);【org/apache/catalina/mapper/MapperListener.java:377】
    3. prepareWrapperMappingInfo做了什么：wrappers.add(new WrapperMappingInfo(mapping, wrapper, jspWildCard, resourceOnly));
        - wrapper从哪来：wrapper其实就是context的小孩
        - mapping从哪来：String[] mappings = wrapper.findMappings();【org/apache/catalina/mapper/MapperListener.java:449】
            1. wrapper的mappings的元素在哪里保存：mappings.add(mapping);【org/apache/catalina/core/StandardWrapper.java:654】
            2. mapping如何构造：addServletMappingDecoded(String pattern, String name, boolean jspWildCard) 的pattern【org/apache/catalina/core/StandardContext.java:3012】
            3. pattern怎么来：org/apache/catalina/core/StandardContext.java:3006 -> org/apache/catalina/startup/ContextConfig.java:1289 -> org/apache/catalina/core/StandardContext.java:3006 -> org/apache/catalina/startup/ContextConfig.java:1287 -> 【注意：org/apache/catalina/core/StandardContext.java:3030 这一行保存了urlPattern对应的servletName的关系】
            4. webxml.getServletMappings元素如何构建：【org/apache/tomcat/util/descriptor/web/WebXml.java:1804】，再往上就不找了，实质上解析 web.xml 中的 servlet-mapping，pattern就是url-pattern，servlet-name就是servlet-name，url-pattern与servlet-name是多对一。

# 负责接收HTTP请求信息的 Request 对象是复用的吗？
> 根据源码，我们可以很容易定位到 Request 是出自于 Processor，而 Processor 就是 org.apache.tomcat.util.net.SocketWrapperBase.currentProcessor。

所以我们重点分析 org.apache.tomcat.util.net.SocketWrapperBase.currentProcessor 设置情况。【org/apache/coyote/AbstractProtocol.java:854】
这里重点两个方向，SocketWrapperBase 是复用的吗？currentProcessor是复用的吗？

## SocketWrapperBase 是复用的吗？
SocketProcessorBase 是将 SocketWrapperBase 再包一层，核心字段都在 SocketWrapperBase 中存放。

在 sc = createSocketProcessor(socketWrapper, event);【org/apache/tomcat/util/net/AbstractEndpoint.java:1239】中可以看到，每次请求 SocketProcessorBase 都是新的对象，但是 SocketWrapperBase 是传进来的，
我们再看看 SocketWrapperBase 的创建情况。

我们一路往上 trace，看到 NioSocketWrapper socketWrapper = (NioSocketWrapper) sk.attachment(); 【org/apache/tomcat/util/net/NioEndpoint.java:756】
可以看到 SocketWrapperBase 从 SelectionKey 的 attachment 中取出，我们再来 trace attachment【attachment是SocketWrapperBase的子类】。

SelectionKey 的 attachment 在 sc.register(getSelector(), SelectionKey.OP_READ, socketWrapper);【org/apache/tomcat/util/net/NioEndpoint.java:627】设置，这个代码其实就是在接受到注册事件之后，
再注册一个 OP_READ 事件，下次循环处理的时候就按照 REDA 逻辑处理。

上面的 socketWrapper 又是怎么来的呢？socketWrapper 是从 PollerEvent 中取出来的，在接受到请求的时候会将 socketWrapper 包装在 PollerEvent 对象中
然后将 PollerEvent 塞到 events 中。 poller.register(socketWrapper);【org/apache/tomcat/util/net/NioEndpoint.java:419】


# Servlet 的 response 是如何返回给客户端的
我们得先知道两个基础知识
1. servlet中是如何进行响应的：`resp.getWriter().println("hello");`，resp是 ServletResponse 接口的实现类，具体的实现类是 ResponseFacade。
2. websocket编程中是如何进行响应的：通过Socket的getOutputStream，然后在 OutputStream 中write出去。
所以我们要确定的就是 ResponseFacade 是如何跟 Socket的ResponseFacade关联的。

- ResponseFacade 的 response （connector/Response）是怎么来的，filterChain.doFilter(request.getRequest(), response.getResponse());【org/apache/catalina/core/StandardWrapperValve.java:155】
    - response.getResponse()方法中，会返回一个 ResponseFacade，其中（connector/Response）就是 this对象，因此 ResponseFacade与（connector/Response）关系绑定。
- 接下来只需要专心trace （connector/Response）的getWriter()，而的getWriter中最关键的就是outputBuffer字段。
- 一直往上trace，发现 response.setCoyoteResponse(res);【org/apache/catalina/connector/CoyoteAdapter.java:312】
  - 这里将 coyoteResponse 设置到 （connector/Response）的outputBuffer 字段中（关键）【所以 connector/response 的 outputResponse 就是 coyote/response】
  - 那么开始 trace coyoteResponse
    - coyoteResponse 是存储在 AbstractProcessor 中的【org/apache/coyote/AbstractProcessor.java:66】，在 【org/apache/coyote/http11/Http11Processor.java:174】位置为初始化 coyoteResponse的 outputBuffer（Http11OutputBuffer）。
    - 在【org/apache/coyote/http11/Http11Processor.java:702】会绑定coyoteResponse的 outputBuffer的socketWrapper。

额，实际上`resp.getWriter().println("hello");`只是把数据写入到 coyoteResponse的 outputBuffer 中，想要实现响应还是的靠 org.apache.catalina.connector.OutputBuffer类将缓冲区的数据写入到Socket连接中发送给客户端。

org/apache/tomcat/util/net/NioEndpoint.java:1336


机缘巧合之下（不会真的有人信吧，其实我是基于 socketWrapper进行trace），我发现了最终是在 org/apache/tomcat/util/net/NioEndpoint.java:1336 拿到 socket 并向客户端写出响应的。

connector/Response 的 writer【CoyoteWriter】 或者 outputBuffer 是什么时候设值的

org/apache/coyote/AbstractProcessor.java:79

一直到这 org/apache/catalina/connector/CoyoteAdapter.java:305 都还是 connector/response

## Servlet注解如何实现的？
【我一开始直接点击 @WebServlet 注解引用到的地方，导致找不到代码入口，后来是通过小chat找到的，其实应该再使用文本搜索进行查找，因为也有可能是通过全限定类名的形式进行使用】
关键位置：org/apache/catalina/startup/ContextConfig.java:1988。
这个还是挺简单的，web.xml 是解析 xml 文件生成 servlet，而注解是解析 class 文件生成 servlet，最终都是生成 ServletDef 添加到 WebXml。

## websocket如何实现的？
我发现当我想要去了解Tomcat是怎么实现Websocket协议的时候，无从下手，后来发现原因是我根本不知道最简单的Websocket协议应该如何实现（可能使用BIO、NiIO或者netty），
所以我们想要看懂Tomcat是如何实现，起码得搞清楚最基本要做的事情是那些。

注意普通HTTP与ws的 SocketChannel 的断开机制

准备工作：org.apache.tomcat.websocket.server.WsHttpUpgradeHandler.init

1. 连接阶段
org/apache/coyote/AbstractProtocol.java:929，在第一次握手的时候，HttpProcessor.process 会返回一个  UPGRADING 的 state，这个state是怎么来的。
这里： org/apache/coyote/http11/Http11Processor.java:683

- 如何判断这是一个ws请求？【逆推】
  - 请求过程中执行
    1. org/apache/coyote/AbstractProtocol.java:955 -> org/apache/coyote/AbstractProtocol.java:933 -> org/apache/coyote/http11/Http11Processor.java:1541 -> org/apache/coyote/Request.java:501
    -> org/apache/catalina/connector/Request.java:2020 -> org/apache/tomcat/websocket/server/UpgradeUtil.java:239 -> org/apache/tomcat/websocket/server/WsFilter.java:77 -> org/apache/catalina/core/ApplicationFilterChain.java:181
    -> org/apache/catalina/core/ApplicationFilterChain.java:156 -> org/apache/catalina/core/StandardWrapperValve.java:167 -> org/apache/catalina/core/StandardContextValve.java:90
    2. 实际上 WsFilter 就是一个过滤器，存放在 ApplicationFilterConfig 中（org/apache/catalina/core/ApplicationFilterChain.java:181）
  - tomcat启动执行
    1. ApplicationFilterConfig 又是怎样初始化的呢？在 org.apache.catalina.core.ApplicationFilterFactory.createFilterChain -> org/apache/catalina/core/ApplicationFilterFactory.java:130 -> org/apache/catalina/core/ApplicationFilterFactory.java:81
    -> org/apache/catalina/core/StandardContext.java:2753 -> org/apache/catalina/core/ApplicationFilterRegistration.java:102 -> org/apache/tomcat/websocket/server/WsServerContainer.java:116 -> org/apache/tomcat/websocket/server/WsSci.java:137
    -> org/apache/tomcat/websocket/server/WsSci.java:49 -> org/apache/catalina/core/StandardContext.java:4874 -> org/apache/catalina/core/StandardContext.java:168 -> org/apache/catalina/core/StandardContext.java:1153
    -> org/apache/catalina/startup/ContextConfig.java:1101 -> org/apache/catalina/startup/ContextConfig.java:159 -> org/apache/catalina/startup/ContextConfig.java:1553 -> org/apache/catalina/startup/ContextConfig.java:1542
扫描 /Users/kundy/Downloads/tomcat-source/apache-tomcat-8.5.87-src/webapps/my-servlet/WEB-INF/lib/tomcat-websocket-8.5.87.jar!/META-INF/services/javax.servlet.ServletContainerInitializer
这个文件中的org.apache.tomcat.websocket.server.WsSci，放到 org.apache.catalina.startup.ContextConfig.initializerClassMap 中
![img_6.png](img_6.png)

所以，在tomcat启动的时候会扫描webapp项目下的 ServletContainerInitializer，如果配置了WsSci，就拥有了 ws 过滤器。在执行请求的过程中，请求就会经过 ws 过滤器，从而进行升级。

那怎么确定当前请求是否需要升级的呢？
org/apache/catalina/core/StandardWrapperValve.java:141 【Create the filter chain for this request】
- 从 context 中取出所有 filterMap，然后根据 UrlPattern 决定某个 filter 是否要假如到当前请求的过滤链（filterChain）中。

怎么确定某个 request 需要执行哪些 filter
- 执行请求的时候会根据 request url 决定将哪些 filter 加入到 filterChain中。这个 context 中所有的 filter都已经在启动的时候准备好了，按需加入即可 org/apache/catalina/core/ApplicationFilterFactory.java:130

那么 request url 是怎么跟 filter 绑定的？
- 这个就要看context 启动的时候是怎么初始化 filterMaps，
ws filter 的 urlPattern /* ，也就是说只要项目中引入了 tomcat-websocket 依赖的，所以请求都会经过 wsFilter（无论是否是ws升级请求），
但是wsFilter执行的第一步就是判断是否是ws升级请求【org/apache/tomcat/websocket/server/WsFilter.java:51】

【一个端口可以同时监听HTTP和WS，本质上HTTP和WS没有什么区别，一个短链接、一个长链接】

switch protocol 101 响应是在哪里返回的
- org/apache/catalina/connector/CoyoteAdapter.java:346 -> org/apache/catalina/connector/Request.java:2024：在这里设置 response  101。
- org/apache/coyote/http11/Http11OutputBuffer.java:644，在这儿写出响应

OnOpen 什么时候调用：org/apache/tomcat/websocket/pojo/PojoEndpointBase.java:69

3. 报文传输阶段
- OnMessage 什么时候调用：org/apache/tomcat/websocket/pojo/PojoMessageHandlerWholeBase.java:104
- sendText 实现原理

5. 断开连接
- OnClose 什么时候调用：org/apache/tomcat/websocket/pojo/PojoEndpointBase.java:107
- HTTP什么时候断？
1. 大致的流程：如果HTTP请求携带 keep-alive 属性，会等到超时之后才断开这次连接【wrapper.setReadTimeout(wrapper.getEndpoint().getConnectionTimeout());】，所以这里我们明白了keep-alive实质就是当前这次连接的socket不会因一次数据传输结束就随之结束，而是还会保持着，这样的目的是看看这一次请求中会不会有数据需要继续传输的，减少了连接的成本。
2. 所以，一次HTTP处理完数据之后，会返回 OPEN 的状态，如果是keep-alive，超时后就会close掉。
3. 那么是如何实现keep-alive的？
4. 【如何实现keep-alive】实质上【org/apache/coyote/AbstractProtocol.java:930】处理完后，会返回 OPEN 状态，接着【org/apache/coyote/AbstractProtocol.java:999】会进行【wrapper.registerReadInterest();】，就是把读事件又塞回poller。
5. 【那还是没有解释到如何实现超时close的】，那我们看看wrapper.getConnectionTimeout用在了什么地方。用在了【org/apache/tomcat/util/net/NioEndpoint.java:983】，每次从poller消费事件之前都需要判断是否超时【org/apache/tomcat/util/net/NioEndpoint.java:766】，一旦发现超时就会cancelKey()，cancelKey()除了会取消channel关注事件之外，还会把当次连接socket的close掉。【org/apache/tomcat/util/net/NioEndpoint.java:1010 -> org/apache/tomcat/util/net/NioEndpoint.java:1704】
6.  注意上面实质是在processSocket（SocketEvent.ERROR）中执行 cancelKey 的，if里面只是processSocket失败之后的兜底cancelKey。
7.  你可能会有疑问，cancelKey的时候，就只是 cancelKey，close socket。那？processor不释放了吗？释放的，不过是在上一次OPEN之后就释放了【org/apache/coyote/AbstractProtocol.java:997】


WS又什么时候断？
- 断开报文有明确的标识，代码中org/apache/tomcat/websocket/WsFrameBase.java:152 中 设置，opCode，后续根据opCode判断是否要断开。
![img_7.png](img_7.png)
  怎么断？
1. sendCloseMessage：向客户端发送ws断开报文。【org/apache/tomcat/websocket/WsSession.java:748】
2. fireEndpointOnClose：
   - 其实核心就是调用对应@ServerEndpoint的类的 onClose()方法，org/apache/tomcat/websocket/pojo/PojoEndpointBase.java:108，这里的关键就是 pojo 是怎么注入进去的？
   - ws第一次请求链接的时候初始化pojo（保存在WsSession的localEndpoint字段中），在这里关键的是 serverEndpointConfig.getEndpointClass();【org/apache/tomcat/websocket/WsSession.java:260】，
   - 这个class就是处理当前请求的 ServerEndpoint，它是在启动的时候，初始化WsServerContainer的时候设置的【org/apache/tomcat/websocket/server/WsServerContainer.java:270】
   - 那再来看WsServerContainer是怎么初始化的，是在 WsSci（引入了 websocket依赖就有） 启动的时候初始化的，启动的时候扫描 filteredPojoEndpoints，逐个添加ServerEndpointConfig（这里面就包含了path与对应PojoEndpoint的信息，到时候要用就在这里取），然后放到WsServerContainer的templateMatches中【org/apache/tomcat/websocket/server/WsServerContainer.java:176】
   - 首先从哪里取数据？放到什么地方？在哪里取出来使用？
   - 【哪里取数据？】数据从 filteredPojoEndpoints 中来，里面的clazz中含有注解，注解上有path，所以万事俱备。filteredPojoEndpoints怎样初始化的，
   - 【放到什么地方】启动的时候扫描/WEB-INF/classes中的所有类，就会扫描到 ServerEndpoint ，并添加到 initializerClassMap 中【org/apache/catalina/startup/ContextConfig.java:2078】，然后filteredPojoEndpoints本质上是在 initializerClassMap 取到对应 clazzs，然后初始化的。
   - 【哪里使用】创建 WsSession 需要传入 ServerEndpointConfig，这就实现了当前session 和 endpointConfig 的绑定（他们都存放在WsHttpUpgradeHandler中）。那如何决定当前session的endpointConfig，
   - 【那如何决定当前session的endpointConfig】那就要追溯到 WsFilter中，会根据path去WsServerContainer中找出对应的 WsMappingResult【这里面就有endpointConfig，org/apache/tomcat/websocket/server/WsFilter.java:68】，而这个WsMappingResult实质上是从WsServerContainer的templateMatches中取到的， 到这里就与上面的WsServerContainer初始化闭环了。
3. Close the socket：socketWrapper.close();

WsSession是怎么维护的？
- 每次请求都会创建一个新的 WsHttpUpgradeHandler ，而一个 WsHttpUpgradeHandler 中又有一个 wsSession 和一个【WsFrameServer继承于WsFrameBase】，而wsSession又反向注入了WsFrameServer。
- 然后每次onClose等等都是从WsFrameServer中调用 wsSession.onClose()，
- 【所以是每一个ws连接都对应一个 WsHttpUpgradeHandler？】是的，WsHttpUpgradeHandler其实是 UpgradeToken 中的一个变量，而 UpgradeToken 又会作为创建 process 的参数【org/apache/coyote/AbstractProtocol.java:961】
- 在 processor 处理请求的过程中就会使用到 WsHttpUpgradeHandler，所以是不是每一个ws连接都会新建一个 WsHttpUpgradeHandler，就得看org.apache.coyote.UpgradeToken.httpUpgradeHandler的httpUpgradeHandler字段是不是都是新建的
- 一个全新的ws连接进来，会创建一个新的 processor，自然 WsHttpUpgradeHandler 也是新的，但是如果是同一个连接的多次ws报文发送，是不会新建processor的，而是复用【org/apache/coyote/AbstractProtocol.java:848，Processor processor = (Processor) wrapper.takeCurrentProcessor(】
- 当ws断开之后，就会释放 WsHttpUpgradeHandler【org/apache/coyote/AbstractProtocol.java:1028】、processor【org/apache/coyote/AbstractProtocol.java:1045，实质上是recycle processor】

6. 研究细节

## ConnectionHandler 和 WsFrameServer 对象多
ws链接越多WsFrameServer就越多，什么情况下会导致WsFrameServer比连接数多很多呢？

再研究一下HTTP、WS能同时处理多少连接？
无论是 HTTP还是 WS，都不会一直占用线程的，特别是 WS，处理完一次报文之后，就会把连接【包括socketWrapper，socketWrapper里面又有processor，下次请求进来就可以从poller event中取到所有要用的新】扔回到poller，
所以处理完一个WS报文，线程池的线程就释放了，只是有另外一个线程一个轮询 events()处理事件。

证明一下这个事情，很重要



## 为什么tomcat能够加载到 webapps 下的 class 文件
ParallelWebappClassLoader findClass 会根据类路径到 webapps 下找到改类的 class 二进制流，然后通过 defineClass 创建对应的 class。

# JVM 类加载机制



# Tomcat类加载器机制
org.apache.catalina.startup.Bootstrap.initClassLoaders，初始化了三个 classLoader：commonLoader、catalinaLoader、sharedLoader。

## 各 classLoader 负责加载的路径
> 这些路径都定义在 catalina.properties 文件中，其中 ${catalina.base} 项目的根目录【/Users/kundy/Downloads/tomcat-source/apache-tomcat-8.5.87-src】
- `commonLoader`：${catalina.base}/lib","${catalina.base}/lib/*.jar","${catalina.home}/lib","${catalina.home}/lib/*.jar
- `catalinaLoader`：空？
- `sharedLoader`：空？

