package xyz.acygn.mokapot.examples.readyreminder;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;

/**
 * A program that specifies times at which something will become ready, then
 * checks to see when those things are ready. The logic is not stored locally,
 * but rather on a Mokapot server.
 * <p>
 * As this is just a toy example, credentials are hardcoded
 * (<code>test2.p12</code>, password <code>testpassword1</code>).
 *
 * @author Alex Smith
 */
public class ReadyReminderClientMokapot {
    public static void main(String[] args) throws Exception {
        DistributedCommunicator communicator =
                new DistributedCommunicator("test2.p12",
                "testpassword1".toCharArray());
        communicator.startCommunication();
        
        CommunicationAddress serverAddress =
                communicator.lookupAddress(InetAddress.getLoopbackAddress(), 15238);
        
        ReadyReminderServer<String> server =
                communicator.runRemotely(ReadyReminderServer<String>::new, serverAddress);
        
        server.submitEvent("two seconds", Instant.now().plusSeconds(2));
        server.submitEvent("one second", Instant.now().plusSeconds(1));
        server.submitEvent("three seconds", Instant.now().plusSeconds(3));
        
        System.out.println(Objects.toString(server.extractReadyEvent()));
        Thread.sleep(2500);
        System.out.println(Objects.toString(server.extractReadyEvent()));
        System.out.println(Objects.toString(server.extractReadyEvent()));
        System.out.println(Objects.toString(server.extractReadyEvent()));
        
        server = null;
        communicator.stopCommunication();
    }
}
