package xyz.acygn.millr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import xyz.acygn.millr.messages.MessageUtil;
import xyz.acygn.millr.mokapotsemantics.PathConstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A convenience utility to avoid field.setAccessible(true);
 *
 *         fieldComponents.add(new FieldComponent(field, target, type));unnecessary code repetition. Handle with care,
 * this class is very fragile.
 *
 * @author Marcello De Bernardi, Thomas Cuvillier
 */
public class MillUtil implements PathConstant {


    static String classSuffix = ".class";


    public static List<Class> mill(String folder, List<String> classes) throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException, NoSuchMethodException, IOException {
        List<String> listFullClasses = classes.stream().map(e-> folder.replace("/", ".") + e).collect(Collectors.toList());
        return mill(listFullClasses);
    }

    public static List<Class> mill(List<String> classes) throws IllegalAccessException, InstantiationException, IOException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        return mill(classes, (URLClassLoader) MillUtil.class.getClassLoader());
    }

    /**
     * Mills the indicated classfile, then uses a {@link ClassLoader} to
     * load the milled class and returns an instance of the milled class.
     *
     * @param classes list of class names, without ".class" extension
     * @return instance of milled version of provided class
     */
    public static List<Class>  mill(List<String> classes, URLClassLoader customClassLoader)
            throws ClassNotFoundException,  IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, NoSuchMethodException {

         String millingPath = OUTPUTMILLRTESTCLASSES;
         List<String> arguments = new ArrayList<>(Arrays.asList("-v", "-f", "-i"));

         System.out.println("millr CLASSPATH");
        Arrays.stream(((URLClassLoader) MillUtil.class.getClassLoader()).getURLs()).forEach(e->System.out.println(e.getFile()));
        System.out.println("millr CLASSPATH END");

        for (String className : classes) arguments.add(millingPath + "/" + className.replace(".", "/") + classSuffix);

        arguments.add("-o");
        arguments.add(OUTPUTMILLEDCLASSES);
        System.out.println("ARGUEMENTS " + Arrays.toString(arguments.toArray()));

        // mill the original class
        try {
            Mill.main(arguments.toArray(new String[0]));
        }
        catch (Exception e) {
            MessageUtil.error(e).report().terminate();
        }

        /** This class loader will prefer loading the milled class over the non-milled class if the two got same name
         *
         */

        List<Class> milledClasses = new ArrayList<>();
        addMillrClassPathToClassLoader(customClassLoader);

        for (String name : classes) {
            Class c = customClassLoader.loadClass(name);
            System.out.println(MillrClassLoader.doesComeFromMillr(c));
            Arrays.stream(c.getInterfaces()).forEach(e->System.out.println(e.getCanonicalName()));
            System.out.println("MilledInstance Class Loader" + c.getClassLoader());
            milledClasses.add(customClassLoader.loadClass( name));
        }
        return milledClasses;
    }


    public static void addMillrClassPathToClassLoader(URLClassLoader cl) throws NoSuchMethodException, MalformedURLException, InvocationTargetException, IllegalAccessException {
        File millrFile = new File(OUTPUTMILLEDCLASSES);
        Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        addURL.invoke(cl, millrFile.toURI().toURL());
    }


    public static String removeFormerMillFromClassPath(String classPath, String millFolder) throws IOException {
        String millPath = new File(millFolder).getCanonicalPath();
        StringBuilder sr = new StringBuilder();
        Arrays.stream(classPath.split(File.pathSeparator)).map(e->{ try{return new File(e).getCanonicalPath();}catch(Exception ex){return e;}}).filter(e-> !e.equals(millPath)).forEach(e->sr.append(e).append(File.pathSeparator.toString()));
        return sr.toString();
    }



}
