package xyz.acygn.mokapot.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import static java.lang.Thread.sleep;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import xyz.acygn.millr.generation.StandinGenerator;
import xyz.acygn.mokapot.CommunicationAddress;
import xyz.acygn.mokapot.CommunicationEndpoint;
import xyz.acygn.mokapot.CopiableRunnable;
import xyz.acygn.mokapot.CopiableSupplier;
import xyz.acygn.mokapot.DebugMonitor;
import xyz.acygn.mokapot.DistributedCommunicator;
import static xyz.acygn.mokapot.DistributedCommunicator.LIFETIME_TIMEOUT;
import static xyz.acygn.mokapot.DistributedCommunicator.getCommunicator;
import xyz.acygn.mokapot.IsolatedEndpoint;
import xyz.acygn.mokapot.LengthIndependent;
import static xyz.acygn.mokapot.LengthIndependent.getActualClass;
import xyz.acygn.mokapot.MigrationActions;
import static xyz.acygn.mokapot.MigrationActions.createMigratably;
import static xyz.acygn.mokapot.MigrationActions.isStoredRemotely;
import xyz.acygn.mokapot.markers.Copiable;
import xyz.acygn.mokapot.markers.NonCopiable;
import xyz.acygn.mokapot.skeletons.Standin;
import xyz.acygn.mokapot.util.BackgroundGarbageCollection;
import static xyz.acygn.mokapot.util.BackgroundGarbageCollection.Operation.FINALIZE;
import xyz.acygn.mokapot.util.BlockingQueueInputStream;
import xyz.acygn.mokapot.util.BlockingQueueOutputStream;
import xyz.acygn.mokapot.util.Holder;
import xyz.acygn.mokapot.util.ObjectUtils;
import xyz.acygn.mokapot.util.Pair;
import xyz.acygn.mokapot.util.ResettableThreadLocal;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptions;
import static xyz.acygn.mokapot.util.ThreadUtils.delayInterruptionsRv;
import xyz.acygn.mokapot.wireformat.ObjectWireFormat;
import xyz.acygn.mokapot.wireformat.ReadableDescription;

/**
 * Main entry point for testing the distributed communication system. Contains
 * definitions of all the actual tests, plus a number of inner classes defined
 * in ways that will trigger special cases within the distributed communication
 * system.
 *
 * @author Alex Smith
 */
public class TestMain {

    /**
     * A thread-local string whose purpose is to determine which thread we're
     * on. Each client testing thread will set it to the name of its test group,
     * to make it easy to determine whether we're on one of those threads. If
     * we're on one of the others, it will be <code>null</code> by default until
     * it's set to some other value.
     */
    private final static ResettableThreadLocal<String> threadIdentity
            = new ResettableThreadLocal<>();

    /**
     * Whether to bail after marshal/unmarshal testing. If marshalling or
     * unmarshalling is broken, doing the rest of the tests is likely a bad
     * idea. This is <code>false</code> by default, but becomes
     * <code>true</code> if a problem in marshalling or unmarshalling is
     * detected.
     */
    private static boolean bailAfterUnmarshal = false;

    /**
     * Whether to disable timeouts while testing. Both the timeout-enabled and
     * timeout-disabled situations can meaningfully be tested.
     */
    private static boolean disableTimeouts = false;

    /**
     * Private constructor. This class is intended to be executed, not
     * instantiated.
     */
    private TestMain() {
    }

