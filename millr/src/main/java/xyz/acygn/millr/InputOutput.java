package xyz.acygn.millr;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import xyz.acygn.millr.messages.ClassNotLoadedException;
import xyz.acygn.millr.messages.DuplicateClassNameException;
import xyz.acygn.millr.messages.MessageUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import xyz.acygn.mokapot.util.Pair;

/**
 * This class represents the inputs provided to the program by the user. The
 * class provides access to static utility methods for dealing with inputs
 *
 * @author Marcello De Bernardi, Thomas Cuvillier
 */
public class InputOutput {

    // user input processing
    private static final String INPUT_FLAG = "-i";
    private static final String OUTPUT_FLAG = "-o";
    private static final String OVERWRITE_OUTPUT_FLAG = "-f";
    private static final String WIPE_OUTPUT_FLAG = "-w";
    private static final String HELP_FLAG = "-h";
    private static final String VERBOSE_FLAG = "-v";
    private static final String SUPPRESS_WARNINGS_FLAG = "-s_warn";
    private static final String SUPPRESS_MESSAGES_FLAG = "-s_mess";
    private static final String SUPPRESS_ALL_FLAG = "-s_all";
    private static final String EXTENDED_ANALYSIS = "-extendedAnalyse";
    private static final String JAVAC_FLAG = "-javac";
    private static final String JAVAC_END_FLAT = "-endjavac";
    private static final String HELP_STRING
            = "Use millr to pre-process individual .class files, .jar applications, "
            + "or folders containing .class files,\ninto a form that conforms to mokapot's limitations. "
            + "When invoking millr in any preferred way\n(.jar file, ant script, etc), the following CLI arguments "
            + "are accepted:\n\n"
            + "-h:         help (prints this message, if present all other arguments are ignored)\n"
            + "-javac:     compile java classes before milling them, all options following that would be pass as options" +
            "to the javac command tool \n"
            + "-endjavac:    all options following that will be pass as options to Millr"
            + "-i:         input (flags the successive arguments as input locations)\n"
            + "-o:         output (flags the successive argument as the output location)\n"
            + "-f:         force (overwrite output files if they exists)\n"
            + "-w:         wipe (deletes and recreates the output folder before milling)\n"
            + "-v:         verbose (verbose output mode)\n"
            + "-s_warn:    suppress warnings (no warnings printed)\n"
            + "-s_mess:    suppress messages (no messages printed)\n"
            + "-s_all:     suppress all (only errors printed)\n"
            + "-extendedAnalyse: Carry an analysis on the non-milled classes to detected potential problems\n"
            + "Millr's argument format is thus as follows:\n\n"
            + "-i {path}+ {-o path}? {-f}? {-w}? {-h}? {-v}? ... {-extendedAnalyse}? \n\n"
            + "The only compulsory argument is -i, followed by one or more input locations. The order of the blocks\n"
            + "is not important, with exception of the fact that the input and output locations must immediately\n"
            + "follow the respective flags -i and -o. An output location is optional, but at most one can be given.";
    // method name generation
    private static final String VALID_CHARACTER_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    // singleton instance
    private static InputOutput instance;

    // name generation
    private Random random;
    private Set<String> previouslyGeneratedMethodNames;
    // files and directories
    private final File millrTempFolder;
    private Set<File> inputPaths;
    private File outputPath;
    // subprojects and classpaths
    private Set<File> pathLoaded;
    // cli options
    private boolean wipeOutput;
    private boolean overWrite;
    private boolean extendedAnalysis;

    /**
     * Private singleton constructor.
     */
    private InputOutput() {
        random = new Random();
        previouslyGeneratedMethodNames = new HashSet<>();
        millrTempFolder = createMillrTempFolder();

        inputPaths = new HashSet<>();
        outputPath = new File("output");
        pathLoaded = new HashSet<>();

        // default cli options
        wipeOutput = false;
        overWrite = false;
        extendedAnalysis = false;
    }

