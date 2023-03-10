/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import xyz.acygn.millr.messages.ClassNotLoadedException;
import xyz.acygn.millr.messages.MessageUtil;
import xyz.acygn.millr.messages.ClassNoAssociatedPathException;

import static xyz.acygn.millr.InputOutput.walk;

/**
 * A SubProject represents a set of classes that either reside in the same input
 * folder, or in the same .jar. In either case, a SubProject represents a set of
 * classes that are naturally associated. If a single .class file is fed to
 * millr directly, it will be its own SubProject.
 *
 * @author thomas Cuvillier
 */
public class SubProject {


    /**
     * If the input of this project is a jar file.
     */
    boolean isJar;


    String nameOfTheProject;

    /**
     * Where the output of the sub-project will be written.
     */
    File outputFolder;

    /**
     * In case the input was a jar, the output jar file.
     */
    File outputJar;

    /**
     * The input File of this Project.
     */
    File inputFile;

    /**
     * Temporary Folder this subproject is going to rely on.
     */
    File temporaryFolder;

    /**
     * Where the Jar will be first extracted.
     */
    File folderForJarExtract;

    /**
     * In case of a jar included in a SubProject, the relative path from the original subproject to this jar.
     */
    Path relativePathToJar;

    /**
     * If the input is a jar, where the output files will be first written before being transformed into a jar again.
     */
    File folderForJarOuptut;


    /**
     * Map the internal name the relative paths inputFile ----> Path. The first is its
     * path if this one does not belong to a *.jar, otherwise is the path to the
     * jar, and then the path to this file inside this jar.
     */
    Map<String, Path> fromNameClassToPath;

    /**
     * Will store the subprojects that may be children of this subProject (in
     * case some *.jar files are in the folder).
     */
    Set<SubProject> setSubProject;

    /**
     * The collection of ClassReader being part of the project.
     */
    Collection<ClassReader> CollectionClassReader;

    /**
     * The Collection of the names of the ClassReaders being part of the
     * project. Should be accessed only through the getter method, so that it
     * can be updated.
     */
    private Collection<String> CollectionClassName;

    /**
     * The getter method for the collection of Class Names. Update it if needed
     * before returning it. Note that if the Class-Readers are being modified,
     * then this collection might not be up-to-date.
     *
     * @return
     */
    public Collection<String> getCollectionClassName() {
          if (!(CollectionClassReader.hashCode() == hashCodeUpdateCollectionClassName)) {
        updateCollectionClassName();
          }
        return CollectionClassName;
    }

    /**
     * A hashcode to test whethere there is a need to update the
     * CollectionClassName, or if this one is the up-to-date version.
     */
    private int hashCodeUpdateCollectionClassName = 0;

    /**
     * Private constructor for SubProject. Initializes the collections fields,
     * but not the ones with relevant informations.
     */
    private SubProject() {
        CollectionClassReader = new HashSet<>();
        fromNameClassToPath = new HashMap<>();
        setSubProject = new HashSet<>();
        setSubProject.add(this);
        CollectionClassName = new HashSet<>();
    }

    /**
     * Given a file, creates a SubProject associated with it. The file can
     * either be a *.class, a *.jar, or a directory.
     *
     * @param inputFile the file being the root of our subproject.
     * @return the corresponding subproject
     * @throws IOException                   if the file does not exists.
     * @throws UnsupportedOperationException if the input is neither a directory
     *                                       or a .jar file
     */
    static SubProject getSubProject(File inputFile) throws IOException, UnsupportedOperationException {
        if (!inputFile.isDirectory() && !inputFile.getName().endsWith(".jar") && !inputFile.getName().endsWith(".class")) {
            throw new UnsupportedOperationException("A project can only be created from a folder, " +
                    "a *.class file or a *.jar");
        }
        if (!inputFile.exists()) {
            throw new IOException("The input file" + inputFile.getPath() + "does not exists");
        }
        SubProject blankProject = new SubProject();
        if (inputFile.getName().endsWith(".jar")) {
            blankProject.initializeFieldsWithJarInput(inputFile);
        } else if (inputFile.getName().endsWith(".class")) {
            blankProject.initializeFieldsWithClassInput(inputFile);
        } else {
            blankProject.initializeFieldsWithFolderInput(inputFile);
        }
        return blankProject;
    }

