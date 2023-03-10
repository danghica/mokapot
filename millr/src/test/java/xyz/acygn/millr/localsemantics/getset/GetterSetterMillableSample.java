package xyz.acygn.millr.localsemantics.getset;

/**
 * Defines a set of public methods, where each method represent a sequence of
 * actions that may be performed on an object's variables, and whose semantics
 * should be preserved by milling.
 *
 * @author Marcello De Bernardi
 */
public interface GetterSetterMillableSample {
    /**
     * Exercises the semantics of accessing a public static final field in
     * a class (a class constant).
     *
     * @return the result of getting a constant belonging to a class
     */
    int getPublicStaticFinalVariable();

    /**
     * Exercises the semantics of accessing a public static field in a class
     * (a class variable).
     * @return the result of getting a class variable
     */
    int getPublicStaticVariable();

    /**
     * Exercises the semantics of both updating and then accessing a public
     * static field in a class (a class variable).
     *
     * @return the result of first setting and then getting a class variable
     */
    int setAndGetPublicStaticVariable();

    /**
     * Exercises the semantics of accessing a public field in a class instance.
     *
     * @return the result of getting an instance variable
     */
    int getPublicVariable();

    /**
     * Exercises the semantics of both updating and then accessing a public field
     * from a class instance.
     *
     * @return the result of setting and then getting an instance variable
     */
    int setAndGetPublicVariable();

    /**
     * Exercises the semantics of getting the value of an overriding variable, where
     * dispatch is made explicit by use of the keyword "this".
     *
     * @return the result of getting the value of an overriding variable
     */
    int getThisProtectedVariable();

    /**
     * Exercises the semantics of getting the value of an overridden variable by
     * means of dispatching using the keyword "super".
     *
     * @return the result of getting the value of an overridden variable
     */
    int getSuperProtectedVariable();

    /**
     * Exercises the semantics of getting the value of an overriding variable where
     * the keyword "this" is not used; that is, simple name hiding is mechanism for
     * dispatch.
     *
     * @return the result of getting the value of an overriding variable
     */
    int getOverridingVariable();

    // todo class variable through instance
    // todo visibility
    // todo race conditions?
}