    /**
     * Returns a reference to the singleton instance of this class.
     *
     * @return {@link InputOutput} singleton
     */
    public static InputOutput getInstance() {
        if (instance == null) {
            instance = new InputOutput();
        }
        return instance;
    }


    public static ClassReader fromWriterToReader(ClassWriter cw) {
        return fromWriterToReader(Collections.singleton(cw)).iterator().next();
    }

    /**
     * Method taking a collection of ClassWriter, and returning a collection of
     * ClassReader that read them.
     *
     * @param classWriterCollection A collection of ClassWriter.
     * @return A collection of ClassReader reading them.
     */
    public static Collection<ClassReader> fromWriterToReader(Collection<ClassWriter> classWriterCollection) {
        return classWriterCollection.stream()
                .map(e -> new ClassReader(e.toByteArray()))
                .collect(Collectors.toSet());
    }

    /**
     * Takes a ClassReader a produce a classWriter that writes it.
     *
     * @param cr A classReader.
     * @return A classWriter that writes the input from the ClassReader.
     */
    public static ClassWriter fromReaderToWriter(ClassReader cr) {
        ClassWriter cw = new ClassWriter(0);
        cr.accept(cw, 0);
        return cw;
    }

    /**
     * Write a class Writer into a specified file.
     *
     * @param cw         The ClassWriter to write.
     * @param fileOutput The file where it should be written.
     * @throws FileNotFoundException if can't find output file
     * @throws IOException           if can't write for a number of reasons
     */
    public static void writeClassWriter(ClassWriter cw, File fileOutput, boolean verbose) throws FileNotFoundException, IOException {
        if (fileOutput == null) {
            throw new IOException("fileOutput is null");
        }

        if (!(new File(fileOutput.getParent())).exists()) {
            (new File(fileOutput.getParent())).mkdirs();
        }

        try (FileOutputStream fo = new FileOutputStream(fileOutput)) {
            fo.write(cw.toByteArray());
            if (verbose) {
                MessageUtil.message("[WROTE FILE] ->" + fileOutput.getAbsolutePath(), false).emit();
            }
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("failed at writing the file " + fileOutput.getAbsolutePath());
        }
    }

    public static void writeClassWriter(ClassWriter cw, File fileOutput) throws FileNotFoundException, IOException {
        writeClassWriter(cw, fileOutput, true);
    }

    /**
     * Return a list of ClassReader reading each .class file from a .jar file.
     *
     * @param file A jar File
     * @return A list of ClassReader, one for each .class file in the jar.
     * @throws IOException
     */
    public static List<ClassReader> readClassesFromJar(File file) throws IOException {
        return readClassesFromJar(file, e -> e.getName().endsWith(".class"));
    }

    /**
     * Return a list of ClassReader reading each .class file from a .jar file.
     *
     * @param file A jar File
     * @return A list of ClassReader, one for each .class file in the jar.
     * @throws IOException
     */
    public static List<ClassReader> readClassesFromJar(File file, Filter<ZipEntry> f) throws IOException {
        JarFile jarFile = new JarFile(file);
        List<ZipEntry> list = openJar(file, f);
        List<ClassReader> listCr = new ArrayList<>();
        for (ZipEntry entry : list) {
            listCr.add(new ClassReader(jarFile.getInputStream(entry)));
        }
        return listCr;
    }

