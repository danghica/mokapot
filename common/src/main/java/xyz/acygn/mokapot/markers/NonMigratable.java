package xyz.acygn.mokapot.markers;

/**
 * A marker interface indicating that no objects of the given class should be
 * migrated. This can be used semantically (to suppress automatic migration in
 * situations in which it would change the semantics of the program), or as an
 * optimisation to prevent migration-related state being initialised for objects
 * that it is known will never need to be migrated.
 * <p>
 * Note that non-migratable objects are inherently non-copiable (a copiable
 * object will be copied to every system on which it is used, a form of
 * migration).
 *
 * @author Alex Smith
 */
public interface NonMigratable extends NonCopiable {
}
