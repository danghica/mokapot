package xyz.acygn.mokapot.whitelist;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static xyz.acygn.mokapot.whitelist.KeytoolWhitelistControllerEngine.x509UnescapeDName;

/**
 * One entry in an unbuilt whitelist. This will eventually be used to generate a
 * <code>.p12</code> file, but for the time being it's just a (potentially
 * incomplete) list of information about what the final <code>.p12</code> file
 * will look like. You can think of this as a "partially constructed
 * <code>.p12</code> file".
 * <p>
 * Unlike most objects, objects implementing this interface are typically
 * mutable, as the fields in it are designed so that they can be presented to a
 * user and modified interactively.
 * <p>
 * Instances of this interface are also used to describe the parameters used to
 * generate unbuilt persistent whitelist controllers.
 *
 * @author Alex Smith
 */
interface WhitelistBuilderEntry {

    /**
     * Returns the filesystem location at which the <code>.p12</code> file will
     * be created.
     *
     * @return The filesystem location, or <code>null</code> if this has not yet
     * been specified.
     */
    Path getFilesystemLocation();

    /**
     * Sets the filesystem location at which the <code>.p12</code> file will be
     * created.
     *
     * @param newLocation The new filesystem location.
     */
    void setFilesystemLocation(Path newLocation);

    /**
     * The human-readable name of this whitelist controller or whitelist entry.
     *
     * @return The human-readable name. If this has not yet been specified, it
     * will be an empty string.
     */
    String getEntryName();

    /**
     * Sets the human-readable name of this whitelist controller or whitelist
     * entry.
     *
     * @param newEntryName The new human-readable name for the entry.
     */
    void setEntryName(String newEntryName);

    /**
     * Returns the length of time for which the newly generated whitelist or
     * entry will be valid. If this has not yet been specified, it will be at
     * its default of 1.
     *
     * @return The number of days for which the entry will be valid.
     */
    int getValidity();

    /**
     * Sets the length of time for which the newly generated whitelist or entry
     * will be valid.
     *
     * @param newValidity The number of days for which the entry should be
     * valid.
     */
    void setValidity(int newValidity);

    /**
     * Returns the password for this entry.
     *
     * @return The password, or an empty string if it has not yet been set. This
     * is a freshly allocated array (not a copy of an internal array storing a
     * password).
     */
    char[] getPassword();

    /**
     * Sets the password for this entry. Before calling this method, you should
     * call <code>clearPassword()</code> to ensure that the old password does
     * not persist in memory.
     *
     * @param newPassword The new password for this entry. This array should not
     * be modified or read by the caller after the method is called (i.e. the
     * object takes ownership of the array).
     * @see #clearPassword()
     */
    void setPassword(char[] newPassword);

    /**
     * Clears the "password" field of this whitelist builder entry in a secure
     * manner. After calling this, <code>getPassword()</code> will return an
     * empty array.
     */
    void clearPassword();

    /**
     * Returns whether this whitelist builder entry should be interpreted as
     * referring to a whitelist controller that already exists.
     *
     * @return The most recent value set with <code>setExisting</code>,
     * defaulting to <code>false</code>
     */
    boolean isExisting();

    /**
     * Specifies whether this whitelist builder entry refers to a whitelist
     * controller that already exists. (If it does, then the title and validity
     * fields cannot be set by the user.)
     *
     * @param existing <code>true</code> if the entry is supposed to refer to a
     * pre-existing whitelist controller.
     */
    void setExisting(boolean existing);

