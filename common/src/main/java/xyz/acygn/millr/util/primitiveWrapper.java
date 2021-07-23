/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.util;

/**
 *
 * @author thomasc
 */
public final class primitiveWrapper {
    
    interface isPrimitiveWrapper{  }
    
    
    final static class booleanWrapper implements isPrimitiveWrapper{
        public final boolean internal;
        public booleanWrapper(final boolean z){
            internal = z;
        }
    }
    
    final static class byteWrapper implements isPrimitiveWrapper{
        public final byte internal;
        public byteWrapper(final byte b){
            internal = b;
        }
    }
    
    final static class charWrapper implements isPrimitiveWrapper{
        public final char internal;
        public charWrapper(final char c){
            internal = c;
        }  
    }
    
    final static class doubleWrapper implements isPrimitiveWrapper{
        public final double internal;
        public doubleWrapper(final double d){
            internal = d;
        }
                
    }
    
    public final static class floatWrapper implements isPrimitiveWrapper{
        public final float internal;
        public floatWrapper(final float f) {
            internal = f;
        }
        
    }
    
    final static class intWrapper implements isPrimitiveWrapper{
        public final int internal;
        public intWrapper(final int i){
            internal = i;
        }
    }
    
    final static class longWrapper implements isPrimitiveWrapper{
        public final long internal;
        public longWrapper( final long l) {
            internal = l;
        }
    }
    
    final static class shortWrapper implements isPrimitiveWrapper{
        public final short internal;
        public shortWrapper(final short s){
            internal = s;
        }
        
    }
}
