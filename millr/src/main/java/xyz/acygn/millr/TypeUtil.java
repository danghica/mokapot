/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import xyz.acygn.millr.messages.NoSuchClassException;

/**
 * Collection of static final methods and constant that allows us to deal and
 * operate with types, descriptions, and signatures more easily.
 *
 * @author thomas
 */
public class TypeUtil {


    public static final Type ObjectType = Type.getType("Ljava/lang/Object;");

    /**
     * The array of primitive types.
     */
    private static final Type[] PRIMITIVE_TYPE = new Type[]{
        Type.BOOLEAN_TYPE,
        Type.BYTE_TYPE,
        Type.CHAR_TYPE,
        Type.DOUBLE_TYPE,
        Type.FLOAT_TYPE,
        Type.INT_TYPE,
        Type.LONG_TYPE,
        Type.SHORT_TYPE
    };

    /**
     * Given the internal name of class, or the internal name of a primitive
     * type ("boolean" for instance), or "void", returns the type associated
     * with it.
     *
     * @param internalName of a Class or a primitive type.
     * @return The associated type.
     */
    public static Type getObjectType(String internalName) {
        switch (internalName) {
            case "boolean":
                return Type.BOOLEAN_TYPE;
            case "byte":
                return Type.BYTE_TYPE;
            case "char":
                return Type.CHAR_TYPE;
            case "double":
                return Type.DOUBLE_TYPE;
            case "float":
                return Type.FLOAT_TYPE;
            case "int":
                return Type.INT_TYPE;
            case "long":
                return Type.LONG_TYPE;
            case "short":
                return Type.SHORT_TYPE;
            case "void":
                return Type.VOID_TYPE;
            default:
                return Type.getObjectType(internalName);
        }
    }


    /**
     * Acts as isNotProject.
     *
     * @param T A type.
     * @return True if the class corresponding to this type will be milled,
     * false otherwise.
     * @deprecated in favor of isNotProject()
     */
    @Deprecated
    static boolean isSystem(Type T) {
        return isNotProject(T);
    }


    /**
     * This methods discriminates between those types corresponding to classes
     * that will be milled as part of the transformation, and those that won't.
     *
     * @param internalName internal name of a type
     * @return true if not to be milled
     */
    @Deprecated
    static boolean isSystem(String internalName) {
        return isSystem(getObjectType(internalName));
    }

    /**
     * Returns true if the type T does not have a corresponding
     * {@link ClassReader} in {@link Mill}'s collection of class readers for
     * milling.
     *
     * @param T type to check
     * @return true if Mill does not have a ClassReader for T
     * @deprecated in favor of isNotProject()
     */
    @Deprecated
    static boolean isProtected(Type T) {
        // original implementation removed as it was identical to isNotProject
        return isNotProject(T);
    }

    /**
     * Given a Type T, returns its internal name if this one corresponds to a
     * class, or its descriptor if this one is primitive.
     *
     * @param T A type
     * @return Internal name for the type, or its description if positive.
     * @deprecated due to incoherent handling of arrays
     */
    @Deprecated
    static String getInternalName(Type T) {
        if (!isPrimitive(T)) {
            return T.getInternalName();
        }
        return T.getDescriptor();
    }

    /**
     * Explore whether this Type is the type of a class that is not milled, or
     * the type of an array (maybe multidimensional) of elements whose types are
     * not milled.
     *
     * @param internalName internal name of class
     * @return True if the class corresponding to T is not a milled, or if T is
     * the type of an array of element whose class is not milled.
     * @deprecated in favor of isRecursivelyNotProject
     */
    @Deprecated
    static boolean isRecursivelySystem(String internalName) {
        return isRecursivelyNotProject(getObjectType(internalName));
    }

    /**
     * Whether a signature / description has a component that is an array.
     *
     * @param signature A description - signature
     * @return True if this one has a component that is an array, false
     * otherwise.
     */
    static boolean hasArray(String signature) {
        class hasArraySignatureVisitor extends SignatureVisitor {

            private boolean hasArray = false;

            hasArraySignatureVisitor(int api) {
                super(api);
            }

            @Override
            public SignatureVisitor visitArrayType() {
                hasArray = true;
                return this;
            }
        }
        SignatureReader sr = new SignatureReader(signature);
        hasArraySignatureVisitor sv = new hasArraySignatureVisitor(Opcodes.ASM5);
        sr.accept(sv);
        return sv.hasArray;
    }

