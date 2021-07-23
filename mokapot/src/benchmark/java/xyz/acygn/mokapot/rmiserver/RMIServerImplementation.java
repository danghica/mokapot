package xyz.acygn.mokapot.rmiserver;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Simple implementation of an RMI server capable of accepting arbitrary computational
 * tasks and returning a result.
 *
 * TODO: add functionality like logging etc
 *
 * @author Marcello De Bernardi
 */
public class RMIServerImplementation implements RMIServer {
    private static int port;

    /**
     * Executes the given executable on the RMI server.
     */
    @Override
    public <T> T execute(Executable<T> executable) throws RemoteException {
        return executable.execute(port);
    }


    /**
     * Server application entry point.
     *
     * @param args cli parameters: The first should be the location of the .p12 key, which gets ignored, and the second should be the port
     */
    public static void main(String[] args) {
        // install security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        port = args.length < 1 ? 15400 : Integer.parseInt(args[0]);

        // create instance of RMI server and export it to RMI registry
        try {
            RMIServer server = new RMIServerImplementation();
            RMIServer serverStub = (RMIServer) UnicastRemoteObject.exportObject(server, port);
            System.out.println("Created server stub on port " + port);

            LocateRegistry.createRegistry(port);
            System.out.println("Created registry on port " + port);

            LocateRegistry.getRegistry(port).rebind("RMIServer", serverStub);
            System.out.println("RMI server bound to registry on port " + port);

        } catch (Exception e) {
            System.err.println("Failed to create and bind server.");
            e.printStackTrace();
        }
    }
}
