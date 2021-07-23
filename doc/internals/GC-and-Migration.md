Garbage Collection and Migration in Mokapot
===========================================

This document discusses the design of *garbage collection* and
*migration* in Mokapot.  Garbage collection is the mechanism via which
objects are deallocated when they're no longer in use.  Migration is
the mechanism via which objects are moved to a different JVM,
presumably because they're more useful there than on the original JVM.
These are different systems, but related (in particular, migration has
a large effect on garbage collection because it makes it harder to
determine whether an object is in use).


Copiable objects
----------------

Garbage collection of copiable objects is very simple: a copy of them
is made on each JVM where the object is required, and each copy is
garbage-collected individually.  Thus, if the object is no longer in
use on any JVM, it'll be garbage-collected on every JVM when the
individual Java objects representing the copies are.

Copiable objects are not "migrated" in the sense that referencing them
from another system adds a new (mostly independent) copy on that
system.  As such, there's nothing to migrate, as copies will always be
created when required and there's no need to delete the original
(unless/until it becomes unreferenced).

In order for `==` to work correctly, an implicitly Copiable object is
stored along with an ID number, so that if it's copied to another
system and then back to the original system without being deallocated
in the meantime, the existing copy can be used.  (This is currently
unimplemented due to concerns about the performance cost versus the
benefit from it.)

The rest of this article only discusses noncopiable objects, which are
more interesting as they must be stored in only one place at a time.


Standins, standin storages, referents, location managers
--------------------------------------------------------

There are four main parts to a noncopiable object: the *standin*, the
*standin storage*, the *referent*, and the *location manager*.  Some
of these may be the same Java object (in particular, the standin is
its own referent whenever possible, as that increases the chance that
`this` will have the expected effect).  (This article uses "Java
object" for an object as seen by the JVM, and just "object" for an
object as seen by the programmer.)

The referent contains the actual data of the object.  Methods that run
on the object are implemented by (possibly) going via some wrappers,
and then eventually calling the method on the referent, which actually
implements the method (using `invokespecial` to select the object's
expected class; that way, a standin that's also a referent can
override the method to act as the wrapper if called via
`invokevirtual` but actually run the method if called via
`invokespecial`).  There is only ever one referent for any given
object, and we refer to the JVM containing the referent as "the JVM
where the object is" or "the JVM where the object's data is stored",
or more simply "the object's location".

The standin is what references to the object actually point at, and
might or might not be the same Java object as the referent.  There's a
standin on each system that references the object (so that the
references have something to reference); obviously, this means that
some instances of the standin will necssarily be different Java
objects from the referent itself (which is not on the same system).
When a method is called on the standin, it inspects its standin
storage to determine what to do.

The standin storage is a separate object with multiple purposes:

  * It specifies what happens when a method call is made on the
    standin.  This might either be to ask the standin to make the call
    directly to the referent (only possible if the referent is on the
    local JVM), or to make the method call indirectly via the location
    manager.
  * It works around visibility issues, making it possible to produce a
    reference to the location manager if given a reference to the
    standin.
  * It acts as a hook to the garbage collector, allowing the fields of
    the referent to be saved in the location manager when the standin
    is deallocated.  (If the referent and standin are the same object,
    this is a fairly awkward operation, given that we're trying to
    read fields of a deallocated object, something that Java doesn't
    like.  The current implementation makes use of a deprecated
    garbage collector hook, and thus may potentially break in the
    future.  There's a potential alternative implementation involving
    polling, but it may be less efficient.)

There's also a location manager for the object on every system which
references the object (exception: if only one system references the
object and the object is on that system, the location manager is
unnecessary and will be deallocated if possible); its job is to track
the object's location and garbage collection information.  The
location manager that's in the same place as the object will always
know that the object is local.  The other location managers will know
that the object is remote, but might not know its exact location;
rather, each remote location manager has a reference to another, in
such a way that following these references will always eventually lead
to the object's location.

The lack of cycles among location managers is enforced using a
"timestamp" system; location managers store information of the form
"the object was in place *A* at time *T*".  There's an invariant that
works as follows: if we have a location manager that points to its
counterpart on another system:

  * In place *A*: "the object was in place *B* at time *T*"
  * In place *B*: "the object was in place *C* at time *U*"

then *U* must be newer than *T* (i.e. following the chain of
timestamps always takes you to more recent timestamps until eventually
you find the system where the object currently is, which acts as the
base case of the recursion).  It's clear that it's impossible to have
a cycle, so this method is guaranteed to make it possible to locate
any object.

There's another invariant: the location information in a location
manager can only be updated by increasing the time (i.e. moving to a
newer idea of the location).  This second invariant ensures that
changing location information on one system cannot violate the first
invariant on a *different* system, and is also needed for correctness
of the garbage collection algorithm.

It's possible to have a location manager without a standin (an
"offline" location manager, used to store the fields of an object
whose standin got deallocated; or a "remnant" location manager, used
to forward messages destined for an object that has since migrated).
It's also possible to have a standin without a location manager, which
is basically a normal Java object that happens to be a standin (and
will be given a trivial standin storage to fulfil its contract); this
is used to avoid having to locate and update existing references to
the object if it ever needs to be migrated to a different system.