    /**
     * Return whether a type is primitive or not.
     *
     * @param t A type.
     * @return True if t is primitive, false otherwise.
     */
    static boolean isPrimitive(Type t) {
        return (Arrays.asList(PRIMITIVE_TYPE).contains(t));
    }

    /**
     * Return whether a type T is the type of an Array Wrapper.
     *
     * @param T A type T
     * @return true if T is one of the ArrayWrapper types, false otherwise.
     */
    static boolean isArrayWrapper(Type T) {
        return Arrays.asList(PathConstants.LIST_ARRAY_WRAPPER).contains(getInternalName(T));
    }

    /**
     * Given a primitive type, returns the opcode associated with it (for
     * instance, Opcodes.T_BOOLEAN for boolean). If the Type is not primitive,
     * returns -1.
     *
     * @param T A type
     * @return -1 if the type T is not primitive, the opcode associated with the
     * primitive type otherwise.
     */
    static int getPrimOpcode(Type T) {
        switch (T.getSort()) {
            case (Type.BOOLEAN):
                return Opcodes.T_BOOLEAN;
            case (Type.BYTE):
                return Opcodes.T_BYTE;
            case (Type.CHAR):
                return Opcodes.T_CHAR;
            case (Type.DOUBLE):
                return Opcodes.T_DOUBLE;
            case (Type.FLOAT):
                return Opcodes.T_FLOAT;
            case (Type.INT):
                return Opcodes.T_INT;
            case (Type.LONG):
                return Opcodes.T_LONG;
            case (Type.SHORT):
                return Opcodes.T_SHORT;
            default:
                return -1;
        }
    }

    /**
     * Given a primitive type T, returns the name of the object Type acting as
     * primitive boxing for T. For instance, Integer for int. If the type T is
     * not primitive, then it throws a runtime Exception.
     *
     * @param T A primitive type.
     * @return The type of the arrayWrapper whose elements are of type T.
     * @throws IllegalArgumentException if the method is called on a
     * non-primitive
     */
    static Type getPrimWrapperType(Type T) throws IllegalArgumentException {
        switch (T.getDescriptor()) {
            case "I":
                return Type.getObjectType("java/lang/Integer");
            case "B":
                return Type.getObjectType("java/lang/Byte");
            case "C":
                return Type.getObjectType("java/lang/Character");
            case "Z":
                return Type.getObjectType("java/lang/Boolean");
            case "D":
                return Type.getObjectType("java/lang/Double");
            case "F":
                return Type.getObjectType("java/lang/Float");
            case "S":
                return Type.getObjectType("java/lang/Short");
            case "J":
                return Type.getObjectType("java/lang/Long");
        }
        throw new IllegalArgumentException("getPrimWrapperType should be only called on primitives.");
    }

    /**
     * Given an opcode corresponding to a primitive type (that is, Opcodes.T_INT
     * for instance), returns the corresponding primitive type. If the opcode
     * does not corresponds to a primitive type, throws a new
     * {@link IllegalArgumentException}.
     *
     * @param opcode An opcode corresponding to a primitive type.
     * @return The primitive type associated with it.
     * @throws IllegalArgumentException if the opcode does not corresponds to a
     * primitive type.
     */
    static Type getPrimType(int opcode) throws IllegalArgumentException {
        switch (opcode) {
            case Opcodes.T_INT:
                return Type.INT_TYPE;
            case (Opcodes.T_BYTE):
                return Type.BYTE_TYPE;
            case (Opcodes.T_CHAR):
                return Type.CHAR_TYPE;
            case (Opcodes.T_BOOLEAN):
                return Type.BOOLEAN_TYPE;
            case (Opcodes.T_DOUBLE):
                return Type.DOUBLE_TYPE;
            case (Opcodes.T_FLOAT):
                return Type.FLOAT_TYPE;
            case (Opcodes.T_LONG):
                return Type.LONG_TYPE;
            case (Opcodes.T_SHORT):
                return Type.SHORT_TYPE;
            default:
                throw new IllegalArgumentException("opcode does not correspond to a Primitive Type");
        }
    }

    static boolean isNotProject(String internalName) {
        return isNotProject(internalName, Mill.getInstance().getAllClassReaders());
    }

