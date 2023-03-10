/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import xyz.acygn.millr.messages.MessageUtil;
import xyz.acygn.millr.messages.NoSuchClassException;
import xyz.acygn.millr.util.RuntimeUnwrapper;

/**
 * @author thomasc Collection of methods allowing us to study how class /
 * methods relate to their environment.
 */
public final class VisibilityTools {

//    /**
//     * Return the list of classes extended by the class read by the classreader
//     * that does not belong to the same package as the class read by the
//     * classreader.
//     *
//     * @param cr A ClassReader reading a classFile.
//     * @return The list of classes extended by the class read that does not
//     * belong to the same package.
//     */
//    static List<ClassDataBase.ClassData> doesExtendsOuterPackage(String className) throws NoSuchClassException {
//        ClassDataBase.ClassData cd = ClassDataBase.getClassData(className);
//        List<ClassDataBase.ClassData> listSuperClass = new ArrayList<>();
//        ClassDataBase.ClassData cdTemp = cd;
//        while (cdTemp.getSuperClass().isPresent()) {
//            cdTemp = cdTemp.getSuperClass().get();
//            if (!cdTemp.getPackage().equals(cd.getPackage())) {
//                listSuperClass.add(cdTemp);
//            }
//        }
//        return listSuperClass;
//    }




    /**
     * Given a method, fully qualified by the name of its owner, its name, and its description, returns wether the code
     * for the actual method lies outside the project.
     * @param owner The name of the class that the method is called on.
     * @param methodName The name of the method.
     * @param methodDesc The description of the method.
     * @return
     */
    static boolean doesMethodImplementationNonProject(String owner, String methodName, String methodDesc){
        if (owner.startsWith("[")){
            return true;
        }
        if (TypeUtil.isNotProject(owner)){
            return true;
        }
        return doesMethodImplementationNonProject(MethodParameter.getMethod(owner, methodName, methodDesc));
    }



    static boolean doesMethodImplementationNonProject(MethodParameter mp){
        return TypeUtil.isNotProject(mp.className);
    }






    /**
     * The list of methodParameter extended by a class, such that the methodName
     * and methodDescription of the class are overriding method from the
     * superclasses.
     *
     * @param owner      The internal name of class.
     * @param methodName The name of method (thought as belonging to the class)
     * @param methodDesc The description of the method
     * @return The list of superclasses of the original class that have
     * implementation of a method that this method overrides.
     */
//    static List<MethodParameter> doesOverride(String owner, String methodName, String methodDesc) throws NoSuchClassException {
//        if (TypeUtil.getObjectType(owner).getSort() == Type.ARRAY || owner.startsWith("[")) {
//            List<MethodParameter> returnlist = new ArrayList<>(1);
//            returnlist.add(hasMethodUnderOverrides("java/lang/Object", methodName, methodDesc));
//            return returnlist;
//        } else if (TypeUtil.getObjectType(owner).getSort() != Type.OBJECT) {
//            return new ArrayList<>();
//        }
//        List returnList = new ArrayList<>();
//        for (String sr : getSuperClasses(Mill.getInstance().getClassReader(owner))) {
//            MethodParameter mp = hasMethodUnderOverrides(sr, methodName, methodDesc);
//            if (mp != null) {
//                returnList.add(mp);
//            }
//        }
//        return returnList;
//    }

    /**
     * Given a method overriding some methods coming from outside the project,
     * returns the last subclass that is not project that this method overrides.
     * Throws a runtime Exception if this method is called on a method that does
     * not override a non-project method.
     *
     * @param owner      The name of the owner of the method
     * @param methodName The name of the method
     * @param methodDesc The description of the method
     * @return The least non-project subclass that this method overrides.
     */
//    static String upperNotProjectClassOveride(String owner, String methodName, String methodDesc) throws NoSuchClassException {
//        if (TypeUtil.getObjectType(owner).getSort() == Type.ARRAY) {
//            return "java/lang/Object";
//        }
//        if (TypeUtil.isNotProject(owner)) {
//            return owner;
//        }
//        MethodParameter output = doesOverride(owner, methodName, methodDesc).stream().filter(e-> TypeUtil.isNotProject(e.className)).reduce((first, second) -> second).orElse(null);
//        if (output != null) {
//            return output.className;
//        }
//        throw new RuntimeException("This method was called on a method that does not overrides a non-project method \n"
//                + " owner = " + owner + "\n"
//                + " methodName = " + methodName + "\n"
//                + " methodDesc = " + methodDesc + "\n"
//               // + doesMethodComeFromNonProject(owner, methodName, methodDesc) + "\n"
//                + doesOverrideNonProject(owner, methodName, methodDesc) + "\n"
//                + TypeUtil.isNotProject(owner));
//    }