    /**
     * Runs the distributed communication tests.
     *
     * @param args Command-line arguments. There are three recognised arguments:
     * <code>-wfk</code> to wait for a newline before starting testing (making
     * it possible to attach a profiler or similar tool), <code>-dt</code> to
     * disable timeouts, and <code>-local</code> to perform the entire test on a
     * single JVM via the use of secondary communicators.
     * @throws java.io.IOException If there was an error creating a temporary
     * file on disk
     * @throws java.security.KeyManagementException If there was an error
     * initialising the Java cryptography library
     */
    public static void main(String[] args)
            throws IOException, KeyManagementException {
        final Copiable copiableTestObject = new Copiable() {
            @Override
            public boolean equals(Object obj) {
                return obj.getClass().equals(this.getClass());
            }

            @Override
            public int hashCode() {
                return 0;
            }
        };

        Path testStandinPath = Paths.get(
                System.getProperty("mokapot.buildinternal"),
                "test-standins");
        if (!Files.isDirectory(testStandinPath)) {
            testStandinPath = null;
        }

        final Path finalTestStandinPath = testStandinPath;

        if (Arrays.stream(args).anyMatch((s) -> s.equals("-wfk"))) {
            System.in.read();
        }

        disableTimeouts
                = Arrays.stream(args).anyMatch((s) -> s.equals("-dt"));
        ClientServerTest.setDisableTimeouts(disableTimeouts);
        ClientServerTest.setLocalServer(
                Arrays.stream(args).anyMatch((s) -> s.equals("-local")));

        StandinGenerator.setListener((generatedBytecode, name) -> {
            synchronized (System.out) {
                System.out.println("# client: standin generated: " + name);
                if (finalTestStandinPath != null) {
                    try {
                        Path generatedClassFilename
                                = finalTestStandinPath.resolve(name + ".class");
                        try {
                            Files.write(generatedClassFilename, generatedBytecode);
                        } catch (IOException ex) {
                            System.out.println("# client: warning: could not write "
                                    + "to " + generatedClassFilename.toString()
                                    + ": " + ex);
                        }
                    } catch (Exception ex) {
                        System.out.println("# client: warning: could not "
                                + "resolve " + name + ".class" + " against "
                                + finalTestStandinPath.toString() + ": " + ex);
                    }
                }
            }
        });

        @SuppressWarnings({"UnusedAssignment", "SleepWhileInLoop"})
        TestGroup[] testArray = new TestGroup[]{
            /* Long reference tests */
            /* test creation of local long references to class objects */
            new ClientOnlyTest(2, "create local long reference to object",
            (communicator, address, testGroup) -> {
                ArrayList<Integer> al
                        = new ArrayList<>();
                @SuppressWarnings("unchecked")
                ArrayList<Integer> alRef
                        = communicator.getTestHooks().makeLongReference(al);
                alRef.add(4);
                testGroup.okEq(alRef.get(0), 4,
                        "methods can be called via long references");
                testGroup.ok(alRef instanceof Standin,
                        "local long references implement Standin");
                /* The previous isStoredRemotely test is no longer valid; a
                   long reference created via test hook looks like a potentially
                   remote reference. */
            }),
            /* test creation of local long references in special cases */
            new ClientOnlyTest(10, "special cases of long references",
            (communicator, address, testGroup) -> {
                /* Object is a special case because it's effectively an
                   interface, and thus goes through the "interface-only"
                   code. */
                Object o = new Object();
                Object oRef = communicator.getTestHooks().makeLongReference(o);
                testGroup.okEq(oRef.hashCode(), o.hashCode(),
                        "long reference to Object");
                testGroup.ok(oRef instanceof Standin,
                        "long reference to Object implements Standin");

                List<Object> l;
                l = new ArrayList<>();
                l.add(o);

                /* UnmodifiableRandomAccessList has frequently given trouble in
                   the past (given that it's the core of an Exception), thus
                   needs a regression test. */
                l = Collections.unmodifiableList(l);
                List<?> lRef = communicator.getTestHooks().makeLongReference(l);
                testGroup.okEq(lRef.size(), l.size(), "unmodifiable List");
                testGroup.okEq(lRef.get(0), o, "unmodifiable List element");
                testGroup.ok(lRef instanceof Standin,
                        "long reference to unmodifiable List implements Standin");
                if (lRef instanceof Standin) {
                    testGroup.okEq(((Standin<?>) lRef).getReferentClass(null),
                            l.getClass(), "class of long reference to unmodifiable List");
                    System.out.println("# class is: " + l.getClass());
                } else {
                    testGroup.skipTest("class of long reference to unmodifiable List");
                }

                /* SingletonList uses the rarely used standin type 3. */
                l = Collections.singletonList(o);
                lRef = communicator.getTestHooks().makeLongReference(l);
                testGroup.okEq(lRef.size(), l.size(), "singleton List");
                testGroup.okEq(lRef.get(0), o, "singleton List element");
                testGroup.ok(lRef instanceof Standin,
                        "long reference to singleton List implements Standin");
                if (lRef instanceof Standin) {
                    testGroup.okEq(((Standin<?>) lRef).getReferentClass(null),
                            l.getClass(), "class of long reference to singleton List");
                    System.out.println("# class is: " + l.getClass());
                } else {
                    testGroup.skipTest("class of long reference to singleton List");
                }
            }),
            /* test cross-object method calls */
            new ClientOnlyTest(4, "private methods via local long reference",
            (communicator, address, testGroup) -> {
                CrossObjectVisibilityTest covt1
                        = new CrossObjectVisibilityTest();
                CrossObjectVisibilityTest covt2
                        = new CrossObjectVisibilityTest();
                CrossObjectVisibilityTest covt2r
                        = communicator.getTestHooks().makeLongReference(covt2);
                testGroup.okEq(covt1.addTwoWrapper(covt1, 3), 5,
                        "cross-object call, short to short");
                testGroup.okEq(covt1.addTwoWrapper(covt2r, 3), 5,
                        "cross-object call, short to long");
                testGroup.okEq(covt2r.addTwoWrapper(covt1, 3), 5,
                        "cross-object call, long to short");
                testGroup.okEq(covt2r.addTwoWrapper(covt2r, 3), 5,
                        "cross-object call, long to long");
            }),
            /* test .getClass() via a long reference */
            new ClientOnlyTest(3, "getActualClass() on long reference",
            (communicator, address, testGroup) -> {
                ArrayList<?> l = new ArrayList<>();
                Object l2 = communicator.getTestHooks().makeLongReference(l);
                List<?> l3 = communicator.getTestHooks().makeLongReference(l);
                ArrayList<?> l4 = communicator.getTestHooks().makeLongReference(l);
                testGroup.okEq(getActualClass(l2), getActualClass(l),
                        "via Object");
                testGroup.okEq(getActualClass(l3), getActualClass(l),
                        "via interface");
                testGroup.okEq(getActualClass(l4), getActualClass(l),
                        "via class");
            }),
            /* test .equals via a long reference */
            /*            new ClientOnlyTest(6, "equality of long references via interface",
            (communicator, address, testGroup) -> {
            Integer i = 4;
            Object i2 = DistributionUtils.makeLongReference(JAVA_PROXY,
            i, Object.class);
            testGroup.ok(i.equals(i2), "custom .equals, short = long");
            testGroup.ok(i2.equals(i), "custom .equals, long = short");
            testGroup.ok(i2.equals(i2), "custom .equals, long = long");
            Object o = new Object();
            Object o2 = DistributionUtils.makeLongReference(JAVA_PROXY,
            o, Object.class);
            testGroup.ok(o.equals(o2), "default .equals, short = long");
            testGroup.ok(o2.equals(o), "default .equals, long = short");
            testGroup.ok(o2.equals(o2), "default .equals, long = long");
            }),*/
            /* Marshal-via-copy tests */
            marshalUnmarshalTest(4, "unboxed primitive"),
            marshalUnmarshalTest(5, "boxed primitive"),
            marshalUnmarshalTest(6, "primitive as Object"),
            marshalUnmarshalTest("test", "string"),
            marshalUnmarshalTest(ClientOnlyTest.class, "class name"),
            marshalUnmarshalTest(new ExpectedException("test exception"), "exception"),
            marshalUnmarshalTest(new CopiableInt(4), "copiable"),
            marshalUnmarshalTest(new BigInteger(
            "A1A2A3A4B1B2B3B4C1C2C3C4D1D2D3D4", 16), "big integer"),
            marshalUnmarshalTest(FINALIZE,
            "enum"),
            marshalUnmarshalTest((CopiableSupplier) (() -> copiableTestObject + ":test"),
            "copiable lambda"),
            // marshalUnmarshalTest(createCyclicStructure(), "cyclic structure"),
            marshalUnmarshalTest(new int[]{6, 7}, "primitive array"),
            marshalUnmarshalTest(new BaseClass[]{
                new BaseClass(), new DerivedClass(), new BaseClass()},
            "heterogenous array"),
            marshalUnmarshalTest(new String[]{"one", "two", "three"},
            "homogenous array"),
            marshalUnmarshalTest(System.getSecurityManager(),
            "uncopiable object as Object"),
            new TestBailOutPoint(() -> !bailAfterUnmarshal),
            /* Client/server tests */
            /* the simplest possible client/server test */
            new ClientServerTest("basic client/server behaviour", 0,
            (communicator, address, testGroup) -> {
            }, false),
            /* run the same test again, to make sure the client shut down
               correctly */
            new ClientServerTest("rerun client after shutdown", 0,
            (communicator, address, testGroup) -> {
            }, false),
            /* the simplest possible communicator use */
            new ClientServerTest("runRemotely on self", 1,
            (communicator, address, testGroup) -> {
                communicator.runRemotely(() -> testGroup.ok(true, "method ran"),
                        address.getClientAddress());
            }, false),
            /* the simplest possible remote call */
            new ClientServerTest("trivial remote call", 1,
            new ExpectedValueTest(() -> 12, 12), false),
            /* make sure the remote call actually runs remotely */
            new ClientServerTest("remote calls run remotely", 1,
            new RunCodeTest(() -> getCommunicator().getMyAddress()) {
                @Override
                protected void verifyExpectedValue(Object actual,
                        ClientTestCode.AddressPair address,
                        TestGroup testGroup) {
                    testGroup.okEq(actual, address.getServerAddress(),
                            "expected address was seen");
                }
            }, false),
            /* test a simple callback */
            new ClientServerTest("simple callback", 1,
            new ClientCallbackTest((clientAddress) -> {
                CopiableSupplier<Integer> supply4 = () -> 4;
                return 1 + getCommunicator()
                        .runRemotely(supply4, clientAddress);
            }, 5), false),
            /* test serializability of addresses */
            new ClientServerTest("addresses are serializable", 4,
            (communicator, address, testGroup) -> {
                BlockingQueue<Byte> bq = new LinkedBlockingQueue<>();
                OutputStream os = new BlockingQueueOutputStream(bq);
                InputStream is = new BlockingQueueInputStream(bq);
                ObjectOutputStream oos = new ObjectOutputStream(os);
                ObjectInputStream ois = new ObjectInputStream(is);
                oos.writeObject(address.getClientAddress());
                oos.writeObject(address.getServerAddress());
                Object clientAddress = ois.readObject();
                Object serverAddress = ois.readObject();

                testGroup.okEq(clientAddress, address.getClientAddress(),
                        "deserialised client address equals original");
                testGroup.okEq(serverAddress, address.getServerAddress(),
                        "deserialised server address equals original");
                CommunicationAddress castClientAddress
                        = (CommunicationAddress) clientAddress;
                CommunicationAddress castServerAddress
                        = (CommunicationAddress) serverAddress;

                Object remoteServerAddress
                        = communicator.runRemotely(
                                () -> getCommunicator().getMyAddress(),
                                castServerAddress);
                testGroup.okEq(serverAddress, remoteServerAddress,
                        "deserialised server address is usable");
                Object callbackClientAddress
                        = communicator.runRemotely(
                                () -> communicator.runRemotely(
                                        () -> getCommunicator().getMyAddress(),
                                        castClientAddress),
                                address.getServerAddress());
                testGroup.okEq(serverAddress, remoteServerAddress,
                        "deserialised client address is usable");
            }, false),
            /* test that we can shut down the server from inside the client */
            new ClientServerTest("shut down server from client", 1,
            new ExpectedValueTest(() -> {
                new Thread(() -> getCommunicator().stopCommunication()).start();
                return null;
            }, null), true),
            /* simple tests of three-communicator cases */
            new ClientServerTest("simple three-communicator case", 2,
            (communicator, address, testGroup) -> {
                if (ClientServerTest.isLocalServer()) {
                    CommunicationAddress third;
                    {
                        MutableInteger i = communicator.runRemotely(
                                () -> new MutableInteger(),
                                address.getServerAddress());
                        i.addAndGet(4);
                        third = communicator.getTestHooks()
                                .createSecondaryCommunicator("third", null);
                        Holder<MutableInteger> hi
                                = communicator.runRemotely(
                                        () -> new Holder<>(i), third);
                        testGroup.okEq(hi.getValue(), i,
                                "indirect objects can be stored");
                        testGroup.okEq(hi.getValue().addAndGet(5), 9,
                                "indirect objects can be used");
                    }
                    communicator.runRemotely(
                            () -> {
                                getCommunicator().asyncStopCommunication();
                            }, third);
                } else {
                    // These aren't currently implemented, and trying to test
                    // them anyway breaks so dramatically that we can't even
                    // use a TODO mark, so skip them.
                    testGroup.skipTest("indirect objects can be stored");
                    testGroup.skipTest("indirect objects can be used");
                }
                communicator.runRemotely(
                        () -> {
                            getCommunicator().asyncStopCommunication();
                        }, address.getServerAddress());
            }, true),
            /* test that threads keep their identity across multiple systems */
            new ClientServerTest("threads exist across systems", 3,
            (communicator, address, testGroup) -> {
                threadIdentity.set("threads exist across systems: client");
                CopiableSupplier<Pair<String, Integer>> getThreadIdentity
                        = () -> new Pair<>(
                                getCommunicator().runRemotely(
                                        () -> threadIdentity.get(),
                                        address.getClientAddress()),
                                LengthIndependent.currentThread().hashCode());
                Pair<String, Integer> remoteReturn
                        = getCommunicator().runRemotely(getThreadIdentity,
                                address.getServerAddress());
                testGroup.okEq(remoteReturn.getFirst(),
                        "threads exist across systems: client",
                        "thread projection has the expected thread-locals");
                testGroup.okEq(remoteReturn.getSecond(),
                        LengthIndependent.currentThread().hashCode(),
                        "LengthIndependent#currentThread is consistent");
                testGroup.okEq(Thread.currentThread(),
                        LengthIndependent.currentThread(),
                        "{LengthIndependent,Thread}#currentThread match");
                threadIdentity.set(null);
            }, false),
            /*new ClientCallbackTest((clientAddress) -> {
                CopiableSupplier<Pair<String, Integer>> getThreadIdentity
                        = () -> new Pair<>(threadIdentity.get(),
                                LengthIndependent.currentThread().hashCode());
                return getCommunicator()
                        .runRemotely(getThreadIdentity, clientAddress);
            }, new Pair<>("threads exist across systems: client",
            Thread.currentThread().hashCode())), false),*/
            /* test that creating a new thread remotely creates a corresponding
               thread locally */
            new ClientServerTest("new threads correspond to new threads", 1,
            new ClientCallbackTest((clientAddress) -> {
                BlockingQueue<Optional<String>> rq = new SynchronousQueue<>();
                Thread thread = new Thread(() -> {
                    String remoteThreadIdentity;
                    CopiableSupplier<String> getThreadIdentity
                            = () -> threadIdentity.get();
                    try {
                        remoteThreadIdentity
                                = getCommunicator()
                                        .runRemotely(getThreadIdentity, clientAddress);
                    } catch (Throwable ex) {
                        remoteThreadIdentity = "Exception on server: " + ex;
                    }
                    final Optional<String> finalRTI
                            = Optional.ofNullable(remoteThreadIdentity);
                    delayInterruptions(() -> rq.offer(finalRTI, 3, SECONDS));
                });
                thread.start();
                return delayInterruptionsRv(() -> rq.poll(3, SECONDS));
            }, Optional.empty()), false),
            /* make sure a copiable lambda is recreated with a compatible
               type */
            new ClientServerTest("lambda copied between systems", 5,
            (communicator, address, testGroup) -> {
                /* Make sure the calculation of i isn't optimized out */
                final int i = (address.hashCode() + 16) - address.hashCode();
                IntUnaryOperator u
                        = (IntUnaryOperator & Serializable & Copiable) ((x)
                        -> (2 * x) + i);
                CopiableSupplier<Integer> f = () -> {
                    int j = u.applyAsInt(32);
                    if (u instanceof NonCopiable) {
                        j += 8;
                    }
                    if (u instanceof IntUnaryOperator) {
                        j += 4;
                    }
                    if (u instanceof Serializable) {
                        j += 2;
                    }
                    if (u instanceof Copiable) {
                        j += 1;
                    }
                    new Thread(() -> getCommunicator().stopCommunication()).start();
                    return j;
                };
                int j = communicator.runRemotely(f, address.getServerAddress());
                synchronized (System.out) {
                    System.out.println("# Marshalled value is: "
                            + communicator.getTestHooks().describeField(u, null)
                                    .toString());
                }
                testGroup.ok((j & 112) == 80, "correct value captured");
                testGroup.ok((j & 4) == 4, "correct functional interface");
                testGroup.ok((j & 2) == 2, "preserves Serializable");
                testGroup.ok((j & 1) == 1, "preserves marker interfaces");
                testGroup.ok((j & 8) == 0, "no extra marker interfaces");
            }, true),
            /* test cross-system method calls (i.e. send a method parameter by
               (long) reference, and ensure the reference works) */
            new ClientServerTest("cross-system long reference", 4,
            (communicator, address, testGroup) -> {
                List<Integer> l = new ArrayList<>();
                l.add(4);
                l.add(6);
                CopiableSupplier<Integer> f = () -> {
                    l.add(8);
                    new Thread(() -> getCommunicator().stopCommunication()).start();
                    return l.get(0)
                            + (l instanceof Standin ? 128 : 0)
                            + (isStoredRemotely(l) ? 256 : 0);
                };
                int i = communicator.runRemotely(f, address.getServerAddress());
                testGroup.okEq(i & 127, 4,
                        "read-only calls return the correct value");
                testGroup.ok((i & 128) == 128,
                        "remote references implement Standin");
                testGroup.ok((i & 256) == 256,
                        "remote references are stored remotely");
                testGroup.okEq(l.get(2), 8,
                        "impure calls mutate local values as expected");
            }, true),
            new ClientServerTest("HashSet remotes correctly", 2,
            (communicator, address, testGroup) -> {
                Set<Integer> s = new HashSet<>();
                s.add(4);
                Integer i = communicator.runRemotely(
                        () -> {
                            Integer first = s.iterator().next();
                            s.add(7);
                            new Thread(() -> getCommunicator()
                            .stopCommunication()).start();
                            return first;
                        }, address.getServerAddress());
                testGroup.okEq(i, (Integer) 4,
                        "HashSet can be read remotely");
                s.remove(4);
                testGroup.okEq(s.iterator().next(), 7,
                        "HashSet can be modified remotely");
            }, true),
            new ClientServerTest("standins implement interfaces correctly", 2,
            (communicator, address, testGroup) -> {
                CopiableSupplier<BaseClass> f = () -> {
                    new Thread(() -> getCommunicator().stopCommunication()).start();
                    return new DerivedClass();
                };
                BaseClass bi = communicator.runRemotely(f,
                        address.getServerAddress());
                testGroup.ok(bi instanceof DerivedClass,
                        "standin stands in for the correct class");
                testGroup.ok(bi instanceof BaseInterface,
                        "standin has the correct interfaces implemented");
            }, true),
            /* test cross-system method calls, using a class with intentionally
               colliding method codes */
            new ClientServerTest("cross-system call with hash collision", 2,
            (communicator, address, testGroup) -> {
                ClashingMethodCodes cmc1 = new ClashingMethodCodes();
                CopiableSupplier<Void> f = () -> {
                    ClashingMethodCodes cmc2 = new ClashingMethodCodes();
                    cmc1.setRef(cmc2);
                    cmc2.setRef(cmc1);
                    new Thread(() -> getCommunicator().stopCommunication()).start();
                    return null;
                };
                communicator.runRemotely(f, address.getServerAddress());
                testGroup.okEq(cmc1.meb69d9e302b46574(), 63,
                        "correct methods were run (first direction)");
                testGroup.okEq(cmc1.getRef().meb69d9e302b46574(), 63,
                        "correct methods were run (second direction)");
                /* break the cycle */
                cmc1.getRef().setRef(null);
                cmc1.setRef(null);
            }, true),
            /* test cross-object method calls, this time remotely */
            new ClientServerTest("private methods via remote long reference", 4,
            (communicator, address, testGroup) -> {
                CrossObjectVisibilityTest covt2
                        = new CrossObjectVisibilityTest();
                CopiableSupplier<List<Integer>> f = () -> {
                    List<Integer> rl = new ArrayList<>(4);
                    CrossObjectVisibilityTest covt1
                            = new CrossObjectVisibilityTest();
                    rl.add(covt1.addTwoWrapper(covt1, 5));
                    rl.add(covt1.addTwoWrapper(covt2, 6));
                    rl.add(covt2.addTwoWrapper(covt1, 7));
                    rl.add(covt2.addTwoWrapper(covt2, 8));
                    new Thread(() -> getCommunicator().stopCommunication()).start();
                    return rl;
                };
                @SuppressWarnings("unchecked")
                List<Integer> rl = communicator.runRemotely(
                        f, address.getServerAddress());
                testGroup.okEq(rl.get(0), 7,
                        "cross-object call, local to local");
                testGroup.okEq(rl.get(1), 8,
                        "cross-object call, local to remote");
                testGroup.okEq(rl.get(2), 9,
                        "cross-object call, remote to local");
                testGroup.okEq(rl.get(3), 10,
                        "cross-object call, remote to remote");
            }, true),
            /* regression test for bug #47 */
            new ClientServerTest("thread projections keep their identity", 3,
            (communicator, address, testGroup) -> {
                ThreadLocal<Integer> tl
                        = communicator.runRemotely(
                                () -> ThreadLocal.withInitial(() -> 0),
                                address.getServerAddress());
                tl.set(1);
                testGroup.okEq(tl.get(), 1, "thread-local retains value");
                tl.set(2);
                /* Note: we're testing a race condition, the sleeps are to
                   increase the chance that specific race condition happens */
                Thread t2 = new Thread(() -> {
                    int i = communicator.runRemotely(
                            () -> {
                                delayInterruptions(() -> Thread.sleep(2000));
                                return tl.get();
                            }, address.getServerAddress());
                    testGroup.okEq(i, 0,
                            "fresh threads don't reuse old thread-locals");
                });
                t2.start();
                Thread.sleep(1000);
                testGroup.okEq(tl.get(), 2, "with two threads running, "
                        + "the correct thread gets the value");
                t2.join(3000);
                communicator.runRemotely(
                        () -> {
                            getCommunicator().asyncStopCommunication();
                        },
                        address.getServerAddress());
            }, true),
            /* test the simplest case of manual migration */
            new ClientServerTest("simple manual migration", 8,
            (communicator, address, testGroup) -> {
                BlockingQueue<Integer> bq = new SynchronousQueue<>();
                MutableInteger i = createMigratably(MutableInteger::new, false);
                testGroup.okEq(i.addAndGet(4), 4,
                        "migratable objects act like the object");
                CopiableRunnable f = () -> {
                    new Thread(() -> {
                        /* Ensure i is remote, from the server's point of
                           view. */
                        delayInterruptions(() -> bq.offer(isStoredRemotely(i)
                                ? 1 : 0, 3, SECONDS));
                        /* Wait for i to be migrated from the client. */
                        int j = delayInterruptionsRv(() -> bq.poll(3, SECONDS));
                        /* Ensure that we can still call methods on it. */
                        delayInterruptions(() -> bq.offer(i.addAndGet(j),
                                3, SECONDS));
                        /* Check to see if it's local now. */
                        delayInterruptions(() -> bq.offer(isStoredRemotely(i)
                                ? 1 : 0, 3, SECONDS));
                        /* Stop communication. This uses a separate thread
                            to allow bq and i to be deallocated. */
                        new Thread(getCommunicator()::stopCommunication).start();
                    }).start();
                };
                communicator.runRemotely(
                        f, address.getServerAddress());

                /* Ensure i is local from our point of view, remote from the
                   server's. */
                testGroup.okEq(isStoredRemotely(i), false,
                        "i is local to the client");
                testGroup.okEq(delayInterruptionsRv(() -> bq.poll(3, SECONDS)),
                        1, "i is remote to the server");

                MigrationActions<?> ima
                        = communicator.getMigrationActionsFor(i);
                testGroup.okEq(ima.getCurrentLocation(), address.getClientAddress(),
                        "i is hosted on the client");
                ima.migratePrepare(false);
                ima.migrateCommit(address.getServerAddress(), false);
                testGroup.okEq(ima.getCurrentLocation(), address.getServerAddress(),
                        "after migrate, i is now hosted on the server");
                ima.migrateConclude();

                /* Tell the server that it can continue now. */
                delayInterruptions(() -> bq.offer(3, 3, SECONDS));
                /* See what value we got back from it. In order to produce a
                   value, the object has to be onlined on the server. */
                testGroup.okEq(delayInterruptionsRv(() -> bq.poll(3, SECONDS)),
                        7, "migrated object responded to remote method calls");
                /* Check to see where the object is now. */
                testGroup.okEq(isStoredRemotely(i), true,
                        "i is now remote to the client");
                testGroup.okEq(delayInterruptionsRv(() -> bq.poll(3, SECONDS)),
                        0, "i is now local to the server");
            }, true),
            /* test the simplest case of automatic migration */
            new ClientServerTest("simple automatic migration", 9,
            (communicator, address, testGroup) -> {
                BlockingQueue<Integer> bq = new SynchronousQueue<>();
                Holder<Reference<MutableInteger>> ri = new Holder<>();

                /* The server and client routines run in parallel (thus the use
                   of a new thread and a blocking queue to synchronise
                   them). <code>i</code> is created inside a lambda so that
                   it'll go out of scope and thus become deallocatable. */
                Runnable r = () -> {
                    MutableInteger i = createMigratably(
                            MutableInteger::new, false);
                    ri.accept(new WeakReference<>(i));
                    testGroup.okEq(i.addAndGet(4), 4,
                            "migratable objects act like the object");
                    testGroup.ok(!communicator.getMigrationActionsFor(i)
                            .isMigratingAnywhere(),
                            "locally alive object is loosely referenced");
                    CopiableRunnable f = () -> {
                        new Thread(() -> {
                            /* Ensure i is remote, from the server's point of
                               view. */
                            delayInterruptions(() -> bq.offer(isStoredRemotely(i)
                                    ? 1 : 0, 3, SECONDS));
                            /* Wait for i to be deallocated on the client. */
                            int j = delayInterruptionsRv(() -> bq.poll(3, SECONDS));
                            /* Check to see if it's tightly referenced, now that it's
                               locally dead. */
                            delayInterruptions(() -> bq.offer(getCommunicator()
                                    .getMigrationActionsFor(i)
                                    .isMigratingAnywhere() ? 1 : 0, 3, SECONDS));
                            /* Ensure that we can still call methods on it. */
                            delayInterruptions(
                                    () -> bq.offer(i.addAndGet(j), 3, SECONDS));
                            /* Check to see if it's local now. */
                            delayInterruptions(() -> bq.offer(isStoredRemotely(i)
                                    ? 1 : 0, 3, SECONDS));
                            /* Check to see if it's loosely referenced now. */
                            delayInterruptions(() -> bq.offer(getCommunicator()
                                    .getMigrationActionsFor(i)
                                    .isMigratingAnywhere() ? 1 : 0, 3, SECONDS));
                            BackgroundGarbageCollection.volatileAccess(i);
                            /* Stop communication. This uses a separate thread
                               to allow bq and i to be deallocated. */
                            new Thread(getCommunicator()::stopCommunication).start();
                        }).start();
                    };
                    communicator.runRemotely(
                            f, address.getServerAddress());

                    /* Ensure i is local from our point of view, remote from the
                       server's. */
                    testGroup.okEq(isStoredRemotely(i), false,
                            "i is local to the client");
                    testGroup.okEq(delayInterruptionsRv(() -> bq.poll(3, SECONDS)),
                            1, "i is remote to the server");
                };
                r.run();

                /* Wait for i to be deallocated. */
                System.out.println("# client: waiting for deallocation");
                int tryCount = 0;
                while (ri.getValue().get() != null && tryCount < 10) {
                    /* Note: not BackgroundGarbageCollection, we want to block
                       until the GC and finalization is done (i.e. this is
                       foreground GC, not background GC). */
                    System.gc();
                    System.runFinalization();
                    tryCount++;
                }
                testGroup.ok(tryCount < 10, "locally dead object was deallocated");
                System.out.println("# number of GC cycles run: " + tryCount);

                /* Tell the server that it can continue now. */
                delayInterruptions(() -> bq.offer(3, 3, SECONDS));

                /* Ask the server whether the object is online or not. (We
                   can't check directly as we don't have a local reference to
                   it.) */
                testGroup.okEq(delayInterruptionsRv(() -> bq.poll(3, SECONDS)),
                        1, "locally dead object is tightly referenced");

                /* See what value we got back from it. */
                testGroup.okEq(delayInterruptionsRv(() -> bq.poll(3, SECONDS)),
                        7, "tightly referenced object responded to remote method calls");
                /* Check to see where the object is now. */
                testGroup.markTodo();
                testGroup.okEq(delayInterruptionsRv(() -> bq.poll(3, SECONDS)),
                        0, "i is now local to the server");
                /* Check to ensure the object is now loosely referenced. */
                testGroup.okEq(delayInterruptionsRv(() -> bq.poll(3, SECONDS)),
                        0, "after migration, i is loosely referenced again");
            }, true),
            //            /* Test reference loops */
            //            new ClientServerTest("reference loop",
            //            1, (communicator, address, testGroup) -> {
            //                Holder<Pair<Object, Object>> hpoo1
            //                        = MigrationActions.createMigratably(Holder::new, false);
            //                Holder<Pair<Object, Object>> hpoo2
            //                        = MigrationActions.createMigratably(Holder::new, false);
            //                boolean hpoo3Migrated = communicator.runRemotely(() -> {
            //                    int t = 0;
            //                    Holder<Pair<Object, Object>> hpoo3
            //                            = MigrationActions.createMigratably(
            //                                    Holder::new, false);
            //                    Holder<Pair<Object, Object>> hpoo4
            //                            = MigrationActions.createMigratably(
            //                                    Holder::new, false);
            //                    hpoo1.accept(new Pair<>(hpoo3, hpoo2));
            //                    hpoo2.accept(new Pair<>(hpoo4, hpoo1));
            //                    hpoo3.accept(new Pair<>(hpoo1, hpoo4));
            //                    hpoo4.accept(new Pair<>(hpoo2, hpoo3));
            //                    System.gc();
            //                    System.runFinalization();
            //                    return isStoredRemotely(hpoo3);
            //                }, address.getServerAddress());
            //                testGroup.ok(!hpoo3Migrated, "no premature migration");
            //
            //                /* Note: we get the test framework to kill the server, rather
            //                   than attempting a clean shutdown, as a clean shutdown is
            //                   highly unlikely to work if anything goes wrong with the
            //                   test. */
            //            }, false),
            /* Test that the keepalive system works */
            new ClientServerTest("objects stay alive beyond the timeout",
            2, (communicator, address, testGroup) -> {
                /* create a reference in each direction */
                MutableInteger i = new MutableInteger();
                i.addAndGet(4);
                ArrayList<MutableInteger> iList
                        = communicator.runRemotely(() -> new ArrayList<>(),
                                address.getServerAddress());
                iList.add(i);
                i = null;
                System.out.println(
                        "# client: waiting for 3 seconds past timeout");
                sleep((LIFETIME_TIMEOUT + 3)
                        * 1000);
                i = iList.get(0);
                testGroup.ok(true, "local-to-remote reference still works");
                iList.add(i);
                testGroup.okEq(iList.size(), 2,
                        "remote-to-local reference still works");
            }, false),
            /* regression test for bug #10 */
            new ClientServerTest("communications can be stopped at one end only",
            5, (communicator, address, testGroup) -> {
                /* first create a reference from remote to local, to get the
                   problem to show up */
                MutableInteger i = new MutableInteger();
                i.addAndGet(4);
                ArrayList<MutableInteger> iList
                        = communicator.runRemotely(() -> new ArrayList<>(),
                                address.getServerAddress());
                iList.add(i);
                testGroup.okEq(iList.get(0).addAndGet(3), 7,
                        "remote-to-local reference has the right value");
                testGroup.okEq(i.addAndGet(1), 8,
                        "remote-to-local reference is indeed a reference");
                testGroup.ok(isStoredRemotely(iList),
                        "remote-to-local reference is indeed remote");
                /* freeing iList should make it so that no references go via the
                   communicator */
                iList = null;
                if (!ClientServerTest.isLocalServer()) {
                    /* so can we stop communications? */
                    communicator.stopCommunication();
                    testGroup.ok(true, "can stop communications");
                    /* restart communications, a) to maintain the state that the
                    test driver expects, b) to make sure we can */
                    communicator.startCommunication();
                    testGroup.ok(true, "can start communications");
                } else {
                    /* we don't expect to be able to stop communications at the
                       client end only when we have a secondary communicator, so
                       there's no point in running that part of the test. */
                    System.out.println(
                            "# client: cannot test this, it wouldn't be "
                            + "expected to work in local-server mode");
                    testGroup.skipTest("can stop communications");
                    testGroup.skipTest("can start communications");
                }
            }, false),
            /* regression test for bug #13 */
            /*            new ClientServerTest("AtomicInteger unmarshals correctly", 5,
            (communicator, address, testGroup) -> {
                AtomicInteger i = communicator.runRemotely(
                        () -> new AtomicInteger(5), address);
                testGroup.ok(i instanceof Standin,
                        "marshalled via standin");
                testGroup.ok(i instanceof AtomicInteger,
                        "appropriate class for reference");
                testGroup.okEq(i.incrementAndGet(), 6,
                        "incremement remote value locally");
                testGroup.okEq(communicator.runRemotely(
                        i::incrementAndGet, address),
                        7, "increment remote value remotely");
                testGroup.okEq(i.get(), 7, "verify new value");
                communicator.runRemotely(() -> {
                    new Thread(getCommunicator()::stopCommunication).start();
                    return null;
                }, address);
            }, true), */
            /* regression test for bug #3 */
            new ClientServerTest("location manager finalize/create race", 1,
            (communicator, address, testGroup) -> {
                MutableInteger i = new MutableInteger();
                int total = 0;
                for (int j = 0; j < 100; j++) {
                    total += communicator.runRemotely(() -> i.addAndGet(1),
                            address.getServerAddress());
                }
                communicator.runRemotely(() -> {
                    new Thread(getCommunicator()::stopCommunication).start();
                    return null;
                },
                        address.getServerAddress());
                testGroup.okEq(total, 5050,
                        "no race repeatedly managing the same object");
            }, true)
        };
        TestGroup allTests = new SerialTests("main test", testArray);

        allTests.plan();
        /* Don't output to stdout until after plan() */
        System.out.println("# configuration: timeouts enabled: "
                + !disableTimeouts);

        allTests.runTest(1);

        System.exit(TestGroup.getTestingStatus(true));
    }

