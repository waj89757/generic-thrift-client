package com.jjbond.generic.server;

import com.alibaba.fastjson.JSON;
import model.HelloWorldService;
import model.Model;
import org.apache.thrift.TException;

import java.util.Random;

/**
 * Created by waj on 16-4-22.
 * helloWorld 服务端
 */
public class HelloWorldImpl implements HelloWorldService.Iface {


    @Override
    public String sayHello(String username) throws TException {
        return "hello world!";
    }

    @Override
    public Model returnModel(String name) throws TException {
        Model model = new Model();
        Random random = new Random();
        model.id = random.nextInt(1000);
        model.name = name;
        System.out.println(JSON.toJSONString(model));
        return model;
    }

    @Override
    public String modelProcess(Model model) throws TException {
        String name = model.getName();

        return name + " hehe ";
    }
}