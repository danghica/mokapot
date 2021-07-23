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
public class LatchThread extends Thread{
    
    hasSyncMethods o;
    CountDownLatch signal;
    
    public LatchThread(hasSyncMethods o, CountDownLatch signal){
        this.o = o;
        this.signal = signal;
    }
    
    public void run(){
        o.getLock(signal);
    }
    
}
