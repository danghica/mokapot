/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import xyz.acygn.millr.messages.*;

/**
 * @author thomas
 */
public class ArrayRewriteTransformation extends Transformation {

    /**
     * A methodParameter with impossible name / methodDescription / methodSignature that will be used to refer
     * to a method that needs to be created. This one will take n int parameter, and returns an integer array stocking
     * them. The number n will need to be specified.
     */
    Collection<ClassWriter> collectionClassWriter;


    public ArrayRewriteTransformation(SubProject sp) {
        super(sp);
        ClassDataBase.constructSetOfCompatibleMethod();
        collectionClassWriter = new HashSet<>();
    }

    @Override
    public Collection<ClassWriter> getClassWriter() {
        collectionClassWriter.addAll(super.getClassWriter());
        return collectionClassWriter;
    }

    @Override
    String getNameOfTheTransformation() {
        return "from array to arrayWrapper ";
    }

    @Override
    public Instance getNewInstance(ClassReader classReader) {
        return new ArrayRewriteTransformationInstance(classReader);
    }

    class ArrayRewriteTransformationInstance extends Instance {


        final ClassWriter cw;
        final ClassVisitor cv;
        Set<MethodParameter> doNotWrapMethods = new HashSet<>();

        ClassVisitor getClassTransformer() {
            return cv;
        }

        @Override
        ClassWriter getClassWriter() {
            return cw;
        }

        @Override
        ClassVisitor getClassVisitorChecker() {
            return new ArrayCompatibilityChecker(Opcodes.ASM6) {
            };
        }

        public ArrayRewriteTransformationInstance(ClassReader classReader) {
            super(classReader);
            ClassDataBase.ClassData cd = ClassDataBase.getClassData(cr.getClassName());
            Set<MethodParameter> setMethods = cd.getAllMethods();
            //We collect the set of methods that are present in this class that have same name
            //and same arguements types. That is, they only differ by their return types.
            //For each set of methods, only one of them must be "real", the others must be synthetic and generated
            //by type erasure.
            //   if (cr.getClassName().equals("org/gjt/sp/jedit/ActionSet")) {
            Set<Map.Entry<String, List<MethodParameter>>> setMethodCompatible = setMethods.stream().
                    collect(Collectors.groupingBy(e -> e.methodName + Arrays.toString(Type.getArgumentTypes(e.methodDesc))))
                    .entrySet().stream().filter(e -> e.getValue().size() > 1).collect(Collectors.toSet());
            //We want to make sure that once we replace their types with arrayWrapper, we do not end up with
            //methods that have same name and same types.

            for (Map.Entry<String, List<MethodParameter>> methodsWithSameDesc : setMethodCompatible) {
                Set<Map.Entry<String, List<MethodParameter>>> problematic =
                        methodsWithSameDesc.getValue().stream().collect(
                                Collectors.groupingBy(e -> SignatureArrayTypeModifier.getNewSignature(e.methodDesc))).entrySet();
                problematic.removeIf(e -> e.getValue().size() < 2);

                //For those, we wish to only wrap one of them, the one that is not synthetic,
                problematic.stream().map(e -> e.getValue()).flatMap(e -> e.stream()).
                        filter(e -> (e.methodAccess & Opcodes.ACC_SYNTHETIC) != 0).forEach(doNotWrapMethods::add);
            }
            //    }
            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cv = new ArrayRewriteVisitor(Opcodes.ASM6, cw, cr.getClassName());
        }


        @Override
        void classTransformerToClassWriter() {

        }


        private class ArrayCompatibilityChecker extends ClassVisitor {

            String className;

            public ArrayCompatibilityChecker(int api) {
                super(api);
            }

            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                if (signature.contains("[")) {
                    //     System.out.println("class " + name + "incompatible since its signature "+ signature + "contains arrays");
                }
                className = name;
                if (superName != null) {
                    // Run the same algorithm on the superClasss
                }
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                if (desc.contains("[")) {
                    printIncompatibilityMessage("Field array" + name + "whose description is " + desc);
                } else if (signature != null) {
                    if (signature.contains("[")) {
                        printIncompatibilityMessage("Field array" + name + "whose signature is" + desc);
                    }
                }
                return new FieldVisitor(Opcodes.ASM5) {
                };
            }

