package xyz.acygn.millr;


import org.objectweb.asm.Type;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.Opcodes;
import xyz.acygn.millr.messages.NoSuchClassException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class with static methods and caching to speed up getting informations about class.
 * Note that we obtain informations about class BEFORE any transformation happens, that is, the original class.
 * If a class is created by the transformation, it is a bad idea to try get informations from here.
 */

public class ClassDataBase {

    /**
     * Private Constructor since this class has should not be instantiated;
     */
    private ClassDataBase() {
    }

    /**
     * If this boolean is set, then the construction of database will be slower, but we will look into all the details
     * of the class, notably looking at all the methods it inherits, and constructing the appropriate relation.
     */
    private static boolean constructThoroughDatabase;


    static void setConstructThoroughDatabase(boolean on) {
        constructThoroughDatabase = on;
    }

    static Optional<ClassDataBase.ClassData> getClassDataOrNull(String owner) {
        return Optional.ofNullable(classDataMap.get(owner));
    }


    /**
     * Set of methods that are linked one to another by the "overriding / implement"
     * relations in the classes that we mill.
     */

    final static Set<Set<MethodParameter>> setOfCompatibleMethod = new HashSet<>();


    final static boolean addCompatible(MethodParameter mpOne, MethodParameter mpTwo) {
        if (mpOne.areOverrideCompatible(mpTwo)) {
            return overridingRelation.add(new Pair<>(mpOne, mpTwo));
        }
        return false;
    }


    /*
        The difficulty regarding the overriding / implementation relation is that a method can override/implement
        a method without knowing it in the first place.
        For instance, let us consider a class A with a method "methodSpecial", a class B extending A and
        an interface C, with the interface C defining methodSpecial. Then the methodSpecial from A implements
        the method from C, even though class A does not implements C.
     */


    /**
     * We need to make sure that if one method implements another, then their millr implementation get the same name.
     * Unfortunately, one cannot solely rely on their argument types, since two methods may have same
     * name, same argument types, but different return types in a class (then one of the method would have to be
     * synthetic, and defined by the java compiler for type erasure).
     * Furthermore, one method can override another and have a different return type as well.
     * To avoid this issue, we scan through the classes and store what method overrides another. Then methods
     * that are in the same "equivalence class" of the reflexive, transitive closure of the overriding /implement
     * relation get mapped to the same name.
     */

    /**
     *
     */
    final static Set<Pair<MethodParameter, MethodParameter>> overridingRelation = new HashSet<>();


    final static HashMap<MethodParameter, Set<MethodParameter>> getCompatibleEquivalenceClass = new HashMap<>();


    static String getCodeForMethod(MethodParameter mp) {
//        Set<MethodParameter> setClass = getCompatibleEquivalenceClass.get(mp);
//        if (setClass != null) {
//            return String.valueOf(setClass.hashCode());
//        } else {
            // Default value, but this code should not be reached.
            return String.valueOf(Math.abs(Arrays.hashCode(Type.getArgumentTypes(mp.methodDesc))));
 //       }
    }

    static class Pair<T, U> {
        T key;
        U value;

        Pair(T key, U value) {
            this.key = key;
            this.value = value;
        }
    }


    static synchronized void constructSetOfCompatibleMethod() {
        if (!setOfCompatibleMethod.isEmpty()) {
            // The set has already been constructed.
            return;
        } else {
            //We iterate through the overridingRelation
            for (Pair<MethodParameter, MethodParameter> pair : overridingRelation) {
                Optional<Set<MethodParameter>> setKey = setOfCompatibleMethod.stream().filter(e -> e.contains(pair.key)).findAny();
                Optional<Set<MethodParameter>> setValue = setOfCompatibleMethod.stream().filter(e -> e.contains(pair.value)).findAny();
                if (!setKey.isPresent()) {
                    setKey = Optional.of(new HashSet<>(Collections.singleton(pair.key)));
                    setOfCompatibleMethod.add(setKey.get());
                }
                if (!setValue.isPresent()) {
                    setValue = Optional.of(new HashSet<>(Collections.singleton(pair.key)));
                } else {
                    setOfCompatibleMethod.remove(setValue.get());
                }
                // We now have two sets containing the sets of methods that are overriden/ implemented by one another.
                // We merge then.
                setKey.get().addAll(setValue.get());
            }
        }
        setOfCompatibleMethod.stream().forEach(e -> {
                    e.stream().forEach(f -> getCompatibleEquivalenceClass.put(f, e));
                }
        );
    }