    /**
     * Given a file that is a *.class file, initialize the fields of this
     * SubProject appropriately.
     *
     * @param inputClass The *.class file.
     * @throws IOException If we cannot figure the canonical path to the file
     *                     appropriately.
     */
    private void initializeFieldsWithClassInput(File inputClass) throws IOException {
        if (!inputClass.toString().endsWith(".class")) {
            throw new UnsupportedOperationException("This method should only be called on *.class file. " +
                    "File given as input " + inputClass.toString());
        }
        nameOfTheProject = inputClass.getName().substring(0, inputClass.getName().length() - 6);
        outputFolder = InputOutput.getInstance().getOutputDirectory().getCanonicalFile();
        inputFile = inputClass.getCanonicalFile();
        temporaryFolder = InputOutput.getInstance().getTempDirectory().getCanonicalFile();
        isJar = false;
        folderForJarExtract = null;
        outputJar = null;
        relativePathToJar = null;
        folderForJarOuptut = null;
    }

    /**
     * Given a file that is a *.jar file, initialize the fields of this
     * SubProject appropriately.
     *
     * @param inputJar The jar file input.
     * @throws IOException If we cannot figure the canonical path to the file
     *                     appropriately.
     */
    private void initializeFieldsWithJarInput(File inputJar) throws IOException {
        if (!inputJar.toString().endsWith(".jar")) {
            throw new UnsupportedOperationException("This method can only be called with *.jar file." +
                    " File given as input" + inputJar.toString());
        }
        isJar = true;
        nameOfTheProject = inputJar.getName().substring(0, inputJar.getName().length() - 4);
        outputJar = new File(InputOutput.getInstance().getOutputDirectory(), nameOfTheProject + ".jar").getCanonicalFile();
        inputFile = inputJar.getCanonicalFile();
        temporaryFolder = new File(getMillrTempDirForJar(), nameOfTheProject).getCanonicalFile();
        folderForJarExtract = getMillrTempDirForJarExtract(new File(nameOfTheProject, ".jar").toPath());
        folderForJarOuptut = getMillrTempDirForJarOutput(new File(nameOfTheProject, ".jar").toPath());
        outputFolder = folderForJarOuptut;
        relativePathToJar = null;

    }

    /**
     * Given a file that is a directory, initialize the fields of this
     * subproject appropriately.
     *
     * @param folder A file referring to a directory.
     * @throws IOException If we cannot figure the canonical path to the file
     *                     appropriately
     */
    private void initializeFieldsWithFolderInput(File folder) throws IOException {
        if (!folder.isDirectory()) {
            throw new UnsupportedOperationException("This function can only be called with a directory as input, " +
                    "but the file " + folder.toString() + " is not a directory");
        }
        isJar = false;
        nameOfTheProject = folder.getName();
        outputFolder = new File(InputOutput.getInstance().getOutputDirectory(), nameOfTheProject).getCanonicalFile();
        inputFile = folder.getCanonicalFile();
        temporaryFolder = new File(InputOutput.getInstance().getTempDirectory(), nameOfTheProject);
        folderForJarExtract = null;
        outputJar = null;
        relativePathToJar = null;
    }

    /**
     * A private constructor for a jar file that is found inside another
     * subproject.
     *
     * @param jarFile          The jar file that belongs inside the subproject.
     * @param parentSubProject The subproject this jar file is part of.
     * @throws IOException If one cannot figure out a canonical path.
     */
    private SubProject(File jarFile, SubProject parentSubProject) throws IOException {
        this();
        isJar = true;
        relativePathToJar = parentSubProject.inputFile.toPath().relativize(jarFile.toPath());
        nameOfTheProject = jarFile.getName().substring(0, jarFile.getName().length() - 4);
        outputFolder = getMillrTempDirForJarOutput(relativePathToJar).getCanonicalFile();
        outputJar = new File(parentSubProject.outputFolder, relativePathToJar.toString());
        inputFile = jarFile;
        temporaryFolder = new File(getMillrTempDirForJar(), nameOfTheProject).getCanonicalFile();
        folderForJarExtract = getMillrTempDirForJarExtract(relativePathToJar).getCanonicalFile();
        folderForJarOuptut = getMillrTempDirForJarOutput(relativePathToJar).getCanonicalFile();
    }

