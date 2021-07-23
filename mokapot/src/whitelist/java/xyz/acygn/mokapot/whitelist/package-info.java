/**
 * Utilities for manipulating whitelists and cryptographic material.
 * <p>
 * For security, all communications between the various Java virtual machines
 * involved in a distributed computation are encrypted and authenticated. This
 * is done by means of PKCS #12 (".p12") files; each Java virtual machine
 * involved in the computation has one such file. Each such file contains two
 * pieces of information:
 * </p><ol><li>A public and private key, specific to that .p12 file (thus
 * specific to one Java virtual machine), which are used to sign and encrypt the
 * communications;</li>
 * <li>A public key shared between all .p12 files that belong to the same "set"
 * of .p12 files, that signs each of the specific public keys, and thus makes it
 * possible for the .p12 files to recognise each other.</li></ol><p>
 * As such, each computation has a set of .p12 files that all recognise each
 * other, ensuring that unauthorised connections do not have any impact on the
 * computation. Such a set is called a "whitelist" (each of the .p12 files
 * recognises each of the others, and rejects any other cryptographic material,
 * and thus acts as a whitelist of which Java virtual machines can connect).
 * </p><p>
 * In order to create a whitelist, the necessary information is the private key
 * corresponding to the shared public key; as long as the private key in
 * question is available, new .p12 files of the correct form can be created.
 * Once that private key is lost, the whitelist is still usable, but becomes
 * "fixed" with no ability to add new .p12 files to it. The private key in
 * question is thus known as a "whitelist controller" because it provides the
 * necessary information to update the whitelist.
 * </p><p>
 * This package contains two major classes. <code>WhitelistController</code>
 * represents a whitelist controller; it corresponds to a private key on disk
 * (which could be a temporary file which is deleted when the program stops
 * running). <code>WhitelistUI</code> is an application that the user can use to
 * create new whitelists, either from the command line or via a GUI.
 * </p>
 */
package xyz.acygn.mokapot.whitelist;
