package xyz.acygn.millr;

import java.io.*;

import xyz.acygn.millr.messages.MessageUtil;
import xyz.acygn.millr.messages.NoClassesToMillException;

import java.util.*;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
//import org.apache.tools.ant.AntClassLoader;

import xyz.acygn.millr.CoreArrayAnalysis.CoreArrayAnalysis;
import xyz.acygn.millr.messages.NoSuchClassException;
import xyz.acygn.millr.Reason;

/**
 * The main class of millr
 *
 * @author Thomas Cuvillier, Marcello De Bernardi
 */
public class Mill {

    // millr instance, i/o util instance, core array analysis instance
    private static Mill millr;
    private InputOutput inputOutput;
    private CoreArrayAnalysis coreArrayAnalysis;
    private boolean testingMode;
    // the set of sub-projects that will be milled
    private final List<SubProject> subProjectsToMill;
    // approximately reflects all the classes read by millr, mostly used for transformations
    // that need to be global (i.e. cannot be executed on sub-projects individually)
    private Collection<ClassReader> classReaders;
    // tracks which sub-projects each class being milled belongs to
    private final Map<String, SubProject> fromClassNameToProject;

    private final Map<String, Set<Reason>> unsafeClassForRemote;


    // DYNAMIC INITIALIZER
    {
        inputOutput = InputOutput.getInstance();
        subProjectsToMill = new ArrayList<>();
        classReaders = Collections.synchronizedList(new ArrayList<ClassReader>());
        fromClassNameToProject = new HashMap<>();
        coreArrayAnalysis = new CoreArrayAnalysis();
        unsafeClassForRemote = new HashMap<>();
    }


     final void addReasonForUnsafe(String className, Reason reason){
        synchronized (unsafeClassForRemote) {
            if (unsafeClassForRemote.get(className) != null) {
                unsafeClassForRemote.get(className).add(reason);
            } else {
                unsafeClassForRemote.put(className, new HashSet<Reason>(Collections.singleton(reason)));
            }
        }
    }

    /**
     * Private constructor for Mill. Mill is a singleton and cannot be
     * instantiated directly. The only access point to millr from outside the
     * {@link xyz.acygn.millr} package is the {@code main()} method in this
     * class.
     * <p>
     * Only code within the {@link xyz.acygn.millr} package is allowed to access
     * the singleton instance directly via the {@code getInstance()} method.
     */
    private Mill() {
    }

    /**
     * Private constructor for Mill, which creates a testing singleton for the
     * class. The testing singleton has all of its state initialized.
     *
     * @param dummyInputLocation file location for dummy state
     */
    private Mill(String dummyInputLocation) {
        try {
            inputOutput.readInputLine("-i", dummyInputLocation, "-o", "./dummy_output", "-f");
            inputOutput.resolveOutputConflicts(inputOutput.getInputLocations(), inputOutput.getOutputDirectory());

            inputOutput.getOutputDirectory().deleteOnExit();

//            MessageUtil.message("[INPUT LOCATIONS] -> " + Arrays.toString(inputOutput.getInputLocations().toArray())).emit();
//            MessageUtil.message("[OUTPUT LOCATION] -> " + inputOutput.getOutputDirectory() + "\n").emit();

            inputOutput.mkdirs(inputOutput.getTempDirectory(), inputOutput.getOutputDirectory());
            subProjectsToMill.addAll(InputOutput.constructSubprojects(inputOutput.getInputLocations()));
            inputOutput.checkClassNameConflicts(subProjectsToMill);

            for (SubProject sp : subProjectsToMill) {
                sp.CollectionClassReader.forEach(e -> fromClassNameToProject.put(e.getClassName(), sp));
                classReaders.addAll(sp.CollectionClassReader);
            }

            if (inputOutput.shouldPerformExtendedAnalysis())
                coreArrayAnalysis.coreArrayInitialize(subProjectsToMill);
        } catch (Exception e) {
            MessageUtil.error(e).report("Error while creating millr test instance").terminate();
        }
    }


    /**
     * The main function. Reads user inputs, initializes millr variables, and
     * performs all required transformations sequentially.
     *
     * @param args the command-line arguments. Currently supports the following options:
     *             -h:         help (prints this message, if present all other arguments are ignored)
     *             -i:         input (flags the successive arguments as input locations)
     *             -o:         output (flags the successive argument as the output location)
     *             -f:         force (overwrite output files if they exists)
     *             -w:         wipe (deletes and recreates the output folder before milling)
     *             -v:         verbose (verbose output mode)
     *             -s_warn:    suppress warnings (no warnings printed)
     *             -s_mess:    suppress messages (no messages printed)
     *             -s_all:     suppress all (only errors printed)
     *             -extendedAnalyse: Carry an analysis on the non-milled classes to detected potential problems.
     *             <p>
     *             Millr's argument format is thus as follows:
     *             -i {path}+ {-o path}? {-f}? {-h}? {-v}?
     */
    public static void main(String[] args) {
        millr = new Mill();
        millr.run(args);
    }

