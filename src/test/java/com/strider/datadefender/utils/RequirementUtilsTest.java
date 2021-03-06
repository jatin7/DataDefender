/*
 *
 * Copyright 2014, Armenak Grigoryan, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.strider.datadefender.utils;

import com.strider.datadefender.database.DatabaseAnonymizerException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.strider.datadefender.database.metadata.MatchMetaData;
import com.strider.datadefender.requirement.Column;
import com.strider.datadefender.requirement.Key;
import com.strider.datadefender.requirement.Parameter;
import com.strider.datadefender.requirement.Requirement;
import com.strider.datadefender.requirement.Table;

/**
 * Unit test to test requirement related utility methods
 *
 * @author Matthew Eaton
 */
public class RequirementUtilsTest extends TestCase {
    private Requirement requirement;
    
    private static final String TEST_FILE_NAME = "target/test-classes/utest-req-write.xml";
    private static final String STRING_TYPE = "String";

    // Setup load-related tests
    private void load() throws DatabaseAnonymizerException {
        load(getClass().getResource("/Requirement.xml").getFile());
    }
    private void load(final String fname) throws DatabaseAnonymizerException {
        requirement = RequirementUtils.load(fname);
        assertNotNull(requirement);
    }
    
    // Doesn't tests a reading a writing a simple requirement
    public void testSimpleWriteRead() throws Exception {
        // create
        final Requirement req = new Requirement();
        req.setClient("Unit test client.");
        req.setVersion("X.X");
        
        final Table table = new Table();
        table.setName("Table name");
        table.setPkey("pKey");
        
        final Column col = new Column();
        col.setName("cName");
        col.setFunction("some.function()");
        
        final Parameter param = new Parameter();
        param.setName("pName");
        param.setValue("pvalue/file.txt");
        param.setType(STRING_TYPE);
        
        col.setParameters(Arrays.asList(param));
        table.setColumns(Arrays.asList(col));
        req.setTables(Arrays.asList(table));
        
        // write
        RequirementUtils.write(req, TEST_FILE_NAME);
        
        // read and test
        load(TEST_FILE_NAME);
        assertEquals("Unit test client.", requirement.getClient());
        assertEquals("X.X", requirement.getVersion());
        assertEquals(1, requirement.getTables().size());
        final Table t1 = requirement.getTables().get(0);
        assertEquals("Table name", t1.getName());
        assertEquals("pKey", t1.getPKey());
        assertEquals(1, t1.getColumns().size());
        final Column c1 = t1.getColumns().get(0);
        assertEquals("cName", c1.getName());
        assertEquals("some.function()", c1.getFunction());
        assertEquals(1, c1.getParameters().size());
        final Parameter p1 = c1.getParameters().get(0);
        assertEquals("pName", p1.getName());
        assertEquals("pvalue/file.txt", p1.getValue());
        assertEquals(STRING_TYPE, p1.getType());
    }
    
    @SuppressWarnings("serial")
    public void testCreate() { // just test the important stuff for now (as we aren't sure if the defaults make sense)
        final List<MatchMetaData> list = new ArrayList<MatchMetaData>() {{  
            add(new MatchMetaData(null, "t1", Arrays.asList("pk"), "col1", STRING_TYPE, 5)); // one pk, 1 column
            add(new MatchMetaData(null, "t2", Arrays.asList("pk1", "pk2"), "col1", STRING_TYPE, 5)); // two pk, two columns
            add(new MatchMetaData(null, "t2", Arrays.asList("pk1", "pk2"), "col2", STRING_TYPE, 5));
        }};
        final Requirement req = RequirementUtils.create(list);
        assertEquals("Autogenerated Template Client", req.getClient());
        assertEquals("1.0", req.getVersion());
        assertEquals(2, req.getTables().size());
        
        final Table t1 = req.getTables().get(0);
        assertEquals("t1", t1.getName());
        assertEquals("pk", t1.getPKey());
        assertEquals(1, t1.getColumns().size());
        final Column c1 = t1.getColumns().get(0);
        assertEquals("col1", c1.getName());
        assertEquals(STRING_TYPE, c1.getReturnType());

        // default function
        assertEquals("com.strider.dataanonymizer.functions.CoreFunctions.randomStringFromFile", c1.getFunction());
        assertEquals(1, c1.getParameters().size());
        
        final Table t2 = req.getTables().get(1);
        assertEquals("t2", t2.getName());
        assertEquals(2, t2.getPrimaryKeys().size());
        assertEquals(2, t2.getColumns().size());
    }

