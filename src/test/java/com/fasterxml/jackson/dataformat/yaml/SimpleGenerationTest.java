package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SimpleGenerationTest extends ModuleTestBase
{
    public void testBasicPOJO() throws Exception
    {
//        public FiveMinuteUser(String first, String last, boolean verified, Gender g, byte[] data)

        ObjectMapper mapper = mapperForYAML();
        FiveMinuteUser user = new FiveMinuteUser("Bob", "Dabolito", false,
                FiveMinuteUser.Gender.MALE, new byte[] { 1, 3, 13, 79 });
        String yaml = mapper.writeValueAsString(user);
        
        
        System.err.println("YAML == "+yaml);
    }

}