    /**
     * A map from classParameter to ClassData to cache the result;
     */
    private static Map<String, ClassData> classDataMap = new HashMap<>();


    static ClassData getClassData(String name) {
        if (name.startsWith("[")) {
            throw new UnsupportedOperationException();

        }
        if (classDataMap.containsKey(name)) {
            return classDataMap.get(name);
        }
        ClassData cd;
        try {
            cd = new ClassData(new ClassReader(name));
        } catch (IOException ex) {
            cd = new ClassData(Mill.getClassReader(name));
        }
        synchronized (classDataMap) {
            if (classDataMap.containsKey(name)) {
                return classDataMap.get(name);
            }
            classDataMap.put(name, cd);
        }
        return cd;
    }


    /**
     * Private method to construct the set of all Methods defined by a class.
     * Should only be called by the constructor.
     *
     * @param cr A classReader.
     * @param cp The ClassParameter corresponding to this classReader.
     * @return The set of MethodsParameter defined by the class.
     */
    static private Set<MethodParameter> getAllDirectMethodInternal(ClassReader cr, ClassParameter cp) {
        Set<MethodParameter> setMethodParameter = new HashSet<>();
        ClassNode cn = new ClassNode(Opcodes.ASM6);
        cr.accept(cn, ClassReader.SKIP_CODE);
        cn.methods.stream().forEach(mn ->
                setMethodParameter.add(new MethodParameter(cp, mn.access, mn.name, mn.desc, mn.signature, mn.exceptions.toArray(new String[0]))));
        return setMethodParameter;
    }

    /**
     * Given the name of a class, return the list of names of the classes ext
     * ended by this class. Achieves that by attempting to read the class
     * specified by the name. Might throw a runtime exception.
     *
     * @param owner The internal name of Class
     * @return The list of internal names of classes extended by this class.
     */
    static private Set<ClassData> getSuperClassesInternal(String owner) throws NoSuchClassException {
        if (owner.startsWith("[")) {
            return Collections.singleton(getClassData("java/lang/Object"));
        }
        return getSuperClassesInternal(Mill.getClassReader(owner));
    }

    /**
     * Return the list of classes extended by the class read by a ClassReader.
     *
     * @param cr A classReader
     * @return The list of classes extended by the class read by the
     * classReader, with the first element being "java/lang/Object", and the
     * last one being the direct superclass of the class read by the
     * classreader.
     */
    static private Set<ClassData> getSuperClassesInternal(ClassReader cr) throws NoSuchClassException {
        String s = cr.getSuperName();
        if (s != null) {
            Set<ClassData> listString = getSuperClassesInternal(cr.getSuperName());
            listString.add(getClassData(cr.getSuperName()));
            return listString;
        } else {
            return new HashSet<>();
        }
    }

    static private Set<ClassData> getInterfacesInternal(ClassReader cr) {
        return getInterfacesInternal(cr, new HashSet<>());
    }

    /**
     * Return the set of interfaces implemented by a class read by a
     * classReader.
     *
     * @param cr A classReader reading a classfile for clazz.
     * @return The set of interfaces implemented by clazz.
     */
    static private Set<ClassData> getInterfacesInternal(ClassReader cr, Set<ClassData> setString) throws NoSuchClassException {
        Set<ClassData> interfaces = Arrays.stream(cr.getInterfaces()).map(e -> getClassData(e)).collect(Collectors.toSet());
        interfaces.stream().filter(o -> !setString.contains(o)).peek(setString::add).forEach(o -> setString.addAll(getInterfacesInternal(o.getClassReader(), setString)));
        for (ClassData superClass : getSuperClassesInternal(cr)) {
            setString.addAll(getInterfacesInternal(superClass.getClassReader(), setString));
        }
        return setString;
    }