    /**
     * From a set of input files, constructs a set of {@link SubProject}s.
     *
     * @param inputs input locations the user specified to millr
     * @return set of {@link SubProject}s to be milled
     * @throws IOException             any issues with finding or accessing files
     * @throws ClassNotLoadedException if some classes failed to be added to the ClassLoader
     */
    public static Set<SubProject> constructSubprojects(Set<File> inputs) throws IOException, ClassNotLoadedException {
        Set<SubProject> subprojects = new HashSet<>();

        // initialize all the variables for classpaths related to the subprojects.
        for (File file : inputs) {
            SubProject sp = SubProject.getSubProject(file);
            subprojects.add(sp);
        }

        // load the class files and copy the non-class files.
        for (SubProject sp : subprojects) {
            sp.initializeProjectAndCopyFiles();
        }

        // in the case where *.jar files were found in subprojects, new subprojects
        // were created. We now collect all the subprojects.
        for (SubProject sp : subprojects) {
            subprojects.addAll(sp.setSubProject);
        }
        // TODO: 09/08/2018 does this work? Modifying collection in for-each loop

        return subprojects;
    }

    /**
     * Walks from a path a listing all the files that can recursively reached
     * from this path.
     *
     * @return All the files belonging in the subtree from this path.
     * @throws IOException if the path or some sub-paths of it cannot be read.
     */
    public static List<File> walk(File file) throws IOException {
        return walk(file, e -> true);
    }

    /**
     * Walk from a path, and returns the list of files that can be reached from
     * this path that satisfies the condition established by the filter.
     *
     * @param filter A condition on the files.
     * @return The list of files whose paths are a subpath of the original path,
     * and that satisfy the condition established by the filter.
     * @throws IOException if a file path cannot be read
     */
    public static List<File> walk(File root, Filter<File> filter) throws IOException {
        List<File> list = new ArrayList<>();
        root = root.getCanonicalFile();
        if (!root.isDirectory() && filter.accept(root)) {
            list.add(root);
            return list;
        }

        if (root.listFiles() == null) {
            return list;
        } else {
            list.addAll(Arrays.asList(root.listFiles()));
        }

        List<File> newList = new ArrayList<>();

        for (File f : list) {
            if (f.isDirectory()) {
                newList.addAll(walk(f, filter));
            } else if (filter.accept(f)) {
                newList.add(f);
            }
        }

        return newList;
    }


    /**
     * Given a collection of ClassReader, check if any has a null name,
     * indicating an error.
     *
     * @param readers A collection of ClassReaders.
     * @return True if one of them has a null name.
     */
    public static boolean readersContainNullName(Collection<ClassReader> readers) {
        return readers.stream().anyMatch(e -> (e.getClassName() == null));
    }

