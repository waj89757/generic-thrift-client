package com.jjbond.generic;

import com.jjbond.generic.bean.GenericNode;
import com.jjbond.generic.bean.GenericTree;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.transport.TNonblockingTransport;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by waj on 16-4-22.
 * 泛化客户端抽象接口
 * 定义了泛化类型和 Thrift类型的映射关系
 *
 */
public interface GenericClient {


    //泛化thrift客户端 同步调用方法
    public Object syncProcess(org.apache.thrift.protocol.TProtocol oprot, GenericNode genericNode) throws Exception;

    //泛化thrift客户端 异步调用方法
    public void asyncProcess(GenericNode node, TProtocolFactory tProtocolFactory, TNonblockingTransport transport, AsyncMethodCallback callback) throws Exception;

    public int sendMsg(org.apache.thrift.protocol.TProtocol oprot, GenericNode genericNode) throws Exception;

    public Object recvMsg(org.apache.thrift.protocol.TProtocol iprot, GenericTree genericTree, int seqid_) throws Exception;

    // 泛化对象的thriftType 和TType的映射关系
    static Map<String, Byte> ttypeMap = new HashMap<String, Byte>() {
        {
            put("BOOL", TType.BOOL);
            put("BYTE", TType.BYTE);
            put("DOUBLE", TType.DOUBLE);
            put("I16", TType.I16);
            put("I32", TType.I32);
            put("I64", TType.I64);
            put("STRING", TType.STRING);
            put("MAP", TType.MAP);
            put("SET", TType.SET);
            put("LIST", TType.LIST);
            put("STRUCT", TType.STRUCT);
        }
    };

    // 泛化对象的thriftType 和TProtocol write方法参数类型的对应关系
    static Map<String, Class> TPParamsMap = new HashMap<String, Class>() {
        {
            put("BOOL", Boolean.TYPE);
            put("BYTE", Byte.TYPE);
            put("DOUBLE", Double.TYPE);
            put("I16", Short.TYPE);
            put("I32", Integer.TYPE);
            put("I64", Long.TYPE);
            put("STRING", String.class);
        }
    };

    // 泛化对象的thriftType 和TProtocol write方法名的对应关系 只定义了基本类型的映射关系
    static Map<String, String> TPWriteMethodMap = new HashMap<String, String>() {
        {
            put("BOOL", "writeBool");
            put("BYTE", "writeByte");
            put("DOUBLE", "writeDouble");
            put("I16", "writeI16");
            put("I32", "writeI32");
            put("I64", "writeI64");
            put("STRING", "writeString");
        }
    };

    // 泛化对象的thriftType 和TProtocol read方法名的对应关系 只定义了基本类型的映射关系
    static Map<String, String> TPReadMethodMap = new HashMap<String, String>() {
        {
            put("BOOL", "readBool");
            put("BYTE", "readByte");
            put("DOUBLE", "readDouble");
            put("I16", "readI16");
            put("I32", "readI32");
            put("I64", "readI64");
            put("STRING", "readString");
        }
    };
}
