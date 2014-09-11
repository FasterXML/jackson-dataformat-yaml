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

    private final static String SIMPLE_YAML_NATIVE =
            "---\n"
            +"&1 name: \"first\"\n"
            +"next:\n"
            +"  &2 name: \"second\"\n"
            +"  next: *1"
            ;

    private final static String SIMPLE_YAML_NON_NATIVE =
            "---\n"
            +"'@id': 1\n"
            +"name: \"first\"\n"
            +"next:\n"
            +"  '@id': 2\n"
            +"  name: \"second\"\n"
            +"  next: 1"
            ;
    
    public void testNativeSerialization() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        Node first = new Node("first");
        Node second = new Node("second");
        first.next = second;
        second.next = first;
        String yaml = mapper.writeValueAsString(first);
        assertYAML(SIMPLE_YAML_NATIVE, yaml);
    }

    // [Issue#23]
    public void testNonNativeSerialization() throws Exception
    {
        YAMLMapper mapper = new YAMLMapper();
        mapper.disable(YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID);
        Node first = new Node("first");
        Node second = new Node("second");
        first.next = second;
        second.next = first;
        String yaml = mapper.writeValueAsString(first);
        assertYAML(SIMPLE_YAML_NON_NATIVE, yaml);
    }

    public void testDeserialization() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        Node first = mapper.readValue(SIMPLE_YAML_NATIVE, Node.class);
        _verify(first);

        // Also with non-antive
        Node second = mapper.readValue(SIMPLE_YAML_NON_NATIVE, Node.class);
        _verify(second);
    }

    public void testRoundtripWithBuffer() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        TokenBuffer tbuf = mapper.readValue(SIMPLE_YAML_NATIVE, TokenBuffer.class);
        assertNotNull(tbuf);
        Node first = mapper.readValue(tbuf.asParser(), Node.class);
        tbuf.close();
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
