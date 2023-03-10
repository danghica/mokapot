/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xyz.acygn.millr;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.acygn.millr.DeprecatedClasses.HigherOrderUnwrapCode;

import static org.junit.Assert.*;

/**
 *
 * @author thomasc
 */
public class HigherOrderUnwrapCodeTest {
    
    public HigherOrderUnwrapCodeTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getUnwrap method, of class HigherOrderUnwrapCode.
     */
    @Test
    public void testGetUnwrap() {
        System.out.println("getUnwrap");
        Object a = null;
        Object expResult = null;
        Object result = HigherOrderUnwrapCode.getUnwrap(a);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getWrap method, of class HigherOrderUnwrapCode.
     */
    @Test
    public void testGetWrap() {
        System.out.println("getWrap");
        Object copy = null;
        Object expResult = null;
        Object result = HigherOrderUnwrapCode.getWrap(copy);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of doesItNeedsWrapping method, of class HigherOrderUnwrapCode.
     */
    @Test
    public void testDoesItNeedsWrapping() {
        System.out.println("doesItNeedsWrapping");
        Object a = null;
        boolean expResult = false;
        boolean result = HigherOrderUnwrapCode.doesItNeedsWrapping(a);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
