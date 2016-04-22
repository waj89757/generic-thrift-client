package bean;

import java.util.List;

/**
 * Created by waj on 16-4-11.
 * 总结构体
 * 入参
 * 出参
 * 方法名
 */
public class GenericNode {
    //入参结构描述
    List<GenericTree> inputs;
    //出参结构描述
    GenericTree output;
    //入参数据
    List<Object> values;
    //调用方法名
    String methodName;

    public List<GenericTree> getInputs() {
        return inputs;
    }

    public void setInputs(List<GenericTree> inputs) {
        this.inputs = inputs;
    }

    public GenericTree getOutput() {
        return output;
    }

    public void setOutput(GenericTree output) {
        this.output = output;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
