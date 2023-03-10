package xyz.acygn.millr;



/**
 * The reason why a class should be unsafe to create remotely / migrate.
 *
 * The two important methods of this class are the creator, and the toString.
 * Should be subclassed to take into account the variety of reasons.
 */
public class Reason {


    String message;

    String getReason(){
        return message;
    }


    static class methodPackagePrivateOutsideProject extends Reason{

        public methodPackagePrivateOutsideProject(String owner, String nameMethod, String methodDesc, String ownerOfCode){
            this.message = "The class " + owner + " is not-copiable and " +
                    "inherits the method " + nameMethod + ", whose description is " + methodDesc
                    + " from the class " + ownerOfCode + ", and this method is package-private";
        }
    }


    static class methodAccessApiField extends Reason{


        public methodAccessApiField(MethodParameter methodCalling , FieldParameter field){
            this.message = " The class " + methodCalling.className + " has a method; " + "\n"
                    + "methodName : " + methodCalling.methodName + "\n"
                    + "methodDesc :  " + methodCalling.methodAccess + " \n"
                    + " that tries to access an API field \n "
                    + "Class the field belongs to " + field.cp.className
                    + "Name of the field: " + field.name
                    + "Desc of the field: " + field.desc;

        }

    }

}