    /**
     * Method to check if a method overrides another method that is not part of
     * the project being milled.
     *
     * @param owner      Internal name of the class the method belongs to.
     * @param methodName The name of the method.
     * @param methodDesc The description of the method.
     * @return True if the method defined by the triple of inputs overrides a
     * method that is not part of the project being milled.
     */
//    static final boolean doesOverrideNonProject(String owner, String methodName, String methodDesc) throws NoSuchClassException {
//        return doesOverride(owner, methodName, methodDesc).stream().anyMatch(e-> TypeUtil.isNotProject(e.className));
//    }

//    /**
//     * Method to check if a method either is not part of the project being
//     * milled, or overrides another method that is not part of the project being
//     * milled.
//     *
//     * @param owner      Name of the class the method belongs to.
//     * @param methodName The name of the method.
//     * @param methodDesc The description of the method.
//     * @return True if the method comes from outside the project, or if it
//     * describes a method that overrides another not part of the project.
//     */
//    static final boolean doesMethodComeFromOutsideProject(String owner, String methodName, String methodDesc) throws NoSuchClassException {
//        return TypeUtil.isNotProject(owner) || doesOverrideNonProject(owner, methodName, methodDesc);
//    }


    /**
     * Method that will allows us to check whether a given method of a class
     * implements some methods coming from interfaces implemented by this class.
     *
     * @param owner      The name of the class the method belongs to.
     * @param methodName The name of the method.
     * @param methodDesc The description of the method.
     * @return The list of interfaces that this have a method that this method
     * implements.
     */
//    static final List<String> doesImplement(String owner, String methodName, String methodDesc) throws NoSuchClassException {
//        if (TypeUtil.getObjectType(owner).getSort() == Type.ARRAY) {
//            // Array does only implement Cloneable and Seriaziable interfaces, but these ones are only marker interfaces without methods.
//            return new ArrayList<>();
//        }
//        if (TypeUtil.getObjectType(owner).getSort() != Type.OBJECT) {
//            return new ArrayList<>();
//        }
//        List<String> listString = new ArrayList<>();
//        for (String inter : getInterfaces(owner)) {
//            if (hasMethodUnderOverrides(inter, methodName, methodDesc) != null) {
//                listString.add(inter);
//            }
//        }
//        return listString;
//    }

    /**
     * Allows us to check whether a method is a implementation of an abstract
     * method that is not being milled as part of the project.
     *
     * @param owner      The name of the class the method belongs to.
     * @param methodName The name of the method.
     * @param methodDesc The description of the method
     * @return True if this method is an implementation of an abstract method
     * belonging to an interface that is not part of the project.
     */
//    static boolean doesImplementNonProject(String owner, String methodName, String methodDesc) throws NoSuchClassException {
//        return doesImplement(owner, methodName, methodDesc).stream().anyMatch(TypeUtil::isNotProject);
//    }

    /**
     * Check if a method is implementing - overriding a method that is not part
     * of the project being milled.
     *
     * @param owner      The name of the class the method belongs to
     * @param methodName The name of the method
     * @param methodDesc The description of the method
     * @return True if this method is implementing - overriding a method that is
     * not part of the project being milled.
     */
//    static boolean doesImplementOrOverrideNonProject(String owner, String methodName, String methodDesc) throws NoSuchClassException {
//        return doesImplementNonProject(owner, methodName, methodDesc) || doesOverrideNonProject(owner, methodName, methodDesc) || isValueFromEnum(owner, methodName, methodDesc);
//    }


//    static boolean doesMethodComeFromNonProject(MethodParameter mp){
//        return doesMethodComeFromNonProject(mp.className, mp.methodName, mp.methodDesc);
//    }

