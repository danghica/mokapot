//
//package xyz.acygn.millr;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;
//import static xyz.acygn.millr.VisibilityTools.*;
//import org.objectweb.asm.*;
//import xyz.acygn.millr.messages.NoSuchClassException;
//
//import javax.management.relation.RoleUnresolved;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.PrintStream;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.*;
//
//
///**
// * Tests for the VisibilityTools class, which implements functionality allowing
// * the analysis of a class' environment.
// */
//class VisibilityToolsTest {
//    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
//    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
//    private final PrintStream originalOut = System.out;
//    private final PrintStream originalErr = System.err;
//
//    VisibilityToolsTest() {
//    }
//
//    @BeforeEach
//    void setupStreams() {
//        System.setOut((new PrintStream(outContent)));
//        System.setErr((new PrintStream(errContent)));
//    }
//
//    @AfterEach
//    void restoreStreams() {
//        System.setOut(originalOut);
//        System.setErr(originalErr);
//    }
////    *************************
////    TESTS FOR getSuperClasses
////    *************************
//
//    /*
//     * Tests that getSuperClasses(String) returns the same list as for getSuperClasses(cr)
//     */
//    @Test
//    void getSuperClassesTest_cr_equalTo_String() {
//        try {
//            ClassReader cr = Mill.getInstance().getClassReader(Class4.class.getName());
//            List<String> superclasses_from_cr = getSuperClasses(cr);
//            for (String s : superclasses_from_cr) {
//                System.out.println(s);
//            }
//
//            List<String> superclasses_from_s = getSuperClasses(Class4.class.getName());
//            for (String s : superclasses_from_s) {
//                System.out.println(s);
//            }
//            assertEquals(superclasses_from_cr, superclasses_from_s);
//        } catch (NoSuchClassException e) {
//            fail("Should have provided list of superclasses, instead " + e.getLocalizedMessage());
//        }
//    }
//
//    /*
//     * Tests that getSuperClasses returns an empty arraylist if provided a class with no superclasses
//     */
//    @Test
//    void getSuperClassesTest_Object() {
//        try {
//            List<String> superclasses = getSuperClasses("java/lang/Object");
//            assertEquals(new ArrayList<>(), superclasses);
//        } catch (NoSuchClassException e) {
//            fail("Should have returned an empty array list, instead " + e.getLocalizedMessage());
//        }
//    }
//
//    /*
//     * tests that getSuperClasses raises an exception if provided a primitive
//     */
//    @Test
//    void getSuperClassesTest_primitive() {
//        try {
//            List<String> superclasses = getSuperClasses("int");
//            fail("Should have raised a runtime exception");
//        } catch (NoSuchClassException e) {
//            assertEquals("java.io.IOException: Class not found", e.getLocalizedMessage());
//        }
//    }
//
//    /*
//     * tests that getSuperClasses raises an exception if provided an invalid String
//     */
//    @Test
//    void getSuperClassesTest_invalid() {
//        try {
//            List<String> superclasses = getSuperClasses("blahhhhh");
//            fail("Should have raised a runtime exception");
//        } catch (NoSuchClassException e) {
//            assertEquals("java.io.IOException: Class not found", e.getLocalizedMessage());
//        }
//    }
//
//    /**
//     * Tests that VisibilityTools.getSuperClasses correctly returns no superclasses
//     * for Object.
//     */
//    @Test
//    void shouldReturnNoSuperclassesForObject() {
//        try {
//            Method getSuperClasses = VisibilityTools.class.getDeclaredMethod("getSuperClasses", String.class);
//            getSuperClasses.setAccessible(true);
//            assertEquals(0, ((List<String>) getSuperClasses.invoke(new VisibilityTools(), "java/lang/Object")).size());
//        } catch (NoSuchMethodException e) {
//            System.err.println("The method getSuperClasses() has been removed or renamed.");
//            e.printStackTrace();
//        } catch (IllegalAccessException | InvocationTargetException e) {
//            System.out.println(e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
////    ***********************
////    TESTS FOR getInterfacesInternal
////    ***********************
//
//    /*
//     * Tests that getInterfacesInternal(String) returns the same list as for getInterfacesInternal(cr)
//     */
////    @Test
////    void getInterfacesTest_cr_equalTo_String() {
////        try {
////            ClassReader cr = Mill.getInstance().getClassReader(Class4.class.getName());
////            Set<String> interfaces_from_cr = getInterfacesInternal(cr);
////            for (String s : interfaces_from_cr) {
////                System.out.println(s);
////            }
////
////            Set<String> interfaces_from_s = getInterfacesInternal(Class4.class.getName());
////            for (String s : interfaces_from_s) {
////                System.out.println(s);
////            }
////
////            assertEquals(interfaces_from_cr, interfaces_from_s);
////        } catch (IOException e) {
////            fail("Should have provided list of superclasses, instead " + e.getLocalizedMessage());
////        }
////    }
//
//    /*
//     * Tests that getInterfacesInternal returns an empty arraylist if provided a class with no superclasses
//     */
////    @Test
////    void getInterfacesTest_Object() {
////        try {
////            Set<String> interfaces = getInterfacesInternal("java/lang/Object");
////            assertEquals(new HashSet<>(), interfaces);
////        } catch (RuntimeException e) {
////            fail("Should have returned an empty array list, instead " + e.getLocalizedMessage());
////        }
////    }
//
//    /*
//     * tests that getInterfacesInternal raises an exception if provided a primitive
//     */
////    @Test
////    void getInterfacesTest_primitive() {
////        try {
////            Set<String> interfaces = getInterfacesInternal("int");
////            fail("Should have raised a runtime exception");
////        } catch (RuntimeException e) {
////            assertEquals("java.io.IOException: Class not found", e.getLocalizedMessage());
////        }
////    }
////
////    /*
////     * tests that getInterfacesInternal raises an exception if provided an invalid String
////     */
////    @Test
////    void getInterfacesTest_invalid() {
////        try {
////            Set<String> interfaces = getInterfacesInternal("blahhhhh");
////            fail("Should have raised a runtime exception");
////        } catch (RuntimeException e) {
////            assertEquals("java.io.IOException: Class not found", e.getLocalizedMessage());
////        }
////    }
//
////    ********************
////    TESTS FOR getPackage
////    ********************
//
//    /*
//     * Tests to ensure getPackage returns the correct package name
//     */
//    @Test
//    void getPackageTest() {
//        assertEquals("xyz/acygn/millr", getPackage(Class4.class.getName().replace('.', '/')));
//    }
//
////    **********************************
////    TESTS FOR doesDescriptionOverrides
////    **********************************
//
//    /*
//     * Tests that doesDescriptionOverrides returns true if method overridable
//     */
//    @Test
//    void doesDescriptionOverridesTest_true() {
//        try {
//            assertTrue(doesDescriptionOverrides("([Ljava/lang/Object;)Ljava/lang/Object;",
//                    "([Ljava/lang/Object;)Ljava/lang/Object;"));
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests that doesDescriptionOverridesTest returns false if method not overridable
//     */
//    @Test
//    void doesDescriptionOverridesTest_false() {
//        try {
//            assertFalse(doesDescriptionOverrides("([Ljava/lang/Object;)Ljava/lang/Object;",
//                    "([Ljava/lang/String;)Ljava/lang/Object;"));
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//    }
//
////    **********************
////    TESTS FOR doesOverride
////    **********************
//
//    /*
//     * Tests that doesOverride the correct superclasses are returned
//     */
//    @Test
//    void doesOverrideTest() {
//        try {
//            for (String s : doesOverride(Class4.class.getName(), "method_diffReturn_moreSpecific", "()L" + Class2.class.getName() + ";")) {
//                System.out.println(s);
//            }
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests that doesOverride correctly returns an empty ArrayList if no superclasses.
//     */
//    @Test
//    void doesOverrideTest_noOverrides() {
//        try {
//            assertEquals(new ArrayList<>(), doesOverride(Class2.class.getName(), "main", "()L" + Class2.class.getName() + ";"));
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests that doesOverride correctly returns an ArrayList with single object java/lang/Object for Object arrays
//     */
//    @Test
//    void doesOverrideTest_array_object() {
//        try {
//            assertEquals(Arrays.asList("java/lang/Object"), doesOverride(Class4[][].class.getName(), null, null));
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests that doesOverride correctly returns an ArrayList with single object java/lang/Object for Primitive arrays
//     */
//    @Test
//    void doesOverrideTest_array_primitive() {
//        try {
//            assertEquals(Arrays.asList("java/lang/Object"), doesOverride(int[].class.getName(), null, null));
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests that doesOverride correctly returns an empty ArrayList for Primitives
//     */
//    @Test
//    void doesOverrideTest_primitive() {
//        try {
//            assertEquals(new ArrayList<>(), doesOverride(int.class.getName(), null, null));
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests that doesOverride correctly returns an empty ArrayList for Primitives
//     */
//    @Test
//    void doesOverrideTest_error() {
//        try {
//            doesOverride("blah", null, null);
//            fail("Should have raised exception");
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//        catch (RuntimeException e) {
//            assertEquals("Class not found: Blah", e.getLocalizedMessage());
//        }
//    }
//
//
////    ********************************
////    TESTS FOR doesOverrideNonProject
////    ********************************
//
//    @Test
//    void doesOverrideNonProjectTest() {
////        TODO Implement test for doesOverrideNonProject
//    }
//
////    ******************************************
////    TESTS FOR doesMethodComeFromOutsideProject
////    ******************************************
//
//    @Test
//    void doesMethodComeFromOutsideProjectTest() {
////        TODO Implement test for doesMethodComeFromOutsideProject
//    }
//
////    ******************************************
////    TESTS FOR doesImplement
////    ******************************************
//
//    @Test
//    void doesImplementTest() {
////        TODO Implement test for doesImplement
//        try {
//            List<String> list = doesImplement(Class4.class.getName(), "Interface4OverriddenMethod", "()V");
//            ClassReader cr = new ClassReader(Class4.class.getName());
//            ClassPrinter cv = new ClassPrinter();
//            cr.accept(cv, 0);
//            HashMap<String, TestMethodParameter> methods = cv.getMethods();
//            methods.forEach((k, v) -> System.out.println(k + ":" + v.getDesc()));
//            List<String> expected_list = Arrays.asList("xyz/acygn/millr/Interface4");
//            assertEquals(expected_list, list);
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//        catch (IOException e) {
//            fail("Unexpected error: " + e.getLocalizedMessage());
//        }
//    }
//
//    @Test
//    void doesImplementTest_Primitive() {
////        TODO Implement test for doesImplement
//        try {
//            List<String> list = doesImplement(int.class.getName(), null, null);
//            List<String> expected_list = new ArrayList<>();
//            assertEquals(expected_list, list);
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
//    @Test
//    void doesImplementTest_Array() {
////        TODO Implement test for doesImplement
//        int[] i = new int[10];
//        try {
//            List<String> list = doesImplement(i.getClass().getName(), "clone", "()I");
//            List<String> expected_list = Arrays.asList();
//            assertEquals(expected_list, list);
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//
//    }
//
//    @Test
//    void doesImplementTest_Array2() {
//        try {
//            List<String> list = doesImplement("jsand", "clone", "()I");
//            List<String> expected_list = Arrays.asList();
//            fail("Should have raised an exception");
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//        catch (RuntimeException e) {
//            assertEquals(errContent.toString(), "IOException when asm tried to access class: jsand\n");
//            assertEquals("java.io.IOException: Class not found", e.getLocalizedMessage());
//        }
//    }
//
////    **************************
////    TESTS FOR isValueFromEnum
////    **************************
//
//    @Test
//    void isValueFromEnumTest_enums() {
//        try {
//            assertTrue(isValueFromEnum(Level.class.getName(), "values", "()[Lxyz.acygn.millr.Level;"));
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
//    @Test
//    void isValueFromEnumTest_error() {
//        try {
//            boolean b = isValueFromEnum("bdkhsb", "values", "()[Lbdkhsb;");
//            assertFalse(b);
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
//    @Test
//    void isValueFromEnumTest_notEnums() {
//        try {
//            boolean b = isValueFromEnum("java/lang/String", "values", "()[Ljava/lang/String;");
//            assertFalse(b);
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
////    ********************************
////    TESTS FOR isEnum
////    ********************************
//
//    /*
//     * Tests if isEnum returns false if Object provided is not Enum
//     */
//
//    @Test
//    void isEnumTest_false() {
//        try {
//            assertFalse(isEnum("java/lang/String"));
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests if isEnum returns false if Object provided is not Enum
//     */
//    @Test
//    void isEnumTest_true() {
//        try {
//            assertTrue(isEnum(Level.class.getName()));
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests if isEnum carries exception if Object provided is not valid
//     */
//    @Test
//    void isEnumTest_error() {
//        try {
//            isEnum("java/lang/fndjslbv");
//        } catch (NoSuchClassException e) {
//            assertEquals("java.io.IOException: Class not found", e.getLocalizedMessage());
//        }
//    }
//
//
////    ********************************
////    TESTS FOR classFieldComesFromTest
////    ********************************
//
//    /*
//     * Tests if classFieldComesFromTest raises exception if invalid class provided
//     */
//    @Test
//    void classFieldComesFromTest_invalidClass() {
//        try {
//            String s = classFieldComesFrom("java/lang/bkdshbhk", "kbsabd", "dsnbhak");
//            fail("Should have raised a runtime exception");
//        } catch (ClassNotFoundException e) {
//            assertEquals("java.io.IOException: Class not found", e.getLocalizedMessage());
//        }
//    }
//
//    /*
//     * Tests if classFieldComesFromTest returns correct class owner on singular class
//     */
//    @Test
//    void classFieldComesFromTest_sameClass() {
//        try {
//            String s = classFieldComesFrom(Class1.class.getName(), "number1", "I");
//            assertEquals("xyz.acygn.millr.Class1", s);
//        } catch (ClassNotFoundException e) {
//            fail("Raised exception despite correct input.");
//        }
//    }
//
//    /*
//     * Tests if classFieldComesFromTest returns correct class owner on class inheriting from another class
//     */
//    @Test
//    void classFieldComesFromTest_superClass() {
//        try {
//            String s = classFieldComesFrom(Class2.class.getName(), "number1", "I");
//            assertEquals("xyz/acygn/millr/Class1", s);
//        } catch (ClassNotFoundException e) {
//            fail("Raised exception despite correct input.");
//        }
//    }
//
//    /*
//     * Tests if classFieldComesFromTest returns correct class owner on class inheriting from another class
//     */
//    @Test
//    void classFieldComesFromTest_supersuperclass() {
//        try {
//            String s = classFieldComesFrom(Class3.class.getName(), "number2", "I");
//            assertEquals("xyz/acygn/millr/Class2", s);
//        } catch (ClassNotFoundException e) {
//            fail("Raised exception despite correct input.");
//        }
//    }
//
//    /*
//     * Tests if classFieldComesFromTest returns correct class owner on class inheriting constant
//     * (public static final) from interface
//     */
//    @Test
//    void classFieldComesFromTest_interfaceConstantField() {
//        try {
//            String s = classFieldComesFrom(Class4.class.getName(), "NUMBER", "I");
//            assertEquals("xyz.acygn.millr.Interface4", s);
//        } catch (ClassNotFoundException e) {
//            fail("Raised exception despite correct input.");
//        }
//    }
//
//    /*
//     * Tests if classFieldComesFromTest returns correct class owner on class inheriting static field
//     * (public static) from interface
//     */
//    @Test
//    void classFieldComesFromTest_interfaceStaticField() {
//        try {
//            String s = classFieldComesFrom(Class4.class.getName(), "NUMBER1", "I");
//            assertEquals("xyz.acygn.millr.Interface4", s);
//        } catch (ClassNotFoundException e) {
//            fail("Raised exception despite correct input.");
//        }
//    }
//
//    /*
//     * Tests if classFieldComesFromTest returns correct class owner on class inheriting static field
//     * (public static) from interface
//     */
//    @Test
//    void classFieldComesFromTest_invalidField() {
//        try {
//            String s = classFieldComesFrom(Class4.class.getName(), "unknown", "I");
//            fail("Should have raised Runtime Exception");
//        } catch (ClassNotFoundException e) {
//            assertEquals("This case shall not be reached: Impossible to find field, unknown, whose desc is I, exploring classes from xyz.acygn.millr.Class4", e.getLocalizedMessage());
//        }
//    }
//
//    /*
//     * Tests if classFieldComesFromTest returns correct class owner on class inheriting field
//     * from abstract class
//     */
//    @Test
//    void classFieldComesFromTest_abstractClass_field() {
//        try {
//            String s = classFieldComesFrom(Class6.class.getName(), "field", "I");
//            assertEquals("xyz/acygn/millr/Class5", s);
//        } catch (ClassNotFoundException e) {
//            fail("Should have raised Runtime Exception");
//        }
//    }
//
//    /*
//     * Tests if classFieldComesFromTest returns correct class owner on class inheriting static field
//     * from abstract class
//     */
//    @Test
//    void classFieldComesFromTest_abstractClass_staticField() {
//        try {
//            String s = classFieldComesFrom(Class6.class.getName(), "staticField", "I");
//            assertEquals("xyz/acygn/millr/Class5", s);
//        } catch (ClassNotFoundException e) {
//            fail("Should have raised Runtime Exception");
//        }
//    }
//
//    /*
//     * Tests if classFieldComesFromTest returns correct class owner on class inheriting static field
//     * from abstract class
//     */
//    @Test
//    void classFieldComesFromTest_abstractClass_constantField() {
//        try {
//            String s = classFieldComesFrom(Class6.class.getName(), "constantField", "I");
//            assertEquals("xyz/acygn/millr/Class5", s);
//        } catch (ClassNotFoundException e) {
//            fail("Should have raised Runtime Exception");
//        }
//    }
//
////    ********************************
////    TESTS FOR isValueField
////    ********************************
//
//    /*
//     * Tests if isValueField returns true if field provided is a value field
//     */
//    @Test
//    void isValueFieldTest() {
//        try {
//            assertTrue(isValueField("xyz.acygn.millr.Level", "$VALUES", "[Lxyz.acygn.millr.Level;"));
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests if isValueField returns true if field provided is a value field
//     */
//    @Test
//    void isValueFieldTest_invalidField() {
//        try {
//            assertTrue(isValueField("xyz.acygn.millr.Level", "value", "[Lxyz.acygn.millr.Level;"));
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//    }
//
//
////    ********************************
////    TESTS FOR isMethodSpecial
////    ********************************
//
//    /*
//     * Tests if isMethodSpecial returns true if field provided is a special method
//     */
//    @Test
//    void isMethodSpecialTest_specialMethod_init() {
//        try {
//            assertTrue(isMethodSpecial("xyz/acygn/millr/Level", "<init>", "()V"));
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
//
//    /*
//     * Tests if isMethodSpecial returns true if field provided is a special method
//     */
//    @Test
//    void isMethodSpecialTest_specialMethod_clinit() {
//        try {
//            assertTrue(isMethodSpecial("xyz/acygn/millr/Level", "<clinit>", "()V"));
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests if isMethodSpecial returns true if field provided is a special method
//     */
//    @Test
//    void isMethodSpecialTest_specialMethod_value() {
//        try {
//            assertTrue(isMethodSpecial("xyz/acygn/millr/Level", "values", "()[Lxyz/acygn/millr/Level;"));
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//    }
//
////    ********************************
////    TESTS FOR isSignaturePolymorphic
////    ********************************
//
//    /*
//     * Tests if isSignaturePolymorphic returns false if method declared in the java.lang.invoke.MethodHandle class
//     */
//    // FIXME: 07/08/2018 getSimpleMethodParameter no longer exists, refactor test?
//
////    @Test
////    void isSignaturePolymorphicTest_methodHandle() {
////        MethodParameter mp = MethodParameter.getSimpleMethodParameter("int", "test_name", "test_description");
////        mp.className = "java/lang/invoke/MethodHandle";
////
////        assertFalse(VisibilityTools.isSignaturePolymorphic(mp));
////    }
//
//    /*
//     * Tests if is SignaturePolymorphic returns false if method declared in the java.lang.invoke.MethodHandle class
//     */
//    // FIXME: 07/08/2018 getSimpleMethodParameter no longer exists, refactor test?
//
////    @Test
////    void isSignaturePolymorphicTest_signature() {
////        MethodParameter mp = MethodParameter.getSimpleMethodParameter("int", "test_name", "test_description");
////        mp.className = "java/lang/invoke/MethodHandle";
////        mp.methodDesc = "([Ljava/lang/Object;)Ljava/lang/Object;";
////
////        assertFalse(VisibilityTools.isSignaturePolymorphic(mp));
////    }
//
//
//    /*
//     * Tests if is SignaturePolymorphic returns false if method declared in the java.lang.invoke.MethodHandle class
//     */
//
////    @Test
////    void isSignaturePolymorphicTest_flags_VARARGS() {
////        MethodParameter mp = MethodParameter.getSimpleMethodParameter("int", "test_name", "test_description");
////        mp.className = "java/lang/invoke/MethodHandle";
////        mp.methodDesc = "([Ljava/lang/Object;)Ljava/lang/Object;";
////        mp.methodAccess = 128;
////
////        assertFalse(VisibilityTools.isSignaturePolymorphic(mp));
////    }
//
//
//    /*
//     * Tests if is SignaturePolymorphic returns false if method declared in the java.lang.invoke.MethodHandle class
//     */
//    // FIXME: 07/08/2018 getSimpleMethodParameter no longer exists, refactor test?
//
////    @Test
////    void isSignaturePolymorphicTest_flags_NATIVE() {
////        MethodParameter mp = MethodParameter.getSimpleMethodParameter("int", "test_name", "test_description");
////        mp.className = "java/lang/invoke/MethodHandle";
////        mp.methodDesc = "([Ljava/lang/Object;)Ljava/lang/Object;";
////        mp.methodAccess = 256;
////
////        assertFalse(VisibilityTools.isSignaturePolymorphic(mp));
////    }
//
//
//    /*
//     * Tests if is SignaturePolymorphic returns false if method declared in the java.lang.invoke.MethodHandle class
//     */
//    // FIXME: 07/08/2018 getSimpleMethodParameter no longer exists, refactor test?
//
////    @Test
////    void isSignaturePolymorphicTest_flags_BOTH() {
////        MethodParameter mp = MethodParameter.getSimpleMethodParameter("int", "test_name", "test_description");
////        mp.className = "java/lang/invoke/MethodHandle";
////        mp.methodDesc = "([Ljava/lang/Object;)Ljava/lang/Object;";
////        mp.methodAccess = 128 + 256;
////
////        assertTrue(VisibilityTools.isSignaturePolymorphic(mp));
////    }
//
//
//    /*
//     * Tests if the two methods invoke and invokeExact are recognised correctly
//     */
//    @Test
//    void isSignaturePolymorphicTest_methods_invoke() {
////        new class reader, with name of class
//        try {
//            ClassReader cr = new ClassReader("java/lang/invoke/MethodHandle");
//            ClassVisitor cpr = new ClassPrinter();
//            cr.accept(cpr, 0);
//
//            for (String key : ((ClassPrinter) cpr).getMethods().keySet()) {
//                MethodParameter method = ((ClassPrinter) cpr).getMethods().get(key).createMethodParameter();
//                if (VisibilityTools.isSignaturePolymorphic(method)) {
//                    System.out.println(method.methodName);
//                }
//            }
//
//        } catch (IOException e) {
//            System.out.println("failed to read");
//        }
//    }
//
////    ****************************
////    TESTS FOR getSignatureOrDesc
////    ****************************
//
//    /*
//     * Tests if the two methods invoke and invokeExact are recognised correctly
//     */
//    @Test
//    void getSignatureOrDescTest() {
////        new class reader, with name of class
//        try {
//            ClassReader cr = new ClassReader("java/lang/invoke/MethodHandle");
//            ClassVisitor cpr = new ClassPrinter();
//            cr.accept(cpr, 0);
//
//            boolean b = true;
//
//            for (String key : ((ClassPrinter) cpr).getMethods().keySet()) {
//                MethodParameter method = ((ClassPrinter) cpr).getMethods().get(key).createMethodParameter();
//                if (method.methodSignature == null) {
//                    if (!method.methodDesc.equals(VisibilityTools.getSignatureOrDesc(method.className, method.methodName, method.methodDesc))) {
//                        b = false;
//                    }
//                } else {
//                    if (method.methodDesc.equals(VisibilityTools.getSignatureOrDesc(method.className, method.methodName, method.methodDesc))) {
//                        b = false;
//                    }
//                }
//            }
//
//            assertTrue(b);
//        }
//        catch (ClassNotFoundException e) {
//            fail(e);
//        }
//        catch (IOException e) {
//            System.out.println("failed to read");
//        }
//    }
//
////    *********************************
////    TESTS FOR hasMethodUnderOverrides
////    *********************************
//
//    /*
//     *
//     */
////    TODO Check for difference in access
//    @Test
//    void hasMethodUnderOverridesTest() {
//        try {
//            ClassReader c1_reader = new ClassReader(Class1.class.getName());
//            ClassPrinter c1_printer = new ClassPrinter();
//            c1_reader.accept(c1_printer, 0);
//
//            for (String key : c1_printer.getMethods().keySet()) {
//                MethodParameter method = c1_printer.getMethods().get(key).createMethodParameter();
//                System.out.println(method.methodName + ": " +
//                        method.methodDesc + "\t" +
//                        method.methodSignature + "\t" +
//                        hasMethodUnderOverrides(c1_reader.getClassName(), method.methodName, method.methodDesc));
//            }
//
//            ClassReader c2_reader = new ClassReader(Class2.class.getName());
//            ClassPrinter c2_printer = new ClassPrinter();
//            c2_reader.accept(c2_printer, 0);
//
//
//            for (String key : c2_printer.getMethods().keySet()) {
//                MethodParameter method = c2_printer.getMethods().get(key).createMethodParameter();
//                System.out.println("assert" + hasMethodUnderOverrides(c1_reader.getClassName(), method.methodName, method.methodDesc) +
//                        "(hasMethodUnderOverrides(\"" + c2_reader.getClassName()
//                        + "\",\"" + method.methodName + "\",\"" + method.methodDesc + "\"));");
//                hasMethodUnderOverrides(c2_reader.getClassName(), method.methodName, method.methodDesc);
//            }
//
//        } catch (IOException e) {
//            System.out.println(e.getLocalizedMessage());
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//
//    }
//
//    /*
//     * Tests to ensure that hasMethodUnderOverride raises the Runtime exception on invalid class name.
//     */
////    TODO Check for difference in access
//    @Test
//    void hasMethodUnderOverridesTest_error() {
//        try {
//            hasMethodUnderOverrides("blah", "blah", "blah");
//        } catch (RuntimeException e) {
//            assertEquals("java.io.IOException: Class not found", e.getLocalizedMessage());
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//    }
//
////    ************************
////    TESTS FOR introduceField
////    ************************
//
//    /*
//     * Tests introduceField raises true if field introduced.
//     */
//    @Test
//    @SuppressWarnings("Duplicates")
//    void introduceFieldTest_FieldIntroduced() {
//        try {
//            ClassReader c2_reader = Mill.getInstance().getClassReader(Class2.class.getName());
//            ClassVisitor c2_visitor = new ClassVisitor(Opcodes.ASM5) {
//                @Override
//                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
//                    System.out.println("FieldName: " + name + "\t" + desc);
//                    super.visitEnd();
//                    return null;
//                }
//            };
//            c2_reader.accept(c2_visitor, 0);
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//
//        try {
//            assertTrue(introduceField(Class2.class.getName(), "number2", "I"));
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests introduceField raises true if field introduced.
//     */
//    @Test
//    @SuppressWarnings("Duplicates")
//    void introduceFieldTest_FieldNotIntroduced() {
//        try {
//            ClassReader c2_reader = Mill.getInstance().getClassReader(Class2.class.getName());
//            ClassVisitor c2_visitor = new ClassVisitor(Opcodes.ASM5) {
//                @Override
//                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
//                    System.out.println("FieldName: " + name + "\t" + desc);
//                    super.visitEnd();
//                    return null;
//                }
//            };
//            c2_reader.accept(c2_visitor, 0);
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//
//        try {
//            assertFalse(introduceField(Class2.class.getName(), "number1", "I"));
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//    }
//
//    /*
//     * Tests introduceField raises true if field introduced.
//     */
//    @Test
//    void introduceFieldTest_Field_InvalidField() {
//        try {
//            introduceField("blah", null, null);
//        } catch (RuntimeException e) {
//            assertEquals("java.io.IOException: Class not found", e.getLocalizedMessage());
//        }
//        catch (NoSuchClassException e) {
//            fail(e);
//        }
//    }
//}
//
//// UTILITY - TEST CLASSES
//
//class Class1 {
//    public int number1;
//
//    public Class2 method_notOveridden() {
//        return new Class2();
//    }
//
//    public Class1 method_diffReturn_moreSpecific() {
//        return new Class1();
//    }
//
//    public Object method_diffReturn_ObjectString() {
//        return new Object();
//    }
//
//    private Class2 method_DiffAccess() {
//        return new Class2();
//    }
//
//    public Class2 method_diffSignature_ObjectString(Object o) {
//        return new Class2();
//    }
//
//    public Class2 method_diffSignature_StringObject(String s) {
//        return new Class2();
//    }
//
//}
//
//class Class2 extends Class1 {
//    public int number2;
//
//    @Override
//    public Class2 method_diffReturn_moreSpecific() {
//        return new Class2();
//    }
//
//    private Class2 method_DiffAccess() {
//        return new Class2();
//    }
//
//    public String method_diffReturn_ObjectString() {
//        return "xyz";
//    }
//
//    public Class2 method_diffSignature_ObjectString(String s) {
//        return new Class2();
//    }
//
//    public Class2 method_diffSignature_StringObject(Object o) {
//        return new Class2();
//    }
//
//    public static void main(String... args) {
//        Class1 c1 = new Class1();
//        Class2 c2 = new Class2();
//        System.out.println("Class 1: " + c1.method_diffReturn_moreSpecific());
//        System.out.println("Class 1: " + c1.method_diffReturn_ObjectString());
//        System.out.println("Class 2: " + c2.method_DiffAccess());
//    }
//}
//
//class Class3 extends Class2 {
//    @Override
//    public Class2 method_diffReturn_moreSpecific() {
//        return new Class2();
//    }
//
//}
//
//class Class4 extends Class3 implements Interface4 {
//    @Override
//    public Class2 method_diffReturn_moreSpecific() {
//        return new Class2();
//    }
//
//    public void Interface4OverriddenMethod() {
//        boolean b = implementedDefaultMethod();
//    }
//
//    public Class4 values() {
//        return null;
//    }
//}
//
//abstract class Class5 {
//    public int field;
//    public static int staticField;
//    public final static int constantField = 4;
//
//    abstract public void abstractMethod();
//    public void implementedMethod() { System.out.print("implementedMethod()"); }
//    final public void finalMethod() { System.out.print("finalMethod()"); }
//}
//
//class Class6 extends Class5 {
//   public void abstractMethod() {
//        System.out.println("blah");
//    }
//}
//
//interface Interface1 {
//}
//
//interface Interface2 extends Interface1 {
//
//}
//
//interface Interface3 extends Interface1 {
//
//}
//
//interface Interface4 extends Interface3 {
//    public final static int NUMBER = 4;
//    public static int NUMBER2 = 5;
//
//    default boolean implementedDefaultMethod() {
//        System.out.println("A default method is added in interface");
//        return blah();
//    }
//
//    void Interface4OverriddenMethod();
//
//    default boolean blah() {
//        return true;
//    }
//}
//
//interface Interface5 {
//    default boolean blah() {
//        return false;
//    }
//
//    public static void main(String... args) {
//        Class4 c = new Class4();
//        System.out.println(c.blah());
//    }
//}
//
//enum Level {
//    HIGH("Red"),
//    MEDIUM("Yellow"),
//    LOW("Green");
//
//    private final String colour;
//
//    Level(String colour) {
//        this.colour = colour;
//    }
//
//    public String getColour() {
//        return colour;
//    }
//}
//
