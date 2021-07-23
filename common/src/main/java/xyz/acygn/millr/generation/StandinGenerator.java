package xyz.acygn.millr.generation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.NATIVE;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PROTECTED;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isStatic;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.L2I;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LSHR;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SWAP;
import org.objectweb.asm.Type;
import static org.objectweb.asm.Type.ARRAY;
import static org.objectweb.asm.Type.BOOLEAN;
import static org.objectweb.asm.Type.BYTE;
import static org.objectweb.asm.Type.CHAR;
import static org.objectweb.asm.Type.DOUBLE;
import static org.objectweb.asm.Type.FLOAT;
import static org.objectweb.asm.Type.INT;
import static org.objectweb.asm.Type.LONG;
import static org.objectweb.asm.Type.METHOD;
import static org.objectweb.asm.Type.OBJECT;
import static org.objectweb.asm.Type.SHORT;
import static org.objectweb.asm.Type.VOID;
import org.objectweb.asm.util.CheckClassAdapter;
import xyz.acygn.mokapot.skeletons.Authorisation;
import xyz.acygn.mokapot.skeletons.ForwardingStandinStorage;
import xyz.acygn.mokapot.skeletons.IndirectStandin;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.skeletons.StandinStorage;
import xyz.acygn.mokapot.skeletons.TrivialStandinStorage;
import xyz.acygn.mokapot.util.Pair;
import static xyz.acygn.mokapot.util.VMInfo.isClassNameInSealedPackage;
import static xyz.acygn.mokapot.wireformat.MethodCodes.defaultMethodCode;
import xyz.acygn.mokapot.wireformat.ObjectWireFormat;
import static xyz.acygn.mokapot.wireformat.ObjectWireFormat.Technique.LONG_REFERENCE;

/**
 * An object that generates standins for a particular class. By default, a
 * standin generator uses slow but general implementations for the various parts
 * of the standin. However, it has methods that allow these to be overriden with
 * more efficient implementations in special cases where those implementations
 * are known (by the caller) to work.
 * <p>
 * Both inheritance-based and composition-based standins can be generated.
 *
 * @author Alex Smith
 * @param <T> The class for which the standin is being generated.
 * @see xyz.acygn.mokapot.skeletons.Standin
 */
public class StandinGenerator<T> {

    /**
     * The class for which standins are being generated.
     */
    private final Class<T> about;

    /**
     * A list of the fields and methods of the class we're generating standins
     * for. Also contains salting information that let us convert the method
     * names to method codes.
     */
    private final ObjectWireFormat<T> db;

    /**
     * Creates a standin generator for the given class, with default settings.
     *
     * @param db An object wire format for the class for which the standings
     * will be generated.
     */
    public StandinGenerator(ObjectWireFormat<T> db) {
        this.about = db.getAbout();
        this.db = db;
    }

    /**
     * Generates a standin class.
     *
     * @param writeTo The class visitor to which to write the generated class.
     * (The given visitor will be used to visit the generated class, and would
     * typically be something that writes the visited class to a file or loads
     * it via a class loader.) Note that this will not be given accurate stack
     * frame information, and as such must compute it itself (most class
     * visitors which write classes have an option to do this).
     * @param inheritanceBased <code>true</code> to generate an
     * inheritance-based standin class; <code>false</code> to generate a
     * composition-based standin class.
     * @return Two values: the recommended internal name for the generated class
     * (which is similar to the return value from <code>Class#forName</code>,
     * but uses slashes rather than dots to separate components); and whether
     * the class is internally final (if this is <code>true</code>, it's a bad
     * idea to try to generate an inheritance-based standin for the class).
     * @throws IOException If the definition of the class for which the standin
     * is being generated could not be loaded
     */
    public Pair<String, Boolean> generate(
            ClassVisitor writeTo, boolean inheritanceBased)
            throws IOException {
        Visitor v = new Visitor(new CheckClassAdapter(writeTo, false),
                inheritanceBased);
        String resourceName = "/" + about.getName().replace('.', '/') + ".class";
        InputStream classStream = about.getResourceAsStream(resourceName);
        if (classStream == null) {
            throw new IOException("Cannot find class resource " + resourceName);
        }

        ClassReader cr = new ClassReader(classStream);
        cr.accept(v, SKIP_DEBUG | SKIP_FRAMES);

        if (writeTo instanceof ClassWriter && listener != null) {
            listener.classGenerated(((ClassWriter) writeTo).toByteArray(),
                    v.getGeneratedClassName().replace('/', '.'));
        }

        return new Pair<>(v.getGeneratedClassName(), v.isInternallyFinal());
    }

    /**
     * Generates a standin class, returning its bytecode. The name of the
     * generated class will also be returned.
     *
     * @param inheritanceBased Whether to generate an inheritance-based standin
     * (true) or composition-based standin (false).
     * @return A pair of the bytecode of the generated class, and its name (in
     * Java format with dots, not internal format with slashes).
     * @throws IOException If the bytecode of the original class could not be
     * found or read
     */
    public Pair<byte[], String> generateAsBytecode(boolean inheritanceBased)
            throws IOException {
        ClassWriter cw = new ClassWriter(COMPUTE_FRAMES);
        Pair<String, Boolean> nameAndFinality
                = generate(cw, inheritanceBased);
        return new Pair<>(cw.toByteArray(),
                nameAndFinality.getFirst().replace('/', '.'));
    }

