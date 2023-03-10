package xyz.acygn.millr.mokapotsemantics;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;


import java.nio.file.Files;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Marcello De Bernardi, Thomas Cuvillier
 */
public class MillUtil implements PathConstant {


    static String classSuffix = ".class";


    public static List<Class> mill(MillrClassLoader classLoader) throws ClassNotFoundException, InvocationTargetException, IOException, NoSuchMethodException, IllegalAccessException {
        File f = new File(OUTPUTMILLRTESTCLASSES);
        List<File> listFiles = walk(f, file->file.getPath().endsWith(".class"));
        System.out.println("insn");
        return mill(listFiles.stream().map(file-> new File(OUTPUTMILLRTESTCLASSES).toPath().
                relativize(file.toPath())).map(e->e.toString().substring(0, e.toString().length() - ".class".length()))
                .collect(Collectors.toList()), classLoader);
    }




    /**
     * Mills the indicated classfile, then uses a {@link ClassLoader} to
     * load the milled class and returns an instance of the milled class.
     * The classes should be located in PathConstant.OUTPUTMILLRTESTCLASSES.
     * and will be milled into PathConstant.OUTPUTMILLEDCLASSES
     * The ClassLoader passed as arguement should have millr in its ClassPath.
     *
     * @param classes list of class names, without ".class" extension
     * @return instance of milled version of provided class
     */
    public static List<Class> mill(List<String> classes, MillrClassLoader customClassLoader)
            throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, NoSuchMethodException {
        String millingPath = OUTPUTMILLRTESTCLASSES;
        List<String> arguments = new ArrayList<>(Arrays.asList("-v", "-f", "-i"));
        URL[] arrayURL = customClassLoader.getURLs();


        for (String className : classes) arguments.add(millingPath + "/" + className.replace(".", "/") + classSuffix);
        arguments.add("-o");
        arguments.add(OUTPUTMILLEDCLASSES);


        // mill the original class

        Class millrMainClass = customClassLoader.loadClass("xyz.acygn.millr.Mill");
        if (millrMainClass.getClassLoader().equals(ClassLoader.getSystemClassLoader())) {
            throw new RuntimeException();
        }
        if (!millrMainClass.getClassLoader().equals(customClassLoader)) throw new RuntimeException();

        if (!millrMainClass.getProtectionDomain().implies(new RuntimePermission("createClassLoader"))){
            throw new SecurityException();
        }
        if (!Policy.getPolicy().getPermissions(millrMainClass.getProtectionDomain()).implies(new RuntimePermission("createClassLoader"))){
            throw new SecurityException();
        }

        millrMainClass.getMethod("main", String[].class).invoke(null, new Object[]{arguments.toArray(new String[0])});


        /** This class loader will prefer loading the milled class over the non-milled class if the two got same name
         *
         */

        List<Class> milledClasses = new ArrayList<>();
        List<URL> listURL = new ArrayList<>(Arrays.asList(arrayURL));
        File millrFile = new File(OUTPUTMILLEDCLASSES);
        listURL.add(millrFile.toURI().toURL());

        for (String name : classes) {
            milledClasses.add(customClassLoader.loadClass(name.replace("/", ".")));
        }
        return milledClasses;
    }


//    public static void addMillrClassPathToClassLoader(URLClassLoader cl) throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException {
//        File millrFile = new File(OUTPUTMILLEDCLASSES);
//        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
//        addURL.setAccessible(true);
//        addURL.invoke(cl, millrFile.toURI().toURL());
//    }
//
//
//    public static String removeFormerMillFromClassPath(String classPath, String millFolder) throws IOException {
//        String millPath = new File(millFolder).getCanonicalPath();
//        StringBuilder sr = new StringBuilder();
//        Arrays.stream(classPath.split(File.pathSeparator)).map(e -> {
//            try {
//                return new File(e).getCanonicalPath();
//            } catch (Exception ex) {
//                return e;
//            }
//        }).filter(e -> !e.equals(millPath)).forEach(e -> sr.append(e).append(File.pathSeparator.toString()));
//        return sr.toString();
//    }

    public static List<File> walk(File f, Predicate<File> filter){
        if (!f.isDirectory()){
            if (filter.test(f)){
                return Collections.singletonList(f);
            }
            else{
                return new ArrayList<>();
            }
        }
        ArrayList<File> arrayFile = new ArrayList<>();
        for (File child : f.listFiles()){
            arrayFile.addAll(walk(child, filter));
        }
        return arrayFile;
    }


}
