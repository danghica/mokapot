package xyz.acygn.millr;

import java.io.File;

/**
 * Useful constants for dealing with paths in millr.
 *
 * @author thomas
 */
public final class PathConstants {
    public static final String ARRAY_FOLDER = "ArrayWrappingClasses/";
    public static final String MOKAPOT_DIRECT_ACCESS_NAME = "xyz/acygn/mokapot/LengthIndependent";
    public static final String GET_CLASS_METHOD = "getActualClass";
    public static final String IS_INSTANCE_METHOD = "isInstanceOf";
    public static final String IS_EQUAL_METHOD = "areReferenceEqual";
    public static final String GET_CAST_METHOD = "cast";
    public static final String GET_EQUALS_METHOD = "getEquals";

    public static final String BOOLEAN_ARRAY_WRAPPER_NAME = "xyz/acygn/millr/util/booleanArrayWrapper";
    public static final String BYTE_ARRAY_WRAPPER_NAME = "xyz/acygn/millr/util/byteArrayWrapper";
    public static final String CHAR_ARRAY_WRAPPER_NAME = "xyz/acygn/millr/util/charArrayWrapper";
    public static final String DOUBLE_ARRAY_WRAPPER_NAME = "xyz/acygn/millr/util/doubleArrayWrapper";
    public static final String FLOAT_ARRAY_WRAPPER_NAME = "xyz/acygn/millr/util/floatArrayWrapper";
    public static final String INT_ARRAY_WRAPPER_NAME = "xyz/acygn/millr/util/intArrayWrapper";
    public static final String LONG_ARRAY_WRAPPER_NAME = "xyz/acygn/millr/util/longArrayWrapper";
    public static final String SHORT_ARRAY_WRAPPER_NAME = "xyz/acygn/millr/util/shortArrayWrapper";
    public static final String OBJECT_ARRAY_WRAPPER_NAME = "xyz/acygn/millr/util/ObjectArrayWrapper";

    public static final String PRIMITIVE_WRAPPER = "xyz/acygn/millr/util/primitiveWrapper";
    public static final String BOOLEAN_WRAPPER = PRIMITIVE_WRAPPER + "$booleanWrapper";
    public static final String BYTE_WRAPPER = PRIMITIVE_WRAPPER + "$byteWrapper";
    public static final String CHAR_WRAPPER = PRIMITIVE_WRAPPER + "$charWrapper";
    public static final String DOUBLE_WRAPPER = PRIMITIVE_WRAPPER + "$doubleWrapper";
    public static final String FLOAT_WRAPPER = PRIMITIVE_WRAPPER + "$floatWrapper";
    public static final String INT_WRAPPER = PRIMITIVE_WRAPPER + "$intWrapper";
    public static final String LONG_WRAPPER = PRIMITIVE_WRAPPER + "$longWrapper";
    public static final String SHORT_WRAPPER = PRIMITIVE_WRAPPER + "$shortWrapper";


    public static final String[] LIST_ARRAY_WRAPPER = {BOOLEAN_ARRAY_WRAPPER_NAME, BYTE_ARRAY_WRAPPER_NAME,
            CHAR_ARRAY_WRAPPER_NAME, FLOAT_ARRAY_WRAPPER_NAME, DOUBLE_ARRAY_WRAPPER_NAME, INT_ARRAY_WRAPPER_NAME,
            LONG_ARRAY_WRAPPER_NAME, SHORT_ARRAY_WRAPPER_NAME, OBJECT_ARRAY_WRAPPER_NAME};


    public static final String RUNTIME_UNWRAPPER_CLASS = "xyz/acygn/millr/util/RuntimeUnwrapper";
    public static final String MILLR_INTERFACE = "xyz/acygn/millr/util/MillredClass";
}