    /**
     * Main method, allowing batch/independent standin generation. This takes a
     * set of classes to generate standins for as arguments; the classes
     * themselves, and all dependencies, must already be on the classpath (and
     * will be found via a standard classpath search). For each class given,
     * both sorts of standin (inheritance-based and composition-based) will be
     * generated. The resulting classes will be written to the appropriate place
     * relative to the current directory (e.g. a standin for
     * <code>java.lang.ArrayList</code> might be written to
     * <code>./java/lang/ArrayList$mokapot_standin1.class</code>);
     * inheritance-based standins have names ending
     * <code>$mokapot_standin1</code> and composition-based standins have names
     * ending <code>$mokapot_standin2</code>.
     * <p>
     * Note that if a class already implements
     * <code>xyz.acygn.mokapot.Standin</code>, it will be ignored (because if an
     * object is already a standin, it doesn't need a separate standin), and
     * classes with names appropriate for standins may be overwritten even if
     * they appear in the input. Likewise, if the "class" is actually an
     * interface or abstract, it will be ignored. This makes it easy to use this
     * program with an existing output folder full of <code>.class</code> files,
     * even if some of them were generated by this program itself.
     * <p>
     * Progress output will be written to standard error.
     *
     * @param args The class names of the classes to generate standins for, from
     * Java's point of view (e.g. <code>java.lang.ArrayList</code>). An argument
     * <code>-v</code> is not a class name; rather, it requests more verbose
     * output on standard error.
     */
    public static void main(String[] args) {
        /* Load all the classes /first/, so that we don't do anything if there's
           an error in the command line. */
        AtomicInteger errorCount = new AtomicInteger(0);
        /* Using Class#forName, rather than the more direct version via
           ClassLoader, deadlocks for some reason, at a consistent point each
           time. We can fix this via serialising the forName calls, but it's
           simpler and clearer to just use the ClassLoader directly. */
        ClassLoader loader = StandinGenerator.class.getClassLoader();
        boolean verbose = Arrays.asList(args).parallelStream()
                .anyMatch((s) -> s.equals("-v"));
        Set<Class<?>> classes = Arrays.asList(args).parallelStream()
                .filter((s) -> !s.equals("-v"))
                .flatMap((arg) -> {
                    try {
                        Class<?> c = loader.loadClass(arg);
                        if (Standin.class.isAssignableFrom(c)
                                || c.isInterface()
                                || isAbstract(c.getModifiers())) {
                            return Stream.empty();
                        } else {
                            return Stream.of(c);
                        }
                    } catch (ClassNotFoundException | VerifyError ex) {
                        /* Note: the reason we're catching an Error here is
                           because if the class file exists but is corrupted,
                           we can recover (we haven't tried to use the class in
                           question for anything and can choose not to), and
                           the error message is more readable this way. */
                        synchronized (System.err) {
                            System.err.println("Error: could not load " + arg
                                    + ": " + ex);
                        }
                        errorCount.getAndIncrement();
                        return Stream.empty();
                    }
                }).collect(Collectors.toSet());

        if (errorCount.get() != 0) {
            System.err.println("Not continuing: " + errorCount + " errors.");
            System.exit(66);
        }

        classes.parallelStream().forEach((Class<?> c) -> {
            StandinGenerator<?> generator
                    = new StandinGenerator<>(new ObjectWireFormat<>(c));
            for (int i = 0; i <= 1; i++) {
                try {
                    ClassWriter cw = new ClassWriter(COMPUTE_FRAMES);
                    Pair<String, Boolean> nameAndFinality
                            = generator.generate(cw, i == 1);
                    try (OutputStream outputFile
                            = new FileOutputStream(
                                    nameAndFinality.getFirst() + ".class")) {
                        outputFile.write(cw.toByteArray());
                    }

                    String modprefix = Modifier.toString(c.getModifiers());
                    if (!"".equals(modprefix)) {
                        modprefix += " ";
                    }
                    if (verbose) {
                        synchronized (System.err) {
                            System.err.println(
                                    modprefix + c + ": compiled to "
                                    + nameAndFinality.getFirst() + ".class");
                        }
                    }

                    /* Don't try to generate the inheritance-based standin if
                       we discovered, while generating the composition-based
                       standin, that the class is internally final. */
                    if (nameAndFinality.getSecond()) {
                        break;
                    }
                    /* Don't generate inheritance-based standins for copiable
                       classes; we rely too heavily on copiable objects having
                       correct output from getClass and Object#toString. */
                    ObjectWireFormat<?> cOwf
                            = new ObjectWireFormat<>(c);
                    if (cOwf.getTechnique()
                            != LONG_REFERENCE) {
                        break;
                    }
                } catch (IOException | AssertionError ex) {
                    /* Ugh at having to catch an error /again/, but
                       AssertionError is how CheckClassAdapter reports an
                       incorrect use of the ASM API. It should really be using
                       an exception for that. */
                    synchronized (System.err) {
                        System.err.println(c + ": " + ex);
                        errorCount.getAndIncrement();
                    }
                }
            }
        });

        if (errorCount.get() > 0) {
            System.err.println("Build failed: " + errorCount + " errors.");
            System.exit(71);
        }

        if (verbose) {
            System.err.println("Build complete.");
        }
    }

    /**
     * The visitor used to actually generate the standin class. The visitor will
     * be used to visit the original class, and will describe the new standin
     * class as it does so.
     */
    private class Visitor extends ClassVisitor {

        /**
         * Whether to generate an inheritance-based standin.
         */
        private final boolean inheritanceBased;

        /**
         * The internal name of the class being visited.
         */
        private String oldClassName;

        /**
         * Whether the class specifies itself as final in its bytecode. (For
         * whatever reason, this isn't 100% correlated with whether Java's
         * reflection API considers the class to be final; so far I've seen the
         * discrepancy only show up with static anonymous inner classes that
         * were produced via constructing an interface, but there might be other
         * cases where this happens.)
         */
        private boolean isInternallyFinal;

        /**
         * Creates a new visitor to generate the standin class.
         *
         * @param writeTo The visitor that will be given a description of the
         * standin class. Typically, this would be a visitor that writes the
         * given description into a Java class file.
         * @param inheritanceBased Whether the standin to generate should be
         * inheritance-based.
         */
        private Visitor(ClassVisitor writeTo, boolean inheritanceBased) {
            super(ASM5, writeTo);
            this.inheritanceBased = inheritanceBased;
        }

