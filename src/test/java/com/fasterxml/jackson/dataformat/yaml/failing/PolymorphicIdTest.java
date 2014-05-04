package com.fasterxml.jackson.dataformat.yaml.failing;

import org.junit.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class PolymorphicIdTest extends ModuleTestBase
{
    static class SingleNesting {
        public Nested nested;
    }

    public interface Nested {

    }

    @JsonTypeName("single")
    static class NestedImpl implements Nested {

    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = NestedImpl.class) })
    public class NestingMixin {
    }
    
    static class TestModule extends SimpleModule {

        private static final long serialVersionUID = 1L;

        @Override
        public void setupModule(SetupContext context) {
            setMixInAnnotation(Nested.class, NestingMixin.class);
            super.setupModule(context);
        }

    }
    
    @Test
    public void testPolymorphicType() throws Exception {
        final String YAML = "nested: \n    type: single\n";
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new TestModule());
        SingleNesting top = mapper.readValue(YAML, SingleNesting.class);
        assertNotNull(top);
    }

    @Test
    public void testNativePolymorphicType() throws Exception {
        String YAML = "nested: !single";
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new TestModule());
        SingleNesting top = mapper.readValue(YAML, SingleNesting.class);
        assertNotNull(top);
    }
}
