import java.io.IOException;
import java.net.InetAddress;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.NonCopiable;
import xyz.acygn.mokapot.TCPCommunicationAddress;

/**
 * @author Kelsey McKenna
 */
public class DestroyRemoteReferencesDemo {

    private static class NonCopiableType implements NonCopiable {

    }

    public static void main(String[] args) throws IOException {
        DistributedCommunicator communicator
                = new DistributedCommunicator(
                TCPCommunicationAddress.fromInetAddress(InetAddress.getLoopbackAddress(), 15238));

        communicator.startCommunication();

        CommunicationAddress remoteAddress
                = TCPCommunicationAddress.fromInetAddress(InetAddress.getLoopbackAddress(), 15239);

        NonCopiableType remoteObject = DistributedCommunicator.getCommunicator()
                .runRemotely(NonCopiableType::new, remoteAddress);

        remoteObject = null;

        DistributedCommunicator.getCommunicator().stopCommunication();

        System.out.println("Finished");
    }

}
