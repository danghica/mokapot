package xyz.acygn.millr.mokapotsemantics;

public interface PathConstant {

    /**
     * Where the test will be compiled.
     */
    public final static String OUTPUTMILLRTEST="build-internal/test/millr";

    /**
     * Where the tests classes on which the tests are going to ran will be located.
     */
    public final static String OUTPUTMILLRTESTCLASSES = OUTPUTMILLRTEST + "/testclasses";

    /**
     * Where the runner classes of the tests will be located.
     */
    public final static String OUTPUTMILLRTESTMAIN= OUTPUTMILLRTEST +"/testMain";

    /**
     * Where the milled versions of the test classes will be located.
     */
    public final static String OUTPUTMILLEDCLASSES= "build-internal/out/test/test-millr/";

    /**
     * The classes for mokapot.
     */
    public final static String MOKAPOTCLASSES = "build-output/classes-mokapot";

    /**
     * The classes for Millr.
     */
    public final static String MILLRCLASSES="build-output/classes-millr";

    /**
     * The common classes.
     */
    public final static String COMMONCLASSES="build-output/classes-common";
}
