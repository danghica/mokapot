/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.util;

import java.util.concurrent.CountDownLatch;

/**
 *
 * @author thomasc
 */
public interface hasSyncMethods {
    
    public void startLock();
    
    public void getLock(CountDownLatch signal);
    
    public void releaseLock();
    
}
