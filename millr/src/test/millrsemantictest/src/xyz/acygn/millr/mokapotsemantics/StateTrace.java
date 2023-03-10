package xyz.acygn.millr.mokapotsemantics;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Predicate;

/**
 * A state trace is a representation of value of fields of objects at given times.
 * A good implementation of this class must provide two core functionalities:
 * - A useful toString representation.
 * - A useful comparison method, that highlights where two fields differ.
 */

public class StateTrace {

    private List<Snapshot> listSnapshots;
    private Snapshot currentSnapshot;

    /**
     * True if some of the explored objects are remote. In which case we will not try to access fields directly.
     */
    private boolean isRemote;

    public StateTrace(boolean isRemote) {
        this.isRemote = isRemote;
        listSnapshots = new ArrayList<>();
    }

    public void createNewShapShot() {
        currentSnapshot = new Snapshot(isRemote);
        listSnapshots.add(currentSnapshot);
    }

    public boolean compareToStateTrace(StateTrace otherStateTrace) {
        if (otherStateTrace.listSnapshots.size() != this.listSnapshots.size()) {
            return false;
        }
        for (int i = 0; i < this.listSnapshots.size(); i++) {
            if (!listSnapshots.get(i).compareTo(otherStateTrace.listSnapshots.get(i))) {
                return false;
            }
        }
        return true;
    }

    public void register(Object o) {
        try {
            currentSnapshot.registerObject(o);
        }
        catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void registerAll(Object o) {
        try {
            currentSnapshot.registerAllObject(o);
        }
        catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public List<Snapshot> getListSnapShot(){
        return listSnapshots;
    }


    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        int i = 0;
        for (Snapshot snap : listSnapshots){
            str.append("Snapshot ").append(String.valueOf(i)).append(" : ").append(snap.toString()).append(" \n");
            i++;
        }
        return str.toString();
    }



}
