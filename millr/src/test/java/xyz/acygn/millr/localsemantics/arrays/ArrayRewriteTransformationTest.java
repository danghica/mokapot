package xyz.acygn.millr.localsemantics.arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xyz.acygn.millr.util.util.ArrayWrapper;
import xyz.acygn.millr.ArrayRewriteTransformation;
import xyz.acygn.millr.MillUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for semantic equivalence between original (unmilled) code and milled code.
 * The tests in this class compare the results of executing the original code with the
 * results obtained by executing the milled version of the code.
 *
 * @author Marcello De Bernardi
 */
class ArrayRewriteTransformationTest {
    private static ArbitraryMillable original;
    private static ArbitraryMillable milled;


    /**
     * Before any tests are run, the original sample class is milled and the milled version
     * is then loaded using a ClassLoader.
     */
    @BeforeAll
    @SuppressWarnings("Duplicates")
    static void init() {
        // partial milling of the new class. Note that the test relies on comparing
        // the behavior of the original class to the milled version. To simplify this,
        // we define an interface which is implemented by the original class, and therefore
        // also by the milled class.

        List<String> classes = new ArrayList<>(Collections.singletonList("ArbitraryUserCode"));
        try {
            // get instances of original and milled classes
            original = new ArbitraryUserCode();
            milled = (ArbitraryMillable) MillUtil.mill("localsemantics/arrays/", classes).get(0).getConstructors()[0].newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Before each test, checks that milling was actually successful, and only runs the test
     * if it was.
     */
    @BeforeEach
    void skipIfNotMilled() {
        org.junit.jupiter.api.Assumptions.assumeTrue(milled != null);
    }


    /**
     * Tests that the transformation correctly detects special method names that are not
     * to be changed by millr.
     */
    @Test
    void isMethodNameFixed_shouldDetectReservedKeywords() {
        assertTrue(ArrayRewriteTransformation.isMethodNameFixed("<init>")
                && ArrayRewriteTransformation.isMethodNameFixed("<clinit>"));
    }

    /**
     * Tests that the transformation leaves methods with special names unchanged.
     */
    @Test
    void getMethodName_shouldLeaveSpecialNamesUnchanged() {
        String init = "<init>";
        String clinit = "<clinit>";

        assertEquals(ArrayRewriteTransformation.getMethodName(init, ""), init);
        assertEquals(ArrayRewriteTransformation.getMethodName(clinit, ""), clinit);
    }

    /**
     * Tests that the transformation correctly returns the milled names of methods.
     */
    @Test
    void shouldReturnMilledMethodName() {
        // todo
    }


    ///////////////////////////////////////////////////////////////////////////////////////
    /////////////                          ACCESS                             /////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    // these tests pertain to simple access of array elements


    /**
     * Accessing an array index which is outside the range of valid indices results in a
     * {@link ArrayIndexOutOfBoundsException} being thrown. Tests that, where this occurs
     * in original code, the milled code (using {@link ArrayWrapper})
     * should have the same result.
     */
    @Test
    void accessArray_shouldThrowIndexOutOfBoundsException() {
        assertAll(
                () -> assertThrows(
                        ArrayIndexOutOfBoundsException.class,
                        () -> original.accessArray_indexOutOfBoundsException()),
                () -> assertThrows(
                        ArrayIndexOutOfBoundsException.class,
                        () -> milled.accessArray_indexOutOfBoundsException())
        );
    }

    /**
     * Accessing an array index returns the value at that index. Tests that, where such
     * an operation is performed in the original code, the milled code produces the same
     * result.
     */
    @Test
    void accessArray_shouldReturnCorrectElement() {

    }


    ///////////////////////////////////////////////////////////////////////////////////////
    /////////////                       ANNOTATIONS                           /////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    // these tests pertain to situations where an array is annotated


    /**
     * An annotation placed on the declaration of an array field should be retained when
     * milling and apply to the {@link ArrayWrapper} replacing the
     * original array.
     */
    @Test
    void annotations_fieldAnnotationShouldBeRetained() {
        assertAll(
                () -> assertTrue(original.annotations_getAnnotationFromArrayField()),
                () -> assertTrue(milled.annotations_getAnnotationFromArrayField())
        );
    }

    /**
     * An annotation placed on the declaration of a formal array parameter should be
     * retained when milling and apply to the {@link ArrayWrapper}
     * replacing the original array.
     */
    @Test
    void annotations_arrayParameterAnnotationsShouldBeRetained() {
        assertAll(
                () -> assertTrue(original.annotations_getAnnotationFromArrayParameter()),
                () -> assertTrue(milled.annotations_getAnnotationFromArrayParameter())
        );
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    /////////////                      API OPERATIONS                         /////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    // these tests pertain to situations where an array is fed to an unmilled API


    /**
     * Where an array reference is passed to an API method, and the original array variable
     * is then set to null in such a way that the only references to the array exist within
     * the API code, and a reference to the same array is eventually returned by the API, the
     * hashCode of the returned array should be the same as before calling the API. Any tests
     * for that property should return the same result in milled code.
     *
     * This test is of interest because it is possible that in the milled code the original
     * {@link ArrayWrapper} might be lost when the reference to it is
     * set to null (the API has a reference to the internal array but not the wrapper), and
     * a new wrapper might be created when the API eventually returns. If so, the semantics
     * of object identity testing would not be equivalent to the original code.
     */
    @Test
    void apiUsage_hashCodeShouldRemainSameAfterLosingArrayReference() {
        assertAll(
                () -> assertTrue(original.api_passArrayAndReleaseReference()),
                () -> assertTrue(milled.api_passArrayAndReleaseReference())
        );
    }

    /**
     * The {@code clone()} operation as applied on arrays returns an object of the same
     * type. Wherever this happens in original code, the same constraint should hold for
     * milled code.
     */
    @Test
    void clonedArray_shouldHaveSameTypeAsOriginal() {
        assertAll(
                () -> assertTrue(original.cloneArray_cloneHasSameTypeAsOriginal()),
                () -> assertTrue(milled.cloneArray_cloneHasSameTypeAsOriginal())
        );
    }

    /**
     * A cloned object does not have the same identity as its original object and, in cases
     * where the {@code equals()} has not been overridden, an equality test should return
     * false. Wherever this happens, the milled code should return false too.
     */
    @Test
    void clonedArray_shouldReturnFalseForEquals() {
        assertAll(
                () -> assertFalse(original.cloneArray_equalsShouldReturnFalse()),
                () -> assertFalse(milled.cloneArray_equalsShouldReturnFalse()));
    }

    /**
     * {@code Arrays.equal()} compares two arrays by their content rather than by
     * their identity. Wherever two arrays are compared in that manner, the milled code
     * should return the same result.
     */
    @Test
    void clonedArray_shouldReturnTrueForArraysEqual() {
        assertAll(
                () -> assertTrue(original.cloneArray_arraysEqualShouldReturnTrue()),
                () -> assertTrue(milled.cloneArray_arraysEqualShouldReturnTrue())
        );
    }

    /**
     * Arrays can be sorted using the {@code Arrays.sort()} method. Code that uses
     * this operation to sort an array should produce the same sorted array in the
     * milled version as well.
     */
    @Test
    void sortedArray_arraysSortShouldReturnSameResult() {
        int[] array = new int[10000];

        assertTrue(Arrays.equals(
                original.sortArray_sortUsingArraysSort(array),
                milled.sortArray_sortUsingArraysSort(array)));
    }


    ///////////////////////////////////////////////////////////////////////////////////////
    /////////////                        CONCURRENCY                          /////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    // these tests pertain to concurrent operations on arrays


    // todo


    ///////////////////////////////////////////////////////////////////////////////////////
    /////////////                      INITIALIZATION                         /////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    // these tests pertain to initialization of arrays


    /**
     * An uninitialized array variable is a null reference. Tests that this property is preserved
     * by milling; that is, where a test for the array variable being null returns a certain value,
     * the milled code (using {@link ArrayWrapper}) is expected to behave in
     * the same manner.
     */
    @Test
    void uninitializedArrayShouldBeNull() {
        assertEquals(
                original.defineArray_returnReferenceToUninitialized(),
                milled.defineArray_returnReferenceToUninitialized());
    }

    /**
     * An uninitialized array variable is a null reference. Tests that this property is preserved by
     * milling; that is, where an operation on an uninitialized array variable results in a
     * {@link NullPointerException} being throw, the equivalent operation on an
     * {@link ArrayWrapper} should also throw a {@link NullPointerException}.
     */
    @Test
    void uninitializedArrayShouldThrowNullPointerException() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> original.accessArray_throwsNullPointerException()),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> milled.accessArray_throwsNullPointerException())
        );
    }

    /**
     * The size of an array object is uniquely determined by the size to which it was initialized.
     * Tests that milled code also follows this constraint, and that a test for the size of an
     * {@link ArrayWrapper} returns the same value as a test for the array
     * size would.
     */
    @Test
    void initialization_lengthShouldBeSameAsDeclared() {
        assertEquals(original.defineArray_lengthIsImmutable(), milled.defineArray_lengthIsImmutable());
    }


    ///////////////////////////////////////////////////////////////////////////////////////
    /////////////                        REFLECTION                           /////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    // these tests pertain to reflective operations on arrays


    /**
     * Tests what happens when control flow is affected by reflection on the class of an
     * array declared locally.
     */
    @Test
    void reflection_controlFlowBasedOnClassOfLocalArray() {
        assertEquals(
                original.reflection_localArrayHasClassArray(),
                milled.reflection_localArrayHasClassArray()
        );
    }

    /**
     * Tests what happens when a method returns the runtime class of an array passed to it
     * as an argument.
     */
    @Test
    void reflection_getClassOfArgument() {
        int[] array = new int[2];

        assertAll(
                () -> assertEquals(
                        int[].class,
                        original.reflection_getClassOfArgument(array)),
                () -> assertEquals(
                        int[].class,
                        milled.reflection_getClassOfArgument(array))
        );
    }

    /**
     *
     */
    @Test
    void reflection_getComponentTypeOfArray() {
        int[] array = new int[10];

        assertEquals(original.reflection_getComponentType(array), milled.reflection_getComponentType(array));
    }

    /**
     * Array elements can be accessed using the {@link java.lang.reflect.Array} class. Where
     * such an operation is performed, milled code should be semantically equivalent.
     */
    @Test
    void reflection_getValueFromArrayUsingReflection() {
        int[] array = IntStream.range(0, 10).toArray();

        assertAll(
                () -> assertEquals(
                        1,
                        original.reflection_getValueFromArrayUsingReflection(array, 1)),
                () -> assertEquals(
                        1,
                        milled.reflection_getValueFromArrayUsingReflection(array, 1))
        );
    }

    /**
     * Arrays can be initialized using the {@link java.lang.reflect.Array} class. Where such
     * an operation is performed, milled code should perform semantically equivalently.
     */
    @Test
    void reflection_initialize1DUsingArrayClass() {
        assertAll(
                () -> assertEquals(
                        int[].class,
                        original.reflection_initialize1DUsingArraysClass().getClass()),
                () -> assertEquals(
                        int[].class,
                        milled.reflection_initialize1DUsingArraysClass().getClass())
        );
    }

    /**
     * Arrays can be initialized using the {@link java.lang.reflect.Array} class. Where
     * such an operation is performed, milled code should perform semantically equivalently.
     * In this test the initialization is for a two-dimensional array.
     */
    @Test
    void reflection_initialize2DUsingArrayClass() {
        assertAll(
                () -> assertEquals(
                        int[][].class,
                        original.reflection_initialize2DUsingArraysClass().getClass()),
                () -> assertEquals(
                        int[][].class,
                        milled.reflection_initialize2DUsingArraysClass().getClass())
        );
    }

    /**
     * Arrays can be initialized using the {@link java.lang.reflect.Array} class. Where
     * such an operation is performed, milled code should perform semantically equivalently.
     * In this test the initialization is for a three-dimensional array.
     */
    @Test
    void reflection_initialize3DUsingArrayClass() {
        assertAll(
                () -> assertEquals(
                        int[][][].class,
                        original.reflection_initialize3DUsingArraysClass().getClass()),
                () -> assertEquals(
                        int[][][].class,
                        milled.reflection_initialize3DUsingArraysClass().getClass())
        );
    }

    /**
     * Elements in arrays can be accessed and set using methods from {@link java.lang.reflect.Array}.
     * Where such an operation is performed, milled code should perform semantically
     * equivalently. In this test an element of an array is first set and then retrieved
     * using reflection.
     */
    @Test
    void reflection_setAnfGetValueFromArrayUsingArrayClass() {
        int[] array = IntStream.range(0, 10).toArray();
        int index = 3;
        int value = 54;

        assertAll(
                () -> assertEquals(
                        value,
                        original.reflection_setAndGetValueFromArrayUsingArrayClass(array, index, value)),
                () -> assertEquals(
                        value,
                        milled.reflection_setAndGetValueFromArrayUsingArrayClass(array, index, value))
        );
    }
}
