package xyz.acygn.mokapot.whitelist;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.lang.ProcessBuilder.Redirect.INHERIT;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static java.util.concurrent.TimeUnit.SECONDS;
import xyz.acygn.mokapot.util.ThreadUtils;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptions;

/**
 * An implementation of <code>WhitelistControllerEngine</code> that works via
 * executing <code>keytool</code> in a new process. This is not quite as secure
 * as some alternatives (e.g. because <code>keytool</code> can duplicate serial
 * numbers, because the passwords are passed via environment variables and thus
 * may be viewable to other processes running under the same user ID, and
 * because it's therefore impossible to wipe passwords from memory once they've
 * been used), but is likely to be portable to most systems capable of running
 * Java.
 *
 * @author Alex Smith
 */
public class KeytoolWhitelistControllerEngine implements WhitelistControllerEngine {

    @Override
    public void initialize(OutputStream os, char[] password)
            throws IOException {
        KeyStore store;
        try {
            store = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException ex) {
            throw new IOException("Unrecognised keystore type PKCS12", ex);
        }
        try {
            /* A <code>null</code> argument to store.load creates an empty
               keystore */
            store.load(null, null);
            store.store(os, password);
        } catch (KeyStoreException | NoSuchAlgorithmException
                | CertificateException ex) {
            throw new IOException("Issue storing keystore", ex);
        }
    }

