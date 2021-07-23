/**
 * Generation routines to produce generated standin implementations. These are
 * shared between the runtime and compiler, and used to derive facts about the
 * code or to generate code at runtime. It can run in either a just-in-time
 * fashion (deriving facts about Java classes, and generating code to deal with
 * them lazily); or in an ahead-of-time fashion (doing the analysis once at
 * compile time rather than during every run of the program).
 * <p>
 * This package contains only the "compiler" parts of the code (which generate
 * the code). Classes implemented by the generated code are in the package
 * <code>xyz.acygn.mokapot.skeletons</code>.
 */
package xyz.acygn.millr.generation;
