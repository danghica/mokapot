package xyz.acygn.millr.testclasses;

import xyz.acygn.millr.mokapotsemantics.StateTracker;
import xyz.acygn.millr.mokapotsemantics.TrackableSampleProgram;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 *
 * @author Marcello De Bernardi
 */
public class StreamsLocal extends TrackableSampleProgram {
    private Random localRng;
    private Random remoteRng;
    private Set<String> localSet;
    private Set<String> remoteSet;
    private Stream<String> localStream;
    private Stream<String> remoteStream;


    public StreamsLocal() throws Exception {
        super((URLClassLoader) StreamsLocal.class.getClassLoader(), false);
        stateTracker.register(this);
    }


    @Override
    public StateTracker run() throws IllegalAccessException, Exception {
        localRng = new Random(1);
        remoteRng = new Random(1);
        localSet = new TreeSet<>();
        remoteSet = new TreeSet<>();
        stateTracker.collect();

        for (int i = 0; i < 1000; i++) {
            localSet.add("" + (char)remoteRng.nextInt());
            remoteSet.add("" + (char)localRng.nextInt());
        }
        stateTracker.collect();

        localStream = remoteSet.stream();
        remoteStream = localSet.stream();
        stateTracker.collect();

 //       remoteStream.forEach(System.out::println);
 //       localStream.forEach(System.out::println);
        stateTracker.collect();

        return stateTracker;
    }
}
