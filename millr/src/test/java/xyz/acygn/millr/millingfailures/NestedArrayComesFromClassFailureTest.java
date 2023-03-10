package xyz.acygn.millr.millingfailures;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import org.junit.jupiter.api.Test;
import xyz.acygn.millr.MillUtil;

import java.util.Arrays;

/**
 * Test runner for milling {@link NestedArrayComesFromClassFailureExample}.
 *
 * @author Marcello De Bernardi
 */
class NestedArrayComesFromClassFailureTest {
    @Test
    void test() throws Exception {
        MillUtil.mill("millingfailures/", Arrays.asList(
                "NestedArrayComesFromClassFailureExample",
                "NestedArrayComesFromClassFailureExample2"));
    }
}
