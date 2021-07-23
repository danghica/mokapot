import java.io.IOException;
import java.net.InetAddress;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.TCPCommunicationAddress;

/**
 * @author Kelsey McKenna
 */
public class PalindromeDemo {

    public static void main(String[] args) throws IOException {
        DistributedCommunicator communicator
                = new DistributedCommunicator(
                TCPCommunicationAddress.fromInetAddress(InetAddress.getLoopbackAddress(), 15238));

        communicator.startCommunication();

        CommunicationAddress remoteAddress
                = TCPCommunicationAddress.fromInetAddress(InetAddress.getLoopbackAddress(), 15239);

        final PalindromeVerifier palindromeVerifier = DistributedCommunicator.getCommunicator()
                .runRemotely(PalindromeVerifier::new, remoteAddress);

        System.out.println(palindromeVerifier.verify("racecar"));

        DistributedCommunicator.getCommunicator().stopCommunication();
    }

}
