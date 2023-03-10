package xyz.acygn.millr.mokapotsemantics;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A class implements the getters interface if, for all fields that it implements,
 * it also provides a getter.
 *
 * The getter should be of the following form.
 *  @code{private typeOfTheField nameOfTheField_get(){
 *      return nameOfTheField;
 *  }
 *
 * Ideally, runtime verifications should be added through the use of the pluggable module like
 * theCheckerFramework.
 *
 * Atm, check can be done at compile time using the checkerGetters.checkGetters(Object o) method.
 *
 */


@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Getters {
}