    /**
     * Generates a test that marshalling and unmarshalling an object will
     * produce something that compares equal to the original object.
     *
     * @param o The object to marshal and unmarshal.
     * @param what A description of what's being marshalled and unmarshalled.
     * @return A test that tests whether the unmarshalling of the marshalling of
     * <code>o</code> compares equal to the original.
     */
    private static ClientOnlyTest marshalUnmarshalTest(
            Object o, String what) {
        final int testCount = (o instanceof Throwable) ? 4 : 1;

        return new ClientOnlyTest(testCount, "marshal/unmarshal " + what,
                (communicator, address, testGroup)
                -> {
            /* if something goes wrong before we can set this back to false,
               bail */
            boolean oldBail = bailAfterUnmarshal;
            bailAfterUnmarshal = true;

            /* Verify that we can describe the object. (Some objects might not
               be reproducible from a description, so we don't test that.) */
            ReadableDescription description
                    = communicator.getTestHooks().describe(o);
            synchronized (System.out) {
                System.out.println("# Described: " + description.toString());
            }

            /* Verify that we can reference-describe the object, then reproduce
               it from that reference. */
            ReadableDescription marshalled
                    = communicator.getTestHooks().describeField(o, null);
            synchronized (System.out) {
                System.out.println("# Marshalled: " + marshalled.toString());
            }
            Object unmarshalled = communicator.getTestHooks()
                    .readClassAndObject(marshalled, null, null);

            if (testCount == 1) {
                boolean isOk;
                if (o instanceof CopiableSupplier) {
                    /* Lambdas don't necessarily have a functioning .equals(),
                       so instead run them and see if we get the same result. */
                    Object expectedResult = ((CopiableSupplier) o).get();
                    Object actualResult
                            = unmarshalled instanceof CopiableSupplier
                                    ? ((CopiableSupplier) unmarshalled).get()
                                    : "<not a copiable supplier>";
                    isOk = testGroup.okEq(actualResult, expectedResult,
                            "unmarshalled supplier produces expected result");
                } else {
                    isOk = testGroup.okEq(unmarshalled, o,
                            "correct value was unmarshalled");
                }
                bailAfterUnmarshal = !isOk || oldBail;
            } else {
                bailAfterUnmarshal = false;
                Throwable actual = (Throwable) unmarshalled;
                Throwable expected = (Throwable) o;
                bailAfterUnmarshal |= !testGroup.okEq(
                        actual.getCause(), expected.getCause(),
                        "unmarshalled throwable has expected cause");
                bailAfterUnmarshal |= !testGroup.okEq(
                        actual.getMessage(), expected.getMessage(),
                        "unmarshalled throwable has expected message");
                /* Don't compare stack traces; the act of calling-
                   getStackTrace() produces different values for the two
                   exceptions, for reasons I don't fully understand, even though
                   the exceptions are identical */
                bailAfterUnmarshal |= !testGroup.okEq(
                        actual.getSuppressed(),
                        expected.getSuppressed(), "unmarshalled "
                        + "throwable has expected suppressed list");
                bailAfterUnmarshal |= !testGroup.okEq(
                        actual.toString(), expected.toString(),
                        "unmarshalled throwable stringifies correctly");
                bailAfterUnmarshal |= oldBail;
            }
        });
    }

