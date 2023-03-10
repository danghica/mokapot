/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import xyz.acygn.millr.messages.NoSuchClassException;

/**
 *
 * @author thomas Objects of this class contains relevant informations regarding
 * a class, notably those that are useful for asm. Thse include the
 * classVersion, the classAccess, the class-name, the class-signature, the name
 * of the super-class, if any, and the name of the interfaces extended by this
 * class.
 */
class ClassParameter {

    public int classVersion;
    public int classAccess;
    public String className;
    public String classSignature;
    public String classSuperName;
    public String[] classInterfaces;
    public static final int javaAPI = 52;

    public static final ClassParameter CP_INT = new ClassParameter(javaAPI, Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC, "int", "I", null, new String[0]);
    public static final ClassParameter CP_BOOLEAN = new ClassParameter(javaAPI, Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC, "boolean", "Z", null, new String[0]);
    public static final ClassParameter CP_BYTE = new ClassParameter(javaAPI, Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC, "byte", "B", null, new String[0]);
    public static final ClassParameter CP_CHAR = new ClassParameter(javaAPI, Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC, "char", "C", null, new String[0]);
    public static final ClassParameter CP_DOUBLE = new ClassParameter(javaAPI, Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC, "double", "D", null, new String[0]);
    public static final ClassParameter CP_FLOAT = new ClassParameter(javaAPI, Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC, "float", "F", null, new String[0]);
    public static final ClassParameter CP_LONG = new ClassParameter(javaAPI, Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC, "long", "J", null, new String[0]);
    public static final ClassParameter CP_SHORT = new ClassParameter(javaAPI, Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC, "short", "S", null, new String[0]);

    public static final ClassParameter getPrimitiveClassParameter(String owner) {
        switch (owner) {
            case "int":
                return CP_INT;
            case "boolean":
                return CP_BOOLEAN;
            case "byte":
                return CP_BYTE;
            case "char":
                return CP_CHAR;
            case "double":
                return CP_DOUBLE;
            case "float":
                return CP_FLOAT;
            case "long":
                return CP_LONG;
            case "short":
                return CP_SHORT;
            default:
                throw new RuntimeException(owner + "is not a primitive type");
        }
    }

    /**
     * Create a ClassParameter object, by receiving relevant informations about
     * it as input. The signature, supername, and interfaces can be given as
     * null. Having null interfaces is understood as the class does not
     * implement any interface. Having a null superName is understood as either
     * the class is /java/lang/Object or the class is an interface.
     *
     * @param version Version of the class
     * @param access The encoding of the access.
     * @param name The name of the class. Throw an exception if name is null.
     * @param signature Signature. Can be null if the class is not generic.
     * @param superName Name of the super class. Can be null if the only class
     * extended is Object, or if the object stores information about the
     * /java/lang/Object class.
     * @param interfaces Array of strings representing the name of the
     * interfaces implemented.
     */
    protected ClassParameter(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (name == null) {
            throw new NullPointerException("name is null in the classParameter creation");
        }
        boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        if (superName == null && (!name.equals("java/lang/Object") && !isInterface && !TypeUtil.isPrimitive(TypeUtil.getObjectType(name)))) {
            throw new NullPointerException("Given a null name for the superClass of " + name + " but this one "
                    + "is neither /java/lang/Object nor an interface");
        }
        this.classVersion = version;
        this.classAccess = access;
        this.className = name;
        this.classSignature = signature;
        this.classSuperName = superName;
        this.classInterfaces = interfaces == null ? new String[0] : interfaces;
    }