    /**
     * Given the name of a class originally part of the project, returns the
     * outputFolder corresponding.
     *
     * @param ClassName The name of the class.
     * @return The file this class is going to be milled into.
     */
    public File getOuputFile(String ClassName) {
        if (fromNameClassToPath.containsKey(ClassName)) {
            MessageUtil.warning("ClassName " + ClassName + " not found in subproject "
                    + this.nameOfTheProject + "; we output a file that may not be correct");
            return new File(outputFolder, ClassName + ".class");
        }
        return new File(outputFolder, fromNameClassToPath.get(ClassName).toString());
    }

    /**
     * Return the file for the temporary folder in which operations on jar files
     * such as extracting should be done.
     *
     * @return The file for the temporary folder in which operations on jar
     * files such as extracting should be done.
     */
    private static File getMillrTempDirForJar() {
        return new File(InputOutput.getInstance().getTempDirectory(), "millrJar");
    }

    /**
     * Method for getting the output folder for the project.
     *
     * @return The output folder in which the milled class files will be
     * written.
     */
    private File getOutputFolder() {
        return outputFolder;
    }

    /**
     * Given the path to a Jar File, returns the name of the folder in which the
     * jar might be extracted.
     *
     * @param pathToJarFile A relative path to the jar file.
     * @return A folder in which the jar file might be extracted.
     */
    private static final File getMillrTempDirForJarExtract(Path pathToJarFile) {
        String jarPath = pathToJarFile.toString();
        return new File(getMillrTempDirForJar().toString(), jarPath.substring(0, jarPath.length() - 4));
    }

    /**
     * @param pathToJarFile Relative path from the inputFolder to the Jar file,
     *                      or the name of the jar file if fed directly as input.
     * @return File
     */
    private static final File getMillrTempDirForJarOutput(Path pathToJarFile) {
        String jarPath = pathToJarFile.toString();
        File fileForJarOutput = new File(InputOutput.getInstance().getTempDirectory(), "millrJarOutput");
        try {
            return new File(fileForJarOutput.toString(), jarPath.substring(0, jarPath.length() - 4));
        }
        catch (Throwable t){
            throw t;
        }
    }

    /**
     * Will read the input file, input-folder, extract the jars, copy the non
     * *.class files, and initialize lists of class readers. This is seperated
     * from the constructor since the constructor might fail and we do not want
     * to start copy files in that case.
     *
     * @throws FileNotFoundException        if input file is not found
     * @throws IOException                  if Java API fails to work with filesystem
     * @throws ReflectiveOperationException if reflective use of ClassLoaders
     *                                      fails
     */
    void initializeProjectAndCopyFiles() throws IOException, ClassNotLoadedException {
        if (inputFile.toPath().toString().endsWith(".class")) {
            addClass(inputFile);
            ClassReader cr = computeFrame(new ClassReader(new FileInputStream((inputFile))));
            CollectionClassReader.add(cr);
            CollectionClassName.add(cr.getClassName());
            fromNameClassToPath.put(cr.getClassName(), new File(cr.getClassName() + ".class").toPath());
        } else if (inputFile.toPath().toString().endsWith(".jar")) {
            extractJar(inputFile, folderForJarExtract);
            fromListOfFilesToReader(folderForJarExtract, folderForJarOuptut);
        } else if (inputFile.isDirectory()) {
            fromListOfFilesToReader(inputFile, outputFolder);
        }
        Collection<ClassReader> temporaryCollection =
                CollectionClassReader.stream().map(SubProject::computeFrame).collect(Collectors.toList());
        CollectionClassReader.clear();  CollectionClassReader.addAll(temporaryCollection);

        removeClassInfo();

    }

