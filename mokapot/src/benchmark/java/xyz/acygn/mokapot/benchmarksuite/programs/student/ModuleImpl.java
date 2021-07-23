package xyz.acygn.mokapot.benchmarksuite.programs.student;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Class to represent a university module, with an associated name and id.
 * @author Alexandra Paduraru
 *
 */
public class ModuleImpl implements Remote, Module {

    private final String name;
    private final int id;

    public ModuleImpl(String name, int id) {
        this.name = name;
		this.id = id;
	}

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Module)) {
            return false;
        }

        Module module = (Module) o;

        try {
            return id == module.getId()
                    && (name != null ? name.equals(module.getName()) : module.getName() == null);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

}
