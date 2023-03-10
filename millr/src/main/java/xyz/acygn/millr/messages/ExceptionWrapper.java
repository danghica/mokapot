/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.messages;

/**
 *
 * @author thomasc
 */
public class ExceptionWrapper extends RuntimeException{
    
    Exception e;
    
    public ExceptionWrapper(Exception e){
        super(e);
        this.e = e;
    }

    public Exception getOriginalException(){
        return e;
    }
    
}