        /**
         * Works out an appropriate name (including package) for the class that
         * this generator is generating. Cannot be called until after
         * <code>#visit()</code> has started to run.
         *
         * @return A class name, in internal format (like the format used by
         * <code>Class#getName()</code>, but with slashes instead of periods).
         */
        private String getGeneratedClassName() {
            String mangledClassName = oldClassName;
            if (isClassNameInSealedPackage(oldClassName)) {
                /* The JVM simply won't let us inject into the standard Java
                   packages, even by loading from the command line. So create
                   a wrapper package instead. */
                mangledClassName = "xyz/acygn/mokapot/wrapper/"
                        + mangledClassName;
            }

            return mangledClassName
                    + "$mokapot_standin" + (inheritanceBased ? "2" : "1");
        }

        /**
         * Calculates the name of the field used to store the standin's current
         * storage situation. When using <code>IndirectStandin</code>, this
         * needs to match the field name declared there. Otherwise, this is
         * arbitrary but aims to avoid clashes with existing fields that might
         * exist in the class.
         *
         * @return An appropriate name for the field, matching that in
         * <code>IndirectStandin</code> if it's in use.
         * @see IndirectStandin#storage
         */
        private String getStandinStorageFieldName() {
            return inheritanceBased ? "mokapot_standin$storage" : "storage";
        }

        /**
         * Returns whether the visited class is internally treated as
         * <code>final</code> by Java. For some reason, this doesn't 100% match
         * up to whether the reflection API claims it to be <code>final</code>;
         * this is a more accurate indication of whether inheriting from the
         * class is possible. This has nothing to do with the generated standin,
         * and is information determined as a side effect.
         * <p>
         * The intended use is to, while generating a composition-based standin,
         * determine whether an inheritance-based standin could also work.
         *
         * @return Whether the class is internally final.
         */
        public boolean isInternallyFinal() {
            return isInternallyFinal;
        }

        /**
         * Causes the output visitor to visit a slightly modified version of
         * this class, with the name, superclass, and interfaces of the class
         * replaced.
         *
         * @param version The version of the class. Will be unchanged.
         * @param access The access flags of the class. Will be unchanged.
         * @param name The name of the class. Will be changed to a name
         * generated by <code>#getGeneratedClassName()</code>.
         * @param signature The generic signature of the class. Will be
         * unchanged.
         * @param superName The name of the superclass. The superclass of the
         * standin will be the original class itself (if this is an
         * inheritance-based standin) or
         * <code>xyz/mokapot/acygn/skeletons/IndirectStandin</code> (if this is
         * a composition-based standin).
         * @param interfaces The interfaces directly implemented by the original
         * class. This will be replaced by
         * <code>xyz/acygn/mokapot/skeletons/Standin</code> plus all the
         * interfaces transitively implemented by the original class (because
         * the superclass may change, thus breaking the transitivity).
         */
        @Override
        public void visit(int version, int access, String name,
                String signature, String superName, String[] interfaces) {

            isInternallyFinal = ((access & ACC_FINAL) != 0);

            oldClassName = name;
            String newSuperName = name;
            if (!inheritanceBased) {
                newSuperName = "xyz/acygn/mokapot/skeletons/IndirectStandin";
            }

            String[] newInterfaces;
            if (inheritanceBased) {
                newInterfaces = new String[]{
                    "xyz/acygn/mokapot/skeletons/Standin"};
            } else {
                /* We need the whole recursive tree of interfaces that are
                   implemented by the original class. (Simply copying them from
                   <code>interfaces</code> will do no good as interfaces that
                   were implemented on the superclass won't be listed there.) */
                newInterfaces = Stream.concat(
                        db.getAccessibleInterfaces().stream().map(
                                (c) -> c.getName().replace('.', '/')),
                        Stream.of("xyz/acygn/mokapot/skeletons/Standin")).
                        toArray((x) -> new String[x]);
            }

            /* Note: nulling out the signature does a full type erasure, which
               is what we want in this case in order to avoid having to generate
               a generic type; we use an unchecked type instead */
            super.visit(version, access, getGeneratedClassName(),
                    null, newSuperName, newInterfaces);
        }