    /**
     * Return whether the method described is not part of the project being
     * milled, or if it is an implementation - overriding of a method not part
     * of the project being milled.
     *
     * @param owner      The name of the class the method belongs to.
     * @param methodName The name of the method.
     * @param methodDesc The description of the method.
     * @return True if this method overrides or implement an method not part of
     * the project being milled, or if it is not part of the project being
     * milled.
     */
//    static boolean doesMethodComeFromNonProject(String owner, String methodName, String methodDesc) throws NoSuchClassException {
//        return doesImplementOrOverrideNonProject(owner, methodName, methodDesc) || TypeUtil.isNotProject(owner) || isValueFromEnum(owner, methodName, methodDesc);
//    }

    /**
     * Each ENUM class has to implement a special method called values; this
     * method check whether the method considered is this method.
     *
     * @param owner      The internal name of the class the method belongs to.
     * @param methodName The name of the method.
     * @param methodDesc The description of the method
     * @return True if the method is the "values" method from ENUM.
     */
    static boolean isValueFromEnum(String owner, String methodName, String methodDesc) throws NoSuchClassException {
        String descOwner = Type.getObjectType(owner).getDescriptor();
        if (methodDesc.equals("()[" + descOwner) && methodName.equals("values")) {
            ClassReader cr;

            cr = Mill.getInstance().getClassReader(owner);
            if (cr.getSuperName().equals("java/lang/Enum")) {
                return true;
            }
        }
        return false;
    }


    static boolean isValueFromEnum(MethodParameter mp){
        return isValueFromEnum(mp.className, mp.methodName, mp.methodDesc);
    }

    /**
     * Check whether a field of a class is coming from a class not being milled
     * as part of the project.
     *
     * @param owner     The name of the class the field belongs to.
     * @param fieldName The name of the field.
     * @param fieldDesc The description of the field.
     * @return True if this fields hides, or is coming from another class not
     * being milled.
     */
//    static final boolean doesFieldComeFromNotProject(String owner, String fieldName, String fieldDesc) throws NoSuchClassException {
//        return isFieldExtendedNotProject(owner, fieldName, fieldDesc) || TypeUtil.isNotProject(owner) || isValueField(owner, fieldName, fieldDesc);
//    }

    /**
     * Each Enum class has a special field called value; this method examines
     * whether the field given as input is this special field.
     *
     * @param owner     Internal name of the class owning the field.
     * @param fieldName The name of the field.
     * @param fieldDesc The description of the field.
     * @return True if the input field is the special values field from an enum
     * class.
     */
    static final boolean isValueField(String owner, String fieldName, String fieldDesc) throws NoSuchClassException {
        String valuesDesc = "[" + Type.getObjectType(owner).getDescriptor();
        return (fieldName.equals("$VALUES") && fieldDesc.equals(valuesDesc) && isEnum(owner));
    }

    /**
     * Examine if a class is a Enum class.
     *
     * @param owner The internal name of a class
     * @return True if it is an enum class.
     */
    static final boolean isEnum(String owner) throws NoSuchClassException {
        return ClassDataBase.getClassData(owner).getSuperClass().isPresent() &&
                ClassDataBase.getClassData(owner).getSuperClass().get().cp.className.equals("java/lang/Enum");
    }

    /**
     * Examine whether a field of a class belongs to a class not being milled as
     * part of the project.
     *
     * @param owner     The internal name of the class owning the field.
     * @param fieldName The name of the field.
     * @param fieldDesc The description of the field.
     * @return True if the field belongs to a class not being milled as part of
     * the project.
     */
//    static final boolean isFieldExtendedNotProject(String owner, String fieldName, String fieldDesc) throws NoSuchClassException {
//        return TypeUtil.isNotProject(classFieldComesFrom(owner, fieldName, fieldDesc));
//    }