    /**
     * Provides access to the existing singleton instance of Mill. This method
     * is for internal use of the {@link xyz.acygn.millr} package only, and its
     * access modifier should not be loosened.
     *
     * @return the current instance of millr if it exists
     */
    static Mill getInstance() {
        if (millr == null) {
            throw new UnsupportedOperationException("Attempting to access millr instance using getInstance "
                    + "before having started millr from Mill.main(), or before creating a testing instance.");
        }

        return millr;
    }

    /**
     * Creates a singleton instance of the Mill class to a "dummy" instance suitable for unit
     * testing of specific methods.
     */
    static Mill createTestInstance(String dummyInputLocation) {
        millr = new Mill(dummyInputLocation);

        return millr;
    }


    /**
     * Return the class reader for the name given, if found. We first look
     * through our own collection of class readers for one with the appropriate
     * name, so that it always return the most up-to-date version, and resolves
     * to use the default mechanism if the previous method fails.
     *
     * @param internalName The internal name of the class.
     * @return The most up-to-date version of the ClassReader if this one is
     * being transformed, the normal one otherwise.
     * @throws IOException If no classes for the specified internal name can be
     *                     found.
     */
    ClassReader getClassReaderInstance(String internalName) throws NoSuchClassException {
        Optional<ClassReader> ocr = classReaders.stream().filter(e -> e.getClassName().equals(internalName)).findAny();
        if (ocr.isPresent()) {
            return ocr.get();
        }
        try {
            return new ClassReader(internalName);
        } catch (IOException ex) {
            try {
                return new ClassReader(Mill.class.getClassLoader().getResourceAsStream(internalName + ".class"));
            } catch (IOException exx) {
                ocr = subProjectsToMill
                        .parallelStream()
                        .flatMap(e -> e.CollectionClassReader.parallelStream())
                        .filter(e -> e.getClassName().equals(internalName))
                        .findAny();
                if (ocr.isPresent()){
                    return ocr.get();
                }
                else{
                    throw new NoSuchClassException(internalName);
                }
            }


        }
    }

    /**
     * Update the Collection of Class Readers and returns it.
     *
     * @return The collection of class reader being milled by the project.
     */
    Collection<ClassReader> getAllClassReaders() {
        synchronized (classReaders) {
            // updateGlobalCollection();
            return classReaders;
        }
    }

    /**
     * Entry point for optional extended analysis of arrays.
     *
     * @param opcode     opcode
     * @param nameClass  name of the class the method is in
     * @param nameMethod name of the method to analyze
     * @param descMethod method description string
     * @throws ClassNotFoundException if class with given identification is not found
     */
    void analyzeMethodCall(int opcode, String nameClass, String nameMethod, String descMethod) throws ClassNotFoundException {
        if (inputOutput.shouldPerformExtendedAnalysis()) {
            coreArrayAnalysis.analyzeMethodCall(opcode, nameClass, nameMethod, descMethod);
        }
    }


