package xyz.acygn.millr.testclasses;

import xyz.acygn.millr.mokapotsemantics.IsMokapotVersion;
import xyz.acygn.millr.mokapotsemantics.PathConstant;
import xyz.acygn.millr.mokapotsemantics.StateTracker;
import xyz.acygn.millr.mokapotsemantics.TrackableSampleProgram;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URLClassLoader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class StreamsMokapot extends TrackableSampleProgram implements IsMokapotVersion {
    private DistributedCommunicator communicator;
    private CommunicationAddress remote;

    private Random localRng;
    private Random remoteRng;
    private Set<String> localSet;
    private Set<String> remoteSet;
    private Stream<String> localStream;
    private Stream<String> remoteStream;


    public StreamsMokapot(DistributedCommunicator communicator, CommunicationAddress remote) throws Exception{
        super((URLClassLoader) StreamsMokapot.class.getClassLoader(), true);
        stateTracker.register(this);
            this.communicator = communicator;
            this.remote = remote;
    }

    @Override
    public StateTracker run() throws IllegalAccessException, Exception {
        localRng = new Random(1);
        remoteRng = communicator.runRemotely(() -> new Random(1), remote);
        localSet = new TreeSet<>();
        remoteSet = communicator.runRemotely(() -> new TreeSet<>(), remote);
        stateTracker.collect();

      //  for (int i = 0; i < 1000; i++) {
            localSet.add("" + (char)remoteRng.nextInt());

                String s =  "" + (char) localRng.nextInt();
            try {
                remoteSet.add(s);
            }
            catch (java.lang.NullPointerException ex){
                if (remoteSet==null) { throw new Exception("RemoteSet is null", ex);}
                else if (localRng==null){ throw new Exception("localRng is null ", ex);}
                else{
                    throw new Exception("Seems like the string is not passed remotely: "  + s, ex );
                }
            }
       // }
        stateTracker.collect();

        localStream = remoteSet.stream();
        remoteStream = localSet.stream();
        stateTracker.collect();

     //   remoteStream.forEach(System.out::println);
      //  localStream.forEach(System.out::println);
        stateTracker.collect();

        return stateTracker;
    }
}
