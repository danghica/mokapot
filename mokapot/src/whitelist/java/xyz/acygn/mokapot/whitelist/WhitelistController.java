package xyz.acygn.mokapot.whitelist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import xyz.acygn.mokapot.util.BackgroundGarbageCollection;
import xyz.acygn.mokapot.util.Pair;

/**
 * A private key that allows the creation of new .p12 files that are part of an
 * existing whitelist. Each <code>WhitelistController</code> file corresponds to
 * a file on disk (possibly a temporary file) that holds a private key.
 *
 * @author Alex Smith
 */
public class WhitelistController {

    /**
     * Random number generator used to generate secure cryptographic material
     * (typically passwords).
     */
    protected static final SecureRandom CSPRNG = new SecureRandom();

    /**
     * The implementation of the actual cryptographic methods.
     */
    private final WhitelistControllerEngine engine;

    /**
     * The location at which the private key is stored. This may also contain
     * other information useful for a whitelist controller, e.g. a method of
     * avoiding duplicate serial numbers.
     */
    private final Path privateKeyLocation;

    /**
     * The password which protects the private key.
     */
    private final char[] password;

    /**
     * Creates a new whitelist controller in a temporary file, using a randomly
     * generated password. When the Java virtual machine exits, the password
     * will be forgotten (making the temporary file useless), and there will
     * also be an attempt to delete the file.
     * <p>
     * Note: this constructor calls the virtual methods <code>initialize</code>
     * and <code>generate</code>, which must therefore be written so that they
     * can handle partially constructed objects. (All the methods in
     * <code>WhitelistController</code> itself can handle such objects, but if
     * an overriding class adds any fields, they may not be initialised at the
     * time at which <code>initialize</code> and <code>generate</code> are
     * called.)
     *
     * @param engine The code that actually does the generation of cryptographic
     * material.
     * @param whitelistName A human-readable string allowing identification of a
     * specific whitelist.
     * @param expiration The number of days before the entire newly created
     * whitelist should stop functioning.
     * @throws IOException If something goes wrong trying to create the file
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public WhitelistController(WhitelistControllerEngine engine,
            String whitelistName, int expiration) throws IOException {
        this(engine, markDeleteOnExit(Files.createTempFile("mokapot-whitelist-",
                ".p12", PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString("rw-------")))),
                generatePassword(), whitelistName, expiration);
    }

    /**
     * Creates a whitelist controller object to describe an existing whitelist
     * controller file.
     *
     * @param engine The code that actually does the generation of cryptographic
     * material.
     * @param controllerLocation The location of the existing whitelist
     * controller file.
     * @param password The password protecting the existing whitelist controller
     * file. This array will be cleared to all-zeroes before the constructor
     * exits.
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public WhitelistController(WhitelistControllerEngine engine,
            Path controllerLocation, char[] password) {
        this.password = Arrays.copyOf(password, password.length);
        Arrays.fill(password, (char) 0);

        final char[] passwordCopy = this.password;
        BackgroundGarbageCollection.addFinaliser(this,
                () -> Arrays.fill(passwordCopy, (char) 0));

        this.engine = engine;
        this.privateKeyLocation = controllerLocation;
    }

    /**
     * Creates a new whitelist controller file and corresponding whitelist
     * controller object.
     *
     * @param engine The code that actually does the generation of cryptographic
     * material.
     * @param controllerLocation The location at which to place the new
     * whitelist controller file. The file is expected to be persistent, i.e.
     * will not be automatically deleted even after the Java virtutal machine
     * exits.
     * @param password The password that should protect the new whitelist
     * controller file. This array will be cleared to all-zeroes before the
     * constructor exits.
     * @param whitelistName A human-readable string allowing identification of a
     * specific whitelist.
     * @param expiration The number of days before the entire newly created
     * whitelist should stop functioning.
     * @throws IOException If something goes wrong trying to create the file
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public WhitelistController(WhitelistControllerEngine engine,
            Path controllerLocation, char[] password, String whitelistName,
            int expiration) throws IOException {
        this.password = Arrays.copyOf(password, password.length);
        Arrays.fill(password, (char) 0);

        final char[] passwordCopy = this.password;
        BackgroundGarbageCollection.addFinaliser(this,
                () -> Arrays.fill(passwordCopy, (char) 0));

        this.engine = engine;
        this.privateKeyLocation = controllerLocation;
        File pklFile = privateKeyLocation.toFile();
        try (OutputStream os = new FileOutputStream(pklFile)) {
            engine.initialize(os, this.password);
        }
        engine.generate(privateKeyLocation, this.password,
                whitelistName, expiration);
    }

    /**
     * Creates a new entry on the controlled whitelist. That is, a new
     * <code>.p12</code> file that will recognise and be recognised by all the
     * existing files on the whitelist.
     *
     * @param location The location on disk where the new <code>.p12</code> file
     * should be written.
     * @param password The password that should be used to protect the new
     * <code>.p12</code> file. This will <i>not</i> be automatically wiped to
     * all-zeroes (as the caller is likely to want to immediately use it to make
     * use of the new <code>.p12</code> file); wiping it is the caller's
     * responsibility.
     * @param name A human-readable name for the new whitelist entry.
     * @param expiration The number of days before the new entry should stop
     * functioning.
     * @throws IOException If something went wrong creating the entry
     */
    public void newEntry(Path location, char[] password, String name,
            int expiration) throws IOException {
        engine.newEntry(privateKeyLocation, location, this.password, password,
                name, expiration);
    }