        /**
         * Generates all the additional fields and methods required in the new
         * standin class, that don't correspond directly to a field or method
         * read from the original class's bytecode. Note that this includes
         * methods that actually exist in the original class, in cases where the
         * new method is based on, e.g., a reflected version of the original
         * class rather than on reading its bytecode.
         */
        @Override
        public void visitEnd() {
            /* We call Authorisation#verify() quite a bit, so extract a
               reference for it in advance. Likewise for IndirectStandin's
               constructor. */
            MethodReference authVerify;
            MethodReference indirectStandinConstructor;
            try {
                authVerify = new MethodReference(
                        Authorisation.class.getMethod("verify"));
                indirectStandinConstructor = new MethodReference(
                        IndirectStandin.class.getDeclaredConstructor(
                                Object.class, Authorisation.class));
            } catch (NoSuchMethodException | SecurityException ex) {
                throw new RuntimeException(ex);
            }

            if (inheritanceBased) {
                /* We need to create the standin storage field. */
                super.visitField(ACC_PRIVATE,
                        getStandinStorageFieldName(),
                        Type.getDescriptor(StandinStorage.class), null, null);

                /* We need to create a getter and setter for the standin
                   storage field. */
                MethodVisitor writeGetterTo = super.visitMethod(ACC_PUBLIC, "getStorage",
                        "(Lxyz/acygn/mokapot/skeletons/Authorisation;)Lxyz/acygn/mokapot/skeletons/StandinStorage;",
                        null, null);
                writeGetterTo.visitParameter("auth", 0);
                writeGetterTo.visitCode();
                writeGetterTo.visitVarInsn(ALOAD, 0);
                writeGetterTo.visitFieldInsn(GETFIELD,
                        getGeneratedClassName(), getStandinStorageFieldName(),
                        Type.getDescriptor(StandinStorage.class));
                writeGetterTo.visitInsn(ARETURN);
                writeGetterTo.visitMaxs(0, 0); // placeholder
                writeGetterTo.visitEnd();

                MethodVisitor writeSetterTo = super.visitMethod(ACC_PUBLIC, "setStorage",
                        "(Lxyz/acygn/mokapot/skeletons/StandinStorage;Lxyz/acygn/mokapot/skeletons/Authorisation;)V",
                        null, null);
                writeSetterTo.visitParameter("storage", 0);
                writeSetterTo.visitParameter("auth", 0);
                writeSetterTo.visitCode();
                writeSetterTo.visitVarInsn(ALOAD, 2);
                authVerify.generateCallInsn(INVOKEVIRTUAL, writeSetterTo);
                writeSetterTo.visitVarInsn(ALOAD, 0);
                writeSetterTo.visitVarInsn(ALOAD, 1);
                writeSetterTo.visitFieldInsn(PUTFIELD,
                        getGeneratedClassName(), getStandinStorageFieldName(),
                        Type.getDescriptor(StandinStorage.class));
                writeSetterTo.visitInsn(RETURN);
                writeSetterTo.visitMaxs(0, 0); // placeholder
                writeSetterTo.visitEnd();

                /* We also need a getReferent method. */
                MethodVisitor writeGetReferentTo = super.visitMethod(ACC_PUBLIC, "getReferent",
                        "(Lxyz/acygn/mokapot/skeletons/Authorisation;)Ljava/lang/Object;",
                        null, null);
                writeGetReferentTo.visitParameter("auth", 0);
                writeGetReferentTo.visitCode();
                writeGetReferentTo.visitVarInsn(ALOAD, 0);
                writeGetReferentTo.visitInsn(ARETURN);
                writeGetReferentTo.visitMaxs(0, 0); // placeholder
                writeGetReferentTo.visitEnd();
            }

            /* Generate the getReferentClass method. */
            MethodVisitor writeGetReferentClassTo = super.visitMethod(ACC_PUBLIC, "getReferentClass",
                    "(Lxyz/acygn/mokapot/skeletons/ProxyOrWrapper$Namespacer;)Ljava/lang/Class;",
                    null, null);
            writeGetReferentClassTo.visitParameter("dummy", 0);
            writeGetReferentClassTo.visitCode();
            writeGetReferentClassTo.visitLdcInsn(Type.getType(about));
            writeGetReferentClassTo.visitInsn(ARETURN);
            writeGetReferentClassTo.visitMaxs(0, 0); // placeholder
            writeGetReferentClassTo.visitEnd();

            /* We can't override or call a method if it's final, private,
               static, or package-private to the wrong package; additionally, we
               shouldn't try to forward finalize() between systems as it ends up
               running locally no matter what you do. */
            Set<Method> invocableMethods
                    = db.getMethods().stream().filter((Method m) -> {
                        int modifiers = m.getModifiers();
                        if ((modifiers & (FINAL | PRIVATE
                                | STATIC)) != 0) {
                            return false;
                        }
                        if (m.getName().equals("finalize")
                                && m.getParameterCount() == 0) {
                            return false;
                        }
                        if ((modifiers
                                & (PROTECTED | PUBLIC)) != 0) {
                            return true;
                        }
                        return Objects.equals(m.getDeclaringClass().getPackage(),
                                about.getPackage());
                    }).collect(Collectors.toSet());

            /* Generate the delegating method wrappers. */
            invocableMethods.stream().forEach((m) -> {
                /* Note: We're assuming that the modifier values in Opcodes and
                   in Modifier are the same. They were last time I checked, and
                   there's good reason to assume that ASM did that
                   intentionally, but if this breaks, check to make sure that
                   they're still the same. */
                int modifiers = m.getModifiers();

                /* Even if the original method is native, the delegating method
                   won't be. */
                modifiers &= ~NATIVE;

                if (!isFinal(modifiers)
                        && !isPrivate(modifiers)
                        && !isStatic(modifiers)
                        && (!m.getName().equals("finalize")
                        || m.getParameterCount() != 0)) {
                    generateDelegatingMethod(m, m.getName(),
                            Type.getMethodDescriptor(m),
                            modifiers, Arrays.stream(m.getExceptionTypes())
                                    .map((x) -> Type.getType(x).getInternalName())
                                    .toArray((x) -> new String[x]));
                }
            });

            /* Generate the from-referent constructor (if this is
               composition-based). We just defer to IndirectStandin's. */
            if (!inheritanceBased) {
                MethodVisitor writeConstructorTo = super.visitMethod(ACC_PUBLIC, "<init>", "(L" + oldClassName
                        + ";Lxyz/acygn/mokapot/skeletons/Authorisation;)V", null, null);
                writeConstructorTo.visitParameter("referent", 0); // local 1
                writeConstructorTo.visitParameter("auth", 0); // local 2
                writeConstructorTo.visitCode();

                /* Just delegate to the super constructor. */
                writeConstructorTo.visitVarInsn(ALOAD, 0);
                writeConstructorTo.visitVarInsn(ALOAD, 1);
                writeConstructorTo.visitVarInsn(ALOAD, 2);
                indirectStandinConstructor.generateCallInsn(INVOKESPECIAL, writeConstructorTo);
                writeConstructorTo.visitInsn(RETURN);
                writeConstructorTo.visitMaxs(0, 0); // placeholder
                writeConstructorTo.visitEnd();
            }

            /* Generate invoke(). */
            MethodVisitor writeInvokeTo = super.visitMethod(ACC_PUBLIC, "invoke",
                    "(J[Ljava/lang/Object;Lxyz/acygn/mokapot/skeletons/Authorisation;)Ljava/lang/Object;",
                    null, null);
            writeInvokeTo.visitParameter("methodCode", 0); // local 1
            writeInvokeTo.visitParameter("methodArguments", 0); // local 3
            writeInvokeTo.visitParameter("auth", 0); // local 4
            writeInvokeTo.visitCode();

            /* Verify the authorisation. */
            writeInvokeTo.visitVarInsn(ALOAD, 4);
            authVerify.generateCallInsn(INVOKEVIRTUAL, writeInvokeTo);

            /* Create a label corresponding to each invocable method. */
            Map<Method, Label> methodLabelMap = new HashMap<>();
            invocableMethods.stream().forEach(
                    (m) -> methodLabelMap.put(m, new Label()));

            /* Calculate method codes, and partition them by their bottom 32
               bits. Within each partition, we create a map for the top 32
               bits, and a label for the partition. */
            int salt = db.getMethodCodeSalt();
            Stream<Pair<Method, Long>> codeStream
                    = invocableMethods.stream().map(Pair.preservingMap((m)
                            -> defaultMethodCode(m, salt)));
            Map<Integer, Pair<Map<Integer, Method>, Label>> codeGroupMap
                    = codeStream.collect(Collectors.groupingBy(
                            (p) -> (int) (long) p.getSecond(),
                            Collectors.collectingAndThen(
                                    Collectors.toMap(
                                            (p) -> (int) (p.getSecond() >> 32),
                                            Pair::getFirst),
                                    Pair.preservingMap((x) -> new Label()))));
            /* Ensure that the codeGroupMap is sorted. That means that we'll
               get the keys and values in corresppnding order, and also satisfy
               the JVM's requirement for a lookupswitch to have sorted keys. */
            codeGroupMap = new TreeMap<>(codeGroupMap);

            /* Load the top 32 bits of the method code, leaving them on the
               stack for later. */
            writeInvokeTo.visitVarInsn(LLOAD, 1);
            writeInvokeTo.visitLdcInsn((Integer) 32);
            writeInvokeTo.visitInsn(LSHR);
            writeInvokeTo.visitInsn(L2I);

            /* Load the bottom 32 bits of the method code. */
            writeInvokeTo.visitVarInsn(LLOAD, 1);
            writeInvokeTo.visitInsn(L2I);

            /* Create labels for error handling code. errorHandlerPrePop is
               called before the top 32 bits of the code have been popped from
               the stack; errorHandlerPostPop after that's already happened. */
            Label errorHandlerPrePop = new Label();
            Label errorHandlerPostPop = new Label();

            /* Create a lookupswitch for the bottom 32 bits of the method
               code. */
            writeInvokeTo.visitLookupSwitchInsn(errorHandlerPrePop,
                    codeGroupMap.keySet().stream()
                            .mapToInt(Number::intValue).toArray(),
                    codeGroupMap.values().stream()
                            .map(Pair::getSecond).toArray(Label[]::new));

            /* Generate the error handling code. */
            writeInvokeTo.visitLabel(errorHandlerPrePop);
            writeInvokeTo.visitInsn(POP);
            writeInvokeTo.visitLabel(errorHandlerPostPop);
            writeInvokeTo.visitVarInsn(LLOAD, 1);
            writeInvokeTo.visitMethodInsn(INVOKESTATIC,
                    Type.getInternalName(Standin.class),
                    "unrecognisedMethodCode", "(J)V", true);
            /* This code is unreachable, but the verifier doesn't know that.
               Keep it happy. */
            writeInvokeTo.visitInsn(ACONST_NULL);
            writeInvokeTo.visitInsn(ARETURN);

            /* For each of the lookupswitch entries (other than the default),
               generate a second lookupswitch that checks the top 32 bits. */
            codeGroupMap.entrySet().stream().forEach((entry) -> {
                writeInvokeTo.visitLabel(entry.getValue().getSecond());
                /* Sort the map, for the same reason as before. */
                SortedMap<Integer, Method> top32map
                        = new TreeMap<>(entry.getValue().getFirst());
                writeInvokeTo.visitLookupSwitchInsn(errorHandlerPostPop,
                        top32map.keySet().stream()
                                .mapToInt(Number::intValue).toArray(),
                        top32map.values().stream()
                                .map(methodLabelMap::get).toArray(Label[]::new));
            });

            /* At this point, we've either jumped to the label corresponding to
               a method, or else have run the error handling code (and exited
               via exception). So we just need to write code to invoke each
               method. */
            methodLabelMap.entrySet().stream().forEach((entry) -> {
                writeInvokeTo.visitLabel(entry.getValue());
                Method method = entry.getKey();

                Type[] arguments = Type.getArgumentTypes(method);
                Type returnType = Type.getReturnType(method);

                /* Push the referent onto the stack. That's either
                   <code>this</code> (if inheritance-based), or
                   <code>(T)this.referent</code> (if composition-based). */
                if (!inheritanceBased) {
                    writeInvokeTo.visitVarInsn(ALOAD, 0);
                    writeInvokeTo.visitFieldInsn(GETFIELD,
                            Type.getInternalName(IndirectStandin.class),
                            "referent",
                            Type.getDescriptor(Object.class)
                    );
                    writeInvokeTo.visitTypeInsn(CHECKCAST,
                            oldClassName);
                } else {
                    writeInvokeTo.visitVarInsn(ALOAD, 0);
                }

                /* Push the arguments onto the stack. */
                int j = 0; // index into arguments array
                for (Type argument : arguments) {
                    /* Argument 3 to invoke() is the argument array for the
                       invoked method. */
                    writeInvokeTo.visitVarInsn(ALOAD, 3);
                    writeInvokeTo.visitLdcInsn((Integer) j);
                    writeInvokeTo.visitInsn(AALOAD);
                    writeUnboxInsn(writeInvokeTo, argument);
                    j++;
                }

                /* Invoke the method, bypassing any wrappers within this class
                   (as is required by the API of invoke()). */
                if (!inheritanceBased
                        && method.getDeclaringClass().equals(Object.class)
                        && method.getName().equals("clone")
                        && method.getParameterCount() == 0) {
                    /* We can't use INVOKEVIRTUAL/INVOKESPECIAL to invoke
                       Object#clone, as it's <code>protected</code>, unless
                       this is an inheritance-based standin. Instead, we call
                       a method of the authorisation, local variable 4, to get
                       an object cloner; and then call a method of that. */
                    writeInvokeTo.visitVarInsn(ALOAD, 4);
                    generateObjectCloneCode(writeInvokeTo);
                } else {
                    MethodReference mRef = new MethodReference(method, about);
                    mRef.generateCallInsn(inheritanceBased ? INVOKESPECIAL
                            : INVOKEVIRTUAL, writeInvokeTo);
                }

                /* Box the return value, and return it. */
                writeBoxInsn(writeInvokeTo, returnType.getSort());
                writeInvokeTo.visitInsn(ARETURN);
            });

            writeInvokeTo.visitMaxs(0, 0); // placeholder
            writeInvokeTo.visitEnd();

            super.visitEnd();
        }