    /**
     * Verifies the fields of this whitelist builder entry to determine whether
     * all the information needed for generation is there. If the builder is
     * meant to refer to a new whitelist entry or whitelist controller, then its
     * location must be a nonexistent file in an existing directory, its name
     * must be nonempty, and its validity must be positive. If it's meant to
     * refer to an existing controller, then its location must already exist
     * (and be a loadable PKCS #12 file), and the password must match it. In any
     * case, the password must be at least 6 characters long.
     * <p>
     * When called on an entry that's meant to reflect an existing whitelist
     * controller, this method has a side effect: it will update the
     * <code>validity</code> and <code>entryName</code> (via the setter
     * methods).
     *
     * @param index The index that should be used within the resulting
     * <code>VerificationFailure</code> objects.
     * @return An unmodifiable list of the ways in which the whitelist builder
     * entry failed verification.
     */
    default List<VerificationFailure> listVerificationFailures(int index) {
        List<VerificationFailure> failures = new ArrayList<>();
        char[] password = getPassword();
        Path location = getFilesystemLocation();

        if (isExisting()) {
            if (location == null) {
                failures.add(new VerificationFailure(index, Parameter.LOCATION,
                        new FileNotFoundException("Location cannot be empty")));
            } else {
                try {
                    KeyStore ks = KeyStore.getInstance("PKCS12");
                    try (InputStream is = new FileInputStream(location.toFile())) {
                        ks.load(is, password);
                        if (!ks.containsAlias("whitelist")) {
                            throw new UnrecoverableEntryException(
                                    "This PCKS #12 file is not a whitelist controller");
                        }
                        X509Certificate cert
                                = (X509Certificate) ks.getCertificate("whitelist");
                        long remainingDays = Instant.now().until(
                                cert.getNotAfter().toInstant(), ChronoUnit.DAYS);
                        if (remainingDays > Integer.MAX_VALUE) {
                            remainingDays = Integer.MAX_VALUE;
                        }
                        cert.checkValidity();
                        /* If the certificate is valid, then it was valid when
                           the number of remaining days was calculated. So it
                           would have rounded down; we want to round up instead
                           when showing this value to the user. */
                        setValidity((int) remainingDays + 1);
                        String name = cert.getSubjectX500Principal().getName();
                        setEntryName(x509UnescapeDName(name));
                    }
                } catch (KeyStoreException | UnrecoverableEntryException
                        | IOException | NoSuchAlgorithmException
                        | IllegalArgumentException
                        | ClassCastException | CertificateException ex) {
                    failures.add(new VerificationFailure(
                            index, Parameter.LOCATION, ex));
                }
            }
        } else {
            if (getValidity() < 1) {
                failures.add(new VerificationFailure(index, Parameter.VALIDITY,
                        new IllegalArgumentException(
                                "Generated entries must be valid for at least 1 day")));
            }
            if (getEntryName().isEmpty()) {
                failures.add(new VerificationFailure(index, Parameter.NAME,
                        new IllegalArgumentException(
                                "Name must be nonempty")));
            }
            if (location == null) {
                failures.add(new VerificationFailure(index, Parameter.LOCATION,
                        new FileNotFoundException(
                                "Location cannot be empty")));
            } else if (location.toFile().exists()) {
                failures.add(new VerificationFailure(index, Parameter.LOCATION,
                        new FileAlreadyExistsException(
                                "This file already exists")));
            } else if (!location.getParent().toFile().isDirectory()) {
                failures.add(new VerificationFailure(index, Parameter.LOCATION,
                        new FileNotFoundException(
                                "The file can only be created within an existing directory")));
            }
        }

        if (password.length < 6) {
            failures.add(new VerificationFailure(index, Parameter.PASSWORD,
                    new IllegalArgumentException(
                            "Password must be at least 6 characters long")));
        }

        Arrays.fill(password, (char) 0);
        return Collections.unmodifiableList(failures);
    }

    /**
     * A way in which a whitelist builder entry failed verification.
     */
    public static class VerificationFailure {

        /**
         * Creates an object to describe a verification failure.
         *
         * @param index The index of the entry that failed verification within
         * the list of entries being generated (0 for a controller, positive for
         * an entry).
         * @param parameter Which parameter failed verification.
         * @param reason The reason that verification failed.
         */
        public VerificationFailure(
                int index, Parameter parameter, Exception reason) {
            this.index = index;
            this.parameter = parameter;
            this.reason = reason;
        }

        /**
         * The index of the entry that failed verification. This is 0 for an
         * entry referring to a whitelist controller, positive for an entry that
         * refers to something else. The indexes are relative to the list of
         * entries being generated.
         */
        public final int index;

        /**
         * Which of the entry's parameters failed verification.
         */
        public final Parameter parameter;

        /**
         * The nature of the verification failure.
         */
        public final Exception reason;
    }

    /**
     * The four parameters of a whitelist builder entry. This is the name,
     * location, password, and validity.
     */
    public static enum Parameter {
        /**
         * The filesystem location of the entry's or controller's
         * <code>.p12</code> file.
         */
        LOCATION,
        /**
         * The password that protects the entry or controller.
         */
        PASSWORD,
        /**
         * The human-readable name of the entry or controller.
         */
        NAME,
        /**
         * The number of days for which the entry or controller will be valid.
         */
        VALIDITY;
    }
}
