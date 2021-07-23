Using Mokapot
=============

Basic example (client)
----------------------

```java
public class Demo {
    public static void main(String[] args) {
        // Start a communicator on this JVM.
        char[] password = /* get the password in some secure way */;
        DistributedCommunicator communicator
            = DistributedCommunicator("client.p12", password);
        communicator.start();
        
        // Look up the address of the server's communicator.
        CommunicationAddress remoteAddress
            = communicator.lookupAddress(
                InetAddress.getLoopbackAddress(), 15238);
        
        // Create a list on the remote machine (remoteList will be a long reference).
        List<String> remoteList = DistributedCommunicator.getCommunicator()
                .runRemotely(() -> new ArrayList<>(), remoteAddress);
        
        // Add an element to the remote list.
        remoteList.add("Some string");
        
        /* Compute the size of the list. The computation will run on
           the remote machine.  Since remoteList is a long reference,
           method invocations will automatically be directed to the
           machine holding the actual object. */
        System.out.println(remoteList.size());
        
        // Shutdown our communicator once we're finished.
        DistributedCommunicator.getCommunicator().stopCommunication();
    }
}
```

Creating a server
-----------------

Mokapot allows a program to run on multiple Java virtual machines at
once. The JVM on which the program starts executing will be one of
them, but you'll also need to start extra JVMs that will also help run
the program. In most cases, these extra JVMs will be "mokapot
servers", i.e. JVMs which *only* respond to remote requests from other
JVMs to help them run a program cooperatively, and do no work of their
own.

The `DistributedServer` class (the default main class for
`mokapot.jar`) has a main method which takes command line arguments
such as the port number and host, e.g.

```
java -Djava.security.manager -Djava.security.policy=localhost-only.policy \
    -cp target/classes:objenesis.jar:javassist.jar:asm-all.jar \
    -jar mokapot.jar server.p12 15238 127.0.0.1
```

will start a `DistributedCommunicator` listening on `127.0.0.1:15238`.
(You'll need to specify the password to `client.p12` on standard
input; note that if you're typing it rather than redirecting it from a
file, it will echo.)  Note that the host you give here is your own IP,
i.e. the name via which remote systems will connect to the local
system (in this case, the use of localhost means that the "remote"
JVMs must be on the same physical computer, as otherwise connecting
via localhost would not work).

Other useful command-line options include `-d`, which causes the
server to produce debugging output explaining everything that it's
doing (which could be useful for debugging your application and/or
Mokapot itself, especially when trying to explain performance issues);
and `-w`, which causes the server to exit if it's had no communication
for 40 seconds (useful for preventing server leaks if something goes
wrong when running a Mokapot server in an automated way).

