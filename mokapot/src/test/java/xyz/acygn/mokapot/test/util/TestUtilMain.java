package xyz.acygn.mokapot.test.util;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import xyz.acygn.mokapot.test.ClientTestCode;
import xyz.acygn.mokapot.test.SerialTests;
import xyz.acygn.mokapot.test.TestGroup;
import xyz.acygn.mokapot.test.bytecode.TrivialTestClass;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper;
import xyz.acygn.mokapot.util.AutocloseableLockWrapper.CannotLockException;
import xyz.acygn.mokapot.util.BackgroundGarbageCollection;
import xyz.acygn.mokapot.util.BackgroundGarbageCollection.Operation;
import xyz.acygn.mokapot.util.ComparablePair;
import xyz.acygn.mokapot.util.CrossThreadReadWriteLock;
import xyz.acygn.mokapot.util.DataByteBuffer;
import xyz.acygn.mokapot.util.DeterministicAutocloseable;
import xyz.acygn.mokapot.util.DoublyWeakConcurrentMap;
import xyz.acygn.mokapot.util.EnumerationIterator;
import xyz.acygn.mokapot.util.ExpirableMap;
import xyz.acygn.mokapot.util.ExtendedList;
import xyz.acygn.mokapot.util.Holder;
import xyz.acygn.mokapot.util.ImmutableSets;
import xyz.acygn.mokapot.util.KeepalivePool;
import xyz.acygn.mokapot.util.MutexPool;
import xyz.acygn.mokapot.util.ObjectIdentity;
import xyz.acygn.mokapot.util.ObjectMethodDatabase;
import xyz.acygn.mokapot.util.Pair;
import xyz.acygn.mokapot.util.ResettableThreadLocal;
import xyz.acygn.mokapot.util.ThreadUtils;
import xyz.acygn.mokapot.util.TypeSafe;
import xyz.acygn.mokapot.util.VMInfo;
import xyz.acygn.mokapot.util.WeakConcurrentSet;
import xyz.acygn.mokapot.util.WeakValuedConcurrentMap;

/**
 * Tests of utility classes. Contains definitions of all the actual tests,
 * linked up to a standard TAP test runner.
 *
 * @author Stefan Hagiu
 */
public class TestUtilMain {

