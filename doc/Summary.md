Summary of Mokapot
==================

*Mokapot* is a library for allowing Java programs to run across
multiple Java Virtual Machines (which could be situated on different
physical computers).  This allows its users to easily run their
programs in a distributed way.

This document is a short summary of how *Mokapot* is used, and how it
works internally.

Communicator
------------

Each virtual machine involved in the distributed computation has a
*communicator*.  Its purpose is to store the state used in the
computation, and to receive messages from other virtual machines.  A
running communicator will process any message it receives from another
machine that's part of the computation.  (Security is handled using
public key cryptography to sign and encrypt messages; there's a
keypair for the communication as a whole, with each communicator's key
signed using the communication's private key, and checked at runtime
against the communication's public key.)

To start a *Mokapot* computation, all JVMs involved in the computation
will be given a running communicator.  A computation will start on one
of the JVMs, but can create objects on others using methods of the
`DistributedCommunicator` class.  For example:

    DistributedCommunicator communicator
        = DistributedCommunicator("client.p12", password);
    communicator.start();
    CommunicationAddress serverAddress
        = communicator.lookupAddress(hostname, port);
    List<String> remoteList = DistributedCommunicator.getCommunicator()
            .runRemotely(ArrayList::new, serverAddress);

The "server" is just a JVM running a *Mokapot* communicator and
nothing else.  This program is the start of a computation, and works
by creating a communicator (specifying the key material), retrieving
the server's public key and connection information (`lookupAddress`),
and then creating an `ArrayList` on the server (by using `runRemotely`
on its constructor).  The result `remoteList` will be a standin for an
`ArrayList` (see the section on remote objects below).

Global threads
--------------

*Mokapot*'s threading model aims to match Java's; a program should
have the same semantics as if it were run on a single JVM.

The way this works is via creating *global threads* that represent
`Thread`s in the original program.  While a global thread is running
on a single JVM, it's just a normal Java thread.  However, if a thread
crosses multiple JVMs (i.e. a method running on one JVM makes a method
call and the newly called method runs on a different JVM), a *thread
projection* is created on each JVM involved; this is a Java thread
which contains only those parts of the call stack that run on the
actual JVM.

The "gaps" in the call stack (which represent methods running on a
different JVM) are filled by running *Mokapot*'s internal code;
normally, this is simply waiting for the thread to return to that JVM,
but the thread projection is also used to handle the decoding of
messages that describe method calls and returns on that global thread.
This means that the communicator merely needs to read the address on a
message, and forward it to the appropriate thread projection to
deserialise (which can be time-consuming); this means the communicator
can handle multiple messages in parallel.  If the global thread has no
projection onto a JVM and a message is sent to it, the communicator
will create one (and its first job will be to decode the message).

The actual calls are accomplished using message passing (over
TCP+TLS).  Messages are delay-insensitive (we make no assumptions
about what order they will arrive in), so a method call is a message,
and a method return is a separate message.

Note that this system means that no extra syntax is needed to specify
how threading/parallelism works; to create a new global thread, you
just create a new `Thread`.  It also means that the user msut create
threads manually if they wish to benefit from parallelism.

Remoting objects
----------------

Distribution in *Mokapot* is done at the object level; an object can
be referenced from multiple JVMs at once.  There are two basic
implementations used for this within *Mokapot*.

Some objects can be safely replaced by a copy.  *Mokapot* calls these
`Copiable` objects; examples include `String` and `Class`.  These
objects are handled by copying them to every JVM that needs them,
i.e. if a method call uses such an object as a parameter or return
value, the object will be serialised and sent in the same message as
the call itself.  The user can mark an object as `implements Copiable`
manually; *Mokapot* will also detect copiable objects via static
analysis (basically, objects that are both immutable and
constructable).  Copiable objects don't have a "home" JVM; they exist
equally on every JVM that needs them.

