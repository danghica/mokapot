/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.messages;

/**
 * @author Thomas Cuvillier
 * 
 * This exception is thrown if the transformation applied on a class may have not removed
 * all the limitations, or using this class with mokapot might result in behaviour different
 * than the original one.
 *
 * Note that this class extends RuntimeException so it can be thrown within overriden methods of asm. However, it must be
 * caught.
 */
public class TransformationMayFailException extends RuntimeException {
    
    public TransformationMayFailException(String nameOfTheClass, String nameOfTheMethod, String reasonForFailing, Throwable t){
        super("Transformation of the method " + nameOfTheMethod + " from Class  " + nameOfTheClass + " has lead to a class that is not safe to use with mokapot, because "
        + reasonForFailing, t);
        
    }
    
        public TransformationMayFailException(String nameOfTheClass, String reasonForFailing, Throwable t){
           super("Transformation of the class " + nameOfTheClass + " has lead to a class that is not safe to use with mokapot, because " + reasonForFailing, t);
    }
          public TransformationMayFailException(String nameOfTheClass, String reasonForFailing){
              this(nameOfTheClass, reasonForFailing, (Throwable) null);
          }
           public TransformationMayFailException(String nameOfTheClass, String nameOfTheMethod, String reasonForFailing){
              this(nameOfTheClass, nameOfTheMethod, reasonForFailing, null);
          }
          
          
          
        
        
}
