package xyz.acygn.millr;


import javassist.bytecode.Opcode;
import org.objectweb.asm.commons.Remapper;
import org.junit.After;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.objectweb.asm.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import xyz.acygn.millr.messages.NoSuchClassException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;


/**
 * Unit testing for {@link ClassParameter}
 *
 * @author David Hopes
 */
class ClassParameterTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;


    ClassParameterTest() {}


    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @BeforeEach
    void instantiateMillr() {
//        String workingDir = System.getProperty("user.dir");
        new File("dummyDirectory").mkdir();
        Mill m = Mill.createTestInstance("dummyDirectory");
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @AfterEach
    void removeDummyDirectory() {
        new File("dummyDirectory").delete();
    }

    // test that default constructor initialises className correctly to "test_name"
    // uses field className
    // tests line #58
    @Test
    void defaultConstructorTest_attribute_className() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test_interface 2"});
        assertEquals("test_name", cp.className);
    }

    // test that default constructor initialises classVersion correctly to 2
    // uses field classVersion
    // tests line #56
    @Test
    void defaultConstructorTest_attribute_classVersion() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test_interface 2"});
        assertEquals(2, cp.classVersion);
    }

    // test that default constructor initialises classAccess correctly to 3
    // uses field classAccess
    // tests line #57
    @Test
    void defaultConstructorTest_attribute_classAccess() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test_interface 2"});
        assertEquals(3, cp.classAccess);
    }

    // test that default constructor initialises classSignature correctly to "test_signature"
    // uses field classSignature
    // tests line #59
    @Test
    void defaultConstructorTest_attribute_classSignature() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test_interface 2"});
        assertEquals("test_signature", cp.classSignature);
    }

    // test that default constructor initialises classSuperName correctly to "test_super"
    // uses field classSuperName
    // tests line #60
    @Test
    void defaultConstructorTest_attribute_classSuperName() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test_interface 2"});
        assertEquals("test_super", cp.classSuperName);
    }

    // test that default constructor raises error if classSuperName is null and class not an interface or object
    // uses field classSuperName
    // tests line #52-53
    @Test
    void defaultConstructorTest_nullSuperName_SuperClass() {
        try {
            ClassParameter cp = new ClassParameter(2, 3, "not an object", "test_signature", null, new String[]{"test interface 1", "test_interface 2"});
            fail("Should have raised a NullPointerException as superName is null and it is neither an interface or object.");
        }
        catch (NullPointerException e) {
            assertEquals("Given a null name for the superClass of not an object but this one"
                    + "is neither /java/lang/Object nor an interface", e.getLocalizedMessage());
        }
    }

    // test that default constructor doesn't raise error if classSuperName is null and class is an interface
    // uses field classSuperName
    // tests line #52-54
    @Test
    void defaultConstructorTest_nullSuperName_Interface() {
        try {
            ClassParameter cp = new ClassParameter(2, Opcodes.ACC_INTERFACE, "not an object", "test_signature", null, new String[]{"test interface 1", "test_interface 2"});
            assertNull(cp.classSuperName);
            assertEquals(Opcodes.ACC_INTERFACE, cp.classAccess);
        }
        catch (NullPointerException e) {
            fail("Shouldn't have raised a NullPointerException as classSuperName is null and it is an interface.");
        }
    }

    // test that default constructor doesn't raise error if classSuperName is null and class is an object
    // uses field classSuperName
    // tests line #52-54
    @Test
    void defaultConstructorTest_nullSuperName_Object() {
        try {
            ClassParameter cp = new ClassParameter(2, Opcodes.ACC_INTERFACE, "java/lang/Object", "test_signature", null, new String[]{"test interface 1", "test_interface 2"});
            assertNull(cp.classSuperName);
            assertEquals("java/lang/Object", cp.className);
            assertEquals(Opcodes.ACC_INTERFACE, cp.classAccess);
        }
        catch (NullPointerException e) {
            fail("Shouldn't have raised a NullPointerException as classSuperName is null and it is an object.");
        }
    }

    // test that default constructor correctly initialises classInterfaces and when there are interfaces
    // uses field classInterfaces
    // tests line #61
    @Test
    void defaultConstructorTest_attribute_classInterfaces() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_super", new String[]{"test interface 1", "test_interface 2"});
        assertEquals("test interface 1", cp.classInterfaces[0]);
        assertEquals("test_interface 2", cp.classInterfaces[1]);
        assertArrayEquals(new String[]{"test interface 1", "test_interface 2"}, cp.classInterfaces);
    }

    // test that default constructor correctly initialises classInterfaces to a 0 length String[]  when there are no interfaces
    // uses field classInterfaces
    // tests line #61
    @Test
    void defaultConstructorTest_attribute_classInterfaces_null() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_super", null);
        assertArrayEquals(new String[0], cp.classInterfaces);
    }

    // test that default constructor correctly raises an error when className is null
    // uses field className
    // tests line #48-50
    @Test
    void defaultConstructorTest_attribute_className_null() {
        try {
            ClassParameter cp = new ClassParameter(2, 2, null, "test_signature", "test_super", new String[]{"test interface 1", "test_interface 2"});
            fail("Should have raised a NullPointerException as name is null.");
        }
        catch (NullPointerException e) {
            assertEquals("name is null in the classParameter creation", e.getLocalizedMessage());
        }
    }

    // test that default constructor correctly raises an error when className is null
    // uses field className
    // tests line #48-50
    @Test
    void defaultConstructorTest_attribute_className_primitive() {
        ClassParameter cp = new ClassParameter(2, 2, "int", "test_signature", null, null);
        assertEquals("int", cp.className);
    }

    // test that toString correctly returns string representation of ClassParameter object
    // tests line #68-75
    @Test
    void toStringTest() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});
        String s = "Class Version: 2\n" +
                "Class Access: 2\n" +
                "Class Name: test_name\n" +
                "Class Signature: test_signature\n" +
                "Class SuperName: test_supername\n" +
                "Class Interfaces: [test interface 1, test_interface 2]\n";

        assertEquals(s, cp.toString());
    }

    // test that myEquals correctly returns false for null and string
    // tests line #86
    @Test
    void myEqualsTest_null_String() {
        Boolean b = ClassParameter.myEquals(null, "string");

        assertFalse(b);
    }

    // test that myEquals correctly returns true for null and null
    // tests line #88
    @Test
    void myEqualsTest_null_null() {
        Boolean b = ClassParameter.myEquals(null, null);

        assertTrue(b);
    }

    // test that myEquals correctly returns true for equal strings
    // tests line #92
    @Test
    void myEqualsTest_String_String() {
        Boolean b = ClassParameter.myEquals("string", "string");

        assertTrue(b);
    }

    // test that myEquals correctly returns false for string and null
    // tests line #90
    @Test
    void myEqualsTest_String_null() {
        Boolean b = ClassParameter.myEquals("string", null);

        assertFalse(b);
    }

    // test that myEquals correctly returns false for unequal strings
    // tests line #90
    @Test
    void myEqualsTest_String_unequalStrings() {
        Boolean b = ClassParameter.myEquals("string", "string2");

        assertFalse(b);
    }

    // test that equals correctly returns true for equal ClassParameter objects
    // tests line #110-111
    @Test
    void equalsTest_same() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});

        assertEquals(cp, cp2);
    }

    // test that equals correctly returns false for unequal objects on version
    // tests line #110-112
    @Test
    void equalsTest_diff_version() {
        ClassParameter cp = new ClassParameter(1, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});

        assertNotEquals(cp, cp2);
    }

    // test that equals correctly returns false for unequal objects on access
    // tests line #110-112
    @Test
    void equalsTest_diff_access() {
        ClassParameter cp = new ClassParameter(2, 1, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});

        assertNotEquals(cp, cp2);
    }

    // test that equals correctly returns false for unequal objects on name
    // tests line #110-112
    @Test
    void equalsTest_diff_name() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name2", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});

        assertNotEquals(cp, cp2);
    }

    // test that equals correctly returns false for unequal objects on signature
    // tests line #110-112
    @Test
    void equalsTest_diff_signature() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature2", "test_supername", new String[]{"test interface 1", "test_interface 2"});
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});

        assertNotEquals(cp, cp2);
    }

    // test that equals correctly returns false for unequal objects on superName
    // tests line #110-112
    @Test
    void equalsTest_diff_supername() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername2", new String[]{"test interface 1", "test_interface 2"});
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});

        assertNotEquals(cp, cp2);
    }

    // test that equals correctly returns false for unequal objects on interfaces
    // tests line #110-112
    @Test
    void equalsTest_diff_interfaces() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 3"});

        assertNotEquals(cp, cp2);
    }

    // test that equals correctly returns true for objects with no interfaces
    // tests line #110-111
    @Test
    void equalsTest_no_interfaces() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", null);
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", null);

        assertEquals(cp, cp2);
    }

    // test that equals correctly returns false for unequal objects on version
    // tests line #105
    @Test
    void equalsTest_diff_types() {
        String cp = "blah";
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", null);

        assertNotEquals(cp2, cp);
    }

    // tests that equals correctly returns false for null object parameter
    //tests line 103
    @Test
    void equalsTest_null_object() {
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", null);
        assertNotEquals(null, cp2);
    }

    // tests that equals correctly returns false for object that is not instance of ClassParameter
    // tests line 105
    @Test
    void equalsTest_nonClassParameter_object() {
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", null);
        assertNotEquals("blah", cp2);
    }

    // test that myHashcode correctly returns 1 for null objects
    // tests line #123
    @Test
    void myHashcodeTest_null() {
        assertEquals(1, ClassParameter.myHashcode(null));
    }

    // test that myHashcode correctly returns hashcode for ClassParameter object
    // tests line #125
    @Test
    void myHashcodeTest_ClassParameterObject() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", null);

        assertEquals(cp.hashCode(), ClassParameter.myHashcode(cp));
    }

    // test that hashcode correctly returns same value for two different objects with equal attributes
    // tests line #134
    @Test
    void hashCodeTest_equalObjects() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", null);
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", null);

        assertEquals(cp.hashCode(), cp2.hashCode());
    }

    // test that hashcode correctly returns different value for two different objects with different attributes
    // tests line #134
    @Test
    void hashCodeTest_unequalObjects_name() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name1", "test_signature", "test_supername", null);
        ClassParameter cp2 = new ClassParameter(2, 2, "test_name", "test_signature", "test_supername", null);

        assertNotEquals(cp.hashCode(), cp2.hashCode());
    }

    // test that hashcode correctly returns different value for two different objects with different type
    // tests line #134
    @Test
    void hashCodeTest_unequalObjects_diffType() {
        ClassParameter cp = new ClassParameter(2, 2, "test_name1", "test_signature", "test_supername", null);
        Object cp2 = (Object) cp;
        assertEquals(cp2.hashCode(), cp.hashCode());
    }

    // tests that visit correctly assigns cp with ClassParameter of specific fields
    @Test
    void ClassVisitorGetInfo_visit_test() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name1", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});
        ClassParameter.ClassVisitorGetInfo cvgi = new ClassParameter.ClassVisitorGetInfo(Opcodes.ASM5);
        cvgi.visit(2, 3, "test_name1", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});

        assertEquals(cvgi.cp, cp);
    }


