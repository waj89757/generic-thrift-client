package com.jjbond.generic.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jjbond.generic.GenericClient;
import com.jjbond.generic.bean.GenericNode;
import com.jjbond.generic.bean.GenericTree;
import com.jjbond.generic.bean.TypeEnum;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.async.TAsyncMethodCall;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TNonblockingTransport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by waj on 16-4-22.
 * 根据GenericTree 构建thrift泛化服务
 */
public class TGenericClient implements GenericClient {


    public static String PARAMINFO_COLLECTION_INNER_KEY = "collection_inner_key";
    public static String PARAMINFO_COLLECTION_MAP_KEY = "collection_map_key";
    public static String PARAMINFO_COLLECTION_MAP_VALUE = "collection_map_value";
    private static String WRITE = "write";
    private static String READ = "read";

    //异步通信客户端
    GenericTAsyncClient genericTAsyncClient;
    //异步调用入口
    GenericMethodCall genericMethodCall;


    @Override
    public Object syncProcess(TProtocol oprot, GenericNode genericNode) throws Exception {
        oprot.getTransport().open();
        int seqid_ = sendMsg(oprot, genericNode);
        Object result = recvMsg(oprot, genericNode.getOutput(), seqid_);
        oprot.getTransport().close();
        return result;
    }

    @Override
    public void asyncProcess(GenericNode node, TProtocolFactory tProtocolFactory, TNonblockingTransport transport, AsyncMethodCallback callback) throws Exception {
        TAsyncClientManager clientManager = new TAsyncClientManager();
        GenericTAsyncClient genericTAsyncClient = getGenericTAsyncClient(tProtocolFactory, clientManager, transport, 3000);
        GenericMethodCall genericMethodCall = getGenericMethodCall(genericTAsyncClient, tProtocolFactory, transport, callback, node);
        genericTAsyncClient.checkReadyExt();
        genericTAsyncClient.setCurrentMethod(genericMethodCall);
        genericTAsyncClient.getManager().call(genericMethodCall);
    }


