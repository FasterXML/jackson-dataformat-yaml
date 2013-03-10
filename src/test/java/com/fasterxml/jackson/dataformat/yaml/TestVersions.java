package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestVersions extends ModuleTestBase
{
    public void testMapperVersions() throws IOException
    {
        YAMLFactory f = new YAMLFactory();
        assertVersion(f);
        YAMLParser jp = (YAMLParser) f.createJsonParser("123");
        assertVersion(jp);
        YAMLGenerator jgen = (YAMLGenerator) f.createJsonGenerator(new ByteArrayOutputStream());
        assertVersion(jgen);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void assertVersion(Versioned vers)
    {
        assertEquals(PackageVersion.VERSION, vers.version());
    }
}