    /**
     * Attempts to convert this whitelist controller to a Java
     * <code>KeyStore</code>. Depending on how the whitelist controller is
     * implemented on disk, this might or might not be a meaningful operation;
     * it's mostly intended for testing purposes.
     *
     * @return A <code>KeyStore</code> containing the same cryptographic
     * material as this whitelist controller
     * @throws KeyStoreException If the <code>KeyStore</code> cannot be created
     * @throws IOException If the key material on disk cannot be read
     * @throws NoSuchAlgorithmException If the key material on disk is encoded
     * in a way that Java doesn't understand
     * @throws CertificateException If the key material on disk contains an
     * invalid certificate
     */
    public KeyStore asKeyStore() throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(privateKeyLocation.toFile())) {
            ks.load(is, password);
            return ks;
        }
    }

    /**
     * Convenience method for generating a set of endpoint files that all belong
     * to the same whitelist. Both the whitelist, and the generated files, will
     * be temporary; an attempt will be made to delete them once the JVM exits,
     * and even if they are not deleted for some reason, they will be useless at
     * this point.
     *
     * @param engine The code that actually does the generation of cryptographic
     * material.
     * @param quantity The number of endpoint keystores to create.
     * @param expiration The length of time the keystores should be valid, in
     * days. (This should normally be 1, unless you're calling this method from
     * a process that expects to run for more than one day.)
     * @return An array of <code>quantity</code> endpoint keystores, all of
     * which belong to the same whitelist.
     * @throws java.io.IOException If something goes wrong creating the files
     * backing the whitelist (e.g. insufficient disk space to create them)
     */
    public static List<Pair<File, char[]>> generateEndpointKeystoreSet(
            WhitelistControllerEngine engine, int quantity, int expiration)
            throws IOException {
        WhitelistController wlc = new WhitelistController(engine,
                "Temporary Whitelist Controller", expiration);
        List<Pair<File, char[]>> rv = new ArrayList<>(quantity);

        /* Because we're creating files via keytool (not via Java), we need to
           place them in a secure directory in order to ensure that the
           resulting files are secure at the OS level. This creates an extra
           layer of security over relying on the fact that the passwords are,
           as far as possible, confined to the memory of the processes we
           use. */
        Path tempdir = Files.createTempDirectory("mokapot-entry-",
                PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString("rwx------")));
        tempdir.toFile().deleteOnExit();

        for (int i = 0; i < quantity; i++) {
            char[] password = generatePassword();
            Path location = tempdir.resolve(i + ".p12");
            wlc.newEntry(location, password,
                    "Temporary Whitelist Entry " + (i + 1), expiration);
            location.toFile().deleteOnExit();
            rv.add(new Pair<>(location.toFile(), password));
        }

        return Collections.unmodifiableList(rv);
    }

    /**
     * Function that returns a random password. This is intended for situations
     * in which a key on disk is protected by a password in memory, that will be
     * used only in an automated way and thus does not need to be memorable.
     *
     * @return A string of 64 characters, drawn from printable ASCII.
     */
    public static char[] generatePassword() {
        char[] rv = new char[64];
        AtomicInteger i = new AtomicInteger(0);
        /* the range includes the low end (32 = ' '), but not the high end,
           so the last generatable character is 127 = '~'; note that the use of
           atomics here ensures that each array element will be initialized
           exactly once, and as all the values are random anyway it doesn't
           matter if they end up initializing out of order */
        CSPRNG.ints(64, 32, 127).forEach((c) -> {
            rv[i.getAndAdd(1)] = (char) c;
        });
        return rv;
    }

    /**
     * Marks a <code>Path</code> for deletion when the Java virtual machine
     * exits, and returns that Path. This exists as a separate method to get
     * around the requirement that <code>this</code>/<code>super</code> must be
     * the first line of a constructor.
     *
     * @param p The path that should be deleted when the virtual machine exits.
     * @return <code>p</code>.
     */
    private static Path markDeleteOnExit(Path p) {
        p.toFile().deleteOnExit();
        return p;
    }
}
