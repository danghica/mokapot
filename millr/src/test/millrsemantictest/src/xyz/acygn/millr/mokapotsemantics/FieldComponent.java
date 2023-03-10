/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr.mokapotsemantics;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Represents a field which is registered with the StateTracker.
 * Encapsulates
 * the field itself, the target object for which the field is being tracked.
 * Two fieldsComponent are equal if they have same name, almost equal Classes,
 * and they are coming from object with almost equal Types.
 * The class hides away if the field is coming from an arrayWrapper, by
 * making it as if it was an array.
 * Therefore, this class provides a set of tools to access the class of the
 * object stored in the field, tools that
 * abstract away the difference between array and arrayWrapper.
 */
class FieldComponent<T> implements Comparable<FieldComponent> {
    private final String name;
    private final boolean isInitialized;
    private final Object target;
    private final WeakClass weakClassField;
    private final WeakClass weakClassTarget;
    private final WeakObject weakValue;


    /**
     * The list of types that are deemed printable.
     */


    public Object getTarget() {
        return target;
    }


    /**
     * The field component is built from the actual field, and the object it comes from.
     *
     * @param field
     * @param target
     */
    FieldComponent(Field field, Object target) {
        try {
            this.name = field.getName();
            this.weakClassTarget = new WeakClass(target.getClass(), target);
            this.target = target;
            this.isInitialized = setInitialized(field);
            WeakObject tempValue;
            try {
                tempValue = tryGetValue(field);
            } catch (Exception e) {
                tempValue = null;
            }
            this.weakValue = tempValue;
            this.weakClassField = (tempValue==null || tempValue.isNull()) ? new WeakClass(field.getType(), null ) : tempValue.getWeakClass();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     * We build a field component out of an array, by saying the element i of the array as the i^th field of the
     * array.
     * @param o
     * @param i
     */
    FieldComponent(Object o, int i) {
        this.name = "Element " + String.valueOf(i);
        this.isInitialized = true;
        this.target = o;
        this.weakClassTarget = new WeakClass(o.getClass(), o);
        this.weakValue = new WeakObject(Array.get(o, i));
        this.weakClassField = new WeakClass(o.getClass().getComponentType(), weakValue);
    }

    public boolean isInitialized() {
        return isInitialized;
    }


    private boolean isObjectMilled() {
        return getWeakClassObject().isMilled();
    }


    /**
     * Test if the field has been initialized.
     *
     * @return
     */
    private boolean setInitialized(Field field) throws InvocationTargetException {
        try {
            tryGetValue(field);
            return true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isInitializedWithNonNull() {
        return isInitialized() && !getValue().isNull();
    }

    /**
     * Name of the field.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * The WeakClass of the field.
     *
     * @return
     */
    public WeakClass getDeclaredWeakClassField() {
        return weakClassField;
    }

    /**
     * Get the WeakClass of the value actually detained by the field.
     * Should only be called if the field has been initialized with a non-null value;
     * Return null if the field has not been initialized.
     *
     * @return
     */
    public WeakClass getWeakClassField() {
        if (!isInitializedWithNonNull()) {
            return null;
        }
        return getValue().getWeakClass();
    }

    /**
     * The WeakClass of the object.
     *
     * @return
     */
    public WeakClass getWeakClassObject() {
        return weakClassTarget;
    }

    /**
     * Two fields component are equal if they have same name, the WeakClass of their object is the same, the WeakClass of their value are the same.
     * However, they can have different values ! (Since they may be carried by different objects...).
     * The idea is as follows, if we have two different objects, we may want to compare fields from these objects that are the same, to check if the two
     * objects are "equal".
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof FieldComponent)) {
            return false;
        }
        FieldComponent objField = (FieldComponent) o;
        return objField.getName().equals(this.getName()) && objField.getDeclaredWeakClassField().equals(this.getDeclaredWeakClassField()) && this.getWeakClassObject().equals(objField.getWeakClassObject());
    }

    /**
     * The appropriate hashCode function, designed to work in conjunction with the equals one.
     *
     * @return
     */
    @Override
    public int hashCode() {
        return getWeakClassObject().hashCode() ^ getDeclaredWeakClassField().hashCode() ^ getName().hashCode();
    }


    private WeakObject tryGetValue(Field field) throws IllegalAccessException, InvocationTargetException {
        Object returnValue;
        if (!isObjectMilled()) {
            field.setAccessible(true);
            returnValue = field.get(target);
        } else {
            try {
                Method millrRemote = target.getClass().getDeclaredMethod("_millr_" + field.getName() + "get");
                millrRemote.setAccessible(true);
                returnValue = millrRemote.invoke(target);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException((ex));
            }
        }
        return new WeakObject(returnValue);
    }


    /**
     * Return the value of this field. If the actual value of the field is an arrayWrapper, then it will returns the
     * array encapsulated by this ArrayWrapper instead.
     *
     * @return
     */
    public WeakObject getValue() {
        return weakValue;
    }


    /**
     * Implementation of the compareTo function that leads to a total order: Two fields that are not equal
     * are related.
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(FieldComponent o) {
        if (!this.getName().equals(o.getName())) {
            return (o.getName()).hashCode() - getName().hashCode();
        } else if (!this.getDeclaredWeakClassField().equals(o.getDeclaredWeakClassField())) {
            return o.getDeclaredWeakClassField().hashCode() - this.getDeclaredWeakClassField().hashCode();
        } else if (!this.getWeakClassObject().equals(o.getWeakClassObject())) {
            return o.getWeakClassObject().hashCode() - this.getWeakClassObject().hashCode();
        }
        return 0;
    }


    /**
     * Is the field coming from Millr (tipically, the lock).
     * Maybe changed as Millr evolves.
     *
     * @return
     */
    public boolean isComingFromMillr() {
        try {
            return (getName().equals("_millr_doneLatch") || getName().equals("_millr_startLatch"))
                    && getDeclaredWeakClassField().getInternalClass().equals(java.util.concurrent.CountDownLatch.class);
        }
        catch( Throwable t){
            System.out.println("insn");
            throw t;
        }
    }

    /**
     * If the value of the field is neither a primitive nor an Array.
     *
     * @return
     */
    public boolean isObject() {
        return getWeakClassObject().isObject();
    }

    /**
     * If the value / object of the field is coming from mokapot ...
     *
     * @return
     */
    public boolean isComingFromMokapot() {
        if (getDeclaredWeakClassField().getName().startsWith("xyz.acygn.mokapot")) return true;
        if (isInitializedWithNonNull()) {
            return getWeakClassField().getName().startsWith("xyz.acygn.mokapot");
        }
        return false;
    }


    /**
     * A valid String representation of the field. Note that two fields that are equal can have different String
     * representation, since this one takes into account the value.
     *
     * @return
     */
    public String toString() {
        try {
            return this.getName() + " = " + getValue().getStringValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            return this.getName() + " = " + super.toString();
        }
    }

    public boolean shouldNotBeExplored(){
        return isComingFromMillr() || isComingFromMokapot() || isPartOfTheTest();
    }

    private boolean isPartOfTheTest(){
        return getDeclaredWeakClassField().getName().startsWith("xyz.acygn.millr.mokapotsemantics");
    }

    public boolean hasTargetGetter(){
        return new WeakObject(target).hasGetter() || target.getClass().isAssignableFrom(StateTracker.ArrayWrapperClass);
    }




}

