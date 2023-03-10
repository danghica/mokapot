/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.DeprecatedClasses;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AnalyzerAdapter;

/**
 *
 * @author thomasc
 */

@Deprecated
public class ArraySignatureMethodAdapter extends AnalyzerAdapter{
    
    String desc;
    String signature;
    String owner;
    
    public ArraySignatureMethodAdapter(String owner, int access, String name, String desc, String signature, MethodVisitor mv)  {
        super(owner, access, name, desc, mv);
    }
    
    
    

    
}
