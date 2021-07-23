package xyz.acygn.mokapot.whitelist;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * A specific implementation of a whitelist controller. This is the interface
 * via which the main <code>WhitelistController</code> class calls out to the
 * methods that actually do the work of generating the cryptographic material.
 *
 * @author Alex Smith
 */
public interface WhitelistControllerEngine {

    /**
     * Specifies the initial content of the file that holds the private key. The
     * whitelist controller itself is created via calling
     * <code>initialize</code> to create the file, followed by
     * <code>generate</code> on the location of the created file (this is in two
     * stages, because <code>generate</code> may need the file to already exist,
     * but <code>initialize</code>'s caller needs to create the file itself to
     * ensure that the permissions are correct).
     * <p>
     * This method may be called while <code>this</code> is a partially
     * constructed object, and thus should not read instance fields.
     *
     * @param os The stream to which the initial contents of the file should be
     * written.
     * @param password The password that should protect the file (this may be
     * ignored if partially-constructed whitelist controllers do not yet make
     * use of the password). The content of this array should not be modified.
     * @throws IOException If the output stream could not be written to
     */
    void initialize(OutputStream os, char[] password) throws IOException;

    /**
     * Generates a fresh private key for use as the whitelist controller. The
     * <code>whitelistLocation</code> will be the location on disk of a file
     * whose initial contents were created using <code>initialize</code>.
     * <p>
     * This method may be called while <code>this</code> is a partially
     * constructed object, and thus should not read instance fields.
     *
     * @param whitelistLocation The location of the file in which to generate
     * the whitelist controller.
     * @param password The password to use. The content of this array should not
     * be modified.
     * @param whitelistName A human-readable string that identifies the
     * whitelist.
     * @param expiration The number of days before the entire whitelist should
     * stop functioning.
     * @throws IOException If something went wrong reading or writing the
     * whitelist controller
     */
    void generate(Path whitelistLocation, char[] password,
            String whitelistName, int expiration) throws IOException;

    /**
     * Uses an existing whitelist controller to create a new entry in the
     * whitelist that it controls.
     *
     * @param whitelistLocation The (existing) file containing the whitelist
     * controller.
     * @param entryLocation The place at which the (newly created) entry file
     * should be written.
     * @param whitelistPassword The password that guards the whitelist. The
     * content of this array should not be modified.
     * @param entryPassword The password that guards the whitelist entry. The
     * content of this array should not be modified.
     * @param entryName A human-readable string that identifies the entry within
     * the whitelist.
     * @param expiration The number of days before the new entry should stop
     * functioning.
     * @throws IOException If something went wrong reading the whitelist
     * controller or writing the new whitelist entry
     */
    void newEntry(Path whitelistLocation, Path entryLocation,
            char[] whitelistPassword, char[] entryPassword,
            String entryName, int expiration) throws IOException;
}