        /**
         * Generates code to call <code>Object#clone</code> despite the fact
         * that it's a <code>protected</code> method. This uses methods of the
         * <code>Authorisation</code> object. The generated code assumes that
         * the Java stack will have an <code>Authorisation</code> on top and the
         * object to clone immediately beneath that; the caller may need to
         * write code to set up the stack accordingly.
         *
         * @param writeTo The <code>MethodVisitor</code> to write the generated
         * code into.
         */
        private void generateObjectCloneCode(MethodVisitor writeTo) {
            try {
                MethodReference getCloner = new MethodReference(
                        Authorisation.class.getMethod("getObjectCloner"));
                getCloner.generateCallInsn(INVOKEVIRTUAL, writeTo);

                /* We're calling the cloner with the object to clone, not vice
                   versa. */
                writeTo.visitInsn(SWAP);
                MethodReference useCloner = new MethodReference(
                        Authorisation.ObjectCloner.class.getMethod(
                                "cloneObject", Object.class));
                useCloner.generateCallInsn(INVOKEVIRTUAL, writeTo);

            } catch (NoSuchMethodException ex) {
                /* Shouldn't happen, as we control the classes whose methods
                   we're requesting. */
                throw new RuntimeException(ex);
            }
        }

        /**
         * Does nothing. This effectively means that objects that appear in the
         * original class being visited won't appear in the standin.
         *
         * @param access Ignored.
         * @param name Ignored.
         * @param desc Ignored.
         * @param signature Ignored.
         * @param value Ignored.
         * @return Always <code>null</code>.
         */
        @Override
        public FieldVisitor visitField(int access, String name, String desc,
                String signature, Object value) {
            return null;
        }