Standin storage states
----------------------

In order to make garbage collection and migration work, a standin is
conceptually a state machine that can be taken through a few different
states.  These are implemented as different classes of
`StandinStorage` objects.

When an object is created, it has no location manager associated.
None of the rest of the standin machinery will be involved either,
unless the object is of class `Standin`, in which case it'll be given
a `TrivialStandinStorage` to specify that methods should be forwarded
to the referent directly.  (In this case, the standin will usually be
its own referent, although it'd be possible to create a standin with a
separate referent for the purpose of optimising cross-system
operations, if those seemed likely to be involved.)

When an object is sent to another system for the first time, a
location manager is created to handle the inbound method calls.  The
object is given a standin if it doesn't have one (in which case the
object will become the referent), and the standin storage is changed
to a `ManagedLocalStandinStorage` subclass.  These can only be used
with standins which have a referent (thus "local"), and will handle
the situation where the object becomes locally unused.  (There are two
such subclasses: one where the standin is its own referent, and one
where the two are separate objects.  The difference is that the former
needs a `finalize` method, but the latter can survive without one.)

Meanwhile, the system which received the object will also need a
standin created, using `ForwardingStandinStorage` as its storage (thus
sending methods via the location manager).

In order for migration to occur, we need to be confident that the
object being migrated isn't currently in use.  The migration process
thus has three parts: prepare, commit, conclude.  Preparing the
migration changes the standin storage unconditionally to
`ForwardingStandinStorage`, and also changes the location manager to
hold the standin alive; this means that the location manager can track
all the message calls through the standin that start after the
prepare.  Committing the migration moves the referent from one system
to another (as explained below) but does not change the storages.
Concluding the migration changes the standin state (to, back to)
`ManagedLocalStandinStorage` on the system that has the referent (and
the location manager stops holding the standin alive again).

One awkwardness here is for objects that were created outside the
distributed system; these have the issue that there are likely to be
some references directly to the *referent* (rather than to the
standin, like they should be).  In these cases, the location manager
holds the standin alive, the standin holds the referent alive, and
automatic migration will not work (nor will manual migration unless
we're willing to do operations that we can't prove are safe).

A summary of the various possible states:

 1. `TrivialStandinStorage`: no location manager exists; the standin
    storage is held alive only by the standin, and nothing but inbound
    references hold the standin alive.
 2. `SeparateReferentStandinStorage` (extends
    `ManagedLocalStandinStorage`): the standin itself is not held
    alive by anything; the location manager is held alive only by
    lifetime managers; the standin storage holds itself alive, and
    will take the location manager offline if the standin dies first.
    Note: this state is not currently implemented, with the base class
    `ManagedLocalStandinStorage` implementing this state and the next
    in a general way (using the algorithm in the next state).
 3. `CombinedReferentStandinStorage` (extends
    `ManagedLocalStandinStorage`): the standin and its storage are a
    unit (i.e. freed or not together), with nothing externally holding
    them alive (other than inbound references to the standin); the
    location manager is held alive only by lifetime managers; if the
    standin dies, the storage's finalizer will take the location
    manager offline (assuming it didn't die first).
 4. `ForwardingStandinStorage`, the referent isn't here: the standin
    storage holds the location manager alive; nothing holds the
    standin alive.  The standin just disappears when there are no more
    inbound references to it, without any cleanup necessary.
 5. `ForwardingStandinStorage`, during migration: the standin storage
    holds the location manager alive; the location manager holds the
    standin alive.


Migration
---------

Migration is the ability for objects to move from one JVM to another
at runtime (i.e. the object's location changes, or to put it another
way, the referent is removed from one system, serialised, and
recreated on another system).

Migration can occur in two ways, manual or automatic.  In manual
migration, the steps of migration occur when directed by the user.
Automatic migration goes through most of the same steps at moments
determined by activity in the program.

 1. Migration prepare: instruments method calls on the object to make
    sure we know when they're happening.  This only occurs in manual
    migration, and works by changing all the standin storages to
    `ForwardingStandinStorage`.  It can be undone via a migration
    conclude.
 2. Migration verify: waits until the referent is no longer in use.
    We have various ways of proving this.  In a manual migration, it's
    partially checked by the user, and partially checked by ensuring
    that no method calls that started since the prepare are still
    ongoing.  In an automatic migration, this is checked by the
    garbage collector; in particular, the referent needs to have no
    inbound strong references, and the verify is a consequence of the
    referent's deallocation.  (In cases where we know that the only
    inbound references to the referent are from the standin, this is
    accomplished using an extra reference to the referent directly,
    together with a phantom reference on the standin; that way, we can
    make use of phantom reachability without actually losing the
    data.)  A migration commit starts by blocking until the verify can
    occur.
 3. Offlining: a description of the referent is made, and stored in
    the location manager.  This happens immediately after (or as part
    of) the verify.  After this point, the local referent no longer
    exists; the standin might not exist either, and is switched into
    "no referent" mode if it's still referenced (this could happen
    during manual migration).  The migration commit is treated as
    having finished at this point unless there was a request to
    migrate to a specific system; everything else is automatic upon an
    attempt to make a method call to the object.
 4. Referent recreation: the description is undescribed into a
    referent on the target system (possibly being sent across the
    network in the progress, along with some GC weight; see below).
    That system's standin is changed into migration mode.  This is
    automatic upon an attempt to call a method on an offline location
    manager, and can be caused to happen "early" at user request.
 5. Migration conclude: changes the standin storage for the system
    which has the referent back into something more normal (i.e. a
    `ManagedLocalStandinStorage` subclass).  If the user requests a
    "just migrate anywhere" and then concludes the migration before
    we've found a good place to migrate the object, it's unclear what
    should happen: maybe pick an arbitrary lifetime manager and make
    the system in question deal with the object?


