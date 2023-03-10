package xyz.acygn.millr;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static xyz.acygn.millr.TypeUtil.getArgumentsAndReturnTypes;
import static xyz.acygn.millr.TypeUtil.getPrimType;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import util.*;
import xyz.acygn.millr.util.util.*;

import java.io.BufferedReader;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;


/**
 * Tests for {@link TypeUtil}.
 *
 * @author Marcello De Bernardi & David Hopes
 */
class TypeUtilTest {

    /**
     * Tests whether the getObjectType method returns the correct types given their
     * internal names.
     */
    @Test
    void getObjectType_shouldReturnCorrectTypesForPrimitiveInternalNames() {
        assertAll("primitive types",
                () -> assertEquals(Type.BOOLEAN_TYPE, TypeUtil.getObjectType("boolean")),
                () -> assertEquals(Type.BYTE_TYPE, TypeUtil.getObjectType("byte")),
                () -> assertEquals(Type.CHAR_TYPE, TypeUtil.getObjectType("char")),
                () -> assertEquals(Type.DOUBLE_TYPE, TypeUtil.getObjectType("double")),
                () -> assertEquals(Type.FLOAT_TYPE, TypeUtil.getObjectType("float")),
                () -> assertEquals(Type.INT_TYPE, TypeUtil.getObjectType("int")),
                () -> assertEquals(Type.LONG_TYPE, TypeUtil.getObjectType("long")),
                () -> assertEquals(Type.SHORT_TYPE, TypeUtil.getObjectType("short")),
                () -> assertEquals(Type.VOID_TYPE, TypeUtil.getObjectType("void")),
                () -> assertEquals(Type.getObjectType("java/lang/String"), TypeUtil.getObjectType("java/lang/String"))
            );
    }

    /**
     * Passing null to getObjectType should not be done, as doing so lacks a meaningful interpretation.
     * The method should throw a NullPointerException.
     */
    @Test
    void getObjectType_shouldThrowNullPointerExceptionWhenPassingNull() {
        assertThrows(NullPointerException.class, () -> TypeUtil.getObjectType(null));
    }

    /**
     * Passing a signature that has an array component should return true
     * The signature here is a List of String Arrays
     */
    @Test
    void hasArrayTest_shouldReturnTrueWithArrayComponent() {
        assertTrue(TypeUtil.hasArray("Ljava/util/List<[Ljava/lang/String;>;"));
    }

    /**
     * Passing a signature that has an array component should return true
     * The signature here is an array of List of Strings
     */
    @Test
    void hasArrayTest_shouldReturnTrueWithComponentArrayOfLists() {
        assertTrue(TypeUtil.hasArray("[Ljava/util/List<Ljava/lang/String;>;"));
    }

    /**
     * Passing a signature that has no array component should return false
     * The signature here is a List of Strings
     */
    @Test
    void hasArrayTest_shouldReturnFalseWithNoArrayComponent() {
        assertFalse(TypeUtil.hasArray("Ljava/util/List<Ljava/lang/String;>;"));
    }

    /**
     * Passing a signature that has no array component should return false
     * The signature here is a List of Strings
     */
    @Test
    void hasArrayTest_shouldReturnFalseWithPrimitive() {
        assertFalse(TypeUtil.hasArray("I"));
    }


    /**
     * Tests whether isPrimitive correctly identify Object as a non-primitive type.
     */
    @Test
    void isPrimitive_shouldReturnFalseForObject() {
        Type object = Type.getType(Object.class);
        assertFalse(TypeUtil.isPrimitive(object));
    }

    /**
     * Tests that the type utils correctly differentiate a primitive array type from a primitive type.
     */
    @Test
    void isPrimitive_shouldReturnFalseOnPrimitiveArray() {
        Type primitiveArray = Type.getType(int[].class);

        assertFalse(TypeUtil.isPrimitive(primitiveArray));
    }