    /**
     * Runs the tests of utility classes.
     *
     * @param args Command-line arguments. Ignored.
     */
    public static void main(String[] args) {
        @SuppressWarnings({"SleepWhileInLoop", "SizeReplaceableByIsEmpty", "UnusedAssignment"})
        TestGroup[] testArray = new TestGroup[]{
            new ClientOnlyTest(3, "mokapot.util.AutocloseableLockWrapper",
            (communicator, address, testGroup) -> {
                try {
                    Lock l = new ReentrantLock();
                    AutocloseableLockWrapper lWrap
                            = new AutocloseableLockWrapper(l, new Object());

                    lWrap.close();
                    lWrap.close();
                    testGroup.ok(false, "AutocloseableLockWrapper letting user unlock twice.");
                } catch (IllegalStateException e) {
                    testGroup.ok(true, "AutocloseableLockWrapper not letting user unlock twice.");
                }

                Lock l = new ReentrantLock();
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        l.lock();
                        try {
                            Thread.sleep(9999999);
                        } catch (InterruptedException e) {
                            l.unlock();
                        }
                    }
                };
                try {
                    t.start();
                    while (t.getState() != Thread.State.TIMED_WAITING) {
                        Thread.sleep(1);
                    }

                    @SuppressWarnings({"unused", "resource"})
                    AutocloseableLockWrapper lWrap
                            = new AutocloseableLockWrapper(l, false, "testing");
                    testGroup.ok(false, "AutocloseableLockWrapper didn't throw a CannotLockException.");
                    testGroup.ok(false, "AutocloseableLockWrapper didn't throw a CannotLockException.");
                } catch (CannotLockException e) {
                    testGroup.ok(true, "AutocloseableLockWrapper threw a CannotLockException, just as expected.");
                    try {
                        t.interrupt();
                        t.join();
                        AutocloseableLockWrapper lWrap
                                = new AutocloseableLockWrapper(l, false, "testing");
                        lWrap.close();
                        lWrap.close();
                        testGroup.ok(false, "AutocloseableLockWrapper letting user unlock twice.");
                    } catch (IllegalStateException e1) {
                        testGroup.ok(true, "AutocloseableLockWrapper not letting user unlock twice.");
                    }
                }
            }),
            new ClientOnlyTest(10, "mokapot.util.BackgroundGarbageCollection",
            (communicator, address, testGroup) -> {
                //Testing addFinalizer
                {
                    ArrayList<Integer> ran = new ArrayList<>();

                    TrivialTestClass t = new TrivialTestClass(5);
                    BackgroundGarbageCollection.addFinaliser(t, () -> {
                        ran.add(1);
                    });

                    if (ran.isEmpty()) {
                        testGroup.ok(true, "BackgroundGarbageCollection addFinaliser works fine.");
                    } else {
                        testGroup.ok(false, "BackgroundGarbageCollection addFinaliser does not work correctly.");
                    }

                    t = null;

                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (ran.size() == 1 && ran.get(0) == 1) {
                        testGroup.ok(true, "BackgroundGarbageCollection addFinaliser works fine.");
                    } else {
                        testGroup.ok(false, "BackgroundGarbageCollection addFinaliser does not work correctly.");
                    }
                }
                //Testing startKeepalive and stopKeepalive
                {
                    TrivialTestClass t = new TrivialTestClass(5);
                    try {
                        BackgroundGarbageCollection.endKeepalive(t);
                        testGroup.ok(false, "BackgroundGarbageCollection endKeepalive does not work fine.");
                    } catch (IllegalArgumentException e) {
                        testGroup.ok(true, "BackgroundGarbageCollection endKeepalive works fine.");
                    }

                    WeakReference<TrivialTestClass> wr = new WeakReference<>(t);
                    BackgroundGarbageCollection.startKeepalive(t);
                    t = null;

                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (wr.get() != null) {
                        testGroup.ok(true, "BackgroundGarbageCollection startKeepalive works fine.");
                    } else {
                        testGroup.ok(false, "BackgroundGarbageCollection startKeepalive does not work correctly.");
                    }

                    BackgroundGarbageCollection.endKeepalive(wr.get());

                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (wr.get() == null) {
                        testGroup.ok(true, "BackgroundGarbageCollection startKeepalive works fine.");
                    } else {
                        testGroup.ok(false, "BackgroundGarbageCollection startKeepalive does not work correctly.");
                    }

                    t = new TrivialTestClass(5);
                    wr = new WeakReference<>(t);
                    for (int i = 0; i < 10; i++) {
                        BackgroundGarbageCollection.startKeepalive(t);
                    }
                    t = null;

                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (wr.get() != null) {
                        testGroup.ok(true, "BackgroundGarbageCollection startKeepalive works fine.");
                    } else {
                        testGroup.ok(false, "BackgroundGarbageCollection startKeepalive does not work correctly.");
                    }

                    for (int i = 0; i < 9; i++) {
                        BackgroundGarbageCollection.endKeepalive(wr.get());
                    }

                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (wr.get() != null) {
                        testGroup.ok(true, "BackgroundGarbageCollection startKeepalive works fine.");
                    } else {
                        testGroup.ok(false, "BackgroundGarbageCollection startKeepalive does not work correctly.");
                    }

                    BackgroundGarbageCollection.endKeepalive(wr.get());
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (wr.get() == null) {
                        testGroup.ok(true, "BackgroundGarbageCollection startKeepalive works fine.");
                    } else {
                        testGroup.ok(false, "BackgroundGarbageCollection startKeepalive does not work correctly.");
                    }
                }
                //Testing perform
                {
                    TrivialTestClass t1 = new TrivialTestClass(5);
                    TrivialTestClass t2 = new TrivialTestClass(6);
                    WeakReference<TrivialTestClass> r1 = new WeakReference<>(t1);
                    WeakReference<TrivialTestClass> r2 = new WeakReference<>(t2);

                    BackgroundGarbageCollection.startKeepalive(t2);

                    t1 = null;
                    t2 = null;

                    BackgroundGarbageCollection.perform(Operation.GC_THEN_FINALIZE);
                    Thread.sleep(50);

                    if (r1.get() == null && r2.get() != null) {
                        testGroup.ok(true, "BackgroundGarbageCollection perform works fine.");
                    } else {
                        testGroup.ok(false, "BackgroundGarbageCollection perform does not work correctly.");
                    }

                    TrivialTestClass t3 = new TrivialTestClass(6);
                    WeakReference<TrivialTestClass> r3 = new WeakReference<>(t3);
                    t3 = null;

                    BackgroundGarbageCollection.perform(Operation.NO_OPERATION);
                    if (r3.get() != null) {
                        testGroup.ok(true, "BackgroundGarbageCollection perform works fine.");
                    } else {
                        testGroup.ok(false, "BackgroundGarbageCollection perform does not work correctly.");
                    }
                }
            }),
            new ClientOnlyTest(5, "mokapot.util.ComparablePair",
            (communicator, address, testGroup) -> {
                ComparablePair<Integer, Integer> a = new ComparablePair<>(20, 11);
                ComparablePair<Integer, Integer> b = new ComparablePair<>(20, 9);
                ComparablePair<Integer, Integer> c = new ComparablePair<>(10, 15);

                testGroup.okEq(a.compareTo(b), a.getFirst().compareTo(b.getFirst()) == 0
                        ? a.getSecond().compareTo(b.getSecond())
                        : a.getFirst().compareTo(b.getFirst()), "ComparablePair.compareTo()");

                testGroup.okEq(b.compareTo(a), b.getFirst().compareTo(a.getFirst()) == 0
                        ? b.getSecond().compareTo(a.getSecond())
                        : b.getFirst().compareTo(a.getFirst()), "ComparablePair.compareTo()");

                testGroup.okEq(a.compareTo(c), a.getFirst().compareTo(c.getFirst()) == 0
                        ? a.getSecond().compareTo(c.getSecond())
                        : a.getFirst().compareTo(c.getFirst()), "ComparablePair.compareTo()");

                testGroup.okEq(c.compareTo(a), c.getFirst().compareTo(a.getFirst()) == 0
                        ? c.getSecond().compareTo(a.getSecond())
                        : c.getFirst().compareTo(a.getFirst()), "ComparablePair.compareTo()");

                testGroup.okEq(a.compareTo(a), a.getFirst().compareTo(a.getFirst()) == 0
                        ? a.getSecond().compareTo(a.getSecond())
                        : a.getFirst().compareTo(a.getFirst()), "ComparablePair.compareTo()");
            }),
            new ClientOnlyTest(9, "mokapot.util.CrossThreadReadWriteLock",
            (communicator, address, testGroup) -> {
                //First test
                {
                    CrossThreadReadWriteLock lock = new CrossThreadReadWriteLock();
                    try {
                        lock.readLock().unlock();
                        testGroup.ok(false, "Unlocked CrossThreadReadWriteLock which wasn't locked");
                    } catch (IllegalStateException e) {
                        testGroup.ok(true, "Cannot unlock unlocked CrossThreadReadWriteLock, just as expected");
                    }
                }
                //Second test
                {
                    CrossThreadReadWriteLock lock = new CrossThreadReadWriteLock();
                    try {
                        lock.writeLock().unlock();
                        testGroup.ok(false, "Unlocked CrossThreadReadWriteLock which wasn't locked");
                    } catch (IllegalStateException e) {
                        testGroup.ok(true, "Cannot unlock unlocked CrossThreadReadWriteLock, just as expected");
                    }
                }
                //Third test
                {
                    CrossThreadReadWriteLock lock = new CrossThreadReadWriteLock();
                    lock.writeLock().lock();
                    if (!lock.readLock().tryLock()) {
                        testGroup.ok(true, "Cannot lock read lock when write lock is locked.");
                    } else {
                        testGroup.ok(false, "Locked read lock when write lock is locked");
                    }
                }
                //Fourth test
                {
                    CrossThreadReadWriteLock lock = new CrossThreadReadWriteLock();
                    lock.readLock().lock();
                    if (!lock.writeLock().tryLock()) {
                        testGroup.ok(true, "Cannot lock read lock when write lock is locked.");
                    } else {
                        testGroup.ok(false, "Locked read lock when write lock is locked");
                    }
                }
                //Fifth test
                {
                    CrossThreadReadWriteLock lock = new CrossThreadReadWriteLock();
                    for (int i = 0; i < 100; i++) {
                        lock.readLock().lock();
                    }
                    if (lock.countReadLocks() == 100) {
                        testGroup.ok(true, "Read lock locked the right amount of times.");
                    } else {
                        testGroup.ok(false, "Read lock not locked the right amount of times.");
                    }

                    for (int i = 0; i < 100; i++) {
                        lock.readLock().unlock();
                    }

                    if (lock.writeLock().tryLock()) {
                        testGroup.ok(true, "Could lock write lock after playing with read lock.");
                    } else {
                        testGroup.ok(false, "Could not lock write lock after playing with read lock.");
                    }

                    try {
                        lock.readLock().unlock();
                        testGroup.ok(false, "Unlocked readLock which wasn't locked");
                    } catch (IllegalStateException e) {
                        testGroup.ok(true, "Cannot unlock unlocked readLock, just as expected");
                    }
                }
                // (AIS) Test race conditions in the read lock.
                {
                    final CrossThreadReadWriteLock lock
                            = new CrossThreadReadWriteLock();
                    final Runnable readLockTestThreadBody = () -> {
                        for (int i = 0; i < 100000; i++) {
                            try (DeterministicAutocloseable da
                                    = new AutocloseableLockWrapper(
                                            lock.readLock(), "lock test")) {
                            }
                        }
                    };
                    final Runnable writeLockTestThreadBody = () -> {
                        for (int i = 0; i < 100000; i++) {
                            try (DeterministicAutocloseable da
                                    = new AutocloseableLockWrapper(
                                            lock.writeLock(), "lock test")) {
                            }
                        }
                    };
                    Thread t1 = new Thread(readLockTestThreadBody);
                    Thread t2 = new Thread(readLockTestThreadBody);

                    t1.start();
                    t2.start();
                    readLockTestThreadBody.run();
                    t1.join();
                    t2.join();
                    testGroup.okEq(lock.countReadLocks(), 0,
                            "read lock/unlock handles race conditions");

                    Thread t3 = new Thread(readLockTestThreadBody);
                    Thread t4 = new Thread(writeLockTestThreadBody);
                    Thread t5 = new Thread(writeLockTestThreadBody);
                    readLockTestThreadBody.run();
                    t3.join();
                    t4.join();
                    t5.join();
                    testGroup.okEq(lock.countReadLocks(), 0,
                            "write lock/unlock handles race conditions");
                }
            }),
            new ClientOnlyTest(54, "mokapot.util.DataByteBuffer",
            (communicator, address, testGroup) -> {
                //Test 1 - Testing constructors.
                {
                    DataByteBuffer db1 = new DataByteBuffer(1);
                    if (db1.isReadOnly() || db1.isRewound()) {
                        testGroup.ok(false, "DataByteBuffer constructor not working properly.");
                    } else {
                        testGroup.ok(true, "DataByteBuffer constructor working properly.");
                    }
                    try {
                        db1.skipBytes(1);
                        testGroup.ok(true, "DataByteBuffer skipped 1 byte, just as expected.");

                    } catch (IOException e) {
                        testGroup.ok(false, "DataByteBuffer couldn't skip byte.");
                    }

                    try {
                        db1.readBoolean();
                        testGroup.ok(false, "DataByteBuffer read an unexisting bit.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer cannot read unexisting bits.");
                    }

                    DataByteBuffer db2 = new DataByteBuffer("abc".getBytes(), false);
                    if (db2.readByte() == "a".getBytes()[0]) {
                        testGroup.ok(true, "DataByteBuffer first character is right.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer first character is not right.");
                    }
                    db2.writeByte("9".getBytes()[0]);
                    testGroup.ok(true, "DataByteBuffer wrote as expected.");

                    if (db2.readByte() == "c".getBytes()[0]) {
                        testGroup.ok(true, "DataByteBuffer first character is right.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer first character is not right.");
                    }

                    DataByteBuffer db3 = new DataByteBuffer(db2);

                    if (db3.readByte() == "a".getBytes()[0]
                            && db3.readByte() == "9".getBytes()[0]
                            && db3.readByte() == "c".getBytes()[0]) {
                        testGroup.ok(true, "DataByteBuffer constructor working fine.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer constructor not working properly.");
                    }

                    DataByteBuffer db4 = new DataByteBuffer("abc".getBytes(), true);

                    try {
                        db4.writeByte("9".getBytes()[0]);
                        testGroup.ok(false, "Read only constructor makes writeable DataByteBuffer.");
                    } catch (IOException e) {
                        testGroup.ok(true, "Read only constructor makes read only DataByteBuffer.");
                    }
                }
                //Test 2 - getWrittenLength
                {
                    DataByteBuffer db1 = new DataByteBuffer("abc".getBytes(), false);
                    if (db1.getWrittenLength() == 0) {
                        testGroup.ok(true, "DataByteBuffer getWrittenLength returns 0 when nothing was written.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer getWrittenLength does not return 0 when nothing was written.");
                    }

                    db1.writeByte("z".getBytes()[0]);

                    if (db1.getWrittenLength() == 1) {
                        testGroup.ok(true, "DataByteBuffer getWrittenLength returned correct value.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer getWrittenLength did not return correct value.");
                    }

                    db1.readByte();

                    db1.writeByte("z".getBytes()[0]);

                    if (db1.getWrittenLength() == 3) {
                        testGroup.ok(true, "DataByteBuffer getWrittenLength returned correct value.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer getWrittenLength did not return correct value.");
                    }
                }
                //Test 3 - isReadOnly and isRewound
                {
                    DataByteBuffer db1 = new DataByteBuffer("abc".getBytes(), true);
                    if (db1.isReadOnly() && db1.isRewound()) {
                        testGroup.ok(true, "DataByteBuffer isReadOnly and isRewound gave correct answer.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer isReadOnly and isRewound did not give correct answer.");
                    }
                    db1.resetForRead();
                    if (db1.isReadOnly() && db1.isRewound()) {
                        testGroup.ok(true, "DataByteBuffer isReadOnly and isRewound gave correct answer.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer isReadOnly and isRewound did not give correct answer.");
                    }

                    DataByteBuffer db2 = new DataByteBuffer("abc".getBytes(), false);
                    if (!db2.isReadOnly() && !db2.isRewound()) {
                        testGroup.ok(true, "DataByteBuffer isReadOnly and isRewound gave correct answer.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer isReadOnly and isRewound did not give correct answer.");
                    }

                    db2.resetForRead();
                    if (db2.isReadOnly() && db2.isRewound()) {
                        testGroup.ok(true, "DataByteBuffer isReadOnly and isRewound gave correct answer.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer isReadOnly and isRewound did not give correct answer.");
                    }

                    DataByteBuffer db3 = new DataByteBuffer("abc".getBytes(), false);
                    db3.writeByte("z".getBytes()[0]);
                    db3.resetForRead();
                    if (db3.isReadOnly() && db3.isRewound()) {
                        testGroup.ok(true, "DataByteBuffer isReadOnly and isRewound gave correct answer.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer isReadOnly and isRewound did not give correct answer.");
                    }
                }
                //Test 4 - write and read
                {
                    DataByteBuffer db = new DataByteBuffer(100);

                    db.writeBoolean(false);
                    db.writeByte(1);
                    db.writeChar('a');
                    db.writeDouble(1.5);

                    db.writeFloat(2.5f);
                    db.writeInt(97);
                    db.writeLong(101);
                    db.writeShort((short) -7);

                    db.writeByte(127);
                    db.writeByte(-128);
                    db.writeShort((short) -1);
                    db.writeShort((short) 1);

                    db.write("abc".getBytes());
                    db.write("def".getBytes());
                    db.write("aaaghijklmn".getBytes(), 3, 8);

                    db.resetForRead();

                    if (db.readBoolean() == false
                            && db.readByte() == 1
                            && db.readChar() == 'a'
                            && db.readDouble() == 1.5
                            && db.readFloat() == 2.5f
                            && db.readInt() == 97
                            && db.readLong() == 101
                            && db.readShort() == -7
                            && db.readUnsignedByte() == 127
                            && db.readUnsignedByte() == 128
                            && db.readUnsignedShort() == 65535
                            && db.readUnsignedShort() == 1) {
                        testGroup.ok(true, "DataByteBuffer All read methods work fine.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer There is a problem with at least one of the read methods.");
                    }

                    ByteBuffer h = db.readByteSlice(3);
                    if (h.get() == "a".getBytes()[0] && h.get() == "b".getBytes()[0] && h.get() == "c".getBytes()[0]) {
                        testGroup.ok(true, "DataByteBuffer readByteSlice working properly.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer readByteSlice not working properly.");
                    }

                    byte[] helper = new byte[3];
                    db.readFully(helper);
                    if (helper[0] == "d".getBytes()[0]
                            && helper[1] == "e".getBytes()[0]
                            && helper[2] == "f".getBytes()[0]) {
                        testGroup.ok(true, "DataByteBuffer readFully works.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer readFully does not work properly.");
                    }

                    helper = new byte[2];
                    db.readFully(helper, 0, 2);
                    if (helper[0] == "g".getBytes()[0]
                            && helper[1] == "h".getBytes()[0]) {
                        testGroup.ok(true, "DataByteBuffer readFully works.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer readFully does not work properly.");
                    }

                    helper = new byte[6];
                    db.readFully(helper, 2, 4);
                    if (helper[2] == "i".getBytes()[0]
                            && helper[3] == "j".getBytes()[0]
                            && helper[4] == "k".getBytes()[0]
                            && helper[5] == "l".getBytes()[0]) {
                        testGroup.ok(true, "DataByteBuffer readFully works.");
                    } else {
                        testGroup.ok(false, "DataByteBuffer readFully does not work properly.");
                    }
                }
                //Test 4 - read when there is nothing to read
                {
                    DataByteBuffer db = new DataByteBuffer(0);
                    try {
                        db.readBoolean();
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readByte();
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readByteSlice(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException | IllegalArgumentException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readChar();
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readDouble();
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readFloat();
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readFully(new byte[100]);
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readFully(new byte[100], 1, 20);
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readInt();
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readLong();
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readShort();
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readUnsignedByte();
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.readUnsignedShort();
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                }
                //Test 5 - writing when not enough space
                {
                    DataByteBuffer db = new DataByteBuffer(0);
                    try {
                        db.writeBoolean(true);
                        testGroup.ok(false, "DataByteBuffer problem in test 5.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeByte(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 5.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeChar('a');
                        testGroup.ok(false, "DataByteBuffer problem in test 5.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeDouble(2);
                        testGroup.ok(false, "DataByteBuffer problem in test 5.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeFloat(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 5.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeInt(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 5.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeLong(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 5.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeShort(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 5.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.write(new byte[1]);
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.write(new byte[1], 0, 1);
                        testGroup.ok(false, "DataByteBuffer problem in test 4.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                }
                //Test 6 -writing on read only buffer
                {
                    DataByteBuffer db = new DataByteBuffer(new byte[2000], true);
                    try {
                        db.writeBoolean(true);
                        testGroup.ok(false, "DataByteBuffer problem in test 6.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeByte(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 6.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeChar('a');
                        testGroup.ok(false, "DataByteBuffer problem in test 6.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeDouble(2);
                        testGroup.ok(false, "DataByteBuffer problem in test 6.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeFloat(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 6.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeInt(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 6.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeLong(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 6.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.writeShort(1);
                        testGroup.ok(false, "DataByteBuffer problem in test 6.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer writing seems to work fine.");
                    }
                    try {
                        db.write(new byte[1]);
                        testGroup.ok(false, "DataByteBuffer problem in test 6.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                    try {
                        db.write(new byte[1], 0, 1);
                        testGroup.ok(false, "DataByteBuffer problem in test 6.");
                    } catch (IOException e) {
                        testGroup.ok(true, "DataByteBuffer reading seems to work fine.");
                    }
                }
            }),
            new ClientOnlyTest(25, "mokapot.util.DoublyWeakConcurrentMap",
            (communicator, address, testGroup) -> {
                //Testing clear and isEmpty, size, get, and put
                {
                    DoublyWeakConcurrentMap<TrivialTestClass, TrivialTestClass> dwcm = new DoublyWeakConcurrentMap<>();
                    if (dwcm.isEmpty() && dwcm.size() == 0) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap isEmpty and size work.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap isEmpty and size don't work properly.");
                    }

                    TrivialTestClass a = new TrivialTestClass(1);
                    TrivialTestClass b = new TrivialTestClass(1);
                    TrivialTestClass c = new TrivialTestClass(1);
                    TrivialTestClass d = new TrivialTestClass(1);

                    if (dwcm.put(a, a) == null
                            && dwcm.get(a) == a
                            && dwcm.get(b) == null
                            && !dwcm.isEmpty()
                            && dwcm.size() == 1
                            && dwcm.put(a, b) == a
                            && dwcm.put(b, d) == null
                            && dwcm.put(c, a) == null
                            && dwcm.put(b, c) == d) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap put works fine");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap put doesn't work properly.");
                    }

                    if (!dwcm.isEmpty() && dwcm.size() == 3) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap isEmpty and size work.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap isEmpty and size don't work properly.");
                    }

                    dwcm.clear();
                    testGroup.ok(true, "DoublyWeakConcurrentMap clear works.");

                    if (dwcm.isEmpty() && dwcm.size() == 0) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap isEmpty and size work.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap isEmpty and size don't work properly.");
                    }
                }
                //Testing containsKey and containsValue
                {
                    DoublyWeakConcurrentMap<TrivialTestClass, TrivialTestClass> dwcm = new DoublyWeakConcurrentMap<>();
                    if (!dwcm.containsKey(new TrivialTestClass(1)) && !dwcm.containsValue(new TrivialTestClass(1))) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap containsKey and containsValue work correctly.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap containsKey and containsValue don't work correctly.");
                    }

                    dwcm.put(new TrivialTestClass(1), new TrivialTestClass(1));
                    if (!dwcm.containsKey(new TrivialTestClass(1)) && !dwcm.containsValue(new TrivialTestClass(1))) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap containsKey and containsValue work correctly.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap containsKey and containsValue don't work correctly.");
                    }

                    TrivialTestClass a = new TrivialTestClass(5);
                    TrivialTestClass b = new TrivialTestClass(6);

                    dwcm.put(a, b);
                    if (dwcm.containsKey(a) && dwcm.containsValue(b)) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap containsKey and containsValue work correctly.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap containsKey and containsValue don't work correctly.");
                    }

                    b = null;
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (!dwcm.containsKey(a) && !dwcm.containsValue(b) && dwcm.get(a) == null) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap containsKey and containsValue work correctly.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap containsKey and containsValue don't work correctly.");
                    }

                    b = new TrivialTestClass(6);
                    dwcm.put(a, b);
                    a = null;
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (!dwcm.containsKey(a) && !dwcm.containsValue(b) && dwcm.get(a) == null) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap containsKey and containsValue work correctly.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap containsKey and containsValue don't work correctly.");
                    }
                }
                //Testing putAll
                {
                    DoublyWeakConcurrentMap<TrivialTestClass, TrivialTestClass> dwcm = new DoublyWeakConcurrentMap<>();
                    Map<TrivialTestClass, TrivialTestClass> m = new HashMap<>();
                    TrivialTestClass a = new TrivialTestClass(1);
                    TrivialTestClass b = new TrivialTestClass(2);
                    TrivialTestClass c = new TrivialTestClass(3);
                    TrivialTestClass d = new TrivialTestClass(4);
                    m.put(a, b);
                    m.put(b, b);
                    m.put(c, d);
                    dwcm.putAll(m);

                    if (!dwcm.isEmpty() && dwcm.size() == 3 && dwcm.get(a) == b && dwcm.get(b) == b && dwcm.get(c) == d) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap putAll works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap putAll doesn't work properly.");
                    }

                    a = null;
                    m.remove(a);
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (m.size() == 3 && dwcm.size() == 3 && !dwcm.containsKey(a) && dwcm.containsValue(b)) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap putAll works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap putAll doesn't work properly.");
                    }
                }
                //Testing putIfAbsent
                {
                    DoublyWeakConcurrentMap<TrivialTestClass, TrivialTestClass> dwcm = new DoublyWeakConcurrentMap<>();
                    TrivialTestClass a = new TrivialTestClass(1);
                    TrivialTestClass b = new TrivialTestClass(2);
                    TrivialTestClass c = new TrivialTestClass(3);
                    TrivialTestClass d = new TrivialTestClass(4);

                    if (dwcm.putIfAbsent(a, b) == null) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap putIfAbsent works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap putIfAbsent doesn't work properly.");
                    }
                    if (dwcm.putIfAbsent(a, c) == b) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap putIfAbsent works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap putIfAbsent doesn't work properly.");
                    }
                }
                //Testing remove
                {
                    DoublyWeakConcurrentMap<TrivialTestClass, TrivialTestClass> dwcm = new DoublyWeakConcurrentMap<>();
                    TrivialTestClass a = new TrivialTestClass(1);
                    TrivialTestClass b = new TrivialTestClass(2);
                    TrivialTestClass c = new TrivialTestClass(3);
                    TrivialTestClass d = new TrivialTestClass(4);

                    dwcm.put(a, b);
                    dwcm.put(b, c);
                    dwcm.put(c, d);

                    if (dwcm.remove(d) == null
                            && !dwcm.isEmpty()
                            && dwcm.size() == 3
                            && dwcm.get(a) == b
                            && dwcm.get(b) == c
                            && dwcm.get(c) == d) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap remove works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap remove doesn't work properly.");
                    }

