package xyz.acygn.mokapot.test.util;

import xyz.acygn.mokapot.util.Expirable;

public class TrivialExpirable implements Expirable {

    public boolean expired = false;

    @Override
    public synchronized void expire() throws ExpiredException {
        if (expired) {
            throw ExpiredException.SINGLETON;
        }
        expired = true;
    }
}