    /**
     * Read a jar File and output the list of entries that are accepted by a
     * filter.
     *
     * @param file A .jar file
     * @throws IOException If the jar File cannot be read or is not a .jar file.
     */
    public static List<ZipEntry> openJar(File file, DirectoryStream.Filter<ZipEntry> filter) throws IOException {
        JarFile jarFile = new JarFile(file);
        Enumeration<? extends ZipEntry> entries = jarFile.entries();
        List<ZipEntry> list = new ArrayList<>();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                if (filter.accept(entry)) {
                    list.add(entry);
                }
            }
        }
        return list;
    }

    /**
     * Recursively deletes all content of the specified file or directory.
     * Java's File.delete mechanism does not delete directories with contents.
     *
     * @param directory file or directory to delete
     * @return true if successfully deleted, false otherwise
     */
    public static boolean recursiveDelete(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                recursiveDelete(file);
            }
        }
        try {
            Files.delete(directory.toPath());
            return true;
        } catch (NoSuchFileException e) {
            MessageUtil.warning(e, String.format("%s: no such " + " file or directory", directory.toString())).emit();
            return false;
        } catch (DirectoryNotEmptyException x) {
            MessageUtil.warning(x, String.format("%s not empty", directory.toString())).emit();
            return false;
        } catch (FileSystemException x) {
            MessageUtil.warning(x, String.format("%s: no such " + " file or directory", directory.toString())).emit();
            return false;
        } catch (IOException x) {
            // File permission problems are caught here.
            MessageUtil.error(x).report().resume();
            return false;
        }
    }

    /**
     * Return the output Folder where the millr classes will be written.
     *
     * @return output directory
     */
    File getOutputDirectory() {
        return outputPath;
    }

    /**
     * Return the input locations of the classes to be milled.
     *
     * @return input
     */
    Set<File> getInputLocations() {
        return inputPaths;
    }

    /**
     * Return the temporaryFolder created by millr, and deleted when millr
     * exits, that should be used as a temporary Folder for every millr classes.
     *
     * @return The temporary Folder
     */
    File getTempDirectory() {
        return millrTempFolder;
    }

    /**
     * Return true if the user indicated that extended array analysis should be
     * performed by millr.
     *
     * @return true if extended analysis should be performed, false otherwise
     */
    boolean shouldPerformExtendedAnalysis() {
        return extendedAnalysis;
    }

    /**
     * Randomly produce a 15 characters long string of character that is
     * compatible to be the name of a method. It is statically ensured that each
     * call to this function will return a new name.
     *
     * @return A 15 characters long new name randomly generated.
     */
    String generateNewMethodName() {
        // todo remove SynchronizedTransformationOldOld so this can be private
        char[] c = new char[15];
        int length = VALID_CHARACTER_ALPHABET.length();

        for (int i = 0; i < 15; i++) {
            int randomNumber = random.nextInt(length);
            c[i] = VALID_CHARACTER_ALPHABET.charAt(randomNumber);
        }

        String nameGenerated = String.valueOf(c);
        if (!previouslyGeneratedMethodNames.contains(nameGenerated)) {
            previouslyGeneratedMethodNames.add(nameGenerated);
            return nameGenerated;
        } else {
            return generateNewMethodName();
        }
    }

    /**
     * Load a path into the system class Loader. Only works if this one is an
     * URLClassLoader. Throws an exception if loading the path fails.
     *
     * @param file The file to be added to the system class loader.
     * @throws ClassNotLoadedException if the class fails to load
     * @throws IOException             if the file is not referring to any actual file.
     *                                 operations break
     */
    void addPath(File file) throws ClassNotLoadedException, IOException {
        File f = file.getCanonicalFile();
        if (!pathLoaded.contains(f)) {
            System.out.println(f.toString());
            try {
                URL url = f.toURI().toURL();
                URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
                // InputOutput.class.getClassLoader();
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, url);
            } catch (Throwable t) {
                try {
                    Class classLoader = InputOutput.class.getClassLoader().getClass();
                    Method method = classLoader.getMethod("addPathComponent", File.class);
                    method.setAccessible(true);
                    method.invoke(InputOutput.class.getClassLoader(), file);
                } catch (Throwable tr) {
                    throw new ClassNotLoadedException(t, file.getPath());
                }
            }
            pathLoaded.add(f);

        }
    }


    /**
     * Read the argument from the command line. Arguments should specified
     * inputs, preceded by -i, and a single output, preceded by -o. If more that
     * one output is given, an exception is thrown. If the file system cannot
     * get a grip of the input files, an exception is thrown. If the input files
     * are not either folder, or .class, or .jar files, an exception is thrown.
     * It stores the result in the field outputPath for the output file, and
     * into inputPaths for the input files.
     *
     * @param args The arguments from the command line.
     * @throws IllegalArgumentException if the user provides an illegal argument
     * @throws NoSuchFileException      if none of the indicated input files exist
     * @throws IOException              if Java API throws IOException
     */
    public Pair<File, Set<File>> readInputLineAndReturn(String... args) throws IllegalArgumentException, NoSuchFileException, IOException {
        Set<File> inputPaths = new HashSet<>();
        File outputPath = null;
        // user must provide some arguments
        if (args.length == 0) {
            throw new IllegalArgumentException("The user must provide at least an input location.");
        }

        boolean readingInputArgs = false;
        boolean readingOutputArgs = false;
        List<String> inputStrings = new ArrayList<>();
        List<String> outputStrings = new ArrayList<>();

        String[] javacArgs = null;
        boolean callToJavac = Arrays.stream(args).anyMatch(e -> e.equals(JAVAC_FLAG));

        if (callToJavac) {
            int javacPosition = Arrays.asList(args).indexOf(JAVAC_FLAG) + 1;
            int javacEndPosition = Arrays.asList(args).indexOf(JAVAC_END_FLAT);
            if (javacEndPosition == -1) {
                javacEndPosition = args.length + 1;
            }
            if (javacPosition < args.length) {
                javacArgs = Arrays.copyOfRange(args, javacPosition + 1, javacEndPosition - 1);
            } else javacArgs = new String[0];
            args = Stream.concat(Arrays.stream(Arrays.copyOfRange(args, 0, javacPosition - 1)),
                    (javacEndPosition < args.length) ? Arrays.stream(Arrays.copyOfRange(args, javacEndPosition + 1, args.length)) : Stream.empty()).collect(Collectors.toList()).toArray(new String[0]);
        }


        // iterate over arguments to determine available information
        for (String argument : args) {
            argument = argument.trim();
            if (argument.equals(EXTENDED_ANALYSIS)) {
                extendedAnalysis = true;
            }
            if (argument.equals(VERBOSE_FLAG)) {
                MessageUtil.setVerbose(true);
            } else if (argument.equals(SUPPRESS_WARNINGS_FLAG)) {
                MessageUtil.suppressWarnings(true);
            } else if (argument.equals(SUPPRESS_MESSAGES_FLAG)) {
                MessageUtil.suppressMessages(true);
            } else if (argument.equals(SUPPRESS_ALL_FLAG)) {
                MessageUtil.suppressWarnings(true);
                MessageUtil.suppressMessages(true);
            } else if (argument.equals(HELP_FLAG)) {
                MessageUtil.message(HELP_STRING, true).emit();
                System.exit(0);
            } else if (argument.equals(OVERWRITE_OUTPUT_FLAG)) {
                overWrite = true;
            } else if (argument.equals(WIPE_OUTPUT_FLAG)) {
                wipeOutput = true;
            } else if (argument.equals(INPUT_FLAG)) {
                readingInputArgs = true;
                readingOutputArgs = false;
            } else if (argument.equals(OUTPUT_FLAG)) {
                readingOutputArgs = true;
                readingInputArgs = false;
            } else if (readingInputArgs) {
                inputStrings.add(argument);
            } else if (readingOutputArgs) {
                outputStrings.add(argument);
            } else {
                throw new IllegalArgumentException("Unrecognized CLI argument " + argument);
            }
        }

        // handle output location
        if (outputStrings.size() > 1) {
            throw new IllegalArgumentException("Only one output location can be specified.");
        } else if (outputStrings.size() == 1) {
            outputPath = new File(outputStrings.get(0)).getCanonicalFile();
        }

        // handle input locations
        for (String location : inputStrings) {
            File input = new File(location).getCanonicalFile();

            if (!input.exists()) {
                MessageUtil.error(new NoSuchFileException("Input file " + input + " does not exist"))
                        .report()
                        .resume();
            } else if (input.isDirectory()) {
                inputPaths.add(input);
            } else if (!callToJavac) {
                if (!location.endsWith(".class") && !location.endsWith(".jar")) {
                    throw new IllegalArgumentException("Input files must be  *.class or a *.jar files"
                            + "\n stopped at argument " + location);
                } else {
                    inputPaths.add(input);
                }
            }
            else{
                if (!location.endsWith(".java")){
                    throw new IllegalArgumentException("Input files must be  *.java files"
                            + "\n stopped at argument " + location);
                }
                else{
                    inputPaths.add(input);
                }
            }

        }
        // if all input locations turned out to be bad, or none were given
        if (inputPaths.isEmpty()) {
            throw new NoSuchFileException("All input locations were bad, or no input locations were provided");
        }

        if (callToJavac) {
            inputPaths = callJavacOnInput(inputPaths, javacArgs);
        }

        return new Pair<>(outputPath, inputPaths);
    }


    /**
     * Call javac on a set of Input Locations, with specified arguements,
     * and return a set of locations, which contains the results.
     * These locations are generated at runtime and placed in the millr Temporary Folder.
     */
    Set<File> callJavacOnInput(Set<File> inputPaths, String[] args) throws IOException {
        File tempFolder = getTempDirectory();
        File newTempFolder;
        do {
            newTempFolder = new File(tempFolder, generateNewMethodName().substring(0, 5));
        } while (tempFolder.exists());
        newTempFolder.mkdirs();
        executeBashCommand(newTempFolder,
                Stream.concat(Stream.concat(Stream.concat(Stream.of("javac"), inputPaths.stream().map(e -> e.toString())), Arrays.stream(args))
                        , Arrays.stream(new String[]{"-d", newTempFolder.toString()})).collect(Collectors.toList()).toArray(new String[0]));
        return Collections.singleton(newTempFolder);

    }


    /**
     * Reads the command-line arguments and initializes all internal state of
     * the {@link InputOutput} utility accordingly.
     *
     * @param args the command-line arguments passed to millr
     * @throws IllegalArgumentException if an unrecognized argument is
     *                                  encountered
     * @throws NoSuchFileException      if an input/output location is not valid
     * @throws IOException              any problem with the filesystem
     */
    void readInputLine(String... args) throws IllegalArgumentException, NoSuchFileException, IOException {
        Pair<File, Set<File>> pair = readInputLineAndReturn(args);
        inputPaths = pair.getSecond();

        if (pair.getFirst() != null) {
            outputPath = pair.getFirst();
        }
    }

    /**
     * Read the command line arguement and return a collection of SubProject.
     * The argument must be of the form -i fileToSubProjects... . Using -o, -f
     * options would not operate, and anything else will result in an error.
     *
     * @param args Arguments of the form -i arg1 arg2....
     * @return The collection of SubProject associated.
     * @throws Exception If any IOException should occurs.
     */
    public Collection<SubProject> readCommandLineIntoSubproject(String... args) throws Exception {
        Pair<File, Set<File>> pair = readInputLineAndReturn(args);
        Collection<File> inputPaths = pair.getSecond();
        Collection<SubProject> collectionSubProject = new HashSet<>();
        //First we initialize all the variables for classpaths related to the subprojects.
        for (File file : inputPaths) {
            SubProject sp = SubProject.getSubProject(file);
            collectionSubProject.add(sp);
        }
        return collectionSubProject;
    }

    /**
     * Checks for conflicts with existing files in the output directory. If the
     * user has specified a force deletion option, the user is not asked for
     * input. Else, the user has to decide how the conflict is resolved (abort,
     * ignore, erase existing, etc).
     *
     * @throws IOException if some file path cannot be read
     */
    void resolveOutputConflicts(Set<File> inputPaths, File outputPath)
            throws IOException {
        // if wiping output folder, check that it is not parent of input
        if (wipeOutput && inputPaths.stream().anyMatch((file) -> file.toPath().startsWith(outputPath.toPath()))) {
            MessageUtil.error(new RuntimeException()).report("-w is being used to forcefully clear "
                    + "the output path, but the output path is the parent of at least one input path.\n"
                    + "This would result in wiping one or more input files. If the input and output paths "
                    + "are correct, try running millr without -w.").terminate();
        } else if (wipeOutput) {
            InputOutput.recursiveDelete(outputPath);
            outputPath.mkdirs();
        }

        // if neither overwriting nor wiping overwriting, check with user in case of conflicts
        if (!overWrite && outputPath.exists() && (InputOutput.walk(outputPath)).size() != 0) {
            String query = "The output directory is not empty, which might cause a conflict. Proceed?\n"
                    + "y: yes\n"
                    + "n: no\n"
                    + "erase: delete folder contents\n";
            String[] options = {"yes", "y", "no", "n", "erase"};

            String answer = getUserDecision(query, options);

            if (answer.equalsIgnoreCase("no") || answer.equalsIgnoreCase("n")) {
                System.exit(1);
            } else if (answer.equalsIgnoreCase("erase")) {
                InputOutput.recursiveDelete(outputPath);
                outputPath.mkdirs();
            }
            // if yes, we do nothing
        }
    }

    /**
     * Create all the directories needed for milling.
     *
     * @param millrTempFolder the folder for temporary outputs
     * @param outputPath      the folder for final outputs
     * @throws IOException
     */
    void mkdirs(File millrTempFolder, File outputPath) throws IOException {
        // setup required directories
        millrTempFolder.mkdirs();
        millrTempFolder.deleteOnExit();
        Files.createDirectories(outputPath.getCanonicalFile().toPath());
    }

    /**
     * It is required that no two subprojects contain classes with the same
     * names. This method checks for class name conflicts in the subprojects to
     * be milled, and throws a {@link DuplicateClassNameException} if there are
     * conflicts. If there are not, the method returns normally.
     *
     * @param subprojects subprojects to mill
     * @throws DuplicateClassNameException if there are duplicate class names
     */
    void checkClassNameConflicts(Collection<SubProject> subprojects) throws DuplicateClassNameException {
        // We want to create a map that to each Class Name attribute the
        // subproject it comes from. For that, we need to check that no
        // two sub-projects have classes with the same class name.
        Iterator<SubProject> iteratorOne = subprojects.iterator();
        Iterator<SubProject> iteratorTwo = subprojects.iterator();
        while (iteratorOne.hasNext()) {
            SubProject sbOne = iteratorOne.next();
            while (iteratorTwo.hasNext()) {
                SubProject sbTwo = iteratorTwo.next();
                if (sbOne.equals(sbTwo)) {
                    continue;
                }
                Set<String> intersect = new HashSet<>(sbOne.getCollectionClassName());
                intersect.retainAll(sbTwo.getCollectionClassName());
                if (!intersect.isEmpty()) {
                    throw new DuplicateClassNameException("The project " + sbOne.nameOfTheProject
                            + " and the project " + sbTwo.nameOfTheProject + "both contains classes "
                            + " with same class name " + Arrays.toString(intersect.toArray()));
                }
            }
        }
    }

    /**
     * HELPER: Create a temporary folder for extracting the content of the jar
     * files.
     */
    private File createMillrTempFolder() {
        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFolderName = generateNewMethodName();

        while (new File(tempDir, tempFolderName).exists()) {
            tempFolderName = generateNewMethodName();
        }
        return new File(tempDir, tempFolderName);
    }

    /**
     * HELPER: prompts the user to provide one of the given options on CLI
     */
    private String getUserDecision(String query, String... options) {
        Scanner reader = new Scanner(System.in);
        MessageUtil.message(query, true).emit();
        String answer = reader.nextLine();

        // loop until user gives good response
        while (!Arrays.asList(options).contains(answer)) {
            MessageUtil.message("Unrecognized answer, try again:\n", true).emit();
            answer = reader.nextLine();
        }

        return answer;
    }

    static final void executeBashCommand(String... command) throws IOException {
        executeBashCommand(null, command);
    }


    /**
     * Execute a bash command.
     *
     * @param command The command to be executed.
     * @throws IOException In case the execution results in a IO exception.
     */
    static final void executeBashCommand(File processDirectory, String... command) throws IOException {
        System.out.println("Executing Command " + Arrays.toString(command));

        ProcessBuilder pb = new ProcessBuilder(command);
        if (processDirectory != null) {
            pb.directory(processDirectory);
        }
        if (MessageUtil.isVerbose()) {
            pb.inheritIO();
        }
        Process pstart = pb.start();
        ProcessHandler.startProcess(pstart, 10, TimeUnit.SECONDS);
    }
}
