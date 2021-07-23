package xyz.acygn.mokapot.examples.readyreminder;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import static xyz.acygn.mokapot.examples.readyreminder.XWWWFormUrlencodedHTTPHandler.*;

/**
 * An HTTP server that acts like <code>ReadyReminderServer</code> does.
 *
 * @author Alex Smith
 */
public class ReadyReminderServerHTTP {

    /* How to submit events using curl:
    
       curl -w '%{response_code}\n' --data-urlencode "event=two seconds" --data-urlencode "readyAt=`date -Iseconds -d '2 seconds'`" localhost:15230/ReadyReminder
     */
    public static void main(String[] args) throws Exception {
        ReadyReminderServer<String> list = new ReadyReminderServer<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 15230), 4);

        server.createContext("/ReadyReminder", (XWWWFormUrlencodedHTTPHandler) (params, method) -> {
            if (!method.equals("POST")) {
                return stringResponse("request method " + method + " is inapplicable\n", 405);
            }

            list.submitEvent(params.parameter("event"),
                    params.decodeParameter("readyAt", Instant::parse,
                            DateTimeParseException.class));

            return voidResponse();
        });
        server.start();
    }
}
