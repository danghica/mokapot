///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package xyz.acygn.millr.DeprecatedClasses;
//
//import java.lang.reflect.Constructor;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.lang.reflect.Type;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import org.objenesis.Objenesis;
//import org.objenesis.ObjenesisStd;
//import xyz.acygn.millr.ImplementMilledClassTransformation;
//import xyz.acygn.millr.PathConstants;
//import xyz.acygn.millr.TypeUtil;
//import xyz.acygn.millr.util.ArrayWrapper;
//import xyz.acygn.millr.util.MillredClass;
//import xyz.acygn.millr.util.ObjectArrayWrapper;
//import xyz.acygn.millr.util.RuntimeUnwrapper;
//import xyz.acygn.millr.util.booleanArrayWrapper;
//import xyz.acygn.millr.util.byteArrayWrapper;
//import xyz.acygn.millr.util.charArrayWrapper;
//import xyz.acygn.millr.util.doubleArrayWrapper;
//import xyz.acygn.millr.util.floatArrayWrapper;
//import xyz.acygn.millr.util.intArrayWrapper;
//import xyz.acygn.millr.util.longArrayWrapper;
//import xyz.acygn.millr.util.shortArrayWrapper;
//
///**
// *
// * @author thomasc
// * The main functionality of this class is to establish if a object has a chain of reference to
// * another object with ArrayWrapper types. If so, this class can be used to create a "unwrap" Object,
// * that is a copy of the original object, but such that the chain of references is replaced by
// * a new one leading to an object similar to the first one but with array-types. Furthermore, given an
// * object and his "standin with appropriate types", this class provides two methods allowing one to be
// * updated by copying all fields from the first one.
// *
// * This Class maybe used in the future, maybe not. Shall not be tested.
// */
//public final class HigherOrderUnwrapCode {
//
//
//
//    /**
//     *
//     * A map between Object with ArrayWrapper, and their already created unWrap sibbling.
//     **/
//    static final Map<Object, Object> fromObjectToUnwrap = new HashMap<>();
//    // A map between Standin acting as unwrapped version of objects, and their original referent.
//    static final Map<Object, Object> fromUnwrapToObject = new HashMap<>();
//    // This two class should be inverse to each other at all points.
//
//    /**Given a object living in the millr-space, return a standin for it.
//    *It starts by testing if this object has any chain of references leading to a wrong type. If not, it
//    *simply returns the same object, and update the appropriate static maps.
//    *If so, it either the standin for this object, by creating one if this one has not been created already,
//    /** or by returning the already created one.
//    *
//    * @param a The object with (maybe) arrayWrapper or chain of references to arrayWrapper types.
//    * @return An object similar to the first one without arrayWrapper types.
//    */
//    public static Object getUnwrap(Object a) {
//        if (fromTheApi(a)) {
//            fromObjectToUnwrap.put(a, a);
//            fromUnwrapToObject.put(a, a);
//            return a;
//        }
//        if (!doesItNeedsUnwrapping(a)) {
//            fromObjectToUnwrap.put(a, a);
//            fromUnwrapToObject.put(a, a);
//            return a;
//        }
//        Object copy = fromObjectToUnwrap.get(a);
//        if (copy == null) {
//            Objenesis objenesis = new ObjenesisStd(); // or ObjenesisSerializer
//            copy = objenesis.newInstance(getUnMillrClass(a));
//            fromObjectToUnwrap.put(a, copy);
//            fromUnwrapToObject.put(copy, a);
//        }
//        updateFields(a, copy, Direction.UNWRAP);
//        return copy;
//    }
//
//    /**
//     * Given a standin, returns the referent with updated fields.
//     * That is, given a standin, looks at the map to see which object it is coming from. If none is found,
//     * then this object is assumed not to be a standin, and is returned. Otherwise, the referent is updated
//     * with fields copied from the standin, and returned.
//     * @param copy To fill
//     * @return  To fill
//     */
//    public static final Object getWrap(Object copy) {
//        Object wrap = fromUnwrapToObject.get(copy);
//        if (wrap == null) {
//            return copy;
//        } else {
//            Field[] fields = getAllFields(copy.getClass());
//            updateFields(copy, wrap, Direction.WRAP);
//            return wrap;
//        }
//    }
//
//    /**
//     * The two possible direction: WRAP for wrapping (from array to arrayWrapper), or Unwrap (from arrayWrapper to Array).
//     */
//    enum Direction {
//        UNWRAP, WRAP;
//    }
//
//    /**Given two objects source and target, two fields sourceField and targetField belonging to source
//     * and target respectively, and one direction, update the targetField of the object target by wrapping/unwrapping
//     * the field soureField of the object source and assigning the result to targetField.
//     *
//     * @param source The source Object
//     * @param target The target Object
//     * @param fieldSource The field of the source Object we will get the data from
//     * @param fieldTarget The field of the target Object we will update.
//     * @param direction  Wether we need to wrap / unwrap the field from the source before assigning it to the target.
//     */
//    private final static void updateField(Object source, Object target, Field fieldSource, Field fieldTarget, Direction direction) {
//            if (!Arrays.asList(source.getClass().getFields()).contains(fieldSource)){
//                throw new RuntimeException("The fieldSource is not a field of the source Object");
//            }
//            if (!Arrays.asList(target.getClass().getFields()).contains(fieldTarget)){
//                throw new RuntimeException("The fieldSource is not a field of the source Object");
//            }
//
//        try {
//            fieldSource.setAccessible(true);
//            fieldTarget.setAccessible(true);
//            if (fieldSource.getType().isPrimitive()) {
//                if (!fieldTarget.getType().equals(fieldSource.getType())){
//                    throw new RuntimeException();
//                }
//                switch (fieldSource.getType().getName()) {
//                    case "Z":
//                        fieldTarget.setBoolean(target, fieldSource.getBoolean(source));
//                        break;
//                    case "B":
//                        fieldTarget.setByte(target, fieldSource.getByte(source));
//                        break;
//                    case "C":
//                        fieldTarget.setChar(target, fieldSource.getChar(source));
//                        break;
//                    case "D":
//                        fieldTarget.setDouble(target, fieldSource.getDouble(source));
//                        break;
//                    case "F":
//                        fieldTarget.setFloat(target, fieldSource.getFloat(source));
//                        break;
//                    case "I":
//                        fieldTarget.setInt(target, fieldSource.getInt(source));
//                        break;
//                    case "J":
//                        fieldTarget.setLong(target, fieldSource.getLong(source));
//                        break;
//                    case "S":
//                        fieldTarget.setShort(target, fieldSource.getShort(source));
//                        break;
//                }
//            }else if ((fieldSource.get(source)) instanceof ArrayWrapper){
//                if (direction == Direction.UNWRAP){
//                    fieldTarget.set(target, RuntimeUnwrapper.unwrap(fieldSource.get(source)));
//                }
//                if (direction == Direction.WRAP){
//                    System.err.println("We are trying to wrap sth with an ArrayWrapper");
//                }
//            }
//            else if (direction == Direction.UNWRAP ? doesItNeedsUnwrapping(fieldSource.get(source)) : doesItNeedsWrapping(fieldSource.get(source))) {
//                fieldTarget.set(target, (direction == Direction.UNWRAP) ? getUnwrap(fieldSource.get(source)) : getWrap(fieldSource.get(source)));
//            } else {
//                fieldTarget.set(target, fieldSource.get(source));
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    /**
//     * Given a source object, a target Object, such that the two form a pair referent-standin, update
//     * all the fields from one to the other.
//     * @param source The source object
//     * @param target The target object whose fields will be updated
//     * @param direction Wether we wrap or unwrap.
//     */
//    private final static void updateFields(Object source, Object target, Direction direction) {
//        Field[] fieldsSource = getAllFields(source.getClass());
//        Field[] fieldTraget = getAllFields(target.getClass());
//        Map<Field, Field> fromSourceToTarget = new HashMap<>();
//        for (Field field : fieldsSource){
//            Type T = (direction==Direction.UNWRAP)? getUnMillrClass( field.getType()) : getMillrClass(field.getType());
//            fromSourceToTarget.put(field, Arrays.asList(fieldTraget).stream().filter(e -> e.getName().equals(field.getName())).findAny().get());
//        }
//          fromSourceToTarget.keySet().stream().forEach(field -> updateField(source, target, field, fromSourceToTarget.get(field), direction));
//    }
//
//    /**
//     * Return if an object is from the project or not. If not, it is considered to be coming from the api.
//     *
//     * @param a A object
//     * @return Wether this object is from API or not.
//     */
//    private static boolean fromTheApi(Object a) {
//        return TypeUtil.isNotProject(a.getClass().getCanonicalName());
//    }
//
//    /**
//     * A map that collects information about wether a class needs unwrapping or not.
//     */
//    private static final Map<Class, Boolean> doesClassNeedsUnwrapping = new HashMap<>();
//
//    /**
//     * Return wether an object needs unwrapping or not, that is, if it has a chain of references to
//     * a field of a wrong type (with arrayWrapper in it)
//     * @param a The object
//     * @return Wether it can be passed as such to the API, or if it needs unwrapping.
//     */
//    private static boolean doesItNeedsUnwrapping(Object a) {
//        return doesItNeedsUnwrapping(a, new ArrayList<>());
//    }
//
//
//    /**Method that explore recursively the classes of a method of an object, store the classes explored in
//     * the field classesInExploration, and will not explore the class of a field if this one belongs in classesInExploration.
//     * That way, we can avoid loop of references.
//     *
//     * @param a The object whose field we will explore.
//     * @param classesInExploration The classes that we have already explored.
//     * @return True if if needs unwrapping, false otherwise.
//     */
//    private static boolean doesItNeedsUnwrapping(Object a, Collection<Class> classesInExploration) {
//        classesInExploration.add(a.getClass());
//        if (doesClassNeedsUnwrapping.get(a.getClass()) != null) {
//            return doesClassNeedsUnwrapping.get(a.getClass());
//        }
//        //Needs unwrapping if any of its methods or fields contains arrayWrapper;
//        List<Class> inheritedClassesAndInterfaces = getAllSuperClassAndInterfaces(a);
//        if (doesContainArrayWrapperType(inheritedClassesAndInterfaces, a)) {
//            doesClassNeedsUnwrapping.put(a.getClass(), Boolean.TRUE);
//            return true;
//        }
//        // Or if it recursively points to a object that needs unwrapping;
//        List<Class> inheritedClasses = getAllSuperClass(a);
//        for (Field f : getAllReferenceFields(inheritedClasses)) {
//            try {
//                if (classesInExploration.contains(f.get(a).getClass())) {
//                    // do nothing
//                } else if (doesItNeedsUnwrapping(f.get(a), classesInExploration)) {
//                    doesClassNeedsUnwrapping.put(a.getClass(), Boolean.TRUE);
//                    return true;
//                }
//            } catch (Exception ex) {
//                throw new RuntimeException(ex);
//            }
//        }
//        doesClassNeedsUnwrapping.put(a.getClass(), Boolean.FALSE);
//        return false;
//    }
//
//    /**
//     * Return wether an object belongs to a class such that a millred class of this class exists.
//     * @param a The object
//     * @return True if a milled version of a.getClass() exists, false otherwise.
//     */
//    public static final  boolean  doesItNeedsWrapping(Object a) {
//        try {
//            Class c = Class.forName(getMillrClass(a).getCanonicalName());
//            return true;
//        } catch (ClassNotFoundException ex) {
//            return false;
//        }
//    }
//
//    /**Given an object o implementing a set of classes col, returns whether the exploration of the object
//     * restrained to this set of classes contains fields/methods of arrayWrapper types.
//     *
//     * @param col A collection of classes
//     * @param o An object implementing this collection of classes.
//     * @return Whether the fields/method of this object, relative to the collection of classes, contain some ArrayWrapper types.
//     */
//    private static boolean doesContainArrayWrapperType(Collection<Class> col, Object o) {
//        return col.stream().anyMatch(c -> doesContainArrayWrapperType(c, o));
//    }
//
//    /**
//     * Given an object o instance of class Clazz, explores the method and fields of clazz implement by o
//     * to check wether they contain an arrayWrapper type.
//     * @param clazz A class
//     * @param o An object implementing this class
//     * @return Wether the partial exploration of the object restricted by clazz contains fields /methods with arrayWrapper types.
//     */
//    private final static boolean doesContainArrayWrapperType(Class clazz, Object o) {
//        if (!clazz.isInstance(o)) {
//            throw new RuntimeException("bouarfg");
//        }
//        Field[] fields = clazz.getDeclaredFields();
//        for (Field field : fields) {
//            field.setAccessible(true);
//        }
//        Method[] methods = clazz.getDeclaredMethods();
//        Constructor[] constructors = clazz.getDeclaredConstructors();
//        for (Field f : fields) {
//            if (!f.getType().isPrimitive()) {
//                try {
//                    if (Arrays.asList(PathConstants.LIST_ARRAY_WRAPPER).contains(f.get(o).getClass().getCanonicalName())) {
//                        return true;
//                    }
//                } catch (Exception ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        }
//        for (Method method : methods) {
//            for (Type T : getAllTypeMethod(method)) {
//                if (T.getTypeName().contains("ArrayWrapper")) {
//                    return true;
//                }
//            }
//        }
//        for (Constructor constructor : constructors) {
//            List<Type> types = new ArrayList<>();
//            types.addAll(Arrays.asList(constructor.getGenericExceptionTypes()));
//            types.addAll(Arrays.asList(constructor.getGenericParameterTypes()));
//            for (Type T : types) {
//                if (T.getTypeName().contains("ArrayWrapper")) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    /**
//     * Return all the classes implemented by an object a.
//     * @param a An object
//     * @return The list of all classes this object extends.
//     */
//    private static final List<Class> getAllSuperClass(Object a) {
//        return getAllSuperClass(a.getClass());
//    }
//
//    /**
//     * Return all the classes that extends a class clazz, together with the class clazz.
//     * @param clazz class clazz
//     * @return All the classes that clazz extends, together with clazz itself.
//     */
//    private static final List<Class> getAllSuperClass(Class clazz) {
//        List<Class> inheritedClasses = new ArrayList<>();
//        inheritedClasses.add(clazz);
//        while (clazz.getSuperclass() != null) {
//            inheritedClasses.add(clazz.getSuperclass());
//            clazz = clazz.getSuperclass();
//        }
//        return inheritedClasses;
//    }
//
//    /**
//     * Return all the classes and interface implemented by an object o.
//     * @param o An object
//     * @return A list of all the classes/interface implemented by this object.
//     */
//    private static final List<Class> getAllSuperClassAndInterfaces(Object o) {
//        List<Class> inheritedClassesAndInterfaces = new ArrayList<>();
//        inheritedClassesAndInterfaces.add(o.getClass());
//        Class b = o.getClass();
//        while (b.getSuperclass() != null) {
//            inheritedClassesAndInterfaces.add(b.getSuperclass());
//            inheritedClassesAndInterfaces.addAll(getDirectInheritedInterface(b));
//            b = b.getSuperclass();
//        }
//        return inheritedClassesAndInterfaces;
//
//    }
//
//    /**
//     * Return a list of interfaces implemented by a class clazz, but restricted to those that do
//     * not come from clazz extending another class implementing some interfaces.
//     * @param clazz A class
//     * @return The list of interface directly implemented by clazz, not those implemented through clazz extending other classes.
//     */
//    private static final List<Class> getDirectInheritedInterface(Class clazz) {
//        List<Class> inheritedInterfaces = new ArrayList<>();
//        for (Class b : clazz.getInterfaces()) {
//            inheritedInterfaces.add(b);
//            inheritedInterfaces.addAll(getDirectInheritedInterface(b));
//        }
//        return inheritedInterfaces;
//    }
//
//
//    /**Given a method, return all type types that this method deal with. It encompasses the
//     * types of the arguments, the return types, and the type of the exceptions.
//     *
//     * @param m A method Parameter
//     * @return All the types present in the description of m.
//     */
//    private static final Type[] getAllTypeMethod(Method m) {
//        List<Type> types = new ArrayList<>();
//        types.addAll(Arrays.asList(m.getGenericExceptionTypes()));
//        types.addAll(Arrays.asList(m.getGenericParameterTypes()));
//        types.addAll(Arrays.asList(m.getGenericReturnType()));
//        return types.toArray(new Type[types.size()]);
//    }
//
//    /**
//     * A map mapping to each class the set of fields that objects of this class have.
//     */
//    private static Map<Class, Field[]> fromClassToFields = new HashMap<>();
//
//    /**
//     * Return all the fields present in an object of an input class.
//     * @param clazz A class
//     * @return All the fields present for an object of this class.
//     */
//    private static final Field[] getAllFields(Class clazz) {
//        if (fromClassToFields.get(clazz) == null) {
//            Field[] fields = getAllFields(getAllSuperClass(clazz));
//            fromClassToFields.put(clazz, fields);
//            return fields;
//        }
//        return fromClassToFields.get(clazz);
//
//    }
//
//    /**
//     * Given a collection of class, return the set of fields implemented by each of this class.
//     * @param col A collection of class
//     * @return An array of all fields implemented by each these classes.
//     */
//    private static final Field[] getAllFields(Collection<Class> col) {
//        List<Field> fields = new ArrayList<>();
//        for (Class clazz : col) {
//            Arrays.asList(clazz.getDeclaredFields()).stream().forEach(f -> fields.add(f));
//        }
//        return fields.toArray(new Field[fields.size()]);
//    }
//
//    /**Given a collection of classes, return all the fields that are object-references that these classes implement.
//     *
//     * @param col A collection of classes
//     * @return The fields implemented by these classes that are subclass of object.
//     */
//    private static final Field[] getAllReferenceFields(Collection<Class> col) {
//        List<Field> fields = new ArrayList<>();
//        for (Class clazz : col) {
//            Arrays.asList(clazz.getDeclaredFields()).stream().filter(f -> !f.getType().isPrimitive()).forEach(f -> fields.add(f));
//        }
//        for (Field field : fields) {
//            field.setAccessible(true);
//        }
//        return fields.toArray(new Field[fields.size()]);
//    }
//
//
//    /**
//     * Given an internal name of a class name, returns the name of its milled equivalent assuming this one exists.
//     * @param className An internal name of a class
//     * @return The name of the milled version of this class.
//     */
//    private static final String getMillrName(String className){
//        return className;
//    }
//
//    /**
//     * Given the name of a milled class, returns the name of the original class.
//     * @param className A milled class Name
//     * @return The name of the original class.
//     */
//    private static final String getUnmillrName(String className){
//        return className;
//    }
//
//    /**
//     * Given a class C, returns the milled class corresponding to C, assuming this one exists.
//     * @param c A class
//     * @return The milled version of this class.
//     */
//    private static final Class getMillrType(Class c){
//       try{
//           return Class.forName(getUnmillrName(c.getCanonicalName()));
//       }
//       catch (ClassNotFoundException ex){
//           return c;
//       }
//    }
//
//    /**
//     * Given a class C, returns the  class D such that C is the milled version of D
//     * @param c A class, maybe a milled one
//     * @return  The input class if this one was not a milled one, otherwise the non-milled version of the input class.
//     */
//    private static final Class getUnMillrType(Class c){
//        if (ArrayWrapper.class.isAssignableFrom(c)){
//            if (c.equals(ObjectArrayWrapper.class)){
//                return Object[].class;
//            }
//            if (c.equals(intArrayWrapper.class)){
//                return int[].class;
//            }
//            if (c.equals(booleanArrayWrapper.class)){
//                return boolean[].class;
//            }
//            if (c.equals(byteArrayWrapper.class)){
//                return byte[].class;
//            }
//            if (c.equals(charArrayWrapper.class)){
//                return char[].class;
//            }
//            if (c.equals(doubleArrayWrapper.class)){
//                return double[].class;
//            }
//            if (c.equals(floatArrayWrapper.class)){
//                return float[].class;
//            }
//            if (c.equals(longArrayWrapper.class)){
//                return long[].class;
//            }
//            if (c.equals(shortArrayWrapper.class)){
//                return short[].class;
//            }
//        }
//        else if (Arrays.asList(c.getInterfaces()).contains(MillredClass.class)){
//            //
//            return Object.class;
//        }
//        else{ return c;}
//        return null;
//    }
//
//    /**
//     * Given an object o of Class C, if C is a milled class, then it returns the class D such that the milled version of D is C, otherwise returns C.
//     * @param o An object of a maybe milled Class
//     * @return The unmilled version of the class of the object.
//     */
//    private static final Class getUnMillrClass(Object o) {
//        if ((ArrayWrapper.class).isAssignableFrom(o.getClass())) {
//            return ((ArrayWrapper) o).getReferentClass(null);
//        } else if (o instanceof MillredClass) {
//            return ((MillredClass) o).getOriginalUnmilledClass();
//        } else {
//            return o.getClass();
//        }
//    }
///**
// * Given an object O of class C, return the milled version of C.
// * @param o An object O
// * @return The milled version of its class.
// */
//    private static final Class getMillrClass(Object o) {
//       try{
//           return Class.forName(ImplementMilledClassTransformation.getMaybeMillrName(o.getClass().getName().replace(".", "/")).replace("/", "."));
//       }
//       catch (ClassNotFoundException ex){
//           return o.getClass();
//       }
//    }
//
//
//
//}
