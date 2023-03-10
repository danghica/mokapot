package xyz.acygn.millr.testclasses;
import xyz.acygn.millr.mokapotsemantics.IsMokapotVersion;
import xyz.acygn.millr.mokapotsemantics.StateTracker;
import xyz.acygn.millr.mokapotsemantics.TrackableSampleProgram;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.DistributedCommunicator;

import java.io.IOException;
import java.net.URLClassLoader;

public class DirectAccessMokapot extends TrackableSampleProgram implements IsMokapotVersion
{
    private DistributedCommunicator communicator;
    private CommunicationAddress remote;

    private Rectangle localRectangle;
    private Rectangle remoteRectangle;
    private boolean equal;


    public DirectAccessMokapot(DistributedCommunicator communicator, CommunicationAddress remote) throws IOException, NoSuchFieldException, NoSuchMethodException, ClassNotFoundException {
//        System.out.println("Direct access mokapot creation started" + this.getClass());
//        // setup state tracking
        super((URLClassLoader) DirectAccessMokapot.class.getClassLoader(), true);
        stateTracker.register(this);

        // setup mokapot
        this.communicator = communicator;
        this.remote = remote;
  /*      System.out.println("Direct access mokapot created"); */

    }

    @Override
    public StateTracker run() throws Exception {
         System.out.println("run start");
        // instantiation
         localRectangle = new Rectangle(10.0, 20.0);
        remoteRectangle = communicator.runRemotely(() -> new Rectangle(2.0, 10.0), remote);
        equal = localRectangle.equals(remoteRectangle);
        stateTracker.collectAll();

        // simple setters
        localRectangle.setSideB(12.0);
        remoteRectangle.setSideB(4.0);
        equal = localRectangle.equals(remoteRectangle);
        stateTracker.collectAll();
        // direct access to fields

        localRectangle.sideB = 30.0;
        remoteRectangle.sideB = 25.0;
        equal = localRectangle.equals(remoteRectangle);
        stateTracker.collectAll();

        // passing objects to each other to alter state
        localRectangle.scaleToMatchA(remoteRectangle);
        remoteRectangle.scaleToMatchB(localRectangle);
        equal = localRectangle.equals(remoteRectangle);
        stateTracker.collectAll();

        // passing again, opposite direction
        localRectangle.scaleToMatchB(remoteRectangle);
        remoteRectangle.scaleToMatchA(localRectangle);
        equal = localRectangle.equals(remoteRectangle);
        stateTracker.collectAll();

        return stateTracker;
    }
}
