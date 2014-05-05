package com.fasterxml.jackson.dataformat.yaml.failing;

import org.junit.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class PolymorphicIdTest extends ModuleTestBase
{
    static class Wrapper {
        public Nested nested;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = NestedImpl.class) })
    static interface Nested { }

    @JsonTypeName("single")
    static class NestedImpl implements Nested {
        public String value;
    }

    @Test
    public void testPolymorphicType() throws Exception {
        final String YAML = "nested:\n"
                +"  type: single\n"
                +"  value: whatever"
                ;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Wrapper top = mapper.readValue(YAML, Wrapper.class);
        assertNotNull(top);
    }

    @Test
    public void testNativePolymorphicType() throws Exception {
        String YAML = "nested: !single\n"
                +"  value: foobar\n"
                ;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Wrapper top = mapper.readValue(YAML, Wrapper.class);
        assertNotNull(top);
        assertNotNull(top.nested);
        assertEquals(NestedImpl.class, top.nested.getClass());
        assertEquals("foobar", ((NestedImpl) top.nested).value);
    }
}
