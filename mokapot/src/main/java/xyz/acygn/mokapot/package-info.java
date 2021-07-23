/**
 * A library for transparently making Java programs distributed.
 * <p>
 * This library handles all the background operations needed to allow a program
 * to run on more than one machine, aiming to keep the behaviour of the program
 * as similar as possible to if it were running on only a single machine. The
 * main difference to running without this library is that an object that exists
 * on one machine can be stored (by reference) in a variable or field that
 * exists on another machine. The references are completely transparent, in the
 * sense that calling a methods on a reference will cause that method to run on
 * that object.
 * <p>
 * In the most general case, a method on an object will always run on the same
 * machine as the object itself. However, this is an implementation detail (due
 * to the fact that not all objects can sensibly be moved between machines), and
 * in cases where it's known that running the method on a copy of the object
 * would be safe, the method may well run in the same place it's invoked (using
 * a deep or shallow copy of the object to run it on, whichever would behave the
 * same way as a call on the object itself). This mechanism can be enhanced via
 * use of the <code>Copiable</code> interface, which allows the user to specify
 * classes which are safe to copy this way (even if they might appear to be
 * unsafe to copy to a computer).
 * <p>
 * To use this library, create, configure and start a
 * <code>DistributedCommunicator</code>; that will allow distributed programs to
 * run on the current Java virtual machine. If you only want to allow programs
 * from elsewhere to run on this machine (rather than starting a program on your
 * own), you can use <code>DistributedServer</code> (a command-line program that
 * simply wraps <code>DistributedCommunicator</code>) for the purpose.
 * Typically, a distributed computation will be started on one computer (that
 * creates a <code>DistributedCommunicator</code> of its own), and allowed to
 * continue onto other computers that are just running a
 * <code>DistributedServer</code>. Note that this mechanism is subject to change
 * (because the current arrangement is hard to secure).
 * <p>
 * Even with multiple machines involved, the entire program will run on a single
 * machine unless some of the computation is moved to another machine. This can
 * be done using <code>DistributedCommunicator#runRemotely</code>. For optimum
 * performance, try to avoid the moved parts of the communication referencing
 * non-<code>Copiable</code> (or effectively copiable, e.g. due to being
 * immutable) objects that exist on the original machine. Hopefully, this
 * process will some day be automated, but for the time being you will need to
 * manually specify what to run where.
 *
 * @see xyz.acygn.mokapot.DistributedServer
 * @see xyz.acygn.mokapot.DistributedCommunicator
 * @see xyz.acygn.mokapot.DistributedCommunicator#runRemotely(
 * xyz.acygn.mokapot.CopiableSupplier, xyz.acygn.mokapot.CommunicationAddress)
 */
package xyz.acygn.mokapot;