    /**
     * This methods discriminates between those types corresponding to classes
     * that will be milled as part of the transformation, and those that won't.
     *
     * @param internalName The internal name of a class.
     * @return true if the class associated with T will not be milled
     */
    static boolean isNotProject(String internalName, Collection<ClassReader> col) {
        return isNotProject(Type.getObjectType(internalName), col);
    }

    static boolean isNotProject(Type T) {
        return isNotProject(T, Mill.getInstance().getAllClassReaders());
    }

    /**
     * This methods discriminates between those types corresponding to classes
     * that will be milled as part of the transformation, and those that won't.
     *
     * @param T A type.
     * @return true if the class associated with T will not be milled
     */
    static boolean isNotProject(Type T, Collection<ClassReader> project) {
            return !project
                    .stream()
                    .map(cr -> Type.getObjectType(cr.getClassName()))
                    .anyMatch(e->e.equals(T));
    }

    /**
     * Explore whether this Type is the type of a class that is not milled, or
     * the type of an array (maybe multidimensional) of elements whose types are
     * not milled.
     *
     * @param T A type
     * @return True if the class corresponding to T is not a milled, or if T is
     * the type of an array of element whose class is not milled.
     */
    static boolean isRecursivelyNotProject(Type T, Collection<ClassReader> col) {
        if (isArray(T)) {
            return isRecursivelyNotProject(Type.getType(T.getDescriptor().substring(1)), col);
        }
        if (isArray(T)) {
            Type subType = Type.getObjectType(T.getInternalName().substring(1));
            return isRecursivelyNotProject(subType, col);
        } else {
            return isNotProject(T, col);
        }
    }

    static boolean isRecursivelyNotProject(Type T) {
        return isRecursivelyNotProject(T, Mill.getInstance().getAllClassReaders());
    }

    /**
     * Predicate for whether the type T is the type of an array.
     *
     * @param T A type.
     * @return True if T is the type of an array, false otherwise.
     */
    static boolean isArray(Type T) {
        return T.getDescriptor().startsWith("[");
    }

    /**
     * Given the description of a method, returns an array containing both the
     * arguments types and its return type.
     *
     * @param desc Description of a method
     * @return Array of arguments and return types.
     */
    static Type[] getArgumentsAndReturnTypes(String desc) {
        List<Type> list = new ArrayList<>(Arrays.asList((Type[]) Type.getArgumentTypes(desc)));
        list.add(Type.getReturnType(desc));
        return list.toArray(new Type[0]);
    }

    /**
     * Takes the type of an arrayWrapper, and returns the type of the
     * corresponding array. Throws an exception if the type T is not the type of
     * an arrayWrapper. Note that for any array of reference, it will return the
     * type object[], and it will return int[] for intArrayWrapper, even if this
     * one might contain an array of the type int[][].
     *
     * @param T The type of an ArrayWrapper.
     * @return The type of the corresponding array.
     * @throws IllegalArgumentException if type T is not an array wrapper type
     */
    static Type unwrap(Type T) throws IllegalArgumentException {
        if (!isArrayWrapper(T)) {
            throw new IllegalArgumentException();
        } else {
            if (T.getInternalName().equals(PathConstants.BOOLEAN_ARRAY_WRAPPER_NAME)) {
                return Type.getType("[Z");
            }
            if (T.getInternalName().equals(PathConstants.BYTE_ARRAY_WRAPPER_NAME)) {
                return Type.getType("[B");
            }
            if (T.getInternalName().equals(PathConstants.CHAR_ARRAY_WRAPPER_NAME)) {
                return Type.getType("[C");
            }
            if (T.getInternalName().equals(PathConstants.DOUBLE_ARRAY_WRAPPER_NAME)) {
                return Type.getType("[D");
            }
            if (T.getInternalName().equals(PathConstants.FLOAT_ARRAY_WRAPPER_NAME)) {
                return Type.getType("[F");
            }
            if (T.getInternalName().equals(PathConstants.INT_ARRAY_WRAPPER_NAME)) {
                return Type.getType("[I");
            }
            if (T.getInternalName().equals(PathConstants.LONG_ARRAY_WRAPPER_NAME)) {
                return Type.getType("[J");
            }
            if (T.getInternalName().equals(PathConstants.SHORT_ARRAY_WRAPPER_NAME)) {
                return Type.getType("[S");
            } else if (!(T.getInternalName().equals(PathConstants.OBJECT_ARRAY_WRAPPER_NAME))) {
                throw new RuntimeException("unknown wrapper type");
            } else {
                return Type.getType("[Ljava/lang/Object;");
            }
        }
    }

