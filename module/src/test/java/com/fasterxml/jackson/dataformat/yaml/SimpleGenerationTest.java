package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;
import java.util.Iterator;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SimpleGenerationTest extends ModuleTestBase
{
    public void testStreamingArray() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        StringWriter w = new StringWriter();
        JsonGenerator gen = f.createGenerator(w);
        gen.writeStartArray();
        gen.writeNumber(3);
        gen.writeString("foobar");
        gen.writeEndArray();
        gen.close();

        String yaml = w.toString();
        // should probably parse?
        // note: 1.12 uses more compact notation; 1.10 has prefix
        yaml = trimDocMarker(yaml).trim();
        assertEquals("- 3\n- \"foobar\"", yaml);
    }

    public void testStreamingObject() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        StringWriter w = new StringWriter();
        @SuppressWarnings("resource")
        JsonGenerator gen = f.createGenerator(w);
        _writeBradDoc(gen);
        String yaml = w.toString();

        // note: 1.12 uses more compact notation; 1.10 has prefix
        yaml = trimDocMarker(yaml).trim();
        assertEquals("name: \"Brad\"\nage: 39", yaml);
    }
    
    public void testBasicPOJO() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        FiveMinuteUser user = new FiveMinuteUser("Bob", "Dabolito", false,
                FiveMinuteUser.Gender.MALE, new byte[] { 1, 3, 13, 79 });
        String yaml = mapper.writeValueAsString(user).trim();
        String[] parts = yaml.split("\n");
        boolean gotHeader = (parts.length == 6);
        if (!gotHeader) {
            // 1.10 has 6 as it has header
            assertEquals(5, parts.length);
        }
        // unify ordering, need to use TreeSets
        TreeSet<String> exp = new TreeSet<String>();
        for (String part : parts) {
            exp.add(part.trim());
        }
        Iterator<String> it = exp.iterator();
        if (gotHeader) {
            assertEquals("---", it.next());
        }
        assertEquals("firstName: \"Bob\"", it.next());
        assertEquals("gender: \"MALE\"", it.next());
        assertEquals("lastName: \"Dabolito\"", it.next());
        assertEquals("userImage: \"AQMNTw==\"", it.next());
        assertEquals("verified: false", it.next());
    }

    // Issue#12:
    public void testWithFile() throws Exception
    {
        File f = File.createTempFile("test", ".yml");
        f.deleteOnExit();
        ObjectMapper mapper = mapperForYAML();
        mapper.writeValue(f, "Foobar");
        assertTrue(f.canRead());
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
                f), "UTF-8"));
        String doc = br.readLine();
        String str = br.readLine();
        if (str != null) {
            doc += "\n" + str;
        }
        doc = trimDocMarker(doc);
        assertEquals("\"Foobar\"", doc);
        br.close();
        f.delete();
    }

    @SuppressWarnings("resource")
    public void testStartMarker() throws Exception
    {
        YAMLFactory f = new YAMLFactory();

        // Ok, first, assume we do get the marker:
        StringWriter w = new StringWriter();
        assertTrue(f.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        YAMLGenerator gen = f.createGenerator(w);
        assertTrue(gen.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        _writeBradDoc(gen);
        String yaml = w.toString().trim();
        assertEquals("---\nname: \"Brad\"\nage: 39", yaml);

        // and then, disabling, and not any more
        f.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        assertFalse(f.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        w = new StringWriter();
        gen = f.createGenerator(w);
        assertFalse(gen.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        _writeBradDoc(gen);
        yaml = w.toString().trim();
        assertEquals("name: \"Brad\"\nage: 39", yaml);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */
    
    
    protected void _writeBradDoc(JsonGenerator gen) throws IOException
    {
        gen.writeStartObject();
        gen.writeStringField("name", "Brad");
        gen.writeNumberField("age", 39);
        gen.writeEndObject();
        gen.close();
    }
}
