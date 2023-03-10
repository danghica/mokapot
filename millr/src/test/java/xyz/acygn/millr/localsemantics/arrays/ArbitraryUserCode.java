package xyz.acygn.millr.localsemantics.arrays;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

/**
 * A class with heavy emphasis on array operations which stands in for any
 * arbitrary code a user of mokapot/millr might write.
 */
@SuppressWarnings("ConstantConditions")
class ArbitraryUserCode implements ArbitraryMillable {
    @ArbitraryAnnotation
    private int[] annotatedArrayField;


    /**
     * An arbitrary method which has an annotated formal array parameter.
     *
     * @param array an annotated array
     * @return false
     */
    public boolean arbitraryMethod(@ArbitraryAnnotation int[] array) {
        return false;
    }

    @Override
    public boolean accessArray_throwsNullPointerException() throws NullPointerException {
        int[] array = null;
        return array.length == 0;
    }

    @Override
    public int accessArray_returnElementAtSpecificPosition() {
        return 0; // TODO: 24/07/2018
    }

    @Override
    public boolean annotations_getAnnotationFromArrayField() {
        try {
            return this.getClass()
                    .getDeclaredField("annotatedArrayField")
                    .getDeclaredAnnotation(ArbitraryAnnotation.class) != null;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean annotations_getAnnotationFromArrayParameter() {
        try {
            return this.getClass()
                    .getDeclaredMethod("arbitraryMethod", int[].class)
                    .getParameterAnnotations()[0][0]
                    .annotationType()
                    .equals(ArbitraryAnnotation.class);
        }
        catch (NoSuchMethodException e) {
            return false;
        }
    }


    @Override
    public boolean defineArray_returnReferenceToUninitialized() {
        int[] array = null;
        return array == null;
    }

    @Override
    public int defineArray_lengthIsImmutable() {
        return new int[27].length;
    }

    @Override
    public boolean accessArray_indexOutOfBoundsException() throws ArrayIndexOutOfBoundsException {
        return (new int[10])[10] == 1;
    }

    @Override
    public boolean cloneArray_equalsShouldReturnFalse() {
        int[] array = new int[10];
        int[] otherArray = array.clone();

        return array.equals(otherArray);
    }

    @Override
    public boolean cloneArray_arraysEqualShouldReturnTrue() {
        int[] array = new int[10];
        int[] otherArray;

        for (int i = 0; i < array.length; i++)
            array[i] = i;

        otherArray = array.clone();

        return Arrays.equals(array, otherArray);
    }

    @Override
    public boolean cloneArray_cloneHasSameTypeAsOriginal() {
        int[] array = new int[10];
        Arrays.setAll(array, (i) -> i);

        int[] anotherArray = array.clone();

        return anotherArray.getClass().equals(array.getClass());
    }

    @Override
    public boolean api_passArrayAndReleaseReference() throws InterruptedException {
        int[] array = new int[50000];
        int originalHashCode;
        int newHashCode;

        Random rng = new Random();
        Arrays.setAll(array, (i) -> rng.nextInt());

        originalHashCode = array.hashCode();
        ArbitraryAPI.passArray(array);
        array = null;


        Thread.sleep(2000);

        int[] someOtherArray = ArbitraryAPI.retrieveArray();
        newHashCode = someOtherArray.hashCode();

        return originalHashCode == newHashCode;
    }

    @Override
    public int[] sortArray_sortUsingArraysSort(int[] array) {
        initializeArrayAsIfRandom(array);
        Arrays.sort(array);
        return array;
    }

    @Override
    public Class<?> reflection_getClassOfArgument(int[] array) {
        return array.getClass();
    }

    @Override
    public Class<?> reflection_getComponentType(int[] array) {
        return array.getClass().getComponentType();
    }

    @Override
    public boolean reflection_localArrayHasClassArray() {
        int[] array = new int[10];

        return array.getClass().equals(int[].class);
    }

    @Override
    public int reflection_getValueFromArrayUsingReflection(int[] array, int index) {
        return (int) Array.get(array, index);
    }

    @Override
    public int[] reflection_initialize1DUsingArraysClass() {
        return (int[]) Array.newInstance(int.class, 10);
    }

    @Override
    public int[][] reflection_initialize2DUsingArraysClass() {
        return (int[][]) Array.newInstance(int.class, 10, 10);
    }

    @Override
    public int[][][] reflection_initialize3DUsingArraysClass() {
        return (int[][][]) Array.newInstance(int.class, 10, 10, 10);
    }

    @Override
    public int reflection_setAndGetValueFromArrayUsingArrayClass(int[] array, int index, int value) {
        Array.set(array, index, value);
        return (int) Array.get(array, index);
    }


    // HELPER: initializes an array to a fixed random-looking sequence
    private void initializeArrayAsIfRandom(int[] array) {
        Random rng = new Random(5);
        Arrays.setAll(array, (i) -> rng.nextInt());
    }

    // HELPER: initializes each array cell to its index
    private void initializeArrayAsIfLinear(int[] array) {
        Arrays.setAll(array, (i) -> i);
    }
}