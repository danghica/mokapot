 /*
  * To change this license header, choose License Headers in Project Properties.
  * To change this template file, choose Tools | Templates
  * and open the template in the edito */
 package xyz.acygn.millr;

 import java.io.IOException;
 import java.lang.reflect.Field;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;
 import java.util.Set;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.stream.Collectors;

 import org.objectweb.asm.Handle;
 import org.objectweb.asm.Label;
 import org.objectweb.asm.AnnotationVisitor;
 import org.objectweb.asm.MethodVisitor;
 import org.objectweb.asm.Opcodes;
 import org.objectweb.asm.Type;
 import org.objectweb.asm.TypePath;
 import org.objectweb.asm.commons.AnalyzerAdapter;
 import xyz.acygn.millr.messages.MessageUtil;

 /**
  * @author thomas
  * <p>
  * This class rewrites the body of the method by replacing creations of arrays,
  * accesses to arrays by their counterpart in terms of arrayWrapper.
  * Furthermore, whenever an array is created, it automatically calls
  * ArrayClassCreator in order to generate the appropriate arrayWrapper class at
  * runtime. AnalyzerAdapter extends MethodVisitor while mimicking the effect of
  * the bytecode instructions on the stack. This allows us, by looking at the
  * stack, to determine the type of the array that we replace and call the
  * appropriate class.
  */
 public class ArrayRewriteMethodAdapter extends AnalyzerAdapter {


     /**
      * The owner here is to be interpreted as internal name of a class. If
      * returns the same class if the class is not an array class, otherwise it
      * returns the corresponding array wrapper class if the owner is an array.
      *
      * @param owner Internal name of a class
      * @return The corresponding arrayWrapper name if the owner is an array,
      * otherwise acts as identity.
      */
     private static String getNewOwner(String owner) {
         Type T = Type.getObjectType(owner);
         String newDesc = SignatureArrayTypeModifier.getNewSignature(T.getDescriptor());
         return Type.getType(newDesc).getInternalName();
     }

     private static final String ARRAYWRAPPERDESC = "Lxyz/acygn/millr/util/ObjectArrayWrapper;";
     private static final String ARRAYWRAPPERNAME = "xyz/acygn/millr/util/ObjectArrayWrapper";
     private static final String OBJECTDESC = "Ljava/lang/Object;";
     private static final String getObjectArray = "getObjectArrayWrapper";

     /*
     This Class is simply extends Analyzer by adding two abilities. The one to
     add an object to the stack, and to remove one.
      */
     class ExtendedAnalyzerAdapter extends AnalyzerAdapter {

         public ExtendedAnalyzerAdapter(String owner, int access, String name, String desc, MethodVisitor mv) {
             super(Opcodes.ASM5, owner, access, name, desc, mv);
         }

         public List<Object> getStack() {
             return stack;
         }

         public List<Object> getLocal() {
             return locals;
         }

         void addStack(Object o) {
             stack.add(o);
         }

         void removeLastStack() {
             stack.remove(stack.size() - 1);
         }

     }

     // The originalAnalyzer will track the stack / store of the original method as we progress through it.
     private ExtendedAnalyzerAdapter originalAnalyzer;

     /**
      * Return description with Arrays replaced as arrayWrapper. If returns the
      * same description if the description does not contain arrays, otherwise it
      * returns the corresponding description with arrayWrapper instead of
      * arrays.
      *
      * @param desc Description of either a method, class, field.
      * @return The corresponding description with arrayWrapper instead of
      * arrays.
      */
     public static String getNewDesc(String desc) {
         return SignatureArrayTypeModifier.getNewSignature(desc);
     }

     /**
      * Return the arrayWrapper class corresponding to an array of primitive
      * types.
      *
      * @param T The primitive type
      * @return The arrayWrapper corresponding to an array of T.
      */
     private static Type getPrimArrayType(Type T) {
         return Type.getObjectType("xyz/acygn/millr/util/" + T.getClassName() + "ArrayWrapper");
     }

     /**
      * Return the class Name of the arrayWrapper class corresponding to an array
      * of primitive types.
      *
      * @param T The primitive type
      * @return The arrayWrapper corresponding to an array of T.
      */
     private static String getPrimClassName(Type T) {
         return getPrimArrayType(T).getInternalName();
     }

     @Deprecated
     private static String getNewSignatureField(String signature) {
         return SignatureArrayTypeModifier.getNewSignatureField(signature);
     }

     /*
     By visiting the method, we might need some method calls to the API that need to be wrapped / unwrapped.
     For that, we will rely on wrapper methods that will be later created.
     We collect the set of method to be created in the field methodToBeCreated.
     The Integer collects how is the method called.
      */
     private Set<MethodParameter> methodToBeCreated;

     private Set<Integer>  methodIntToArray;

     //This is the name of the class the method we visit belongs to.
     private final String ownerMethod;

     //This is the name of the method.
     private final String nameMethod;

     //This is the description of the method.
     private final String descMethod;

     //This field encodes if the class we visit is an interface or a class.
     private boolean isInterface;

     //This field encode wether the method is static or not.
     private boolean isStatic;


     /**
      * Create the AnalyzerAdapter, extends the AnalyzerAdapter standard
      * constructor by adding a set @param MethodToBeCreated, that will used to
      * record the wrapper-unwrapper method that needs to be created in order to
      * make this method work.
      *
      * @param owner             The name of the class owning the method.
      * @param access            The access.
      * @param name              The name of the method.
      * @param desc              The description of the method.
      * @param mv                The MethodVisitor calls can be forwarded to. Can be null but
      *                          then won't produce anything.
      * @param methodToBeCreated Set in which method to be created will be
      *                          recorded. Needs to be non null.
      */
     public ArrayRewriteMethodAdapter(String owner, int access, String name, String desc, MethodVisitor mv,
                                      Set<MethodParameter> methodToBeCreated, Set<Integer> methodIntToArray) {
         super(Opcodes.ASM6, owner, access, name, getNewDesc(desc), mv);
         this.ownerMethod = owner;
         this.nameMethod = name;
         this.descMethod = desc;
         this.methodToBeCreated = methodToBeCreated;
         this.methodIntToArray = methodIntToArray;
         if (this.methodToBeCreated == null) {
             throw new RuntimeException("methodToBeCreated is null");
         }
         this.isInterface = ((access & Opcodes.ACC_INTERFACE) != 0);
         originalAnalyzer = new ExtendedAnalyzerAdapter(owner, access, name, desc, new MethodVisitor(Opcodes.ASM5) {
         });
     }

     /**
      * Instrument the bytecode so that this one implements wrapping an array
      * into an arrayWrapper. As of now, this corresponds to implementing the
      * following java code. AppropriateArrayWrapper.wrap(array).
      *
      * @param desc The description of the array to wrap;
      */
     private void wrap(String desc) {
         if (!desc.startsWith("[")) {
             throw new RuntimeException("trying to wrap an object that is not an array" + desc);
         }
         String descFinalWrapper;
         if (TypeUtil.isPrimitive(Type.getType(desc.substring(1)))) {
             descFinalWrapper = "([" + desc.substring(1) + ")" + (getPrimArrayType(Type.getType(desc.substring(1))).getDescriptor());
             super.visitMethodInsn(Opcodes.INVOKESTATIC, getPrimArrayType(Type.getType(desc.substring(1))).getInternalName(), "get" + Type.getType(desc.substring(1)).getClassName() + "ArrayWrapper", descFinalWrapper, false);
         } else {
             descFinalWrapper = "([Ljava/lang/Object;)" + ARRAYWRAPPERDESC;
             super.visitMethodInsn(Opcodes.INVOKESTATIC, ARRAYWRAPPERNAME, getObjectArray, descFinalWrapper, false);
         }
     }

     /**
      * Instrument the bytecode of the methodVisitor so that this one implements
      * wrapping an array into an arrayWrapper. As of now, this corresponds to
      * implementing the following java code.
      * AppropriateArrayWrapper.wrap(array). Throws an runtimeException if the
      * description is not one of an array.
      *
      * @param desc The description of the array to wrap;
      * @param mv   The methodVisitor that will visit the instructions.
      */
     static void wrap(String desc, MethodVisitor mv) {
         if (!desc.startsWith("[")) {
             throw new RuntimeException("trying to wrap an object that is not an array" + desc);
         }
         String descFinalWrapper;
         if (TypeUtil.isPrimitive(Type.getType(desc.substring(1)))) {
             descFinalWrapper = "([" + desc.substring(1) + ")" + (getPrimArrayType(Type.getType(desc.substring(1)))).getDescriptor();
             mv.visitMethodInsn(Opcodes.INVOKESTATIC, getPrimArrayType(Type.getType(desc.substring(1))).getInternalName(), "get" + Type.getType(desc.substring(1)).getClassName() + "ArrayWrapper", descFinalWrapper, false);
         } else {
             descFinalWrapper = "([Ljava/lang/Object;)" + ARRAYWRAPPERDESC;
             mv.visitMethodInsn(Opcodes.INVOKESTATIC, ARRAYWRAPPERNAME, getObjectArray, descFinalWrapper, false);
         }
     }

     /**
      * Instrument the bytecode so that this one implements an unwrapping from an
      * arrayWrapper into an array. Throws a runtime exception if the description
      * passed is not one of an arrayWrapper. Corresponding to this code
      * AppropriateType[] array; if (arrayWrapper==null){ array = null; } else{
      * array = (AppropriateType[]) arrayWrapper.unwrap(); } The appropriateType
      * is devised by looking at the stack of the original method;
      *
      * @param desc The description of the ArrayWrapper
      */
     private void unwrap(String desc) {
         if (!TypeUtil.isArrayWrapper(Type.getType(desc))) {
             throw new RuntimeException("trying to unwrap an object that is not an ArrayWrapper" + desc);
         }
         Label l0 = new Label();
         mv.visitInsn(Opcodes.DUP);
         mv.visitJumpInsn(Opcodes.IFNONNULL, l0);
         mv.visitInsn(Opcodes.POP);
         mv.visitInsn(Opcodes.ACONST_NULL);
         Label l1 = new Label();
         mv.visitJumpInsn(Opcodes.GOTO, l1);
         mv.visitLabel(l0);
         if (TypeUtil.isPrimitive(Type.getType((TypeUtil.unwrap(Type.getType(desc))).getDescriptor().substring(1)))) {
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getType(desc).getInternalName(), "asArray", "()" + TypeUtil.unwrap(Type.getType(desc)).getDescriptor(), false);
             mv.visitLabel(l1);
         } else {
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getType(desc).getInternalName(), "asArray", "()" + TypeUtil.unwrap(Type.getType(desc)).getDescriptor(), false);
             List<Object> dumpStack = originalAnalyzer.getStack();
             String ArrayType;
                 Object arrayDump = dumpStack.get(dumpStack.size() - 1);
                 if (arrayDump instanceof String) {
                     ArrayType = Type.getType((String) dumpStack.get(dumpStack.size() - 1)).getInternalName();
                     super.visitTypeInsn(Opcodes.CHECKCAST, ArrayType);
                 }
                 else {
                     //Otherwise it means we have push null into the byteCode;
                     ArrayType = TypeUtil.unwrap(Type.getType(desc)).getDescriptor();
                 }
             mv.visitLabel(l1);
         }
     }

     /**
      * Instrument the bytecode so that this one implements updating an
      * ArrayWrapper with an array. . Throws a runtime exception if the
      * description passed is not one of an array. The array and arrayWrapper
      * must already be loaded into the stack, with the Array first.
      * Corresponding to this code if (arrayWrapper==null){ } else{
      * arrayWrapper.update(array); } The appropriateType is devised by looking
      * at the stack of the original method;
      *
      * @param desc The description of the ArrayWrapper
      */
     private void updateArrayWrapper(String desc) {
         if (!TypeUtil.isArrayWrapper(Type.getType(desc)) && !desc.equals("Ljava/lang/Object;")) {
             throw new RuntimeException("The type is not the one of an array Wrapper or Object" + desc);
         }
         Label l0 = new Label();
         mv.visitInsn(Opcodes.DUP);
         mv.visitJumpInsn(Opcodes.IFNONNULL, l0);
         super.visitInsn(Opcodes.POP);
         super.visitInsn(Opcodes.POP);
         // mv.visitInsn(Opcodes.ACONST_NULL);
         Label l1 = new Label();
         mv.visitJumpInsn(Opcodes.GOTO, l1);
         mv.visitLabel(l0);
         if (TypeUtil.isArrayWrapper(Type.getType(desc))) {
             if (TypeUtil.isPrimitive(Type.getType((TypeUtil.unwrap(Type.getType(desc))).getDescriptor().substring(1)))) {
                 mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getType(desc).getInternalName(), "update", "(Ljava/lang/Object;)V", false);
             } else {
                 mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getType(desc).getInternalName(), "update", "(Ljava/lang/Object;)V", false);
             }
         } else {
             mv.visitMethodInsn(Opcodes.INVOKESTATIC, PathConstants.OBJECT_ARRAY_WRAPPER_NAME, "update", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
         }
         mv.visitLabel(l1);
     }

     /**
      * Instrument the bytecode reads by a methodVisitor so that this one
      * implements an unwrapping from an arrayWrapper into an array. Throws a
      * runtime exception if the description passed is not one of an
      * arrayWrapper. Corresponding to this code AppropriateType[] array; if
      * (arrayWrapper==null){ array = null; } else{ array = (AppropriateType[])
      * arrayWrapper.unwrap();
      * <p>
      * }
      *
      * @param desc      The description of the ArrayWrapper
      * @param arrayType the description of the type of the array that will be
      *                  unwrapped.
      * @param mv        The methodVisitor that implements the transformation.
      */
     static void unwrap(String desc, String arrayType, MethodVisitor mv) {
         if (!TypeUtil.isArrayWrapper(Type.getType(desc))) {
             throw new RuntimeException("trying to unwrap an object that is not an ArrayWrapper" + desc);
         }
         Label l0 = new Label();
         mv.visitInsn(Opcodes.DUP);
         mv.visitJumpInsn(Opcodes.IFNONNULL, l0);
         mv.visitInsn(Opcodes.POP);
         mv.visitInsn(Opcodes.ACONST_NULL);
         Label l1 = new Label();
         mv.visitJumpInsn(Opcodes.GOTO, l1);
         mv.visitLabel(l0);
         mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getType(desc).getInternalName(), "asArray", "()" + TypeUtil.unwrap(Type.getType(desc)).getDescriptor(), false);
         mv.visitLabel(l1);
         if (!TypeUtil.isPrimitive(Type.getType((TypeUtil.unwrap(Type.getType(desc))).getDescriptor().substring(1)))) {
             mv.visitTypeInsn(Opcodes.CHECKCAST, arrayType);
         }
     }

     /**
      * Instrument visitFieldInstruction to cope with Arrays wrapped as
      * ArrayWrapper. If the field that is being visited belongs to the API, then
      * direct access is used, followed by wrapping (in the case of getter) when
      * the field is an array - or preceded by unwrapping (in the case of setter)
      * when the field is an array. In the case where the type of the field is
      * object, we call the runtimeUnwrapper/runtimeWrapper to avoid storing an
      * arrayWrapper as a field of an API-object, or to avoid having an object of
      * array Type in the milled part of the program. Otherwise, we simply visit
      * the same field, simply changing its type in the case where the field was
      * an object.
      *
      * @param opcode The opcode of the visit of the field
      * @param owner  The class owner of the field
      * @param name   The name of the field
      * @param desc   The description of the field.
      */
     public void visitFieldInsn(int opcode, String owner, String name, String desc) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         String newDesc = getNewDesc(desc);
         String preciseOwner;