    @Override
    /**
     * 泛化thrift send操作 请求方法调用
     *
     * @param oprot
     * @throws TException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public int sendMsg(TProtocol oprot, GenericNode genericNode) throws Exception {
        int seqid_ = 0;
        oprot.writeMessageBegin(new TMessage(genericNode.getMethodName(), TMessageType.CALL, ++seqid_));
        write(oprot, genericNode.getInputs(), genericNode.getMethodName(), genericNode.getValues());
        oprot.writeMessageEnd();
        oprot.getTransport().flush();
        return seqid_;
    }

    @Override
    public Object recvMsg(TProtocol iprot, GenericTree genericTree, int seqid_) throws Exception {
        TMessage msg = iprot.readMessageBegin();
        if (msg.type == TMessageType.EXCEPTION) {
            TApplicationException x = TApplicationException.read(iprot);
            iprot.readMessageEnd();
            throw x;
        }
        if (msg.seqid != seqid_) {
            throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, " failed: out of sequence response");
        }

        Map result = Maps.newHashMap();
        List list = Lists.newArrayList(genericTree);
        read(iprot, list, result);
        iprot.readMessageEnd();
        //如果genericTree 是复杂类型则需要去掉Map的第一层 ex: 返回值是{a : {b:1,c:2}} 则真正的返回值应该是{b:1,c:2}
        //TODO:如果是集合情况也应该是这样处理
        if (!isPrimitiveType(genericTree)) {
            Object obj = result.get(genericTree.getName());
            return obj;
        }
        return result;

    }

    /**
     * 方法第一层参数写入protocol
     * TODO: value是List 本身是一个json转义来的 如果前端某个值为null 则json不会保存这个可以 转以后是否有这个可以，有的话其对应的value应该是null 如何处理
     *
     * @param oprot
     * @param genericTrees
     * @param methodName
     * @param values
     * @throws TException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    private void write(org.apache.thrift.protocol.TProtocol oprot, List<GenericTree> genericTrees, String methodName, List<Object> values) throws Exception {
        int index = 0;
        String method_args = new StringBuffer(methodName).append("_args").toString();
        org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct(method_args);
        oprot.writeStructBegin(STRUCT_DESC); //TODO:不能写在这里
        short id = 1;
        for (GenericTree first : genericTrees) {
            writeVal(oprot, first, values.get(index++), id++);
        }
        oprot.writeFieldStop();
        oprot.writeStructEnd();
    }

    /**
     * protocol写入具体数据 基本类型直接写入 非基本类型需要递归进行写入
     * ex:oprot.writeString()
     * TODO:存在多次JSON decode enocde 性能问题！
     * TODO:value 为null如何处理
     *
     * @param oprot
     * @param value
     */
    private void writeVal(org.apache.thrift.protocol.TProtocol oprot, GenericTree genericTree, Object value, short id) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, TException, ClassNotFoundException {
        String thriftType = genericTree.getThrfitType();
        if (value == null)
            return;
        if (isPrimitiveType(genericTree)) {
            org.apache.thrift.protocol.TField tField = getTField(genericTree.getName(), thriftType, id);
            oprot.writeFieldBegin(tField);
            doProtocolMethod(oprot, thriftType, value, WRITE);
            oprot.writeFieldEnd();
        } else if (isColletionType(genericTree)) {
            org.apache.thrift.protocol.TField tField = getTField(genericTree.getName(), thriftType, id);
            oprot.writeFieldBegin(tField);
            //TODO:假设目前集合只有List类型
            String json = JSON.toJSONString(value);
            if (isColletionMap(genericTree)) {
                GenericTree k = genericTree.getChildren().get(PARAMINFO_COLLECTION_MAP_KEY);
                GenericTree v = genericTree.getChildren().get(PARAMINFO_COLLECTION_MAP_VALUE);
                if (k == null || v == null) {
                    System.out.println("key or value is not found in GenericNode !");
                } else {
                    Map map =  JSON.parseObject(json, Map.class);
                    oprot.writeMapBegin(new TMap(ttypeMap.get(k.getThrfitType().toUpperCase()), ttypeMap.get(v.getThrfitType().toUpperCase()), map.size()));
                    Set<Map.Entry> set = map.entrySet();
                    for (Map.Entry entry : set) {
                        //TODO:只支持key是简单类型
                        doProtocolMethod(oprot, k.getThrfitType(), entry.getKey(), WRITE);
                        if (v.getParamType() == TypeEnum.PRIMITIVE_TYPE)
                            doProtocolMethod(oprot, v.getThrfitType(), entry.getValue(), WRITE);
                        else {
                            writeVal(oprot, v, entry.getValue(), id);
                        }
                    }
                    oprot.writeMapEnd();
                }
            }else {
                List list = JSON.parseObject(json, List.class);
                GenericTree child = genericTree.getChildren().get(PARAMINFO_COLLECTION_INNER_KEY);
                String childThriftType = child.getThrfitType();
                oprot.writeListBegin(new org.apache.thrift.protocol.TList(ttypeMap.get(childThriftType.toUpperCase()), list.size()));
                //获取容器的孩子节点，如果集合是基础类型的 本次遍历就到这里
                if (isPrimitiveType(child)) {
                    for (Object obj : list) {
                        String childThrfitType = child.getThrfitType();
                        doProtocolMethod(oprot, childThrfitType, obj, WRITE);
                    }
                } else {
                    for (Object obj : list) {
                        short ids = 1;
                        writeVal(oprot, child, obj, ids);
                    }
                }
                oprot.writeListEnd();
            }

            oprot.writeFieldEnd();
        } else {
            String structName = genericTree.getType();
            String struct_args = new StringBuffer(structName).append("_args").toString();
            org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct(struct_args);
            oprot.writeStructBegin(STRUCT_DESC);
            //TODO:这个map是有序的
            Map<String, GenericTree> children = genericTree.getChildren();

            String json = JSON.toJSONString(value);
            Map map = JSON.parseObject(json, Map.class);
            short ids = 1;
            for (GenericTree child : children.values()) {
                writeVal(oprot, child, map.get(child.getName()), ids++);
            }
            oprot.writeFieldStop();
            oprot.writeStructEnd();
        }


    }