        /**
         * If the given method is a constructor, generates a new constructor
         * that delegates to the original constructor. Otherwise, the visited
         * method is deleted.
         *
         * @param access The access flags of the visited method. Will also be
         * used as the access flags for the new method.
         * @param name The name of the visited method.
         * @param desc The descriptor of the visited method.
         * @param signature The signature of the visited method.
         * @param exceptions The checked exceptions that the visited method can
         * throw.
         * @return A visitor that will be used to visit the method in more
         * detail, or <code>null</code> if the detail is not required.
         */
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                String signature, String[] exceptions) {
            /* The only time we care about the method that we read from the
               bytecode is if it's a constructor. Otherwise, don't do anything
               with the method we read. (We might well generate a method that
               delegates to it, but in that case we'll be using the version of
               the method accessed via Java reflection, rather than the version
               read from the class's bytecode. */
            if (!inheritanceBased || !name.equals("<init>")) {
                return null;
            }
            generateDelegatingMethod(null, name, desc, access, exceptions);

            /* We don't care about the old code implementing the method, as
               we're replacing and delegating to it anyway. Tell the caller
               that. */
            return null;
        }

        /**
         * Generates a method that conditionally delegates to the corresponding
         * method of the class we're generating a standin for. If not delegating
         * directly to the super method, we delegate to the standin storage
         * instead (which will in turn probably delegate to a location manager,
         * but that isn't relevant here).
         *
         * @param m The method being delegated to, if it is actually a method.
         * Should be <code>null</code> when delegating to a constructor.
         * @param name The name of the new method. Can be
         * <code>"&lt;init&gt;"</code> to generate a constructor that delegates
         * to a constructor.
         * @param desc The descriptor of the new method.
         * @param access The flags relating to the new method (public, final,
         * etc.).
         * @param exceptions The list of checked exceptions that the method can
         * throw. (Can be empty if the caller is unsure, because checked
         * exceptions aren't actually checked at runtime, only compile time.)
         */
        private void generateDelegatingMethod(Method m, String name,
                String desc, int access, String[] exceptions) {
            MethodReference delegateTo = m == null
                    ? new MethodReference(
                            oldClassName, false, name, desc, null)
                    : new MethodReference(m, about);
            Type methodType = Type.getMethodType(desc);
            Type[] arguments = methodType.getArgumentTypes();

            MethodVisitor writeNewMethodTo = super.visitMethod(
                    access, name, desc, null, exceptions);

            /* Generate names for the parameters to the new method. */
            int i = 1;
            for (Type argument : arguments) {
                writeNewMethodTo.visitParameter("arg" + i, ACC_FINAL);
                i++;
            }

            /* Generate code for calling the new method. */
            writeNewMethodTo.visitCode();

            /* Check the storage to see if we need to delegate in a more complex
               way. */
            if (!"<init>".equals(name)) {
                /* Load the storage twice (we're going to check the type of one
                   copy, and maybe invoke a method on the other). */
                writeNewMethodTo.visitVarInsn(ALOAD, 0);
                writeNewMethodTo.visitFieldInsn(GETFIELD,
                        inheritanceBased ? getGeneratedClassName()
                                : Type.getInternalName(IndirectStandin.class),
                        getStandinStorageFieldName(),
                        Type.getDescriptor(StandinStorage.class));
                writeNewMethodTo.visitInsn(DUP);

                /* Check to see if it's a ForwardingStandinStorage. If it isn't,
                   skip most of the rest of this block. */
                Label label = new Label();
                writeNewMethodTo.visitTypeInsn(INSTANCEOF,
                        Type.getInternalName(ForwardingStandinStorage.class));
                writeNewMethodTo.visitJumpInsn(IFEQ, label);

                /* We know it's a forwarding standin storage; cast to that. */
                writeNewMethodTo.visitTypeInsn(CHECKCAST,
                        Type.getInternalName(ForwardingStandinStorage.class));

                /* Calculate the method code. */
                int salt = db.getMethodCodeSalt();
                long methodCode = defaultMethodCode(m, salt);
                writeNewMethodTo.visitLdcInsn((Long) methodCode);

                /* Pack the arguments into an array. */
                writeNewMethodTo.visitLdcInsn((Integer) arguments.length);
                writeNewMethodTo.visitTypeInsn(ANEWARRAY,
                        Type.getInternalName(Object.class));

                /* We iterate over two indexes: i is the offset into the local
                   variables list (as before), j is the offset into the array
                   we're packing the arguments into. */
                i = 1;
                int j = 0;
                for (Type argument : arguments) {
                    /* Duplicate the array reference (one copy of the array
                       reference is consumed when we assign to it, so we need a
                       new copy to consume so that we can keep the original
                       around). */
                    writeNewMethodTo.visitInsn(DUP);

                    /* Load the array index. */
                    writeNewMethodTo.visitLdcInsn((Integer) j);

                    /* Load the argument. */
                    int opcode = argument.getOpcode(ILOAD);
                    writeNewMethodTo.visitVarInsn(opcode, i);

                    /* Make the argument an object. Autoboxing is syntactic
                       sugar; therefore, we have to "autobox" ourself. */
                    writeBoxInsn(writeNewMethodTo, argument.getSort());

                    /* Store into the array. */
                    writeNewMethodTo.visitInsn(AASTORE);

                    i += argument.getSize();
                    j++;
                }

                /* Call ForwardingStandinStorage#forwardMethodCall. */
                Method fmc;
                try {

                    fmc = ForwardingStandinStorage.class.getMethod(
                            "forwardMethodCall", long.class, Object[].class);
                } catch (NoSuchMethodException | SecurityException ex) {
                    /* Should never happen; ForwardingStandinStorage is our own
                       class and we can determine what methods it has. */
                    throw new RuntimeException(ex);
                }
                MethodReference fmcr = new MethodReference(fmc);
                fmcr.generateCallInsn(INVOKEVIRTUAL, writeNewMethodTo);

                /* Return the returned value. */
                writeUnboxInsn(writeNewMethodTo, methodType.getReturnType());

                int opcode = methodType.getReturnType().getOpcode(IRETURN);
                writeNewMethodTo.visitInsn(opcode);

                /* Non-forwarding storage handling: drop the copy of the
                   StandinStorage object (we don't need it), and place a jump
                   target before that so that we can jump here if we discover
                   that no forwarding is necessary. */
                writeNewMethodTo.visitLabel(label);
                writeNewMethodTo.visitInsn(POP);
            }

            if (!inheritanceBased) {
                /* With a composition-based standin, the new method is called on
                   <code>this.referent</code>. */
                writeNewMethodTo.visitVarInsn(ALOAD, 0);
                writeNewMethodTo.visitFieldInsn(GETFIELD,
                        Type.getInternalName(IndirectStandin.class), "referent",
                        Type.getDescriptor(Object.class)
                );
                /* IndirectStandin.referent has type T (erased to Object); we
                   need to cast it back to a T. */
                writeNewMethodTo.visitTypeInsn(CHECKCAST, oldClassName);
            } else {
                /* With an inheritance-based standin, the new method is called
                   on <code>this</code>. */
                writeNewMethodTo.visitVarInsn(ALOAD, 0);
            }

            i = 1;
            for (Type argument : arguments) {
                /* Find the appropriate opcode for loading this argument. */
                int opcode = argument.getOpcode(ILOAD);
                writeNewMethodTo.visitVarInsn(opcode, i);
                i += argument.getSize();
            }

            /* Actually call the original method or constructor. */
            if (!inheritanceBased && m != null
                    && m.getDeclaringClass().equals(Object.class)
                    && m.getName().equals("clone")
                    && m.getParameterCount() == 0) {
                /* A special case, as we don't have visibility to call
                   Object#clone. We also can't get help from classes with more
                   permissions as this sort of call-through-wrapper doesn't use
                   an authorisation (as it could come from anywhere). For the
                   time being, just return the object itself (i.e. a no-op
                   amount of code) and hope.

                   TODO: Maybe this needs an exception, or to call a static
                   method, or something like that. */
            } else {
                delegateTo.generateCallInsn(inheritanceBased
                        ? INVOKESPECIAL : INVOKEVIRTUAL,
                        writeNewMethodTo);
            }

            /* If this is a constructor, we need to initialise the standin
               storage with a TrivialStandinStorage. */
            if ("<init>".equals(name)) {
                /* We're eventually going to write into a field of this.
                   Because the JVM is stack-based, we're using reverse-Polish,
                   meaning that the way in which things are grouped is
                   determined by the order of opcodes; this one happens to need
                   to come first. */
                writeNewMethodTo.visitVarInsn(ALOAD, 0);
                writeNewMethodTo.visitTypeInsn(NEW,
                        Type.getInternalName(TrivialStandinStorage.class));
                /* The newly created object reference gets consumed by its
                   constructor (i.e. the constructor doesn't return the
                   reference back to us), so we have to copy the reference so
                   that we can store it after construction. */
                writeNewMethodTo.visitInsn(DUP);
                MethodReference tssConstructor;
                try {
                    tssConstructor = new MethodReference(
                            TrivialStandinStorage.class.getConstructor());
                } catch (NoSuchMethodException | SecurityException ex) {
                    /* This should never happen, because TrivialStandinStorage
                       has a no-arg constructor, so if it goes missing for some
                       reason we can safely throw an unhandled exception. */
                    throw new RuntimeException(ex);
                }
                tssConstructor.generateCallInsn(INVOKESPECIAL, writeNewMethodTo);
                writeNewMethodTo.visitFieldInsn(PUTFIELD,
                        getGeneratedClassName(), getStandinStorageFieldName(),
                        Type.getDescriptor(StandinStorage.class));
            }

            /* Return its return value. */
            int opcode = methodType.getReturnType().getOpcode(IRETURN);
            writeNewMethodTo.visitInsn(opcode);

            /* Placeholder value for the new method's stack size. This is meant
               to be replaced some time downstream. */
            writeNewMethodTo.visitMaxs(0, 0);
            /* We're done writing the method. */
            writeNewMethodTo.visitEnd();
        }