    /**
     * One of the simplest possible <code>Copiable</code> classes. Used for
     * testing that <code>Copiable</code> serialisation works correctly.
     */
    private static class CopiableInt implements Copiable {

        /**
         * The integer stored in this <code>CopiableInt</code>.
         */
        private final int value;

        /**
         * Creates a <code>CopiableInt</code> wrapping an integer value.
         *
         * @param value The integer to wrap.
         */
        CopiableInt(int value) {
            this.value = value;
        }

        /**
         * Returns the value of this CopiableInt.
         *
         * @return The value of the CopiableInt.
         */
        @Override
        public int hashCode() {
            return this.value;
        }

        /**
         * Compares this <code>CopiableInt</code> to another object.
         *
         * @param other The object to compare to.
         * @return True if the other object is a <code>CopiableInt</code> that
         * wraps the same value as this object does.
         */
        @Override
        public boolean equals(Object other) {
            return ObjectUtils.equals(this, other, (t) -> t.value);
        }
    }

    /**
     * Client test code for a test in which the client runs code remotely on the
     * server.
     */
    private static abstract class RunCodeTest implements ClientTestCode {

        /**
         * The supplier that executes remotely.
         */
        protected final CopiableSupplier<? extends Object> supplier;

        /**
         * Create a new client test code that runs code on the server.
         *
         * @param supplier The code to run on the server.
         */
        protected RunCodeTest(CopiableSupplier<? extends Object> supplier) {
            this.supplier = supplier;
        }

