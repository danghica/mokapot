package xyz.acygn.millr.mokapotsemantics;
import java.io.IOException;
import java.net.URLClassLoader;

/**
 * A {@code TrackableSampleProgram} is a sample program whose state can be tracked
 * using an instance of a {@link StateTracker}. Any extending class must provide
 * an implementation of the {@code run()} method, which represents executing a sample
 * program in such a way that its state is tracked using the {@link StateTracker}.
 *
 * @author Marcello De Bernardi
 */
abstract public class TrackableSampleProgram {
    protected StateTracker stateTracker;


    /**
     * Constructor. Initializes the state tracking mechanism for tracking sample programs.
     *
     * @throws IOException           in a number of cases related to mokapot
     * @throws NoSuchFieldException  when registering a field with tracker, if field not found
     * @throws NoSuchMethodException when registering a method with tracker, if method not found
     */
    public TrackableSampleProgram(boolean isRemote) throws NoSuchMethodException, ClassNotFoundException {
        stateTracker = new StateTracker((URLClassLoader) this.getClass().getClassLoader(), isRemote);
        stateTracker.register(this);
    }

    /**
     * Constructor. Initializes the state tracking mechanism for tracking sample programs.
     *
     * @throws IOException           in a number of cases related to mokapot
     * @throws NoSuchFieldException  when registering a field with tracker, if field not found
     * @throws NoSuchMethodException when registering a method with tracker, if method not found
     */
    public TrackableSampleProgram(URLClassLoader urlClassLoader, boolean isRemote) throws IOException, NoSuchFieldException, NoSuchMethodException, ClassNotFoundException {
        stateTracker = new StateTracker(urlClassLoader, isRemote);
    }


    public String getStateTrackerStatus(){
        return getStateTracker().getStateToString();
    }


    public StateTracker getStateTracker() {
        return stateTracker;
    }



    /**
     * Run some sample code and return a {@link StateTracker} representing the state
     * of the program during execution.
     *
     * @return {@link StateTracker} representing state during execution
     * @throws IllegalAccessException if state tracker fails to access some field/method
     */
    public abstract StateTracker run() throws Exception;

    @Override
    public boolean equals(Object o){
        if (o==null){
            return false;
        }
        if (!(o instanceof TrackableSampleProgram)){
            return false;
        }
        if (((TrackableSampleProgram) o).getStateTrackerStatus().equals(this.getStateTrackerStatus())){
            return true;
        }
        else{
            System.err.println(
                    "Different ! . Object one status \n " + ((TrackableSampleProgram) o).getStateTrackerStatus()
                            + "\n Object Two Status " + ((TrackableSampleProgram) o).getStateTrackerStatus());
            return false;
        }
    }
}
