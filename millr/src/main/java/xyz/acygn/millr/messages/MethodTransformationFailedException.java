package xyz.acygn.millr.messages;

public class MethodTransformationFailedException extends RuntimeException {

    public MethodTransformationFailedException(Throwable t, String className, String methodName, String methodDesc){
        super("Method Transformation failed for the method \n" +
                "method name: " + methodName + " \n" +
                "method description " + methodDesc + "\n" +
                "belonging to class " + className + "\n" +
                "message : " + t.getMessage(), t);
    }
}