    /**
     * FIXME : BREAK IF THE FIELD COMES FROM INTERFACE
     * Given a field of a class,
     * examines what class the field actually comes from. That is, the original
     * class may extend another class, and the field is actually introduced in
     * the class being extended, not in the original class.
     *
     * @param owner     The internal name of the class the field belongs to.
     * @param fieldName The name of the field.
     * @param fieldDesc The description of the field.
     * @return The internal name of the class introducing the field.
     */
//    static final String classFieldComesFrom(String owner, String fieldName, String fieldDesc) throws NoSuchClassException {
//        String ClassThatMayPotentiallyOwnsField = owner;
//        while (true) {
//            if (introduceField(ClassThatMayPotentiallyOwnsField, fieldName, fieldDesc)) {
//                return ClassThatMayPotentiallyOwnsField;
//            } else if (ClassThatMayPotentiallyOwnsField.equals("java/lang/Object")) {
//                break;
//            } else {
//                ClassThatMayPotentiallyOwnsField = (Mill.getInstance().getClassReader(ClassThatMayPotentiallyOwnsField)).getSuperName();
//            }
//        }
//        String classWhoseInterfacesMayOwnField = owner;
//        while (true){
//            String interfaze = interfaceFieldComeFrom(classWhoseInterfacesMayOwnField, fieldName, fieldDesc);
//            if (interfaze!=null){
//                return interfaze;
//            }
//            else if (classWhoseInterfacesMayOwnField.equals("java/lang/Object")){
//                    throw new NoSuchClassException("Impossible to find the Class that implements the field " + fieldName + "whose desc is " + fieldDesc + " exploring from " + owner );
//                }
//            else{
//                classWhoseInterfacesMayOwnField = (Mill.getInstance()).getClassReader(classWhoseInterfacesMayOwnField).getSuperName();
//            }
//        }
//
//    }

//    static final String interfaceFieldComeFrom(String owner, String fieldName, String fieldDesc) throws NoSuchClassException {
//        String InterfaceThatMayPotentialOwnsField = owner;
//        if (introduceField(InterfaceThatMayPotentialOwnsField, fieldName, fieldDesc)) {
//            return InterfaceThatMayPotentialOwnsField;
//        }
//        for (String interfaze : getInterfaces(owner)) {
//            if (interfaceFieldComeFrom(interfaze, fieldName, fieldDesc) != null) {
//                return interfaze;
//            }
//        }
//        return null;
//    }

    /**
     * Given a class, examines whether the class introduces a field.
     *
     * @param owner     The internal name of a class.
     * @param fieldName The name of the field.
     * @param fieldDesc The description of the field.
     * @return True if the class introduces the field.
     */
//    static final boolean introduceField(String owner, String fieldName, String fieldDesc) throws NoSuchClassException {
//        class booleanWrapper {
//
//            boolean b = false;
//        }
//        final booleanWrapper bool = new booleanWrapper();
//        ClassReader cr = Mill.getInstance().getClassReader(owner);
//        ClassVisitor cv = new ClassVisitor(Opcodes.ASM6) {
//            @Override
//            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
//                if (name.equals(fieldName) && desc.equals(fieldDesc)) {
//                    bool.b = true;
//                }
//                return super.visitField(access, name, desc, signature, value);
//            }
//        };
//        cr.accept(cv, ClassReader.SKIP_CODE);
//        return bool.b;
//    }

    /**
     * Given the name of class, the name of a methodName, and the name of a
     * methodDescription, returns whether this class has a method that the
     * original method with methodName and methodDescription overrides.
     *
     * @param owner      The internal name of a class
     * @param methodName The name of a method
     * @param methodDesc The description of the method
     * @return The methodParameter this class has an overridden implementation of this
     * method, null otherwise.
     */
//    static MethodParameter hasMethodUnderOverrides(String owner, String methodName, String methodDesc) throws NoSuchClassException {
//
//        class MethodParameterWrapper {
//
//            MethodParameter mp = null;
//        }
//
//        class ExceptionWrapper extends RuntimeException {
//
//            ExceptionWrapper(Exception e) {
//                super(e);
//                this.e = e;
//            }
//
//            Exception e;
//        }
//        final MethodParameterWrapper mpwrap = new MethodParameterWrapper();
//        ClassReader cr = Mill.getInstance().getClassReader(owner);
//        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5) {
//            @Override
//            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
//                try {
//                    if (name.equals(methodName) && doesDescriptionOverrides(methodDesc, descriptor)) {
//                        mpwrap.mp = MethodParameter.getMethod(owner, methodName, descriptor);
//                        super.visitEnd();
//                    }
//                } catch (NoSuchClassException ex) {
//                    throw new ExceptionWrapper(ex);
//                }
//                return null;
//            }
//        };
//        try {
//            cr.accept(cv, 0);
//        } catch (ExceptionWrapper ex) {
//            throw (NoSuchClassException) ex.e;
//        }
//        return mpwrap.mp;
//    }