    /**
     * Return a string representation of the fields of this class.
     *
     * @return A string representation of the class Parameter.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("Class Version: " + classVersion + "\n");
        sb.append("Class Access: " + classAccess + "\n");
        sb.append("Class Name: " + className + "\n");
        sb.append("Class Signature: " + classSignature + "\n");
        sb.append("Class SuperName: " + classSuperName + "\n");
        sb.append("Class Interfaces: " + (classInterfaces == null ? null : Arrays.toString(classInterfaces)) + "\n");
        return sb.toString();
    }

    /**
     * Check equality between two strings by taking care of the cases where they
     * are null. That is, returns true if none of them is null and the two are
     * equal, or when the two are null.
     *
     * @param s1 First string being compared for equality
     * @param s2 Second string being compared for equality
     * @return true if equal or both are null, false otherwise.
     */
    static boolean myEquals(String s1, String s2) {
        if (s1 == null) {
            if (s2 == null) {
                return true;
            }
            return false;
        } else {
            return s1.equals(s2);
        }
    }

    /**
     * Overridden version of equals. Return true if the object being compared is
     * a classParameter with equal fields.
     *
     * @param o Object being compared
     * @return true if the two classParameter have same parameters, false
     * otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof ClassParameter)) {
            return false;
        } else {
            final ClassParameter oClass = (ClassParameter) o;
            return ((oClass.classVersion == this.classVersion)
                    && (oClass.classAccess == this.classAccess)
                    && myEquals(oClass.className, this.className)
                    && myEquals(oClass.classSignature, this.classSignature)
                    && myEquals(oClass.classSuperName, this.classSuperName)
                    && Arrays.deepEquals(oClass.classInterfaces, this.classInterfaces));
        }

    }

    /**
     * A hashcode for object that takes care of the case where the object is
     * null by returning one.
     *
     * @param s The maybe null object we desire the hashcode of
     * @return The hashcode.
     */
    protected static int myHashcode(Object s) {
        if (s == null) {
            return 1;
        } else {
            return s.hashCode();
        }
    }

    /**
     * Return a hashCode for the object.
     *
     * @return A hashcode
     */
    @Override
    public int hashCode() {
        return (new Integer(classVersion)).hashCode() + (new Integer(classAccess)).hashCode() + className.hashCode() + myHashcode(classSignature) + myHashcode(classSuperName) + Arrays.hashCode(classInterfaces);
    }

    /**
     * A classVisitor that will allows to visit a classFile
     */
    protected static class ClassVisitorGetInfo extends ClassVisitor {

        ClassParameter cp;

