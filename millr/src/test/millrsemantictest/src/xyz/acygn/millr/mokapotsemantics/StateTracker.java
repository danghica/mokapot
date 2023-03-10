package xyz.acygn.millr.mokapotsemantics;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.*;



/**
 * A {@code StateTracker} is used to track the state of objects, by means of
 * tracking the state of fields and the returned values of methods. Client
 * code registers a field or a method for a specific object with a {@code StateTracker},
 * and calls {@code collect()} on the {@code StateTracker} whenever the state
 * of all registered fields and methods should be queried. A {@code StateTracker}
 * can produce a string representation of the collected state information.
 * <p>
 * This class is designed to be used in conjuntion with mokapot / millr. Therefore, when comparing two objects,
 * fields that have been implemented by millr will not be considered. Furthermore, arrays and arrayWrapper will
 * be considered equally. Finally, accessing remote fields will be handled through getters.
 * <p>
 * <p>
 * At the moment, whereas we are considering remote objects should be indicated in the constructor. In that case,
 * exploration of API class will NOT happen, since fields access do not work remotely. Instead, we will only explore
 * fields from classes that have been milled.
 *
 * <p>
 * This class MUST be called through a ClassLoader that loads Common.
 *
 * @author Marcello De Bernardi, Thomas Cuvillier
 */
public class StateTracker {

    public static Class<?> ProxyOrWrapperClass ;
    public static Method unProxyClass;
    public static Class<?> NameSpacerClass;
    static final Set<Class<?>> printableTypes = new HashSet<>();

    {
        printableTypes.addAll(Arrays.asList(new Class<?>[]{boolean.class, byte.class, char.class, double.class,
                float.class, int.class, long.class, short.class, Boolean.class,
                Byte.class, Character.class, Double.class, Float.class, Integer.class, Long.class, Short.class}));

    }


    /**
     * The registered objects to Track;
     */
    private LinkedHashSet<Object> registeredObjects;


    /**
     * The StateTrace on which the states of the objects will be registered.
     */
    private final StateTrace stateSequence;


    /**
     * True if some of the explored objects are remote. In which case we will not try to access fields directly.
     */
    private boolean isRemote;


    /**
     * The ArrayWrapper class. Will be initialized through reflection thanks to the ClassLoader.
     */
    static Class ArrayWrapperClass;

    /**
     * The method to Unwrap a object (ArrayWrapper -> Array).
     */
    static Class RuntimeUnwrapper;

    /**
     * The method to Unwrap an ObjectArrayWrapper.
     */
    static Method asArray;

    /**
     * The method to Unwrap a class (ArrayWrapper.class -> Array.class)
     */
    static Method unwrapClass;


    public static void intializeVariable(URLClassLoader urlClassLoader) throws NoSuchMethodException, ClassNotFoundException {
        printableTypes.addAll(Arrays.asList(new Class<?>[]{boolean.class, byte.class, char.class, double.class,
                float.class, int.class, long.class, short.class, Boolean.class,
                Byte.class, Character.class, Double.class, Float.class, Integer.class, Long.class, Short.class}));
        ArrayWrapperClass = urlClassLoader.loadClass("xyz.acygn.millr.util.ArrayWrapper");
        RuntimeUnwrapper = urlClassLoader.loadClass("xyz.acygn.millr.util.RuntimeUnwrapper");
        asArray = ArrayWrapperClass.getDeclaredMethod("asArray");
        unwrapClass = RuntimeUnwrapper.getDeclaredMethod("unwrapClass", Class.class);
        ProxyOrWrapperClass = urlClassLoader.loadClass("xyz.acygn.mokapot.skeletons.ProxyOrWrapper");
        unProxyClass = Arrays.stream(ArrayWrapperClass.getDeclaredMethods()).filter(e->e.getName().equals("getReferentClass")).findAny().get();
        System.out.println("UNPROXYClass");
        Arrays.stream(unProxyClass.getParameterTypes()).forEach(e->System.out.println(e.getName()));
        NameSpacerClass  = urlClassLoader.loadClass(ProxyOrWrapperClass.getName() +"$Namespacer");

    }


    /**
     * This class will implements a kind of "String serialization" of an object, by recursively exploring its fields, and
     * calling the toString() method on types that are deemed printable.
     *
     * The printable types are those on which we deem that the toString() method represents an accurate description
     * of the object / type. For instance, two objects of type Integer might be different (wrt to reference equality),
     * but we esteem that their are truely different if and only if they represent two different numbers. Therefore,
     * we do not explore their fields, and simply consider their face values.
     *
     * If a type is printable, then exploration of the object will not happen and the toString() value will be called
     * instead. Therefore, the more printable types, the more concise the representation of the original object will be.
     */


    /**
     * Initialize the state Tracker;
     *
     * @param urlClassLoader a class loader with Millr, Mokapot, and Common on its ClassPath.
     * @param isRemote       if we consider that some objects that will be tracked will be remote.
     */
    public StateTracker(URLClassLoader urlClassLoader, boolean isRemote) throws ClassNotFoundException, NoSuchMethodException {
        this.stateSequence = new StateTrace(isRemote);
        intializeVariable(urlClassLoader);
        registeredObjects = new LinkedHashSet<>();
    }




    /**
     * Register an Object, whose printable fields will be collected. This Object should be a field of the current class, otherwise
     * it would not be registered.
     */
    public void register(Object o) {
        registeredObjects.add(o);
    }


    /**
     * Collects the current state of all tracked fields and methods.
     */
    public void collect() throws IllegalAccessException, InvocationTargetException {
        stateSequence.createNewShapShot();
        for (Object object : registeredObjects) {
            stateSequence.register(object);
        }
    }


    /**
     * collect the current state of all the registered objects.
     *
     * @throws IllegalAccessException
     */
    public void collectAll() throws IllegalAccessException, InvocationTargetException {
        stateSequence.createNewShapShot();
        for (Object object : registeredObjects) {
            stateSequence.registerAll(object);
        }
    }


    /**
     * The {@code StateTracker} is informed that the specified type has a meaningful
     * implementation of {@code toString()}. A meaningful implementation is one that
     * returns a string representation dependent on the internal, human-readable
     * values within an object, not on the identity of the object or the identity
     * of any of its children.
     * <p>
     * As a rule of thumb, if the location of an object in a distributed setting
     * changes its {@code toString()} representation, then that class should not
     * be declared printable.
     *
     * @param type the type which is declared printable
     */
    public void declarePrintable(Class<?> type) {
        printableTypes.add(type);
    }

    public String getStateToString() {
        return stateSequence.toString();
    }


    public StateTrace getStateTrace(){
        return stateSequence;
    }
}



