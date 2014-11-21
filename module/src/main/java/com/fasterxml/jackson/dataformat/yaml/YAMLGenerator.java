package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.emitter.Emitter;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.AliasEvent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.DocumentEndEvent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.DocumentStartEvent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.ImplicitTuple;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.MappingEndEvent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.MappingStartEvent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.ScalarEvent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.SequenceEndEvent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.SequenceStartEvent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.StreamEndEvent;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.StreamStartEvent;


public class YAMLGenerator extends GeneratorBase
{
    /**
     * Enumeration that defines all togglable features for YAML generators
     */
    public enum Feature {
        /**
         * Whether we are to write an explicit document start marker ("---")
         * or not.
         * 
         * @since 2.3
         */
        WRITE_DOC_START_MARKER(true),

        /**
         * Do we try to force so-called canonical output or not.
         */
        CANONICAL_OUTPUT(false)
        
        ;

        protected final boolean _defaultState;
        protected final int _mask;
        
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
    };

    protected final static long MIN_INT_AS_LONG = (long) Integer.MIN_VALUE;
    protected final static long MAX_INT_AS_LONG = (long) Integer.MAX_VALUE;
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    final protected IOContext _ioContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link YAMLGenerator.Feature}s
     * are enabled.
     */
    protected int _yamlFeatures;

    protected Writer _writer;

    protected DumperOptions _outputOptions;

    // for field names, leave out quotes
    private final static Character STYLE_NAME = null;
    
    // numbers, booleans, should use implicit
    private final static Character STYLE_SCALAR = null;
    // Strings quoted for fun
    private final static Character STYLE_STRING = Character.valueOf('"');
        
    // Which flow style to use for Base64? Maybe basic quoted?
    private final static Character STYLE_BASE64 = Character.valueOf('"');

    /*
    /**********************************************************
    /* Output state
    /**********************************************************
     */

    protected Emitter _emitter;

    /**
     * YAML supports native Object identifiers, so databinder may indicate
     * need to output one.
     */
    protected String _objectId;

    /**
     * YAML supports native Type identifiers, so databinder may indicate
     * need to output one.
     */
    protected String _typeId;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public YAMLGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures,
            ObjectCodec codec, Writer out,
            com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.Version version
            ) throws IOException
    {
        super(jsonFeatures, codec);
        _ioContext = ctxt;
        _yamlFeatures = yamlFeatures;
        _writer = out;

        _emitter = new Emitter(_writer, _outputOptions());
        // should we start output now, or try to defer?
        _emitter.emit(new StreamStartEvent(null, null));
        Map<String,String> noTags = Collections.emptyMap();
        
        boolean startMarker = (Feature.WRITE_DOC_START_MARKER.getMask() & yamlFeatures) != 0;
        
        _emitter.emit(new DocumentStartEvent(null, null, startMarker,
                version, // for 1.10 was: ((version == null) ? null : version.getArray()),
                noTags));
    }

    /*                                                                                       
    /**********************************************************                              
    /* Versioned                                                                             
    /**********************************************************                              
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************
    /* Overridden methods, configuration
    /**********************************************************
     */

    /**
     * Not sure what to do here; could reset indentation to some value maybe?
     */
    @Override
    public YAMLGenerator useDefaultPrettyPrinter()
    {
        return this;
    }

    /**
     * Not sure what to do here; will always indent, but uses
     * YAML-specific settings etc.
     */
    @Override
    public YAMLGenerator setPrettyPrinter(PrettyPrinter pp) {
        return this;
    }

    @Override
    public Object getOutputTarget() {
        return _writer;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return false;
    }
    
    //@Override public void setSchema(FormatSchema schema)

    /*
    /**********************************************************************
    /* Overridden methods; writing field names
    /**********************************************************************
     */
    
    /* And then methods overridden to make final, streamline some
     * aspects...
     */

    @Override
    public final void writeFieldName(String name) throws IOException, JsonGenerationException
    {
        if (_writeContext.writeFieldName(name) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name);
    }

    @Override
    public final void writeFieldName(SerializableString name)
        throws IOException, JsonGenerationException
    {
        // Object is a value, need to verify it's allowed
        if (_writeContext.writeFieldName(name.getValue()) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name.getValue());
    }

    @Override
    public final void writeStringField(String fieldName, String value)
        throws IOException, JsonGenerationException
    {
        if (_writeContext.writeFieldName(fieldName) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(fieldName);
        writeString(value);
    }

    private final void _writeFieldName(String name)
        throws IOException, JsonGenerationException
    {
        _writeScalar(name, "string", STYLE_NAME);
    }
    
    /*
    /**********************************************************
    /* Extended API, configuration
    /**********************************************************
     */

    public YAMLGenerator enable(Feature f) {
        _yamlFeatures |= f.getMask();
        return this;
    }

    public YAMLGenerator disable(Feature f) {
        _yamlFeatures &= ~f.getMask();
        return this;
    }

    public final boolean isEnabled(Feature f) {
        return (_yamlFeatures & f.getMask()) != 0;
    }

    public YAMLGenerator configure(Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /*
    /**********************************************************
    /* Public API: low-level I/O
    /**********************************************************
     */

    @Override
    public final void flush() throws IOException
    {
        _writer.flush();
    }
    
    @Override
    public void close() throws IOException
    {
        _emitter.emit(new DocumentEndEvent(null, null, false));
        _emitter.emit(new StreamEndEvent(null, null));
        super.close();
        _writer.close();
    }

    /*
    /**********************************************************
    /* Public API: structural output
    /**********************************************************
     */
    
    @Override
    public final void writeStartArray() throws IOException, JsonGenerationException
    {
        _verifyValueWrite("start an array");
        _writeContext = _writeContext.createChildArrayContext();
        Boolean style = _outputOptions().getDefaultFlowStyle().getStyleBoolean();
        String yamlTag = _typeId;
        boolean implicit = (yamlTag == null);
        String anchor = _objectId;
        if (anchor != null) {
            _objectId = null;
        }
        _emitter.emit(new SequenceStartEvent(anchor, yamlTag,
                implicit,  null, null, style));
    }
    
    @Override
    public final void writeEndArray() throws IOException, JsonGenerationException
    {
        if (!_writeContext.inArray()) {
            _reportError("Current context not an ARRAY but "+_writeContext.getTypeDesc());
        }
        // just to make sure we don't "leak" type ids
        _typeId = null;        
        _writeContext = _writeContext.getParent();
        _emitter.emit(new SequenceEndEvent(null, null));
    }

    @Override
    public final void writeStartObject() throws IOException, JsonGenerationException
    {
        _verifyValueWrite("start an object");
        _writeContext = _writeContext.createChildObjectContext();
        Boolean style = _outputOptions().getDefaultFlowStyle().getStyleBoolean();
        String yamlTag = _typeId;
        boolean implicit = (yamlTag == null);
        String anchor = _objectId;
        if (anchor != null) {
            _objectId = null;
        }
        _emitter.emit(new MappingStartEvent(anchor, yamlTag,
                implicit, null, null, style));
    }

    @Override
    public final void writeEndObject() throws IOException, JsonGenerationException
    {
        if (!_writeContext.inObject()) {
            _reportError("Current context not an object but "+_writeContext.getTypeDesc());
        }
        // just to make sure we don't "leak" type ids
        _typeId = null;        
        _writeContext = _writeContext.getParent();
        _emitter.emit(new MappingEndEvent(null, null));
    }
    
    /*
    /**********************************************************
    /* Output method implementations, textual
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException,JsonGenerationException
    {
        if (text == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write String value");
        _writeScalar(text, "string", STYLE_STRING);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException
    {
        writeString(new String(text, offset, len));
    }

    @Override
    public final void writeString(SerializableString sstr)
        throws IOException, JsonGenerationException
    {
        writeString(sstr.toString());
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int len)
        throws IOException, JsonGenerationException
    {
        _reportUnsupportedOperation();
    }

    @Override
    public final void writeUTF8String(byte[] text, int offset, int len)
        throws IOException, JsonGenerationException
    {
        writeString(new String(text, offset, len, "UTF-8"));
    }

    /*
    /**********************************************************
    /* Output method implementations, unprocessed ("raw")
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        _reportUnsupportedOperation();
    }

    /*
    /**********************************************************
    /* Output method implementations, base64-encoded binary
    /**********************************************************
     */
    
    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException
    {
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        // ok, better just Base64 encode as a String...
        if (offset > 0 || (offset+len) != data.length) {
            data = Arrays.copyOfRange(data, offset, offset+len);
        }
        String encoded = b64variant.encode(data);
        _writeScalar(encoded, "byte[]", STYLE_BASE64);
    }

    /*
    /**********************************************************
    /* Output method implementations, primitive
    /**********************************************************
     */

    @Override
    public void writeBoolean(boolean state) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write boolean value");
        _writeScalar(state ? "true" : "false", "bool", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(int i) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(i), "int", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(long l) throws IOException, JsonGenerationException
    {
        // First: maybe 32 bits is enough?
        if (l <= MAX_INT_AS_LONG && l >= MIN_INT_AS_LONG) {
            writeNumber((int) l);
            return;
        }
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(l), "long", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException, JsonGenerationException
    {
        if (v == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(v.toString()), "java.math.BigInteger", STYLE_SCALAR);
    }
    
    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(d), "double", STYLE_SCALAR);
    }    

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write number");
        _writeScalar(String.valueOf(f), "float", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException, JsonGenerationException
    {
        if (dec == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        String str = isEnabled(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN) ? dec.toPlainString() : dec.toString();
        _writeScalar(str, "java.math.BigDecimal", STYLE_SCALAR);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException,JsonGenerationException, UnsupportedOperationException
    {
        if (encodedValue == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write number");
        _writeScalar(encodedValue, "number", STYLE_SCALAR);
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write null value");
        // no real type for this, is there?
        _writeScalar("null", "object", STYLE_SCALAR);
    }

    /*
    /**********************************************************
    /* Public API, write methods, Native Ids
    /**********************************************************
     */

    @Override
    public boolean canWriteObjectId() {
        // yes, YAML does support Native Type Ids!
        return true;
    }    

    @Override
    public boolean canWriteTypeId() {
        // yes, YAML does support Native Type Ids!
        return true;
    }    

    @Override
    public void writeTypeId(Object id)
        throws IOException, JsonGenerationException
    {
        // should we verify there's no preceding type id?
        _typeId = String.valueOf(id);
    }

    @Override
    public void writeObjectRef(Object id)
        throws IOException, JsonGenerationException
    {
        _verifyValueWrite("write Object reference");
        AliasEvent evt = new AliasEvent(String.valueOf(id), null, null);
        _emitter.emit(evt);
    }
    
    @Override
    public void writeObjectId(Object id)
        throws IOException, JsonGenerationException
    {
        // should we verify there's no preceding id?
        _objectId = String.valueOf(id);
    }
    
    /*
    /**********************************************************
    /* Implementations for methods from base class
    /**********************************************************
     */
    
    @Override
    protected final void _verifyValueWrite(String typeMsg)
        throws IOException, JsonGenerationException
    {
        int status = _writeContext.writeValue();
        if (status == JsonWriteContext.STATUS_EXPECT_NAME) {
            _reportError("Can not "+typeMsg+", expecting field name");
        }
    }

    @Override
    protected void _releaseBuffers() {
        // nothing special to do...
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    // Implicit means that (type) tags won't be shown, right?
    private final static ImplicitTuple DEFAULT_IMPLICIT = new ImplicitTuple(true, true);

    protected void _writeScalar(String value, String type, Character style) throws IOException
    {
        _emitter.emit(_scalarEvent(value, style));
    }
    
    protected ScalarEvent _scalarEvent(String value, Character style)
    {
        String yamlTag = _typeId;
        if (yamlTag != null) {
            _typeId = null;
        }
        String anchor = _objectId;
        if (anchor != null) {
            _objectId = null;
        }
        return new ScalarEvent(anchor, yamlTag, DEFAULT_IMPLICIT, value,
                null, null, style);
    }

    protected DumperOptions _outputOptions()
    {
        if (_outputOptions == null) {
            DumperOptions opt = new DumperOptions();
            // would we want canonical?
            if (isEnabled(Feature.CANONICAL_OUTPUT)) {
                opt.setCanonical(true);
            } else {
                opt.setCanonical(false);
                // if not, MUST specify flow styles
                opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            }
            _outputOptions = opt;
        }
        return _outputOptions;
    }
}
