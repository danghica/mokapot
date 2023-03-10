package xyz.acygn.millr;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import xyz.acygn.millr.ReflectionUtilsMillr;


/**
 * A {@code StateTracker} is used to track the state of objects, by means of
 * tracking the state of fields and the returned values of methods. Client
 * code registers a field or a method for a specific object with a {@code StateTracker},
 * and calls {@code collect()} on the {@code StateTracker} whenever the state
 * of all registered fields and methods should be queried. A {@code StateTracker}
 * can produce a string representation of the collected state information
 *
 *
 * @author Marcello De Bernardi
 */
public class StateTracker {
    private final Set<Class<?>> printableTypes;
    private LinkedHashSet<FieldComponent> fieldComponents;
    private LinkedHashSet<MethodComponent> methodComponents;
    private StringBuilder stateSequence;


    public StateTracker() {
        printableTypes = new HashSet<>();

        printableTypes.add(boolean.class);
        printableTypes.add(byte.class);
        printableTypes.add(char.class);
        printableTypes.add(double.class);
        printableTypes.add(float.class);
        printableTypes.add(int.class);
        printableTypes.add(long.class);
        printableTypes.add(short.class);

        fieldComponents = new LinkedHashSet<>();
        methodComponents = new LinkedHashSet<>();
        stateSequence = new StringBuilder();
    }


    /**
     * Registers the field with the given name as a field whose state should be tracked.
     *
     * @param fieldName name of the field
     * @param target    object to which the field belongs
     * @param type      type of the field
     * @throws NoSuchFieldException
     */
    public void register(String fieldName, Object target, Class<?> type) throws NoSuchFieldException {
        Field field = ReflectionUtilsMillr.getField( target.getClass(), fieldName);
        field.setAccessible(true);

        fieldComponents.add(new FieldComponent(field, target, type));
    }

    /**
     * Registers the method with the given name and parameters (for the provided object)
     * as a method whose state should be tracked.
     *
     * @param methodName     name of the method
     * @param target         object on which we track the method
     * @param parameterTypes parameter types of the method
     * @throws NoSuchMethodException if no method by this name and parameter types is found
     */
    public void register(String methodName, Object target, List<Class<?>> parameterTypes)
            throws NoSuchMethodException {
        Method method = target.getClass().getMethod(methodName, (Class<?>[]) parameterTypes.toArray());
        method.setAccessible(true);
        Class<?> returnType = method.getReturnType();

        methodComponents.add(new MethodComponent(method, target, returnType, (Class<?>[]) parameterTypes.toArray()));
    }

    /**
     * Collects the current state of all tracked fields and methods.
     */
    public void collect() throws IllegalAccessException {
        for (FieldComponent component : fieldComponents) {
            if (isPrintable(component.type)) {
                stateSequence.append(component.field.getName());
                stateSequence.append(" = ");
                stateSequence.append(component.field.get(component.target));
                stateSequence.append("\n");
            }
            else if (isPrintableArray(component.type)) {
                stateSequence.append(component.field.getName());
                stateSequence.append(" = ");
                stateSequence.append(Arrays.toString((Object[]) component.field.get(component.target)));
                stateSequence.append("\n");
            }
        }

        for (MethodComponent component : methodComponents) {
            if (isPrintable(component.returnType)) {
                stateSequence.append(component.method.getName());
                stateSequence.append(" -> ");
//                stateSequence.append(component.method.invoke(component.target, null))
            }
        }
    }

    /**
     * Clears all the state information collected by this {@code StateTracker} up to
     * this point.
     */
    public void clear() {
        stateSequence.delete(0, stateSequence.length());
    }

    /**
     * The {@code StateTracker} is informed that the specified type has a meaningful
     * implementation of {@code toString()}. A meaningful implementation is one that
     * returns a string representation dependent on the internal, human-readable
     * values within an object, not on the identity of the object or the identity
     * of any of its children.
     *
     * As a rule of thumb, if the location of an object in a distributed setting
     * changes its {@code toString()} representation, then that class should not
     * be declared printable.
     *
     * @param type the type which is declared printable
     */
    public void declarePrintable(Class<?> type) {
        printableTypes.add(type);
    }

    @Override
    public String toString() {
        return stateSequence.toString();
    }


    /**
     * Determines whether a type is directly printable, that is, if it belongs to a
     * set of types which we know are trivially convertible to a meaningful string. This
     * includes primitives, wrapper types, and Strings. If a type does belong to this
     * group of types, it can be safely added to the state sequence by calling
     * {@code toString()} on an object of this type.
     *
     * @param type type to check for whether it can be printed
     * @return true if trivially printable, false if requires inspection
     */
    private boolean isPrintable(Class<?> type) {
        return printableTypes.contains(type);
    }

    /**
     * Determines whether the provided type is an array of trivially printable types, that is,
     * if the array consists of elements for which {@code isPrintable()} would return true. Returns
     * false if the array is not of a trivially printable type, or if the type given is not an
     * array type at all.
     *
     * @param type type to inspect
     * @return true if array of trivially printable elements, false in all other cases
     */
    private boolean isPrintableArray(Class<?> type) {
        return type.isArray() && isPrintable(type.getComponentType());
    }


    /**
     * Represents a field which is registered with the StateTracker. Encapsulates
     * the field itself, the target object for which the field is being tracked,
     * and the type of the field for convenience.
     */
    private class FieldComponent  implements Comparable{
        Field field;
        Object target;
        Class<?> type;

        FieldComponent(Field field, Object target, Class<?> type) {
            this.field = field;
            this.target = target;
            this.type = type;
        }

        @Override
        public int compareTo(Object o) {
            return 0;
        }
    }

    /**
     * Represents a method which is registered with the StateTracker. Encapsulates
     * the method itself, the target object on which it should be called, the
     * return type of the method, and the types of the formal parameters.
     */
    private class MethodComponent {
        Method method;
        Object target;
        Class<?> returnType;
        Class<?>[] parameterTypes;

        MethodComponent(Method method, Object target, Class<?> returnType, Class<?>... parameterTypes) {
            this.method = method;
            this.target = target;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }
    }
}
