package xyz.acygn.millr;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;



/**
 * Unit testing class for {@link MethodParameter}
 *
 * @author David Hopes
 */
public class MethodParameterTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    public MethodParameterTest() {
    }

    @BeforeEach
    void setupStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    /*
     * test that the default constructor initialises methodName correctly to "test_name"
     * uses field methodName
     */
    @Test
    void MethodParameterTest_Constructor_Field_methodName() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 1, "test_name", "test_description", "test_signature", new String[]{"test_interface1", "test_interface2"});
        assertEquals("test_name", mp.methodName);
    }

    /*
     * test that the default constructor initialises methodDesc correctly to "test_desc"
     * uses field methodDesc
     */
    @Test
    void MethodParameterTest_Constructor_Field_methodDesc() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 1, "test_name", "test_description", "test_signature", new String[]{"test_interface1", "test_interface2"});
        assertEquals(1, mp.methodAccess);
    }

    /*
     * test that the default constructor initialises methodSignature correctly to "test_signature"
     * uses field methodSignatues
     */
    @Test
    void MethodParameterTest_Constructor_Field_methodSignature() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 1, "test_name", "test_description", "test_signature", new String[]{"test_interface1", "test_interface2"});
        assertEquals("test_description", mp.methodDesc);
    }

    /*
     * test that the default constructor initialises isInterface correctly to "yes" with interfaces
     * uses field isInterface
     */
    @Test
    void MethodParameterTest_Constructor_Field_isInterface_true() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 1, "test_name", "test_description", "test_signature", new String[]{"test_interface1", "test_interface2"});
        assertEquals("test_signature", mp.methodSignature);
    }

    /*
     * test that the default constructor initialises isInterface correctly to "false" with no interfaces
     * uses field isInterface
     */
    @Test
    void MethodParameterTest_Constructor_Field_isInterface_false() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 999, "test_name", "test_description", "test_signature", new String[0]);
        assertFalse(mp.isInterface);
    }

    /*
     * test that the default constructor initialises methodAccess correctly to "1"
     * uses field methodAccess
     */
    @Test
    void MethodParameterTest_Constructor_Field_methodAccess() {
        ClassParameter cp = new ClassParameter(2, Opcodes.ACC_INTERFACE, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 1, "test_name", "test_description", "test_signature", new String[0]);
        assertTrue(mp.isInterface);
    }

    /*
     * test that the default constructor initialises cp correctly with a ClassParameter instance
     * uses field cp
     */
    @Test
    void MethodParameterTest_Constructor_Field_cp() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);
        assertEquals(cp.className, mp.className);
    }

    /*
     * test that the default constructor initialises Exceptions correctly to "{"test_exception1", "test_exception2"}
     * uses field Exceptions
     */
    @Test
    void MethodParameterTest_Constructor_Field_Exceptions() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[]{"test_exception1", "test_exception2"});
        assertTrue(Arrays.deepEquals(mp.Exceptions, new String[]{"test_exception1", "test_exception2"}));
    }

    /*
     * test that the method toString correctly returns a string representation of MethodParameter instance with Exceptions
     * uses method toString
     */
    @Test
    void MethodParameterTest_toString() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[]{"test_exception1", "test_exception2"});
        StringBuilder s = new StringBuilder(cp.toString());
        s.append("methodAccess: ");
        s.append(mp.methodAccess);
        s.append("\nmethodName: ");
        s.append(mp.methodName);
        s.append("\nmethodDesc: ");
        s.append(mp.methodDesc);
        s.append("\nmethodSignature: ");
        s.append(mp.methodSignature);
        s.append("\nExceptions: ");
        s.append(mp.Exceptions == null ? "null" : Arrays.toString(mp.Exceptions));

        assertEquals(s.toString(), mp.toString());
    }

    /*
     * test that the method toString correctly returns a string representation of MethodParameter instance without Exceptions
     * uses method toString
     * */
    @Test
    void MethodParameterTest_toString_noExceptions() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);
        StringBuilder s = new StringBuilder(cp.toString());
        s.append("methodAccess: ");
        s.append(mp.methodAccess);
        s.append("\nmethodName: ");
        s.append(mp.methodName);
        s.append("\nmethodDesc: ");
        s.append(mp.methodDesc);
        s.append("\nmethodSignature: ");
        s.append(mp.methodSignature);
        s.append("\nExceptions: ");
        s.append(mp.Exceptions == null ? "null" : Arrays.toString(mp.Exceptions));

        assertEquals(s.toString(), mp.toString());
    }

    /*
     * test that the method equals returns false if objects are equal
     * uses method equals
     */
    @Test
    void equalsTest_equal() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        assertEquals(mp, mp2);
    }

    /*
     * test that the method equals returns false if objects are not equal by name
     * uses method equals
     */
    @Test
    void equalsTest_notEqual_name() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, Opcodes.ACC_INTERFACE, "test_name2", "test_description", "test_signature", new String[0]);

        assertNotEquals(mp, mp2);
    }

    /*
     * test that the method equals returns false if objects are not equal by access
     * uses method equals
     */
    @Test
    void equalsTest_notEqual_access() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, 4, "test_name", "test_description", "test_signature", new String[0]);

        assertNotEquals(mp, mp2);
    }

    /*
     * test that the method equals returns false if objects are not equal by description
     * uses method equals
     */
    @Test
    void equalsTest_notEqual_description() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 3, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, 3, "test_name", "test_description2", "test_signature", new String[0]);

        assertNotEquals(mp, mp2);
    }

    /*
     * test that the method equals returns false if objects are not equal by description
     * uses method equals
     */
    @Test
    void equalsTest_notEqual_Exceptions() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 3, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, 3, "test_name", "test_description", "test_signature", new String[]{"test_exception"});

        assertNotEquals(mp, mp2);
    }

    /*
     * test that the method equals returns false if objects are not equal by cp
     * uses method equals
     */
    @Test
    void equalsTest_notEqual_cp() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 3, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, 3, "test_name", "test_description", "test_signature", new String[0]);

        assertNotEquals(mp, mp2);
    }

    /*
     * test that the method equals returns false if object is null
     * uses method equals
     */
    @Test
    void equalsTest_null() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        assertNotEquals(null, mp);
    }

    /*
     * test that the method equals returns false if object is not an instance of MethodParameter
     * uses method equals
     */
    @Test
    void equalsTest_notMethodParameter() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        assertNotEquals(" ", mp);
    }

    /*
     * test that method hashCode returns a valid hashcode
     * uses method hashCode
     */
    @Test
    void hashCodeTest_equal() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        assertEquals(mp.hashCode(), mp2.hashCode());
    }

    /*
     * test that the method hashCode returns different hashCode if objects are not equal by name
     * uses method hashCode
     */
    @Test
    void hashCodeTest_notEqual_name() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, Opcodes.ACC_INTERFACE, "test_name2", "test_description", "test_signature", new String[0]);

        assertNotEquals(mp.hashCode(), mp2.hashCode());
    }

    /*
     * test that the method hashCode returns different values if objects are not equal by access
     * uses method hashCode
     */
    @Test
    void hashCodeTest_notEqual_access() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, 4, "test_name", "test_description", "test_signature", new String[0]);

        assertNotEquals(mp.hashCode(), mp2.hashCode());
    }

    /*
     * test that the method hashCode returns different values if objects are not equal by description
     * uses method hashCode
     */
    @Test
    void hashCodeTest_notEqual_description() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 3, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, 3, "test_name", "test_description2", "test_signature", new String[0]);

        assertNotEquals(mp.hashCode(), mp2.hashCode());
    }

    /*
     * test that the method hashCode returns different values if objects are not equal by description
     * uses method hashCode
     */
    @Test
    void hashCodeTest_notEqual_Exceptions() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 3, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, 3, "test_name", "test_description", "test_signature", new String[]{"test_exception"});

        assertNotEquals(mp.hashCode(), mp2.hashCode());
    }

    /*
     * test that the method hashCode returns different values if objects are not equal by cp
     * uses method hashCode
     */
    @Test
    void hashCodeTest_notEqual_cp() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, 3, "test_name", "test_description", "test_signature", new String[0]);

        ClassParameter cp2 = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp2 = new MethodParameter(cp2, 3, "test_name", "test_description", "test_signature", new String[0]);

        assertNotEquals(mp.hashCode(), mp2.hashCode());
    }

    /*
     * test that the method equals returns false if object is not an instance of MethodParameter
     * uses method equals
     */
    @Test
    void hashCodeTest_notMethodParameter() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test interface 2"});
        MethodParameter mp = new MethodParameter(cp, Opcodes.ACC_INTERFACE, "test_name", "test_description", "test_signature", new String[0]);

        assertNotEquals(mp.hashCode(), cp.hashCode());
    }


   
}
