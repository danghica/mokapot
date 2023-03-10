# Millr tests
This readme outlines the structure of the test suite for `millr`. The test suite is roughly split
into four sections:

1. `xyz.acygn.millr`: unit tests
2. `xyz.acygn.millr.localsemantics`: functionality tests for `millr` (non-distributed code)
3. `xyz.acygn.millr.mokapotsemantics`: functionality tests for `millr` (code distributed with `mokapot`)
4. `xyz.acygn.millr.millingfailures`: minimal reproducible examples for discovered milling failures


## Unit tests - `xyz.acygn.millr`
The unit tests are written for `JUnit` and follow standard unit testing guidelines. Run any test
method or class individually, or execute the entire test suite. Due to the fact that methods in
`millr` are highly interdependent, writing unit tests for milling examples is unfeasible. The
unit tests in this package mostly cover `millr`'s utility classes.

## Local semantics - `xyz.acygn.millr.localsemantics`
These tests use the `JUnit` framework but do not follow a standard structure. These tests cover
the semantic equivalence of some "original" code (which executes completely locally) and a milled
version of the same code. These scenarios don't involve `mokapot` in any way. The overall idea of
these tests is to compare the return values of small methods in the original code and the milled
version. If the milled version complies with the specification of the original method, we can be
reasonably satisfied that `millr` has preserved the semantics of the original operations. The 
package is divided into subpackages, such that each subpackage's tests are focused on one of `millr`'s
transformations. For example, consider the package `xyz.acygn.millr.localsemantics.arrays`. It
tests `millr`'s replacement of arrays with array wrappers.

Each subpackage has the following structure. A main test class contains all of the `JUnit` test
code, while a number of supporting classes contain the code to be milled. The code to be milled
consists of a large set of methods, where each method performs some small operation of interest.
These methods are defined in an interface, which is then implemented by a concrete class. In the 
case of the `arrays` subpackage, the class `ArbitraryUserCode` implements the interface 
`ArbitraryMillable`.

The tests are carried out as follows. In the main test class of each package, a method annotated
as `@BeforeAll` invokes `millr` in order to produce milled version of the class files. Then,
an instance of the original class as well as the milled class are created. A `ClassLoader` is used 
to dynamically load the freshly milled code, since the milled class does not exist as far as the
compiler is concerned. Both the milled class and the original class implement the interface mentioned
above, which simplifies the test code considerably. Each of the individual tests simply calls one
of the methods defined in the interface on both the instance of the original class, and the instance
of the milled class. The return values are compared, with the expectation that they should be the
same. The core logic is

```java
assertEquals(original.someMethod(), milled.someMethod());
```

## Distributed semantics - `xyz.acygn.millr.mokapotsemantics`
These tests use the `JUnit` framework but do not follow a standard procedure. They cover the semantic
equivalence of some "original" code (which executes completely locally), a milled version of the code,
and a milled `mokapot` version of the code (which executes at least partially on a remote host). Unlike
the `localsemantics` case, it is not feasible to structure the tests around small cohesive methods whose
return values we compare (an explanation is provided at the bottom of this readme). Instead, we execute
longer, more complex methods, and track the state of variables associated with the methods. If the state
of the variables of interest evolves throughout execution in the same way in all three cases (local,
milled local, milled distributed), we can be reasonably satisfied that both milling and distributing
milled code using `mokapot` preserve the semantics of the original local code

The main test class for this package is `MokapotSemanticsTest`. All other classes in the package inherit
from `TrackableSampleProgram`, which defines the method `public StateTracker run()`. The `run()` method
of any inheriting class is intended to represent a small program. The inheriting classes are structured
in pairs such as `DirectAccessLocal` and `DirectAccessMokapot`, which provide purportedly equivalent
implementations of `run()` (one executes locally, the other is distributed with `mokapot`). Throughout
the execution of `run()`, a `StateTracker` is used to collect the state of any variables of interest.
The tracker is returned at the end of the method.

The tests are carried out as follows. In the main test class, a method annotated with `@BeforeAll`
invokes `millr` to produce milled versions of the class files of all classes in the package. Then,
using a `ClassLoader`, instances of the unmilled and milled classes are obtained. Each individual test
checks the equality of the state trackers returned by the calls to `run()` where we expect the state
sequence of the variables of interest to have been the same.


### Why use StateTracker?
In this case it is not possible to test semantic equivalence by looking at method return values
as we did in the **local semantics** case. Let us say that one wished to test the semantics of some
array operation in this manner. Then, one could encapsulate the operation into a method, and test as
follows:

```java
assertEquals(original.arrayOperation(), communicator.runRemotely(() -> mokapot.arrayOperation(), remote))
```

The problem with doing this is that `mokapot` would simply copy the object on which the method is called
onto the remote machine, execute the method call on the remote object, and return the value to the local
machine. The array operation of interest, hidden inside the method `arrayOperation()`, is executed
entirely locally (on the remote machine). A better solution would be to implement `arrayOperation`
as follows:

```java
public int arrayOperation() {
    return communicator.runRemotely(() -> { /* simple operations here */ }, remote);
}
```

This is better because the object on which the method `arrayOperation()` is called exists on the local
machine, and it is the actual operations of interest that are distributed. However, writing a large
number of methods such as `arrayOperation()` in this manner would not allow us to look at longer, more
complex operations. One could write methods such as `extremelyComplicatedArrayOperation()`, but then
looking simply at the return value would not provide any insight into what is happening within the
code. The solution to these problems is the `StateTracker`, which allows the "registration" of a
field or a method. A field of method that is registered with a `StateTracker` can have its state
collected by the tracker on request. A variable `int[][] matrix` would be registered as follows:

```java
stateTracker.register("matrix", this, int[][].class);
```

Once all fields of interest are registered, the method `tracker.collect()` can be called. The
`StateTracker` will obtain and remember the current values of the registered fields. Tracing
the semantics of the `run()` method then becomes an exercise in identifying what variables need
to be tracked, and when to call `collect()`. A restriction of `StateTracker` is that only fields
of a type with a meaningful implementation of `toString()` can be registered. By default this
includes `String`, primitives, and wrapper types (as well as arrays of all these). It is possible
to indicate to the tracker that a type has a meaningful implementation by calling

```java
tracker.declarePrintable(Class<?> type)
```


## Milling failures - `xyz.acygn.millr.millingfailures`
These tests attempt to produce minimal reproducible examples of observed failures in milling. For 
example, the test `NestedArrayFailureTest` attempts to produce a minimal reproducible example of a case 
in which `millr` was observed to fail to perform the array transformation when a variable `int[][] matrix`
was involved. These tests do not attempt to run the milled code.