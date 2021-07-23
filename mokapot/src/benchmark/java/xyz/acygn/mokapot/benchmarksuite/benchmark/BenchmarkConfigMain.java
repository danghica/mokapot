package xyz.acygn.mokapot.benchmarksuite.benchmark;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import xyz.acygn.mokapot.whitelist.KeytoolWhitelistControllerEngine;
import xyz.acygn.mokapot.whitelist.WhitelistController;

/**
 * A small program to generate a default configuration for the benchmarking
 * system.
 *
 * @author Alex Smith
 */
public class BenchmarkConfigMain {

    /**
     * Creates and populates the directory
     * <code>build-internal/benchmark-config</code>.
     * <p>
     * The created configuration will describe one Mokapot server running on
     * port 15238, together with cryptographic material for it and a client; and
     * an RMI server running on port 15239.
     *
     * @param args Ignored.
     * @throws IOException If something goes wrong populating the directory
     */
    public static void main(String[] args) throws IOException {
        Path benchmarkConfigPath = FileSystems.getDefault().getPath(
                ".", "build-internal", "benchmark-config");
        Files.createDirectories(benchmarkConfigPath);

        WhitelistController wlc = new WhitelistController(
                new KeytoolWhitelistControllerEngine(), "benchmark", 1);
        char[] p12password = WhitelistController.generatePassword();
        wlc.newEntry(benchmarkConfigPath.resolve("client.p12"),
                p12password, "Benchmarking Client", 1);
        wlc.newEntry(benchmarkConfigPath.resolve("server1.p12"),
                p12password, "Benchmarking Server 1", 1);

        try (FileOutputStream fos = new FileOutputStream(
                benchmarkConfigPath.resolve("password.txt").toFile())) {
            for (char c : p12password) {
                /* Note: this is writing encoded as Latin-1, contrary to what
                   the name of the method might imply, but that's OK because
                   generatePassword is limited to printable ASCII */
                fos.write(c);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(
                benchmarkConfigPath.resolve("mokapot-servers.txt").toFile())) {
            fos.write("localhost 15238\n".getBytes("UTF-8"));
        }
        try (FileOutputStream fos = new FileOutputStream(
                benchmarkConfigPath.resolve("rmi-servers.txt").toFile())) {
            fos.write("localhost 15239\n".getBytes("UTF-8"));
        }
    }
}