        public ClassVisitorGetInfo(int api) {
            super(api);
        }


        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            //       System.out.println("visiting the header !");
            //         if (superName != null){
            cp = new ClassParameter(version, access, name, signature, superName, interfaces);
            //           cp.printClass();
            //       }
            //        else { cp = new ClassParameter(version,access, name, signature, "java/lang/Object", interfaces);
            //       cp.printClass();
            //       }
        }
    }

    /**
     * Given a classParameter cp, return the classParameter corresponding to the
     * class of Arrays whose elements are of type cp. Return null if the input
     * ClassParameter is null.
     *
     * @param cp The ClassParameter corresponding to the element of the array.
     * @return The classParameter corresponding to the array.
     */
    private static ClassParameter arrayify(ClassParameter cp) {
        if (cp == null) {
            return null;
        }
        String newSignature = null;
        String newSuperName = null;
        String[] newInterfaces = new String[0];
        if (cp.classSignature != null) {
            newSignature = "[" + cp.classSignature;
        }
        if (cp.classSuperName != null) {
            newSuperName = "[" + cp.classSuperName;
        }
        else{
            newSuperName = "java/lang/Object";
        }
        if (cp.classInterfaces != null) {
            newInterfaces = new String[cp.classInterfaces.length];
            for (int i = 0; i < cp.classInterfaces.length; i++) {
                newInterfaces[i] = "[" + cp.classInterfaces[i];
            }
        }
        return new ClassParameter(cp.classVersion, cp.classAccess, "[" + cp.className, newSignature, newSuperName, newInterfaces);
    }

    /**
     * Will create a classParameter object given only the name of the class by
     * looking exploring the associated classFile. Can get the name of an array
     * as input, or the name of a primitive type. In the case of primitive types
     * since no classFile exists, it will return an equivalent classFile with
     * version 52, which is final and public, has no superName and implements no
     * interface. This method may try to get information about the parameters by
     * reading the classFile, and will throw a runtime Exception if the
     * classFile needed cannot be read. Throw nullPointerException if the input
     * string is null.
     *
     * @param owner The name of the class. Names for primitive are accepted,
     * like "int", as well as arrays, like "'java/lang/Object"
     * @return The associated classParameter.
     */
    public static ClassParameter getClassParameter(String owner) throws NoSuchClassException {
        if (owner == null) {
            throw new NullPointerException("the input parameter is null");
        }
        Type t = TypeUtil.getObjectType(owner);
        int arrayArity = 0;
        while (t.getDescriptor().startsWith("[")) {
            t = Type.getType(t.getDescriptor().substring(1));
            arrayArity += 1;
        }
        if (TypeUtil.isPrimitive(t)) {
            return new ClassParameter(javaAPI, Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC, t.getClassName(), t.getDescriptor(), null, new String[0]);
        } else {
               Optional<ClassDataBase.ClassData> cd = ClassDataBase.getClassDataOrNull(owner);
               if (((Optional) cd).isPresent()){
                   return cd.get().cp;
               }
                ClassReader cr = Mill.getClassReader(t.getInternalName());
                ClassParameter cp = getClassParameter(cr);
                while (arrayArity != 0) {
                    cp = arrayify(cp);
                    arrayArity -= 1;
                }
                return cp;
        }
    }

    /**
     * Return the classParameter associated with a classReader, by simply
     * reading the class and collecting the necessary information.
     *
     * @param cr The classReader
     * @return The classParameter associated with it.
     */
    public static ClassParameter getClassParameter(ClassReader cr) throws NullPointerException {
        if (cr == null) {
            return null;
        }
 
        ClassVisitorGetInfo cv = new ClassVisitorGetInfo(Opcodes.ASM6);
        cr.accept(cv, 0);
        return cv.cp;
    }

    /**
     * Print the classParameter object in the standard output in a meaningful
     * way.
     *
     */
    public void printClass() {
        System.out.println("Name of class: " + className);
        System.out.println("Signature of class: " + classSignature);
        System.out.println("superClass of class: " + classSuperName);
        System.out.println("Implements interfaces:");
        for (String inter : classInterfaces) {
            System.out.println("\t" + inter);
        }
        System.out.println("Version of class: " + classVersion);
        System.out.println("Access of class: " + classAccess);
    }

    /**
     * Given the classParameter of an array, return the classParameter of the
     * element type of the array. For instance, given the classParameter of the
     * array [[/java/lang/Object, it will return the classParameter of
     * /java/lang/Object. If the input is not a classParameter of an array, it
     * will simply acts as identity.
     *
     * @param cp A classParameter of a (maybe) array.
     * @return The classParameter corresponding to its element type.
     */
    public static ClassParameter getCoreClassParameter(ClassParameter cp) throws NoSuchClassException {
        if (cp == null) {
            return null;
        }
        String newDesc = cp.className;
        while (newDesc.startsWith("[")) {
            newDesc = newDesc.substring(1);
        }
        return ClassParameter.getClassParameter(newDesc);
    }

    /**
     * Given the name of a class that maybe an array, return the classParameter
     * associated with it (if not an array), or to its element type (if this is
     * the name of an array). Throw an exception if the input parameter is
     * empty.
     *
     * @param internalName To fill
     * @return To fill
     */
    public static ClassParameter getCoreClassParameter(String internalName) throws NoSuchClassException {
        if (internalName == null) {
            throw new NullPointerException("The input parameter internalName is null");
        }
        return getCoreClassParameter(getClassParameter(internalName));
    }


    public boolean isConcreteClass(){
        return isArray() | (!isInterface() && ( (Opcodes.ACC_ABSTRACT & classAccess) == 0));
    }

    public boolean isArray(){
        return className.startsWith("[");

    }

    public boolean isInterface(){
        return (classAccess & Opcodes.ACC_INTERFACE) != 0;
    }

}