//    TODO Check whether or not requirement to test arrayify

    // tests to ensure that getClassParameter raises exception if owner parameter is null
    // tests getClassParameter(String owner)
    // tests lines 200-201
    @Test
    void getClassParameterTest_null() {
        try {
            ClassParameter.getClassParameter((String) null);
            fail("Should have raised NullPointerException as String parameter 'owner' is null");
        }
        catch (NullPointerException e) {
            assertEquals("the input parameter is null", e.getLocalizedMessage());
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
    }

    // tests to ensure that getClassParameter returns ClassParameter object with correct version number for primitive owner parameter
    @Test
    void getClassParameterTest_primitive_version() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter("int");
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
        assertEquals(cp.classVersion, 52);
    }

    // tests to ensure that getClassParameter returns ClassParameter object with correct access for primitive owner parameter
    @Test
    void getClassParameterTest_primitive_access() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter("int");
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
        assertEquals(cp.classAccess, Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC);
    }

    // tests to ensure that getClassParameter returns ClassParameter object with correct className for primitive owner parameter
    @Test
    void getClassParameterTest_primitive_name() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter("int");
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
        assertEquals(cp.className, "int");
    }

    // tests to ensure that getClassParameter returns ClassParameter object with correct signature for primitive owner parameter
    @Test
    void getClassParameterTest_primitive_signature() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter("int");
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
        assertEquals(cp.classSignature, "I");
    }

    // tests to ensure that getClassParameter returns ClassParameter object with null superName for primitive owner parameter
    @Test
    void getClassParameterTest_primitive_superName() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter("int");
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
        assertNull(cp.classSuperName);
    }

    // tests to ensure that getClassParameter returns ClassParameter object with empty interfaces for primitive owner parameter
    @Test
    void getClassParameterTest_primitive_interfaces() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter("int");
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
        assertTrue(Arrays.deepEquals(cp.classInterfaces, new String[0]));
    }

    // tests to ensure that getClassParameter returns ClassParameter object if owner == void
    @Test
    void getClassParameterTest_void() {
        try {
            ClassParameter cp = ClassParameter.getClassParameter("void");
            fail("Should have raised a Runtime Exception if void passed as parameter");
        }
        catch (RuntimeException e) {
            assertTrue(true);
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
    }

    // tests to ensure that getClassParameter returns ClassParameter object with correct access for non-primitive
    @Test
    void getClassParameterTest_nonPrimitive_String_access() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter("java/lang/String");
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
        assertEquals(cp.classAccess, 49);
    }

    // tests to ensure that getClassParameter returns ClassParameter object with correct name for non-primitive
    @Test
    void getClassParameterTest_nonPrimitive_String_name() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter("java/lang/String");
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
        assertEquals("java/lang/String", cp.className);
    }

    // tests to ensure that getClassParameter returns ClassParameter object with correct superName for non-primitive
    @Test
    void getClassParameterTest_nonPrimitive_String_superName() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter("java/lang/String");
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
        assertEquals("java/lang/Object", cp.classSuperName);
    }

    // tests to ensure that getClassParameter returns ClassParameter object with correct interfaces for non-primitive
    @Test
    void getClassParameterTest_nonPrimitive_String_interfaces() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter("java/lang/String");
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }

        String[] interfaces = new String[]{"java/io/Serializable", "java/lang/Comparable", "java/lang/CharSequence"};
        assertTrue(Arrays.deepEquals(interfaces, cp.classInterfaces));
    }

    // tests to ensure that getClassParameter returns ClassParameter object with correct interfaces for non-primitive
    @Test
    void getClassParameterTest_nonPrimitive_other_interfaces() {
        ClassParameter cp = null;
        try {
            cp = ClassParameter.getClassParameter(SubSub.class.getName());
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
        StringBuilder sb = new StringBuilder("Interfaces: ");
        for (String interfaceName : cp.classInterfaces) {
            sb.append("\n\t");
            sb.append(interfaceName);
        }
        sb.append("\n");
        sb.append(cp.classSuperName);
        sb.append("\n");
        sb.append(cp.className);

        originalOut.println(sb);

        String name = "xyz/acygn/millr/ClassParameterTest$SubSub";
        String[] interfaces = new String[]{"xyz/acygn/millr/ClassParameterTest$Inter", "xyz/acygn/millr/ClassParameterTest$Inter2"};
//        ClassParameter expected_cp = new ClassParameter(null, null, "")
        try {
            ClassReader cr = new ClassReader("java/lang/String");
        } catch (IOException e) {
            originalOut.println(e.getLocalizedMessage());
        }


    }

    // tests to ensure that getClassParameter throws an IOException if it fails to read the class
    @Test
    void getClassParameter_nonPrimitive_doesntExist() {
        try {
            ClassParameter cp = ClassParameter.getClassParameter("java/lang/String");
        }
        catch (RuntimeException e) {
            assertEquals("failed to read java/languid/String + Ljava/languid/String;", errContent);
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
    }

    // tests to ensure that getClassParameter can deal with arrays
    @Test
    void getClassParameter_nonPrimitive_array() {
        try {
            String[][] s = new String[10][10];
            originalOut.println(s.getClass().getName());
            ClassParameter cp = ClassParameter.getClassParameter("[[Ljava/lang/String;");

        } catch (ClassNotFoundException e) {
            fail(e.getLocalizedMessage());
        }
    }


    // tests for null return on getClassParameter(ClassReader cr)
    @Test
    void getClassParameterTest_ClassReader_null() {
        ClassParameter cp = ClassParameter.getClassParameter((ClassReader) null);
        assertNull(cp);
    }

    //test for NullPointerException thrown by getClassParameter(ClassReader cr) if className == null\
    // TODO Complete getClassParameter(ClassReader) test for class with null className
    @Test
    void getClassParameterTest_ClassReader_className_null() {
        try {
            org.objectweb.asm.Type t = TypeUtil.getObjectType("java/lang/String");
            ClassReader cr = Mill.getInstance().getClassReader(null);
            ClassParameter.ClassVisitorGetInfo cv = new ClassParameter.ClassVisitorGetInfo(Opcodes.ASM5);
            cr.accept(cv, 0);
            cr.getClassName();
            ClassParameter cp = ClassParameter.getClassParameter(cr);
            cp.printClass();

        }
        catch (NoSuchClassException e) {
            fail(e);
        }
    }

    //test for NullPointerException thrown by getClassParameter(ClassReader cr) if className == null\
    @Test
    void getClassParameterTest_ClassReader_classreader_null() {
        try {
            ClassParameter cp = ClassParameter.getClassParameter((ClassReader) null);
            cp.printClass();
        } catch (NullPointerException e) {
            assertEquals(null, e.getLocalizedMessage());
        }
    }
//    TODO Complete test for getClassParameter(String owner) - array
//    TODO Create test for getClassParameter(ClassReader cr) - null name

    @Test
    void printClassTest() {
        ClassParameter cp = new ClassParameter(2, 3, "test_name1", "test_signature", "test_supername", new String[]{"test interface 1", "test_interface 2"});
        String s = "Name of class: test_name1\n" +
                "Signature of class: test_signature\n" +
                "superClass of class: test_supername\n" +
                "Implements interfaces:\n" +
                "\ttest interface 1\n\ttest_interface 2" +
                "\nVersion of class: 2" +
                "\nAccess of class: 3\n";
        cp.printClass();
        assertEquals(s, outContent.toString());
    }


    @Test
    void getCoreClassParameter_ClassParameter_null() {
        try {
            ClassParameter.getCoreClassParameter((ClassParameter) null);
            fail("The method should have raised a NullPointerException if cp is null");
        }
        catch (NullPointerException e) {
            assertEquals("The input parameter internalName is null", e.getLocalizedMessage());
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
    }

//    TODO Create further tests for getCoreClassParameter(ClassParameter cp)

    @Test
    void getCoreClassParameter_string_null() {
        try {
            ClassParameter.getCoreClassParameter((String) null);
            fail("The method should have raised a NullPointerException if internalName is null");
        }
        catch (NullPointerException e) {
            assertEquals("The input parameter internalName is null", e.getLocalizedMessage());
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
    }

    @Test
    void getCoreClassParameterTest_cp_name_startsarray() {
        try {
            ClassParameter cp = ClassParameter.getClassParameter("java/lang/String");
            ClassParameter cp2 = ClassParameter.getClassParameter("java/lang/String");
            cp2.className = "[java/lang/String";
            ClassParameter.getCoreClassParameter(cp2);
            assertEquals(cp, ClassParameter.getCoreClassParameter(cp2));
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
    }

    // test getCoreClassParameter(String internalName) to return ClassParameter
    @Test
    void getCoreClassParameterTest_String() {
        try {
            ClassParameter cp = ClassParameter.getCoreClassParameter("java/lang/String");
            assertEquals(cp, ClassParameter.getClassParameter("java/lang/String"));
        }
        catch (ClassNotFoundException e) {
            fail(e);
        }
    }


    class Original {
        public final int level = 1;

        Original() {

        }
    }

    class Sub extends Original {
        public final int level = 2;

        Sub() {
            super();
        }
    }

    class SubSub extends Sub implements Inter, Inter2 {
        public final int level = 3;

        SubSub() {
            super();
        }
    }

    class SubSubSub extends SubSub {
        public final int level = 4;

        public SubSubSub() {
            super();
        }
    }

    interface Inter {
    }

    interface Inter2 extends Inter {
    }
}

