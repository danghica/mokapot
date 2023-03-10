package xyz.acygn.millr;

//THis class tests millrASM against simple examples using primitive data types,
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;


public class TestArray {

   private static class TestObjectSecond {

    public int a;

    public TestObjectSecond(){
    }

    public void testFnArray(Object[] listObject){
        int a;
        a = listObject.length;
        if (a != 0){
            Object ob = listObject[0];
        }
    }

    public int[][][] testFntwo(int[][][] multiArray){
        return multiArray;
    }

    public int testFn(){
        return 4;
    }

}
    
    static int field;
    public static void main(String[] args) throws ClassNotFoundException {

        Class clazzTwo = int[].class;
        Object oweewwde = new int[10];
        System.out.println(oweewwde.getClass());
        clazzTwo.cast(oweewwde);
//        System.out.println(oweewwde.toString());
//        System.out.println(oweewwde);
//
        int[] arrayFor = new int[10];
        for (int index : arrayFor) {
            System.out.println(index);
        }

        boolean[] listbool = new boolean[10];
        listbool[3] = true;
        boolean bool = listbool[5];
        System.out.println(listbool[4]);
        System.out.println(listbool.getClass().getName());

        Object o = new int[10];
        int[] o2 = (int[]) o;
        int[].class.cast(o);
        System.out.println(o.getClass());

        byte[] listbyte = new byte[30];
        listbyte[3] = (byte) 25;
        byte by = listbyte[5];

        char[] listchar = new char[30];
        listchar[3] = 'e';
        char c = listchar[5];

        String[] listString = new String[10];

        double[] listdouble = new double[30];
        listdouble[3] = (double) 654654;
        double d = listdouble[5];

        float[] listfloat = new float[30];
        listfloat[3] = (float) 654654.6545;
        float f = listfloat[5];

        int[] listint = new int[20];
        listint[2] = 3;
        int b = listint[4];

        long[] listlong = new long[15];
        listlong[4] = (long) 646465584;
        long l = listlong[3];

        short[] listshort = new short[561];
        listshort[5] = (short) 5864;
        short s = listshort[5];

        Integer[] listInteger = new Integer[43];
        listInteger[4] = new Integer(8);
        Integer I = listInteger[4];
//
//
        System.out.println("a");
        TestObjectSecond[] list = new TestObjectSecond[10];
        list[2] = new TestObjectSecond();
System.out.println("b");
        //       int a = t.testFn();
        list[2].testFnArray(list);
System.out.println("c");
        int[][] testM = new int[5][];
        System.out.println(testM.getClass().toString());
        int[][][] testMulti = new int[1][5][4];
        System.out.println(testMulti.getClass().toString());
        int[][][] testMutlitwo = list[2].testFntwo(testMulti);
   System.out.println(testMutlitwo.getClass().toString());
   System.out.println(testMulti == testMutlitwo);


       list[2] = new TestObjectSecond();
       TestObjectSecond t = list[2];
       int a = t.testFn();

     list[2].testFnArray(list);

       TestMain.superClass[] arraySuper = new TestMain.subClass[20];

  //     arraySuper = Arrays.stream(arraySuper).map(e -> new TestMain.superClass()).collect(Collectors.toList()).toArray(arraySuper);

       TestMain.subClass[] subClassArray = new TestMain.subClass[10];
       TestMain.subClass[] subClassArrayCopy = subClassArray.getClass().cast(arraySuper);
       System.out.println(arraySuper.getClass().toString());
       subClassArray.getClass().cast(arraySuper);
        System.out.println(arraySuper.getClass().toString());
       TestMain.fooClass[] fooArray = new TestMain.fooClass[10];
   //    subClassArray.getClass().cast(fooArray);


       Testgetset test = new Testgetset();
       System.out.println(test.varPub);
       test.varPub = test.varPub + 1;
       System.out.println(test.varPub);
       System.out.println(test.IntPub.intValue());
       test.IntPub = test.IntPub + 1;
       System.out.println(test.IntPub.intValue());
       System.out.println(Testgetset.statVar);
       Testgetset test2 = new Testgetset();
       System.out.println(Testgetset.statVar);

    }
}