        /**
         * Runs the function remotely, then tests to see whether the expected
         * value was returned.
         *
         * @param communicator The communicator to use for the remote function
         * call.
         * @param address The addresses of the participants in the
         * communication.
         * @param testGroup The test group on which to call <code>okEq</code> to
         * compare the actual and expected return values.
         */
        @Override
        public void testCode(DistributedCommunicator communicator,
                ClientTestCode.AddressPair address, TestGroup testGroup) {
            threadIdentity.set(testGroup.getTestGroupName());
            Object i = communicator.runRemotely(supplier,
                    address.getServerAddress());
            verifyExpectedValue(i, address, testGroup);
        }

        /**
         * Verifies that the correct value was returned from the test. Output
         * from this function is by means of calling <code>ok</code> and friends
         * on the test group.
         *
         * @param actual The value that was actually returned.
         * @param address The addresses that were used in the test.
         * @param testGroup The test group that should be used to report success
         * or failure.
         */
        protected abstract void verifyExpectedValue(Object actual,
                ClientTestCode.AddressPair address, TestGroup testGroup);
    }

    /**
     * Client test code for a test in which the client runs code remotely on the
     * server, and checks to see that it returned the expected value.
     */
    private static class ExpectedValueTest extends RunCodeTest {

        /**
         * The object which we expect to be supplied.
         */
        private final Object expected;

