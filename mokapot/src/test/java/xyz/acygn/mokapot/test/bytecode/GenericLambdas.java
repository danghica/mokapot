package xyz.acygn.mokapot.test.bytecode;

import java.io.Serializable;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

/**
 * Uses a single lambda with more than one type.
 *
 * @author Alex Smith
 */
public class GenericLambdas {

    /**
     * Runs the same "lambda" (actually a method reference) with three different
     * types. This includes both generic and specialised types.
     * <p>
     * There's also a fourth lambda here, with different code. This is basically
     * just to make the decompiled output easier to read in certain decompilers
     * (as it'll split it up into multiple statements rather than trying to fit
     * everything into one massive if statement).
     *
     * @param args Ignored.
     */
    public static void main(String[] args) {
        IntUnaryOperator f0
                = (IntUnaryOperator & Serializable) (x) -> x;
        IntUnaryOperator f1
                = (IntUnaryOperator & Serializable) GenericLambdas::identity;
        UnaryOperator<Integer> f2
                = (UnaryOperator<Integer> & Serializable) GenericLambdas::identity;
        UnaryOperator<String> f3
                = (UnaryOperator<String> & Serializable) GenericLambdas::identity;
        System.out.println(f3.apply("x" + f2.apply(f1.applyAsInt(
                f0.applyAsInt(6)))));
    }

    /**
     * The identity function, written in a generic way.
     *
     * @param <T> The type of the argument.
     * @param t The value to return.
     * @return <code>t</code>.
     */
    private static <T> T identity(T t) {
        return t;
    }
}
