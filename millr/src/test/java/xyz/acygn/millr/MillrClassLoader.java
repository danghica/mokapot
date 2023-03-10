package xyz.acygn.millr;

import xyz.acygn.millr.util.util.MillredClass;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class MillrClassLoader extends URLClassLoader {

        URL[] directURLs;

        public MillrClassLoader(URL[] urls){
            super(urls, ClassLoader.getSystemClassLoader());
            directURLs = urls;
//           listClassCreated = new ArrayList<>();

        }

//        @Override
//        public Class loadClass(String name) throws ClassNotFoundException {
//            System.out.println("MILLRClassLoader attemps to load " + name);
//              //  if (name.startsWith("java.lang") || name.startsWith("java.io") || name.startsWith("java.security") || name.startsWith("java.net")){
//            if (name.startsWith("java") || name.startsWith("sun")){
//                    System.out.println("Relying on Super");
//                        return super.loadClass(name);
//                    }
//            try {
//                Class c = findClass(name);
//                System.out.println("Found the class");
//                if (doesComeFromMillr(c)){
//                    System.out.println("Comes from millr, we return it");
//                    return c;
//                }
//                if (c.getCanonicalName().startsWith("xyz.acygn.mokapot")){
//                    System.out.println("Comes from mokapot, we return it");
//                    return c;
//                }
//                else{
//                    System.out.println("Does not comes from mokapot-millr, we rely on super");
//                    return super.loadClass(name);
//                }
//            }
//            catch(Throwable t){
//                try{
//                   Enumeration<URL> resources = getResources(name.replace(".", "/")+ ".class");
//                   while (resources.hasMoreElements()){
//                        System.out.println("Resource found" +  resources.nextElement().getFile());
//                    }
//                    System.out.println("THE RESOURCE CHOSEN" + getResource(name.replace(".", "/")+ ".class").getFile());
//                   System.out.println(t.getMessage());
//                   System.out.println(t.getClass().getCanonicalName());
//                   t.printStackTrace();
//                }
//                catch(Throwable tr){
//
//                }
//
//
//                System.out.println("Have not found the class, relying on super");
//                return super.loadClass(name);
//            }
//        }
//
    public static boolean doesComeFromMillr(Class c){
        return Arrays.stream(c.getInterfaces()).anyMatch(e->e.getCanonicalName().equals(MillredClass.class.getCanonicalName()));
    }
//
//    @Override
//    public URL getResource(String name){
//        try {
//            Enumeration<URL> resources = getResources(name);
//            while (resources.hasMoreElements()){
//                URL potentialURL = resources.nextElement();
//                if (Arrays.stream(directURLs).anyMatch(e-> potentialURL.getFile().startsWith( e.getFile()))){
//                    return potentialURL;
//                }
//            }
//            return super.getResource(name);
//        } catch (IOException e) {
//            return null;
//        }
//    }
//
//    List<Class> listClassCreated;
//
//    @Override
//    public Class findClass(String name) throws ClassNotFoundException {
//            try{
//                return super.findClass(name);
//            }
//            catch(Throwable  e){
//                Optional<Class> potentialClass = listClassCreated.stream().filter(claz->claz.getCanonicalName().equals(name)).findAny();
//                if (((Optional) potentialClass).isPresent()){
//                    return potentialClass.get();
//                }
//                URL resource = getResource(name.replace(".", "/")+ ".class");
//                if (resource==null){
//                    throw new ClassNotFoundException(name);
//                }
//                try {
//                    byte[] b = Files.readAllBytes(new File(resource.getFile()).toPath());
//                    Class c = defineClass( name, b, 0, b.length);
//                    if (c==null){
//                        throw new ClassNotFoundException(name);
//                    }
//                    listClassCreated.add(c);
//                    return c;
//                }
//                catch (Throwable eTwo){
//                    eTwo.printStackTrace();
//                    throw new  ClassNotFoundException(name, eTwo);
//                }
//            }
//    }



}
