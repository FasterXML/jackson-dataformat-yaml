package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;

public class TypeIdTest extends ModuleTestBase
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "id")
    @JsonSubTypes({ @JsonSubTypes.Type(Impl.class) })
    static abstract class Base {
        public int a;
        
        public Base() { }
        public Base(int a) {
            this.a = a;
        }
    }

    @JsonTypeName("impl")
    static class Impl extends Base {
        public Impl() { }
        public Impl(int a) { super(a); }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testSerialization() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        String yaml = mapper.writeValueAsString(new Impl(13));
        yaml = yaml.trim();
        assertEquals("--- !<impl>\na: 13", yaml);
    }

    public void testDeserialization() throws Exception
    {
        /* Looks like there are couple of alternative ways to indicate
         * type ids... so let's verify variations we know of.
         */
        ObjectMapper mapper = mapperForYAML();
        
        for (String typeId : new String[] {
                "--- !<impl>",
                "--- !impl",
            }) {
            final String input = typeId + "\na: 13";
            Base result = mapper.readValue(input, Base.class);
            _verify(result);
        }
    }

    public void testRoundtripWithBuffer() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        TokenBuffer tbuf = mapper.readValue("--- !impl\na: 13\n", TokenBuffer.class);
        assertNotNull(tbuf);
        Base result = mapper.readValue(tbuf.asParser(), Base.class);
        tbuf.close();
        _verify(result);
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void _verify(Base result)
    {
        assertNotNull(result);
        assertEquals(Impl.class, result.getClass());
        Impl i = (Impl) result;
        assertEquals(13, i.a);
    }
}
