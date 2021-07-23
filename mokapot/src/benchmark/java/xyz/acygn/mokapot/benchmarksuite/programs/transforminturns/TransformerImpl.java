package xyz.acygn.mokapot.benchmarksuite.programs.transforminturns;

import java.rmi.RemoteException;

/**
 * @author Kelsey McKenna
 */
public class TransformerImpl implements Transformer {

    private Transformer pair;

    @Override
    public void pair(Transformer transformerPair) throws RemoteException {
        pair = transformerPair;
    }

    @Override
    public void unpair() throws RemoteException {
        this.pair = null;
    }

    @Override
    public String transform(String target, int times) throws RemoteException {
        if (times == 0) {
            return target;
        }

        String intermediateResult = new StringBuilder(target).reverse().toString();
        return pair.transform(intermediateResult, times - 1);
    }

    @Override
    public Transformer getPartner() throws RemoteException {
        return pair;
    }

    @Override
    public boolean paired() {
        return pair != null;
    }

}
