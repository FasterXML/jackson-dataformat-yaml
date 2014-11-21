package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class TestVersions extends ModuleTestBase
{
    @SuppressWarnings("resource")
    public void testMapperVersions() throws IOException
    {
        YAMLFactory f = new YAMLFactory();
        assertVersion(f);
        YAMLParser jp = (YAMLParser) f.createParser("123");
        assertVersion(jp);
        jp.close();
        YAMLGenerator jgen = (YAMLGenerator) f.createGenerator(new ByteArrayOutputStream());
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

