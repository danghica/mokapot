/**
 * Classes and methods for working with the serialised format used to represent
 * data sent by this library over the network. This includes things such as how
 * to represent a class or method as a sequence of bytes.
 * <p>
 * The format manipulated by this package consists of two parts: a "copiable"
 * part which consists of things that can safely be replaced by a copy, and
 * which is represented as a sequence of bytes; and a "noncopiable" part which
 * consists of things that must be kept as references to the original objects
 * involved and cannot be replaced by copies. (In other words, this is a format
 * which can represent shallow serialisations that produce shallow copies when
 * deserialised, not just regular serialisations that produce deep copies when
 * deserialised.) Obviously, references to objects cannot be sent over the
 * network directly, but from the point of view of this package, the exact
 * mechanism used to achieve this is abstracted away and the objects and methods
 * in this package are used as if the serialisation stored the reference
 * directly.
 * <p>
 * The most important class of this package is <code>ObjectDescription</code>,
 * which is a sequence of bytes and separate sequence of references to objects.
 * An <code>ObjectDescription</code> actually does store the references directly
 * as Java references. It implements two interfaces
 * <code>ReadableDescription</code> and <code>DescriptionOutput</code>, which
 * represent the readable and writable parts of <code>ObjectDescription</code>'s
 * API respectively; unlike <code>ObjectDescription</code>, which stores the
 * noncopiable references directly, <code>ReadableDescription</code> may
 * generate the references lazily as they are read, and
 * <code>DescriptionOutput</code> may likewise do some processing on the
 * references rather than merely storing them.
 * <p>
 * This is separated into a package of its own so that it can be used even if
 * the main distributed computation library isn't currently in use, e.g. if
 * generating code that reads or writes the format as part of a batch process.
 * (It's envisaged that some users might want to generate specialised, fast
 * serialisation and deserialisation code for certain classes ahead of time.)
 *
 * @see xyz.acygn.mokapot.wireformat.ObjectDescription
 */
package xyz.acygn.mokapot.wireformat;
