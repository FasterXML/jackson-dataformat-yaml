package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;

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

    private final static String SIMPLE_YAML =
            "---\n"
            +"&1 name: \"first\"\n"
            +"next:\n"
            +"  &2 name: \"second\"\n"
            +"  next: *1"
            ;
    
    public void testSerialization() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        Node first = new Node("first");
        Node second = new Node("second");
        first.next = second;
        second.next = first;
        String yaml = mapper.writeValueAsString(first);
        assertYAML(SIMPLE_YAML, yaml);
    }

    public void testDeserialization() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        Node first = mapper.readValue(SIMPLE_YAML, Node.class);
        _verify(first);
    }

    public void testRoundtripWithBuffer() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        TokenBuffer tbuf = mapper.readValue(SIMPLE_YAML, TokenBuffer.class);
        assertNotNull(tbuf);
        Node first = mapper.readValue(tbuf.asParser(), Node.class);
        assertNotNull(first);
        _verify(first);
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void _verify(Node first)
    {
        assertNotNull(first);
        assertEquals("first", first.name);
        assertNotNull(first.next);
        assertEquals("second", first.next.name);
        assertNotNull(first.next.next);
        assertSame(first, first.next.next);
    }
}
