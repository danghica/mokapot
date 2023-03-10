package xyz.acygn.millr.millingfailures;


/**
 * Minimum reproducible example of millr's failure to handle a nested
 * array.
 *
 * @author Marcello De Bernardi
 */
public class NestedArrayComesFromClassFailureExample {
    private int number[];


    public NestedArrayComesFromClassFailureExample() {
    }


    public void method() {
        number = new int[5];
    }
}

class NestedArrayComesFromClassFailureExample2 {
    private int number[];


    public NestedArrayComesFromClassFailureExample2() {
    }

    public void method() {
        number = new int[5];
    }
}
