package xyz.acygn.millr.testclasses;

import xyz.acygn.millr.mokapotsemantics.IsMokapotVersion;
import xyz.acygn.millr.mokapotsemantics.MillUtil;
import xyz.acygn.millr.mokapotsemantics.TrackableSampleProgram;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class RunTestClasses {

    public static List<TrackableSampleProgram> getClasses(String password, String key, int port) throws Exception{
        DistributedCommunicator communicator = new DistributedCommunicator(key, password.toCharArray());
        communicator.startCommunication();
        CommunicationAddress remote = communicator.lookupAddress(InetAddress.getLoopbackAddress(), port);

        /**
         * We load all the classes that are in the subtree of RunTestClasses by doing a file exploration.
         */
        URLClassLoader urlClassLoader = (URLClassLoader) RunTestClasses.class.getClassLoader();
        try {
            URL runTestClassURL = urlClassLoader.findResource(RunTestClasses.class.getName().replace(".", "/") + ".class");

        File folderTestFiles = new File(new File(runTestClassURL.getFile()).getParent());
        List<File> listFiles = MillUtil.walk(folderTestFiles, file->file.getPath().endsWith(".class"));
        List<Class> listClasses = listFiles.stream().map(e->{
            String RelativeNameClass = new File(new File(runTestClassURL.getFile()).getParent()).toPath().relativize(e.toPath()).toString();
            String fullNameClass = new File( new File(RunTestClasses.class.getName().replace(".", "/")).getParent()
                    , RelativeNameClass).toString();
            return fullNameClass.substring(0, fullNameClass.length() - ".class".length());
        })
                .map(e-> {
                    try {
                        return urlClassLoader.loadClass(e.replace("/", "."));
                    } catch (ClassNotFoundException | NoClassDefFoundError e1) {
                        throw new RuntimeException(e1);
                    }
                }).filter(e-> TrackableSampleProgram.class.isAssignableFrom(e)).collect(Collectors.toList());

        return listClasses.stream().map(c-> {
            System.out.println("insn");
                    if (IsMokapotVersion.class.isAssignableFrom(c)) {
                        try {
                            return (TrackableSampleProgram) c.getConstructor(DistributedCommunicator.class, CommunicationAddress.class).newInstance(communicator, remote);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        return (TrackableSampleProgram) c.getConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }).collect(Collectors.toList());
        }
        catch(NoSuchElementException ex){
            System.out.println("n");
            throw ex;
        }

    }
}
