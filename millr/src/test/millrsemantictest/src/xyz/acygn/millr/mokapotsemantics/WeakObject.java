package xyz.acygn.millr.mokapotsemantics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class that is used to abstract over Array and ArrayWrapper.
 * One can create a WeakObject(Object o), where o can be either array or an arrayWrapper.
 * In which case, the weakObjects created behave equally, and are equal.
 */
public class WeakObject {


    Object internalObject;
    private WeakClass weakClassObject;


    public WeakObject(Object o) {
        this.internalObject = o;
        if (o == null) {
            weakClassObject = null;
        } else {
            weakClassObject = new WeakClass(o.getClass(), o);
        }
    }


    @Override
    public boolean equals(Object o) {
        return !(o == null) & (o instanceof WeakObject) & (((WeakObject) o).isNull() == this.isNull()) & ((!isNull()) ?
                getValue().equals(((WeakObject) o).getValue()) : true);
    }


    public WeakClass getWeakClass() {
        return weakClassObject;
    }

    public boolean isNull() {
        return internalObject == null;
    }


    public boolean isArray() {
        if (internalObject == null) {
            return false;
        } else {
            return weakClassObject.isArray();
        }
    }

    public boolean isPrintable() {
        if (isNull()) return false;
        else return weakClassObject.isPrintable();
    }

    public boolean isArrayPrintable() {
        if (isNull()) {
            return false;
        }
        return weakClassObject.isArrayPrintable();
    }

    public Object getValue() {
        try {
            if (!isArray()) return internalObject;
            if (StateTracker.ArrayWrapperClass.isAssignableFrom(internalObject.getClass())) {
                return StateTracker.asArray.invoke(internalObject);
            }
            return internalObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Returns the String Value of the field. Is notably designed to work well in the case of arrays, arrayWrappers.
     * Should be called only in the following case:
     * - The field value is Printable or, the field is an array / arrayWrapper, whose is recursively printable.
     * return "null" if the field is null.
     *
     * @return A meaningful string representation of the value holds by the field Component in the case where this one
     * is printable, or recursively printable.
     * @throws IllegalAccessException
     */
    public String getStringValue() throws IllegalAccessException, InvocationTargetException {
        if (isNull()) {
            return "null";
        }
        if (isPrintable()) {
            return String.valueOf(getValue());
        }
        if (isArrayPrintable()) {
            Object value = getValue();
            if (StateTracker.ArrayWrapperClass.isAssignableFrom(value.getClass())) {
                return arrayWrapperToString(value);
            } else {
                return arrayToString(value);
            }
        } else {
            Logger.getGlobal().log(Level.SEVERE, "getStringValue on StateTracker should only be called for " +
                    "fields of primitives or multidminesional arrays of primitive types. \n " +
                    "Class found : " + getValue().getClass().getName());
            return getValue().toString();
        }
    }


    @Override
    public String toString(){
        try{
            return getStringValue();
        }
        catch(Exception e){
            e.printStackTrace();
            return getValue().toString();
        }
    }

    /**
     * Given an array o, returns a string representation of it. Should only be called if the array is
     * recursively printable.
     *
     * @param value The array.
     * @return String representation of the array.
     */
    private String arrayToString(Object value) {
        if (value == null) {
            return null;
        }
        if (!value.getClass().isArray()) {
            throw new RuntimeException(value.getClass().getName());
        } else if (value instanceof boolean[])
            return Arrays.toString((boolean[]) value);
        else if (value instanceof byte[])
            return Arrays.toString((byte[]) value);
        else if (value instanceof char[])
            return Arrays.toString((char[]) value);
        else if (value instanceof int[])
            return Arrays.toString((int[]) value);
        else if (value instanceof double[])
            return Arrays.toString((double[]) value);
        else if (value instanceof float[])
            return Arrays.toString((float[]) value);
        else if (value instanceof long[])
            return Arrays.toString((long[]) value);
        else if (value instanceof short[])
            return Arrays.toString((short[]) value);
        else
            return Arrays.deepToString((Object[]) value);
    }


    /**
     * String representation of an arrayWrapper.
     *
     * @param a
     * @return
     */
    private String arrayWrapperToString(Object a) throws InvocationTargetException, IllegalAccessException {
        if (a == null) {
            return null;
        }
        if (a.getClass().isAssignableFrom(StateTracker.ArrayWrapperClass)) {
            throw new RuntimeException(a.getClass().getName() + " is not an instance of " + StateTracker.ArrayWrapperClass.getName());
        }
        return arrayToString(StateTracker.asArray.invoke(a));
    }

    /**
     * True if this component got getter / setters.
     * Right now, simply checked if this one has been milled.
     * That is :
     * - false if this compoment has not been initialized.
     * - false if this one is not an object.
     * - true if this one is an object and the class of this one has been milled.
     */
    public boolean hasGetter() {
        if (isNull()) {
            return false;
        } else {
            return weakClassObject.isMilled();
        }
    }


}