        /**
         * Creates the test code for a remote code test using a supplier.
         *
         * @param supplier The function to call.
         * @param expected The expected return value of the function.
         */
        ExpectedValueTest(CopiableSupplier<? extends Object> supplier,
                Object expected) {
            super(supplier);
            this.expected = expected;
        }

        @Override
        protected void verifyExpectedValue(Object i,
                ClientTestCode.AddressPair address, TestGroup testGroup) {
            testGroup.okEq(i, expected, "correct value returned");
        }
    }

    /**
     * A function of one argument that can be safely copied.
     *
     * @param <A> The argument type.
     * @param <R> The return value type.
     */
    @FunctionalInterface
    private static interface CopiableFunction<A, R>
            extends Function<A, R>, Copiable, Serializable {
    }

    /**
     * Client test code for a test in which the client runs code remotely on the
     * server, and checks to see that it returned the expected value, but the
     * server runs code on the client as well.
     */
    private static class ClientCallbackTest implements ClientTestCode {

        /**
         * The function which executes remotely.
         */
        private final CopiableFunction<CommunicationAddress, ? extends Object> supplier;

        /**
         * The object which we expect to be supplied.
         */
        private final Object expected;

        /**
         * Creates the test code for a remote code test using a supplier that
         * needs to know the client's address.
         *
         * @param supplier The function to call.
         * @param expected The expected return value of the function.
         */
        ClientCallbackTest(CopiableFunction<CommunicationAddress, ? extends Object> supplier,
                Object expected) {
            this.supplier = supplier;
            this.expected = expected;
        }

