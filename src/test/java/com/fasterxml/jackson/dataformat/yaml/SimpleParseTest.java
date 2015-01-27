package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.math.BigInteger;

/**
 * Unit tests for checking functioning of the underlying
 * parser implementation.
 */
public class SimpleParseTest extends ModuleTestBase
{
    final YAMLFactory YAML_F = new YAMLFactory();

    // Parsing large numbers around the transition from int->long and long->BigInteger
    public void testIntParsing() throws Exception
    {
        String YAML;
        JsonParser jp;

        // Test positive max-int
        YAML = "num: 2147483647";
        jp = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(Integer.MAX_VALUE, jp.getIntValue());
        assertEquals(JsonParser.NumberType.INT, jp.getNumberType());
        assertEquals("2147483647", jp.getText());
        jp.close();

        // Test negative max-int
        YAML = "num: -2147483648";
        jp = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(Integer.MIN_VALUE, jp.getIntValue());
        assertEquals(JsonParser.NumberType.INT, jp.getNumberType());
        assertEquals("-2147483648", jp.getText());
        jp.close();

        // Test positive max-int + 1
        YAML = "num: 2147483648";
        jp = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(Integer.MAX_VALUE + 1L, jp.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, jp.getNumberType());
        assertEquals("2147483648", jp.getText());
        jp.close();

        // Test negative max-int - 1
        YAML = "num: -2147483649";
        jp = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(Integer.MIN_VALUE - 1L, jp.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, jp.getNumberType());
        assertEquals("-2147483649", jp.getText());
        jp.close();

        // Test positive max-long
        YAML = "num: 9223372036854775807";
        jp = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(Long.MAX_VALUE, jp.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, jp.getNumberType());
        assertEquals("9223372036854775807", jp.getText());
        jp.close();

        // Test negative max-long
        YAML = "num: -9223372036854775808";
        jp = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(Long.MIN_VALUE, jp.getLongValue());
        assertEquals(JsonParser.NumberType.LONG, jp.getNumberType());
        assertEquals("-9223372036854775808", jp.getText());
        jp.close();

        // Test positive max-long + 1
        YAML = "num: 9223372036854775808";
        jp = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), jp.getBigIntegerValue());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, jp.getNumberType());
        assertEquals("9223372036854775808", jp.getText());
        jp.close();

        // Test negative max-long - 1
        YAML = "num: -9223372036854775809";
        jp = YAML_F.createParser(YAML);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE), jp.getBigIntegerValue());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, jp.getNumberType());
        assertEquals("-9223372036854775809", jp.getText());
        jp.close();
    }

    // [Issue-4]: accidental recognition as double, with multiple dots
    public void testDoubleParsing() throws Exception
    {
        // First, test out valid use case.
        String YAML;

        YAML = "num: +1_000.25"; // note underscores; legal in YAML apparently
        JsonParser jp = YAML_F.createParser(YAML);

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
        jp = YAML_F.createParser(YAML);
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
        // First, test out valid use case. NOTE: spaces matter!
        String YAML = "section:\n"
                    +"  text: foo:bar\n";
        JsonParser jp = YAML_F.createParser(YAML);

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
        YAMLParser yp = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, yp.nextToken());
        assertFalse(yp.isCurrentAlias());
        assertNull(yp.getObjectId());

        assertToken(JsonToken.FIELD_NAME, yp.nextToken());
        assertEquals("parent", yp.getCurrentName());
        assertFalse(yp.isCurrentAlias());
        assertNull(yp.getObjectId());

        assertToken(JsonToken.START_OBJECT, yp.nextToken());
        assertFalse(yp.isCurrentAlias());
        assertEquals("id1", yp.getObjectId());
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
        assertEquals("id2", yp.getObjectId());
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

    // [Issue#10]
    // Scalars should not be parsed when not in the plain flow style.
    public void testQuotedStyles() throws Exception
    {
        String YAML = "strings: [\"true\", 'false']";
        JsonParser jp = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("strings", jp.getCurrentName());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("true", jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("false", jp.getText());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());

        jp.close();
    }

    // Scalars should be parsed when in the plain flow style.
    public void testUnquotedStyles() throws Exception
    {
        String YAML = "booleans: [true, false]";
        JsonParser jp = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("booleans", jp.getCurrentName());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertToken(JsonToken.VALUE_FALSE, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());

        jp.close();
    }

    public void testObjectWithNumbers() throws Exception
    {
        String YAML = "---\n"
+"content:\n"
+"  uri: \"http://javaone.com/keynote.mpg\"\n"
+"  title: \"Javaone Keynote\"\n"
+"  width: 640\n"
+"  height: 480\n"
+"  persons:\n"
+"  - \"Foo Bar\"\n"
+"  - \"Max Power\"\n"
;

        JsonParser jp = YAML_F.createParser(YAML);

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("content", jp.getCurrentName());

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("uri", jp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("title", jp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("width", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(640, jp.getIntValue());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("height", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(480, jp.getIntValue());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("persons", jp.getCurrentName());

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("Foo Bar", jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("Max Power", jp.getText());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());

        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());

        jp.close();
    }
}
