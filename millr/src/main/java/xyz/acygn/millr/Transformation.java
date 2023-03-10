/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import xyz.acygn.millr.messages.MessageUtil;
import xyz.acygn.millr.messages.TransformationFailedException;

/**
 * Class for the transformations applied by millr. Each of these transformations
 * must be applied by calling the methods in the following order: (initialize.
 * applyTransformation. getClassWriter)
 *
 * @author thomasc
 */
public abstract class Transformation {
    /**
     *
     */
    public Transformation(SubProject sp) {
        this();
        carryConstruction(sp);
    }

    /**
     * This constructor should only be used if a carryConstruction is called right after !
     * This delayed constructor allows us to pass more arguements to the transformation, by initializing
     * variables before calling it.
     */
    Transformation() {
    }

    public void carryConstruction(SubProject sp) {
        carryConstruction(sp.CollectionClassReader);
        this.sp = sp;
    }

    public void carryConstruction(Collection<ClassReader> listClassReader) {
        this.listClassReader = listClassReader;
        listTransformation = listClassReader.parallelStream().map(e -> getNewInstance(e)).collect(Collectors.toList());
        isTransformationApplied = false;
    }


    public void carryTransformation() {
        applyTransformation();
        listClassReader.clear();
        listClassReader.addAll(getClassWriter().parallelStream().map(e -> new ClassReader(e.toByteArray())).collect(Collectors.toSet()));
        if (sp != null) {
            sp.updateCollectionClassName();
            sp.checkConsistencyFromNameToPaths();
            // Normally the ClassReaders have already been checked at the end of the transformation, and hence this
            // double check is useless.
            sp.checkClassReader();
        }
    }

    /**
     * Each instance of the Transformation class will deal with a single file
     * individually, but the Transformation has to be fed with the whole list of
     * files to be transformed at once, since modifying a class may involve
     * modifying other classes that refer to that class. Hence we will create a
     * list of instances transformations, one per class file.
     */
    protected List<Instance> listTransformation;

    /**
     * Return the name of the transformation. Helps for error messages.
     */
    abstract String getNameOfTheTransformation();


    /**
     * The collection of ClassReader the transformation will be operating on.
     */
    Collection<ClassReader> listClassReader;


    /**
     * In case we apply the transformation to a subproject, we may want to store it in a field.
     */
    SubProject sp;

    /**
     * Create a new Transformation object by taking as input a collection of
     * ClassReader whose classes will be transformed, and assign to each
     * ClassReader a new transformation instance.
     *
     * @param listClassReader
     */
    public Transformation(Collection<ClassReader> listClassReader) {
        this();
        carryConstruction(listClassReader);
    }

    /**
     * Not yet implemented: In the future Millr should be able to reference the
     * classes that may cause problems and inform the user on what the issue
     * might be without necesseraly correcting it. The default way of doing it
     * is to call the checkCompatibility method on each instance of the
     * transformation Class.
     */
    public void checkProgramCompatibility() {
        listTransformation.parallelStream().forEach(Instance::checkCompatibility);
    }

    boolean isTransformationApplied;

    /**
     * Apply the transformation to the ClassReaders by going through each
     * Instance and applying the transformation implemented by this instance. If
     * one of the transformation fails it just emit an error and move on to the
     * next one.
     * <p>
     * By default, we use a concurrent stream here, so concurrency should be taken into account.
     */
    private void applyTransformation() {
        isTransformationApplied = true;
       listTransformation.parallelStream().forEach(instance -> {
            try {
                instance.applyTransformation();
                MessageUtil.message(getNameOfTheTransformation() + " done for " + instance.cr.getClassName()).emit();
            } catch (Throwable e) {
                MessageUtil.error(e).report().resume();
            }
       });
    }

    /**
     * Return the collection of ClassWriter corresponding to the result of the
     * transformation: should only be call after applyTransformation. The
     * default behaviour is to collect the ClassWriters from resulting from each
     * Instance of the transformation in a set. If called before
     * applyTransformation, it will log an error and return a set of ClassWriter
     * corresponding to the collection of ClassReader feeds as input.
     *
     * @return A collection of ClassWriters resulting from the Transformation.
     */
    public Collection<ClassWriter> getClassWriter() {
        if (!isTransformationApplied) {
            MessageUtil
                    .error(
                            new RuntimeException("The transformation " + getNameOfTheTransformation() + " "
                                    + "has not been applied before trying to get the "
                                    + "result from it: We are returning ClassWriters "
                                    + "corresponding to the ClassReader from the input."))
                    .report()
                    .resume();
            return listClassReader.parallelStream().map(InputOutput::fromReaderToWriter).collect(Collectors.toSet());
        }
        return listTransformation.parallelStream().map(e -> e.getClassWriters()).flatMap(l -> l.stream()).collect(Collectors.toSet());

    }