        /**
         * Runs the function remotely, then tests to see whether the expected
         * value was returned.
         *
         * @param communicator The communicator to use for the remote function
         * call.
         * @param address The addresses of the participants in the
         * communication.
         * @param testGroup The test group on which to call <code>okEq</code> to
         * compare the actual and expected return values.
         */
        @Override
        public void testCode(DistributedCommunicator communicator,
                ClientTestCode.AddressPair address, TestGroup testGroup) {
            threadIdentity.set(testGroup.getTestGroupName());
            final CommunicationAddress clientAddress
                    = address.getClientAddress();
            Object i;
            /* Note: we want to send supplier across the network, rather than
               this object, so we need to move it into a final local variable */
            final CopiableFunction<CommunicationAddress, ? extends Object> finalSupplier = supplier;
            CopiableSupplier<Object> innerSupplier
                    = () -> finalSupplier.apply(clientAddress);
            i = communicator.runRemotely(innerSupplier,
                    address.getServerAddress());
            testGroup.okEq(i, expected, "correct value returned");
        }
    }

    /**
     * An exception that's used for testing purposes; we throw it, and see if it
     * arrives at the expected place.
     */
    private static class ExpectedException extends RuntimeException {

        /**
         * Explicit serialisation version, as is required for a serialisable
         * class to be compatible between machines. The number was originally
         * generated randomly, and should be changed whenever the class's fields
         * are changed in an incompatible way.
         *
         * @see java.io.Serializable
         */
        private static final long serialVersionUID = 0xadc5562226d3a8fL;

