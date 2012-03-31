package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;

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

        // !!! TODO: verify
        System.out.println("YAML/array -> "+yaml);
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
        
        // !!! TODO: verify
        System.out.println("YAML/object -> "+yaml);
    }
    
    public void testBasicPOJO() throws Exception
    {
//        public FiveMinuteUser(String first, String last, boolean verified, Gender g, byte[] data)

        ObjectMapper mapper = mapperForYAML();
        FiveMinuteUser user = new FiveMinuteUser("Bob", "Dabolito", false,
                FiveMinuteUser.Gender.MALE, new byte[] { 1, 3, 13, 79 });
        String yaml = mapper.writeValueAsString(user);
        
        
        System.out.println("YAML == "+yaml);
    }

}
