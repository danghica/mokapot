package xyz.acygn.millr.localsemantics.impmill;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xyz.acygn.millr.MillUtil;
import xyz.acygn.millr.util.util.MillredClass;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

/**
 * Test class for testing the functionality of
 * @author Marcello De Bernardi
 */
@SuppressWarnings("Duplicates")
class ImplementMilledTransformationTest {
    private static Class<?> newClass;

    @BeforeAll
    static void init() {
        // partial milling of the new class

        List<String> classes = Arrays.asList("ImplementMilledTransformationSample");

        try {
            // mill the original class

            // get instances of original and milled classes
            newClass = MillUtil.mill("localsemantics/impmill/", classes).get(0).getClass();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests that the new class is correctly a subtype of {@link MillredClass}.
     */
    @Test
    void shouldImplementMilledClass() {
        assertTrue(MillredClass.class.isAssignableFrom(newClass));
    }

    /**
     * Tests that the new class correctly includes the method "getOriginalClass".
     */
    @Test
    void getOriginalClass_ShouldExist() {
        assertTrue(Arrays.stream(newClass.getDeclaredMethods()).anyMatch((method) ->
                method.getName().equals("getOriginalClass")));
    }

    /**
     * Tests whether the method "getOriginalClass" has the right return type.
     */
    @Test
    void getOriginalClass_ShouldHaveRightReturnType() {
        try {
            assertEquals(
                    newClass.getDeclaredMethod("getOriginalClass").getReturnType(),
                    Class.class);
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests whether the method getOriginalClass correctly returns the original class.
     */
    @Test
    void getOriginalClass_shouldReturnOriginalClass() {
        try {
            Object milled = newClass.getDeclaredConstructors()[0].newInstance();
            Class original = ImplementMilledTransformationSample.class;
            Object returnedOriginal = newClass.getDeclaredMethod("getOriginalClass").invoke(milled);

            System.out.println("Expected: " + original.getName());
            System.out.println("Actual: " + ((Class) returnedOriginal).getName());

            assertEquals(original, returnedOriginal);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