For other objects, *Mokapot* uses the concept of a *standin*.  If an
object needs to be referenced from multiple JVMs, each JVM represents
that object using a standin.  If the object was created "behind
*Mokapot*'s back", i.e. by a regular Java constructor, the object may
need to act as its own standin (with reduced functionality).
Otherwise, the standin is an object of a class created by *Mokapot*
(either ahead-of-time or just-in-time, using code generation if
possible or reflection/proxying if necessary).  The standin is
designed to be as similar to the original object as possible (in terms
of methods it supports, interfaces it implements, etc.), but its
method implementations can be swapped between two possibilities at
runtime:

1. The standin's methods are simply wrappers for methods of an object
   on the JVM (which could be the standin itself if the standin's
   class inherits from the class specified in the user's program).
   This is used if the object's data is currently stored on the JVM
   on which the call was made.

2. The standin's methods represent the method call as the method
   code + parameters, forwarding these to a *location manager*.  A
   location manager's job is to determine which JVM holds the object's
   data, then send a message to the JVM in question to ask the
   corresponding location manager there to run the method there.  This
   is used if the object's data is stored elsewhere (or the object is
   currently being migrated).

A `NonCopiable` object's data is only stored on one JVM, and all
methods of the object will be run on that JVM.  Method calls on the
object from other JVMs will thus be transparently forwarded via the
location manager and run on the same JVM as the object.  This means
that there are no coherence issues even with stateful operations; as
all the operations on a particular object run in the same place,
they'll always have the latest data for that object's fields.

Note that this construction comes with a major limitation: it only
handles method calls on the object.  Anything that isn't a method call
(e.g. a field access, a `synchronised(object)` call, an array
dereference, etc.) won't be forwarded (as the standin can't override
it) and thus will have incorrect behaviour unless it's run on the same
JVM as the object itself.  At present, it's the user's responsibility
to avoid this (e.g. by using getter and setter methods, which always
run in the same place as the object, for field access).  A program
called *Millr* is under development to automatically replace
problematic code with less problematic code with the same semantics.

Standin classes also implement serialisation/deserialisation code
specialised for the object in question, as an optimisation (meaning
that the code can be much faster, as it can be specialised for the
object ahead of time).

Garbage collection
------------------

In general, *Mokapot* cleans up garbage the same way that Java does;
if something's no longer in use it gets garbage collected.  The main
things that need garbage collection are thread projections, standins,
and location managers.

Thread projections are managed by ensuring that a thread projection
only exists while the global thread in question is actually running on
the relevant JVM; when all methods running on that JVM on that thread
have returned, the thread projection ends.  This can sometimes cause a
thread to be repeatedly created and destroyed if a method of an object
on one JVM runs a method of an object on another JVM in a loop.  To
avoid the performance cost of this, thread pools are used so that the
same thread can be repeatedly recycled while *acting* like a new
thread.

Standins need to be kept alive as long as they're referenced from the
same JVM or from another.  For references within a JVM, Java's normal
garbage collection routine is used (as the references are normal Java
references to the standin, they'll prevent its deallocation).
Cross-JVM references are handled via the location managers; a standin
keeps its location manager alive, and each location manager keeps the
location manager on the same system as the object alive.  In order to
avoid needing to wait for a network round-trip on every message call,
a count is kept of the number of messages in each direction that
mention an object; when a location manager is deallocated, it tells
the location manager on the original system how many messages it
thinks it's sent and received.  If there's a mismatch between the
counts on the two systems, it means that some messages that mention
the object are still in transit, and thus the referenced object
shouldn't be freed yet (as standins for the object will need to be
recreated when the messages arrive).

A location manager on the same system as an object does *not*
necessarily hold the object alive, if it can be migrated.  Thus, if an
object has no local references (only remote references), it can be
"freed"; however, *Mokapot* will intercept the deallocation and keep a
record of all the object's state.  The object will then be recreated
on the next machine to make a method call on it.  This is known as an
*automatic migration*.  This not only helps to make computations
quicker, but also ensures that reference loops will not prevent
garbage collection even if they cross from one system to another.
