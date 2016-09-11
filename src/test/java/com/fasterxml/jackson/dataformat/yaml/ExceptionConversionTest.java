package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Tests to try to ensure that SnakeYAML exceptions are not leaked,
 * both because they are problematic on OSGi runtimes (depending on 
 * whether shading is used) and because it is generally a bad idea
 * to leak implementation details.
 */
public class ExceptionConversionTest extends ModuleTestBase
{
    public void testSimpleParsingLeakage() throws Exception
    {
        YAMLMapper mapper = mapperForYAML();
        try {
             mapper.readTree("foo:\nbar: true\n  baz: false");
             fail("Should not pass with invalid YAML");
        } catch (org.yaml.snakeyaml.scanner.ScannerException e) {
            fail("Internal exception type: "+e);
        } catch (JsonMappingException e) {
//            e.printStackTrace();
            verifyException(e, "YAML decoding problem");
            // good
        } catch (Exception e) {
            fail("Unknown exception: "+e);
        }
    }
}
