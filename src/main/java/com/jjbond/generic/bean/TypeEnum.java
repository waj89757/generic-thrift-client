package com.jjbond.generic.bean;

/**
 * Created by waj on 16-4-5.
 * 参数类型
 */
public enum TypeEnum {
    PRIMITIVE_TYPE(0, "基本类型"),
    COLLECTION_TYPE(1, "集合类型"),
    SYNTHETIC_TYPE(2, "复杂类型"),
    ;

    private final int code;
    private final String message;

    private TypeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    //想获取 code 和message必须自己写方法

    public int getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }
}