        /**
         * Writes one or more Java bytecode instructions that boxes the given
         * sort of value into an Object. The value will be assumed to be at the
         * top of the stack. As an exception, a <code>void</code> value will be
         * assumed to take up no stack slots, but box into <code>null</code>
         * which takes up one stack slot.
         *
         * @param writeCodeTo A MethodVisitor to which to write the
         * instructions.
         * @param sort The sort of value to box (as returned by
         * <code>Type#getSort()</code>.
         */
        private void writeBoxInsn(MethodVisitor writeCodeTo, int sort) {
            switch (sort) {
                case ARRAY:
                case METHOD:
                case OBJECT:
                    // nothing to do
                    break;
                case VOID:
                    writeCodeTo.visitInsn(ACONST_NULL);
                    break;
                default:
                    Class<?> boxInto = autoboxClass(sort);
                    /* Look for a valueOf method on the autobox class; that's
                       how you box a value. */
                    for (Method m : boxInto.getMethods()) {
                        if (m.getName().equals("valueOf")
                                && isStatic(m.getModifiers())
                                && m.getParameterCount() == 1
                                && m.getParameters()[0].getType().isPrimitive()) {
                            MethodReference mr = new MethodReference(m);
                            mr.generateCallInsn(INVOKESTATIC,
                                    writeCodeTo);
                            return;
                        }
                    }

                    throw new RuntimeException("Could not find " + boxInto
                            + ".valueOf()");
            }
        }

