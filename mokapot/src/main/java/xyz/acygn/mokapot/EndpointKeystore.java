package xyz.acygn.mokapot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import xyz.acygn.mokapot.util.EnumerationIterator;

/**
 * The cryptographic material used by an endpoint in a distributed connection.
 * It contains both the material for the individual endpoint, and for the
 * white-list used by the communication as a whole.
 * <p>
 * This is normally stored on disk in the form of a password-protected PKCS#12
 * file, but it has some additional constraints (beyond those of a standard
 * PKCS#12 file):
 * <ul>
 * <li>The file must contain a single alias, <code>endpointkey</code>;
 * <li>This alias must correspond to a certificate chain of length 2 (with a
 * private key for the head of the chain but not the tail);
 * <li>The head of the chain corresponds to the cryptographic material that this
 * endpoint will use when authenticating itself; the tail to the cryptographic
 * material that will be used to authenticate other endpoints and check whether
 * they are authorised.
 * </ul>
 * A particular endpoint is considered to be authorised to participate in the
 * communication if its own certificate has been signed by the white-list's.
 *
 * @author Alex Smith
 */
public class EndpointKeystore {

    /**
     * The name of the only alias that should appear in an endpoint keystore.
     */
    private final static String ALIAS_NAME = "endpointkey";

    /**
     * The keystore that contains the cryptographic material.
     */
    private final KeyStore keyStore;

    /**
     * The certificate chain associated with the public half of the keystore's
     * only alias.
     */
    private final X509Certificate[] chain;

    /**
     * An SSL context that uses this keystore for authentication and
     * authorisation.
     */
    private final SSLContext context;