        /**
         * Creates an ExpectedException with a given message.
         *
         * @param message The message to use.
         */
        ExpectedException(String message) {
            super(message);
        }
    }

    /**
     * A test which runs entirely on one client, no server involved. The client
     * test code will run with <code>null</code> for the server address.
     */
    private static class ClientOnlyTest extends TestGroup {

        /**
         * The code which this test runs.
         */
        private final ClientTestCode testCode;

        /**
         * Creates a test group that runs the given test code.
         *
         * @param testCount The number of individual tests that
         * <code>testCode</code> runs.
         * @param testGroupName The name of the created test group.
         * @param testCode The code to run for the test.
         */
        ClientOnlyTest(int testCount, String testGroupName,
                ClientTestCode testCode) {
            super(testCount, testGroupName);
            this.testCode = testCode;
        }

        /**
         * Run <code>testCode</code>, giving it access to a communicator but not
         * a server address. (In other words, the <code>address</code> argument
         * to the test code will be <code>null</code>.)
         *
         * @throws Exception If the test code throws an exception
         */
        @Override
        protected void testImplementation() throws Exception {
            final CommunicationEndpoint endpoint
                    = new IsolatedEndpoint();
            final DistributedCommunicator communicator
                    = new DistributedCommunicator(endpoint,
                            !disableTimeouts);

            communicator.setDebugMonitor(new DebugMonitor() {
                @Override
                public void warning(String message) {
                    System.out.println("# WARNING: " + message);
                }

                @Override
                public void newMessage(MessageInfo mi) {
                }
            });
            communicator.enableTestHooks();
            communicator.startCommunication();
            try {
                testCode.testCode(communicator, null, this);
            } finally {
                communicator.stopCommunication();
            }
        }
    }

    /**
     * A class used to test cyclic object graphs. It basically contains a single
     * mutable reference to another recursive container.
     * <p>
     * This also implements <code>Copiable</code> (because it's meant to test
     * the code for copying cyclic objects), and <code>Serializable</code> (so
     * that it can be tested for equality via comparing serializations).
     */
    private static class RecursiveContainer implements Copiable, Serializable {

        /**
         * Explicit serialisation version, as is required for a serialisable
         * class to be compatible between machines. The number was originally
         * generated randomly, and should be changed whenever the class's fields
         * are changed in an incompatible way.
         *
         * @see java.io.Serializable
         */
        private static final long serialVersionUID = 0x93b5f70d41fab25eL;

        /**
         * The value of this recursive container. It's a reference to another
         * recursive container (or to itself).
         */
        public RecursiveContainer value;

        /**
         * Default constructor. Initialises <code>value</code> to null.
         */
        RecursiveContainer() {
            value = null;
        }
    }

    /**
     * Creates an object that contains itself. Used for testing serialisation of
     * cyclic structures.
     *
     * @return A cyclic structure.
     */
    private static RecursiveContainer createCyclicStructure() {
        RecursiveContainer r = new RecursiveContainer();
        r.value = r;
        return r;
    }

    /**
     * A class used to test <code>private</code> method calls from one object to
     * another object. The actual functionality of the class isn't very
     * interesting; it exists for its implementation, which exercises features
     * of Java that are hard to test in other ways.
     * <p>
     * Note that some of the tests care that this class is
     * non-<code>final</code> and non-<code>Copiable</code>.
     */
    private static class CrossObjectVisibilityTest implements NonCopiable {

        /**
         * Default constructor. Does nothing, because all
         * <code>CrossObjectVisibilityTest</code> objects happen to be
         * identical.
         */
        CrossObjectVisibilityTest() {
        }

        /**
         * Uses a given CrossObjectVisibilityTest object to add 2 to the given
         * integer.
         *
         * @param covt The CrossObjectVisibilityTest object that performs the
         * addition.
         * @param x The integer to add 2 to.
         * @return <code>x</code> + 2.
         */
        public Integer addTwoWrapper(
                CrossObjectVisibilityTest covt, Integer x) {
            return covt.addTwo(x);
        }

        /**
         * Adds 2 to the given integer. This is <code>private</code>, and thus
         * can only be called indirectly via
         * <code>CrossObjectVisbilityTest#addTwoWrapper</code>.
         *
         * @param x The integer to add 2 to.
         * @return <code>x</code> + 2.
         * @see CrossObjectVisibilityTest#addTwoWrapper(
         * xyz.acygn.mokapot.test.TestMain.CrossObjectVisibilityTest,
         * java.lang.Integer)
         */
        private int addTwo(int x) {
            return x + 2;
        }
    }

    /**
     * An integer that can be mutated. Used as a very simple stateful class for
     * checking that stateful classes are distributed correctly.
     */
    private static class MutableInteger {

        /**
         * The value of the mutable integer.
         */
        private int value;

        /**
         * Creates a new mutable integer with value 0.
         */
        MutableInteger() {
            value = 0;
        }

        /**
         * Adds the given value to the integer, then returns its new value.
         *
         * @param by The value by which to increase the integer.
         * @return The new value.
         */
        int addAndGet(int by) {
            value += by;
            return value;
        }
    }

    /**
     * A marker interface used for tests to see whether interfaces are correctly
     * implemented by standins. This interface is implemented on a class, then
     * we test to see if an object of that class can be stored in a variable of
     * <code>BaseInterface</code> type.
     */
    private static interface BaseInterface {

    }

    /**
     * A concrete class that has subclasses. Used for testing code for
     * reconstructing the class of an object in cases where it's not indicated
     * by the field's declared type. Also used to test indirectly implemented
     * interfaces.
     */
    private static class BaseClass implements BaseInterface, NonCopiable {

        /**
         * Some dummy data stored in an object of the class. This is used to
         * make the equals() method nontrivial.
         */
        private final int dummyData = 0xAABBCCDD;

        /**
         * Returns a value that's consistent between equal objects.
         *
         * @return The dummy data stored in the class.
         */
        @Override
        public int hashCode() {
            return dummyData;
        }

        /**
         * Compares this object to another object for equality. Two objects of
         * this class are equal if they have the same actual class and the same
         * dummy data.
         *
         * @param obj The object to compare to.
         * @return Whether that object and this object are equal.
         */
        @Override
        public boolean equals(Object obj) {
            return ObjectUtils.equals(this, obj, (t) -> t.dummyData);
        }
    }

    /**
     * A class that has a concrete superclass. Used for testing code for
     * reconstructing the class of an object in cases where it's not indicated
     * by the field's declared type.
     */
    private static class DerivedClass extends BaseClass {
    }
}
