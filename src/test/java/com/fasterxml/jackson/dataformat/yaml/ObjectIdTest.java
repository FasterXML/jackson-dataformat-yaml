package com.fasterxml.jackson.dataformat.yaml;

import java.io.StringWriter;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectIdTest extends ModuleTestBase
{
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    static class Node
    {
        public String name;

        public Node next;
        
        public Node() { }
        public Node(String name) {
            this.name = name;
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testSerialization() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        Node first = new Node("first");
        Node second = new Node("second");
        first.next = second;
        second.next = first;
        String yaml = mapper.writeValueAsString(first);
        yaml = yaml.trim();
        assertEquals("---\n"
                +"&1 name: \"first\"\n"
                +"next:\n"
                +"  &2 name: \"second\"\n"
                +"  next: *1",
                yaml);
    }
}