//         try {
//             if (opcode == Opcodes.GETFIELD) {
//                 preciseOwner = (String) stack.get(stack.size() -1);
//             } else if (opcode == Opcodes.PUTFIELD) {
//                 try {
//                     preciseOwner = (String) stack.get(stack.size() - 1 - Type.getType(desc).getSize());
//                 }
//                 catch(ClassCastException ex){
//                     //If we reach this point it means that the owner has not been initialized. It is a weird
//                     //case that happens for constructor of inner classes.
//                     preciseOwner = owner;
//                 }
//             } else {
//                 preciseOwner = owner;
//             }
//         }
//         catch(Throwable t){
//             synchronized (MessageUtil.class) {
//                 System.err.println("ERROR");
//                 System.err.println("static final java.lang.Integer UNINITIALIZED_THIS" + Opcodes.UNINITIALIZED_THIS);
//                 MessageUtil.error(t).report().resume();
//                 System.err.println("ERROR");
//                 System.err.println("Desc  "  + desc + " Owner " + owner);
//                 System.err.println(Type.getType(desc).getSize());
//                 System.err.println("STACK ");
//                 stack.stream().forEach(System.err::println);
//                 System.err.println("STACK ORIGINAL");
//                 originalAnalyzer.getStack().stream().forEach(System.err::println);
//                 preciseOwner = owner;
//             }
//         }
         FieldParameter fp = FieldParameter.getFieldParameter(owner, name, desc);
         String realOwner = fp.cp.className;
         if (TypeUtil.isNotProject(realOwner) || VisibilityTools.isValueField(owner, name, desc)) {
             if (desc.startsWith("[")) {
                 if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
                     super.visitFieldInsn(opcode, owner, name, desc);
                     wrap(desc);
                 } else {
                     unwrap(SignatureArrayTypeModifier.getNewSignature(desc));
                     super.visitFieldInsn(opcode, owner, name, desc);
                 }
             } else if (desc.equals("Ljava/lang/Object;")) {
                 if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
                     super.visitMethodInsn(Opcodes.INVOKESTATIC, PathConstants.RUNTIME_UNWRAPPER_CLASS, "wrap", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                     super.visitFieldInsn(opcode, owner, name, desc);
                 } else {
                     super.visitFieldInsn(opcode, owner, name, desc);
                     super.visitMethodInsn(Opcodes.INVOKESTATIC, PathConstants.RUNTIME_UNWRAPPER_CLASS, "unwrap", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                 }
             } else {
                 super.visitFieldInsn(opcode, owner, name, desc);
             }

         } else {
             super.visitFieldInsn(opcode, owner, ObjectMillrNamingUtil.getNewNameString(FieldParameter.getFieldParameter(owner, name, desc)), newDesc);
         }
         originalAnalyzer.visitFieldInsn(opcode, owner, name, desc);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }

     /**
      * An overriden version of the original visitFrame from methodVisitor, where
      * the types of the objects in the stack-locals have been changed from
      * Arrays to their corresponding arrayWrapper.
      *
      * @param type   The type of the visit.
      * @param nLocal The number of locals.
      * @param local  The locals.
      * @param nStack The number of objects in the stack.
      * @param stackFrame  The stack.
      */
     @Override
     public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stackFrame) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitFrame(type, nLocal, local, nStack, stackFrame);
         modifyStackLocal(local);
         Object[] newLocal = modifyStackLocal(local);
         super.visitFrame(type, nLocal, newLocal, nStack, modifyStackLocal(stackFrame));
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }

     /**
      * Recursively change the type from array to ArrayWrapper in a list of
      * Objects encoding stacks-locals in the ASM AnalyzerAdapter frmework.
      *
      * @param array Array representing either the stack-locals
      * @return The corresponding arrays with types changed from arrays to
      * arrayWrapper.
      */
     private Object[] modifyStackLocal(Object[] array) {
         Object[] newArray = new Object[array.length];
         for (int i = 0; i < array.length; i++) {
             Object o = array[i];
             if (o instanceof String) {
                 try {
                     newArray[i] = getNewOwner((String) o);
                 } catch (Throwable t) {
                     newArray[i] = array[i];
                 }
             } else {
                 newArray[i] = array[i];
             }
         }
         return newArray;
     }

     @Override
     public void visitIincInsn(int var, int increment) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitIincInsn(var, increment);
         super.visitIincInsn(var, increment);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }


     /**
      * Overridden version of visitInsn, where the instructions for array access
      * (IASTORE-IALOAD, and their equivalent for any primitive-reference type),
      * arraylength, have been replaced with getter and setters. Furthermore, the
      * instructions for pushing integers into the stack (like ICONST0) now push
      * their corresponding numbers into the stack. That way, if we have the
      * following list of instructions. ICONST2 newArray [int Then we can analyze
      * the stack to find out that the length of the desired array is two.
      *
      * @param opcode The opcode.
      */
     @Override
     public void visitInsn(int opcode) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         if (opcode == Opcodes.IASTORE) {
             String className = getPrimClassName(Type.INT_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "set", "(II)V", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.IALOAD) {
             String className = getPrimClassName(Type.INT_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "get", "(I)I", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.LASTORE) {
             String className = getPrimClassName(Type.LONG_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "set", "(IJ)V", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.LALOAD) {
             String className = getPrimClassName(Type.LONG_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "get", "(I)J", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.DASTORE) {
             String className = getPrimClassName(Type.DOUBLE_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "set", "(ID)V", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.DALOAD) {
             String className = getPrimClassName(Type.DOUBLE_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "get", "(I)D", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.FASTORE) {
             String className = getPrimClassName(Type.FLOAT_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "set", "(IF)V", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.FALOAD) {
             String className = getPrimClassName(Type.FLOAT_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "get", "(I)F", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.CASTORE) {
             String className = getPrimClassName(Type.CHAR_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "set", "(IC)V", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.CALOAD) {
             String className = getPrimClassName(Type.CHAR_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "get", "(I)C", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.SASTORE) {
             String className = getPrimClassName(Type.SHORT_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "set", "(IS)V", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.SALOAD) {
             String className = getPrimClassName(Type.SHORT_TYPE);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "get", "(I)S", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.BASTORE) {
             String nameArrayWrapper = (String) originalAnalyzer.getStack().get(originalAnalyzer.getStack().size() - 3);
             Type typeWrapped = Type.getType(nameArrayWrapper.substring(1));
             if (typeWrapped.equals(Type.BOOLEAN_TYPE)) {
                 super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getPrimClassName(typeWrapped), "set", "(IZ)V", false);
             } else if (typeWrapped.equals(Type.BYTE_TYPE)) {
                 super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getPrimClassName(typeWrapped), "set", "(IB)V", false);
             } else {
                 throw new RuntimeException("BASTORE is supposed to handle only boolean or byte type");
             }
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.BALOAD) {
             String nameArrayWrapper = (String) originalAnalyzer.getStack().get(originalAnalyzer.getStack().size() - 2);
             Type typeWrapped = (Type.getType(nameArrayWrapper.substring(1)));
             if (typeWrapped.equals(Type.BYTE_TYPE)) {
                 super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getPrimClassName(typeWrapped), "get", "(I)B", false);
             } else if (typeWrapped.equals(Type.BOOLEAN_TYPE)) {
                 super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getPrimClassName(typeWrapped), "get", "(I)Z", false);
             } else {
                 throw new RuntimeException("BALOAD is supposed to handle only boolean or byte type");
             }
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.AALOAD) {
             List<Object> dumpStack = originalAnalyzer.getStack();
             String arrayRef = ((String) dumpStack.get(dumpStack.size() - 2));
             String wrappedRef = arrayRef.substring(1);
             String newDesc = SignatureArrayTypeModifier.getNewSignature(wrappedRef);
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ARRAYWRAPPERNAME, "get", "(I)" + OBJECTDESC, false);
             super.visitTypeInsn(Opcodes.CHECKCAST, Type.getType(newDesc).getInternalName());
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.AASTORE) {
             List<Object> dumpStack = originalAnalyzer.getStack();
             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ARRAYWRAPPERNAME, "set", "(I" + OBJECTDESC + ")V", false);
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.ARRAYLENGTH) {
             List<Object> dumpStack = originalAnalyzer.getStack();
             try {
                 String arrayRef = (String) dumpStack.get(dumpStack.size() - 1);
                 String wrappedDesc = arrayRef.substring(1);
                 if (TypeUtil.isPrimitive(Type.getType(wrappedDesc))) {
                     super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getPrimClassName(Type.getType(wrappedDesc)), "size", "()I", false);
                 } else {
                     super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ARRAYWRAPPERNAME, "size", "()I", false);
                 }
             } catch (ClassCastException ex) {
                 super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ARRAYWRAPPERNAME, "size", "()I", false);
             }
             originalAnalyzer.visitInsn(opcode);
         } else if (opcode == Opcodes.ICONST_0) {
             mv.visitInsn(opcode);
             stack.add(0);
             originalAnalyzer.visitInsn(opcode);
             originalAnalyzer.removeLastStack();
             originalAnalyzer.addStack(0);
         } else if (opcode == Opcodes.ICONST_1) {
             stack.add(1);
             mv.visitInsn(opcode);
             originalAnalyzer.visitInsn(opcode);
             originalAnalyzer.removeLastStack();
             originalAnalyzer.addStack(1);
         } else if (opcode == Opcodes.ICONST_2) {
             stack.add(2);
             mv.visitInsn(opcode);
             originalAnalyzer.visitInsn(opcode);
             originalAnalyzer.removeLastStack();
             originalAnalyzer.addStack(2);
         } else if (opcode == Opcodes.ICONST_3) {
             stack.add(3);
             mv.visitInsn(opcode);
             originalAnalyzer.visitInsn(opcode);
             originalAnalyzer.removeLastStack();
             originalAnalyzer.addStack(3);
         } else if (opcode == Opcodes.ICONST_4) {
             stack.add(4);
             mv.visitInsn(opcode);
             originalAnalyzer.visitInsn(opcode);
             originalAnalyzer.removeLastStack();
             originalAnalyzer.addStack(4);
         } else if (opcode == Opcodes.ICONST_5) {
             stack.add(5);
             mv.visitInsn(opcode);
             originalAnalyzer.visitInsn(opcode);
             originalAnalyzer.removeLastStack();
             originalAnalyzer.addStack(5);
         } else {
             super.visitInsn(opcode);
             originalAnalyzer.visitInsn(opcode);
         }
          if (!isStackCompatible()){
             throw new RuntimeException("poae");
         }
         // LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1,
     }

     /**
      * Overridden version of visitIntInsn where newArray is replaced with
      * arrayWrapper creation and BIPUSH push appropriate integer into the stack.
      *
      * @param opcode The opcode for a visit integer instruction.
      * @param label  The integer.
      */
     @Override
     public void visitIntInsn(int opcode, int label) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         Type T;
         if (opcode == Opcodes.NEWARRAY) {
             try {
                 T = TypeUtil.getPrimType(label);
             } catch (Exception ex) {
                 throw new RuntimeException(ex.getMessage());
             }
             super.visitMethodInsn(Opcodes.INVOKESTATIC, getPrimArrayType(T).getInternalName(), "get" + T.getClassName() + "ArrayWrapper", "(I)" + getPrimArrayType(T).getDescriptor(), false);
             originalAnalyzer.visitIntInsn(opcode, label);
         } else if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
             mv.visitIntInsn(opcode, label);
             stack.add(label);
             originalAnalyzer.visitIntInsn(opcode, label);
             originalAnalyzer.removeLastStack();
             originalAnalyzer.addStack(label);
         } else {
             originalAnalyzer.visitIntInsn(opcode, label);
             super.visitIntInsn(opcode, label);
         }
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }

     }

     /**
      * Overridden version of InvokeDynamnic where the desc and the handle are
      * replaced with appropriate types.
      *
      * @param name    The name of the callsite.
      * @param desc    The description of the callsite.
      * @param bsm     The handle the methods will be call with.
      * @param bsmArgs The arguements ?
      */
     @Override
     public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
         try {
             super.visitInvokeDynamicInsn(name, getNewDesc(desc), bsm, getNewArgs(bsmArgs));
         } catch (ClassNotFoundException ex) {
             throw new RuntimeException(ex);
         }
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }

     /**
      * Modify the methodHandle to have appropriate name and types. If the method
      * comes from API, then we have to wrap/unwrap it. Therefore, we wrap the
      * call to the method and call the wrapper instead.
      *
      * @param bsm A handle
      * @return A handle with appropriate types: the arrays have become
      * arraywrappers.
      */
     private Handle getNewHandle(Handle bsm) {
         MethodParameter mp = MethodParameter.getMethod(bsm.getOwner(), bsm.getName(), bsm.getDesc());
         boolean doesComeFromApi = VisibilityTools.doesMethodImplementationNonProject(mp);
         if (doesComeFromApi) {
             methodToBeCreated.add(mp);
         }
         String name = bsm.getName();
         //TODO this is surely a mistake. We need to wrap the whole methodCall, not just the arguments.
         String newName = doesComeFromApi ? name : ArrayRewriteTransformation.getMethodName(mp);
         return new Handle(bsm.getTag(), bsm.getOwner(), newName, getNewDesc(bsm.getDesc()), bsm.isInterface());
     }

     /**
      * Given BoostrapAguments for a method handle, return new ones with
      * appropriate types.
      *
      * @param bsmArgs arguments for a boostrap method
      * @return new arguments with appropriate types.
      */
     private Object[] getNewArgs(Object... bsmArgs) throws ClassNotFoundException {
         List<Object> newArg = new ArrayList<Object>();
         for (Object o : bsmArgs) {
             if (o instanceof Handle) {
                 newArg.add(getNewHandle((Handle) o));
             } else if (o instanceof Type) {
                 newArg.add(Type.getType(getNewDesc(((Type) o).getDescriptor())));
             } else {
                 newArg.add(o);
             }
         }
         return newArg.toArray();
     }

     @Override
     public void visitJumpInsn(int opcode, Label label) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitJumpInsn(opcode, label);
         super.visitJumpInsn(opcode, label);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }

     private boolean isStackCompatible(){
         if (stack == null){
             return originalAnalyzer.getStack() == null;
         }
         else if (originalAnalyzer.getStack() == null){
             return false;
         }
         else return (stack.size() == originalAnalyzer.getStack().size());
     }

     @Override
     public void visitLabel(Label label) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitLabel(label);
         super.visitLabel(label);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }

     @Override
     public void visitLdcInsn(Object cst) {
         if (cst instanceof Integer) {
             mv.visitLdcInsn(cst);
             stack.add(cst);
         } else {
             super.visitLdcInsn(cst);
         }
         originalAnalyzer.visitLdcInsn(cst);
     }

     @Override
     public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitLookupSwitchInsn(dflt, keys, labels);
         super.visitLookupSwitchInsn(dflt, keys, labels);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }

     @Override
     public void visitMaxs(int maxStack, int maxLocals) {
         try {
             originalAnalyzer.visitMaxs(maxStack, maxLocals);
             super.visitMaxs(maxStack + 20, maxLocals);
         } catch (Throwable t) {
             System.err.println(locals == null ? "local == null " : Arrays.toString(locals.toArray()));
             System.err.println(stack == null ? "stack == null" : Arrays.toString(stack.toArray()));
             throw t;
         }
     }


     /**
      * Overrides call to methods with two distinct cases: Calls to method that
      * have been milled vs called to unmillred methods. Typically, calls to
      * unmilled methods (that is, API) have to be wrapped in two cases: - When
      * one of the type of the method is an Array. - When one of the type of the
      * method is Object. - When the object on which the method is called is an
      * array. In these cases, we call a wrapper method that will make the
      * interface between milled-unmilled code with the appropriate types. The
      * wrapper works by changing the arguments types/return types, but the call
      * the unmmilled method is not done within the wrapper, but remains in the
      * main method code. This is because this method call might be a
      * constructor, notably in the case where the class extends an API class (
      * which is the case for all the classes, since each class extends Object).
      * If what we call is a milled method, we simply need to change the types
      * appropriately.
      *
      * @param opcode The opcode the method is called with
      * @param owner  The owner of the method
      * @param name   The name of the method
      * @param desc   The description of the method
      * @param itf    Whether this method belongs to an interface.
      */
     @Override
     public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
         if (!isStackCompatible()) {
            throw new RuntimeException("poae");
         }
         MethodParameter mp;
         try {
             mp = MethodParameter.getMethod(owner, name, desc);
             if (mp.methodSignature != null){
                 //We need to examine the method signature to replace every type bound that may have come from the class
                 // and replace it with Objects.
                 // In other terms, the signature is an open type, where the type variables may have been externaly.
                 //We need to consider its closure instead.
                 mp.methodSignature = TypeUtil.closeMethodSignature(mp.methodSignature);
             }
             boolean toExamine = false;
             if (owner.equals("xyz/acygn/mokapot/DistributedCommunicator")) {
                 toExamine = true;
             }
             // If the method comes from the API, then the types may not be appropriate, so we change them. Furthermore
             // if the owner of the method used to be an array, for instance if the code used to be :
             //  int[] array = new int[10];
             //  int hash = array.getClass();
             //  then the millr code would return the class of the arrayWrapper. Therefore, we unwrap it before calling
             // the method getClass on it.
             if ((TypeUtil.isNotProject(mp.className)
                     || owner.startsWith("[")) /*|| VisibilityTools.isValueFromEnum(mp) */ && !TypeUtil.isSendRemotely(mp)) {
                 //We build a signature for the methodWrapper. This one will take as arguments the same arguments
                 // as the original method. And return an array consisting of those arguments unwrapped.
                 // Because the problematic method might be a constructor, we cannot simply pass the arguments to
                 // the wrappermethod, and expect the wrapper method to execute the original method.

                 //This wrapper is needed only if the method takes arguments.
                 int numberOfArgs = Arrays.stream(Type.getType(desc).getArgumentTypes()).mapToInt(e -> e.getSize()).sum();
                 int positionOfOwnerIfAny = stack.size() - 1 - numberOfArgs;
                 boolean theOwnerMightNeedToBeModified = opcode != Opcodes.INVOKESTATIC && stack.get(positionOfOwnerIfAny) != null
                         && stack.get(positionOfOwnerIfAny) instanceof String &&
                         (stack.get(positionOfOwnerIfAny).equals("java/lang/Object") || ((String) originalAnalyzer.getStack().get(positionOfOwnerIfAny)).startsWith("["));
                 //(owner.equals("java/lang/Object") || owner.startsWith("[")) && opcode != Opcodes.INVOKESTATIC && stack.get(0) != null && stack.get(0) != Opcodes.UNINITIALIZED_THIS;
                 boolean ArguementNeedToBeModified = numberOfArgs > 0 && originalAnalyzer.stack.stream().
                         skip(positionOfOwnerIfAny+1).anyMatch(
                         e -> ((e instanceof String) ? (Type.getObjectType((String) e).equals(Type.getObjectType("java/lang/Object")) ||
                                 ((String) e).startsWith("[")) : false)) || ((mp.methodSignature!=null) && mp.methodSignature.contains("["));
                 boolean needsWrapperMethod = (
                         (theOwnerMightNeedToBeModified && Type.getArgumentTypes(desc).length > 0) || ArguementNeedToBeModified);
                 if (needsWrapperMethod) {
                     StringBuilder newDesc = new StringBuilder("(");
                     Arrays.stream(Type.getArgumentTypes(SignatureArrayTypeModifier.getNewSignature(desc))).forEach(e -> newDesc.append(e.getDescriptor()));
                     newDesc.append(")[Ljava/lang/Object;");
                     mp.methodDescCalled = newDesc.toString();
                     super.visitMethodInsn(Opcodes.INVOKESTATIC, this.ownerMethod, ArrayRewriteTransformation.getWrappingMethodName(mp), newDesc.toString(), this.isInterface);
                     methodToBeCreated.add(mp);
                 }
                 //At this stage the stack consists only of the original object on which the original method is called (
                 // if this call was not an invokestatic), and the array returned by the wrapper method (if this one was called).
                 //If the object on which the method was called might be an array (that is, the case where its description is either
                 // /java/lang/Object or [...), then we call the RuntimeUnwrapper, that test wether it is an arrayWrapper and unwrap it.
                 // We do this only when the method is not a constructor. We got rid of the constructor / static cases by analyzing the stack
                 // and the opcode of the call to the method.
                 //We cannot run the runtineUnwrapper if we call an invokespecial, since this one call only be called on object of the same class, and runtimeUnwrapper might
                 //mess with it.
                 if (theOwnerMightNeedToBeModified) {
                     if (Type.getArgumentTypes(desc).length > 0) {
                         super.visitInsn(Opcodes.SWAP);
                     }
                     //IT IS NOT CLEAR WHY WE NEED TO REMOVE THE INVOKESPECIAL CASE HERE. IF CALL TO CONSTRUCTOR, THIS SHOULD BE TAKEN CARE OF BY STACK.GET(0) != UNTINITIALZEDTHIS

                     if (owner.equals("java/lang/Object") && opcode != Opcodes.INVOKESPECIAL) {
                         super.visitMethodInsn(Opcodes.INVOKESTATIC, "xyz/acygn/millr/util/RuntimeUnwrapper", "unwrap", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                     }
                     if (owner.startsWith("[")) {
                         // Then it must be an arrayWrapper in the stack.
                         unwrap(getNewDesc(Type.getObjectType(owner).getDescriptor()));
                     }
                     if (Type.getArgumentTypes(desc).length > 0) {
                         super.visitInsn(Opcodes.SWAP);
                     }

                 }

                 // If a wrapperMethod was called, we load each of its arguments in its return array.
                 if (needsWrapperMethod) {
                     if (opcode != Opcodes.INVOKESTATIC) {
                         super.visitInsn(Opcodes.SWAP);
                         super.visitInsn(Opcodes.DUP2);
                         super.visitInsn(Opcodes.POP);
                     } else {
                         super.visitInsn(Opcodes.DUP);
                     }

                     for (int index = 0; index < Type.getArgumentTypes(desc).length; index++) {
                         super.visitInsn(Opcodes.DUP);
                         pushIntegerIntoStack(index);
                         super.visitInsn(Opcodes.AALOAD);
                         if (TypeUtil.isPrimitive(Type.getArgumentTypes(desc)[index])) {
                             super.visitTypeInsn(Opcodes.CHECKCAST, TypeUtil.getPrimWrapperType(Type.getArgumentTypes(desc)[index]).getInternalName());
                             Type T = Type.getArgumentTypes(desc)[index];
                             String nameUnWrap = T.getClassName() + "Value";
                             String descUnWrap = "()" + T.getDescriptor();
                             super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, TypeUtil.getPrimWrapperType(Type.getArgumentTypes(desc)[index]).getInternalName(), nameUnWrap, descUnWrap, false);
                         } else {
                             super.visitTypeInsn(Opcodes.CHECKCAST, Type.getArgumentTypes(desc)[index].getInternalName());
                         }
                         if (Type.getArgumentTypes(desc)[index].getSize() == 1) {
                             super.visitInsn(Opcodes.SWAP);
                         } else {
                             super.visitInsn(Opcodes.DUP2_X1);
                             super.visitInsn(Opcodes.POP2);
                         }
                     }
                     super.visitInsn(Opcodes.POP);
                 }

                 //We call the original method with the right owner and the right types.
                 super.visitMethodInsn(opcode, owner, name, desc, itf);

                 if (needsWrapperMethod) {

                     if (Type.getReturnType(desc).getSize() == 1) {
                         super.visitInsn(Opcodes.SWAP);
                     } else if (Type.getReturnType(desc).getSize() == 2) {
                         super.visitInsn(Opcodes.DUP2_X1);
                         super.visitInsn(Opcodes.POP2);
                     }

                     List<Type> listDescArray = Arrays.asList(Type.getArgumentTypes(desc)).stream().filter(e -> (e.getDescriptor().startsWith("[") || e.equals(Type.getObjectType("java/lang/Object")))).collect(Collectors.toList());
                     for (int i = 0; i < listDescArray.size(); i++) {
                         Type typeArray = listDescArray.get(i);
                         int locInTheObjectArray = Type.getArgumentTypes(desc).length + 2 * i;
                         //We start by loading the ArrayWrapper
                         super.visitInsn(Opcodes.DUP);
                         pushIntegerIntoStack(locInTheObjectArray);
                         super.visitInsn(Opcodes.AALOAD);
                         super.visitTypeInsn(Opcodes.CHECKCAST, getNewOwner(typeArray.getInternalName()));
                         super.visitInsn(Opcodes.SWAP);
                         //Then we  load the Array
                         super.visitInsn(Opcodes.DUP);
                         pushIntegerIntoStack(locInTheObjectArray + 1);
                         super.visitInsn(Opcodes.AALOAD);
                         super.visitTypeInsn(Opcodes.CHECKCAST, typeArray.getInternalName());
                         //Finally, we update the ArrayWrapper/
                         // The stack now is ArrayWrapper [Ljava/lang/Object; array
                         // We want it to be : [Ljava/lang/Object; array; arrayWrapper
                         super.visitInsn(Opcodes.DUP2_X1);
                         //The stack now is [Ljava/lang/Object; array ArrayWrapper [Ljava/lang/Object; array
                         super.visitInsn(Opcodes.POP);
                         super.visitInsn(Opcodes.POP);
                         super.visitInsn(Opcodes.SWAP);
                         updateArrayWrapper(SignatureArrayTypeModifier.getNewSignature(typeArray.getDescriptor()));
                     }

//                     if (mp.methodSignature != null) {
//                         String[] argSignature = MethodSignatureParser.getArguements(mp.methodSignature);
//                         int numberOfInterfaceWrapped = 0;
//                         for (String arg : argSignature) {
//                             if (arg.contains("[") && !arg.startsWith("[")) {
//                                 numberOfInterfaceWrapped += 1;
//                             }
//                         }
//                         for (int i = 0; i < numberOfInterfaceWrapped; i++) {
//                             int locInTheObjectArray = Type.getArgumentTypes(desc).length + 2 * listDescArray.size() + i;
//                             super.visitInsn(Opcodes.DUP);
//                             pushIntegerIntoStack(locInTheObjectArray);
//                             super.visitInsn(Opcodes.AALOAD);
//                             super.visitTypeInsn(Opcodes.CHECKCAST, "xyz/acygn/millr/util/InterfaceWrapUnwrap");
//                             super.visitMethodInsn(Opcodes.INVOKEINTERFACE, "xyz/acygn/millr/util/InterfaceWrapUnwrap", ArrayInterfaceWrapUnwrapTransformation.updateMethodName, "()V", true);
//                         }
//                     }

                     super.visitInsn(Opcodes.POP);
                 }
                 // If the return type is an array then we wrap it.
                 if (Type.getReturnType(desc).getDescriptor().startsWith("[")) {
                     wrap(Type.getReturnType(desc).getDescriptor());
                     // If the return type is an object, we call the runtimeWrapper in case this one is an array.
                 } else {
                     if (Type.getReturnType(desc).equals(Type.getType("Ljava/lang/Object;")) && VisibilityTools.doesMethodImplementationNonProject(mp)) {
                         super.visitMethodInsn(Opcodes.INVOKESTATIC, "xyz/acygn/millr/util/RuntimeUnwrapper", "wrap", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                         // If the return is a class, we want to make sure that this one is not ArrayWrapper class. Therefore we unwrap it if necesseray.
                     } else if (Type.getReturnType(desc).equals(Type.getType("Ljava/lang/Class;")) && VisibilityTools.doesMethodImplementationNonProject(mp)) {
                         super.visitMethodInsn(Opcodes.INVOKESTATIC, "xyz/acygn/millr/util/RuntimeUnwrapper", "unwrapClass", "(Ljava/lang/Class;)Ljava/lang/Class;", false);
                     }
                 }
                 if (TypeUtil.isNotProject(owner) && !owner.startsWith("[") && (Mill.getInstance().getClassReader(owner).getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                     //String upperOwner = VisibilityTools.upperNotProjectClassOveride(owner, name, desc);
                     Mill.getInstance().analyzeMethodCall(opcode, owner, name, desc);
                 }
             } else {
                 String newDesc = getNewDesc(desc);
                 super.visitMethodInsn(opcode, owner, ArrayRewriteTransformation.getMethodName(mp), newDesc, itf);

             }
         } catch (ClassNotFoundException ex) {
             throw new RuntimeException(ex);
         }
         originalAnalyzer.visitMethodInsn(opcode, owner, name, desc, itf);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }


     /**
      * Replace the visit for a creation of a multidimensional array, with the
      * creation of a multidimensional array-wrapper.
      *
      * @param desc The description of the array
      * @param dims The number of dimensions
      */
     @Override
     public void visitMultiANewArrayInsn(String desc, int dims) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitMultiANewArrayInsn(desc, dims);
         super.visitMethodInsn(Opcodes.INVOKESTATIC, ownerMethod, ArrayRewriteTransformation.getMethodNameArrayToInt(dims),
                 ArrayRewriteTransformation.getMethodDescArrayToInt(dims), false);
         for (int i = 0; i < dims; i++) {
             desc = desc.substring(1);
         }
         if (TypeUtil.isPrimitive(Type.getType(desc))) {
             super.visitFieldInsn(Opcodes.GETSTATIC, TypeUtil.getPrimWrapperType(Type.getType(desc)).getInternalName(),
                     "TYPE", "Ljava/lang/Class;");
         } else {
             super.visitLdcInsn(Type.getType(desc));
         }
         super.visitInsn(Opcodes.SWAP);
         super.visitMethodInsn(Opcodes.INVOKESTATIC, ARRAYWRAPPERNAME, getObjectArray, "(Ljava/lang/Class;[I)" + ARRAYWRAPPERDESC, false);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         methodIntToArray.add(dims);
     }


     @Override
     public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         super.visitTableSwitchInsn(min, max, dflt, labels);
         originalAnalyzer.visitTableSwitchInsn(min, max, dflt, labels);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }

     /**
      * Acts as visitTypeInsn in most cases, but replace arrayCreation with
      * arrayWrapper creation.
      *
      * @param opcode The opcode of the instruction
      * @param type   The label.
      */
     @Override
     public void visitTypeInsn(int opcode, final String type) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         if (opcode == Opcodes.ANEWARRAY) {
             Type T = Type.getObjectType(type);
             super.visitLdcInsn(T);
             super.visitMethodInsn(Opcodes.INVOKESTATIC, ARRAYWRAPPERNAME, getObjectArray, "(ILjava/lang/Class;)" + ARRAYWRAPPERDESC, false);
         } else if (opcode == Opcodes.CHECKCAST) {
             String newDesc = getNewDesc(Type.getObjectType(type).getDescriptor());
             super.visitTypeInsn(opcode, Type.getType(newDesc).getInternalName());
         } else {
             super.visitTypeInsn(opcode, type);
         }
         originalAnalyzer.visitTypeInsn(opcode, type);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }

     @Override
     public void visitVarInsn(int opcode, int var) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         super.visitVarInsn(opcode, var);
         originalAnalyzer.visitVarInsn(opcode, var);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }

     @Override
     public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitAnnotation(desc, visible);

         AnnotationVisitor av = super.visitAnnotation(getNewSignatureField(desc), visible);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         return av;

     }

     @Override
     public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitTypeAnnotation(typeRef, typePath, desc, visible);
         AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, getNewSignatureField(desc), visible);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         return av;
     }

     @Override
     public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitParameterAnnotation(parameter, desc, visible);
         AnnotationVisitor av =  super.visitParameterAnnotation(parameter, SignatureArrayTypeModifier.getNewSignature(desc), visible);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         return av;
     }

     @Override
     public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitInsnAnnotation(typeRef, typePath, desc, visible);
         AnnotationVisitor av =  super.visitInsnAnnotation(typeRef, typePath, getNewSignatureField(desc), visible);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         return av;
     }

     @Override
     public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitLocalVariable(name, desc, signature, start, end, index);
         super.visitLocalVariable(name, getNewSignatureField(desc), getNewSignatureField(signature), start, end, index);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }

     @Override
     public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitLocalVariableAnnotation(typeRef, typePath, end, end, index, desc, visible);
         AnnotationVisitor av =  super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, getNewSignatureField(desc), visible);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         return av;
     }

     @Override
     public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
         AnnotationVisitor av =  super.visitTryCatchAnnotation(typeRef, typePath, getNewSignatureField(desc), visible);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         return av;
     }

     @Override
     public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
         originalAnalyzer.visitTryCatchBlock(start, end, handler, type);
         super.visitTryCatchBlock(start, end, handler, type);
         if (!isStackCompatible()) {
             throw new RuntimeException("poae");
         }
     }


     /**
      * Add a call to the runtimeUnwrapper method in a methodVisitor. The stack must start with an object.
      *
      * @param mv The methodVisitor
      */
     static void runtimeUnwrap(MethodVisitor mv) {
         mv.visitMethodInsn(Opcodes.INVOKESTATIC, "xyz/acygn/millr/util/RuntimeUnwrapper", "unwrap", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
     }


     /**
      * Add a call to the runtimewrapper method in a methodVisitor. The stack must start with an object.
      *
      * @param mv The methodVisitor
      */
     static void runtimeWrap(MethodVisitor mv) {
         mv.visitMethodInsn(Opcodes.INVOKESTATIC, "xyz/acygn/millr/util/RuntimeUnwrapper", "wrap", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
     }

     private void pushIntegerIntoStack(int i) {
         /** Bipush    <n> is an integer >= -128 and <= 127 that is pushed onto the stack.
          *
          */
         if (-128 <= i && i <= 127) {
             super.visitIntInsn(Opcodes.BIPUSH, i);
         }
         /*
         Sipush <n> is a signed integer in the range -32768 to 32767.
          */
         else if (-32768 <= i && i <= 32767) {
             super.visitIntInsn(Opcodes.SIPUSH, i);
         } else {
             super.visitLdcInsn(i);
         }

     }


     /**
      * Optimize the push with BIPUSH, SIPUSH, or LDC depending on the length of the integer;
      *
      * @param i
      */
     public static void pushIntegerIntoStack(int i, MethodVisitor mv) {
         /** Bipush    <n> is an integer >= -128 and <= 127 that is pushed onto the stack.
          *
          */
         if (-128 <= i && i <= 127) {
             mv.visitIntInsn(Opcodes.BIPUSH, i);
         }
         /*
         Sipush <n> is a signed integer in the range -32768 to 32767.
          */
         else if (-32768 <= i && i <= 32767) {
             mv.visitIntInsn(Opcodes.SIPUSH, i);
         } else {
             mv.visitLdcInsn(i);
         }

     }
 }

