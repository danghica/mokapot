package xyz.acygn.mokapot.test.bytecode;

import xyz.acygn.mokapot.CopiableSupplier;

/**
 * A small program that generates various sorts of lambdas.
 *
 * @author Alex Smith
 */
public class Lambdas {

    /**
     * Creates a few serializable lambdas and evaluates them.
     *
     * @param args Ignored.
     */
    public static void main(String[] args) {
        final String captured = "captured string";
        final Lambdas selfInstance = new Lambdas();
        CopiableSupplier<?> cs[] = new CopiableSupplier<?>[]{
            () -> "constant lambda",
            () -> captured,
            selfInstance::instanceMethod,
            Lambdas::staticMethod
        };

        for (CopiableSupplier<?> c : cs) {
            System.out.println(c.get().toString());
        }
    }

    /**
     * A trivial instance method.
     *
     * @return A constant string.
     */
    private String instanceMethod() {
        return "instance method result";
    }

    /**
     * A trivial static method.
     *
     * @return A constant string.
     */
    private static String staticMethod() {
        return "static method result";
    }
}
