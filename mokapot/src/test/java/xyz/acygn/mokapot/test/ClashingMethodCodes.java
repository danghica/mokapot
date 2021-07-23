package xyz.acygn.mokapot.test;

/**
 * A class whose methods are named in order to produce hash collisions in the
 * method codes code. It doesn't actually do anything useful (apart from
 * verifying that the correct method was called, and allowing indirect calls to
 * other objects cross-system).
 *
 * @author Alex Smith
 */
public class ClashingMethodCodes {

    /**
     * A reference to another <code>ClashingMethodCodes</code> object. When a
     * method of another object is called, this is the object it's called on.
     * (The more usual way to provide an argument, via a method parameter,
     * wouldn't work in this case as it would affect the method signature, and
     * thus it would hash differently.) Can be <code>null</code>.
     */
    private ClashingMethodCodes ref;

    /**
     * Sets the object that methods will be indirectly called on.
     *
     * @param ref When a method is indirectly called via <code>this</code>, it
     * will be called on <code>ref</code>.
     */
    public void setRef(ClashingMethodCodes ref) {
        this.ref = ref;
    }

    /**
     * Returns the object on which methods are being indirectly called. The main
     * purpose of this is to allow a cyclic structure of
     * <code>ClashingMethodCodes</code> objects to be broken with a reference to
     * only one element of the structure.
     *
     * @return The object on which methods are being indirectly called.
     */
    public ClashingMethodCodes getRef() {
        return ref;
    }

    /**
     * Always returns 1.
     * <p>
     * This method's method code has the same bottom 32 bits as that of
     * <code>mb0fe88f2</code>, when using the default method code salt 1 (which
     * is not normally used with this class).
     *
     * @return 1.
     */
    public int me63b13ce() {
        return 1;
    }

    /**
     * Returns <code>ref.me63b13ce() + 2</code>.
     *
     * @return <code>ref.me63b13ce() + 2</code>.
     */
    public int mb0fe88f2() {
        return ref.me63b13ce() + 2;
    }

    /**
     * Returns <code>ref.mb0fe88f2() + 4</code>.
     * <p>
     * This method's method code has the same bottom 32 bits as that of
     * <code>mf10e6a48</code>, when using the alternative method code salt 1
     * (which is the salt typically used with this class).
     *
     * @return <code>ref.mb0fe88f2() + 4</code>.
     */
    public int m7975bcd8() {
        return ref.mb0fe88f2() + 4;
    }

    /**
     * Returns <code>ref.m7975bcd8() + 8</code>.
     *
     * @return <code>ref.m7975bcd8() + 8</code>.
     */
    public int mf10e6a48() {
        return ref.m7975bcd8() + 8;
    }

    /**
     * Returns <code>ref.mf10e6a48() + 16</code>.
     * <p>
     * This method's method code, when using the default method code salt 1,
     * matches that of <code>meb69d9e302b46574</code> in all 64 bits. This
     * causes the method code salting code to choose an alternative salt (as two
     * methods of the same class can't have the same code, the salt has to be
     * chosen to make all the methods of each class distinctive).
     *
     * @return <code>ref.mf10e6a48() + 16</code>.
     */
    public int mca97d4bfd0460349() {
        return ref.mf10e6a48() + 16;
    }

    /**
     * Returns <code>ref.mca97d4bfd0460349() + 32</code>.
     *
     * @return <code>ref.mca97d4bfd0460349() + 32</code>.
     */
    public int meb69d9e302b46574() {
        return ref.mca97d4bfd0460349() + 32;
    }
}
