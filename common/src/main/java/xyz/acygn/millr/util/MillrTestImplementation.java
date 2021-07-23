///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package xyz.acygn.millr.util.util;
//
//
//import xyz.acygn.millr.SignatureArrayTypeModifier;
//
///**
// *
// * @author thomas
// */
//public class MillrTestImplementation{
//
//    public static final boolean referenceEquals(final Object a, final Object b){
//        if (a instanceof ArrayWrapper){
//            return ((ArrayWrapper) a).isEquals(b);
//        }
//        return a == b;
//    }
//
//    public static final  Class<?> getActualClass(final Object o){
//        if (o instanceof ArrayWrapper){
//            return ((ArrayWrapper) o).getReferentClass(null);
//        }
//        else{
//            return o.getClass();
//        }
//    }
//
//
//    public static final boolean isInstanceOf(final Object o,final  Class<?> Cl){
//        if (o==null){
//            return false;
//        }
//   //     boolean bl = Cl.isAssignableFrom(getActualClass(o));
//        return Cl.isAssignableFrom(getActualClass(o));
//    }
//
//    public static final Object cast(final Class<?> Cl, final Object o){
//        if (Cl.isArray()){
//            switch (Cl.getName()){
//                case "[Z" : return booleanArrayWrapper.class.cast(o);
//                case "[C" : return charArrayWrapper.class.cast(o);
//                case "[S" : return shortArrayWrapper.class.cast(o);
//                case "[I" : return intArrayWrapper.class.cast(o);
//                case "[L" : return longArrayWrapper.class.cast(o);
//                case "[F" : return floatArrayWrapper.class.cast(o);
//                case "[D" : return doubleArrayWrapper.class.cast(o);
//                default : if (!(o instanceof ObjectArrayWrapper) ){
//                    // We expect this to produces a ClassCastException
//                    // As o is not an arrayWrapper, and, if our code is sound, is not an array neither
//                    //so cannot be cast to Cl, that is an array.
//                    Cl.cast(o);
//                    return ArrayWrapper.class.cast(o);
//                }
//                else{
//                    final ObjectArrayWrapper arrayO = (ObjectArrayWrapper) o;
//                    Object arrayCast = java.lang.reflect.Array.newInstance(arrayO.getReferentClass(null).getComponentType(), 0);
//                    Cl.cast(arrayCast);
//                    return ArrayWrapper.class.cast(o);
//                }
//
//            }
//        }
//        else return Cl.cast(o);
//    }
//
//    public static final String introspectionString(final String maybeDesc){
//        try{
//            return SignatureArrayTypeModifier.getNewSignature(maybeDesc);
//        }
//        catch(Exception ex){
//            return maybeDesc;
//        }
//    }
//
//
//
//}
