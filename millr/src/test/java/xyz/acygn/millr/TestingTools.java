package xyz.acygn.millr;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.FieldVisitor;

import java.util.HashMap;

public class TestingTools {

}

/*
 * This is a helper class for delivering method parameters for testing
 */
class ClassPrinter extends ClassVisitor {
    private TestClassParameter tcp;
    private HashMap<String, TestMethodParameter> methods;
    private HashMap<String, TestFieldParameter> fields;

    ClassPrinter() {
        super(Opcodes.ASM5);
        this.methods = new HashMap<>(10);
        this.fields = new HashMap<>(10);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String super_name, String[] interfaces) {
        tcp = new TestClassParameter(version, access, name, signature, super_name, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        methods.put(name, new TestMethodParameter(tcp, access, name, desc, signature, exceptions));

        return null;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        fields.put(name, new TestFieldParameter(tcp, access, name, desc, signature, value));
        return null;
    }

    HashMap<String, TestMethodParameter> getMethods() {
        return methods;
    }
    HashMap<String, TestFieldParameter> getFields() {
        return fields;
    }
}

class MethodPrinter extends MethodVisitor {
    private HashMap<String, TestLocalVariableParameter> local_variables;

    MethodPrinter() {
        super(Opcodes.ASM5);
        this.local_variables = new HashMap<>(10);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        local_variables.put(name, new TestLocalVariableParameter(name, desc, signature, start, end, index));
    }

    HashMap<String, TestLocalVariableParameter> getLocalVariables() {
        return local_variables;
    }
}

class TestClassParameter {

    private int version;
    private int access;
    private String name;
    private String desc;
    private String signature;
    private String super_name;
    private String[] interfaces;

    public TestClassParameter(int version, int access, String name, String signature, String super_name, String[] interfaces) {
        this.version = version;
        this.access = access;
        this.name = name;
        this.signature = signature;
        this.super_name = super_name;
        this.interfaces = interfaces;
    }

    public TestClassParameter(TestClassParameter tcp) {
        this.version = tcp.getVersion();
        this.access = tcp.getAccess();
        this.name = tcp.getName();
        this.desc = tcp.getDesc();
        this.signature = tcp.getSignature();
        this.super_name = tcp.getSuper_Name();
        this.interfaces = tcp.getInterfaces();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSuper_Name() {
        return super_name;
    }

    public void setSuper_Name(String supername) {
        this.super_name = supername;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(String[] interfaces) {
        this.interfaces = interfaces;
    }

    public String getSuper_name() {
        return super_name;
    }

    public void setSuper_name(String super_name) {
        this.super_name = super_name;
    }

    ClassParameter createClassParameter() {
        return new ClassParameter(getVersion(), getAccess(), getName(), getSignature(), getSuper_Name(), getInterfaces());
    }

}

class TestMethodParameter {

    private TestClassParameter tcp;
    private int access;
    private String name;
    private String desc;
    private String signature;
    private String[] exceptions;

    TestMethodParameter(TestClassParameter tcp, int access, String name, String desc, String signature, String[] exceptions) {
        this.tcp = tcp;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        this.exceptions = exceptions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public String[] getExceptions() {
        return exceptions;
    }

    public void setExceptions(String[] exceptions) {
        this.exceptions = exceptions;
    }

    public MethodParameter createMethodParameter() {
        return new MethodParameter(tcp.createClassParameter(), getAccess(), getName(), getDesc(), getSignature(), getExceptions());
    }
}

class TestFieldParameter {

    private TestClassParameter tcp;
    private int access;
    private String name;
    private String desc;
    private String signature;
    private Object value;

    TestFieldParameter(TestClassParameter tcp, int access, String name, String desc, String signature, Object value) {
        this.tcp = tcp;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        this.value = value;
    }

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}

class TestLocalVariableParameter {

    private String name;
    private String desc;
    private String signature;
    private Label start;
    private Label end;
    private int index;

    public TestLocalVariableParameter(String name, String desc, String signature, Label start, Label end, int index) {
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        this.start = start;
        this.end = end;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Label getStart() {
        return start;
    }

    public void setStart(Label start) {
        this.start = start;
    }

    public Label getEnd() {
        return end;
    }

    public void setEnd(Label end) {
        this.end = end;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}