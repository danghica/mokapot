/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.CoreArrayAnalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author thomasc
 * @param <T> The class of the vertices of this tree.
 */
public class Tree<T> {

    final private Node<T> root;

    public Tree(T rootData) {
        root = new Node<T>(rootData, null, new HashSet<Node<T>>());
        root.data = rootData;
        root.children = new HashSet<Node<T>>();
    }

    private Tree(Node<T> node) {
        root = node;
    }

    public Node<T> getRoot() {
        return root;
    }

    public Set<Node<T>> getAllNodes() {
        Set<Node<T>> list = root.getRecursiveChildren();
        list.add(root);
        return list;
    }

    public static class Node<T> {

        private T data;
        private Node<T> parent;
        private Set<Node<T>> children;

        public Node(T data, Node<T> parent, Set<Node<T>> children) {
            this.data = data;
            this.parent = parent;
            this.children = (children != null)? children : new HashSet<>();
            if (parent != null){
                parent.addChild(this);
            }
        }

        public Node<T> getParent() {
            return parent;
        }

        public Set<Node<T>> getChildren() {
            return children;
        }

        public T getData() {
            return data;
        }

        public Set<Node<T>> getRecursiveChildren() {
            return getRecursiveChildren(new HashSet<>());
        }

        private Set<Node<T>> getRecursiveChildren(Set<Node<T>> listChild) {
            listChild.addAll(children);
            for (Node<T> child : children) {
                child.getRecursiveChildren(listChild);
            }
            return listChild;
        }

        public List<Node<T>> getRecursiveParents() {
            return getRecursiveParents(new ArrayList<>());
        }

        private List<Node<T>> getRecursiveParents(List<Node<T>> listParent) {
            if (this.parent == null) {
                return listParent;
            } else {
                parent.getRecursiveParents(listParent);
                listParent.add(parent);
            }
            return listParent;
        }

        public Tree<T> getSubTree() {
            return new Tree<T>(this);
        }

        public void addChild(Node<T> child) {
            children.add(child);
        }
    }

}