            // Even though annotation may contains arrays, one cannot create a annotation object and pass it around, so they should alter the behaviour of mokapot.
        }

        public class ArrayRewriteVisitor extends ClassVisitor {

            private static final String arrayWrapperDesc = "Lxyz/acygn/millr/util/ObjectArrayWrapper;";
            private static final String arrayWrapperName = "xyz/acygn/millr/util/ObjectArrayWrapper";
            private Set<MethodParameter> wrappingMethodToBeCreated;
            private Set<Integer> intToArrayMethodToBeCreated;
            private boolean gotMain = false;
            private String nameClass;
            private boolean isInterface;


            private String getNewSignature(String signature) {
                return SignatureArrayTypeModifier.getNewSignature(signature);
            }

            @Deprecated
            private String getNewFieldSignature(String signature) {
                return SignatureArrayTypeModifier.getNewSignatureField(signature);
            }

            public ArrayRewriteVisitor(int i, ClassVisitor cv, String name) {
                super(i, cv);
                this.nameClass = name;
                wrappingMethodToBeCreated = new HashSet<>();
                intToArrayMethodToBeCreated = new HashSet<>();
            }

            /**
             * Overridden version of the original visitMethod. If the method
             * visited is the "main" method, then this one will be renamed
             * mainMillr. The main method will remain, but will simply consists
             * of wrapping the string[] arguments into an arrayWrapper, and
             * passing it to mainMillr. If the method is not a "special method"
             * then the name of the method will be changed to the result of the
             * method getMethodName, and the original method will remain with
             * original types, and will simply wrap the arrays input (if needed)
             * and call the milled method, unwrap its output if needed, and
             * return it. If the method is abstract, then we create also a
             * method getMethodName() with appropriate types, and both the
             * original method and the new method created remain abstract.
             *
             * @param access     acces of the method
             * @param name       name of the method
             * @param desc       description of the method
             * @param signature  signature of the method
             * @param exceptions Exceptions thrown by the method
             * @return The methodVisitor visiting the method.
             */
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                //The signature, description with arrayWrappers.
                String newSignature;
                String newDesc;
                MethodParameter mp = MethodParameter.getMethod(nameClass, name, desc);
                try {
                    if (name.equals("main") && desc.equals("[Ljava/lang/String;") && (access & Opcodes.ACC_STATIC) != 0) {
                        gotMain = true;
                        MethodVisitor mww = super.visitMethod(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, getMethodName(mp), "(" + arrayWrapperDesc + ")V", null, exceptions);
                        MethodVisitor mw = new ArrayRewriteMethodAdapter(nameClass, Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, getMethodName(mp), desc, mww, wrappingMethodToBeCreated, intToArrayMethodToBeCreated);
                        return mw;
                    } else {
                        boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
                        if (!isAbstract && doesMethodNeedsTwoVersion(desc) && !isMethodNameFixed(mp)) {
                            unWrapMethod(access, name, desc, signature, exceptions, isInterface);
                        } else if (isAbstract && doesMethodNeedsTwoVersion(desc)) {
                            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                            mv.visitEnd();
                        }
                        if (doNotWrapMethods.contains(mp)) {
                            return null;
                        }
                        newSignature = signature != null ? SignatureArrayTypeModifier.getNewSignature(signature) : null;
                        newDesc = SignatureArrayTypeModifier.getNewSignature(desc);
                        access = access & ~Opcodes.ACC_VARARGS;
                        return new ArrayRewriteMethodAdapter(nameClass, access, name, desc, super.visitMethod(access, getMethodName(mp), newDesc, newSignature, exceptions), wrappingMethodToBeCreated, intToArrayMethodToBeCreated);
                    }
                } catch (Throwable tr) {
                    throw new MethodTransformationFailedException(tr, nameClass, name, desc);
                }
            }

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, getNewSignature(signature), superName, interfaces);
                this.nameClass = name;
                this.isInterface = ((access & Opcodes.ACC_INTERFACE) != 0);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return super.visitAnnotation(getNewSignature(desc), visible);
            }

            /**
             * Overriden version of visitField. We change the field we visit
             * from arrays to arrayWrappper if needed, but for the case where it
             * is the values field from ENUM, that we let unchanged.
             *
             * @param access    access for the field
             * @param name      name of the field
             * @param desc      description of the field
             * @param signature signature of the field
             * @param value     ?
             * @return A field Visitor as implemented by the super method.
             */
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                try {
                    if (VisibilityTools.isValueField(nameClass, name, desc)) {
                        return super.visitField(access, name, desc, signature, value);
                    } else {
                        String newName = ObjectMillrNamingUtil.getNewNameString(FieldParameter.getFieldParameter(nameClass, name, desc));
                        return new ArrayRewriteFieldVisitor(api, super.visitField(access, newName, getNewSignature(desc), getNewSignature(signature), value));
                    }
                } catch (NoSuchClassException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void visitOuterClass(String owner, String name, String desc) {
                //The outer class must be part of the project as well. So we can safely replace its signature.
                super.visitOuterClass(owner, name, getNewSignature(desc));
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int Typeref, TypePath typePath, String desc, boolean visible) {
                return super.visitTypeAnnotation(Typeref, typePath, getNewSignature(desc), visible);
            }

            /**
             * Overridden version of visitEnd, that will create the main method
             * (that wraps the mainMillr one), if a main method was present in
             * the code, and will furthermore create all the necessary wrapper
             * methods for the methods that were collected in the field set
             * methodToBeCreated.
             */
            @Override
            public void visitEnd() {
                if (gotMain) {
                    // Create new main
                    MethodParameter mp = MethodParameter.getMethod(nameClass, "main", "([Ljava/lang/String)V");
                    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_VARARGS, "main", "([Ljava/lang/String;)V", null, null);
                    mv.visitCode();
                    mv.visitIntInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, arrayWrapperName, "getObjectArrayWrapper", "([Ljava/lang/Object;)" + arrayWrapperDesc, false);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, this.nameClass, getMethodName(mp), "(" + arrayWrapperDesc + ")V", false);
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(30, 30);
                    mv.visitEnd();
                }
                for (MethodParameter mp : wrappingMethodToBeCreated) {
                    try {
                        wrapMethodCode(mp);
                    } catch (NoSuchClassException ex) {
                        throw new MethodTransformationFailedException(ex, nameClass, mp.methodName, mp.methodDesc);
                    }
                }
                for (Integer integer : intToArrayMethodToBeCreated) {
                    getMethodIntArgsToArray(integer);
                }
            }


            /**
             * Create a wrapper to wrap the arguments types of a method. The
             * method considered is one not part of the project being milled.
             * The Wrapper method created will have the same input types as the
             * method it wrapped, except with arraysWrapper instead of arrays.
             * Furthermore, its return type will always be Object[]. This is
             * because the role of the wrapper method is simply to unwrap the
             * arguments, but the code to the method shall remain where it
             * originally was, due to the fact that it might be a constructor.
             *
             * @param method
             */
            private void wrapMethodCode(final MethodParameter method) {
                /*
        The signature and descriptor (§4.3.3) of a given method or constructor may not correspond exactly, due to compiler-generated artifacts.
        In particular, the number of TypeSignatures that encode formal arguments in MethodTypeSignature may be less than the number of ParameterDescriptors
        in MethodDescriptor.
                 */
                if (method.methodSignature != null && (Type.getArgumentTypes(method.methodDesc).length != MethodSignatureParser.getArguements(method.methodSignature).length)) {
                    if (method.methodSignature.contains("[")) {
                        //To Fill
                        MessageUtil.error(new TransformationMayFailException(this.nameClass, "We need to wrap a call to the methodAPI " + method.className + " ; " + method.methodName + "\n"
                                + " but this one has signature and description of different lengths."));
                    }
                    //Otherwise, we just forget about the signature
                    method.methodSignature = null;
                }

                /* There is three signature - descriptions we need to consider.
                First, the arguement with which the method we create is called, and the description - signature of the mehtods.
               We called these methodDescription, methodSignature, arguementsSignature, arguementsType.
                Second, the ones we are going to pass the the API methods, we called these unwrapMethodSignature, unwrapMethodDescription....
                Third, we consider the description-signature of the API methods. There might me a mismatch between the second and the third in the case
                where the method is signaturePolymorphic. Then the last arguement might be an array, but we might pass several objects instead.
                 */
                final boolean hasSignature = (method.methodSignature != null);

                if (hasSignature){
                    method.methodSignature = TypeUtil.closeMethodSignature(method.methodSignature);
                }

                // The method arguement signature, in ArrayWrapper space.
                String[] arguementSignatures;
                // The arguements types, who lives in ArrayWrapper-space
                Type[] ArguementTypes;
                // The description of the method, as it will be called. Lives in ArrayWrapper space.
                String methodDescription;
                // The signature of this method.
                StringBuilder MethodSignature = new StringBuilder();

                // The types of the arguements we are going to return.
                Type[] unwrapArguementTypes;
                String[] unwrapArguementSignature;

                // the types of the arguements of the API Function
                Type[] APIArguementTypes;
                // The signature of the arguements of the API.
                String[] APIArguementsSignatures;

                //The Java Virtual Machine gives special treatment to signature polymorphic methods in the invokevirtual instruction(§invokevirtual), in order to effect invocation of a method handle.A method handle is a typed,
                // directly executable reference to an underlying method, constructor, field, or similar low-level opera
                //The only methods that are signaturePolymorphic in the current jvm have empty signature.
                boolean isSignaturePolymorphinc = VisibilityTools.isSignaturePolymorphic(method);
                boolean isVarargs = (Opcodes.ACC_VARARGS & method.methodAccess) != 0;
                // if the arguements we call the methods with are not passed as arguement to this method, we guess them by
                // looking at the description of the API method we are dealing with. Note that this procedure fails if
                // we are dealing with VARARGS

                //         System.out.println("methodDEscCalled " + method.methodDescCalled);
                if (method.methodDescCalled == null) {
                    if (isSignaturePolymorphinc) {
                        throw new UnsupportedOperationException("Trying to wrap arguements of a method that is SignaturePolymorphic, without precising the types of the arguements \n"
                                + method.toString());
                    }
                    ArguementTypes = Type.getArgumentTypes(SignatureArrayTypeModifier.getNewSignature(method.methodDesc));
                    StringBuilder newMethodDesc = new StringBuilder("(");
                    Arrays.stream(ArguementTypes).forEach(e -> newMethodDesc.append(e.getDescriptor()));
                    newMethodDesc.append(")[Ljava/lang/Object;");
                    methodDescription = newMethodDesc.toString();
                } else {
                    // Normally, the methodDescCalled already lives on the "ArrayWrapper"-space, so the getNewSignature shall let it invariant.
                    if (!Arrays.deepEquals(Type.getArgumentTypes(SignatureArrayTypeModifier.getNewSignature(method.methodDescCalled)),
                            Type.getArgumentTypes(method.methodDescCalled))) {
                        throw new RuntimeException("We are trying to unwrap arguements of a method, but the unwrap method is not called with "
                                + "arraWrapper but arrays instead \n"
                                + "description of the unwrap method " + method.methodDescCalled + " \n"
                                + "description expected " + SignatureArrayTypeModifier.getNewSignature(method.methodDescCalled) + "\n"
                                + method.toString());
                    }
                    ArguementTypes = Type.getArgumentTypes(method.methodDescCalled);
                    methodDescription = method.methodDescCalled;
                }
                if (hasSignature) {
                    APIArguementsSignatures = MethodSignatureParser.getArguements(method.methodSignature);
                    arguementSignatures = new String[0];
                    unwrapArguementSignature = new String[0];
                    if (!isSignaturePolymorphinc) {
                        unwrapArguementSignature = APIArguementsSignatures;
                        arguementSignatures = MethodSignatureParser.getArguements(SignatureArrayTypeModifier.getNewSignature(
                                method.methodSignature));
                        MethodSignature.append(MethodSignatureParser.getTypeParameterPart(method.methodSignature));
                        MethodSignature.append("(");
                        Arrays.stream(arguementSignatures).forEach(e -> MethodSignature.append(e));
                        MethodSignature.append(")[Ljava/lang/Object;");
                    }
                    if (isSignaturePolymorphinc) {
                        throw new UnsupportedOperationException("trying to figure out the signature of a signaturePolymorphic method \n" + method.toString());
                    }
                } else {
                    arguementSignatures = new String[ArguementTypes.length];
                    APIArguementsSignatures = new String[ArguementTypes.length];
                    unwrapArguementSignature = new String[ArguementTypes.length];

                }

                APIArguementTypes = Type.getArgumentTypes(method.methodDesc);
                if (!isSignaturePolymorphinc) {
                    unwrapArguementTypes = APIArguementTypes;
                } else {
                    if (method.methodDescCalled == null) {
                        throw new UnsupportedOperationException("calling a signature Polymorphic methods without precising the types with which it is called");
                    }
                    arguementSignatures = new String[ArguementTypes.length];
                    unwrapArguementSignature = new String[ArguementTypes.length];
                    unwrapArguementTypes = new Type[ArguementTypes.length];
                    for (int i = 0; i < Type.getArgumentTypes(method.methodDesc).length - 2; i++) {
                        unwrapArguementTypes[i] = Type.getArgumentTypes(method.methodDesc)[i];
                        //        arguementSignatures[i] = (hasSignature)? APIArguementsSignatures[i] : null;
                    }
                    if (ArguementTypes.length == Type.getArgumentTypes(method.methodDesc).length) {
                        unwrapArguementTypes[ArguementTypes.length - 1] = Type.getArgumentTypes(method.methodDesc)[ArguementTypes.length - 1];
                        arguementSignatures[ArguementTypes.length - 1] = (hasSignature) ? APIArguementsSignatures[ArguementTypes.length - 1] : null;
                    } else if (ArguementTypes.length > Type.getArgumentTypes(method.methodDesc).length) {
                        Type varargType = Type.getType(Type.getArgumentTypes(method.methodDesc)[Type.getArgumentTypes(method.methodDesc).length - 1].getDescriptor().substring(1));
                        String varargSignature = (hasSignature) ? APIArguementsSignatures[ArguementTypes.length - 1] : null;
                        for (int i = Type.getArgumentTypes(method.methodDesc).length - 1; i < ArguementTypes.length; i++) {
                            unwrapArguementTypes[i] = varargType;
                            arguementSignatures[i] = varargSignature;
                        }
                    }
                }
                int acVarargs = isVarargs ? Opcodes.ACC_VARARGS : 0;


                MethodVisitor mv = super.visitMethod(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, getWrappingMethodName(method), methodDescription, hasSignature ? MethodSignature.toString() : null, null);
                mv.visitCode();
                // As the method is static the first arguement is loaded in 0, and in 1 if the method is non static.
                int length = ArguementTypes.length;
                int[] indexOfInputArguements = new int[length];
                for (int i = 0; i < length; i++) {
                    if (i == 0) {
                        indexOfInputArguements[i] = 0;
                    } else {
                        indexOfInputArguements[i] = indexOfInputArguements[i - 1] + ArguementTypes[i - 1].getSize();
                    }
                }
                final int slotAfterInputs = (length == 0) ? 0 : indexOfInputArguements[length - 1] + ArguementTypes[length - 1].getSize();
                List<Integer> indexOfNewInputArguements = new ArrayList<>();
                List<Integer> indexOfArrayWrapper = new ArrayList();
                List<Integer> indexOfArrays = new ArrayList<>();
                List<Integer> indexOfInterfaceWrappers = new ArrayList<>();
                int indexNewLocal = 0;
                for (int index = 0; index < ArguementTypes.length; index++) {
                    if (TypeUtil.isArrayWrapper(ArguementTypes[index])) {
                        indexOfArrayWrapper.add(indexOfInputArguements[index]);
                        mv.visitVarInsn(Opcodes.ALOAD, indexOfInputArguements[index]);
                        ArrayRewriteMethodAdapter.unwrap(ArguementTypes[index].getDescriptor(), unwrapArguementTypes[index].getInternalName(), mv);
                        mv.visitVarInsn(Opcodes.ASTORE, slotAfterInputs + indexNewLocal);
                        indexOfNewInputArguements.add(slotAfterInputs + indexNewLocal);
                        indexOfArrays.add(slotAfterInputs + indexNewLocal);
                        indexNewLocal += 1;
                        // THE CASES SHOULD NOT BE EXCLUSIVE. TO BE MODIFIED
                    } else if (ArguementTypes[index].getClassName().equals("java.lang.Object")) {
                        indexOfArrayWrapper.add(indexOfInputArguements[index]);
                        mv.visitVarInsn(Opcodes.ALOAD, indexOfInputArguements[index]);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "xyz/acygn/millr/util/RuntimeUnwrapper", "unwrap", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                        mv.visitVarInsn(Opcodes.ASTORE, slotAfterInputs + indexNewLocal);
                        indexOfNewInputArguements.add(slotAfterInputs + indexNewLocal);
                        indexOfArrays.add(slotAfterInputs + indexNewLocal);
                        indexNewLocal += 1;
                    } else if (hasSignature && arguementSignatures[index].contains("ArrayWrapper")) {
                        ArrayInterfaceWrapUnwrapTransformation AIWUT = new ArrayInterfaceWrapUnwrapTransformation(ArguementTypes[index].getInternalName(), arguementSignatures[index], unwrapArguementSignature[index]);
                        String nameClassInterface = AIWUT.getNameConvertingClass();
                        collectionClassWriter.add(AIWUT.getClassWriter());
                        Path PathOriginalClassReader = sp.fromNameClassToPath.get(nameClass);
                        File baseFolder = SubProject.getBaseFolder(new File(PathOriginalClassReader.toString()), nameClass);
                        sp.fromNameClassToPath.put(nameClassInterface, new File(baseFolder, nameClassInterface + ".class").toPath());
                        Type typeClass = Type.getObjectType(nameClassInterface);
                        mv.visitTypeInsn(Opcodes.NEW, typeClass.getInternalName());
                        mv.visitInsn(Opcodes.DUP);
                        mv.visitVarInsn(Opcodes.ALOAD, indexOfInputArguements[index]);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, typeClass.getInternalName(), "<init>", "(" + ArguementTypes[index].getDescriptor() + ")V", false);
                        mv.visitVarInsn(Opcodes.ASTORE, slotAfterInputs + indexNewLocal);
                        indexOfNewInputArguements.add(slotAfterInputs + indexNewLocal);
                        indexOfInterfaceWrappers.add(slotAfterInputs + indexNewLocal);
                        indexNewLocal += 1;
                    } else {
                        indexOfNewInputArguements.add(indexOfInputArguements[index]);
                    }
                }
                mv.visitIntInsn(Opcodes.BIPUSH, length + 2 * indexOfArrayWrapper.size() + indexOfInterfaceWrappers.size());
                mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                int index = 0;
                for (Integer indexInput : indexOfNewInputArguements) {
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitIntInsn(Opcodes.BIPUSH, index);
                    if (TypeUtil.isPrimitive(ArguementTypes[index])) {
                        mv.visitTypeInsn(Opcodes.NEW, TypeUtil.getPrimWrapperType(ArguementTypes[index]).getInternalName());
                        mv.visitInsn(Opcodes.DUP);
                        mv.visitIntInsn(ArguementTypes[index].getOpcode(Opcodes.ILOAD), indexInput.intValue());
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, TypeUtil.getPrimWrapperType(ArguementTypes[index]).getInternalName(), "<init>", "(" + ArguementTypes[index].getDescriptor() + ")V", false);
                    } else {
                        mv.visitVarInsn(ArguementTypes[index].getOpcode(Opcodes.ILOAD), indexInput.intValue());
                    }
                    mv.visitInsn(Opcodes.AASTORE);
                    index += 1;
                }
                for (int i = 0; i < indexOfArrayWrapper.size(); i++) {
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitIntInsn(Opcodes.BIPUSH, indexOfNewInputArguements.size() + 2 * i);
                    // We push the ArrayWrapper first.
                    mv.visitIntInsn(Opcodes.ALOAD, indexOfArrayWrapper.get(i));
                    mv.visitInsn(Opcodes.AASTORE);
                    // We then push the Array.
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitIntInsn(Opcodes.BIPUSH, indexOfNewInputArguements.size() + 2 * i + 1);
                    mv.visitIntInsn(Opcodes.ALOAD, indexOfArrays.get(i));
                    mv.visitInsn(Opcodes.AASTORE);

                }
                for (int i = 0; i < indexOfInterfaceWrappers.size(); i++) {
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitIntInsn(Opcodes.BIPUSH, indexOfNewInputArguements.size() + 2 * indexOfArrayWrapper.size() + i);
                    mv.visitIntInsn(Opcodes.ALOAD, indexOfInterfaceWrappers.get(i));
                    mv.visitInsn(Opcodes.AASTORE);
                }
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(30, 30);
                mv.visitEnd();
            }

            /**
             * Create a method with arrays type that will redirect its argument
             * to the same method with arrayWrapper types. The name of the
             * method with ArrayWrapper types will be got through the method
             * getMethodName(). The method created will have access, name, desc,
             * signature and Exceptions as passed as arguments. That is, those
             * must not contain arrayWrapper.
             *
             * @param access      access integer of the method
             * @param name        name of the method
             * @param desc        description of the method
             * @param signature   signature of the method
             * @param Exceptions  Exceptions of the method.
             * @param isInterface true if the class that we are milling is an
             *                    interface, false otw.
             */
            private void unWrapMethod(int access, String name, String desc, String signature, String[] Exceptions, boolean isInterface) {
                access = access & ~Opcodes.ACC_SYNCHRONIZED;
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, Exceptions);
                MethodParameter mp = MethodParameter.getMethod(nameClass, name, desc);
                Type[] methodInputTypes = Type.getArgumentTypes(desc);
                boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
                if (!isStatic) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                }
                int locationLocal = isStatic ? 0 : 1;
                for (Type T : methodInputTypes) {
                    if (T.getDescriptor().startsWith("[")) {
                        mv.visitIntInsn(T.getOpcode(Opcodes.ILOAD), locationLocal);
                        ArrayRewriteMethodAdapter.wrap(T.getDescriptor(), mv);
                    } else if (T.getDescriptor().equals("Ljava/lang/Object;")) {
                        mv.visitIntInsn(T.getOpcode(Opcodes.ILOAD), locationLocal);
                        ArrayRewriteMethodAdapter.runtimeWrap(mv);
                    } else {
                        mv.visitVarInsn(T.getOpcode(Opcodes.ILOAD), locationLocal);
                    }
                    locationLocal += T.getSize();
                }

                if ((access & Opcodes.ACC_STATIC) != 0) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, nameClass, getMethodName(mp), SignatureArrayTypeModifier.getNewSignature(desc), false);
                } else if (isInterface) {
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, nameClass, getMethodName(mp), SignatureArrayTypeModifier.getNewSignature(desc), true);

                } else if ((access & Opcodes.ACC_PRIVATE) == 0) {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, nameClass, getMethodName(mp), SignatureArrayTypeModifier.getNewSignature(desc), false);
                } else {
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, nameClass, getMethodName(mp), SignatureArrayTypeModifier.getNewSignature(desc), false);
                }
                if (Type.getReturnType(desc).getDescriptor().startsWith("[")) {
                    ArrayRewriteMethodAdapter.unwrap(SignatureArrayTypeModifier.getNewSignature(Type.getReturnType(desc).getDescriptor()), Type.getReturnType(desc).getDescriptor(), mv);
                } else if (Type.getReturnType(desc).equals(Type.getObjectType("java/lang/Object"))) {
                    ArrayRewriteMethodAdapter.runtimeUnwrap(mv);
                }
                mv.visitInsn(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN));
                mv.visitMaxs(30, 30);
                mv.visitEnd();
            }

            public void getMethodIntArgsToArray(int numberOfArgs) {
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, getMethodNameArrayToInt(numberOfArgs),
                        getMethodDescArrayToInt(numberOfArgs), null, null);
                ArrayRewriteMethodAdapter.pushIntegerIntoStack(numberOfArgs, mv);
                mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
                for (int i = 0; i < numberOfArgs; i++) {
                    mv.visitInsn(Opcodes.DUP);
                    ArrayRewriteMethodAdapter.pushIntegerIntoStack(i, mv);
                    mv.visitVarInsn(Opcodes.ILOAD, i);
                    mv.visitInsn(Opcodes.IASTORE);
                }
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(4, numberOfArgs);
                mv.visitEnd();
            }
        }
    }

    /**
     * Given an API method, returns the name of the method wrapping it.
     *
     * @param method API method
     * @return the name of the method taking same as arguments, and uwrapping
     * them.
     */
    public static String getWrappingMethodName(MethodParameter method) {
        synchronized (getMethodName) {
            //      if (method.className.startsWith("[")) {
            if (getMethodName.containsKey(method)) {
                return getMethodName.get(method);
            } else {
                String newName = InputOutput.getInstance().generateNewMethodName();
                getMethodName.put(method, newName);
                return newName;
            }
        }
    }
