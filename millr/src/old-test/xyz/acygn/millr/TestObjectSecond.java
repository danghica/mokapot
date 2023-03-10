/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

/**
 *
 * @author thomas
 */
public class TestObjectSecond {
    
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
