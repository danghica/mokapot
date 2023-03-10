package xyz.acygn.millr.localsemantics.getset;


public class GetterSetterSample implements GetterSetterMillableSample {
    private Data data;

    @Override
    public int getPublicStaticFinalVariable() {
        return Data.INT_CONSTANT;
    }

    @Override
    public int getPublicStaticVariable() {
        return Data.staticInt;
    }

    @Override
    public int setAndGetPublicStaticVariable() {
        Data.staticInt = 300;
        return Data.staticInt;
    }

    @Override
    public int getPublicVariable() {
        return new Data().intVar;
    }

    @Override
    public int setAndGetPublicVariable() {
        data = new Data();

        data.intVar = 100;
        return data.intVar;
    }

    @Override
    public int getThisProtectedVariable() {
        DataSubClass subdata = new DataSubClass();

        return subdata.getThisProtectedIntVar();
    }

    @Override
    public int getSuperProtectedVariable() {
        DataSubClass subdata = new DataSubClass();

        return subdata.getSuperProtectedIntVar();
    }

    @Override
    public int getOverridingVariable() {
        DataSubClass subdata = new DataSubClass();

        return subdata.getProtectedIntVar();
    }
}


/**
 * An object with a number of different types of fields, used to exercise the semantics
 * of different types of field accesses. In terms of their modifiers, the class contains
 * fields counted as follows:
 *
 * 18 public static final
 * 18 public static
 * 18 public final
 * 18 public
 *
 * 1 private static final
 * 1 private static
 * 1 private final
 * 1 private
 *
 * This totals to 72 public fields and 4 private fields. Out of the 72 public fields, 36 are
 * final. That is, if millr correctly inserts setters and getters for the right fields in such
 * a way that semantics are preserved, 72 getters and 36 setters should be inserted.
 *
 * @author Marcello De Bernardi
 */
@SuppressWarnings("WeakerAccess")
class Data {
    // public static final fields
    public static final int INT_CONSTANT;
    public static final String STRING_CONSTANT;
    // public static fields
    public static int staticInt;
    public static String staticString;
    // public final fields
    public final int finalInt;
    public final String finalString;
    // public fields
    public int intVar;
    public String stringVar;
    // protected fields to inherit
    protected int protectedIntVar;
    protected String protectedStringVar;
    // some additional private fields
    private static final int PRIVATE_INT_CONSTANT;
    private static int privateStaticInt;
    private final int privateFinalInt;
    private int privateInt;


    static {
        // public static final fields
        INT_CONSTANT = 3;
        STRING_CONSTANT = "STRING_CONSTANT";
        // public static
        staticInt = 4;
        staticString = "lol";
        // private static final
        PRIVATE_INT_CONSTANT = 42;
        privateStaticInt = 52;
    }


    Data() {
        // public final variables
        finalInt = 5;
        finalString = "finalString";
        // public variables
        intVar = 6;
        stringVar = "stringVar";
        // protected fields
        protectedIntVar = 7;
        protectedStringVar = "protectedStringVar";
        // private fields
        privateFinalInt = 5;
        privateInt = 5;
    }
}


/**
 * An extended version of {@link Data} which overrides some of the superclass's
 * protected fields. The methods in this class exercise the semantics of field
 * access when those fields are inherited, or overwrite an inherited field.
 *
 * @author Marcello De Bernardi
 */
class DataSubClass extends Data {
    // overriding variables
    private int protectedIntVar;
    private String protectedStringVar;

    DataSubClass() {
        super();

        protectedIntVar = super.protectedIntVar * 2;
        protectedStringVar = super.protectedStringVar + "overriding_version";
    }

    int getThisProtectedIntVar() {
        return this.protectedIntVar;
    }

    int getSuperProtectedIntVar() {
        return super.protectedIntVar;
    }

    int getProtectedIntVar() {
        return intVar;
    }

    String getThisProtectedStringVar() {
        return this.protectedStringVar;
    }

    String getSuperProtectedStringVar() {
        return super.protectedStringVar;
    }

    String getProtectedStringVar() {
        return protectedStringVar;
    }
}
