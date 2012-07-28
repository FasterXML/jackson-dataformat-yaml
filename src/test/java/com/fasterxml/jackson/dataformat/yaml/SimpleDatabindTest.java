package com.fasterxml.jackson.dataformat.yaml;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for checking functioning of the databinding
 * on top of YAML layer.
 */
public class SimpleDatabindTest extends ModuleTestBase
{
    public void testBasicUntyped() throws Exception
    {
        final String YAML =
 "template: Hello, %s!\n"
+"database:\n"
+"  driverClass: org.h2.Driver\n"
+"  user: scott\n"
+"  password: tiger\n"
+"  extra: [1,2]"
;
        ObjectMapper mapper = mapperForYAML();
        Map<?,?> result = mapper.readValue(YAML, Map.class);
        // sanity check first:
        assertEquals(2, result.size());
        // then literal comparison; easiest to just write as JSON...
        ObjectMapper jsonMapper = new ObjectMapper();
        String json = jsonMapper.writeValueAsString(result);
        String EXP = "{\"template\":\"Hello, %s!\",\"database\":{"
                +"\"driverClass\":\"org.h2.Driver\",\"user\":\"scott\",\"password\":\"tiger\","
                +"\"extra\":[1,2]}}";
        assertEquals(EXP, json);
    }

    public void testBasicPOJO() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        final String YAML =
"firstName: Billy\n"
+"lastName: Baggins\n"                
+"gender: MALE\n"        
+"verified: true\n"
+"userImage: AQIDBAU=" // [1,2,3,4,5]
;
        FiveMinuteUser user = mapper.readValue(YAML, FiveMinuteUser.class);
        assertEquals("Billy", user.firstName);
        assertEquals("Baggins", user.lastName);
        assertEquals(FiveMinuteUser.Gender.MALE, user.getGender());
        assertTrue(user.isVerified());
        byte[] data = user.getUserImage();
        assertNotNull(data);
        Assert.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, data);
    }

    public void testIssue1() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        final byte[] YAML = "firstName: Billy".getBytes("UTF-8");
        FiveMinuteUser user = new FiveMinuteUser();
        user.firstName = "Bubba";
        mapper.readerForUpdating(user).readValue(new ByteArrayInputStream(YAML));
        assertEquals("Billy", user.firstName);
    }

    // [Issue-2]
    public void testUUIDs() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        UUID uuid = new UUID(0, 0);
        String yaml = mapper.writeValueAsString(uuid);
        
        UUID result = mapper.readValue(yaml, UUID.class);
        
        assertEquals(uuid, result);
    }
}
