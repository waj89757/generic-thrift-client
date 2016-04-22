package com.jjbond.generic;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.jjbond.generic.bean.GenericNode;
import com.jjbond.generic.bean.GenericTree;
import com.jjbond.generic.bean.TypeEnum;
import com.jjbond.generic.impl.TGenericClient;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.*;
import org.junit.Test;

import java.util.List;
import java.util.TreeMap;

/**
 * Created by waj on 16-4-22.
 */
public class GenericTest {
    public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 8090;
    public static final int TIMEOUT = 30000;

    GenericClient genericAnlyser = new TGenericClient();
    TTransport transport = new TFramedTransport(new TSocket(SERVER_IP,
            SERVER_PORT, TIMEOUT));
    // 协议要和服务端一致
    TProtocol protocol = new TCompactProtocol(transport);

    @Test
    /**
     * thrift 调用流程
     * Client  returnModel
     send_returnModel
     TServiceClient sendBase
     returnModel_args
     returnModel_argsStandardScheme write

     Cleint recv_returnModel
     receiveBase
     returnModel_resultStandardScheme  read
     Model read

     */
    public void returnModelTest() throws Exception {
        //入参
        GenericTree first = new GenericTree();
        first.setName("name");
        first.setParamType(TypeEnum.PRIMITIVE_TYPE);
        first.setThrfitType("STRING");

        //出参
        GenericTree model = new GenericTree();
        model.setParamType(TypeEnum.SYNTHETIC_TYPE);
        model.setThrfitType("struct");
        model.setName("returnModel");

        GenericTree id = new GenericTree();
        id.setParamType(TypeEnum.PRIMITIVE_TYPE);
        id.setThrfitType("i32");
        id.setName("id");

        GenericTree name = new GenericTree();
        name.setParamType(TypeEnum.PRIMITIVE_TYPE);
        name.setThrfitType("string");
        name.setName("name");

        TreeMap<String,GenericTree> children =  new TreeMap<String, GenericTree>();
        children.put(id.getName(),id);
        children.put(name.getName(),name);
        model.setChildren(children);

        List<GenericTree> inputGenericTrees = Lists.newArrayList();
        inputGenericTrees.add(first);
        List<Object> inputVals = Lists.newArrayList();
        inputVals.add("sb");
        String method = "returnModel";

        GenericNode genericNode = new GenericNode();
        genericNode.setInputs(inputGenericTrees);
        genericNode.setMethodName(method);
        genericNode.setValues(inputVals);
        genericNode.setOutput(model);

        Object obj = genericAnlyser.syncProcess(protocol, genericNode);
        System.out.println(JSON.toJSONString(obj));


        //异步
        TNonblockingTransport transport = new TNonblockingSocket(SERVER_IP,
                SERVER_PORT, TIMEOUT);
        TProtocolFactory tprotocol = new TCompactProtocol.Factory();
        AsynCallback callback = new AsynCallback();
        genericAnlyser.asyncProcess(genericNode,tprotocol,transport,callback);
        while (true){

        }
    }


    /**
     * 回调函数范例
     */
    public class AsynCallback implements AsyncMethodCallback<TGenericClient.GenericMethodCall> {

        @Override
        public void onComplete(TGenericClient.GenericMethodCall response) {
            try {
                System.out.println("AsynCall result =:"
                        + JSON.toJSONString(response.getResult()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(Exception exception) {

        }
    }
}