    /**
     * This method reads from an inputFile and create a collection of
     * ClassReader for them. Furthermore. if all the files are coming from a
     * Folder called inputProject, then the relative path from inputFolder to
     * the file will be remembered. Finally, if one of the file is a jar, it
     * will extract the .class
     *
     * @param inputFile A file corresponding to the root of the Project. May be
     *                  null, but, if not null, all the inputFiles must be subfiles of the
     *                  project.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception
     */
    private synchronized void fromListOfFilesToReader(File inputFile, File outputFile)
            throws FileNotFoundException, IOException, ClassNotLoadedException {
        List<File> listInputFiles = walk(inputFile.getCanonicalFile());
            for (File fileToBeRead : listInputFiles) {
                Path path = inputFile.toPath().relativize(fileToBeRead.toPath());
                if (fileToBeRead.getName().endsWith(".class")) {
                    addClass(fileToBeRead);
                    ClassReader cr = new ClassReader(new FileInputStream(fileToBeRead));
                    if (fromNameClassToPath.containsKey(cr.getClassName())) {
                        throw new RuntimeException("trying to millr a project with twice the same class Name" + cr.getClassName() + "\n"
                                + "typically happens if a jar is present and contains the same classes as the ones part of the project ");
                    }
                    fromNameClassToPath.put(cr.getClassName(), path);
                    CollectionClassReader.add(cr);
                } else if (fileToBeRead.getName().endsWith(".jar")) {
                    if (isJar) {
                        throw new UnsupportedOperationException("A jar file inside a jar is not supported by millr");
                    }
                    SubProject jarSubProject = new SubProject(fileToBeRead, this);
                    setSubProject.add(jarSubProject);
                    jarSubProject.initializeProjectAndCopyFiles();
                } else {
                    File fileToBeCreated = new File(outputFile, path.toString());
                    fileToBeCreated.getParentFile().mkdirs();
                    Files.copy(fileToBeRead.toPath(), fileToBeCreated.toPath());
                }
            }

        updateCollectionClassName();
    }

    /**
     * Extract a jar File to a folder. Wait 10s to conclude.
     *
     * @param jarFile
     * @param fileToExtractJar
     * @throws IOException
     */
    private static void extractJar(File jarFile, File fileToExtractJar) throws IOException {
        fileToExtractJar.mkdirs();
        ProcessBuilder pb = new ProcessBuilder("jar", "xf", jarFile.getCanonicalPath());
        pb.directory(fileToExtractJar);
        Process pstart = pb.start();
        ProcessHandler.startProcess(pstart, 10, TimeUnit.SECONDS);
    }

    private final void updateTheJarFile() throws IOException {
        if (this.isJar) {
            //     System.out.println("update the jar " + this.nameOfTheProject);
            //       System.out.println("folderToJar" + this.outputFolder + " jarToBe created " + this.outputJar);
            createJar(this.outputJar, this.outputFolder);
        }

    }



    /**
     * Create a jar file from a set of files / folder.
     *
     * @param jarFileToCreate The jar file to be created.
     * @param folderJar       The folder - files that shall be compressed
     *                        into this jar.
     * @throws IOException
     */
    private static void createJar(File jarFileToCreate, File folderJar) throws IOException {
        if (folderJar == null) {
            throw new IOException("folderJar is null");
        }
        if (!folderJar.isDirectory()) {
            throw new IOException("the input file must be a directory " + folderJar);
        }
        new File(jarFileToCreate.getParent()).mkdirs();
        List<String> command = new ArrayList<>();
        command.add("jar");
        File possibleManifest = new File(folderJar, "META-INF/MANIFEST.MF");
        if (possibleManifest.exists()) {
            command.add("cfm");
            command.add(jarFileToCreate.getCanonicalPath());
            command.add(possibleManifest.getCanonicalPath());
        } else {
            command.add("-cf");
            command.add(jarFileToCreate.getCanonicalPath());
        }
        command.add("./");
        InputOutput.executeBashCommand(folderJar, command.toArray(new String[0]));
    }

