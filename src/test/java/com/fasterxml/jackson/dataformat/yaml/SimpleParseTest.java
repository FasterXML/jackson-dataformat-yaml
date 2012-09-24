package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Unit tests for checking functioning of the underlying
 * parser implementation.
 */
public class SimpleParseTest extends ModuleTestBase
{
    // [Issue-4]: accidental recognition as double, with multiple dots
    public void testDoubleParsing() throws Exception
    {
        YAMLFactory f = new YAMLFactory();

        // First, test out valid use case.
        String YAML;

        YAML = "num: +1_000.25"; // note underscores; legal in YAML apparently
        JsonParser jp = f.createJsonParser(YAML);

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        // should be considered a String...
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(1000.25, jp.getDoubleValue());
        // let's retain exact representation text however:
        assertEquals("+1_000.25", jp.getText());
        jp.close();
        
        // and then non-number that may be mistaken
        
        final String IP = "10.12.45.127";
        YAML = "ip: "+IP+"\n";
        jp = f.createJsonParser(YAML);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("ip", jp.getCurrentName());
        // should be considered a String...
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(IP, jp.getText());
        jp.close();
    }

    // [Issue#7]
    // looks like colons in content can be problematic, if unquoted
    public void testColons() throws Exception
    {
        YAMLFactory f = new YAMLFactory();

        // First, test out valid use case. NOTE: spaces matter!
        String YAML = "section:\n"
                    +"  text: foo:bar\n";
        JsonParser jp = f.createJsonParser(YAML);

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("section", jp.getCurrentName());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("text", jp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("foo:bar", jp.getText());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());

        jp.close();
    }
    
    /**
     * How should YAML Anchors be exposed?
     */
    public void testAnchorParsing() throws Exception
    {
        // silly doc, just to expose an id (anchor) and ref to it
        final String YAML = "---\n"
                +"parent: &id1\n"
                +"    name: Bob\n"
                +"child: &id2\n"
                +"    name: Bill\n"
                +"    parentRef: *id1"
                ;
        YAMLFactory f = new YAMLFactory();
        YAMLParser yp = f.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, yp.nextToken());
        assertFalse(yp.isCurrentAlias());
        assertNull(yp.getCurrentAnchor());

        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("parent", yp.getCurrentName());
        assertFalse(yp.isCurrentAlias());
        assertNull(yp.getCurrentAnchor());

        assertToken(JsonToken.START_OBJECT, yp.nextToken());
        assertFalse(yp.isCurrentAlias());
        assertEquals("id1", yp.getCurrentAnchor());
        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("name", yp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, yp.nextToken());
        assertEquals("Bob", yp.getText());
        assertFalse(yp.isCurrentAlias());
        assertToken(JsonToken.END_OBJECT, yp.nextToken());

        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("child", yp.getCurrentName());
        assertFalse(yp.isCurrentAlias());
        assertToken(JsonToken.START_OBJECT, yp.nextToken());
        assertFalse(yp.isCurrentAlias());
        assertEquals("id2", yp.getCurrentAnchor());
        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("name", yp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, yp.nextToken());
        assertEquals("Bill", yp.getText());
        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("parentRef", yp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, yp.nextToken());
        assertEquals("id1", yp.getText());
        assertTrue(yp.isCurrentAlias());
        assertToken(JsonToken.END_OBJECT, yp.nextToken());

        assertToken(JsonToken.END_OBJECT, yp.nextToken());
        
        assertNull(yp.nextToken());
        yp.close();
    }
}
