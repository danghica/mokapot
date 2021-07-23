import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;
import xyz.acygn.mokapot.TCPCommunicationAddress;

/**
 * @author Kelsey McKenna
 */
public class Demo {

    public static void main(String[] args) throws IOException {
        // Start a communicator on this JVM listening on port 15238.
        DistributedCommunicator communicator
                = new DistributedCommunicator(
                        TCPCommunicationAddress.fromInetAddress(InetAddress.getLoopbackAddress(), 15238));

        communicator.startCommunication();

        // Configure the address of the remote communicator.
        CommunicationAddress remoteAddress
                = TCPCommunicationAddress.fromInetAddress(InetAddress.getLoopbackAddress(), 15239);

        // Create a list on the remote machine.
        List<String> remoteList = DistributedCommunicator.getCommunicator()
                .runRemotely(() -> new ArrayList<>(), remoteAddress);

        // Add an element to the remote list.
        remoteList.add("Some string");

        /*
        Compute the size of the list. The computation will run on the remote machine.
        Since remoteList is a long reference, method invocations will automatically
        be directed to the machine holding the actual object.
         */
        System.out.println(remoteList.size());

        DistributedCommunicator.getCommunicator().stopCommunication();
    }

}
