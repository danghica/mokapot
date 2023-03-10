package xyz.acygn.millr.mokapotArrayTest;

import xyz.acygn.millr.mokapotsemantics.StateTracker;
import xyz.acygn.millr.mokapotsemantics.WeakObject;
import xyz.acygn.millr.util.ObjectArrayWrapper;
import xyz.acygn.millr.util.intArrayWrapper;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;

import java.io.*;
import java.net.InetAddress;
import java.net.URLClassLoader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class Test {

    private static DistributedCommunicator communicator;
    private static CommunicationAddress remote;

    private static List<Process> listMokapotServer = new ArrayList<>();
    static int portMokapot = 15238;
    static int portMokapotDebug = 15239;
    static String keyClient = "millr/src/test/keyOne.p12";
    static String keyServer = "millr/src/test/keyTwo.p12";
    static final String password = "password";

    static class ObjectArray {

        Object[] storage;

        public ObjectArray() {
            storage = new Object[0];
        }

        public ObjectArray(Class<?> clazz, int... dims) {
            storage = (Object[]) java.lang.reflect.Array.newInstance(clazz, dims);
        }

        public Object[] getStorage() {
            return storage;
        }
    }

    public static void main(String... args) {
        try {
            try {

                System.out.println(System.getProperty("java.class.path"));

 //               startMokapotServer(password, System.getProperty("java.class.path"), keyServer, portMokapot, portMokapotDebug);
                startMokapotClient(keyClient, password);
//                ObjectArray arr = new ObjectArray(int.class, 5, 10);
//                Object value = ObjectArray.class.getMethod("getStorage").invoke(arr);
//                Class c = value.getClass();
//                System.out.println(c);
//
//
//
//                ObjectArrayWrapper obj = ObjectArrayWrapper.getObjectArrayWrapper(int.class, new int[]{10, 20});
//                obj.getReferentClass(null);
//
//                StateTracker.intializeVariable((URLClassLoader) Test.class.getClassLoader());
//                StateTracker st = new StateTracker((URLClassLoader) Test.class.getClassLoader(), false);
//                Object o = new Object(){ObjectArrayWrapper arr = ObjectArrayWrapper.getObjectArrayWrapper(int.class, new int[]{10, 20});};
//                st.register(o);
//                st.collectAll();
//                System.out.println(st.getStateToString());
//
//
//                WeakObject wo = new WeakObject(ObjectArrayWrapper.getObjectArrayWrapper(int.class, new int[]{10, 20}));
 //               intArrayWrapper array = xyz.acygn.millr.util.intArrayWrapper.getintArrayWrapper(10);
  //             Object o = communicator.getTestHooks().makeLongReference(new TreeSet<>());
    //            Object o = communicator.getTestHooks().describe(new TreeSet());

                Object object = communicator.runRemotely(() -> new TreeSet(), remote);
                System.out.println(object.toString());
                intArrayWrapper intW = (intArrayWrapper) object;

            } catch (Throwable e) {
                System.out.println("Exception !");
                printThrowableStackTrace(e);
            }
        } finally {
            for (Process server : listMokapotServer) {
                try {
                    server.waitFor(10, TimeUnit.SECONDS);
                    server.destroy();
                } catch (InterruptedException e) {
                    //  e.printStackTrace();
                }
                server.destroyForcibly();
            }
        }
    }

    private static void printThrowableStackTrace(Throwable t) {
        System.out.println("Stack");
        System.out.println(t.getMessage());
        printStackTrace(t.getStackTrace());
        System.out.println("End Stack");
        if (t.getCause() != null) {
            printThrowableStackTrace(t.getCause());
        }
    }

    private static void printStackTrace(StackTraceElement[] stackTrace) {
        for (StackTraceElement elem : stackTrace) {
            System.out.println(" at " + elem.getClassName() + " method: " + elem.getMethodName() + " line: " + elem.getLineNumber());
        }
    }

    private static void startMokapotClient(String key, String password) throws IOException, KeyStoreException, KeyManagementException {
        communicator = new DistributedCommunicator(key, password.toCharArray());
        communicator.enableTestHooks();
        communicator.startCommunication();
        remote = communicator.lookupAddress(InetAddress.getByName("10.0.2.15"), 15238);
        //InetAddress.getLoopbackAddress(), portMokapot);
    }

    private static void startMokapotServer(String password, String classPath, String keyFile, int port, int portDebug) throws IOException {
        System.out.println("Starting mokapot on koport" + port);
        Process server = new ProcessBuilder("java",
                "-classpath", classPath,
                "-Djava.security.policy=millr/src/test/localhost.policy",
                "-Djava.security.manager",
                "-ea", "-Xdebug", "-Xnoagent",
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
}
