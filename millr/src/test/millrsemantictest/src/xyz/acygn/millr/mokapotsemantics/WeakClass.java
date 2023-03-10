package xyz.acygn.millr.mokapotsemantics;


import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Class that encapsulates the right notion of weak class equality. Two classes are weakly equal if one
 * is the milled version of the original one. Since Class does not have an available constructor, one cannot
 * subclass it. We get around this problem by using a constructor that takes a unique arguement of type Class, and
 * provides a list of handful methods.
 * Key amoung them are isPrintable, and isArrayPrintable(), that we use to select those objects whose toString representation
 * are deemed relevant and actually reflecting on their values.
 */
class WeakClass {
    private final Class<?> clazz;
    private final Class<?> unwrapClazz;

    public WeakClass(Class<?> clazz, Object o){
        this.clazz = clazz;
        Class<?> tempclazz = clazz;
        try{
            if (StateTracker.ProxyOrWrapperClass.isAssignableFrom(clazz)){
                tempclazz = (Class) StateTracker.unwrapClass.invoke(null, clazz);
            }
            if (StateTracker.ArrayWrapperClass.isAssignableFrom(tempclazz) || ((o != null) && StateTracker.ArrayWrapperClass.isAssignableFrom(o.getClass()))){
                if (getRealClass(o).isArray()){
                    tempclazz = getRealClass(o);
                }
                else {
                    System.out.println("insn");
                    tempclazz = getRealClass(StateTracker.asArray.invoke(o));
                }
            }
            unwrapClazz = tempclazz;
        } catch (IllegalAccessException | InvocationTargetException  e) {
            System.out.println("l");
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return !(obj==null) & (obj instanceof WeakClass) &
                ((WeakClass) obj).toString().equals(this.toString());
    }

    public String getName() {
        return getUnwrapClass().getName();
    }

    public Class getInternalClass() {
        return clazz;
    }

    public Class getUnwrapClass() {
        return unwrapClazz;
    }

    @Override
    public int hashCode() {
        return getUnwrapClass().toString().hashCode();
    }

    public boolean isArray() {
        return getUnwrapClass().isArray();
    }


    @Override
    public String toString() {
        return getUnwrapClass().toString();
    }

    public boolean isPrintable() {
        return StateTracker.printableTypes.contains(getUnwrapClass());
    }


    public boolean isArrayPrintable() {
        return getUnwrapClass().isArray() && StateTracker.printableTypes.contains(getRecursiveComponentType(getUnwrapClass()));
    }

    public boolean isObject() {
        return !unwrapClazz.isPrimitive() && !unwrapClazz.isArray();
    }

    public boolean isMilled() { return Arrays.stream(unwrapClazz.getInterfaces()).anyMatch(e->e.getName().equals("xyz.acygn.millr.util.MillredClass"));}

    /**
     * Given a class that maybe an (multidimensional) array, returns the first component of
     * the class that is not an array.
     *
     * @param clazz A class that may be a multidimensional array.
     * @return The class of the core component.
     */
    static private Class getRecursiveComponentType(Class clazz) {
        if (!clazz.isArray()) {
            return clazz;
        } else return getRecursiveComponentType(clazz.getComponentType());
    }

    private Class getRealClass(Object o){
        if (o==null){
           return null;
        }
        if (o.getClass().isArray()){
            return arrayify(getRealClass(Array.get(o, 0), o.getClass().getComponentType()));
        }
        return o.getClass();
    }

    private Class getRealClass(Object o, Class<?> c) {
        if (o == null) {
            return c;
        }
        else return getRealClass(o);
    }

    private Class arrayify(Class c){
        return java.lang.reflect.Array.newInstance(c, 0).getClass();
    }
}
