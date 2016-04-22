package com.jjbond.generic;

import com.jjbond.generic.server.HelloWorldImpl;
import model.HelloWorldService;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.junit.Test;

/**
 * Created by waj on 16-4-22.
 */
public class ThriftServerTest {

    public static final int SERVER_PORT = 8090;

    @Test
    /**
     *  使用非阻塞式IO
     */
    public void helloWorldTest3() throws TTransportException {
        System.out.println("HelloWorld TSimpleServer start ....");
        TProcessor tprocessor = new HelloWorldService.Processor(new HelloWorldImpl());
        TNonblockingServerSocket tnbSocketTransport = new TNonblockingServerSocket(
                SERVER_PORT);
        TNonblockingServer.Args tnbArgs = new TNonblockingServer.Args(
                tnbSocketTransport);
        tnbArgs.processor(tprocessor);
        tnbArgs.transportFactory(new TFramedTransport.Factory());
        tnbArgs.protocolFactory(new TCompactProtocol.Factory());//
        // 使用非阻塞式IO，服务端和客户端需要指定TFramedTransport数据传输的方式
        TServer server = new TNonblockingServer(tnbArgs);
        server.serve();
    }

}
