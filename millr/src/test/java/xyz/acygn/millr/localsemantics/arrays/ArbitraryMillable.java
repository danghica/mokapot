package xyz.acygn.millr.localsemantics.arrays;

/**
 * Defines a class with methods that exercise the semantics of arrays. The names
 * of methods follows the following nomenclature to deal with the large number
 * of possible cases:
 * <p>
 * generalScenario_specificOperation
 *
 * @author Marcello De Bernardi
 */
interface ArbitraryMillable {

    ///////////////////////////////////////////////////////////////////////////////////////
    /////////////                          ACCESS                             /////////////
    ///////////////////////////////////////////////////////////////////////////////////////


    /**
     * Exercises the semantics of attempting to use an uninitialized array reference.
     *
     * @return true if throws null pointer exception, false if not
     */
    boolean accessArray_throwsNullPointerException();

    /**
     * Exercises the semantics of accessing a particular element from an array. Which
     * element is returned is not crucial, so long as the milled version of the code
     * returns the same one.
     *
     * @return an element from the array
     */
    int accessArray_returnElementAtSpecificPosition();


    ///////////////////////////////////////////////////////////////////////////////////////
    /////////////                       ANNOTATIONS                           /////////////
    ///////////////////////////////////////////////////////////////////////////////////////


    /**
     * Returns true if an annotation (scope: runtime) placed on an array field declaration
     * is retained and accessible at runtime.
     *
     * @return true if annotation remains, false if it does not
     */
    boolean annotations_getAnnotationFromArrayField();

    /**
     * Returns true if an annotation placed on an array parameter declaration is retained and
     * accessible at runtime.
     *
     * @return true if annotation remains, false if it does not
     */
    boolean annotations_getAnnotationFromArrayParameter();


    ///////////////////////////////////////////////////////////////////////////////////////
    /////////////                      API OPERATIONS                         /////////////
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Exercises the semantics of defining an array variable (but not initializing
     * it) and returning a reference to the uninitialized array.
     *
     * @return true if null pointer, false if not
     */
    boolean defineArray_returnReferenceToUninitialized();

    /**
     * Exercises the semantics of defining an array, initializing it to a given size,
     * and then returning that size.
     *
     * @return size of initialized array
     */
    int defineArray_lengthIsImmutable();

    /**
     * Exercises the semantics of defining an array, initializing it to a given size,
     * and then accessing an element whose index is beyond that size.
     *
     * @return // TODO: 23/07/2018
     */
    boolean accessArray_indexOutOfBoundsException();

    /**
     * Exercises the semantics of cloning an array and testing for equality between the
     * two objects.
     *
     * @return true if the objects are equal, false otherwise
     */
    boolean cloneArray_equalsShouldReturnFalse();

    /**
     * Exercises the semantics of cloning an array and testing for equality using
     * {@link java.util.Arrays}.equals.
     *
     * @return true if equal, false otherwise
     */
    boolean cloneArray_arraysEqualShouldReturnTrue();

    /**
     * Exercises the semantics of cloning an array and checking its apparent type
     * against the original array. For arrays, the type of the clone is the same
     * as that of the original array, but for objects this is not the case.
     *
     * @return true if the object has the same type as the original
     */
    boolean cloneArray_cloneHasSameTypeAsOriginal();


    /**
     * Exercises the semantics of passing an array reference to an API method and
     * then releasing the local reference to the array. The same array is then acquired
     * back from the API later.
     *
     * @return boolean if the hashCode of the original array is the same as the new one
     */
    boolean api_passArrayAndReleaseReference() throws InterruptedException;

    /**
     * Exercises the semantics of sorting an array by using the {@code Arrays.sort} method.
     *
     * @return the sorted array
     */
    int[] sortArray_sortUsingArraysSort(int[] array);


    ///////////////////////////////////////////////////////////////////////////////////////
    /////////////                        REFLECTION                           /////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    // these methods pertain to reflective operations on arrays

    /**
     * Exercises the semantics of querying for the class of an array object, and
     * returning the class.
     *
     * @param array array for which we want to return the class
     * @return class of array
     */
    Class<?> reflection_getClassOfArgument(int[] array);

    /**
     * Exercises the semantics of querying for the component type of an array,
     * and returning it.
     *
     * @param array array for which we want the component type
     * @return component type of array
     */
    Class<?> reflection_getComponentType(int[] array);

    /**
     * Exercises the semantics of making control flow decisions based on the class
     * of an object at runtime.
     *
     * @return true for one control flow path, false for the other
     */
    boolean reflection_localArrayHasClassArray();

    /**
     * Exercises the semantics of accessing an array element using methods from
     * {@link java.lang.reflect.Array}.
     *
     * @return the accessed element
     */
    int reflection_getValueFromArrayUsingReflection(int[] array, int index);

    /**
     * Exercises the semantics of creating an array using the {@link java.lang.reflect.Array}
     * class from Java's reflection API.
     *
     * @return the created array
     */
    int[] reflection_initialize1DUsingArraysClass();

    /**
     * Exercises the semantics of creating a 2D array using {@link java.lang.reflect.Array}
     * from Java's reflection API.
     *
     * @return the created array
     */
    int[][] reflection_initialize2DUsingArraysClass();

    /**
     * Exercises the semantics of creating a nested array using the {@link java.lang.reflect.Array}
     * class from Java's reflection API.
     *
     * @return the created array
     */
    int[][][] reflection_initialize3DUsingArraysClass();

    /**
     * Exercises the semantics of setting the value of an array index using {@code set()} from
     * {@link java.lang.reflect.Array}, and then returning the new value using {@code get()}.
     *
     * @param array the array to manipulate
     * @param index the index into the array to set and then get
     * @param value the value to set the array element to
     * @return the value at the given index, i.e. {@code value}
     */
    int reflection_setAndGetValueFromArrayUsingArrayClass(int[] array, int index, int value);

}