    /**
     * Create a new Instance of the transformation for a given ClassReader.
     *
     * @param cr The ClassReader we want to Transform.
     * @return The Instance of Transformation that will carry the Transformation
     * of this ClassReader.
     */
    abstract Instance getNewInstance(ClassReader cr);

    /**
     * An instance is a Class carrying a given transformation on a ClassReader
     * and resulting in a ClassWriter; in the future, it might be able to check
     * the class coming from the ClassReader to see if a transformation is
     * needed without neccessarily doing it. For the transformation to happen,
     * the following order should be respected: Calling the constructor,
     * applyTransformation, getClassWriter.
     */
    abstract class Instance {

        /**
         * The ClassReader on which the transformation will happen.
         */
        ClassReader cr;

        /**
         * The ClassVisitor doing carrying the transformation.
         */
        abstract ClassVisitor getClassTransformer();

        /**
         * The ClassWriter whose job is to output the result of the
         * transformation.
         */
        abstract ClassWriter getClassWriter();

        /**
         * A ClassVisitor checking the ClassReader to see if the Transformation
         * is needed but not carrying it.
         */
        abstract ClassVisitor getClassVisitorChecker();

        /**
         * Record if the transformation has been applied successfully or not.
         */
        boolean isTransformationApplied;

        /**
         * Construct a new Instance from a given ClassReader.
         *
         * @param cr The ClassReader from which the Instance of this
         *           Transformation will operate.
         */
        Instance(ClassReader cr) {
            this.cr = cr;
        }


        /**
         * Handle a message if there is a problem that the transformation needs
         * to solve.
         *
         * @param problem
         */
        void printIncompatibilityMessage(String problem) {
            String name = cr.getClassName();
            MessageUtil.message(name + "might cause compatibility problems since" + problem).emit();
        }

        /**
         * Check if the transformation is needed and emits messages about the
         * possible issues.
         */
        void checkCompatibility() {
            cr.accept(getClassVisitorChecker(), ClassReader.EXPAND_FRAMES);
        }

        /**
         * Apply the transformation on the {@link ClassReader} by feeding it to the
         * {@link ClassVisitor}. If it results in an exception, an error message is
         * emitted, informing the user on which class failed and why.
         * We also feed the result of the transformation to a CheckClassAdapter, to ensure its
         * consistency. If this detects an error, we assume the transformation fails.
         */
        void applyTransformation() throws TransformationFailedException {
            try {
                cr.accept(getClassTransformer(), ClassReader.EXPAND_FRAMES);
                classTransformerToClassWriter();
                ClassReader crTwo = InputOutput.fromWriterToReader(getClassWriter());
                CheckClassAdapter checkTransfoVisitor = new CheckClassAdapter(new ClassWriter(0) {
                });
               crTwo.accept(checkTransfoVisitor, ClassReader.EXPAND_FRAMES);
            } catch (Throwable t) {
               // Arrays.stream(((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()).forEach(e->System.err.println(e.getFile()));
                throw new TransformationFailedException(t, t.getMessage(), getNameOfTheTransformation(), cr);
            }
            isTransformationApplied = true;
        }


        /**
         * Action to transform the classTransformer into a (set of) ClassWriter();
         * Tipically, if the ClassTransformer is a ClassVisitor, does nothing.
         * In the case where it is a ClassNode, does cv.accept(cw).
         */
        abstract void classTransformerToClassWriter();


        /**
         * Return the ClassWriter(s) the transformation result in. Default
         * behaviour is to return the unique ClassWriter the ClassVisitor
         * resulted in. If called before the transformation has been applied, or
         * if the transformation failed, then it returns the ClassWriter
         * corresponding to the ClassReader, and emmits a warning.
         *
         * @return The set of ClassWriter that the transformation generates.
         */
        Set<ClassWriter> getClassWriters() {
            if (!isTransformationApplied) {
//                MessageUtil
//                        .error(
//                                new RuntimeException("The transformation " + this.getClass().getName() + " "
//                                        + "has not been applied or resulted in an error on "
//                                        + cr.getClassName() + ". \n"
//                                        + "we are returning the ClassWriter corresponding to "
//                                        + "the ClassReader from the input."))
//                        .report()
//                        .resume();
                return Collections.singleton(InputOutput.fromReaderToWriter(cr));
            }
            return Collections.singleton(getClassWriter());
        }

    }

}