    /**
     * Update the collection of name Classes by reading them from the collection
     * of ClassReader.
     */
    public void updateCollectionClassName() {
        synchronized (CollectionClassReader) {
            checkAndRemoveNulls();
            synchronized (CollectionClassName){
                synchronized (CollectionClassReader){
                    CollectionClassName.clear();
                    CollectionClassReader.parallelStream().forEach(e->CollectionClassName.add(e.getClassName()));
                    hashCodeUpdateCollectionClassName = CollectionClassReader.hashCode();
                }
            }
        }
    }

    public void checkAndRemoveNulls() {
        if (CollectionClassReader.stream().anyMatch(e -> e == null)) {
            MessageUtil.error(new Exception("Some class readers are null")).report().resume();
            CollectionClassReader.stream().filter(e -> e == null).forEach(e -> CollectionClassReader.remove(e));
        }
    }

    /**
     * Given a ClassReader of a class file that does not necceseraly contain
     * frames (for instance, before java 7), produce and add the frames to it.
     *
     * @param cr A ClassReader
     * @return A new ClassReader for a new class file similar to the first one
     * but with frames.
     */
    static ClassReader computeFrame(ClassReader cr) {
        ClassWriter computeFrame = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cr.accept(computeFrame, ClassReader.EXPAND_FRAMES);
        return new ClassReader(computeFrame.toByteArray());
    }

    /**
     * Write a ClassWriter. The Algorithm is as follows: If the ClassWriter is
     * detected to "writes" a ClassFile originally part of the project being
     * milled, then it will write it in its appropriate place in the folder
     * designed to be the output folder. If this one was coming from a jar part
     * of the project being milled, then this one will be write to a jar place
     * accordingly in the output folder. Otherwise, it will be just written in
     * the output folder with as path the one that its class name commands.
     */
    void writeClassWriter() throws IOException {
        for (ClassReader cr : CollectionClassReader) {
            ClassWriter cw = InputOutput.fromReaderToWriter(cr);

            if (!fromNameClassToPath.containsKey(cr.getClassName())) {
                MessageUtil.error(
                        new ClassNoAssociatedPathException(
                                "Impossible to find path associated with ClassReader for class" + cr.getClassName()
                        )).report().resume();

                InputOutput.writeClassWriter(cw, new File(outputFolder, cr.getClassName() + ".class"));
            }

            if (fromNameClassToPath.containsKey(cr.getClassName())) {
                Path pathFileToWrite = fromNameClassToPath.get(cr.getClassName());
                File fileToWrite = new File(outputFolder, pathFileToWrite.toString());
                InputOutput.writeClassWriter(cw, fileToWrite);
            }
        }

        if (isJar) {
            updateTheJarFile();
        }
    }

    /**
     * Given a class File, returns the folder file this file shalled be called
     * from / corresponding to the classpath to be given to the jvm to call this
     * class.
     *
     * @param classFile A java class file.
     * @return The folder file.
     * @throws FileNotFoundException If the class file can not be found.
     * @throws IOException           If the program fails to read the class to get its
     *                               class name.
     */
    static File getBaseFolder(File classFile) throws FileNotFoundException, IOException {
        if (!classFile.toString().endsWith("class")) {
            throw new UnsupportedOperationException("this operation is supposed to be used on .class file only");
        }
        ClassReader cr = new ClassReader(new FileInputStream(classFile));
        String ClassName = cr.getClassName();
        File f =  getBaseFolder(classFile.getCanonicalFile(), ClassName);
        return f;

    }

    /**
     * Given an absolute path to a class file, and the name of the class,
     * returns the path to the folder this class file should be call from.
     *
     * @param classFile      A path to a class file.
     * @param nameOfTheClass The full qualified internal name of the class.
     * @return The file corresponding to the folder.
     */
    static File getBaseFolder(File classFile, String nameOfTheClass) {
        int length = numberOf(nameOfTheClass, "/".charAt(0));
        File folderFile = classFile.getParentFile();
        for (int i = 0; i < length; i++) {
            folderFile = folderFile.getParentFile();
        }
        if (folderFile != null) {
            return folderFile;
        } else {
            return new File("");
        }
    }

