
package xyz.acygn.millr;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author thomasc
 */
public class ReflectionUtilsMillr {
    
    
    public static final Field getField(Class clazz, String fieldName) throws NoSuchFieldException{
        Optional<Field> maybeField = Arrays.stream(getAllFields(clazz)).filter(e->e.getName().equals(fieldName)).findAny();
        if (maybeField.isPresent()){
            return maybeField.get();
        }
        else{
            throw new NoSuchFieldException("No such field" + fieldName + "in class" +((clazz==null)? "null" : clazz.getName()));
        }
    }
    
        /**
     * Return all the fields present in an object of an input class.
     * @param clazz A class
     * @return All the fields present for an object of this class.
     */
    private static final Field[] getAllFields(Class clazz) {
            Field[] fields = getAllFields(getAllSuperClass(clazz));
            return fields;
    }
    
    /**
     * Given a collection of class, return the set of fields implemented by each of this class.
     * @param col A collection of class
     * @return An array of all fields implemented by each these classes.
     */
    private static final Field[] getAllFields(Collection<Class> col) {
        List<Field> fields = new ArrayList<>();
        for (Class clazz : col) {
            Arrays.asList(clazz.getDeclaredFields()).stream().forEach(f -> fields.add(f));
        }
        return fields.toArray(new Field[fields.size()]);
    }
    
        /**
     * Return all the classes that extends a class clazz, together with the class clazz.
     * @param clazz class clazz
     * @return All the classes that clazz extends, together with clazz itself.
     */
    private static final List<Class> getAllSuperClass(Class clazz) {
        List<Class> inheritedClasses = new ArrayList<>();
        inheritedClasses.add(clazz);
        while (clazz.getSuperclass() != null) {
            inheritedClasses.add(clazz.getSuperclass());
            clazz = clazz.getSuperclass();
        }
        return inheritedClasses;
    }
}
