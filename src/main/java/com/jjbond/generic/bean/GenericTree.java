package com.jjbond.generic.bean;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Created by waj on 16-4-5.
 * 用来表达IDL里实体逻辑的树
 * 本层是个链表 小
 */
public class GenericTree {
    //参数名称 ex:xxx
    private String name;
    //参数抽象类型 各种类型的名称
    private String type;
    //参数thrift类型 thrift:i32 i64 和TType对应
    private String thrfitType;

    //参数类别 基础类型 复杂类型 集合类型
    private TypeEnum paramType;

    private GenericTree parent;

    private LinkedHashMap<String,GenericTree> children;  //必须按照参数顺序有序排序



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getThrfitType() {
        return thrfitType;
    }

    public void setThrfitType(String thrfitType) {
        this.thrfitType = thrfitType;
    }

    public GenericTree getParent() {
        return parent;
    }

    public void setParent(GenericTree parent) {
        this.parent = parent;
    }

    public Map<String, GenericTree> getChildren() {
        return children;
    }

    public void setChildren(LinkedHashMap<String, GenericTree> children) {
        this.children = children;
    }

    public TypeEnum getParamType() {
        return paramType;
    }

    public void setParamType(TypeEnum paramType) {
        this.paramType = paramType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
