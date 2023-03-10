package xyz.acygn.millr.mokapotsemantics;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.net.ssl.SNIHostName;
import javax.swing.plaf.nimbus.State;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.*;
import java.util.*;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Explores the semantics of execution for milled and distributed versions of
 * the same code. In particular, for each of the programs used in the testing,
 * checks that the semantics of the local, milled local, and milled mokapot
 * versions are the same.
 *
 * @author Marcello De Bernardi, Thomas Cuvillier
 */
@SuppressWarnings("Duplicates")
public class MokapotSemanticsTest implements PathConstant {

    static List<TrackableSampleProgram> listTrackableOriginal;
    static List<TrackableSampleProgram> listTrackableMilled;

    private static List<Process> listMokapotServer = new ArrayList<>();

    /**
     * To understand the structure of this class, one needs to understand the
     * structure of the whole project.\ The goal is to start from some
     * TestClasses, mill them, and "mokapot" them.
     * <p>
     * Hence four different versions of the test classes will run. The original,
     * the milled, the mokapot one, and the milled mokapot one. The milled ones
     * have the same names as the mokapot ones and hence must be loaded by a
     * different ClassLoader.
     * <p>
     * We hence need 2 ClassLoader for running the test: - The one for the
     * original version (OriginalClassLoader) -The one for the milled one
     * (MilledClassLoader).
     * <p>
     * The original have no dependencies. The mokapot ones have dependencies on
     * mokapot. Since mokapot will introspect those classes,it is of the
     * uppermost importance that two different versions of mokapot are loaded by
     * these two ClassLoaders. Note that loading mokapot requires also loading
     * the jar in "contrib", and the classes in "Common".
     * <p>
     * <p>
     * Therefore, this file cannot have dependency on contrib / common / mokapot
     * in order to prevent an "escape" from our custom ClassLoaders, that would
     * lead to a ClassNotFoundException. Notably, we cannot have this package to
     * depends on millr or Common. That is why we access some of the millr /
     * Common features in this package through reflection, making it really
     * fragile for API changes.
     * <p>
     * <p>
     * Finally, we need a ClassLoader for milling the files. This one should
     * have access to millr and the TestFiles in its ClassPath. Notably, millr
     * is dependent on Common and some files in contrib, that would need to be
     * loaded as well.
     * <p>
     * Note that TrackableSampleProgram and StateTracker are both part of this
     * package and no the TestFiles.This way, they are both loaded by the
     * SystemClassLoader (or whatever ClassLoader that loads this class), and
     * therefore are common for both the milled and non milled versions. This
     * allows us to have a common framework for testing the classes.
     * <p>
     * <p>
     * /**
     * Before any tests are run, the original sample class is milled and the
     * milled version is then loaded using a ClassLoader.
     */
    @BeforeAll
    @SuppressWarnings("Duplicates")
    static void init() throws Exception {

        System.getSecurityManager().checkPermission(new AllPermission());
        System.getSecurityManager().checkPermission(new RuntimePermission("createClassLoader"));

        class testPolicy extends Policy {

            @Override
            public PermissionCollection getPermissions(CodeSource codesource) {
                return super.getPermissions(codesource);
            }

            @Override
            public PermissionCollection getPermissions(ProtectionDomain domain) {
                return super.getPermissions(domain);
            }

            @Override
            public boolean implies(ProtectionDomain domain, Permission permission) {
                return super.implies(domain, permission);
            }

        }

        File outputFile = new File(OUTPUTMILLEDCLASSES);
        outputFile.mkdirs();
        Permission perm = new RuntimePermission("accessDeclaredMembers");
        Permission permTwo = new RuntimePermission("createClassLoader");
        System.out.println(MokapotSemanticsTest.class.getProtectionDomain().getPermissions().implies(perm));
        System.out.println(MokapotSemanticsTest.class.getProtectionDomain().implies(perm));
        java.security.AccessController.checkPermission(perm);
        System.getSecurityManager().checkPermission(permTwo);
        System.getSecurityManager().checkCreateClassLoader();
        /**
         * Since we will use a range of URLClassLoaders, we transform each path
         * in PathConstant into an URL.
         */
        URL mokapotURL = new File(MOKAPOTCLASSES).toURI().toURL();
        URL originalTestClassesURL = new File(OUTPUTMILLRTESTCLASSES).toURI().toURL();
        URL outputURL = outputFile.toURI().toURL();
        URL testMainURL = new File(OUTPUTMILLRTESTMAIN).toURI().toURL();
        URL millrURL = new File(MILLRCLASSES).toURI().toURL();
        URL commonURL = new File(COMMONCLASSES).toURI().toURL();

        /**
         * ContribURL is a list of URLS for those jar in contrib.
         */
        List<URL> contribURL = new ArrayList<>();
        File f = new File("contrib");
        for (File potentialLibrary : f.listFiles()) {
            if (potentialLibrary.getPath().endsWith(".jar")) {
                contribURL.add(potentialLibrary.toURI().toURL());
            }
        }

        /**
         * These are the tests classes to be loaded.
         */
        String millrPrefix = "xyz.acygn.millr.testclasses.";

        /**
         * The first step is to milled them. For that, we will prepare a
         * ClassLoader with the ability of milling them.
         */
        String classPath = System.getProperty("java.class.path");

        List<URL> listURLClassLoader = Arrays.stream(classPath.split(File.pathSeparator)).map(e -> {
            try {
                try {
                    return (new File((String) e)).getCanonicalFile().toURI().toURL();
                } catch (IOException ex) {
                    return new File(e).toURI().toURL();
                }
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());

        List<URL> listURLMokapotAndTrackable = new ArrayList<>(listURLClassLoader);
        listURLMokapotAndTrackable.add(mokapotURL);
        listURLMokapotAndTrackable.add(testMainURL);
        listURLMokapotAndTrackable.add(millrURL);
        listURLMokapotAndTrackable.add(commonURL);
        listURLMokapotAndTrackable.addAll(contribURL);

        MillrClassLoader classLoaderForMillr = new MillrClassLoader(listURLMokapotAndTrackable.toArray(new URL[0]), MokapotSemanticsTest.class);

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    MillUtil.mill(classLoaderForMillr);
                } catch (IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
        //List<Class> listMillClasses = MillUtil.mill(classLoaderForMillr);

        List<URL> listURLMilledClasses = new ArrayList<>(listURLClassLoader);
        listURLMilledClasses.add(mokapotURL);
        listURLMilledClasses.add(commonURL);
        listURLMilledClasses.add(outputURL);
        listURLMilledClasses.addAll(contribURL);
        listURLMilledClasses.add(millrURL);
        //listURLMilledClasses.add(testRunnerURL);
        URL[] urls = (URL[]) listURLMilledClasses.toArray(new URL[0]);
        URLClassLoader milledClassesClassLoader = new MillrClassLoader(urls, MokapotSemanticsTest.class);

        List<URL> listURLOriginalClasses = new ArrayList<>(listURLMilledClasses);
        listURLOriginalClasses.remove(outputURL);
        listURLOriginalClasses.remove(millrURL);
        listURLOriginalClasses.add(originalTestClassesURL);
        urls = (URL[]) listURLOriginalClasses.toArray(new URL[0]);
        URLClassLoader mokapotOriginalClassLoader = new MillrClassLoader(urls, MokapotSemanticsTest.class);

        // setup mokapot
        StringBuilder classPathMilledClassesBuilder = new StringBuilder();
        listURLMilledClasses.stream().forEach(e -> classPathMilledClassesBuilder.append(e.getFile()).append(File.pathSeparator));
        String classPathMilledClasses = classPathMilledClassesBuilder.toString();

        StringBuilder classPathOriginalClassesBuilder = new StringBuilder();
        listURLOriginalClasses.stream().forEach(e -> classPathOriginalClassesBuilder.append(e.getFile()).append(File.pathSeparator));
        String classPathOriginalClasses = classPathOriginalClassesBuilder.toString();

        try {

            int portMilledCommunication = 15238;
            int portOriginalCommunication = 15239;

            int portDebugMilledCommunication = 15234;
            int portDebugOriginalCommunication = 15236;

            startMokapot("password", classPathOriginalClasses, "millr/src/test/keyOne.p12", portOriginalCommunication, portDebugOriginalCommunication);
            startMokapot("password", classPathMilledClasses, "millr/src/test/keyOne.p12", portMilledCommunication, portDebugMilledCommunication);

            Class c = mokapotOriginalClassLoader.loadClass("xyz.acygn.millr.testclasses.RunTestClasses");
            listTrackableOriginal = (List<TrackableSampleProgram>) c.getMethod("getClasses", String.class, String.class, int.class).invoke(
                    null, "password", "millr/src/test/keyTwo.p12", portOriginalCommunication);

            c = milledClassesClassLoader.loadClass("xyz.acygn.millr.testclasses.RunTestClasses");
            listTrackableMilled = (List<TrackableSampleProgram>) c.getMethod("getClasses", String.class, String.class, int.class).invoke(
                    null, "password", "millr/src/test/keyTwo.p12", portMilledCommunication);

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Test
    void testAll() {
        listTrackableOriginal.stream().filter(e -> !(e instanceof IsMokapotVersion)).forEach(originalTrack -> {
            System.out.println(originalTrack.getClass().getName());
            TrackableSampleProgram milledTrack = listTrackableMilled.stream().filter(e -> e.getClass().getName().equals(originalTrack.getClass().getName())).findAny().get();
            TrackableSampleProgram milledMokapotTrack = listTrackableMilled.stream().filter(e -> e.getClass().getName().equals(originalTrack.getClass().getName().
                    substring(0, originalTrack.getClass().getName().length() - "local".length()) + "Mokapot"))
                    .findAny().get();
            try {
                originalTrack.run();
                milledTrack.run();
                milledMokapotTrack.run();
            } catch (Exception ex) {
                System.out.println("PRINTING");
                printThrowableStackTrace(ex);

                System.out.println("END PRINTING");
                throw new RuntimeException(new Exception());
            }
            try {
                writeState(originalTrack, milledTrack, milledMokapotTrack);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            assertTrue(originalTrack.getStateTracker().getStateTrace().compareToStateTrace(milledTrack.getStateTracker().getStateTrace()));
            assertTrue(originalTrack.getStateTracker().getStateTrace().compareToStateTrace(milledMokapotTrack.getStateTracker().getStateTrace()));
        });
    }

    /**
     * Tests that the semantics of the directAccess program are unchanged by
     * milling, and by distributing the milled version using mokapot.
     */

    /*
    @Test
    void directAccessSemantics() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, Exception {

        StateTracker localSemantics = da_local.run();
        StateTracker localMilledSemantics = (StateTracker) da_localMilled.getClass().getMethod("run").invoke(da_localMilled, new Object[0]);
        StateTracker mokapotSemantics =  da_mokapot.run();
        StateTracker mokapotMilledSemantics = (StateTracker) da_mokapotMilled.getClass().getMethod("run").invoke(da_mokapotMilled, new Object[0]);

        assertTrue(localSemantics.getStateTrace().compareToStateTrace(localMilledSemantics.getStateTrace()));
        assertTrue(localSemantics.getStateTrace().compareToStateTrace(mokapotMilledSemantics.getStateTrace()));
        assertTrue(localSemantics.getStateTrace().compareToStateTrace(mokapotSemantics.getStateTrace()));
        System.out.println(localSemantics.getStateTrace().toString());
        System.out.println(mokapotSemantics.getStateTrace().toString());
        System.out.println(localMilledSemantics.getStateTrace().toString());
        System.out.println(mokapotMilledSemantics.getStateToString());

    }

     */
    /**
     * Tests that the semantics of the MatrixTransformationLocal program are
     * unchanged by milling, and by the milled mokapot version.
     */
//    @Test
//   void matrixTransformationSemantics() {
//        try {
//            StateTracker localSemantics = mt_local.run();
//            System.out.println("local Semantics ran");
//            StateTracker localMilledSemantics = (StateTracker) mt_localMilled.getClass().getMethod("run").invoke(mt_localMilled, new Object[0]);
//            System.out.println("local milled Semantics ran");
//            StateTracker mokapotMilledSemantics = (StateTracker) mt_mokapotMilled.getClass().getMethod("run").invoke(mt_mokapotMilled, new Object[0]);
//      //      System.out.println("mokapot milled Semantics ran");
//      //      System.out.println(localSemantics.getStateTrace().toString());
//     //       System.out.println(localMilledSemantics.getStateTrace().toString());
//            System.out.println(mokapotMilledSemantics.getStateToString());
//            assertTrue(localSemantics.getStateTrace().compareToStateTrace(localMilledSemantics.getStateTrace()));
//            assertTrue(localSemantics.getStateTrace().compareToStateTrace(mokapotMilledSemantics.getStateTrace()));
//            assertTrue(localSemantics.getStateTrace().compareToStateTrace(mokapotMilledSemantics.getStateTrace()));
//        } catch (Throwable t) {
//            printThrowableStackTrace(t);
//        }
//    }
    private void printThrowableStackTrace(Throwable t) {
        System.out.println(t.getMessage());
        t.printStackTrace(System.out);
        for (Throwable s : t.getSuppressed()) {
            printThrowableStackTrace(s);
        }
        if (t.getCause() != null) {
            printThrowableStackTrace(t.getCause());
        }
    }

//
//        assertAll(
//                () -> assertEquals(localSemantics.toString(), localMilledSemantics.toString())
//              //  () -> assertEquals(localSemantics.toString(), mokapotMilledSemantics.toString())
//        );
//        System.out.println("LOCAL and MOKAPOT: " + (localSemantics.equals(mokapotSemantics) ? "equal" : "different"));
    //  }
    @AfterAll
    static void terminate() throws InterruptedException {
        for (Process server : listMokapotServer) {
            server.destroy();
            server.waitFor(5, TimeUnit.SECONDS);
            server.destroyForcibly();
        }
    }

    private static void startMokapot(String password, String classPath, String keyFile, int port, int portDebug) throws IOException {
        System.out.println("Starting mokapot on koport" + port);
        Process server = new ProcessBuilder("java",
                "-classpath", classPath,
                "-Djava.security.policy=millr/src/test/localhost.policy",
                "-Djava.security.manager",
                "-ea", "-Xdebug", "-Xnoagent", "-Xmx2048m",
                "-Xrunjdwp:transport=dt_socket,server=y,address=" + String.valueOf(portDebug) + ",suspend=n",
                "xyz.acygn.mokapot.DistributedServer",
                "type=tcps; keystore=" + keyFile + "; password_file=/dev/stdin; host=127.0.0.1; port=" + port,
                "-d", "-w").redirectError(
                ProcessBuilder.Redirect.INHERIT).start();
        listMokapotServer.add(server);
        try (OutputStream serverInput = server.getOutputStream()) {
            serverInput.write(password.getBytes());
        }
        BufferedReader serverOutput
                = new BufferedReader(
                        new InputStreamReader(
                                new BufferedInputStream(server.getInputStream())));

        /* Wait for the server to setup. */
        String serverLine;
        do {
            serverLine = serverOutput.readLine();
            //   System.out.println("# server says: " + serverLine);
        } while (serverLine == null || !serverLine.startsWith("Server is now ready"));
        System.out.println("Mokapot started");

    }

    private void writeState(TrackableSampleProgram original, TrackableSampleProgram milled, TrackableSampleProgram milledMokopat) throws IOException {
        List<Snapshot> originalTrace = original.getStateTracker().getStateTrace().getListSnapShot();
        List<Snapshot> milledTrace = milled.getStateTracker().getStateTrace().getListSnapShot();
        List<Snapshot> milledMokapotTrace = milled.getStateTracker().getStateTrace().getListSnapShot();
        String simpleName = originalTrace.getClass().getSimpleName();
        File outputFile = new File(simpleName);
        outputFile.createNewFile();
        FileWriter fileWriter = new FileWriter(outputFile);
        try {
            int length = Math.max(Math.max(originalTrace.size(), milledTrace.size()), milledMokapotTrace.size());
            for (int i = 0; i < length; i++) {
                fileWriter.write("Snapshot " + String.valueOf(i));
                List<List<Snapshot>> listTrace = Arrays.asList(new List[]{originalTrace, milledTrace, milledMokapotTrace});
                String[] listName = new String[]{"Original", "Milled", "Milled Mokapot"};
                for (int a = 0; a < listTrace.size(); a++) {
                    try {
                        fileWriter.write(listName[a]);
                        List<Snapshot> Trace = listTrace.get(a);
                        Snapshot snap = Trace.get(i);
                        fileWriter.write(Trace.toString());
                    } catch (Exception ex) {

                    }
                }
            }
        } finally {
            fileWriter.close();
        }
    }
}