    /**
     * Tests whether the type utils correctly identify Java's wrapper types as non-primitives.
     */
    @Test
    void isPrimitive_shouldReturnFalseOnWrappers() {
        Type booleanWrapper = Type.getType(Boolean.class);
        Type byteWrapper = Type.getType(Byte.class);
        Type charWrapper = Type.getType(Character.class);
        Type doubleWrapper = Type.getType(Double.class);
        Type floatWrapper = Type.getType(Float.class);
        Type intWrapper = Type.getType(Integer.class);
        Type longWrapper = Type.getType(Long.class);
        Type shortWrapper = Type.getType(Short.class);

        assertFalse(TypeUtil.isPrimitive(booleanWrapper));
        assertFalse(TypeUtil.isPrimitive(byteWrapper));
        assertFalse(TypeUtil.isPrimitive(charWrapper));
        assertFalse(TypeUtil.isPrimitive(doubleWrapper));
        assertFalse(TypeUtil.isPrimitive(floatWrapper));
        assertFalse(TypeUtil.isPrimitive(intWrapper));
        assertFalse(TypeUtil.isPrimitive(longWrapper));
        assertFalse(TypeUtil.isPrimitive(shortWrapper));
    }

    /**
     * Tests whether the type utils correctly identify primitive types.
     */
    @Test
    void isPrimitive_shouldReturnTrueForPrimitives() {
        Type booleanPrimitive = Type.getType(boolean.class);
        Type bytePrimitive = Type.getType(byte.class);
        Type charPrimitive = Type.getType(char.class);
        Type doublePrimitive = Type.getType(double.class);
        Type floatPrimitive = Type.getType(float.class);
        Type intPrimitive = Type.getType(int.class);
        Type longPrimitive = Type.getType(long.class);
        Type shortPrimitive = Type.getType(short.class);

        assertAll(
                () -> assertTrue(TypeUtil.isPrimitive(booleanPrimitive)),
                () -> assertTrue(TypeUtil.isPrimitive(bytePrimitive)),
                () -> assertTrue(TypeUtil.isPrimitive(charPrimitive)),
                () -> assertTrue(TypeUtil.isPrimitive(doublePrimitive)),
                () -> assertTrue(TypeUtil.isPrimitive(floatPrimitive)),
                () -> assertTrue(TypeUtil.isPrimitive(intPrimitive)),
                () -> assertTrue(TypeUtil.isPrimitive(longPrimitive)),
                () -> assertTrue(TypeUtil.isPrimitive(shortPrimitive))
        );
    }

    /**
     * Tests whether isArrayWrapper correctly identifies all of Millr's array wrappers
     * as array wrapper types.
     */
    @Test
    void isArrayWrapper_shouldReturnTrueForMillrPrimitiveArrayWrappers() {
        assertAll(
                () -> assertTrue(TypeUtil.isArrayWrapper(Type.getType(booleanArrayWrapper.class))),
                () -> assertTrue(TypeUtil.isArrayWrapper(Type.getType(byteArrayWrapper.class))),
                () -> assertTrue(TypeUtil.isArrayWrapper(Type.getType(charArrayWrapper.class))),
                () -> assertTrue(TypeUtil.isArrayWrapper(Type.getType(doubleArrayWrapper.class))),
                () -> assertTrue(TypeUtil.isArrayWrapper(Type.getType(floatArrayWrapper.class))),
                () -> assertTrue(TypeUtil.isArrayWrapper(Type.getType(intArrayWrapper.class))),
                () -> assertTrue(TypeUtil.isArrayWrapper(Type.getType(longArrayWrapper.class))),
                () -> assertTrue(TypeUtil.isArrayWrapper(Type.getType(shortArrayWrapper.class)))
        );
    }

    /**
     * Tests whether isArrayWrapper correctly identifies millr's Object array wrappers are
     * array wrapper types.
     */
    @Test
    void isArrayWrapper_shouldReturnTrueForMillrObjectArrayWrapper() {
        assertTrue(TypeUtil.isArrayWrapper(Type.getType(ObjectArrayWrapper.class)));
    }