        /**
         * Writes one or more Java bytecode instructions that unboxes the given
         * type of value from an Object. The value will be assumed to be at the
         * top of the stack. If the value is a primitive, it will be unboxed
         * using the appropriate unboxing method; if it is an object, it will be
         * cast into the given type. "Unboxing into <code>void</code>" actually
         * just pops the top of the stack.
         *
         * @param writeCodeTo A MethodVisitor to which to write the
         * instructions.
         * @param type The Type of value to unbox.
         */
        private void writeUnboxInsn(MethodVisitor writeCodeTo, Type type) {
            int sort = type.getSort();
            if (sort == ARRAY || sort == METHOD
                    || sort == OBJECT) {
                writeCodeTo.visitTypeInsn(CHECKCAST,
                        type.getInternalName());
            } else {
                Class<?> unboxFrom = autoboxClass(sort);
                /* Note: when unboxing a Void, don't bother casting it; it's
                   supposed to be null anyway, and not reading the value at all
                   is more in the spirit of void values than trying to verify
                   that it's null (which is what a cast to Void effectively
                   does). */
                if (sort != VOID) {
                    writeCodeTo.visitTypeInsn(CHECKCAST,
                            Type.getInternalName(unboxFrom));
                }

                String unboxMethodName;

                switch (sort) {
                    case VOID:
                        writeCodeTo.visitInsn(POP);
                        return;

                    case BOOLEAN:
                        unboxMethodName = "booleanValue";
                        break;
                    case BYTE:
                        unboxMethodName = "byteValue";
                        break;
                    case CHAR:
                        unboxMethodName = "charValue";
                        break;
                    case DOUBLE:
                        unboxMethodName = "doubleValue";
                        break;
                    case FLOAT:
                        unboxMethodName = "floatValue";
                        break;
                    case INT:
                        unboxMethodName = "intValue";
                        break;
                    case LONG:
                        unboxMethodName = "longValue";
                        break;
                    case SHORT:
                        unboxMethodName = "shortValue";
                        break;
                    default:
                        throw new RuntimeException(
                                "cannot determine unbox method for sort "
                                + sort);
                }

                Method unbox;
                try {
                    unbox = unboxFrom.getMethod(unboxMethodName);
                } catch (NoSuchMethodException | SecurityException ex) {
                    /* We shouldn't have methods missing from the Java API
                       classes! */
                    throw new RuntimeException(ex);
                }

                MethodReference unboxRef = new MethodReference(unbox);
                unboxRef.generateCallInsn(INVOKEVIRTUAL, writeCodeTo);
            }
        }

        /**
         * Calculates what sort of Java object an object of the given sort will
         * autobox into, in order to store it in an Object.
         *
         * @param sort A primitive Java type represented as an ASM type sort,
         * e.g. Type.INT, Type.LONG, etc..
         * @return The class into which the object autoboxes.
         */
        private Class<?> autoboxClass(int sort) {
            switch (sort) {
                case BOOLEAN:
                    return Boolean.class;
                case BYTE:
                    return Byte.class;
                case CHAR:
                    return Character.class;
                case DOUBLE:
                    return Double.class;
                case FLOAT:
                    return Float.class;
                case INT:
                    return Integer.class;
                case LONG:
                    return Long.class;
                case SHORT:
                    return Short.class;
                case VOID:
                    return Void.class;
                default:
                    throw new IllegalArgumentException(
                            "type sort " + sort + " is not a primitive");
            }
        }
    }

    /**
     * The debug listener in use.
     */
    private static DebugListener listener = null;

    /**
     * Sets a debug listener for this standin generator. The bytecode of each
     * generated standin will be sent to the debug listener, allowing it to be
     * saved for future inspection. A newly set debug listener will override any
     * previously set debug listener.
     * <p>
     * Note that this method will only be called when the class is generated via
     * a <code>ClassWriter</code>; otherwise, the bytecode will not be
     * accessible.
     * <p>
     * This method is not thread-safe and should not be called while standin
     * generation is ongoing.
     *
     * @param newListener The new debug listener, or <code>null</code> to remove
     * the debug listener.
     */
    public static void setListener(DebugListener newListener) {
        listener = newListener;
    }

    /**
     * A listener that's informed whenever a new standin is generated. This can
     * be used to debug runtime-generated standins.
     * <p>
     * Note that because <code>generate()</code> is call-by-name, the resulting
     * bytecode might not be capturable if it was called with an unusual type of
     * <code>ClassVisitor</code>, in which case the debug listener will not run.
     * The most common sort, <code>ClassWriter</code>, will work just fine.
     */
    @FunctionalInterface
    public interface DebugListener {

        /**
         * Called whenever a class was generated.
         *
         * @param generatedBytecode The bytecode of the generated class.
         * @param name The name of the generated class (in Java source format,
         * including package names).
         */
        void classGenerated(byte[] generatedBytecode, String name);
    }
}
