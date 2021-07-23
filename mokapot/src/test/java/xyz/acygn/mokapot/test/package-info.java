/**
 * Test code for the distributed computation system.
 * <p>
 * This package mostly consists of two halves. One half implements the
 * <a href="http://testanything.org">TAP protocol</a>, which is used to allow
 * the tests to be automated via an external test driver. The other half is code
 * specific to the distributed communication system, testing various components
 * to ensure that they work correctly.
 * <p>
 * This package mostly focuses on integration tests rather than unit tests
 * (although basic functionality is tested independently before more advanced
 * functionality that depends on it is tested). The <code>TestMain</code> class
 * contains the bulk of the code for the actual tests, and can be run as a
 * program in order to run the tests.
 * <p>
 * @see xyz.acygn.mokapot.test.TestMain
 */
package xyz.acygn.mokapot.test;
