package xyz.acygn.millr;

public class Testgetset{
    
    public int varPub;
    private int varPriv;
    public Integer IntPub;
    private Integer IntPriv;
    static public int statVar = 0;
    static public Integer statVarInt;
    

    public Testgetset() {
        varPub = 4;
        int c = varPub;
        varPriv = 5;
        int d = varPriv;
        IntPub = new Integer(4);
        Integer e = IntPub;
        statVar = statVar +1 ;
        statVarInt = new Integer(5);
    }
}
