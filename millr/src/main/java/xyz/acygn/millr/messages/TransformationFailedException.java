package xyz.acygn.millr.messages;

import org.objectweb.asm.ClassReader;


/**
 * Signals that a specific millr transformation has failed.
 *
 * @author Marcello De Bernardi, Thomas Cuvillier
 */
public class TransformationFailedException extends RuntimeException {

    public TransformationFailedException(Throwable cause, String message, String nameOfTheTransformation, String className){
        super( (className==null)? "Transformation " + nameOfTheTransformation  + " of a classReader failed, the classReader is null. Message thrown is " + message :
                "Transformation " + nameOfTheTransformation + " of ClassReader " + className + " failed due to " + message, cause );
    }

    public TransformationFailedException(Throwable cause, String message, String nameOfTheTransformation, ClassReader cr) {
           this(cause, message, nameOfTheTransformation, (cr==null)? null : cr.getClassName());
        
    }
    
    
}