In order for `DistributedCommunicator`s on separate systems to
function properly, they need the same classpath.  This means that when
you run the `DistributedServer` above, you will need your project's
class files on the classpath (in the example above, `target/classes`
refers to your project's class files).

When you run your application, you should give the address of this
server, e.g. `127.0.0.1:15238`, as the address of a remote
`DistributedCommunicator` (see the demo code at the beginning of this
document).

Security: White-lists and cryptography
--------------------------------------

The whole point of Mokapot is to allow systems to run arbitrary code
on one another.  That's clearly a problem if the systems involved
don't trust one another, or if a connection is made from an
unauthorised source.  As such, each computation is based on a
"white-list" of acceptable communicators.

For ease of use, all the cryptographic material required for a single
machine is bundled together into a `.p12` file; there is one of these
for each machine.  The files are password-protected on disk, but
because they contain information that could allow anyone to run
arbitrary code on your system if it became disclosed, it's best not to
allow them to exist anywhere but the machine where they're supposed to
be.

The `.p12` file contains three pieces of information:

 1. A public key for the individual system, that lets it tell other
    systems how to recognise it; this is signed by the white-list's
    private key

 2. A private key for the individual system; this lets it prove which
    system it is to other systems that take part in the computation

 3. A public key for the white-list as a whole; it can use this to
    verify that other systems are on the white-list by verifying that
    their public keys have been signed by the white-list's private key
 
(The systems' keys are also used to encrypt data in transit.)

In order for one system to prove itself to another system, it'll send
its public key (allowing the other system to verify that it's on the
white-list by ensuring that the public key is signed by the
white-list's private key), and then prove it has access to the
corresponding private key (using the standard TLS client certificate
mechanism).

Note that the white-list's private key doesn't need to be present for
any of this.  (In fact, it's even possible to delete it once you're
done generating the white-list.)

### Simple white-list generation

The easiest way to generate a white-list is to use the provided
scripts in the `scripts/` directory.  (Currently these require OpenSSL
and bash, and thus typically need to be run on a Linux/UNIX system.
However, the resulting `.p12` files will work on any operating system.
Windows versions of these scripts may be provided in the future.)

The `makewhitelistcontroller.sh` file will use OpenSSL to create a
*white-list controller*, which is basically a directory that contains
a private key for the white-list, together with some infrastructure
for, e.g., ensuring that serial numbers are unique.  Once you have the
white-list controller, you can use `addwhitelistentry.sh` (telling it
the location of the controller) to create `.p12` files; all files that
are created from the same controller will recognise each other and
thus belong to the same white-list.  You can (preferably securely!)
delete the controller once the `.p12` files are generated.  Note that
the white-list controller never needs to be online, so you could do
this on an offline system if you're worried about people adding extra
entries to your white-list.

Due to the "white-list entries recognise each other" scheme of doing
things, entries cannot be removed from a white-list once they've been
added.  Each `.p12` file will, however, have an expiry date past which
it will cease to function (as will the white-list as a whole).  (This
can't be bypassed by changing the clocks on the computers involved
unless it's done at both ends of the connection, so as long as the
clocks on your own system are accurate, you can assume that expired
`.p12` files won't connect to it.)  You could also simply replace the
entire set of `.p12` files if you needed to change the set of machines
involved in a computation.

### In more depth

Mokapot's cryptography is entirely standard (never roll your own
cryptography!); key exchange and verification is done via TLS, and the
keys themselves are also in a standard format.  The `.p12` files are
PKCS#12 files, and they contain a single alias `endpointkey`, which
points to a chain of two X.509 certificates.  The first certificate is
the individual system's; the certificate itself contains the public
key (and a minimal amount of identifying information, such as a name
for the system and a unique serial number), and the private key is
stored alongside it.  The second certificate is that of the white-list
as a whole, containing its public key; there is no accompanying
private key.  This is a CA certificate, allowing it to sign other
certificates (such as the first certificate in the chain).

You don't need to use Mokapot's (fairly primitive) scripts to set up
the `.p12` files (which are basically just a thin wrapper around
OpenSSL, which does all the real work; that's entirely what you'd
expect from security-sensitive code).  You can set up your own files
as long as you keep to the same structure.  (For example, you might
want to ensure that the private keys are generated on a system and
never leave it; in this case, you could generate your own keypairs on
the system where the `.p12` file will run, but send only the public
half over to the white-list controller for signing.)  Note that it is
important, though, that the white-list's CA certificate is not used to
sign anything other than white-list entries.  In particular, don't use
a commercial certificate authority for the "white-list" certificate,
or else everything they've ever signed will be able to access your
system.

Security: Java security manager
-------------------------------

In addition to restricting who can connect to your system via use of
the white-list, it's also important to restrict what computations on
the system can do, reducing the potential impact if a malicious system
does manage to connect somehow.

As the documentation for `DistributedCommunicator` explains,

> **This class currently does no sandboxing of its own**, so ensure
> that the port is correctly secured via other means, e.g. installing
> a restrictive security manager, setting up appropriate firewalls,
> and using an endpoint implementation that rejects unauthorised
> connections. As a precaution, attempts to construct instances of the
> class will fail unless a security manager is installed.

(The "convenience constructors" for `DistributedCommunicator`, which
are the ones that most users will use and the ones recommended in this
documentation, use `SecureTCPCommunicationEndpoint` internally.
That's what uses the `.p12` files for authentication and authorisation
purposes.)

In order to fulfil the requirement to install a security manager, the
simplest method is to use command-line arguments to the Java virtual
machine:

```
-Djava.security.manager -Djava.security.policy=/path/to/my-security.policy
```

Your security policy should take into account the fact that anyone who
can access the port on which the server is running, and defeat any
security restrictions that exist on the endpoint, can run arbitrary
code on your JVM (and possibly accessing the computer as a whole,
etc.). Unfortunately, Mokapot also needs the ability to read `.class`
files from disk and to override Java's default visibility
restrictions, limiting the extent to which a security policy can help;
we strongly recommend using a restrictive firewall in order to further
reduce the chance of unauthorised connections. Here's an example of a
security policy:

**my-security.policy**
```
grant {
    permission java.net.SocketPermission "127.0.0.1", "connect,resolve,accept,listen";
    permission java.net.SocketPermission "localhost", "connect,resolve,accept,listen";
    permission java.lang.RuntimePermission "accessDeclaredMembers";
    permission java.lang.RuntimePermission "getProtectionDomain";
    permission java.util.PropertyPermission "java.runtime.version", "read";
    permission java.util.PropertyPermission "java.vm.info", "read";
    permission java.util.PropertyPermission "com.google.appengine.runtime.version", "read";
    permission java.lang.RuntimePermission "accessClassInPackage.sun.misc";
    permission java.lang.RuntimePermission "accessClassInPackage.sun.reflect";
    permission java.lang.RuntimePermission "modifyThreadGroup";
    permission java.lang.RuntimePermission "modifyThread";
    permission java.lang.RuntimePermission "reflectionFactoryAccess";
    permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
    permission java.io.FilePermission "<<ALL FILES>>", "read";
};
```

Limitations
-----------

Mokapot cannot safely (i.e. without potentially altering the semantics
of the program) be used directly on programs that do any of the
following things:

  * Pass an instance of a `final` class (or effectively `final`,
    e.g. `private` or package-private to a `java.*` package) as a
    method argument or return value, unless the class is `Copiable` or
    implicitly so;
  * Pass an array as a method argument or return value, unless the
    array is never mutated from that point onwards;
  * Pass a lambda as a method argument or return value, unless the
    lambda is both `Copiable` and `Serializable` (note that you can
    safely replace it with an inner class if you want it to remain
    noncopiable, although lambdas are normally implicitly copiable
    because any captured values must be immutable by definition);
  * Allow a method of one object to access a property of a *different
    object* directly (i.e. without using a getter/setter), unless both
    objects are `Copiable` or implicitly so, or neither object has
    ever been used as a method argument or return value; accessing
    fields of `this` is acceptable, accessing fields of another object
    of the same class or of an inner/outer class is not;
  * Compare objects for reference equality, or objects' actual classes
    for equality, if the objects have ever been used as method
    arguments or return values (you can use the methods in the
    `LengthIndependent` class in order to get around this);
  * Use a `synchronized` *block* on an object that's ever been a
    method argument or return value (a `synchronized` *method* is OK,
    and `synchronized` blocks should be rewritten to use
    `synchronized` methods instead)
  * Access stateful `static` properties or methods (except via
    `runRemotely` specifying the exact machine to run them on, and
    with the understanding that each machine will have its own state)
  * Mutate `enum` constants (note: very few programs ever want to do
    this)

"Method argument or return value" includes both the object that's
actually passed or returned, and (if the object is `Copiable` or
impliclty so) all its fields, recursively until a `NonCopiable` object
is reached.

Rewriting programs into a form that obeys these restrictions can be
done by hand, or by the help of a separate program (Millr).

Copiable and NonCopiable
------------------------

`Copiable` and `NonCopiable` are marker interfaces, which can be used
to tell Mokapot how to distribute objects of a particular type. If you
do not explicitly mark a type as `Copiable` or `NonCopiable`, Mokapot
will use static analysis to determine which type is most
appropriate. If the performance of your application depends on certain
computations being run remotely, then explicitly mark the relevant
types. You can use assertions to verify that an object was indeed
created remotely:

```java
assert DistributionUtils.isStoredRemotely(myObject);
```

### Copiable

If a class/interface is marked as `Copiable`, then the distributed
communication system will always maintain a copy of the object on each
virtual machine that needs it, rather than trying to maintain a
reference to it. This is usually suitable for small, immutable
objects. You should not mark types as `Copiable` if they hold a lot of
data, as the resulting network overhead could be detrimental to the
performance of the application. Mutable types, or types whose
reference identity is relevant (e.g. types whose `.equals` is
reference equality), should also not be marked as `Copiable`, as the
Javadoc explains:

> Copiable: A marker interface specifying that an object can be safely
> replaced with a copy/clone without changing the semantics of the
> program.

If an object is mutable but marked as `Copiable`, and changes are made
to one copy of the object while multiple JVMs have references to it,
then these changes will not be propagated to the other JVMs; this
changes the semantics of the program.

Note that copiability needs only go one level, e.g. replacing an
object with a copy means that all the fields will still contain the
same references as before (unless they are themselves `Copiable`).

### NonCopiable

This is the opposite of `Copiable`; it ensures the object's data never
exists on multiple systems at once .  For example, assume
`ComputationEngine` implements `NonCopiable`, then the following code
contains a successful assertion:

```java
ComputationEngine remoteComputationEngine 
    = DistributedCommunicator.getCommunicator.runRemotely(
          ComputationEngine::new, remoteAddress);
assert DistributionUtils.isStoredRemotely(remoteComputationEngine);
```

The actual `ComputationEngine` object resides on the remote machine.
Method invocations on the `remoteComputationEngine` object will be
forwarded to the object on the remote machine.

The `Copiable` or `NonCopiable` nature of each of the method's
arguments will determine whether or not Mokapot makes a copy of the
object and forwards it to the remote machine, or whether it uses a
long reference (i.e. a reference that crosses the boundary from one
machine to another).  You don't need to do anything special here; just
make sure user-defined types are marked as `Copiable` or `NonCopiable`
in cases where this is relevant.  Mokapot contains heuristics to
automatically treat types as `Copiable` or `NonCopiable` in cases
where this is not explicitly specified (e.g. `String` and `Class` are
treated as `Copiable`, `ArrayList` as `NonCopiable`).

### Walkthrough of Copiable and NonCopiable

To understand this in the context of an example, look at the
`CopiableAndNonCopiableDemo` example in the `examples` directory. We
have three types: `ImmutableType`, which implements `Copiable` (as you
would expect from the name), and `MutableType`, which implements
`NonCopiable`.  Finally, we have another type `Mutator`, which
maintains a reference to an `ImmutableType` and a `MutableType`, and
performs some operations on these objects.  Following the execution of
the program starting at the line

```java
remoteEngine.mutate(immutableType, mutableType);
```

Since `remoteEngine` is a long reference, this method invocation will
actually be performed on the remote machine. Since `ImmutableType` is
`Copiable`, a copy of this object will be passed to the remote
machine. This means that the remote machine will have access to all
of the `ImmutableType`s fields, an won't have to send requests back to
the 'source' machine.  But `MutableType` is `NonCopiable`, which means
that the `MutableType` argument will actually be a long reference.
Method calls will be forwarded to the actual object on the source
machine.

When the `Mutator.mutate(ImmutableType, MutableType)` method is
executed on the remote machine, the value of `val` can be obtained
directly from the `ImmutableType` argument because it is a local copy
to the remote machine. But the value of the `nonPermanentVal` field on
the `MutableType` object cannot be obtained directly, because the
`MutableType` is actually a long reference. Therefore, it must forward
the method invocation to the source machine. When changing the value
of the field by calling `MutableType.setNonPermanentVal(String)`,
again, the call must be forwarded to the source machine.  Since the
argument is a `String`, and `String`s are (implicitly) `Copiable`, the
`nonPermanentVal` field on the `MutableType` object local to the
source machine will be a short reference (i.e. a normal Java
reference, that can only exist within a single machine).

For more information about `Copiable` and `NonCopiable`, please read
the documentation.

Short-circuiting
----------------

Mokapot allows operations on objects which are referenced on multiple
systems to be short-circuited, so that method invocations operate
exactly as Java short references. This is not the case with Java's
RMI - the
[specification](https://docs.oracle.com/javase/8/docs/platform/rmi/spec/rmi-objmodel7.html#a3404)
states that

> When passing an exported remote object as a parameter or return
> value in a remote method call, the stub for that remote object is
> passed instead.

And when a method is invoked on a stub, several things happen,
including marshalling and unmarshalling arguments, network calls, etc.

For example, below is a representation of a Matrix:

```java
class Matrix {
    int[][] elements;

    Matrix(int rows, int cols) {
        this.elements = new int[rows][cols];
    }

    int get(int i, int j) {
        return elements[i][j];
    }

    void add(Matrix other) {
        for (int i = 0; i < elements.length; ++i) {
            for (int j = 0; j < elements[i].length; ++j) {
                elements[i][j] += other.get(i, j);
            }
        }
    }

    public static void main(String[] args) {
        // Create two Matrix objects on the same remote system
        // ...

        a.add(b);
    }
}
```

If we use Mokapot to create two remote Matrix objects, then when the
above code calls `other.get(i, j)`, Mokapot will realise that the
`other` object is on the same system, and so no network calls (even
through loopback) will occur. Instead, the method call will go locally
via Java's normal reference mechanisms (i.e. a "short reference").

However, if we use RMI, `other` will be a stub, and the call will not
be short-circuited, i.e. directly invoked on the actual object, even
though it is on the same machine.

Destroying Long References
--------------------------

The `DestroyRemoteReferencesDemo` class demonstrates how to ensure
your application terminates. Without the line `remoteObject = null;`,
the program would not terminate. In order to properly terminate the
`DistributedCommunicator` in your application, you must be able to
assign null to all long references.