                    if (dwcm.remove(a) == b
                            && !dwcm.isEmpty()
                            && dwcm.size() == 2
                            && dwcm.get(a) == null
                            && dwcm.get(b) == c
                            && dwcm.get(c) == d) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap remove works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap remove doesn't work properly.");
                    }

                    dwcm.put(a, b);

                    a = null;
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (dwcm.remove(a) == null && !dwcm.isEmpty() && dwcm.size() == 2) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap remove works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap remove doesn't work properly.");
                    }

                    a = new TrivialTestClass(1);
                    dwcm.put(a, b);

                    if (dwcm.remove(a, b)) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap remove works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap remove doesn't work properly.");
                    }

                    if (!dwcm.remove(a, b) && !dwcm.remove(d, a)) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap remove works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap remove doesn't work properly.");
                    }
                }
                //Testing replace
                {
                    DoublyWeakConcurrentMap<TrivialTestClass, TrivialTestClass> dwcm = new DoublyWeakConcurrentMap<>();
                    TrivialTestClass a = new TrivialTestClass(1);
                    TrivialTestClass b = new TrivialTestClass(2);
                    TrivialTestClass c = new TrivialTestClass(3);
                    TrivialTestClass d = new TrivialTestClass(4);

                    if (dwcm.replace(a, b) == null) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap replace works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap replace doesn't work properly.");
                    }

                    dwcm.put(a, b);

                    if (dwcm.replace(a, c) == b) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap replace works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap replace doesn't work properly.");
                    }

                    dwcm.remove(a);

                    if (dwcm.replace(a, b) == null) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap replace works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap replace doesn't work properly.");
                    }

                    dwcm.put(a, b);

                    a = null;
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);
                    a = new TrivialTestClass(1);

                    if (dwcm.replace(a, c) == null) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap replace works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap replace doesn't work properly.");
                    }

                    dwcm.put(a, b);

                    if (dwcm.replace(a, b, c)) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap replace works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap replace doesn't work properly.");
                    }

                    dwcm.put(a, b);

                    if (!dwcm.replace(a, c, b)) {
                        testGroup.ok(true, "DoublyWeakConcurrentMap replace works fine.");
                    } else {
                        testGroup.ok(false, "DoublyWeakConcurrentMap replace doesn't work properly.");
                    }
                }
            }),
            new ClientOnlyTest(5, "mokapot.util.EnumerationIterator",
            (communicator, address, testGroup) -> {
                //Test everything, except for iterator()
                {
                    ArrayList<Integer> al = new ArrayList<>();
                    al.add(1);
                    al.add(2);
                    al.add(3);
                    al.add(4);

                    Enumeration<Integer> enm = Collections.enumeration(al);
                    EnumerationIterator<Integer> ei = new EnumerationIterator<>(enm);

                    if (ei.hasNext()) {
                        testGroup.ok(true, "EnumerationIterator works fine.");
                    } else {
                        testGroup.ok(false, "EnumerationIterator didn't behave as expected.");
                    }

                    if (ei.next() == 1) {
                        testGroup.ok(true, "EnumerationIterator works fine.");
                    } else {
                        testGroup.ok(false, "EnumerationIterator didn't behave as expected.");
                    }
                    ei.next();
                    ei.next();

                    if (ei.next() == 4) {
                        testGroup.ok(true, "EnumerationIterator works fine.");
                    } else {
                        testGroup.ok(false, "EnumerationIterator didn't behave as expected.");
                    }

                    if (!ei.hasNext()) {
                        testGroup.ok(true, "EnumerationIterator works fine.");
                    } else {
                        testGroup.ok(false, "EnumerationIterator didn't behave as expected.");
                    }
                }
                //Test iterator()
                {
                    ArrayList<Integer> al = new ArrayList<>();
                    al.add(1);
                    al.add(2);
                    al.add(3);
                    al.add(4);

                    Enumeration<Integer> enm = Collections.enumeration(al);
                    EnumerationIterator<Integer> ei = new EnumerationIterator<>(enm);
                    Iterator<Integer> it = ei.iterator();
                    int good = 0;
                    for (Integer i : al) {
                        if (!(Objects.equals(i, it.next()))) {
                            testGroup.ok(false, "EnumerationIterator didn't behave as expected.");
                            break;
                        } else {
                            good++;
                        }
                    }
                    if (good == al.size()) {
                        testGroup.ok(true, "EnumerationIterator works fine.");
                    }
                }
            }),
            new ClientOnlyTest(5, "mokapot.util.ExpirableMap",
            (communicator, address, testGroup) -> {
                //Testing constructor
                {
                    ArrayList<Integer> a = new ArrayList<>();
                    ExpirableMap<TrivialExpirable, TrivialExpirable> em
                            = new ExpirableMap<>(10, TimeUnit.MILLISECONDS);
                    em.replace(new TrivialExpirable(), new TrivialExpirable());
                    em.replace(new TrivialExpirable(), new TrivialExpirable());
                    em.replace(new TrivialExpirable(), new TrivialExpirable());

                    Thread.sleep(15);
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    em.forEachKey((TrivialExpirable t) -> {
                        em.runMethodOn(t, (TrivialExpirable arg0) -> {
                            if (!t.expired) {
                                a.add(1);
                            }
                        }, null);
                    });
                    if (a.isEmpty()) {
                        testGroup.ok(true, "ExpirableMap constructor works as intended.");
                    } else {
                        testGroup.ok(false, "ExpirableMap constructor does not work as intended.");
                    }
                }
                //Testing clear and forEachKey
                {
                    ArrayList<Integer> a = new ArrayList<>();
                    ExpirableMap<TrivialExpirable, TrivialExpirable> em
                            = new ExpirableMap<>(10, TimeUnit.SECONDS);
                    em.replace(new TrivialExpirable(), new TrivialExpirable());
                    em.replace(new TrivialExpirable(), new TrivialExpirable());
                    em.replace(new TrivialExpirable(), new TrivialExpirable());

                    em.forEachKey((TrivialExpirable t) -> {
                        if (!t.expired) {
                            a.add(1);
                        }
                    });
                    if (a.size() == 3) {
                        testGroup.ok(true, "ExpirableMap forEachKey works as intended.");
                    } else {
                        testGroup.ok(false, "ExpirableMap forEachKey does not work as intended.");
                    }

                    a.clear();

                    em.clear();
                    em.forEachKey((TrivialExpirable t) -> {
                        if (!t.expired) {
                            a.add(1);
                        }
                    });
                    if (a.isEmpty()) {
                        testGroup.ok(true, "ExpirableMap forEachKey works as intended.");
                    } else {
                        testGroup.ok(false, "ExpirableMap forEachKey does not work as intended.");
                    }

                }
                //Testing replace
                {
                    ExpirableMap<TrivialExpirable, TrivialExpirable> em
                            = new ExpirableMap<>(10, TimeUnit.SECONDS);
                    TrivialExpirable a = new TrivialExpirable();
                    TrivialExpirable b = new TrivialExpirable();
                    TrivialExpirable c = new TrivialExpirable();
                    em.replace(a, b);
                    em.replace(a, c);
                    em.forEachKey((TrivialExpirable t) -> {
                        em.runMethodOn(t, (TrivialExpirable arg0) -> {
                            if (arg0 == c) {
                                testGroup.ok(true, "ExpirableMap replace works as intended.");
                            } else {
                                testGroup.ok(false, "ExpirableMap replace does not work as intended.");
                            }
                        }, null);
                    });
                }
                //testing runMethodOn
                {
                    ExpirableMap<TrivialExpirable, TrivialExpirable> em
                            = new ExpirableMap<>(10, TimeUnit.MILLISECONDS);
                    TrivialExpirable a = new TrivialExpirable();
                    TrivialExpirable b = new TrivialExpirable();
                    TrivialExpirable c = new TrivialExpirable();
                    ArrayList<TrivialExpirable> result = new ArrayList<>();
                    em.replace(a, b);

                    b.expire();
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    em.informOfExpiry(a, b);

                    result.add(em.runMethodOn(a, (TrivialExpirable arg0) -> {
                    }, () -> c));

                    if (result.get(0) == c) {
                        testGroup.ok(true, "ExpirableMap runMethodOn works as intended.");
                    } else {
                        testGroup.ok(false, "ExpirableMap runMethodOn does not work as intended.");
                    }
                }
            }),
            new ClientOnlyTest(1, "mokapot.util.ExtendedList",
            (communicator, address, testGroup) -> {
                ExtendedList<Integer> el = new ExtendedList<>(new ArrayList<>(), 1);
                ExtendedList<Integer> el2 = new ExtendedList<>(el, 2);
                ExtendedList<Integer> el3 = new ExtendedList<>(el2, 3);
                if (el3.get(0) == 1 && el3.get(1) == 2 && el3.get(2) == 3) {
                    testGroup.ok(true, "ExtendedList behaves normally.");
                } else {
                    testGroup.ok(false, "Unexpected behaviour from ExtendedList.");
                }
            }),
            new ClientOnlyTest(3, "mokapot.util.Holder",
            (communicator, address, testGroup) -> {
                Holder<Integer> h = new Holder<>();
                if (h.getValue() == null) {
                    testGroup.ok(true, "Holder is null when initialized.");
                } else {
                    testGroup.ok(false, "Holder is not null when initialized.");
                }

                h.setValue(1);

                if (h.setValue(4) == 1 && h.getValue() == 4) {
                    testGroup.ok(true, "Holder setValue works as expected.");
                } else {
                    testGroup.ok(false, "Holder setValue does not work as expected.");
                }

                if (h.setAndGet(6) == 6 && h.getValue() == 6) {
                    testGroup.ok(true, "Holder setAndGet works as expected.");
                } else {
                    testGroup.ok(false, "Holder setAndGet does not work as expected.");
                }
            }),
            new ClientOnlyTest(5, "mokapot.util.ImmutableSets",
            (communicator, address, testGroup) -> {
                Set<Integer> s = new HashSet<>();
                s.add(1);
                s.add(2);
                s.add(3);

                s = ImmutableSets.plusElement(s, 3);
                if (s.size() == 3 && s.contains(3)) {
                    testGroup.ok(true, "ImmutableSets plus element works.");
                } else {
                    testGroup.ok(false, "ImmutableSets plus element doesn't work properly.");
                }

                s = ImmutableSets.plusElement(s, 4);
                if (s.size() == 4 && s.contains(4)) {
                    testGroup.ok(true, "ImmutableSets plus element works.");
                } else {
                    testGroup.ok(false, "ImmutableSets plus element doesn't work properly.");
                }

                s = ImmutableSets.minusElement(s, 4);

                if (s.size() == 3 && !s.contains(4)) {
                    testGroup.ok(true, "ImmutableSets minus element works.");
                } else {
                    testGroup.ok(false, "ImmutableSets minus element doesn't work properly.");
                }

                s = ImmutableSets.minusElement(s, 3);

                if (s.size() == 2 && !s.contains(3)) {
                    testGroup.ok(true, "ImmutableSets minus element works.");
                } else {
                    testGroup.ok(false, "ImmutableSets minus element doesn't work properly.");
                }

                s = ImmutableSets.minusElement(s, 4);

                if (s.size() == 2 && !s.contains(4)) {
                    testGroup.ok(true, "ImmutableSets minus element works.");
                } else {
                    testGroup.ok(false, "ImmutableSets minus element doesn't work properly.");
                }
            }),
            new ClientOnlyTest(4, "mokapot.util.KeepalivePool",
            (communicator, address, testGroup) -> {
                //Test1
                {
                    KeepalivePool<TrivialExpirable> ka = new KeepalivePool<>();
                    TrivialExpirable te = new TrivialExpirable();
                    ka.keepAlive(te, 1000, TimeUnit.MILLISECONDS);
                    Thread.sleep(200);

                    if (!te.expired) {
                        testGroup.ok(true, "KeepalivePool kept object alive long enough.");
                    } else {
                        testGroup.ok(false, "KeepalivePool didn't keep object alive long enough.");
                    }

                    Thread.sleep(1200);
                    if (!te.expired) {
                        testGroup.ok(false, "KeepalivePool kept object alive more than expected.");
                    } else {
                        testGroup.ok(true, "KeepalivePool didn't keep object alive for longer than expected.");
                    }

                    TrivialExpirable te1 = new TrivialExpirable();
                    ka.keepAlive(te1, 10000, TimeUnit.MILLISECONDS);
                    ka.expireNow(te1, false);

                    if (te.expired) {
                        testGroup.ok(true, "KeepalivePool expired object when asked to.");
                    } else {
                        testGroup.ok(false, "KeepalivePool didn't expire object when asked to.");
                    }

                    TrivialExpirable te2 = new TrivialExpirable();
                    TrivialExpirable te3 = new TrivialExpirable();
                    TrivialExpirable te4 = new TrivialExpirable();
                    ka.keepAlive(te2, 10000, TimeUnit.MILLISECONDS);
                    ka.keepAlive(te3, 10000, TimeUnit.MILLISECONDS);
                    ka.keepAlive(te4, 10000, TimeUnit.MILLISECONDS);

                    ka.shutdown();
                    if (te2.expired && te3.expired && te4.expired) {
                        testGroup.ok(true, "KeepalivePool expired all objects on shutdown, just as expected.");
                    } else {
                        testGroup.ok(false, "KeepalivePool did not expire all objects on shutdown.");
                    }
                }
            }),
            new ClientOnlyTest(7, "mokapot.util.MutexPool",
            (communicator, address, testGroup) -> {
                //First test
                {
                    MutexPool<Integer> mp = new MutexPool<>();
                    try {
                        mp.lock(1);
                        mp.lock(2);
                        testGroup.ok(false, "MutexPool let one thread lock twice with different keys.");
                    } catch (IllegalStateException e) {
                        testGroup.ok(true, "MutexPool doesn't let one thread lock twice with different keys.");
                    }
                }
                //Second test
                {
                    MutexPool<Integer> mp = new MutexPool<>();
                    try {
                        mp.lock(1);
                        mp.lock(1);
                        testGroup.ok(false, "MutexPool let one thread lock twice with same key.");
                    } catch (IllegalStateException e) {
                        testGroup.ok(true, "MutexPool doesn't let one thread lock twice with same key.");
                    }
                }
                //Third test
                {
                    MutexPool<Integer> mp = new MutexPool<>();
                    try {
                        mp.lock(1);
                        mp.unlock();
                        mp.unlock();
                        testGroup.ok(false, "MutexPool let one thread unlock twice in a row.");
                    } catch (IllegalStateException e) {
                        testGroup.ok(true, "MutexPool did not let one thread unlock twice in a row.");
                    }
                }
                //Fourth test
                {
                    MutexPool<Integer> mp = new MutexPool<>();
                    try {
                        mp.unlock();
                        testGroup.ok(false, "MutexPool let one thread unlock without prior lock.");
                    } catch (IllegalStateException e) {
                        testGroup.ok(true, "MutexPool did not let one thread unlock without prior lock.");
                    }
                }
                //Fifth test
                {
                    MutexPool<Integer> mp = new MutexPool<>();
                    Thread a = new Thread() {
                        @Override
                        public void run() {
                            mp.lock(1);
                            try {
                                Thread.sleep(999999);
                            } catch (InterruptedException e) {
                                try {
                                    mp.unlock();
                                    testGroup.ok(true, "MutexPool did let the first thread unlock, as expected.");
                                } catch (IllegalStateException e1) {
                                    testGroup.ok(false, "MutexPool did not let first thread unlock.");
                                }
                            }
                        }
                    };
                    a.start();
                    while (a.getState() != Thread.State.TIMED_WAITING) {
                        Thread.sleep(1);
                    }

                    Thread b = new Thread() {
                        @Override
                        public void run() {
                            mp.lock(1);
                            mp.unlock();
                        }
                    };
                    b.start();
                    Thread.sleep(100);
                    if (b.isInterrupted()) {
                        testGroup.ok(false, "MutexPool blocked second thread using same key");
                    } else {
                        testGroup.ok(true, "MutexPool did not block second thread using same key, just as expected.");
                    }
                    a.interrupt();
                }
                //Sixth test
                {
                    MutexPool<Integer> mp = new MutexPool<>();
                    Thread a = new Thread() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 10000; i++) {
                                mp.lock(1);
                                mp.unlock();
                            }
                            for (int i = 0; i < 10000; i++) {
                                mp.lock(1);
                                mp.unlock();
                            }
                        }
                    };
                    Thread b = new Thread() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 10000; i++) {
                                mp.lock(1);
                                mp.unlock();
                            }
                            for (int i = 0; i < 10000; i++) {
                                mp.lock(2);
                                mp.unlock();
                            }
                        }
                    };

                    a.start();
                    b.start();
                    a.join();
                    b.join();
                    testGroup.ok(true, "MutexPool works just as expected.");
                }
            }),
            new ClientOnlyTest(4, "mokapot.util.ObjectIdentity",
            (communicator, address, testGroup) -> {
                TrivialTestClass ttc1 = new TrivialTestClass(5);
                TrivialTestClass ttc2 = new TrivialTestClass(10);
                ObjectIdentity<TrivialTestClass> ittc1 = new ObjectIdentity<>(ttc1);
                ObjectIdentity<TrivialTestClass> ittc2 = new ObjectIdentity<>(ttc2);

                if (ittc1.dereference() != ittc2.dereference()) {
                    testGroup.ok(true, "ObjectIdentity dereference not equal for different objects.");
                } else {
                    testGroup.ok(false, "ObjectIdentity dereference equal for different objects.");
                }

                if (ittc1.dereference().equals(ttc1)) {
                    testGroup.ok(true, "ObjectIdentity .equals returns true for referenced object.");
                } else {
                    testGroup.ok(false, "ObjectIdentity .equals returns false for referenced object.");
                }

                if (ittc1.hashCode() != ittc2.hashCode() && ittc1.hashCode() == ittc1.hashCode()) {
                    testGroup.ok(true, "ObjectIdentity hashCode works properly.");
                } else {
                    testGroup.ok(false, "ObjectIdentity hashCode does not work properly.");
                }

                ttc1 = ttc2;

                if (ittc1.equals(ittc2) || ittc1.hashCode() == ittc2.hashCode()) {
                    testGroup.ok(false, "ObjectIdentity should not change when variable used changes");
                } else {
                    testGroup.ok(true, "ObjectIdentity doesn't change when variable used changes.");
                }
            }),
            /* This test is disabled for now, until we remove the dependency on
               DistributedCommunicator; Java rightfully complains that we're
               trying to bypass the permissions system.

            new ClientOnlyTest(9, "mokapot.util.ObjectMethodDatabase",
            (communicator, address, testGroup) -> {
                ObjectMethodDatabase<SubclassOfClassForObjectMethodDatabase> o
                        = new ObjectMethodDatabase<>(SubclassOfClassForObjectMethodDatabase.class);

                if (o.getAbout().equals(SubclassOfClassForObjectMethodDatabase.class)) {
                    testGroup.ok(true, "ObjectMethodDatabase getAbout works fine.");
                } else {
                    testGroup.ok(false, "ObjectMethodDatabase getAbout does not work correctly.");
                }

                if (o.getClosestExtensibleSuperclass().equals(ClassForObjectMethodDatabase.class)) {
                    testGroup.ok(true, "ObjectMethodDatabase getClosestExtensibleSuperclass works fine.");
                } else {
                    testGroup.ok(false, "ObjectMethodDatabase getClosestExtensibleSuperclass does not work correctly.");
                }

                int total = 0;
                for (@SuppressWarnings("unused") Field f : o.getInstanceFieldList()) {
                    total++;
                }
                if (total == 2) {
                    testGroup.ok(true, "ObjectMethodDatabase getInstanceFieldList works fine.");
                } else {
                    testGroup.ok(false, "ObjectMethodDatabase getInstanceFieldList does not work correctly.");
                }

                if (o.getInterfaces().size() == 1) {
                    testGroup.ok(true, "ObjectMethodDatabase getInterfaces works fine.");
                } else {
                    testGroup.ok(false, "ObjectMethodDatabase getInterfaces does not work correctly.");
                }

                if (o.getMethods().size() == 16) {
                    testGroup.ok(true, "ObjectMethodDatabase getMethods works fine.");
                } else {
                    testGroup.ok(false, "ObjectMethodDatabase getMethods does not work correctly.");
                }

                if (o.hasNonInterfaceMethods()) {
                    testGroup.ok(true, "ObjectMethodDatabase hasNonInterfaceMethods works fine.");
                } else {
                    testGroup.ok(false, "ObjectMethodDatabase hasNonInterfaceMethods does not work correctly.");
                }

                if (o.hasOwnMethods()) {
                    testGroup.ok(true, "ObjectMethodDatabase hasOwnMethods works fine.");
                } else {
                    testGroup.ok(false, "ObjectMethodDatabase hasOwnMethods does not work correctly.");
                }

                if (!ObjectMethodDatabase.isClassExtensible(SubclassOfClassForObjectMethodDatabase.class)
                        && ObjectMethodDatabase.isClassExtensible(ClassForObjectMethodDatabase.class)) {
                    testGroup.ok(true, "ObjectMethodDatabase isClassExtensible works fine.");
                } else {
                    testGroup.ok(false, "ObjectMethodDatabase isClassExtensible does not work correctly.");
                }

                total = (int) o.getMethods().stream()
                        .map((m) -> ObjectMethodDatabase.methodSignature(m))
                        .filter((ms) -> (ms.equals("doNothing,java.lang.Integer")
                        || ms.equals("uselessMethod")
                        || ms.equals("returnFive")
                        || ms.equals("returnSomething"))).count();
                if (total == 4) {
                    testGroup.ok(true, "ObjectMethodDatabase methodSignature works fine.");
                } else {
                    testGroup.ok(false, "ObjectMethodDatabase methodSignature does not work correctly.");
                }
            }), */
            new ClientOnlyTest(3, "mokapot.util.Pair",
            (communicator, address, testGroup) -> {
                Pair<Integer, String> p = new Pair<>(1, "5");
                if (p.getFirst() == 1 && p.getSecond().equals("5")) {
                    testGroup.ok(true, "Pair getFirst and getSecond work fine.");
                } else {
                    testGroup.ok(false, "Pair getFirst and getSecond do not work correctly.");
                }

                Pair<Integer, Integer> p2 = p.mapSecond((s) -> Integer.parseInt(s));
                if (p2.getFirst() == 1 && p2.getSecond() == 5) {
                    testGroup.ok(true, "Pair mapSecond works fine.");
                } else {
                    testGroup.ok(false, "Pair mapSecond does not work correctly.");
                }

                Function<Integer, Pair<Integer, String>> f = Pair.preservingMap(x -> Integer.toString(x + 10));
                Pair<Integer, Pair<Integer, String>> t = new Pair<>(5, f.apply(5));
                if (t.getFirst() == 5 && t.getSecond().getFirst() == 5 && t.getSecond().getSecond().equals("15")) {
                    testGroup.ok(true, "Pair preservingMap works fine.");
                } else {
                    testGroup.ok(false, "Pair preservingMap does not work correctly.");
                }
            }),
            new ClientOnlyTest(7, "mokapot.util.ResettableThreadLocal",
            (communicator, address, testGroup) -> {
                //Test 1
                {
                    ResettableThreadLocal<Integer> rtl = new ResettableThreadLocal<>();
                    if (rtl.get() == null) {
                        testGroup.ok(true, "ResettableThreadLocal returned null, just as expected.");
                    } else {
                        testGroup.ok(false, "ResettableThreadLocal did not return null.");
                    }
                    Thread a = new Thread() {
                        @Override
                        public void run() {
                            rtl.set(1);
                            try {
                                Thread.sleep(1000000);
                            } catch (InterruptedException e) {
                                //Nothing to do.
                            }
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                // Nothing to do.
                            }

                            if (rtl.get() == 1) {
                                testGroup.ok(true, "ResettableThreadLocal holds correct value.");
                            } else {
                                testGroup.ok(false, "ResettableThreadLocal does not hold correct value.");
                            }
                        }
                    };
                    Thread b = new Thread() {
                        @Override
                        public void run() {
                            rtl.set(2);
                            try {
                                Thread.sleep(1000000);
                            } catch (InterruptedException e) {
                                // Nothing to do.
                            }
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                // Nothing to do.
                            }

                            if (rtl.get() == 2) {
                                testGroup.ok(true, "ResettableThreadLocal holds correct value.");
                            } else {
                                testGroup.ok(false, "ResettableThreadLocal does not hold correct value.");
                            }

                        }
                    };
                    a.start();
                    b.start();

                    rtl.set(3);
                    a.interrupt();
                    b.interrupt();

                    if (rtl.get() == 3) {
                        testGroup.ok(true, "ResettableThreadLocal holds correct value.");
                    } else {
                        testGroup.ok(false, "ResettableThreadLocal does not hold correct value.");
                    }

                    rtl.remove();

                    if (rtl.get() == null) {
                        testGroup.ok(true, "ResettableThreadLocal holds correct value.");
                    } else {
                        testGroup.ok(false, "ResettableThreadLocal does not hold correct value.");
                    }

                    a.join();
                    b.join();
                }
                //Test 2
                {
                    ResettableThreadLocal<Integer> rtl1 = new ResettableThreadLocal<>();
                    ResettableThreadLocal<Integer> rtl2 = new ResettableThreadLocal<>();
                    ResettableThreadLocal<Integer> rtl3 = new ResettableThreadLocal<>();

                    Thread a = new Thread() {
                        @Override
                        public void run() {
                            rtl1.set(1);
                            rtl2.set(2);
                            rtl3.set(3);
                            try {
                                Thread.sleep(100000);
                            } catch (InterruptedException e) {
                                // Nothing to do
                            }
                            if (rtl1.get() == 1 && rtl2.get() == 2 && rtl3.get() == 3) {
                                testGroup.ok(true, "ResettableThreadLocal holds correct values.");
                            } else {
                                testGroup.ok(false, "ResettableThreadLocal does not hold correct values.");
                            }
                        }
                    };
                    a.start();

                    rtl1.set(10);
                    rtl2.set(20);
                    rtl3.set(30);

                    ResettableThreadLocal.reset();

                    if (rtl1.get() == null && rtl2.get() == null && rtl3.get() == null) {
                        testGroup.ok(true, "ResettableThreadLocal holds correct values.");
                    } else {
                        testGroup.ok(false, "ResettableThreadLocal does not hold correct values.");
                    }
                    a.interrupt();
                    a.join();
                }
            }),
            new ClientOnlyTest(12, "mokapot.util.ThreadUtils",
            (communicator, address, testGroup) -> {
                //Testing unwrapAndRethrow
                {

                    try {
                        ThreadUtils.unwrapAndRethrow(new InvocationTargetException(null));
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow didn't throw.");
                    } catch (IllegalArgumentException e) {
                        testGroup.ok(true, "ThreadUtils unwrapAndRethrow works fine.");
                    } catch (RuntimeException | Error e) {
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow does not work correctly.");
                    }

                    try {
                        ThreadUtils.unwrapAndRethrow(new InvocationTargetException(new RuntimeException("AAAA")));
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow didn't throw.");
                    } catch (RuntimeException e) {
                        testGroup.ok(true, "ThreadUtils unwrapAndRethrow works fine.");
                    } catch (Error e) {
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow does not work correctly.");
                    }

                    try {
                        ThreadUtils.unwrapAndRethrow(new InvocationTargetException(new Error("AAAA")));
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow didn't throw.");
                    } catch (Error e) {
                        testGroup.ok(true, "ThreadUtils unwrapAndRethrow works fine.");
                    } catch (RuntimeException e) {
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow does not work correctly.");
                    }

                    try {
                        ThreadUtils.unwrapAndRethrow(new InvocationTargetException(new UndeclaredThrowableException(null, "AAAA")));
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow didn't throw.");
                    } catch (UndeclaredThrowableException e) {
                        testGroup.ok(true, "ThreadUtils unwrapAndRethrow works fine.");
                    } catch (RuntimeException | Error e) {
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow does not work correctly.");
                    }

                    try {
                        ThreadUtils.unwrapAndRethrow(new ExecutionException(null));
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow didn't throw.");
                    } catch (IllegalArgumentException e) {
                        testGroup.ok(true, "ThreadUtils unwrapAndRethrow works fine.");
                    } catch (RuntimeException | Error e) {
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow does not work correctly.");
                    }

                    try {
                        ThreadUtils.unwrapAndRethrow(new ExecutionException(new RuntimeException("AAAA")));
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow didn't throw.");
                    } catch (RuntimeException e) {
                        testGroup.ok(true, "ThreadUtils unwrapAndRethrow works fine.");
                    } catch (Error e) {
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow does not work correctly.");
                    }

                    try {
                        ThreadUtils.unwrapAndRethrow(new ExecutionException(new Error("AAAA")));
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow didn't throw.");
                    } catch (Error e) {
                        testGroup.ok(true, "ThreadUtils unwrapAndRethrow works fine.");
                    } catch (RuntimeException e) {
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow does not work correctly.");
                    }

                    try {
                        ThreadUtils.unwrapAndRethrow(new ExecutionException(new UndeclaredThrowableException(null, "AAAA")));
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow didn't throw.");
                    } catch (UndeclaredThrowableException e) {
                        testGroup.ok(true, "ThreadUtils unwrapAndRethrow works fine.");
                    } catch (RuntimeException | Error e) {
                        testGroup.ok(false, "ThreadUtils unwrapAndRethrow does not work correctly.");
                    }
                }
                //Testing delayInterruptionsRv with 0 interruptions
                {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            ClassWithSlowConstructor c = ThreadUtils.delayInterruptionsRv(() -> new ClassWithSlowConstructor());
                            if (c instanceof ClassWithSlowConstructor && !Thread.interrupted()) {
                                testGroup.ok(true, "ThreadUtils delayInterruptionsRv works fine.");
                            } else {
                                testGroup.ok(false, "ThreadUtils delayInterruptionsRv does not work correctly.");
                            }
                        }
                    };
                    t.start();
                    t.join();
                }
                //Testing delayInterruptionsRV with multiple interruptions
                {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            ClassWithSlowConstructor c = ThreadUtils.delayInterruptionsRv(() -> new ClassWithSlowConstructor());
                            if (c instanceof ClassWithSlowConstructor && Thread.interrupted()) {
                                testGroup.ok(true, "ThreadUtils delayInterruptionsRv works fine.");
                            } else {
                                testGroup.ok(false, "ThreadUtils delayInterruptionsRv does not work correctly.");
                            }
                        }
                    };
                    t.start();

                    for (int i = 0; i < 10; i++) {
                        t.interrupt();
                    }

                    t.join();
                }

                ThreadUtils.InterruptiblyRunnable ir = () -> {
                    ClassWithSlowConstructor c = new ClassWithSlowConstructor();
                };
                //Testing delayInterruptions with 0 interruptions
                {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            ThreadUtils.delayInterruptions(ir);
                            if (!Thread.interrupted()) {
                                testGroup.ok(true, "ThreadUtils delayInterruptions works fine.");
                            } else {
                                testGroup.ok(false, "ThreadUtils delayInterruptions does not work correctly.");
                            }
                        }
                    };
                    t.start();
                    Thread.sleep(50);
                    t.join();
                }
                //Testing delayInterruptions with multiple interruptions
                {
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            ThreadUtils.delayInterruptions(ir);
                            if (Thread.interrupted()) {
                                testGroup.ok(true, "ThreadUtils delayInterruptions works fine.");
                            } else {
                                testGroup.ok(false, "ThreadUtils delayInterruptions does not work correctly.");
                            }
                        }
                    };
                    t.start();
                    for (int i = 0; i < 10; i++) {
                        t.interrupt();
                    }
                    t.join();
                }
            }),
            new ClientOnlyTest(6, "mokapot.util.TypeSafe",
            (communicator, address, testGroup) -> {
                boolean a1 = true;
                byte a2 = Byte.MAX_VALUE;
                char a3 = 'a';
                double a4 = 1;
                float a5 = 1.5f;
                int a6 = 1;
                long a7 = 1;
                TrivialTestClass a8 = new TrivialTestClass(1);
                if (TypeSafe.valueCast(a1, Boolean.class) == true
                        && TypeSafe.valueCast(a2, Byte.class) == Byte.MAX_VALUE
                        && TypeSafe.valueCast(a3, Character.class) == 'a'
                        && TypeSafe.valueCast(a4, Double.class) == 1
                        && TypeSafe.valueCast(a5, Float.class) == 1.5f
                        && TypeSafe.valueCast(a6, Integer.class) == 1
                        && TypeSafe.valueCast(a7, Long.class) == 1
                        && TypeSafe.valueCast(a8, TrivialTestClass.class) == TrivialTestClass.class.cast(a8)) {
                    testGroup.ok(true, "TypeSafe valueCast works fine.");
                } else {
                    testGroup.ok(false, "TypeSafe valueCast doesn't work correctly.");
                }

                if (TypeSafe.classCast(ComparablePair.class, Pair.class, null) == ComparablePair.class) {
                    testGroup.ok(true, "TypeSafe classCast casted as expected.");
                } else {
                    testGroup.ok(false, "TypeSafe classCast did not cast as expected.");
                }

                ClassWithGeneric<ComparablePair<Integer, Integer>> a = new ClassWithGeneric<>();
                ClassWithGeneric<Pair<Integer, Integer>> b = new ClassWithGeneric<>();

                if (TypeSafe.classCast(a.getClass(), b.getClass(), null) == b.getClass() && a.getClass() == b.getClass()) {
                    testGroup.ok(true, "TypeSafe classCast casted as expected.");
                } else {
                    testGroup.ok(false, "TypeSafe classCast did not cast as expected.");
                }

                if (TypeSafe.classCast(Pair.class, ComparablePair.class, null) == null) {
                    testGroup.ok(true, "TypeSafe classCast casted as expected.");
                } else {
                    testGroup.ok(false, "TypeSafe classCast did not cast as expected.");
                }

                List<Integer> l = new ArrayList<>();
                if (TypeSafe.getActualClass(l).toString().equals("class java.util.ArrayList")) {
                    testGroup.ok(true, "TypeSafe getActualClass works as expected.");
                } else {
                    testGroup.ok(false, "TypeSafe getActualClass doesn't work as expected.");
                }

                if (TypeSafe.getInterfacesAndSuperclass(l.getClass()).size() == 5) {
                    testGroup.ok(true, "TypeSafe getInterfacesAndSuperclass answer length is correct.");
                } else {
                    testGroup.ok(false, "TypeSafe getInterfacesAndSUperclass answer length is incorrect.");
                }
            }),
            new ClientOnlyTest(3, "mokapot.util.VMInfo",
            (communicator, address, testGroup) -> {
                if (!VMInfo.isRunningOnAndroid()) {
                    testGroup.ok(true, "JVM is not running on android, as expected.");
                } else {
                    testGroup.ok(false, "JVM is not running on android, but VJMInfo says otherwise.");
                }

                if (VMInfo.isClassNameInSealedPackage("java.lang.String")) {
                    testGroup.ok(true, "VMInfo says String is a sealed package, which is true.");
                } else {
                    testGroup.ok(false, "VMInfo says String is not in a sealed package, which is false.");
                }

                if (VMInfo.isClassNameInSealedPackage("xyz.acygn.mokapot.test.TestMain")) {
                    testGroup.ok(false, "VMInfo says TestMain is in a sealed package, which is not the case.");
                } else {
                    testGroup.ok(true, "VMInfo says TestMain is not in a sealed package, which is true.");
                }
            }),
            new ClientOnlyTest(7, "mokapot.util.WeakConcurrentSet",
            (communicator, address, testGroup) -> {
                WeakConcurrentSet<TrivialTestClass> wcs = new WeakConcurrentSet<>();
                if (wcs.size() == 0 && wcs.isEmpty()) {
                    testGroup.ok(true, "WeakConcurrentSet size is 0 at initialization.");
                } else {
                    testGroup.ok(false, "WeakConcurrentSet size is not 0 at initialization.");
                }

                TrivialTestClass t1 = new TrivialTestClass(5);
                TrivialTestClass t2 = new TrivialTestClass(6);
                TrivialTestClass t3 = new TrivialTestClass(7);
                TrivialTestClass t4 = new TrivialTestClass(7);

                if (wcs.add(t1) && wcs.size() == 1 && !wcs.isEmpty()) {
                    testGroup.ok(true, "WeakConcurrentSet added new element.");
                } else {
                    testGroup.ok(false, "WeakConcurrentSet did not add new element.");
                }

                if (!wcs.add(t1) && wcs.size() == 1) {
                    testGroup.ok(true, "WeakConcurrentSet did not add duplicate element.");
                } else {
                    testGroup.ok(false, "WeakConcurrentSet added duplicate element.");
                }

                if (wcs.contains(t1) && !wcs.contains(t2)) {
                    testGroup.ok(true, "WeakConcurrentSet contains works fine.");
                } else {
                    testGroup.ok(false, "WeakConcurrentSet contains doesn't work properly.");
                }

                wcs.add(t2);
                wcs.add(t3);
                wcs.add(t4);

                t2 = null;
                System.gc();
                System.runFinalization();
                Thread.sleep(50);

                if (wcs.size() == 3 && !wcs.contains(t2)) {
                    testGroup.ok(true, "WeakConcurrentSet did not keep element alive.");
                } else {
                    testGroup.ok(false, "WeakConcurrentSet kept element alive, and it shouldn't have.");
                }

                wcs.remove(t1);

                if (wcs.size() == 2 && !wcs.contains(t1)) {
                    testGroup.ok(true, "WeakConcurrentSet remove works.");
                } else {
                    testGroup.ok(false, "WeakConcurrentSet remove doesn't work.");
                }

                wcs.clear();

                if (wcs.size() == 0 && wcs.isEmpty() && !wcs.contains(t1) && !wcs.contains(t2) && !wcs.contains(t3)) {
                    testGroup.ok(true, "WeakConcurrentSet clear works fine.");
                } else {
                    testGroup.ok(false, "WeakConcurrentSet clear not working properly.");
                }
            }),
            new ClientOnlyTest(18, "mokapot.util.WeakValuedConcurrentMap",
            (communicator, address, testGroup) -> {
                //Test 1
                {
                    WeakValuedConcurrentMap<Integer, Integer> m = new WeakValuedConcurrentMap<>();
                    if (m.isEmpty()) {
                        testGroup.ok(true, "WeakValuedConcurrentMap is empty, just as expected.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap is not empty.");
                    }

                    int i = 1;
                    if (m.put(1, i) == null && m.isEmpty()) {
                        testGroup.ok(false, "WeakValuedConcurrentMap is empty, although it shouldn't be.");
                    } else {
                        testGroup.ok(true, "WeakValuedConcurrentMap is not empty.");
                    }
                }
                //Test 2
                {
                    WeakValuedConcurrentMap<Integer, TrivialTestClass> m = new WeakValuedConcurrentMap<>();
                    TrivialTestClass t = new TrivialTestClass(5);

                    if (m.put(1, t) == null && m.keySet().size() == 1) {
                        testGroup.ok(true, "WeakValuedConcurrentMap keyset of right size.");
                    } else {
                        testGroup.ok(true, "WeakValuedConcurrentMap keyset not of right size.");
                    }

                    t = null;
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);
                    if (m.isEmpty()) {
                        testGroup.ok(true, "WeakValuedConcurrentMap is empty, just as expected.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap is not empty.");
                    }

                    if (m.keySet().size() == 0) {
                        testGroup.ok(true, "WeakValuedConcurrentMap keyset of right size.");
                    } else {
                        testGroup.ok(true, "WeakValuedConcurrentMap keyset not of right size.");
                    }
                }
                //Test 3
                {
                    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                    WeakValuedConcurrentMap<Integer, Integer> m = new WeakValuedConcurrentMap<>();
                    if (!m.containsKey(1)) {
                        testGroup.ok(true, "WeakValuedConcurrentMap does not contain inexistent key.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap contains inexistent key.");
                    }
                }
                //Test 4
                {
                    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                    WeakValuedConcurrentMap<Integer, Integer> m = new WeakValuedConcurrentMap<>();
                    if (!m.containsValue(1)) {
                        testGroup.ok(true, "WeakValuedConcurrentMap does not contain inexistent value.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap contains inexistent value.");
                    }
                }
                //Test 5
                {
                    WeakValuedConcurrentMap<Integer, Integer> m = new WeakValuedConcurrentMap<>();
                    int i = 5;

                    if (m.put(1, i) == null && m.get(0) == null && m.get(1) == 5) {
                        testGroup.ok(true, "WeakValuedConcurrentMap get works properly.");
                    } else {
                        testGroup.ok(true, "WeakValuedConcurrentMap get does not work properly.");
                    }
                }
                //Test 6
                {
                    WeakValuedConcurrentMap<Integer, Integer> m = new WeakValuedConcurrentMap<>();
                    if (m.put(1, 1) == null && m.put(1, 2) == 1 && m.get(1) == 2) {
                        testGroup.ok(true, "WeakValuedConcurrentMap replaces values with put, just as expected.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap does not replace values with put.");
                    }
                }
                //Test 7
                {
                    WeakValuedConcurrentMap<Integer, Integer> m = new WeakValuedConcurrentMap<>();
                    Map<Integer, Integer> otherM = new HashMap<>();
                    otherM.put(1, 1);
                    otherM.put(2, 2);
                    m.putAll(otherM);
                    if (m.get(1) == 1 && m.get(2) == 2 && m.keySet().size() == 2) {
                        testGroup.ok(true, "WeakValuedConcurrentMap putall works fine.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap putall does not work fine.");
                    }
                }
                //Test 8
                {
                    WeakValuedConcurrentMap<Integer, TrivialTestClass> m = new WeakValuedConcurrentMap<>();
                    TrivialTestClass t1 = new TrivialTestClass(1);
                    TrivialTestClass t2 = new TrivialTestClass(2);

                    if (m.putIfAbsent(1, t1) == null && m.putIfAbsent(1, t2) == t1 && m.get(1).getData() == 1) {
                        testGroup.ok(true, "WeakValuedConcurrentMap putIfAbsent didn't replace existing object.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap putIfAbsent replaced existing object.");
                    }

                    t1 = null;
                    System.gc();
                    System.runFinalization();
                    Thread.sleep(50);

                    if (m.putIfAbsent(1, t2) == null && m.get(1).getData() == 2) {
                        testGroup.ok(true, "WeakValuedConcurrentMap putIfAbsent replaced deallocated object.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap putIfAbsent did not replace deallocated object.");
                    }
                }
                //Test 9
                {
                    WeakValuedConcurrentMap<Integer, Integer> m = new WeakValuedConcurrentMap<>();
                    if (m.put(1, 1) == null && m.remove(1) == 1 && m.remove(2) == null && m.get(1) == null && m.get(2) == null) {
                        testGroup.ok(true, "WeakValuedConcurrentMap remove seems to be working properly.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap remove is not working properly.");
                    }

                    if (m.put(1, 1) == null && !m.remove(1, 2) && m.get(1) == 1) {
                        testGroup.ok(true, "WeakValuedConcurrentMap remove doesn't remove nonexistent key/value pair");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap removed value when it shouldn't have.");
                    }
                }
                //Test 10
                {
                    WeakValuedConcurrentMap<Integer, Integer> m = new WeakValuedConcurrentMap<>();
                    if (m.replace(1, 1) == null && m.get(1) == null) {
                        testGroup.ok(true, "WeakValuedConcurrentMap replace doesn't map over null value");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap replace maps value over emtpy.");
                    }
                    if (m.put(1, 1) == null && m.replace(1, 2) == 1 && m.get(1) == 2) {
                        testGroup.ok(true, "WeakValuedConcurrentMap replace works correctly.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap replace does not replace correctly.");
                    }
                    if (m.replace(1, 2, 1) && m.get(1) == 1) {
                        testGroup.ok(true, "WeakValuedConcurrentMap replace works correctly.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap replace does not replace correctly.");
                    }
                    if (!m.replace(1, 2, 3) && m.get(1) == 1) {
                        testGroup.ok(true, "WeakValuedConcurrentMap replace works correctly.");
                    } else {
                        testGroup.ok(false, "WeakValuedConcurrentMap replace does not replace correctly.");
                    }
                }
            })
        };

        TestGroup allTests = new SerialTests("main test", testArray);

        allTests.plan();

        allTests.runTest(1);

        System.exit(TestGroup.getTestingStatus(true));

    }

    /**
     * A test which runs entirely on one client, no server involved. The client
     * test code will run with <code>null</code> for the server address. This is
     * a stripped down version of ClientOnlyTest from mokapot.test.TestMain,
     * which was originally written by somebody else.
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
         * Runs <code>testCode</code>.
         *
         * @throws java.lang.Exception
         */
        @Override
        protected void testImplementation() throws Exception {
            testCode.testCode(null, null, this);
        }
    }
}
