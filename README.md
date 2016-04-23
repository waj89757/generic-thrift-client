# thrift 泛化客户端
##一、痛点
在使用thrift时，必须要根据IDL文件编译生成支持thrift协议的客户端代码。一旦有新服务添加，客户端程序必须重新编译并更新线上服务，无法动态调用新服务。<br>


##二、解决方案
###2.1主要解决两个问题：
>1.数据结构的描述信息<br>
>2.一个可以通用的thrift客户端<br>

###2.2数据结构的描述信息
>众所周知thrift是跨平台的，无法像dubbo那样提供一个泛化客户端和服务端，必须要根据IDL定义的格式来转换。所以，要实现一个类泛化客户端，我们需要将描述信息抽象，并存在中间介质（我使用的是mysql），一旦有新的服务，只需要结构描述信息写入中间介质，泛化客户端便可以调用了。<br>
 
>描述信息实体：<br>
>>GenericTree<br>
![](https://github.com/waj89757/generic-thrift-client/blob/master/img/GenericNode.jpg)<br>

>如图，用来描述一个thrift对象。且这个树状结构可以将这个对象的层级关系描述清楚（比如说一个struct，就可能有多层关系）。

<br>
>>GenericNode<br>
![](https://github.com/waj89757/generic-thrift-client/blob/master/img/genericTree.jpg)<br>
>它是一个总的结构体，用来描述该服务相关的所有信息。调用具体方法的方法名、入参数据以及入参、出参描述信息

###2.3一个可以通用的thrift客户端
>我们知道，thrift底层传输协议虽然有多种，但是它都是将数据按顺序排列构造成一个数据包，我们只要清楚这个数据包的顺序，并动态获取描述信息，就可以自己构造和解析传输数据了。<br>
>thrift数据包描述（比较简单，里面还有一些细节）<br>
![](https://github.com/waj89757/generic-thrift-client/blob/master/img/thirft_transport_packet.jpg)<br>

>一旦可以获取描述信息 并且摸清解析规则，这样我们便可以构造一个泛化客户端了。<br>
write:<br>
基于java的泛化客户端，一切传输数据都是Object，根据描述信息转换成具体的thrift类型数据,按照一定的传输协议发送到客户端<br>
read:<br>
按照描述信息遍历获取的数据包，并最终构造一个json对象，返回给调用方。<br>
>本项目同时支持同步和异步调用