package xyz.acygn.millr.mokapotsemantics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Check that a type correctly implements the Getters annotation
 */

public interface checkGetters {

    static boolean checkGetters(@Getters Object o){
        if (o==null){
            return true;
        }
        for (Field field : o.getClass().getDeclaredFields()){
            try {
                Method m =  o.getClass().getMethod(field.getName() + "_get", new Class[0]);
            }
            catch (NoSuchMethodException ex){
                return false;
            }
        }
        return true;
    }
}