    /**
     * Test loaded requirements from file
     * @throws Exception
     */
    public void testLoad() throws Exception {
        load();
        assertEquals("Test Client", requirement.getClient());
        assertEquals("1.0", requirement.getVersion());
        assertEquals(3, requirement.getTables().size());
        assertEquals("test_table", requirement.getTables().get(0).getName());

        int columnNo = 0;
        for (final Column column : requirement.getTables().get(0).getColumns()) {
            assertEquals(("column" + ++columnNo), column.getName());
        }
        
        final List<Key> pKeys = requirement.getTables().get(1).getPrimaryKeys();
        assertNotNull(pKeys);
        assertEquals(2, pKeys.size());
        assertEquals("id1", pKeys.get(0).getName());
        assertEquals("id2", pKeys.get(1).getName());

        assertEquals(5, columnNo);
    }

    /**
     * Test getting parameters with name "file"
     * @author Matthew Eaton
     * @throws Exception
     */
    public void testGetFileParameter() throws Exception {
        load();

        int columnNo = 0;
        for (final Column column : requirement.getTables().get(0).getColumns()) {
            if ("column1".equals(column.getName()) || "column2".equals(column.getName())) {
                final Parameter parameter = RequirementUtils.getFileParameter(column.getParameters());
                assertNotNull(parameter);
                assertEquals(RequirementUtils.PARAM_NAME_FILE, parameter.getName());
            } else {
                final Parameter parameter = RequirementUtils.getFileParameter(column.getParameters());
                assertNull(parameter);
            }
            assertEquals(("column" + ++columnNo), column.getName());
        }
    }
    
    public void testGetPrimitiveParameterValues() throws Exception {
        load();

        final List<Parameter> params = requirement.getTables().get(2).getColumns().get(0).getParameters();
        assertNotNull(params);
        
        assertEquals(true, (boolean) params.get(0).getTypeValue());
        assertEquals(Boolean.class, params.get(0).getTypeValue().getClass());
        assertEquals(1, (byte) params.get(1).getTypeValue());
        assertEquals(Byte.class, params.get(1).getTypeValue().getClass());
        assertEquals(2, (short) params.get(2).getTypeValue());
        assertEquals(Short.class, params.get(2).getTypeValue().getClass());
        assertEquals('s', (char) params.get(3).getTypeValue());
        assertEquals(Character.class, params.get(3).getTypeValue().getClass());
        assertEquals(-3, (int) params.get(4).getTypeValue());
        assertEquals(Integer.class, params.get(4).getTypeValue().getClass());
        assertEquals(4, (long) params.get(5).getTypeValue());
        assertEquals(Long.class, params.get(5).getTypeValue().getClass());
        assertEquals(0.25f, (float) params.get(6).getTypeValue());
        assertEquals(Float.class, params.get(6).getTypeValue().getClass());
        assertEquals(0.5, (double) params.get(7).getTypeValue());
        assertEquals(Double.class, params.get(7).getTypeValue().getClass());
    }
    
    public void testGetStringArrayParameterValue() throws Exception {
        load();

        final List<Parameter> params = requirement.getTables().get(2).getColumns().get(1).getParameters();
        assertNotNull(params);
        
        final Object ret = params.get(0).getTypeValue();
        assertNotNull(ret);
        
        final String[] arr = (String[]) ret;
        assertEquals(3, arr.length);
        
        assertEquals("column1", arr[0]);
        assertEquals("column2", arr[1]);
        assertEquals("column3", arr[2]);
    }
    
    public void testGetPrimitiveArrayParameterValue() throws Exception {
        load();

        List<Parameter> params = requirement.getTables().get(2).getColumns().get(2).getParameters();
        assertNotNull(params);
        
        Object ret = params.get(0).getTypeValue();
        assertNotNull(ret);
        
        final int[] arr = (int[]) ret;
        assertEquals(3, arr.length);
        
        assertEquals(1, arr[0]);
        assertEquals(10, arr[1]);
        assertEquals(-20, arr[2]);
        
        params = requirement.getTables().get(2).getColumns().get(2).getParameters();
        assertNotNull(params);
        
        ret = params.get(1).getTypeValue();
        assertNotNull(ret);
        
        final double[] dArr = (double[]) ret;
        assertEquals(3, dArr.length);
        
        assertEquals(1.2, dArr[0]);
        assertEquals(10.5, dArr[1]);
        assertEquals(-20.1, dArr[2]);
    }
}