    /**
     * Given a method, return the the signature of this method if this one is
     * not null, its description otherwise.
     *
     * @param owner      The internal name of the class owning a method.
     * @param methodName The name of the method.
     * @param methodDesc The description of the method,
     * @return Its signature if this one is not null, its description otherwise.
     */
    @Deprecated
    static final String getSignatureOrDesc(String owner, String methodName, String methodDesc) throws NoSuchClassException {
        MethodParameter mp = MethodParameter.getMethod(owner, methodName, methodDesc);
        return mp.methodSignature != null ? mp.methodSignature : methodDesc;
    }

    /**
     * Return whether this method is "special", that is "init" or "clinit", or
     * if it is the value() method from enum.
     *
     * @param owner      The internal name of the class the method belongs to.
     * @param methodName The name of the method.
     * @param methodDesc The description of the method.
     * @return True if this method is init, clinit, or values from enum
     */
//    public static final boolean isMethodSpecial(String owner, String methodName, String methodDesc) throws NoSuchClassException {
//        return (isValueFromEnum(owner, methodName, methodDesc)
//                || (methodName.equals("<init>") && methodDesc.equals("()V"))
//                || (methodName.equals("<clinit>") && methodDesc.equals("()V")));
//    }

    /**
     * Examine whether a method is signature polymorphic. A method is signature
     * polymorphic if and only if all of the following conditions hold : It is
     * declared in the java.lang.invoke.MethodHandle class. It has a single
     * formal parameter of type Object[]. It has a return type of Object. It has
     * the ACC_VARARGS and ACC_NATIVE flags set. In Java SE 7, the only
     * signature polymorphic methods are the invoke and invokeExact methods of
     * the class java.lang.invoke.MethodHandle.
     *
     * @param mp A method Parameter.
     * @return True if it refers to a signature polymorphic method, false
     * otherwise.
     */ // 
    public static final boolean isSignaturePolymorphic(MethodParameter mp) {
        if (mp.className.equals("java/lang/invoke/MethodHandle")) {
            if (mp.methodDesc.equals("([Ljava/lang/Object;)Ljava/lang/Object;")) {
                if ((mp.methodAccess & Opcodes.ACC_VARARGS) != 0) {
                    if ((mp.methodAccess & Opcodes.ACC_NATIVE) != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Given a SubProject sp, and a file F, returns whether this file is part of
     * SubProject.
     *
     * @param f  A file
     * @param sp A subproject
     * @return True if f is part of this subproject, false otherwise.
     */
    public final static boolean isSubPathInput(File f, SubProject sp) throws IOException {
        try {
            Path path = f.getCanonicalFile().toPath();
            return path.startsWith(sp.inputFile.getCanonicalPath());
        } catch (IOException ex) {
            throw new IOException(((f == null) ? "File is null" : "Path to file" + f.toString()) + ((sp == null) ? "subproject is null" : sp.inputFile.toString()), ex);
        }

    }

    /**
     * Given a String representing a list of ClassPath, with ";" between each
     * ClassPath, filter out those that are part of a SubProject.
     *
     * @param classPath a sequence of classpaths, typically the one returned by
     *                  System.getProperty();
     * @param col       A collection of subprojects
     * @return A classpath as the input one without those paths that are
     * subpaths of a SubProject.
     */
    public final static String getGoodClassPath(String classPath, Collection<SubProject> col) throws IOException {
        String separator = File.pathSeparator;
        String[] stringPaths = classPath.split(separator);
        StringBuilder returnString = new StringBuilder();
        boolean b;
        for (String path : stringPaths) {
            b = false;
            for (SubProject sp : col) {
                if (isSubPathInput(new File(path), sp)) {
                    b = true;
                }
            }
            if (!b) {
                returnString.append(path).append(separator);
            }
        }
        return returnString.toString();
    }

    /**
     * Given a String representing a list of ClassPath, with ";" between each
     * ClassPath, filter out those that are part of a SubProject.
     *
     * @param classPath a sequence of classpaths, typically the one returned by
     *                  System.getProperty();
     * @param sp        A subproject
     * @return A classpath as the input one without those paths that are
     * subpaths of a SubProject.
     */
//    public final static String getGoodClassPath(String classPath, SubProject sp) throws IOException {
//        Collection<SubProject> col = new ArrayList<>(1);
//        col.add(sp);
//        return getGoodClassPath(classPath, col);
//    }

    /**
     * Given a collection of SubProject col, returns all the ClassPaths loaded
     * by the System ClassLoader others than the ones corresponding to theses
     * sub-projects.
     *
     * @param sp  A subproject.
     * @return The classpath loaded by the System classloader exempt of the
     * classpath corresponding to this subproject.
     */
    public final static String getClassPathOtherThanSubProject(SubProject sp) throws IOException {
        Collection<SubProject> col = new ArrayList<>(1);
        col.add(sp);
        return getClassPathOtherThanSubProject(col);
    }

    /**
     * Given a collection of SubProject sp, returns all the ClassPaths loaded by
     * the System ClassLoader others than the one corresponding to this
     * sub-projects.
     *
     * @param col A collection of subproject.
     * @return The classpath loaded by the System classloader exempt of the
     * classpath corresponding to this subproject.
     */
    public final static String getClassPathOtherThanSubProject(Collection<SubProject> col) throws IOException {
        return getGoodClassPath(getCurrentClassPath(), col);
    }

    public final static String getCurrentClassPath() {
        return tryGetClassPathClassLoader(VisibilityTools.class.getClassLoader());
    }

    public final static String tryGetClassPathClassLoader(ClassLoader currentClassLoader) {
        String returnString = System.getProperty("java.class.path");
        String separator = File.pathSeparator;
        try {
            // First, the case where the current ClassLoader is an URL ClassLoader.
            URLClassLoader currentCL = (URLClassLoader) currentClassLoader;
            URL[] currentUrl = currentCL.getURLs();
            StringBuilder ClassPathBuilder = new StringBuilder();
            try {
                for (URL url : currentUrl) {
                    ClassPathBuilder.append(Paths.get(url.toURI()).toString()).append(separator);
                }
            } catch (URISyntaxException ex) {
                return returnString;
            }
            returnString = ClassPathBuilder.toString();
        } catch (ClassCastException ex) {
            try {
                // Then we try the case where the current ClassLoader is an ANT ClassLoader.
                Class AntClassLoaderClass = currentClassLoader.loadClass("org.apache.tools.ant.AntClassLoader");
                Method getClassPathMethod = AntClassLoaderClass.getDeclaredMethod("getClasspath", new Class[0]);
                String ClassPath = (String) getClassPathMethod.invoke(currentClassLoader, new Object[0]);
                returnString = ClassPath + separator;
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exTwo) {
                MessageUtil.error(exTwo).report().resume();
                return returnString;
            }
        }
        return returnString;

    }

//    static public String getOwnerOfField(String className, String name, String desc){
//        List<FieldParameter> listField =  ClassDataBase.getClassData(className).getAllField().stream().
//                filter(e->e.name.equals(name) && e.desc.equals(desc)).collect(Collectors.toList());
//        //If there is none, then that's a problem:
//        if (listField.isEmpty()){
//            throw new RuntimeException("impossible to find field " + name + " whose description is "
//                    + desc + " from className " + className);
//        }
//        // if there is only one, then it's easy.
//        if (listField.size()==0){
//            return listField.get(0).cp.className;
//        }
//        //If there are more, we need to find the "uppest" one.
//        List<ClassDataBase.ClassData> listClassData = new ArrayList<>(listField.size());
//        listField.stream().forEach(e->listClassData.add(ClassDataBase.getClassData(e.cp.className)));
//        Collections.sort(listClassData);
//        listClassData.
//
//    }





}