    /**
     * Creates a new endpoint keystore from a .p12 file. The keystore will be
     * verified to ensure that it has the correct format.
     *
     * @param fileName The filename of the file to load.
     * @param password An array holding the password that protects the file.
     * This will be zeroed before the function returns.
     * @return The newly constructed keystore.
     * @throws KeyStoreException If something goes wrong reading the keystore
     * (other than an issue with retrieving its bits from disk), or the keystore
     * is in an unexpected format
     * @throws KeyManagementException If something goes wrong setting up
     * authorisation/authentication routines
     * @throws IOException If the file containing the keystore could not be
     * retrieved from disk
     */
    public static EndpointKeystore fromFile(String fileName, char[] password)
            throws KeyStoreException, KeyManagementException, IOException {
        try {
            KeyStore keystore = KeyStore.getInstance("pkcs12");
            try {
                keystore.load(new FileInputStream(new File(fileName)), password);
            } catch (NoSuchAlgorithmException | CertificateException ex) {
                throw new KeyStoreException(ex);
            }
            return new EndpointKeystore(keystore, password);
        } finally {
            /* Note: this zeroes the password twice in the case where everything
               succeeds, but it's a trivial performance loss and reduces the
               chance of logic bugs in security-sensitive code as far as
               possible */
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Creates a new endpoint from a loaded keystore. The keystore will be
     * verified to ensure it has the correct format.
     *
     * @param keyStore A loaded keystore in the expected format.
     * @param keyPassword An array holding the password that protects the keys
     * within the file. This will be zeroed before the function returns.
     * @throws KeyStoreException If something goes wrong reading the keystore,
     * or the keystore is in an unexpected format
     * @throws KeyManagementException If something goes wrong setting up
     * authorisation/authentication routines
     */
    public EndpointKeystore(KeyStore keyStore, char[] keyPassword)
            throws KeyStoreException, KeyManagementException {
        try {
            this.keyStore = keyStore;

            /* Verify that we have the right alias pattern. */
            boolean ok = false;
            for (String alias : new EnumerationIterator<>(keyStore.aliases())) {
                if (ALIAS_NAME.equals(alias)) {
                    ok = true;
                } else {
                    throw new KeyStoreException(
                            "The keystore should contain only one alias, '"
                            + ALIAS_NAME + "', but contains alias '"
                            + alias + "'");
                }
            }
            if (!ok) {
                throw new KeyStoreException("The keystore should contain "
                        + "an alias '" + ALIAS_NAME + ", but is empty");
            }

            /* Get the certificate chain, and verify that each certificate in it
               is an X509 certificate. */
            try {
                chain = Arrays.stream(keyStore.getCertificateChain(ALIAS_NAME))
                        .map((cert) -> (X509Certificate) cert).toArray(
                        X509Certificate[]::new);
            } catch (ClassCastException ex) {
                throw new KeyStoreException("The keystore should contain only "
                        + "X.509 certificates");
            }

            /* Verify that the certificate chain has the right length. */
            if (chain.length != 2) {
                throw new KeyStoreException("The keystore's alias '"
                        + ALIAS_NAME + "' should have a length 2 chain");
            }

            try {
                /* Create the SSL context. */
                context = SSLContext.getInstance("TLSv1.2");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
                kmf.init(keyStore, keyPassword);
                context.init(kmf.getKeyManagers(),
                        new TrustManager[]{new KeystoreTrustManager()}, null);
            } catch (NoSuchAlgorithmException ex) {
                throw new KeyManagementException(ex);
            } catch (UnrecoverableKeyException ex) {
                throw new KeyStoreException("The given password does not "
                        + "match that of the private key in the keystore", ex);
            }
        } finally {
            Arrays.fill(keyPassword, '\0');
        }
    }

    /**
     * Given the certificate chain of a remote endpoint, ensure that it's on the
     * white-list of approved certificates. This will be done by ensuring that
     * the chain has the correct structure, then attempting to verify its head
     * against the white-list's public key.
     *
     * @param checkChain The chain to check.
     * @throws CertificateException If the remote endpoint could not be
     * authenticated or could not be authorised
     */
    void checkChainTrusted(X509Certificate[] checkChain)
            throws CertificateException {
        if (checkChain.length != 2) {
            throw new CertificateException("The remote endpoint's certificate "
                    + "has an unexpected chain structure");
        }
        try {
            checkChain[0].verify(chain[1].getPublicKey());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex) {
            throw new CertificateException("The remote endpoint's certificate "
                    + "is not on the whitelist", ex);
        } catch (NoSuchProviderException ex) {
            /* Apparently the crypto code isn't running? We can't do much about
               this. */
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns the cryptographic material as a Java <code>KeyStore</code>.
     *
     * @return A loaded keystore.
     */
    KeyStore getKeyStore() {
        return keyStore;
    }

    /**
     * Returns the serial number of this endpoint's public key. This is used as
     * a unique identifier for a given system within a given communication,
     * because all certificates on a white-list will (if the white-list was
     * generated correctly) have distinct serial numbers.
     *
     * @return The serial number.
     */
    public Number getSerial() {
        BigInteger serial = chain[0].getSerialNumber();

        /* If the serial number is small, pack it into a smaller Number type so
           that the address itself becomes smaller. */
        try {
            return serial.intValueExact();
        } catch (ArithmeticException ex) {
            try {
                return serial.longValueExact();
            } catch (ArithmeticException ex2) {
                return serial;
            }
        }
    }

    /**
     * Returns an SSL context that can be used to make secure connections using
     * the cryptographic material in this keystore.
     *
     * @return The SSL context.
     */
    SSLContext getContext() {
        return context;
    }

    /**
     * An implementation of <code>X509TrustManager</code> that uses the
     * cryptographic material in this keystore. It ensures that only connections
     * that are on the whitelist will be authenticated. (Note that this does not
     * directly impact authorisation; if the socket authorises unauthenticated
     * endpoints to connect, there's not much the trust manager can do. As such,
     * it's highly recommended that the endpoint is re-verified to ensure that
     * the certificate is actually present, authenticated, and authorised.)
     */
    private class KeystoreTrustManager implements X509TrustManager {

        /**
         * Given the certificate chain of a remote endpoint which made an
         * inbound connection to this endpoint, ensure that it's on the
         * white-list of approved certificates.
         *
         * @param checkChain The chain to check.
         * @param ignored Ignored.
         * @throws CertificateException If the remote endpoint could not be
         * authenticated or could not be authorised
         */
        @Override
        public void checkClientTrusted(X509Certificate[] checkChain,
                String ignored) throws CertificateException {
            checkChainTrusted(checkChain);
        }

        /**
         * Given the certificate chain of a remote endpoint which we just made
         * an outbound connection to, ensure that it's on the white-list of
         * approved certificates.
         *
         * @param checkChain The chain to check.
         * @param ignored Ignored.
         * @throws CertificateException If the remote endpoint could not be
         * authenticated or could not be authorised
         */
        @Override
        public void checkServerTrusted(X509Certificate[] checkChain,
                String ignored) throws CertificateException {
            checkChainTrusted(checkChain);
        }

        /**
         * Returns the white-list's public certificate. (This is the only
         * certificate that we'd accept as a chain root for the given
         * certificate.)
         *
         * @return An array containing only the white-list's public certificate.
         */
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{chain[1]};
        }
    }
}
