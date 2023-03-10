package xyz.acygn.millr.localsemantics.visib;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import xyz.acygn.millr.localsemantics.getset.GetterSetterMillableSample;

/**
 * Tests for the VisibilityTransformation class, which is supposed to change the access
 * modifiers of a class to be distributed by mokapot.
 *
 * @author Marcello De Bernardi
 */
class VisibilityTransformationTest {
    private static GetterSetterMillableSample original;
    private static GetterSetterMillableSample milled;

    @BeforeEach
    @SuppressWarnings("Duplicates")
    void init() {
        // partial milling of the new class. Note that the test relies on comparing
        // the behavior of the original class to the milled version. To simplify this,
        // we define an interface which is implemented by the original class, and therefore
        // also by the milled class.

//        List<String> classes = new ArrayList<>(Arrays.asList(
//                "GetterSetterSample",
//                "Data",
//                "DataSubClass"
//        ));
//
//        try {
//            // mill the original class
//            Mill.main(arguments.toArray(new String[0]));
//
//            // get instances of original and milled classes
//            original = new GetterSetterSample();
//            milled = (GetterSetterMillableSample)
//                    ClassLoader.getSystemClassLoader()
//                            .loadClass(millrPrefix + classes.get(0))
//                            .newInstance();
//
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Test
    void someTest() {

    }

}
