package xyz.acygn.millr;


import xyz.acygn.millr.subPack.inter;

import java.lang.reflect.Field;

public  class ClassA {

    Object FieldA;


    Object getField(){
        return FieldA;
    }


     class classB extends ClassC{


        Object getFieldFromB(){
            return FieldA;
        }
    }

}



