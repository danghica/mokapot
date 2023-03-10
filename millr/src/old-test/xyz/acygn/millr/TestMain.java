package xyz.acygn.millr;

import com.sun.org.apache.xpath.internal.functions.FuncBoolean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TestMain {
    
    static class superClass{
        
    }
    static class subClass extends superClass{
        
    }
    
    class fooClass{
        
    }

    class a{
        a b;
        a c = b.b;
    }
    
    @interface StupidAnnotation{
    }


    public void stupidMethod(@StupidAnnotation int a){
        
    }
    
    public static void main(String[] args) {
//    int[] a = new int[300];
//
////        for (int i=12; i<a.length; i++) {
////            System.out.println(i);
////            a = new int[i+1];
////
//
// //   } // prints 12 once
//
//            for (int i: a) {
//     //       System.out.println(i + "" + a.length);
//            a = new int[i+1];
//        }
//
//        for (int i: a) {
//       //     System.out.println(i + "" + a.length);
//            a = new int[a.length-1];
//        } // prints 0 thirteen times
//
//
//        Object oOne = new Object(){};
//        Object oTwo = new Object(){ int field;};
//        Method[] oOneMethods = oOne.getClass().getDeclaredMethods();
//        Method[] oTwoMethods = oTwo.getClass().getDeclaredMethods();
//        System.out.println(Arrays.equals(oOneMethods, oTwoMethods));
//
//       superClass[] arraySuper = new subClass[20];
//       arraySuper = Arrays.stream(arraySuper).map(e -> new subClass()).collect(Collectors.toList()).toArray(arraySuper);
//
//       BiFunction<int[], superClass[], Object[]> function = new BiFunction<int[], superClass[], Object[]>(){
//        @Override
//        public Object[] apply(int[] t, superClass[] u) {
//            System.out.println(t.getClass());
//            System.out.println(u.getClass());
//            return new Object[]{t, u};
//        }
//
//    };

       String[] arrayStringInitial = new String[]{"a", "b", "c"};
        String[] arrayString = Stream.concat(Arrays.stream(arrayStringInitial),
                Stream.of("xyz/acygn/mokapot/Standin")).
                toArray((x) -> new String[x]);





//       Object[] result = function.apply(new int[10], new superClass[10]);
//       System.out.println(result.length);
//
//
//       subClass[] subClassArray = new subClass[10];
//       subClass[] subClassArrayCopy = subClassArray.getClass().cast(arraySuper);
//       System.out.println(arraySuper.getClass().toString());
//       subClassArray.getClass().cast(arraySuper);
//        System.out.println(arraySuper.getClass().toString());
//       fooClass[] fooArray = new fooClass[10];
//     //  subClassArray.getClass().cast(fooArray);
//
//
//       Testgetset test = new Testgetset();
//       System.out.println(test.varPub);
//       test.varPub = test.varPub + 1;
//       System.out.println(test.varPub);
//       System.out.println(test.IntPub.intValue());
//       test.IntPub = test.IntPub + 1;
//       System.out.println(test.IntPub.intValue());
//       System.out.println(Testgetset.statVar);
//       Testgetset test2 = new Testgetset();
//       System.out.println(Testgetset.statVar);
//
//        String[] stringArray = new String[]{"a", "abc", "awerfwe", "de"};
//        List<String> stringList = Arrays.asList(stringArray);
//        int sum = Arrays.stream(stringArray).toArray((n) -> new Object[n]).length;
//     //   System.out.println(sum);

    }
}
