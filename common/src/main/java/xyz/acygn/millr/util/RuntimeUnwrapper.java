/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.util;


/**
 * @author thomas
 */
public final class RuntimeUnwrapper {

    public static Object unwrap(Object arg) {
        if (arg == null) {
            return arg;
        }
        if (arg instanceof ArrayWrapper) {
            return ((ArrayWrapper) arg).asArray();
        }
        return arg;
    }


    public static Object wrap(Object arg) {
        if (arg == null) {
            return arg;
        }
        if (arg.getClass().isArray()) {
            switch (arg.getClass().getSimpleName()) {
                case "boolean[]":
                    return booleanArrayWrapper.getbooleanArrayWrapper((boolean[]) arg);
                case "byte[]":
                    return byteArrayWrapper.getbyteArrayWrapper((byte[]) arg);
                case "char[]":
                    return charArrayWrapper.getcharArrayWrapper((char[]) arg);
                case "double[]":
                    return doubleArrayWrapper.getdoubleArrayWrapper((double[]) arg);
                case "float[]":
                    return floatArrayWrapper.getfloatArrayWrapper((float[]) arg);
                case "int[]":
                    return intArrayWrapper.getintArrayWrapper((int[]) arg);
                case "long[]":
                    return longArrayWrapper.getlongArrayWrapper((long[]) arg);
                case "short[]":
                    return shortArrayWrapper.getshortArrayWrapper((short[]) arg);
                default:
                    return ObjectArrayWrapper.getObjectArrayWrapper((Object[]) arg);
            }
        }
        return arg;
    }

    static public final Class unwrapClass(Class c) {
        if (c==null) return null;
        if (c.equals(ObjectArrayWrapper.class)) return Object[].class;
        else if (c.equals(booleanArrayWrapper.class)) return boolean[].class;
        else if (c.equals(byteArrayWrapper.class)) return byte[].class;
        else if (c.equals(charArrayWrapper.class)) return char[].class;
        else if (c.equals(doubleArrayWrapper.class)) return double[].class;
        else if (c.equals(floatArrayWrapper.class)) return float[].class;
        else if (c.equals(intArrayWrapper.class)) return int[].class;
        else if (c.equals(longArrayWrapper.class)) return long[].class;
        else if (c.equals(shortArrayWrapper.class)) return short[].class;
        return c;
    }

    static int[] getIntArray(int... args){
        return args;
    }

}
