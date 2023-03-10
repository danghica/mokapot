package xyz.acygn.millr.localsemantics.getset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xyz.acygn.millr.MillUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests whether the semantics of programs are changed with regards to accessing
 * fields.
 *
 * @author Marcello De Bernardi
 */
class GetterSetterTransformationTest {
    private static GetterSetterMillableSample original;
    private static GetterSetterMillableSample milled;
    private static Class<Data> dataClass;
    private static Class<?> _millr_dataClass;


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

        List<String> classes = new ArrayList<>(Arrays.asList(
                "GetterSetterSample",
                "Data",
                "DataSubClass"
        ));

        try {
            // mill the original class
            List<Class> milledClasses = MillUtil.mill("localsemantics/getset/", classes);

            // get instances of original and milled classes
            original = new GetterSetterSample();
            milled = (GetterSetterMillableSample) milledClasses.get(0).getConstructors()[0].newInstance();
            dataClass = Data.class;
            _millr_dataClass = milledClasses.get(1).getClass();
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
     * Tests that every variable correctly receives a getter and a setter.
     */
    @Test
    void shouldAddGettersToCorrectFields() {
        Field[] fields = Data.class.getDeclaredFields();
        // List<Method> methods = Arrays.asList(_millr_dataClass.getDeclaredMethods());

        // for (Field f : fields) {
            // System.out.print(f.getName() + " ");
        // }

        // for (Method m : methods) {
            // System.out.print(m.getName() + " ");
        // }

        for (Field field : fields) {
            assertAll(
                    () -> assertTrue(Arrays.stream(_millr_dataClass.getDeclaredMethods())
                            .anyMatch((method) ->
                                    method.getName().equalsIgnoreCase("_millr_" + field.getName() + "set"))),
                    () -> assertTrue(Arrays.stream(_millr_dataClass.getDeclaredMethods())
                            .anyMatch((method) ->
                                    method.getName().equalsIgnoreCase("_millr_" + field.getName() + "get")))
            );
        }

    }

    /**
     * Tests whether the semantics regarding direct access to public static final variables
     * (i.e. class constants) are preserved by milling.
     */
    @Test
    void shouldPreserveSemanticsForConstants() {
        assertEquals(original.getPublicStaticFinalVariable(), milled.getPublicStaticFinalVariable());
    }

    /**
     * Tests whether the semantics regarding direct access to public static variables are
     * preserved by milling.
     */
    @Test
    void shouldPreserveSemanticsForClassVariables() {
        assertEquals(original.getPublicStaticVariable(), milled.getPublicStaticVariable());
        assertEquals(original.setAndGetPublicStaticVariable(), milled.setAndGetPublicStaticVariable());
    }

    /**
     * Tests whether the semantics regarding direct access to public instance variables
     * are preserved by milling.
     */
    @Test
    void shouldPreserveSemanticsForInstanceVariables() {
        assertEquals(original.getPublicVariable(), milled.getPublicVariable());
        assertEquals(original.setAndGetPublicVariable(), milled.setAndGetPublicVariable());
    }

    /**
     * Tests whether the semantics regarding the access of overridden fields using the
     * keyword "this" are preserved by milling.
     */
    @Test
    void shouldPreserveSemanticsOfThis() {
        assertEquals(original.getThisProtectedVariable(), milled.getThisProtectedVariable());
    }

    /**
     * Tests whether the semantics regarding the access of overridden fields using the
     * keyword "super" are preserved by milling.
     */
    @Test
    void shouldPreserveSemanticsOfSuper() {
        assertEquals(original.getSuperProtectedVariable(), milled.getSuperProtectedVariable());
    }

    /**
     * Tests whether the semantics regarding the access of overridden fields using neither
     * "this" nor "super" are preserved.
     */
    @Test
    void shouldPreserveSemanticsOfOverriding() {
        assertEquals(original.getOverridingVariable(), milled.getOverridingVariable());
    }
}
