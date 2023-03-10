package xyz.acygn.millr.testclasses;

import xyz.acygn.millr.mokapotsemantics.TrackableSampleProgram;
import xyz.acygn.millr.mokapotsemantics.StateTracker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;

public class DirectAccessLocal extends TrackableSampleProgram {
    private Rectangle localRectangle;
    private Rectangle remoteRectangle;
    private boolean equal;


    public DirectAccessLocal() throws IOException, NoSuchFieldException, NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        super((URLClassLoader) DirectAccessLocal.class.getClassLoader(), false);
        System.out.println(this.getClass().getName());
        stateTracker.register(this);
    }


    @Override
    public StateTracker run() throws Exception {
        // instantiation
        localRectangle = new Rectangle(10.0, 20.0);
        remoteRectangle = new Rectangle(2.0, 10.0);
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
