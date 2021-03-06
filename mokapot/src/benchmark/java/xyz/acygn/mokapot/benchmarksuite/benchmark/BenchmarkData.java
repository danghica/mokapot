package xyz.acygn.mokapot.benchmarksuite.benchmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This class stores information on where the p12 key and password are located
 * on the disk.
 *
 * At present, the information is read from the <code>benchmark-config</code>
 * directory of <code>build-internal</code>, allowing it to be generated by the
 * build system.
 */
public abstract class BenchmarkData {

    public static String keyLocation = "build-internal/benchmark-config/client.p12";
    public static String passwordLocation = "build-internal/benchmark-config/password.txt";
    public static String mokapotServersDataLocation = "build-internal/benchmark-config/mokapot-servers.txt";
    public static String rMIServersDataLocation = "build-internal/benchmark-config/rmi-servers.txt";

    /**
     * Reads the password from the given file
     *
     * @return The password
     */
    public static String getPassword() {
        Scanner scanner;
        try {
            scanner = new Scanner(new File(passwordLocation));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        String password = new String();
        while (scanner.hasNextLine()) {
            password += scanner.nextLine();
        }
        scanner.close();
        return password;
    }

    /**
     * Returns the List of Mokapot servers
     *
     * @return The List of Mokapot servers
     */
    public static List<ServerInfo> getMokapotServers() {
        List<ServerInfo> mokapotServers = new ArrayList<ServerInfo>();
        Scanner scanner;
        try {
            scanner = new Scanner(new File(mokapotServersDataLocation));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();
            mokapotServers.add(new ServerInfo(input.split(" ")[0], Integer.parseInt(input.split(" ")[1])));
        }
        scanner.close();
        return mokapotServers;
    }

    /**
     * Returns the List of RMI servers
     *
     * @return The List of RMI servers
     */
    public static List<ServerInfo> getRMIServers() {
        List<ServerInfo> rMIServers = new ArrayList<ServerInfo>();
        Scanner scanner;
        try {
            scanner = new Scanner(new File(rMIServersDataLocation));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();
            rMIServers.add(new ServerInfo(input.split(" ")[0], Integer.parseInt(input.split(" ")[1])));
        }
        scanner.close();
        return rMIServers;
    }
}
