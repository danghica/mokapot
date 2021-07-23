package xyz.acygn.mokapot.test.bytecode;

import java.lang.reflect.Field;
import org.objenesis.ObjenesisStd;

/**
 * A class used for experiments with extending inner classes. In particular,
 * it's possible for this class to extend a private inner class and nonetheless
 * be constructable via reflection.
 *
 * @author Alex Smith
 */
public class DerivedFromInnerClass extends InnerBaseClass.Inner {

    /**
     * Creates an instance of this class, and attempts to call some methods on
     * the inner and outer objects.
     *
     * @param args Ignored.
     * @throws java.lang.NoSuchFieldException If the <code>this$0</code> field
     * of the instance cannot be found
     * @throws java.lang.IllegalAccessException If the <code>this$0</code> field
     * of the instance cannot be set
     */
    public static void main(String[] args)
            throws NoSuchFieldException, IllegalAccessException {
        InnerBaseClass ibc = new InnerBaseClass();
        System.out.println("base object hash code: " + ibc.hashCode());

        /* We can't construct a DerivedFromInnerClass the normal way, but we
           can construct it without the constructor and then set this$0
           manually. */
        DerivedFromInnerClass dfic
                = new ObjenesisStd().newInstance(DerivedFromInnerClass.class);
        Field f = InnerBaseClass.Inner.class.getDeclaredField("this$0");
        f.setAccessible(true);
        f.set(dfic, ibc);

        dfic.printHashCodeReport();
    }
}