    private void read(org.apache.thrift.protocol.TProtocol iprot, List<GenericTree> genericTrees, Map result) throws TException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        org.apache.thrift.protocol.TField schemeField;
        iprot.readStructBegin();
        //每一层的参数 按顺序逐个进行遍历 读取二进制序列返回值
        boolean isStop = false;
        for (GenericTree genericTree : genericTrees) {
            schemeField = iprot.readFieldBegin();
            if (schemeField.type == org.apache.thrift.protocol.TType.STOP) {
                System.out.println("返回结果参数个数不对！ type : stop 返回值可能为null");
                isStop = true;
                break;
            }
            if (isPrimitiveType(genericTree)) {
                String key = genericTree.getName();
                String thriftType = genericTree.getThrfitType();
                Object obj = doProtocolMethod(iprot, thriftType, null, READ);
                result.put(key, obj);
            } else if (isColletionType(genericTree)) {
                if (schemeField.type != org.apache.thrift.protocol.TType.LIST && schemeField.type != org.apache.thrift.protocol.TType.SET && schemeField.type != TType.MAP) {
                    System.out.println("返回结果参数类型不是集合类型！ ： " + schemeField.type);
                    break;
                    //TODO:若类型不匹配则跳过这段数据报文   org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                }

                //MAP单独处理
                if (schemeField.type == TType.MAP) {
                    org.apache.thrift.protocol.TMap _map0 = iprot.readMapBegin();
                    GenericTree key = genericTree.getChildren().get(PARAMINFO_COLLECTION_MAP_KEY);
                    GenericTree value = genericTree.getChildren().get(PARAMINFO_COLLECTION_MAP_VALUE);
                    if (key == null || value == null) {
                        System.out.println("key or value is not found in GenericNode !");
                        break;
                    }
                    Map map = new HashMap(2 * _map0.size);
                    //TODO:默认key是简单类型
                    for (int i = 0; i < _map0.size; ++i) {
                        Object obj_key = doProtocolMethod(iprot, key.getThrfitType(), null, READ);
                        if (value.getParamType() == TypeEnum.PRIMITIVE_TYPE) {
                            Object obj_value = doProtocolMethod(iprot, key.getThrfitType(), null, READ);
                            map.put(obj_key, obj_value);
                        } else {
                            Map map_value = Maps.newHashMap();
                            List<GenericTree> children = Lists.newArrayList(value.getChildren().values());
                            read(iprot, children, map_value);
                            map.put(obj_key, map_value);
                        }
                    }
                    result.put(genericTree.getName(), map);
                    iprot.readMapEnd();
                    break;
                }

                org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                //获取容器的孩子节点，如果集合是基础类型的 本次遍历就到这里
                GenericTree child = genericTree.getChildren().get(PARAMINFO_COLLECTION_INNER_KEY);
                List list = Lists.newArrayList();
                if (isPrimitiveType(child)) {
                    for (int _i2 = 0; _i2 < _list0.size; ++_i2) {
                        String childThrfitType = child.getThrfitType();
                        list.add(doProtocolMethod(iprot, childThrfitType, null, READ));
                    }
                    iprot.readListEnd();
                } else {
                    //如果发现集合内部是复杂类型 则遍历_list0 内部就是复杂类型的结构参数
                    for (int _i2 = 0; _i2 < _list0.size; ++_i2) {
                        Map childResult = Maps.newHashMap();
                        //这里的child是复杂类型说明，应该直接拿到它的children 即它的子参数类型
                        List<GenericTree> children = Lists.newArrayList(child.getChildren().values());
                        read(iprot, children, childResult);
                        list.add(childResult);
                    }
                    iprot.readListEnd();
                }
                result.put(genericTree.getName(), list);

            } else {
                if (schemeField.type != org.apache.thrift.protocol.TType.STRUCT) {
                    System.out.println("返回结果参数类型不是复杂类型！ ： " + schemeField.type);
                    break;
                    //TODO:若类型不匹配则跳过这段二进制序列   org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
                }
                List<GenericTree> children = Lists.newArrayList(genericTree.getChildren().values());
                Map childResult = Maps.newHashMap();
                //如果是第一层 即返回值类型本身，则本身没有key，应该直接将map传入下一层。通过map size是否为0判断是否未第一层
                result.put(genericTree.getName(), childResult);
                read(iprot, children, childResult);
            }
            iprot.readFieldEnd();
        }

        if (!isStop) {
            schemeField = iprot.readFieldBegin();
            if (schemeField.type != org.apache.thrift.protocol.TType.STOP) {
                System.out.println("type is not stop : " + schemeField.type);
            }
        }

        iprot.readStructEnd();

    }


