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
        JsonGenerator gen = f.createJsonGenerator(w);
        gen.writeStartArray();
        gen.writeNumber(3);
        gen.writeString("foobar");
        gen.writeEndArray();
        gen.close();
        
        String yaml = w.toString();
        // should probably parse...
        assertEquals("---\n- 3\n- \"foobar\"\n", yaml);
    }

    public void testStreamingObject() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        StringWriter w = new StringWriter();
        JsonGenerator gen = f.createJsonGenerator(w);
        gen.writeStartObject();
        gen.writeStringField("name", "Brad");
        gen.writeNumberField("age", 39);
        gen.writeEndObject();
        gen.close();
        
        String yaml = w.toString();
        assertEquals("---\nname: \"Brad\"\nage: 39\n", yaml);
    }
    
    public void testBasicPOJO() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        FiveMinuteUser user = new FiveMinuteUser("Bob", "Dabolito", false,
                FiveMinuteUser.Gender.MALE, new byte[] { 1, 3, 13, 79 });
        String yaml = mapper.writeValueAsString(user).trim();
        String[] parts = yaml.split("\n");
        assertEquals(6, parts.length);
        // unify ordering, need to use TreeSets
        TreeSet<String> exp = new TreeSet<String>();
        for (String part : parts) {
            exp.add(part.trim());
        }
        Iterator<String> it = exp.iterator();
        assertEquals("---", it.next());
        assertEquals("firstName: \"Bob\"", it.next());
        assertEquals("gender: \"MALE\"", it.next());
        assertEquals("lastName: \"Dabolito\"", it.next());
        assertEquals("userImage: \"AQMNTw==\"", it.next());
        assertEquals("verified: false", it.next());
    }
}
