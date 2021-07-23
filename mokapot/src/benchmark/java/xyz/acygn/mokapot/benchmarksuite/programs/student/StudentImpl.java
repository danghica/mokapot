package xyz.acygn.mokapot.benchmarksuite.programs.student;

import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;
import xyz.acygn.mokapot.markers.NonCopiable;

/**
 * Class to represent a student with an associated id, name and a list of
 * modules the student is enrolled in.
 * 
 * @author Alexandra Paduraru
 *
 */
public class StudentImpl implements Remote, NonCopiable, Student {

    private List<Module> modules = new ArrayList<>();

    @Override
    public void addModule(Module newModule) {
        assert newModule != null;

        modules.add(newModule);
    }

    @Override
    public boolean hasModule(Module m) {
        assert m != null;

        for (Module module : modules) {
            if (m.equals(module)) {
                return true;
            }
        }

        return false;
    }

}