    /**
     * Method to test whether a type is a subtype of another type. Might explore
     * the classFiles associated with T1, T2, so might throw a runtimeException
     * if the classFile is not found.
     *
     * @param T1 A type
     * @param T2 A second type.
     * @return True if T1 is a subtype of T2, that is, if an object of T1 might
     * be cast to T2
     */
    static boolean isSubType(Type T1, Type T2) throws NoSuchClassException {
        if (T1.equals(T2)) {
            return true;
        }
        if (T1.equals(Type.getObjectType("java/lang/Object")) || TypeUtil.isPrimitive(T1) || TypeUtil.isPrimitive(T2) || TypeUtil.isVoid(T1) || TypeUtil.isVoid(T2)) {
            return false;
        }
        if (TypeUtil.isArray(T1)) {
            if (T2.equals(Type.getObjectType("java/lang/Object"))) {
                return true;
            }
            if (TypeUtil.isArray(T2)) {
                return isSubType(T1.getDescriptor().substring(1), T2.getDescriptor().substring(1));
            }
            return false;
        }
        if (T2.equals(Type.getObjectType("java/lang/Object"))) {
            return true;
        }
        ClassReader cr = Mill.getInstance().getClassReader(T1.getClassName());
        for (String inter : cr.getInterfaces()) {
            if (isSubType(Type.getObjectType(inter), T2)) {
                return true;
            }
        }
        if (cr.getSuperName() != null) {
            if (isSubType(Type.getObjectType(cr.getSuperName()), T2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether two description of types describe type that are subtype to
     * one another. Note: work only on value types, not on method types.
     *
     * @param typeDesc1 The description of a type.
     * @param typeDesc2 The description of a type.
     * @return True if the type described by the first argument is a subtype of
     * the type described by the second argument.
     */
    static boolean isSubType(String typeDesc1, String typeDesc2) throws NoSuchClassException {
        return isSubType(Type.getType(typeDesc1), Type.getType(typeDesc2));
    }

    /**
     * Given a signature of a value type / method type, returns the associated
     * description by replacing every type variable by their bound.
     *
     * @param signature The signature of a field / method
     * @return The description of the method obtained by replacing every type
     * variable by their bound.
     */
    public static String fromSignatureToDesc(String signature) {
        SignatureReader sr = new SignatureReader(signature);
        SignatureWriter sw = new SignatureWriter() {
            final HashMap<String, String> fromFormalTypeToBound = new HashMap<>();
            final StringBuilder lastVisitedFormalType = new StringBuilder();

            class addVisitedBoundTypeToMap extends SignatureWriter {

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    fromFormalTypeToBound.put(lastVisitedFormalType.toString(), this.toString());
                }
            }

            @Override
            public void visitFormalTypeParameter(String name) {
                lastVisitedFormalType.setLength(0);
                lastVisitedFormalType.append(name);
            }

            @Override
            public SignatureVisitor visitInterfaceBound() {
                return new addVisitedBoundTypeToMap();
            }

            public SignatureVisitor visitClassBound() {
                return new addVisitedBoundTypeToMap();
            }

            @Override
            public void visitTypeVariable(String name) {
                if (fromFormalTypeToBound.containsKey(name)) {
                    String newName = fromFormalTypeToBound.get(name);
                    //Not ideal, since some bounds may be lost.
                    String newNameWithoutTypeVariable = fromSignatureToDesc(newName);
                    char[] toWrite = newNameWithoutTypeVariable.toCharArray();
                    for (char c : toWrite) {
                        super.visitBaseType(c);
                    }
                } else {
                    super.visitClassType("java/lang/Object");
                    super.visitEnd();
                }
            }
        };
        try {
            sr.accept(sw);
        }
        catch(Throwable t){
            throw new RuntimeException(signature, t);
        }
        return sw.toString();
    }

    /**
     * Given a method signature with some open type variables, close these type variables by declaring them
     * and bounding them to objects.
     * @param signature method signature
     * @return The closure of its signature.
     */

    static String closeMethodSignature(String signature){
        try {
            // First, we need to detect the open Type of the signature.
            Set<String> openTypeVariable = getOpenType(signature);
            Set<String> typeVariables = getTypeVariables(signature);
            SignatureWriter sw = new SignatureWriter();
            for (String openType : openTypeVariable) {
                if (isWrongClosedType(openType)){
                    String newName = getNewNameForOpenType(typeVariables);
                    typeVariables.add(newName);
                    signature =  renameType(signature, openType, newName);
                    openType = newName;
                }
                sw.visitFormalTypeParameter(openType);
                SignatureVisitor svv = sw.visitClassBound();
                svv.visitClassType("java/lang/Object");
                svv.visitEnd();
            }
            SignatureReader sr = new SignatureReader(signature);
            sr.accept(sw);
            return sw.toString();
        }
        catch(Throwable t){
            throw t;
        }
    }

    static String renameType(String signature, String formerTypeVariable, String newTypeVariable){
        SignatureReader sr = new SignatureReader(signature);
        SignatureWriter sw = new SignatureWriter(){

            @Override
            public void visitTypeVariable(String name) {
                if(name.equals(formerTypeVariable)){
                    super.visitTypeVariable(newTypeVariable);
                }
                else {
                    super.visitTypeVariable(name);
                }
            }
        };
        sr.accept(sw);
        return sw.toString();
    }

    static String[] possibleNameForType =  new String[]{"E", "G", "H", "K", "M", "N", "O", "P", "Q", "R", "U", "W", "X", "Y"};

    static String getNewNameForOpenType(Set<String> alreadyPresentType){
        int i = 0;
        while(true){
            String newTypeName = possibleNameForType[i];
            if (!alreadyPresentType.contains(newTypeName) && !isWrongClosedType(newTypeName)){
                return newTypeName;
            }
            i+=1;
        }
    }

    static boolean isWrongClosedType(String parameterType){
        return parameterType.length()==1 && forbiddenChar.contains(parameterType);
    }

    static String forbiddenChar="BCDFIJSVZTL";



    static Set<String> getOpenType(String signature){
        //We collect the closed types.
        Set<String> closedTypes = getClosedTypes(signature);
        Set<String> typeVariables = getTypeVariables(signature);
        typeVariables.removeAll(closedTypes);
        return typeVariables;
    }

    static Set<String> getClosedTypes(String signature){
        SignatureReader sr = new SignatureReader(signature);
        final Set<String> setClosedType = new HashSet<>();
        SignatureVisitor sv = new SignatureVisitor(Opcodes.ASM6) {
            @Override
            public void visitFormalTypeParameter(String name) {
                setClosedType.add(name);
                super.visitFormalTypeParameter(name);
            }
        };
        sr.accept(sv);
        return setClosedType;
    }

    static Set<String> getTypeVariables(String signature){
        SignatureReader sr = new SignatureReader(signature);
        final Set<String> setTypeVariable = new HashSet<>();
        SignatureVisitor sv = new SignatureVisitor(Opcodes.ASM6) {
            @Override
            public void visitTypeVariable(String name) {
                setTypeVariable.add(name);
                super.visitTypeVariable(name);
            }
        };
        sr.accept(sv);
        return setTypeVariable;
    }


    static final boolean isFromMokapot(Type T){
        if (TypeUtil.isPrimitive(T)){
            return false;
        }
        if (TypeUtil.isArray(T)){
            return false;
        }
        return isFromMokapot(T.getInternalName());
    }

    static final boolean isFromMokapot(String internalName){
        return internalName.startsWith("xyz/acygn/mokapot");
    }

    static final boolean isSendRemotely(MethodParameter mp){
        return (mp.className.startsWith("xyz/acygn/mokapot") && mp.methodName.equals("runRemotely"));
    }

    static final boolean isVoid(Type T){
        return T.equals(Type.VOID_TYPE);
    }

    static final  String getClassNameFromSignature(String signature){
        SignatureReader sr = new SignatureReader(TypeUtil.fromSignatureToDesc(signature));
        class SignatureReference{
            String ref  = null;
        }
        final SignatureReference signRef = new SignatureReference();
        sr.accept(new SignatureVisitor(Opcodes.ASM6){
            @Override
            public void visitClassType(String name) {
                if (signRef.ref==null){
                    signRef.ref = name;
                }
            }
        });
        return signRef.ref;
    }

}
