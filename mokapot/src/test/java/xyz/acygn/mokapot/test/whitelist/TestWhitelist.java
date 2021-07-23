package xyz.acygn.mokapot.test.whitelist;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Arrays;
import xyz.acygn.mokapot.EndpointKeystore;
import xyz.acygn.mokapot.test.SerialTests;
import xyz.acygn.mokapot.test.SimpleTest;
import xyz.acygn.mokapot.test.TestGroup;
import xyz.acygn.mokapot.whitelist.KeytoolWhitelistControllerEngine;
import static xyz.acygn.mokapot.whitelist.KeytoolWhitelistControllerEngine.x509EscapeDName;
import static xyz.acygn.mokapot.whitelist.KeytoolWhitelistControllerEngine.x509UnescapeDName;
import xyz.acygn.mokapot.whitelist.WhitelistController;

/**
 * Tests for the <code>xyz.acygn.mokapot.whitelist</code> package.
 *
 * @author Alex Smith
 */
public class TestWhitelist {

    /**
     * Runs the tests. Outputs of the test are presented on standard output, in
     * Test Anything Protocol format.
     *
     * @param args Ignored.
     */
    public static void main(String[] args) {
        TestGroup tests = new SerialTests("whitelist test",
                new SimpleTest(3, "generatePassword", (tg) -> {
                    char[] password1 = WhitelistController.generatePassword();
                    tg.ok(String.valueOf(password1).matches("^[ -~]{64}$"),
                            "first random password has the right format");
                    tg.comment("Password: " + String.valueOf(password1));
                    char[] password2 = WhitelistController.generatePassword();
                    tg.ok(String.valueOf(password2).matches("^[ -~]{64}$"),
                            "second random password has the right format");
                    tg.comment("Password: " + String.valueOf(password2));
                    tg.ok(!Arrays.equals(password1, password2),
                            "the two random passwords are different");
                }),
                new SimpleTest(12, "x509EscapeDName", (tg) -> {
                    testDName(tg, "Abc", "CN=Abc", "basic ASCII");
                    testDName(tg, " abc", "CN=\\ abc", "leading space");
                    testDName(tg, "# a  ", "CN=\\# a \\ ",
                            "leading hash, trailing space");
                    testDName(tg, "+#,-/;:<\"\\;>",
                            "CN=\\+#\\,-/\\;:\\<\\\"\\\\\\;\\>",
                            "miscellaneous punctuation");
                    testDName(tg, "a\0b\1c", "CN=a\\00b\1c",
                            "non-ASCII characters");
                    testDName(tg, "CN=abc; CN=de\\0f",
                            "CN=CN=abc\\; CN=de\\\\0f",
                            "double escaping");
                }),
                new SimpleTest(2, "create temporary whitelist controller", (tg) -> {
                    WhitelistController wlc = new WhitelistController(
                            new KeytoolWhitelistControllerEngine(),
                            "Temporary Test Whitelist", 1);
                    KeyStore ks = wlc.asKeyStore();
                    tg.ok(ks.containsAlias("whitelist"),
                            "contains alias 'whitelist'");
                    ks.getCertificate("whitelist").getPublicKey().getEncoded();
                    tg.ok(true, "can fetch an encoded version of the certificate");
                }),
                new SimpleTest(5, "create permanent whitelist controller", (tg) -> {
                    Path tempFile = Paths.get("./build-internal/testwlc.p12");
                    if (tempFile.toFile().exists()) {
                        tempFile.toFile().delete();
                    }
                    char[] password = "test123456".toCharArray();
                    byte[] encodedCert;
                    {
                        WhitelistController wlc = new WhitelistController(
                                new KeytoolWhitelistControllerEngine(),
                                tempFile, password, "Test Whitelist", 1);
                        tempFile.toFile().deleteOnExit();
                        KeyStore ks = wlc.asKeyStore();
                        tg.ok(ks.containsAlias("whitelist"),
                                "contains alias 'whitelist' when created");
                        encodedCert = ks.getCertificate("whitelist")
                                .getPublicKey().getEncoded();
                    }
                    tg.okEq(password, new char[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                            "password was zeroed out");
                    password = "test123456".toCharArray();
                    {
                        WhitelistController wlc = new WhitelistController(
                                new KeytoolWhitelistControllerEngine(),
                                tempFile, password);
                        KeyStore ks = wlc.asKeyStore();
                        tg.ok(ks.containsAlias("whitelist"),
                                "contains alias 'whitelist' when reloaded");
                        byte[] encodedCert2 = ks.getCertificate("whitelist")
                                .getPublicKey().getEncoded();
                        tg.okEq(encodedCert2, encodedCert,
                                "public key is still the same");
                    }
                    tg.okEq(password, new char[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                            "password was zeroed out");
                }),
                new SimpleTest(3, "create whitelist entry", (tg) -> {
                    Path tempEntry = Paths.get("./build-internal/testentry.p12");
                    if (tempEntry.toFile().exists()) {
                        tempEntry.toFile().delete();
                    }

                    WhitelistController wlc = new WhitelistController(
                            new KeytoolWhitelistControllerEngine(),
                            "Temporary Test Whitelist", 1);
                    char[] password = WhitelistController.generatePassword();
                    wlc.newEntry(tempEntry, password, "Test Entry", 1);
                    tempEntry.toFile().deleteOnExit();

                    tg.ok(tempEntry.toFile().exists(), "test entry was created");

                    KeyStore ks = KeyStore.getInstance("PKCS12");
                    try (InputStream is = new FileInputStream(tempEntry.toFile())) {
                        ks.load(is, password);
                    }
                    tg.ok(ks.containsAlias("endpointkey"),
                            "contains alias 'endpointkey'");

                    EndpointKeystore eks = new EndpointKeystore(ks, password);
                    tg.ok(true, "format suitable for EndpointKeystore");
                })
        );
        tests.plan();
        tests.runTest(1);
        System.exit(TestGroup.getTestingStatus(true));
    }

    /**
     * Tests that the specified human readable name and X.509 name match. This
     * produces two <code>ok()</code> calls, one testing encoding, one testing
     * decoding.
     *
     * @param tg The test group on which to make the <code>ok()</code> calls.
     * @param humanReadableName The human-readable name to test.
     * @param x509Name The corresponding X.509 name to test.
     * @param testName The name to use for the test.
     */
    private static void testDName(TestGroup tg,
            String humanReadableName, String x509Name, String testName) {
        tg.okEq(x509EscapeDName(humanReadableName),
                x509Name, testName + ": encode");
        tg.okEq(x509UnescapeDName(x509Name),
                humanReadableName, testName + ": decode");
    }
}