    /**
     * Tests whether isArrayWrapper correctly returns false for other classes that are not
     * millr array wrappers. The classes tested are all from the {@link xyz.acygn.millr.util}
     * package, because it is possible that a list of array wrapper classes may accidentally
     * be initialized to contain the other classes in the package as well.
     */
    @Test
    void isArrayWrapper_shouldReturnFalseForOtherClasses() {
        assertAll(
                () -> assertFalse(TypeUtil.isArrayWrapper(Type.getType(LatchThread.class))),
                () -> assertFalse(TypeUtil.isArrayWrapper(Type.getType(MillrTestImplementation.class))),
//                () -> assertFalse(TypeUtil.isArrayWrapper(Type.getType(ObjectWeakHashMap.class))),
                () -> assertFalse(TypeUtil.isArrayWrapper(Type.getType(RuntimeUnwrapper.class))),
                () -> assertFalse(TypeUtil.isArrayWrapper(Type.getType(SeparationClass.class)))
        );
    }

    /**
     * Tests whether a non-primitive type is correctly given an opcode of -1.
     */
    @Test
    void getPrimOpcode_shouldReturnNegativeOpcodeForNonPrimitive() {
        Type object = Type.getType(Object.class);
        Type array = Type.getType(int[].class);

        assertEquals(-1, TypeUtil.getPrimOpcode(object));
        assertEquals(-1, TypeUtil.getPrimOpcode(array));
    }

    /**
     * Tests whether the type utils correctly return the matching opcodes for primitive types.
     */
    @Test
    void getPrimOpcode_shouldReturnMatchingOpcodes() {
        assertAll(
                () -> assertEquals(Opcodes.T_BOOLEAN, TypeUtil.getPrimOpcode(Type.getType(boolean.class))),
                () -> assertEquals(Opcodes.T_BYTE, TypeUtil.getPrimOpcode(Type.getType(byte.class))),
                () -> assertEquals(Opcodes.T_CHAR, TypeUtil.getPrimOpcode(Type.getType(char.class))),
                () -> assertEquals(Opcodes.T_DOUBLE, TypeUtil.getPrimOpcode(Type.getType(double.class))),
                () -> assertEquals(Opcodes.T_FLOAT, TypeUtil.getPrimOpcode(Type.getType(float.class))),
                () -> assertEquals(Opcodes.T_INT, TypeUtil.getPrimOpcode(Type.getType(int.class))),
                () -> assertEquals(Opcodes.T_LONG, TypeUtil.getPrimOpcode(Type.getType(long.class))),
                () -> assertEquals(Opcodes.T_SHORT, TypeUtil.getPrimOpcode(Type.getType(short.class))),
                () -> assertEquals(-1, TypeUtil.getPrimOpcode(Type.getType(void.class))));
    }

    /**
     * Tests that the type utils can provide the correct primitive wrapper types.
     */
    @Test
    void getPrimWrapperType_shouldReturnCorrectPrimWrappers() {
        assertAll(
                () -> assertEquals(Type.getType(Integer.class), TypeUtil.getPrimWrapperType(Type.getType(int.class))),
                () -> assertEquals(Type.getType(Byte.class), TypeUtil.getPrimWrapperType(Type.getType(byte.class))),
                () -> assertEquals(Type.getType(Character.class), TypeUtil.getPrimWrapperType(Type.getType(char.class))),
                () -> assertEquals(Type.getType(Boolean.class), TypeUtil.getPrimWrapperType(Type.getType(boolean.class))),
                () -> assertEquals(Type.getType(Double.class), TypeUtil.getPrimWrapperType(Type.getType(double.class))),
                () -> assertEquals(Type.getType(Float.class), TypeUtil.getPrimWrapperType(Type.getType(float.class))),
                () -> assertEquals(Type.getType(Short.class), TypeUtil.getPrimWrapperType(Type.getType(short.class))),
                () -> assertEquals(Type.getType(Long.class), TypeUtil.getPrimWrapperType(Type.getType(long.class)))
        );
    }

