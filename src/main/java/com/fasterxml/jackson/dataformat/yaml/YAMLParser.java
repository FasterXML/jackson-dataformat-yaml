package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;

/**
 * {@link JsonParser} implementation used to expose YAML documents
 * in form that allows other Jackson functionality to deal
 * with it.
 */
public class YAMLParser
    extends ParserBase
{
    /**
     * Enumeration that defines all togglable features for YAML parsers.
     */
    public enum Feature {
        
        BOGUS(false)
        ;

        final boolean _defaultState;
        final int _mask;
        
        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
            _mask = (1 << ordinal());
        }
        
        public boolean enabledByDefault() { return _defaultState; }
        public int getMask() { return _mask; }
    }

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */
    
    /**
     * Codec used for data binding when (if) requested.
     */
    protected ObjectCodec _objectCodec;

    protected int _yamlFeatures;

    /*
    /**********************************************************************
    /* Input sources
    /**********************************************************************
     */

    /**
     * Need to keep track of underlying {@link Reader} to be able to
     * auto-close it (if required to)
     */
    protected Reader _reader;
    
    protected ParserImpl _yamlParser;
    
    /*
    /**********************************************************************
    /* State
    /**********************************************************************
     */

    /**
     * Keep track of the last event read, to get access to Location info
     */
    protected Event _lastEvent;

    /**
     * We need to keep track of text values.
     */
    protected String _textValue;

    /**
     * Let's also have a local copy of the current field name
     */
    protected String _currentFieldName;
    
    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */
    
    public YAMLParser(IOContext ctxt, BufferRecycler br,
            int parserFeatures, int csvFeatures,
            ObjectCodec codec, Reader reader)
    {
        super(ctxt, parserFeatures);    
        _objectCodec = codec;
        _yamlFeatures = csvFeatures;
        _reader = reader;
        _yamlParser = new ParserImpl(new StreamReader(reader));
    }


    @Override
    public ObjectCodec getCodec() {
        return _objectCodec;
    }

    @Override
    public void setCodec(ObjectCodec c) {
        _objectCodec = c;
    }
    
    /*                                                                                       
    /**********************************************************                              
    /* Versioned                                                                             
    /**********************************************************                              
     */

    @Override
    public Version version() {
        return ModuleVersion.instance.version();
    }

    /*
    /**********************************************************                              
    /* ParserBase method impls
    /**********************************************************                              
     */

    
    @Override
    protected boolean loadMore() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _finishString() throws IOException, JsonParseException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _closeInput() throws IOException {
        _reader.close();
    }
    
    /*
    /**********************************************************                              
    /* Overridden methods
    /**********************************************************                              
     */
    
    /*
    /***************************************************
    /* Public API, configuration
    /***************************************************
     */

    /**
     * Method for enabling specified CSV feature
     * (check {@link Feature} for list of features)
     */
    public JsonParser enable(YAMLParser.Feature f)
    {
        _yamlFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified  CSV feature
     * (check {@link Feature} for list of features)
     */
    public JsonParser disable(YAMLParser.Feature f)
    {
        _yamlFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Method for enabling or disabling specified CSV feature
     * (check {@link Feature} for list of features)
     */
    public JsonParser configure(YAMLParser.Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for checking whether specified CSV {@link Feature}
     * is enabled.
     */
    public boolean isEnabled(YAMLParser.Feature f) {
        return (_yamlFeatures & f.getMask()) != 0;
    }

//    @Override public CsvSchema getSchema() 
    
    /*
    /**********************************************************
    /* Location info
    /**********************************************************
     */

    @Override
    public JsonLocation getTokenLocation()
    {
        if (_lastEvent == null) {
            return JsonLocation.NA;
        }
        return _locationFor(_lastEvent.getStartMark());
    }

    @Override
    public JsonLocation getCurrentLocation() {
        // can assume we are at the end of token now...
        if (_lastEvent == null) {
            return JsonLocation.NA;
        }
        return _locationFor(_lastEvent.getEndMark());
    }
    
    protected JsonLocation _locationFor(Mark m)
    {
        if (m == null) {
            return new JsonLocation(_ioContext.getSourceReference(),
                    -1, -1, -1);
        }
        return new JsonLocation(_ioContext.getSourceReference(),
                -1,
                m.getLine() + 1, // from 0- to 1-based
                m.getColumn() + 1); // ditto
    }

    // Note: SHOULD override 'getTokenLineNr', 'getTokenColumnNr', but those are final in 2.0
    
    /*
    /**********************************************************
    /* Parsing
    /**********************************************************
     */
    
    @Override
    public JsonToken nextToken() throws IOException, JsonParseException
    {
        _binaryValue = null;
        if (_closed) {
            return null;
        }

        while (true) {
            Event evt = _yamlParser.getEvent();

            // is null ok? Assume it is, for now, consider to be same as end-of-doc
            if (evt == null) {
                return (_currToken = null);
            }
            
            _lastEvent = evt;

            /* One complication: field names are only inferred from the
             * fact that we are in Object context...
             */
            if (_parsingContext.inObject() && _currToken != JsonToken.FIELD_NAME) {
                if (!evt.is(Event.ID.Scalar)) {
                    // end is fine
                    if (evt.is(Event.ID.MappingEnd)) {
                        if (!_parsingContext.inObject()) { // sanity check is optional, but let's do it for now
                            _reportMismatchedEndMarker('}', ']');
                        }
                        _parsingContext = _parsingContext.getParent();
                        return (_currToken = JsonToken.END_OBJECT);
                    }
                    _reportError("Expected a field name (Scalar value in YAML), got this instead: "+evt);
                }
                String name = ((ScalarEvent) evt).getValue();
                _currentFieldName = name;
                _parsingContext.setCurrentName(name);
                return (_currToken = JsonToken.FIELD_NAME);
            }
            // Ugh. Why not expose id, to be able to Switch?

            // scalar values are probably the commonest:
            if (evt.is(Event.ID.Scalar)) {
                ScalarEvent scalar = (ScalarEvent) evt;
                _textValue = scalar.getValue();
                
                // TODO: perhaps try making use of tag, for type resolution?
                // (note, tho, that tags are usually implicit and ignored)
//                String typeTag = scalar.getTag();
                
                // any way to figure out actual type? No?
                return (_currToken = JsonToken.VALUE_STRING);
            }

            // followed by maps, then arrays
            if (evt.is(Event.ID.MappingStart)) {
                Mark m = evt.getStartMark();
                _parsingContext = _parsingContext.createChildObjectContext(m.getLine(), m.getColumn());
                return (_currToken = JsonToken.START_OBJECT);
            }
            if (evt.is(Event.ID.MappingEnd)) { // actually error; can not have map-end here
                _reportError("Not expecting END_OBJECT but a value");
            }
            if (evt.is(Event.ID.SequenceStart)) {
                Mark m = evt.getStartMark();
                _parsingContext = _parsingContext.createChildArrayContext(m.getLine(), m.getColumn());
                return (_currToken = JsonToken.START_ARRAY);
            }
            if (evt.is(Event.ID.SequenceEnd)) {
                if (!_parsingContext.inArray()) { // sanity check is optional, but let's do it for now
                    _reportMismatchedEndMarker(']', '}');
                }
                _parsingContext = _parsingContext.getParent();
                return (_currToken = JsonToken.END_ARRAY);
            }

            // after this, less common tokens:
            
            if (evt.is(Event.ID.DocumentEnd)) {
                // logical end of doc; fine. Two choices; either skip, or
                // return null as marker. Do latter, for now. But do NOT close.
                return (_currToken = null);
            }
            if (evt.is(Event.ID.DocumentStart)) {
                // does this matter? Shouldn't, should it?
                continue;
            }
            if (evt.is(Event.ID.Alias)) {
                // for now, nothing to do: in future, maybe try to expose as ObjectIds?
                continue;
            }
            if (evt.is(Event.ID.StreamEnd)) { // end-of-input; force closure
                close();
                return (_currToken = null);
            }
            if (evt.is(Event.ID.StreamStart)) { // useless, skip
                continue;
            }
        }
    }
    
    /*
    /**********************************************************
    /* String value handling
    /**********************************************************
     */

    // For now we do not store char[] representation...
    @Override
    public boolean hasTextCharacters() {
        return false;
    }
    
    @Override
    public String getText() throws IOException, JsonParseException
    {
        if (_currToken == JsonToken.VALUE_STRING) {
            return _textValue;
        }
        if (_currToken == JsonToken.FIELD_NAME) {
            return _currentFieldName;
        }
        if (_currToken != null) {
            return _currToken.asString();
        }
        return null;
    }

    @Override
    public String getCurrentName() throws IOException, JsonParseException
    {
        if (_currToken == JsonToken.FIELD_NAME) {
            return _currentFieldName;
        }
        return super.getCurrentName();
    }
    
    @Override
    public char[] getTextCharacters() throws IOException, JsonParseException {
        String text = getText();
        return (text == null) ? null : text.toCharArray();
    }

    @Override
    public int getTextLength() throws IOException, JsonParseException {
        String text = getText();
        return (text == null) ? 0 : text.length();
    }

    @Override
    public int getTextOffset() throws IOException, JsonParseException {
        return 0;
    }
    
    /*
    /**********************************************************************
    /* Binary (base64)
    /**********************************************************************
     */

    @Override
    public Object getEmbeddedObject() throws IOException, JsonParseException {
        return null;
    }
    
    @Override
    public byte[] getBinaryValue(Base64Variant variant) throws IOException, JsonParseException
    {
        if (_binaryValue == null) {
            if (_currToken != JsonToken.VALUE_STRING) {
                _reportError("Current token ("+_currToken+") not VALUE_STRING, can not access as binary");
            }
            ByteArrayBuilder builder = _getByteArrayBuilder();
            _decodeBase64(getText(), builder, variant);
            _binaryValue = builder.toByteArray();
        }
        return _binaryValue;
    }

    /*
    /**********************************************************************
    /* Number accessors
    /**********************************************************************
     */

    /*
    /**********************************************************************
    /* Helper methods from base class
    /**********************************************************************
     */

    /*
    @Override
    protected void _handleEOF() throws JsonParseException {
        // I don't think there's problem with EOFs usually; except maybe in quoted stuff?
        _reportInvalidEOF(": expected closing quote character");
    }
    */

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */
    
    public ByteArrayBuilder _getByteArrayBuilder()
    {
        if (_byteArrayBuilder == null) {
            _byteArrayBuilder = new ByteArrayBuilder();
        } else {
            _byteArrayBuilder.reset();
        }
        return _byteArrayBuilder;
    }
}
