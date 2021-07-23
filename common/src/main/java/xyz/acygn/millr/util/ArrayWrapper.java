/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.util;

import java.io.Serializable;

import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.skeletons.ArtificialWrapper;
import xyz.acygn.mokapot.markers.NonCopiable;
import xyz.acygn.mokapot.skeletons.ProxyOrWrapper.Namespacer;

/**
 *
 * @author thomas
 */
public interface ArrayWrapper extends java.lang.Cloneable, java.io.Serializable, Copiable {
    
    public Object asArray();
    
//    public void setArray(Object o);
    

    // The goal of isEquals is to overload ==
    public boolean isEquals(Object o);
    
    // This one should be overloaded as well
    public boolean equals(Object o);
    
    public Class getReferentClass(Namespacer dummy);
    
    
    
    public void update(Object o);

}
