/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

/**
 *
 * @author thomasc
 */
public class testsynchronized {
    
    static class newClazz{
        
    }

        public static void main(String... args){
            Object o = new newClazz();
            newClazz otwo = null;
            int i = 2;
            synchronized(o){
                System.out.println("hello world");
                System.out.println(i);
                 i += 1;
            }
            System.out.println(i);
        }
    
    
}
