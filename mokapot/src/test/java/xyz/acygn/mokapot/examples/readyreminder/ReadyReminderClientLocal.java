package xyz.acygn.mokapot.examples.readyreminder;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * A program that specifies times at which something will become ready, then
 * checks to see when those things are ready.
 * 
 * @author Alex Smith
 */
public class ReadyReminderClientLocal
{
    public static void main(String[] args) throws Exception {
        ReadyReminderServer<String> server = new ReadyReminderServer<>();

        server.submitEvent("two seconds", Instant.now().plusSeconds(2));
        server.submitEvent("one second", Instant.now().plusSeconds(1));
        server.submitEvent("three seconds", Instant.now().plusSeconds(3));
        
        System.out.println(Objects.toString(server.extractReadyEvent()));
        Thread.sleep(2500);
        System.out.println(Objects.toString(server.extractReadyEvent()));
        System.out.println(Objects.toString(server.extractReadyEvent()));
        System.out.println(Objects.toString(server.extractReadyEvent()));
    }
}