//
//        }
//        if (methodName.equals("<init>")) {
//            return "millr" + className.replace("/", "_") + "init";
//        } else if (methodName.endsWith("<clinit>")) {
//            return "millr" + className.replace("/", "_") + "clinit";
//        } else {
//            return "millr" + className.replace("/", "_") + methodName;
//        }
    //  }

    /**
     * Given an API method, returns the name of the method wrapping it.
     *
     * @param className The name of the class the method belongs to.
     * @param methodName The name of the API method the wrapper is designed for.
     * @return The name of the method unwrapping the arguments for this methods.
     */

    private static Map<MethodParameter, String> getMethodName = new HashMap<>();


    /**
     * Predicate that specifies those methods that are special for the jvm, and
     * whose names cannot be changed. Those are the object initializer method
     * init, and the class initializer clinit, and the values method for enum.
     *
     * @param mp The methodParameter
     * @return if the method is special to the jvm and hence its name cannot be changed.
     */
    public static boolean isMethodNameFixed(MethodParameter mp) {
        return mp.methodName.equals("<init>") || mp.methodName.equals("<clinit>");
        // || VisibilityTools.isValueFromEnum(mp);
    }

    /**
     * Given the name of a method, returns the name of the milled one.
     *
     * @param mp A methodParameter.
     * @return The name of the new method.
     */
    public static String getMethodName(MethodParameter mp) {
//        if (TypeUtil.isSendRemotely(mp)) {
//            return mp.methodName;
//        } else {
            return getMethodNameInternal(mp);
 //       }
    }

    /**
     * Given a method that is part of the project, returns the name of the milled one.
     *
     * @param mp The name of the original method
     * @return The name of the new method.
     */
    @Deprecated
    private static String getMethodNameInternal(MethodParameter mp) {
        if (TypeUtil.isNotProject(mp.className)) {
            throw new RuntimeException("This method should not be called on a non-Project method");
        }
        if (isMethodNameFixed(mp)) {
            return mp.methodName;
        }
        if (doesMethodNeedsTwoVersion(mp.methodDesc)) {
            /**
             * Since two methods that had same name, but different descriptions may now be mapped to methods
             * with same description, we need to be careful to avoid that they have same name.
             */
            return ObjectMillrNamingUtil.getMillrMethodName(mp);
        } else {
            return mp.methodName;
        }
    }


    public static String getMethodNameArrayToInt(int numberOfArgs) {
        return "_millr_ getInt" + String.valueOf(numberOfArgs) + "Array";
    }

    public static String getMethodDescArrayToInt(int numberOfArgs) {
        StringBuilder descBuilder = new StringBuilder("(");
        for (int i = 0; i < numberOfArgs; i++) {
            descBuilder.append("I");
        }
        descBuilder.append(")[I");
        return descBuilder.toString();
    }


    /**
     * Return true if we keep the original name and original description for the method, but create a new name for the milled method.
     *
     * @param desc The description of the method.
     * @return A boolean as specified above.
     */
    private static boolean doesMethodNeedsTwoVersion(String desc) {
        return (desc.contains("[") || desc.contains("Ljava/lang/Object;"));
    }

    public class ArrayRewriteFieldVisitor extends FieldVisitor {

        public ArrayRewriteFieldVisitor(int api) {
            super(api);
        }

        public ArrayRewriteFieldVisitor(int api, FieldVisitor fv) {
            super(api, fv);
        }

        private String getNewSignature(String signature) {
            return SignatureArrayTypeModifier.getNewSignature(signature);
        }

        private String getNewSignatureField(String signature) {
            return SignatureArrayTypeModifier.getNewSignatureField(signature);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return fv.visitAnnotation(getNewSignature(desc), visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return fv.visitTypeAnnotation(typeRef, typePath, getNewSignatureField(desc), visible);
        }

    }

}