    /**
     * Given a class file, adds the necessary classpath to the bootloader so
     * that it can be found by the jvm at runtime.
     *
     * @param classFile A class File.
     * @throws FileNotFoundException        If this file cannot be found.
     * @throws IOException                  If this file cannot be read.
     * @throws ReflectiveOperationException If reflective access to classloader
     *                                      fails
     */
    private static void addClass(File classFile)
            throws ClassNotLoadedException, IOException {
        InputOutput.getInstance().addPath(getBaseFolder(classFile));
    }

    /**
     * Returns the number of times a char appears in a string.
     *
     * @param str A string.
     * @param c   A character
     * @return The number of times the character appears in the string. Returns
     * 0 if the string is null.
     */
    private static int numberOf(String str, char c) {
        if (str == null) {
            return 0;
        }
        int number = 0;
        for (char d : str.toCharArray()) {
            if (d == c) {
                number += 1;
            }
        }
        return number;
    }

    /**
     * Examine if the collection of ClassReader has a package-info class.
     *
     * @return True if the collection has a package-info class.
     */
    private boolean hasPackageInfo() {
        return CollectionClassReader.stream().anyMatch(cr -> cr.getClassName().endsWith("package-info"));

    }

    /**
     * Remove the class-informations classes from the collection of ClassReader
     * of this sub-project.
     */
    private void removeClassInfo() {
        CollectionClassReader.removeIf(cr -> cr.getClassName().endsWith("package-info"));
    }

    /**
     * Check the consistency of the map mapping ClassNames to path, by verifying
     * that each class name in the subproject is indeed mapped to a path. An
     * error is emitted and logged each time a class name not belonging to the
     * pre-image of the map is found
     *
     * @return True if the pre-image of the map is a super-set of the set of
     * class-names, false otherwise.
     */
    boolean checkConsistencyFromNameToPaths() {
        Collection<String> ProblematicClassName = new HashSet<String>(CollectionClassName);
        ProblematicClassName.removeIf(e -> fromNameClassToPath.containsKey(e));
        for (String className : ProblematicClassName) {
            MessageUtil.error(
                    new ClassNoAssociatedPathException(
                            "The class name " + className + " does not have an appropriate path "
                                    + "recorded, we adjoin him a standard one from its output repository")).report().resume();
            fromNameClassToPath.put(className, new File(className + ".class").toPath());
        }
        return ProblematicClassName.isEmpty();
    }

    final void checkClassReader() {
        checkAndRemoveNulls();
        for (ClassReader cr : CollectionClassReader) {
            if (!problematicClassReader.contains(cr.getClassName())) {
                try {
                    CheckClassAdapter cv = new CheckClassAdapter(new ClassWriter(0) {
                    });
                    cr.accept(cv, 0);
                } catch (Throwable t) {
                    if (t instanceof Exception) {
                        MessageUtil.error((Exception) t).report(
                                " Problem at " + cr.getClassName()).resume();
                        problematicClassReader.add(cr.getClassName());
                    }
                    ClassWriter cw = new ClassWriter(0);
                    try {
                        cr.accept(cw, ClassReader.EXPAND_FRAMES);
                        InputOutput.writeClassWriter(cw, new File(new File("FailedTransformation"), cr.getClassName() + ".class"));
                    } catch (Throwable th) {
                        MessageUtil.error(th).report("Failed writing : We are removing the Class Reader from the transformation");
                        CollectionClassReader.remove(cr);
                        updateCollectionClassName();
                    }
                }
            }
        }
    }

    /**
     * Collection of ClassReaders where the checks have failed.
     * This collection is maintainted such that errors are not reported twice.
     */
    Collection<String> problematicClassReader = new HashSet<>();


    /**
     * Collection of ClassReader that have been removed from the transformation due to
     *
     * @return
     */

    public final boolean isJar() {
        return isJar;
    }

    public final File getInputFile() {
        return inputFile;
    }
}
