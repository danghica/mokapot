/**
 * Interfaces and classes used by generated code.
 * <p>
 * When code generation is required (either ahead-of-time or at runtime), the
 * resulting code needs to be able to communicate with existing code, which
 * means implementing standard interfaces so that the two pieces of code can
 * communicate with each other.
 * <p>
 * Additionally, some of the generated code has fairly large "fixed sections".
 * Those are most easily implemented as methods, and this class contains
 * implementations of the methods in question (either in concrete classes or as
 * default methods on interfaces).
 * <p>
 * The generated code is used for "standin classes". Standin objects behave like
 * objects of a particular class, but can intercept method calls and forward
 * them elsewhere. Unlike a proxy class, they don't <i>have</i> to intercept
 * method calls; instead, they can allow them to be executed normally, by a
 * "referent object" (which might be the standin itself).
 * <p>
 * The classes in this package are <code>public</code> for technical reasons;
 * generated code might need to extend the classes and/or implement the
 * interfaces from arbitrary packages. However, they are not intended to be used
 * or implemented by end users, and should effectively be treated as being
 * package-private.
 */
package xyz.acygn.mokapot.skeletons;