    private void run(String[] args) {
        // CLI ARGUMENT PROCESSING PHASE: read arguments from user and check validity
        try {
            inputOutput.readInputLine(args);
            inputOutput.resolveOutputConflicts(inputOutput.getInputLocations(), inputOutput.getOutputDirectory());

            // inform user of locations being milled
            MessageUtil.message("[INPUT LOCATIONS] -> " + Arrays.toString(inputOutput.getInputLocations().toArray())).emit();
            MessageUtil.message("[OUTPUT LOCATION] -> " + inputOutput.getOutputDirectory() + "\n").emit();
        } catch (IllegalArgumentException | IOException e) {
            MessageUtil.error(e).report().terminate();
        }

        // INITIALIZATION PHASE: create temp folders and initialize global variables
        try {
            inputOutput.mkdirs(inputOutput.getTempDirectory(), inputOutput.getOutputDirectory());
            subProjectsToMill.addAll(InputOutput.constructSubprojects(inputOutput.getInputLocations()));
            inputOutput.checkClassNameConflicts(subProjectsToMill);
            for (SubProject sp : subProjectsToMill) {
                sp.checkAndRemoveNulls();
                sp.checkClassReader();
                sp.checkConsistencyFromNameToPaths();
            }

            // create a global collection of ClassReader, of ClassNames, and a map assigning a
            // ClassReader to a project. Indeed, two transformations need to be applied to the
            // set of projects collectively: the visibility transformation, and the
            // implementMilledTransformation.
            for (SubProject sp : subProjectsToMill) {
                classReaders.addAll(sp.CollectionClassReader);
            }

            if (classReaders.isEmpty()) {
                throw new NoClassesToMillException("Millr has found no classes to mill, and must abort.");
            }

            if (inputOutput.shouldPerformExtendedAnalysis()) {
                coreArrayAnalysis.coreArrayInitialize(subProjectsToMill);
            }
        } catch (Exception e) {
            while (true) if (InputOutput.recursiveDelete(inputOutput.getTempDirectory())) break;

            MessageUtil.error(e).report().terminate();
        }

        // TRANSFORMATION PHASE: carry out transformations
        try {
            MessageUtil.message("[PROJECT INITIALIZED]").emit();

            ClassDataBase.setConstructThoroughDatabase(true);

            // [1/7] lambdas transformation
            for (SubProject sp : subProjectsToMill) {
                new LambdasTransformation(sp).carryTransformation();
            }
            updateGlobalCollection();
            MessageUtil.message("[1/7] -> Lambdas transformation complete", true).emit();

            for (SubProject sp : subProjectsToMill){
                sp.CollectionClassReader.forEach(e -> fromClassNameToProject.put(e.getClassName(), sp));
            }


            // First, we start with the visibility transformation, since it needs to be done
            // to all the sub-projects at once.
            VisibilityTransformation vt = new VisibilityTransformation(classReaders);
            vt.carryTransformation();

            ClassDataBase.setConstructThoroughDatabase(false);

            // Starting from here, the two next transformations, lambdas and ArrayRewrite, might
            // create new additional files. Therefore, we separated the project into sub-projects.

            //Starting from here, we can rely on keep the projects separated.
            for (SubProject sp : subProjectsToMill) {
                sp.CollectionClassReader.clear();
            }
            for (ClassReader cr : classReaders) {
                ((SubProject) fromClassNameToProject.get(cr.getClassName())).CollectionClassReader.add(cr);
            }

            for (SubProject sp : subProjectsToMill) {
                new VisibilityChangeProtectedTransformation(sp, vt.getMethodToChangeToProtected()).carryTransformation();
            }
            updateGlobalCollection();
            MessageUtil.message("[2/7] -> Visibility transformation complete", true).emit();




            for (SubProject sp : subProjectsToMill) {
                sp.checkConsistencyFromNameToPaths();
            }


            // We need to get back the version of class files to java8, because of getter-setter for interfaces.
            for (SubProject sp : subProjectsToMill) {
                new ToJavaEightTransfo(sp).carryTransformation();
            }
            updateGlobalCollection();




            // [4/7] array transformation
            for (SubProject sp : subProjectsToMill) {
                new ArrayRewriteTransformation(sp).carryTransformation();
            }
            updateGlobalCollection();
            MessageUtil.message("[3/7] -> Array transformation complete", true).emit();

            // [5/7] introspection transformation
            for (SubProject sp : subProjectsToMill) {
                new IntrospectionTransformation(sp).carryTransformation();
            }
            updateGlobalCollection();
            MessageUtil.message("[4/7] -> Introspection transformation complete", true).emit();

            // [6/7] synchronized transformation
            for (SubProject sp : subProjectsToMill) {
                new SynchronizedTransformation(sp).carryTransformation();
            }
            updateGlobalCollection();
            MessageUtil.message("[5/7] -> Synchronized transformation complete", true).emit();

            //  [7/7] get/set transformation
            for (SubProject sp : subProjectsToMill) {
                new GetSetTransformation(sp).carryTransformation();
            }
            updateGlobalCollection();
            MessageUtil.message("[6/7] -> Get/Set transformation complete", true).emit();

            // [2/7] millr implementation transformation
            for (SubProject sp : subProjectsToMill) {
                new ImplementMilledClassTransformation(sp).carryTransformation();
            }
            updateGlobalCollection();
            MessageUtil.message("[7/7] -> Millr implementation transformation complete", true).emit();


            // write transformed classes to files
            for (SubProject sp : subProjectsToMill) {
                sp.writeClassWriter();
            }
        } catch (Exception e) {
            MessageUtil.error(e).report();
        } finally {
            while (true) if (InputOutput.recursiveDelete(inputOutput.getTempDirectory())) break;
        }

    }

    /**
     * Update the classReaders from the set of collections of ClassReader from
     * all the SubProjects that are being milled.
     */
    private void updateGlobalCollection() {
        classReaders.clear();
        for (SubProject sp : subProjectsToMill) {
            classReaders.addAll(sp.CollectionClassReader);
        }
    }

    static ClassReader getClassReader(String name) {
        try {
            return Mill.getInstance().getClassReaderInstance(name);
        } catch (Throwable t) {
            try {
                return new ClassReader(name);
            } catch (IOException ex) {
                throw new NoSuchClassException(name);
            }

        }
    }


}
