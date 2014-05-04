package com.fasterxml.jackson.dataformat.yaml.failing;

import org.junit.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class PolymorphicIdTest extends ModuleTestBase
{
    static class SingleNesting {
        public Nested nested;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = NestedImpl.class) })
    static interface Nested { }

    @JsonTypeName("single")
    static class NestedImpl implements Nested { }

    @Test
    public void testPolymorphicType() throws Exception {
        final String YAML = "--- nested: \n    type: single\n";
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SingleNesting top = mapper.readValue(YAML, SingleNesting.class);
        assertNotNull(top);
    }

    @Test
    public void testNativePolymorphicType() throws Exception {
        String YAML = "--- nested: !single";
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SingleNesting top = mapper.readValue(YAML, SingleNesting.class);
        assertNotNull(top);
    }
}
