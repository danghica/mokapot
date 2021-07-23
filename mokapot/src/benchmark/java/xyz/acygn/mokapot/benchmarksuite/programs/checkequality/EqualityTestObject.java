package xyz.acygn.mokapot.benchmarksuite.programs.checkequality;

import java.rmi.RemoteException;

/**
 * @author Kelsey McKenna
 */
public class EqualityTestObject implements ValueHolder {

    private final int value;

    public EqualityTestObject(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        /*
        obj may be another EqualityTestObject, or it could
        be a proxy, i.e. a remote object, in which case instanceof
        checks against EqualityTestObject will fail. However,
        obj instanceof ValueHolder will still pass, and since
        the only comparisons we need to make are against the 'value'
        field, we can cast obj to a ValueHolder instead. Every
        EqualityTestObject object implements ValueHolder, so we only
        need to try to cast to ValueHolder.
         */

        try {
            return obj instanceof ValueHolder
                    && ((ValueHolder) obj).getValue() == this.getValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
