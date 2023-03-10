/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.CoreArrayAnalysis;

import xyz.acygn.millr.CoreArrayAnalysis.Tree.Node;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.objectweb.asm.ClassReader;
import xyz.acygn.millr.Mill;
import xyz.acygn.millr.messages.MessageUtil;

/**
 *
 * @author thomasc
 */
public final class HierarchyClassBuilder {



        Set<ClassReader> setClassReader;
        Map<String, ClassReader> fromNameToClassReader;

        private HierarchyClassBuilder(Collection<ClassReader> col) throws Exception {
            this.setClassReader = new HashSet<ClassReader>(col);
            fromClassReaderToNode = new HashMap<>();
            fromNameToClassReader = new HashMap<>();
            for (ClassReader e : col){
                if (fromNameToClassReader.containsKey(e.getClassName())) {
                    throw new Exception("Pb twice the same name :" + e.getClassName() + "\n"
                    + "First one: " + fromNameToClassReader.get(e.getClassName()).toString() 
                    + "Second one" + e.toString()
                    
                    );
                }
                fromNameToClassReader.put(e.getClassName(), e);
            }
            setClassReader.stream().forEach(e->{
                try {
                    getNode(e);
                } catch (IOException ex) {
                    MessageUtil.error(ex)
                            .report("IOException thrown by the ClassReader reading "  + e.getClassName())
                            .resume();
                }
            });
        }

        Map<ClassReader, Node<ClassReader>> fromClassReaderToNode;

        
        /**
         * Return the node associated with a ClassReader. This has both use: constructing the tree, and once the tree is built, returning the node.
         * The algorithm is as follows. If there is a node registered for a ClassReader, then we return it. Otherwise, we build one, we call this method
         * to get the node for the parent, add the one built as child to the parent, register the new node to the tree, and returns it.
         * @param cr A classReader.
         * @return The node associated with the ClassReader.
         * @throws IOException 
         */
        public synchronized Node<ClassReader> getNode(ClassReader cr) throws IOException {
            if (!setClassReader.contains(cr)) {
                throw new RuntimeException();
            } else if (fromClassReaderToNode.containsKey(cr)) {
                return fromClassReaderToNode.get(cr);
            } else {
                Node<ClassReader> Parent;
                if (cr.getSuperName() != null) {
                    try {
                        Parent = getNode(cr.getSuperName());
                    } catch (NameNotRegisteredException ex) {
                        MessageUtil.error(ex).report("Parent of the class " + cr.getClassName() + " is not part of the JVM :" + ex.name);
                        Parent = null;
                    }
                } else if (!cr.getClassName().equals("java/lang/Object")){
                    try {
                        Parent = getNode("java/lang/Object");
                    } catch (NameNotRegisteredException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                else{
                    Parent = null;
                }
                Node node = new Node(cr, Parent, null);
                if (Parent != null){
                    Parent.addChild(node);
                }
                synchronized (this) {
                    if (fromClassReaderToNode.containsKey(cr)) {
                        return fromClassReaderToNode.get(cr);
                    } else {
                        fromClassReaderToNode.put(cr, node);
                        return node;
                    }
                }
            }
        }

        public synchronized Node<ClassReader> getNode(String name) throws IOException, NameNotRegisteredException {
            ClassReader cr = fromNameToClassReader.get(name);
            if (cr != null){
                return getNode(cr);
            } else {
                throw new NameNotRegisteredException(name);
//                MessageUtil.warning("This case should not be happening: no ClassReader registered for " + name).emit();
//                cr = new ClassReader(name);
//                fromNameToClassReader.put(name, cr);
//                setClassReader.add(cr);
//                return getNode(cr);
            }
        }
        
        
        class NameNotRegisteredException extends Exception{
            String name;
            
            public NameNotRegisteredException(String name){
                this.name = name;
            }
        }


    static HierarchyClassBuilder getHierarchy(Collection<ClassReader> col) throws IOException, Exception {
        if (col.stream().anyMatch(e->e==null)){
            MessageUtil.warning("The collection of ClassReader has a null pointer: This should not be happening").emit();
            col = col.stream().filter(e-> e != null).collect(Collectors.toSet());
        }
        if (col.stream().anyMatch(e->e.getClassName()==null)){
            MessageUtil.warning("The collection of ClassReader has a ClassReader whose className is null: This case should not be happening").emit();
            col = col.stream().filter(e-> e.getClassName()!=null).collect(Collectors.toSet());
        }   
       return new HierarchyClassBuilder(col);   

    }


}