    /**
     * 判断是否为基础类型
     *
     * @param genericTree
     * @return
     */
    private boolean isPrimitiveType(GenericTree genericTree) {
        return genericTree.getParamType().getCode() == TypeEnum.PRIMITIVE_TYPE.getCode();
    }


    private boolean isColletionType(GenericTree genericTree) {
        return genericTree.getParamType().getCode() == TypeEnum.COLLECTION_TYPE.getCode();
    }

    private boolean isColletionMap(GenericTree genericTree) {
        return genericTree.getThrfitType().toUpperCase().equals("MAP");
    }


    private org.apache.thrift.protocol.TField getTField(String paramName, String thriftType, short id) {
        return new org.apache.thrift.protocol.TField(paramName, ttypeMap.get(thriftType.toUpperCase()), id);
    }

    private Object doProtocolMethod(org.apache.thrift.protocol.TProtocol oprot, String type, Object obj, String wr) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        String methodName = null;
        Method method = null;
        Object result = null;
        if (wr.equals(WRITE)) {
            methodName = TPWriteMethodMap.get(type.toUpperCase());
            Class clazz = TPParamsMap.get(type.toUpperCase());
            method = oprot.getClass().getMethod(methodName, clazz);
            method.invoke(oprot, obj);
            return result;
        }
        if (wr.equals(READ)) {
            methodName = TPReadMethodMap.get(type.toUpperCase());
            method = oprot.getClass().getMethod(methodName);
            result = method.invoke(oprot);
            return result;
        }
        System.out.println("protocol method : " + method.getName());
        return result;
    }

    private GenericTAsyncClient getGenericTAsyncClient(TProtocolFactory tProtocolFactory, TAsyncClientManager clientManager, TNonblockingTransport transport, long TIMEOUT) {
        if (this.genericTAsyncClient == null)
            this.genericTAsyncClient = new GenericTAsyncClient(tProtocolFactory, clientManager, transport, 3000);
        return this.genericTAsyncClient;
    }

    private GenericMethodCall getGenericMethodCall(GenericTAsyncClient genericTAsyncClient, TProtocolFactory tProtocolFactory, TNonblockingTransport transport, AsyncMethodCallback callback, GenericNode node) {
        if (this.genericMethodCall == null)
            this.genericMethodCall = new GenericMethodCall(genericTAsyncClient, tProtocolFactory, transport, callback, false, node);
        return this.genericMethodCall;
    }

    public class GenericTAsyncClient extends TAsyncClient {

        public GenericTAsyncClient(TProtocolFactory protocolFactory, TAsyncClientManager manager, TNonblockingTransport transport, long timeout) {
            super(protocolFactory, manager, transport, timeout);
        }

        public void setCurrentMethod(TAsyncMethodCall tAsyncMethodCall) {
            this.___currentMethod = tAsyncMethodCall;
        }

        public TAsyncClientManager getManager() {
            return this.___manager;
        }

        public void checkReadyExt() {
            checkReady();
        }

    }


    public class GenericMethodCall extends TAsyncMethodCall {

        int seqid_;
        GenericNode genericNode;

        protected GenericMethodCall(TAsyncClient client, TProtocolFactory protocolFactory, TNonblockingTransport transport, AsyncMethodCallback callback, boolean isOneway, GenericNode genericNode) {
            super(client, protocolFactory, transport, callback, isOneway);
            this.genericNode = genericNode;
        }

        @Override
        protected void write_args(TProtocol protocol) throws TException {
            try {
                seqid_ = sendMsg(protocol, genericNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Object getResult() throws Exception {
            if (getState() != org.apache.thrift.async.TAsyncMethodCall.State.RESPONSE_READ) {
                throw new IllegalStateException("Method call not finished!");
            }
            org.apache.thrift.transport.TMemoryInputTransport memoryTransport = new org.apache.thrift.transport.TMemoryInputTransport(getFrameBuffer().array());
            org.apache.thrift.protocol.TProtocol prot = client.getProtocolFactory().getProtocol(memoryTransport);
            return recvMsg(prot, genericNode.getOutput(), seqid_);
        }
    }
}