Garbage collector implementation
--------------------------------

For garbage collection within a single JVM, Java's built-in garbage
collector is used.  References to an object are made to the standin
(keeping the standin alive); the standin keeps the referent and the
standin storage alive.  If the standin is in forwarding mode (see
below), it holds the location manager alive.  Otherwise, there's no
reason on the local system to hold the location manager alive.

For garbage collection between JVMs, a different system is used.  The
location managers for a given object form a directed tree (which
eventually points at the location manager where the object is, as the
"bottom" of the tree).  We conceptually label each edge with a number,
the "garbage collection weight" or "GC weight"; each system keeps
track of the amount of outbound weight and inbound weight it has for
each object, and what objects the inbound weight is from.  (An
(inbound location, inbound weight) pair is known as a *lifetime
manager*, and such pairs are Java objects with functionality of their
own; they deallocate themselves when the inbound weight becomes zero,
and hold the location manager alive via a normal Java pointer.)  The
GC weight for any such edge must be positive at all times until it's
removed from the graph.

Of course, it's important to ensure that the various systems agree on
the GC weights.  As such, the weights are stored in multiple places;
an outbound weight is stored as a positive number, an inbound weight
as a negative number, and thus the weights over the system as a whole
should sum to zero.  There's an important invariant that the remote
location managers for an object must have a total weight (positive
outbound + negative inbound) that's a positive number; this means that
it's impossible for the weight on an object to become zero unless
there are no remote references to it anywhere, but it will become zero
once all the references to it are local.  That's all the information
the garbage collector needs to know when to deallocate it.  (This is
implemented automatically because the lifetime managers will hold an
object alive until it has no inbound weight.)

In order to avoid issues due to messages in transit that mention an
object, there's a "GC weight requirement" for sending a message;
messages that mention an object (e.g. because they contain a
serialisation of an object that referenced it) conceptually contain an
amount of GC weight bundled with the reference, listing an edge in the
location graph.  This weight, being part of the message, won't be in
either of the systems involved; thus, the referencing system will have
a slightly lower outbound weight than the referenced system does
inbound (with the difference being stored in the message).  This makes
it impossible for the object to be deallocated until the message has
arrived (making the weight on the recipient system more positive or
less negative), making the GC weight system entirely asynchronous.

In order to prevent systems running out of GC weight (and thus needing
to request more from the target of the reference), sending a message
mentioning a local object to a remote system provides a large amount
of GC weight, whereas going the other way round will only return a
small amount of GC weight.  (Of course, a system which no longer needs
to reference the object will return all its GC weight at once.)

Due to migration, it's possible that there will be messages going
around which mention nonexistent edges in the object reference graph
(as the graph has changed since the message was sent).  While the
message is in transit, this is no problem; the GC weights in the
system as a whole and in the message will sum to zero (nothing in the
migration process will change *that* invariant), so the object won't
get deallocated from the original system.  If the message is
"returning" GC weight (i.e. the weight in the message came from the
referencing system and will be given to the referenced system),
there's also no issue; when the edge in question was moved, the system
will have returned all the GC weight it had left, but the inbound
reference won't have been completely removed from the target system
due to some of it being in the message; there'll thus be a small
amount of remnant negative/inbound GC weight corresponding to the
weight stored in the message, which will be removed when the message
arrives.

The only complicated case is when a message is given GC weight from a
system, but it thinks the object is somewhere else.  In order to
handle this, the GC weight stored in a message is accompanied by a
description not only of the inbound and outbound systems, but also the
timestamp of the relationship between them.  There are two cases:

 1. If a system is given GC weight with a timestamp older than its
    current timestamped location, the GC weight is immediately sent
    back to the system it came from (via a message whose only purpose
    is to transmit GC weight).  The system will continue operating on
    the GC weight that it gained from the new location.
 2. If a system is given GC weight with a timestamp newer than its
    current timestamped location, its timestamped location for the
    object is updated to match that in the message, and all its
    previously existing GC weight is returned to the location it was
    taken from.  The system will in future use the GC weight that came
    with the message.  (Note that this update does not violate any
    invariants, as the system sending the weight must have had the
    object locally at the time; the timestamp on that system must
    therefore be equal to or newer than the timestamp in the message.)
