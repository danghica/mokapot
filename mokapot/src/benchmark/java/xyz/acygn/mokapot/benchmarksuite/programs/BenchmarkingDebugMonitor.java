package xyz.acygn.mokapot.benchmarksuite.programs;

import xyz.acygn.mokapot.DebugMonitor;

public class BenchmarkingDebugMonitor implements DebugMonitor {

    @Override
    public void newMessage(MessageInfo mi) {
        System.out.println("Message: " + mi);
    }

    @Override
    public void warning(String arg0) {
        System.out.println("Warning: " + arg0);
    }

}