    static private Set<FieldParameter> getAllDirectField(ClassReader cr, ClassParameter cp) throws NoSuchClassException {
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE);
        return cn.fields.stream().map(e -> new FieldParameter(cp, e.access, e.name, e.desc, e.signature)).collect(Collectors.toSet());
    }

    /**
     * Given the internal name of a class, returns the name of the package.
     *
     * @param className The internal name of a class.
     * @return The internal name of the package.
     */
    static String getPackageInternal(String className) {
        return new File(className).getParent();
    }


    static public class ClassData implements Comparable<ClassData> {


        private ClassData(ClassReader cr) {
            this.cr = new WeakReference<>(cr);
            this.cp = ClassParameter.getClassParameter(cr);
            this.superClass = Optional.ofNullable((cr.getSuperName() == null) ? null : getClassData(cr.getSuperName()));
            if (Arrays.stream(cr.getInterfaces()).anyMatch(e->e.equals("xyz/acygn/mokapot/markers/ComeFromRetroLambda"))){
                String nameUpperClass = LambdasTransformation.getClassLambdaIsComingFrom(cr.getClassName()).getSecond();
                this.upperClass = Optional.of(ClassDataBase.getClassData(nameUpperClass));
            }
            else {
                try {
                    this.upperClass = Optional.ofNullable(
                            cr.getClassName().contains("$") ?
                                    getClassData(cr.getClassName().substring(0, cr.getClassName().lastIndexOf("$")))
                                    : null);
                }
                catch(Throwable t){
                    this.upperClass = Optional.ofNullable(null);
                }
            }
            this.listDirectInterfaces = Collections.unmodifiableCollection(getInterfacesInternal(cr));
            this.listDirectMethods = Collections.unmodifiableSet(getAllDirectMethodInternal(cr, cp));
            this.listDirectFields =
//                    (upperClass.isPresent()? Collections.unmodifiableSet(
//                    Stream.concat(getAllDirectField(cr, cp).stream(),
//                            upperClass.get().getDirectFields().stream()).collect(Collectors.toSet()))
                    Collections.unmodifiableSet(getAllDirectField(cr, cp));
            //We construct this two only if necessary.
            if (constructThoroughDatabase) {
                this.listMethods = getAllMethods();
                this.listField = getAllField();
            } else {
                this.listMethods = null;
                this.listField = null;
            }
        }


        private ClassReader getClassReader() {
            ClassReader cr = this.cr.get();
            if (cr != null) {
                return cr;
            } else {
                try {
                    return new ClassReader(cp.className);
                } catch (IOException ex) {
                    throw new NoSuchClassException(cp.className);
                }
            }
        }


        /**
         * Two classData are equal if they are the data of the same ClassParameter;
         *
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            return (o == null) ? false : ((o instanceof ClassData) ? ((ClassData) o).cp.equals(this.cp) : false);
        }

        @Override
        public int hashCode() {
            return cp.hashCode();
        }

        /**
         * We don't want to necessary hold the ClassReader, but still hols a weak reference to it for quicker access.
         */
        WeakReference<ClassReader> cr;

        /**
         * The class Parameter.
         */
        final ClassParameter cp;


        /**
         * A pointer to the ClassData of the superClass, if any.
         */
        private final Optional<ClassData> superClass;


        /**
         * A set of ClassData corresponding to the direct interfaces.
         */
        private final Collection<ClassData> listDirectInterfaces;


        /**
         * The direct fields are those that are declared within this class. Can notably hide one coming from a subclass.
         * If this class is a innerClass, also contains those coming from the upperClass.
         */
        final private Collection<FieldParameter> listDirectFields;

        /**
         * Direct methods are those that are implemented by the class, and not by any of the subclass.
         */
        final private Set<MethodParameter> listDirectMethods;

        /**
         * The list of methods that are callable on a object of this type.
         */
        private Set<MethodParameter> listMethods;


        /**
         * The list of fields that an object of this type carry. Contains also the hidden fields !
         * These are present since they can be accessed from an object of this type, by "forgetting" its type.
         */
        private Collection<FieldParameter> listField;

        /**
         * If this Class is an inner Class, we maintain a pointer to its upperClass.
         */
        private Optional<ClassData> upperClass;

        Collection<FieldParameter> getDirectFields() {
            return listDirectFields;
        }


        Collection<MethodParameter> getDirectMethods() {
            return listDirectMethods;
        }


        /*
        x < y if x.compareTo(y) < 0;
        Here x < y if x is a superClass of y.
         */
        @Override
        public int compareTo(ClassData o) {
            int i = o.isSubclass(this);
            if (i >= 0) {
                return i;
            }
            i = this.isSubclass(o);
            if (i >= 0) {
                return -i;
            }
            return 0;
        }


        /**
         * Return wether o is a subclass of this.
         * The number corresponds to the number of encapsulations.
         * We return -1 if o is not a subclass.
         *
         * @param o
         * @return
         */
        public int isSubclass(ClassDataBase.ClassData o) {
            int i = 0;
            if (o.equals(this)) {
                return 0;
            }
            ClassDataBase.ClassData cd = o;
            while (cd.getSuperClass().isPresent()) {
                cd = cd.getSuperClass().get();
                i += 1;
                if (cd.equals(this)) {
                    return i;
                }
            }
            return -1;
        }


        private Set<ClassDataBase.ClassData> listInheritedFrom = new HashSet<>();

        public boolean isAssignableFrom(ClassDataBase.ClassData cd){
            return getlistInheritedFrom().contains(cd);
        }

        private Set<ClassData> getlistInheritedFrom(){
            synchronized (listInheritedFrom) {
                if (cp.className.equals("java/lang/Object") || cp.isInterface() && this.listDirectInterfaces.isEmpty()
                        || !listInheritedFrom.isEmpty()) {
                    return listInheritedFrom;
                }
                listInheritedFrom.addAll(listDirectInterfaces);
                if (upperClass.isPresent()) {
                    listInheritedFrom.add(superClass.get());
                }
                listDirectInterfaces.stream().forEach(e -> listInheritedFrom.addAll(e.getlistInheritedFrom()));
                if (upperClass.isPresent()) {
                    listInheritedFrom.addAll(upperClass.get().getlistInheritedFrom());
                }
            }
            return listInheritedFrom;
        }




        static Predicate<Pair<Boolean, Integer>> isNotInherited = pair -> {
            Boolean isSuperclassSamePackage = pair.key;
            Integer access = pair.value;
            if (isSuperclassSamePackage) {
                // If the superClass is from the same package, we inherit all but the private methods / fields.
                return ((access.intValue() & Opcodes.ACC_PRIVATE) != 0);
            } else {
                // If the superClass is from another package, we only inherit the protected and public Method / fields.
                return (((access.intValue() & Opcodes.ACC_PROTECTED) != 0)
                        || ((access.intValue() & Opcodes.ACC_PUBLIC) != 0));

            }
        };


        Optional<ClassData> getSuperClass() {
            return superClass;
        }


        boolean isSuperclassSamePackage() {
            try {
                return superClass.get().getPackage().
                        equals(getPackage());
            } catch (NullPointerException ex) {
                //Surely it means the class do not have a superClass. In that case,
                // we return true to avoid throwing an exception.
                return true;
            }
        }


        String getPackage() {
            return getPackageInternal(cp.className);
        }


        private void addMethodAndUpdateOverriding(Set<MethodParameter> listMethodsUpper, Set<MethodParameter> listMethodClass){
            // It might be that the superClass implements a method coming from the interface.
            Set<MethodParameter> overridenMethods = listMethodsUpper.stream().filter(e ->
                    {
                        Set<MethodParameter> compatibleMethods =
                                listMethodClass.stream().filter(o -> o.areOverrideCompatible(e)).
                                        collect(Collectors.toSet());
                        if (!compatibleMethods.isEmpty()) {
                            for (MethodParameter mp : compatibleMethods) {
                                addCompatible(mp, e);
                            }
                            return true;
                        }
                        return false;
                    }
            ).collect(Collectors.toSet());
            listMethodsUpper.removeAll(overridenMethods);
            listMethodsUpper.addAll(listMethodClass);
        }

        Set<MethodParameter> getAllMethods() {
            if (listMethods == null) {
                Set<MethodParameter> listMethodsTemp = new HashSet<>();
                // Now we add all the methods from the interfaces;
                for (ClassData cd : listDirectInterfaces) {
                    listMethodsTemp.addAll(cd.getAllMethods());
                }
                if (superClass.isPresent()) {
                    HashSet<MethodParameter> methodSuperClass = new HashSet<>();
                    methodSuperClass.addAll(superClass.get().getAllMethods());
//                    methodSuperClass.removeIf(e -> isNotInherited.test(
//                            new Pair<Boolean, Integer>(isSuperclassSamePackage(), e.methodAccess)));
                    // It might be that the superClass implements a method coming from the interface.
                    addMethodAndUpdateOverriding(listMethodsTemp, methodSuperClass);
                }
                //Now we remove any methods if it is overriden by a method directly implemented by the class.
                addMethodAndUpdateOverriding(listMethodsTemp, listDirectMethods);
                listMethods = Collections.unmodifiableSet(listMethodsTemp);
            }
            return listMethods;
        }


        Collection<FieldParameter> getAllField() {
            if (listField == null) {
                Set<FieldParameter> listTempField = new HashSet<>();
                listDirectInterfaces.stream().forEach(e -> listTempField.addAll(e.getAllField()));
                if (superClass.isPresent()) {
                    superClass.get().getAllField().stream()
                            //                      .filter(e-> !isNotInherited.test(new Pair<Boolean, Integer>(isSuperclassSamePackage(), e.access)))
                            .forEach(listTempField::add);
                }
                listTempField.addAll(getDirectFields());
                listField = Collections.unmodifiableSet(listTempField);
            }
            return listField;
        }
    }


}