    /**
     * Tests that the getPrimWrapper throws an UnsupportedOperationException when passed an
     * Object type as argument.
     */
    @Test
    void getPrimWrapper_shouldThrowUnsupportedOperationExceptionForNonPrimitives() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TypeUtil.getPrimWrapperType(Type.getType(Integer.class))
        );
    }

    /**
     * Tests that the getPrimWrapper throws an UnsupportedOperationException when passed an
     * Opcode of type as argument.
     */
    @Test
    void getPrimType_shouldThrowIllegalArgumentExceptionForNonPrimitive() {
        assertThrows( IllegalArgumentException.class,
                () -> getPrimType(33)
        );
    }

    /**
     * Tests that the getPrimWrapper throws an UnsupportedOperationException when passed an
     * Opcode of type as argument.
     */
    @Test
    void getPrimType_shouldReturnCorrectTypeForPrimitives() {
        assertAll(
                () -> assertEquals(Type.INT_TYPE, getPrimType(Opcodes.T_INT)),
                () -> assertEquals(Type.BYTE_TYPE, getPrimType(Opcodes.T_BYTE)),
                () -> assertEquals(Type.CHAR_TYPE, getPrimType(Opcodes.T_CHAR)),
                () -> assertEquals(Type.BOOLEAN_TYPE, getPrimType(Opcodes.T_BOOLEAN)),
                () -> assertEquals(Type.DOUBLE_TYPE, getPrimType(Opcodes.T_DOUBLE)),
                () -> assertEquals(Type.FLOAT_TYPE, getPrimType(Opcodes.T_FLOAT)),
                () -> assertEquals(Type.LONG_TYPE, getPrimType(Opcodes.T_LONG)),
                () -> assertEquals(Type.SHORT_TYPE, getPrimType(Opcodes.T_SHORT))
        );
    }

    /**
     * Tests that getArgumentsAndReturnTypes returns a correct list of the argument and return types of a method
     * Method descriptor is for method with void return type and int and String parameters
     */
    @Test
    void getArgumentsAndReturnTypesTest_Args_VoidReturn() {
        Type[] array = new Type[]{Type.getType("I"), Type.getType("Ljava/lang/String;"), Type.getType("V")};
        System.out.println("array:");
        Arrays.stream(array).forEach(System.out::println);
        Type[] returnedArray = getArgumentsAndReturnTypes("(ILjava/lang/String;)V");
        System.out.println("returnedArray:");
        Arrays.stream(returnedArray).forEach(System.out::println);
        assertTrue(Arrays.deepEquals(array, returnedArray));
    }

    /**
     * Tests that getArgumentsAndReturnTypes returns a correct list of the argument and return types of a method
     * Method descriptor is for method with int return type and int and String parameters
     */
    @Test
    void getArgumentsAndReturnTypesTest_Args_IntReturn() {
        Type[] array = new Type[]{Type.getType("I"), Type.getType("Ljava/lang/String;")};
        System.out.println("array:");
        Arrays.stream(array).forEach(System.out::println);
        Type[] returnedArray = getArgumentsAndReturnTypes("(ILjava/lang/String;)I");
        System.out.println("returnedArray:");
        Arrays.stream(returnedArray).forEach(System.out::println);
        assertTrue(Arrays.deepEquals(array, returnedArray));
    }

    /**
     * Tests whether isSubtype correctly identifies that inheritance is a reflexive relation
     */
    @Test
    void isSubtype_everythingIsASubtypeOfItself() {
        assertAll(
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(Integer.class),
                        Type.getType(Integer.class))),
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(Integer.class).toString(),
                        Type.getType(Integer.class).toString()))
        );
    }

    /**
     * Tests whether isSubtype correctly identifies that all classes are subtypes of Object
     */
    @Test
    void isSubtype_everythingIsASubtypeOfObject() {
        assertAll(
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(Integer.class),
                        Type.getType(Object.class))),
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(Integer.class).toString(),
                        Type.getType(Object.class).toString())),
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(String.class),
                        Type.getType(Object.class))),
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(String.class).toString(),
                        Type.getType(Object.class).toString())),
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(Exception.class),
                        Type.getType(Object.class))),
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(Exception.class).toString(),
                        Type.getType(Object.class).toString()))
        );
    }

    /**
     * As with all other classes, Object too adheres to the rule that it is a subclass of itself.
     * However this should be tested separately as it could easily be an edge case in the implementation.
     */
    @Test
    void isSubtype_objectIsSubclassOfItself() {
        assertAll(
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(Object.class),
                        Type.getType(Object.class))),
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(Object.class).toString(),
                        Type.getType(Object.class).toString()))
        );
    }

    /**
     * Tests whether object is correctly identified as not being a subtype of anything.
     */
    @Test
    void isSubtype_objectIsNotSubtypeOfAnything() {
        assertAll(
                () -> assertFalse(TypeUtil.isSubType(
                        Type.getType(Object.class),
                        Type.getType(Integer.class))),
                () -> assertFalse(TypeUtil.isSubType(
                        Type.getType(Object.class).toString(),
                        Type.getType(Integer.class).toString())),
                () -> assertFalse(TypeUtil.isSubType(
                        Type.getType(Object.class),
                        Type.getType(String.class))),
                () -> assertFalse(TypeUtil.isSubType(
                        Type.getType(Object.class).toString(),
                        Type.getType(String.class).toString())),
                () -> assertFalse(TypeUtil.isSubType(
                        Type.getType(Object.class),
                        Type.getType(Exception.class))),
                () -> assertFalse(TypeUtil.isSubType(
                        Type.getType(Object.class).toString(),
                        Type.getType(Exception.class).toString()))
        );
    }

    /**
     * Tests that a type is correctly identified as a subtype of an interface it implements..
     */
    @Test
    void isSubtype_shouldReturnTrueForImplementedInterface() {
        assertAll(
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(BufferedReader.class),
                        Type.getType(Readable.class))),
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(BufferedReader.class).toString(),
                        Type.getType(Readable.class).toString()))
        );
    }

    /**
     * Tests that a type is correctly identified as not being a subtype of an interface
     * it does not implement.
     */
    @Test
    void isSubtype_shouldReturnFalseForNotImplementedInterface() {
        assertAll(
                () -> assertFalse(TypeUtil.isSubType(
                        Type.getType(BufferedReader.class),
                        Type.getType(Serializable.class))),
                () -> assertFalse(TypeUtil.isSubType(
                        Type.getType(BufferedReader.class).toString(),
                        Type.getType(Serializable.class).toString()))
        );
    }

    /**
     * Tests that classes with no inheritance relationship are correctly identified as not
     * being subtypes.
     */
    @Test
    void isSubtype_unrelatedClassesAreNotSubtypes() {
        assertAll(
                () -> assertFalse(TypeUtil.isSubType(
                        Type.getType(String.class),
                        Type.getType(Integer.class))),
                () -> assertFalse(TypeUtil.isSubType(
                        Type.getType(String.class).toString(),
                        Type.getType(Integer.class).toString()))
        );
    }

    /**
     * Tests that interfaces are correctly identified as subtypes of interfaces they extend.
     */
    @Test
    void isSubtype_interfacesAreSubtypesOfInterfacesTheyExtend() {
        assertAll(
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(SortedSet.class),
                        Type.getType(Set.class))),
                () -> assertTrue(TypeUtil.isSubType(
                        Type.getType(SortedSet.class).toString(),
                        Type.getType(Set.class).toString()))
        );
    }

    @Test
    void isNotProject_() {
        // todo method relies on filesystem functionality from Mill, figure out how to test
    }
}
