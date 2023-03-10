/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import xyz.acygn.millr.messages.MessageUtil;

/**
 * Utility methods for handling external processes such as retrolambda.
 *
 * @author thomasc
 */
public class ProcessHandler {
    /**
     * A safe way to start a process, that ensures that it will proceed as
     * expected without being blocked by the JVM. The output of the process will
     * be redirected to system.out.println(), as well as the output of the
     * exception if one is thrown by the process.
     *
     * @param p        A process
     * @param timeOut  The amount of unit of time we shall wait before the
     *                 process finishes.
     * @param timeUnit The unit of time.( second, millisecond...)
     * @param 
     * @return True if the process returned without throwing an exception, false otherwise.
     */
    public static boolean startProcess(Process p, long timeOut, TimeUnit timeUnit) {
        StreamGobbler[] streams = exhaustStreams(p);
        try {
            if (!p.waitFor(timeOut, timeUnit))
                for (StreamGobbler stream : streams)
                    MessageUtil.message(stream.getResultOfStream());
        }
        catch (InterruptedException ie) {
            MessageUtil.error(ie).report().resume();
            for (StreamGobbler stream : streams) {
                MessageUtil.message(stream.getResultOfStream(), true).emit();
            }
            return false;
        }
        return true;
    }

    /**
     * Redirect the output of a process to the logger with level fine.
     *
     * @param p The process.
     */
    static void outputToLogger(Process p) {
        outputToLogger(p, Level.FINE);
    }

    /**
     * Redirect the output of a process to MessaqeUtil.
     *
     * @param p The process
     * @param l The level the messages should be recorded with.
     */
    static void outputToLogger(Process p, Level l) {
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            while ((line = reader.readLine()) != null) {
                MessageUtil.message(line, l);
            }
        }
        catch (IOException ex) {
            MessageUtil.error(ex).report().resume();
        }
    }


    private static StreamGobbler[] exhaustStreams(Process p) {
        InputStream stderr = p.getErrorStream();
        InputStream stdinn = p.getInputStream();

        StreamGobbler sgInn = new StreamGobbler(stdinn, "inn");
        StreamGobbler sgErr = new StreamGobbler(stderr, "err");

        sgInn.start();
        sgErr.start();

        return new StreamGobbler[]{sgInn, sgErr};
    }


    /**
     * A StreamGobbler is a {@link Thread} which, when started, reads every line
     * buffered in an {@link InputStream} and returns a {@link String} representing
     * the content of the stream.
     */
    static class StreamGobbler extends Thread {
        private InputStream is;
        private String type;
        private StringBuilder resultOfStream;

        StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
            resultOfStream = new StringBuilder();
        }

        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            try {
                String line;
                while ((line = reader.readLine()) != null)
                    resultOfStream.append(line).append("\n");
            }
            catch (IOException ex) {
                MessageUtil.error(ex).report().resume();
            }
        }

        String getResultOfStream() {
            return resultOfStream.toString();
        }

    }
}