    @Override
    public void generate(Path whitelistLocation, char[] password,
            String whitelistName, int expiration) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("keytool",
                "-genkeypair", "-alias", "whitelist", "-keyalg", "ec",
                "-dname", x509EscapeDName(whitelistName), "-ext",
                "BC:c=ca:TRUE,pathlen:0", "-ext", "KU=kCS",
                "-validity", Integer.toString(expiration),
                "-storetype", "pkcs12", "-storepass:env", "CONTROLLERPASSWORD",
                "-keystore", whitelistLocation.toString());
        pb.environment().put("CONTROLLERPASSWORD", String.valueOf(password));
        runKeytool(pb, new byte[0]);
    }

    @Override
    public void newEntry(Path whitelistLocation, Path entryLocation,
            char[] whitelistPassword, char[] entryPassword,
            String entryName, int expiration) throws IOException {
        /* Step 1: create the entry's keypair. We store this on disk, because
           that's what keytool wants; we place the keypair at the place where
           the eventual .p12 file should go (because keytool -importcert will
           update the certificate in-place). */
        ProcessBuilder pb = new ProcessBuilder("keytool",
                "-genkeypair", "-alias", "endpointkey", "-keyalg", "ec",
                "-dname", x509EscapeDName(entryName), "-ext",
                "BC:c=ca:FALSE", "-ext", "KU=keyE", "-ext", "eKU=sA,cA",
                "-validity", Integer.toString(expiration),
                "-storetype", "pkcs12", "-storepass:env", "ENTRYPASSWORD",
                "-keystore", entryLocation.toString());
        pb.environment().put("ENTRYPASSWORD", String.valueOf(entryPassword));
        runKeytool(pb, new byte[0]);

        /* Step 2: Make the CSR, from the entry's keypair. */
        pb = new ProcessBuilder("keytool",
                "-certreq", "-alias", "endpointkey", "-storepass:env",
                "ENTRYPASSWORD", "-keystore", entryLocation.toString());
        pb.environment().put("ENTRYPASSWORD", String.valueOf(entryPassword));
        byte[] csr = runKeytool(pb, new byte[0]);

        /* Step 3: Make the certificate, using the CSR. */
        pb = new ProcessBuilder("keytool",
                "-gencert", "-rfc", "-alias", "whitelist", "-keyalg", "ec",
                "-validity", Integer.toString(expiration),
                "-storetype", "pkcs12", "-storepass:env", "CONTROLLERPASSWORD",
                "-keystore", whitelistLocation.toString());
        pb.environment().put("CONTROLLERPASSWORD",
                String.valueOf(whitelistPassword));
        byte[] cert = runKeytool(pb, csr);

        /* Step 4: Make the certificate chain. */
        pb = new ProcessBuilder("keytool",
                "-exportcert", "-rfc", "-alias", "whitelist",
                "-storetype", "pkcs12", "-storepass:env", "CONTROLLERPASSWORD",
                "-keystore", whitelistLocation.toString());
        pb.environment().put("CONTROLLERPASSWORD",
                String.valueOf(whitelistPassword));
        byte[] certChainTail = runKeytool(pb, new byte[0]);
        byte[] certChain = new byte[cert.length + certChainTail.length];
        System.arraycopy(cert, 0, certChain, 0, cert.length);
        System.arraycopy(certChainTail, 0, certChain, cert.length,
                certChainTail.length);

        /* Step 5: Add the certificate chain to our partially constructed
           .p12 file. */
        pb = new ProcessBuilder("keytool",
                "-importcert", "-alias", "endpointkey",
                "-storetype", "pkcs12", "-storepass:env", "ENTRYPASSWORD",
                "-noprompt", "-keystore", entryLocation.toString());
        pb.environment().put("ENTRYPASSWORD", String.valueOf(entryPassword));
        runKeytool(pb, certChain);
    }

    /**
     * Given a <code>ProcessBuilder</code> representing a <code>keytool</code>
     * process, runs it in a controlled manner. Its standard error stream will
     * be inherited, and it will be given 10 seconds to run before being
     * forcibly destroyed.
     *
     * @param pb A process builder with command line, argument, and disposition
     * @param stdin The bytes to give to the <code>keytool</code> process on its
     * standard input.
     * @return The bytes output by the <code>keytool</code> process on its
     * standard output.
     * @throws IOException If something goes wrong running the process
     */
    private static byte[] runKeytool(ProcessBuilder pb,
            byte[] stdin) throws IOException {
        pb.redirectError(INHERIT);
        final Process keytool = pb.start();
        final BlockingQueue<Optional<IOException>> senderExceptionQueue
                = new LinkedBlockingQueue<>();
        final BlockingQueue<Object> outputReceiverQueue
                = new LinkedBlockingQueue<>();

        /* Note: we need to use separate threads to do the inputting and
           outputting as keytool can block on output from us, and we can block
           on output from keytool */
        Thread inputSender = new Thread(() -> {
            try (OutputStream os = new BufferedOutputStream(keytool.getOutputStream())) {
                os.write(stdin);
            } catch (IOException ex) {
                delayInterruptions(() -> senderExceptionQueue.put(
                        Optional.of(ex)));
                return;
            }
            delayInterruptions(() -> senderExceptionQueue.put(
                    Optional.empty()));
        });
        inputSender.start();
        Thread outputReceiver = new Thread(() -> {
            try (InputStream is = new BufferedInputStream(keytool.getInputStream());
                    ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                for (;;) {
                    int b = is.read();
                    if (b == -1) {
                        break;
                    }
                    os.write(b);
                }
                byte[] ba = os.toByteArray();
                delayInterruptions(() -> outputReceiverQueue.put(ba));
            } catch (IOException ex) {
                delayInterruptions(() -> outputReceiverQueue.put(ex));
            }
        });
        outputReceiver.start();

        Optional<IOException> ex1 = ThreadUtils.delayInterruptionsRv(
                () -> senderExceptionQueue.poll(10, SECONDS));
        Object rv = ThreadUtils.delayInterruptionsRv(
                () -> outputReceiverQueue.poll(10, SECONDS));

        /* outputReceiver joining means that the process has probably already
           ended, but give it some time to wind down cleanly if necessary */
        delayInterruptions(() -> keytool.waitFor(1, SECONDS));
        if (keytool.isAlive()) {
            keytool.destroyForcibly();
            delayInterruptions(() -> keytool.waitFor(1, SECONDS));
            if (keytool.isAlive()) {
                throw new IOException("could not shut down keytool");
            }
        }

        if (keytool.exitValue() != 0) {
            boolean rvIsASCII = false;
            StringBuilder errmsg = new StringBuilder();
            if (rv instanceof byte[]) {
                rvIsASCII = true;
                for (byte b : (byte[]) rv) {
                    /* Printable ASCII is ' ' (32) to '~' (126), plus tab (8)
                       and newline (10)*/
                    if ((b < 32 || b > 126) && b != 8 && b != 10) {
                        rvIsASCII = false;
                        break;
                    } else {
                        errmsg.append((char) b);
                    }
                }
            }

            if (rvIsASCII) {
                throw new IOException("Keytool failed with error exit code "
                        + keytool.exitValue() + ": " + errmsg.toString());
            } else {
                throw new IOException("Keytool failed with error exit code "
                        + keytool.exitValue());
            }
        } else if (ex1.isPresent()) {
            throw ex1.get();
        } else if (rv instanceof IOException) {
            throw (IOException) rv;
        } else if (rv instanceof byte[]) {
            return (byte[]) rv;
        } else {
            throw new ClassCastException("Unexpected return value type: "
                    + rv.getClass());
        }
    }

    /**
     * Produces a representation of the given string in a form that's suitable
     * for an X.509 Distinguished Name. "CN=" will be prefixed to the argument,
     * and the following characters will be escaped: leading space or hash;
     * trailing space; NUL, double quote, plus, comma, semicolon, less than,
     * greater than, backslash. The escape syntax in most cases is to precede
     * the character that needs escaping by a backslash; however, NUL is escaped
     * as a backslash followed by two zeroes.
     *
     * @param humanReadableName The human-readable string that's escaped into a
     * machine-readable Distinguished Name
     * @return <code>humanReadableName</code>, escaped into Distinguished Name
     * syntax.
     */
    public static String x509EscapeDName(String humanReadableName) {
        String dn = humanReadableName;
        dn = dn.replaceAll("(^[ #]|[\"+,;<>\\\\]|[ ]$)", "\\\\$1");
        dn = dn.replaceAll("\0", "\\\\00");
        return "CN=" + dn;
    }

    /**
     * Reverses the operation of x509EscapeDName.
     *
     * @param x509Name The encoded form of an X.509 Distinguished Name
     * @return The CN field of that name.
     */
    public static String x509UnescapeDName(String x509Name) {
        String hn = x509Name;
        /* To simplify backslashing matching, replace \\ with \5C. */
        hn = hn.replaceAll("\\\\\\\\", "\\\\5C");
        /* Now replace escaped semicolons with \3B. */
        hn = hn.replaceAll("\\\\;", "\\\\3B");
        /* Delete everything before the start of the CN field value. */
        hn = hn.replaceAll("(^|.*;) *CN=", "");
        /* Delete everything beyond the end of the CN field value. */
        hn = hn.replaceAll(";.*", "");
        /* Unescape all escaped punctuation. */
        hn = hn.replaceAll("\\\\([^0-9a-zA-Z])", "$1");
        /* Restore \5C, \3B, \00. */
        hn = hn.replaceAll("\\\\00", "\0");
        hn = hn.replaceAll("\\\\3B", ";");
        hn = hn.replaceAll("\\\\5C", "\\\\");
        return hn;
    }
}
