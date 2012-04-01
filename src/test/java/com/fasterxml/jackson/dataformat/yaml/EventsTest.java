package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.DataFormatDetector;
import com.fasterxml.jackson.core.format.DataFormatMatcher;
import com.fasterxml.jackson.core.format.MatchStrength;

/**
 * Tests that test low-level handling of events from YAML source
 */
public class EventsTest extends ModuleTestBase
{
    public void testBasic() throws Exception
    {
        final String YAML =
 "string: 'text'\n"
+"bool: true\n"
+"bool2: false\n"
+"null: null\n"
+"i: 123\n"
+"d: 1.25\n"
;
        YAMLFactory f = new YAMLFactory();
        JsonParser p = f.createJsonParser(YAML);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("text", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals("true", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        assertEquals("false", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertEquals("null", p.getText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("123", p.getText());
        assertEquals(123, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals("1.25", p.getText());
        assertEquals(1.25, p.getDoubleValue());
        assertEquals(1, p.getIntValue());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertNull(p.nextToken());
        assertNull(p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    /**
     * One nifty thing YAML has is the "---" start-doc indicator, which
     * makes it possible to auto-detect format...
     */
    public void testFormatDetection() throws Exception
    {
        YAMLFactory yamlF = new YAMLFactory();
        JsonFactory jsonF = new JsonFactory();
        DataFormatDetector det = new DataFormatDetector(new JsonFactory[] { yamlF, jsonF });
        // let's accept about any match; but only if no "solid match" found
        det = det.withMinimalMatch(MatchStrength.WEAK_MATCH).withOptimalMatch(MatchStrength.SOLID_MATCH);

        // First, give a JSON document...
        DataFormatMatcher match = det.findFormat("{ \"name\" : \"Bob\" }".getBytes("UTF-8"));
        assertNotNull(match);
        assertEquals(jsonF.getFormatName(), match.getMatchedFormatName());
        // and verify we can parse it
        JsonParser p = match.createParserWithMatch();
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("name", p.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("Bob", p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();

        // then YAML
        match = det.findFormat("---\nname: Bob\n".getBytes("UTF-8"));
        assertNotNull(match);
        assertEquals(yamlF.getFormatName(), match.getMatchedFormatName());
        // and parsing
        p = match.createParserWithMatch();
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("name", p.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("Bob", p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();


    }
}
