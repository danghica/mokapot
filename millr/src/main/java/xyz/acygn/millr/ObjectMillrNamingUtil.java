package xyz.acygn.millr;


import org.objectweb.asm.Type;

import java.util.Arrays;

/**
 * This class is a box of tools allowing us to:
 *  - Given a method, returns the name of the _millr_method wrapping it.
 *  - Given a method, telling us if this method is coming from millr.
 *  - Given a field, giving us names of getter and setter methods for it.
 *
 */
public class ObjectMillrNamingUtil {

    /**
     * This class should not be initialized nor extended.
     */
    private ObjectMillrNamingUtil(){

    }


    /**
     * Given a method, returns the name of the method wrapping it.
     * This must follows the following rules:
     *  - Two methods with different descriptions must be mapped to different names
     *      (because replacing everything with arrayWrapper might led them to have same description).
     *  - If a method overrides another method, then the method wrapping them should have same name.
     */
    static final String getMillrMethodName(MethodParameter mp){
        return "millr"+ mp.methodName + ClassDataBase.getCodeForMethod(mp);
    }


    /**
     * Two fields can have same name, different descriptions, but their description is matched once replaced with ArrayWrapper.
     * To avoid that, we rename the fields.
     * @param fp
     * @return
     */
    static final String getNewNameString(FieldParameter fp){
        return (fp.desc.startsWith("[")) ?  fp.name + String.valueOf(Math.abs(fp.desc.hashCode())) : fp.name;
    }


    /**
     * Given a Field, returns the name of the getter for the field.
     * Note that because of hiding, two fields can have same name / description and be present in a class.
     * However, we must have different getter methods for them.
     */
    static final String getNameGetterField(FieldParameter fp){
        return "_millr_" + fp.name + "get" + String.valueOf(Math.abs(fp.cp.className.hashCode()));
    }


    /**
     * Given a Field, returns the name of the setter for the field.
     * Note that because of hiding, two fields can have same name / description and be present in a class.
     * However, we must have different getter methods for them.
     */
    static final String getNameSetterField(FieldParameter fp){
        return "_millr_" + fp.name + "set" + String.valueOf(fp.cp.className.hashCode());
    }

    static final boolean isComingFromMillr(MethodParameter mp){
        return mp.methodName.startsWith("_millr_");
    }

}
