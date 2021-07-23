package xyz.acygn.mokapot;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import xyz.acygn.mokapot.util.ServerSocketLike;
import xyz.acygn.mokapot.util.ServerSocketWrapper;
import xyz.acygn.mokapot.util.SocketLike;
import xyz.acygn.mokapot.util.SocketWrapper;

/**
 * A communication endpoint that communicates using TLS over TCP. Authentication
 * and authorisation are carried out using an <code>EndpointKeystore</code>
 * provided to the constructor.
 *
 * @author Alex Smith
 */
public class SecureTCPCommunicationEndpoint implements CommunicationEndpoint {

    /**
     * The cryptographic material used by the endpoint.
     */
    private final EndpointKeystore keyStore;

    /**
     * The address part of the endpoint. If we can listen, this will be a
     * <code>TCPCommunicationAddress</code> with our host and port. Otherwise,
     * this will be a <code>OutboundOnlyCommunicationAddress</code> with our
     * certificate's serial number.
     */
    private final CommunicationAddress address;

    /**
     * Creates a new secure TCP communication endpoint that supports both
     * inbound and outbound connections.
     *
     * @param keyStore The cryptographic material used to secure the endpoint.
     * @param ipAddress The IP address which the endpoint uses for connections
     * (i.e. the globally visible IP address of the local system).
     * @param port The port on which the endpoint listens.
     */
    public SecureTCPCommunicationEndpoint(EndpointKeystore keyStore,
            InetAddress ipAddress, int port) {
        this.keyStore = keyStore;
        this.address = TCPCommunicationAddress.fromInetAddress(
                keyStore.getSerial(), ipAddress, port);
    }

    /**
     * Creates a new secure TCP communication endpoint that supports outbound
     * connections only. No listening will be done; any received data will be
     * received via the return half of an outgoing connection.
     *
     * @param keyStore The cryptographic material used to secure the endpoint.
     */
    public SecureTCPCommunicationEndpoint(EndpointKeystore keyStore) {
        this.keyStore = keyStore;
        this.address = new OutboundOnlyCommunicationAddress(
                keyStore.getSerial());
    }

    @Override
    public CommunicationAddress getAddress() {
        return address;
    }

    @Override
    public SocketLike newConnection(Communicable remoteAddress)
            throws IOException, IncompatibleEndpointException {
        if (!(remoteAddress instanceof TCPCommunicable)) {
            throw new IncompatibleEndpointException("Remote address "
                    + remoteAddress
                    + " does not support inbound TCP connections");
        }
        TCPCommunicable a = (TCPCommunicable) remoteAddress;

        SSLSocket sendSocket
                = (SSLSocket) (keyStore.getContext().getSocketFactory()
                        .createSocket(a.asInetAddress(), a.getTransmissionPort()));
        sendSocket.setNeedClientAuth(true);
        /* We can reverify the socket immediately, because it's already
           connected. */
        SocketLike rv = new SocketWrapper(sendSocket);
        initialVerifySocket(rv);
        return rv;
    }

    @Override
    public ServerSocketLike newListenSocket() throws IOException,
            UnsupportedOperationException {
        if (!(address instanceof TCPCommunicationAddress)) {
            throw new UnsupportedOperationException("Local address "
                    + address + " does not support inbound connections");
        }
        TCPCommunicationAddress a = (TCPCommunicationAddress) address;

        SSLServerSocket receiveSocket
                = (SSLServerSocket) (keyStore.getContext().getServerSocketFactory()
                        .createServerSocket(a.getTransmissionPort()));
        receiveSocket.setNeedClientAuth(true);
        /* We can't reverify the socket immediately; we have to wait until we
           receive a connection first, as we need to be in communication with
           the other end to reverify it. */
        return new ServerSocketWrapper(receiveSocket);
    }

    /**
     * Creates a debug representation of this endpoint.
     *
     * @return A string containing the communication address of the endpoint.
     */
    @Override
    public String toString() {
        return "Endpoint{" + address + '}';
    }

    @Override
    public void initialVerifySocket(SocketLike socket) throws IOException {
        if (!(socket instanceof SocketWrapper)) {
            throw new IOException(
                    "Attempted to reverify a fake socket");
        }
        Socket wrappedSocket = ((SocketWrapper) socket).getSocket();
        if (!(wrappedSocket instanceof SSLSocket)) {
            throw new IOException(
                    "Attempted to reverify the wrong sort of socket");
        }
        SSLSocket castSocket = (SSLSocket) wrappedSocket;
        castSocket.startHandshake();
        Certificate[] certChain = castSocket.getSession().getPeerCertificates();
        X509Certificate[] castCertChain;
        try {
            castCertChain = Arrays.stream(certChain).map(
                    (cert) -> (X509Certificate) cert)
                    .toArray(X509Certificate[]::new);
        } catch (ClassCastException ex) {
            throw new IOException(
                    "The remote client certificate was not X.509", ex);
        }
        try {
            keyStore.checkChainTrusted(castCertChain);
        } catch (CertificateException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Does nothing. This sort of endpoint communicates entirely over the
     * network, and does not need to be tightly coupled with a communicator.
     *
     * @param communicator Ignored.
     */
    @Override
    public void informOfCommunicatorStart(DistributedCommunicator communicator) {
    }

    /**
     * Does nothing. This sort of endpoint communicates entirely over the
     * network, and does not need to be tightly coupled with a communicator.
     *
     * @param communicator Ignored.
     */
    @Override
    public void informOfCommunicatorStop(DistributedCommunicator communicator) {
    }
}
