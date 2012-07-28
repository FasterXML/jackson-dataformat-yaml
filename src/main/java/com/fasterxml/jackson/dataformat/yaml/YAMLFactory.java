package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;

public class YAMLFactory extends JsonFactory
{
    /**
     * Name used to identify YAML format.
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_YAML = "YAML";
    
    /**
     * Bitfield (set of flags) of all parser features that are enabled
     * by default.
     */
    final static int DEFAULT_YAML_PARSER_FEATURE_FLAGS = YAMLParser.Feature.collectDefaults();

    /**
     * Bitfield (set of flags) of all generator features that are enabled
     * by default.
     */    
    final static int DEFAULT_YAML_GENERATOR_FEATURE_FLAGS = YAMLGenerator.Feature.collectDefaults();

    final static byte UTF8_BOM_1 = (byte) 0xEF;
    final static byte UTF8_BOM_2 = (byte) 0xBB;
    final static byte UTF8_BOM_3 = (byte) 0xBF;
    
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected int _yamlParserFeatures = DEFAULT_YAML_PARSER_FEATURE_FLAGS;

    protected int _yamlGeneratorFeatures = DEFAULT_YAML_GENERATOR_FEATURE_FLAGS;
    
    /*
    /**********************************************************************
    /* Factory construction, configuration
    /**********************************************************************
     */

    protected DumperOptions _outputOptions;

    protected Integer[] _version;
    
    /**
     * Default constructor used to create factory instances.
     * Creation of a factory instance is a light-weight operation,
     * but it is still a good idea to reuse limited number of
     * factory instances (and quite often just a single instance):
     * factories are used as context for storing some reused
     * processing objects (such as symbol tables parsers use)
     * and this reuse only works within context of a single
     * factory instance.
     */
    public YAMLFactory() { this(null); }

    public YAMLFactory(ObjectCodec oc)
    {
        super(oc);
        _outputOptions = _defaultOptions();
        DumperOptions.Version version = _outputOptions.getVersion();
        _version = (version == null) ? null : version.getArray();
    }

    private static DumperOptions _defaultOptions()
    {
        DumperOptions opt = new DumperOptions();
        // would we want canonical?
        opt.setCanonical(false);
        // if not, MUST specify flow styles
        opt.setDefaultFlowStyle(FlowStyle.BLOCK);
        return opt;
        
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
    /* Format detection functionality (since 1.8)
    /**********************************************************
     */
    
    @Override
    public String getFormatName() {
        return FORMAT_NAME_YAML;
    }
    
    /**
     * Sub-classes need to override this method (as of 1.8)
     */
    @Override
    public MatchStrength hasFormat(InputAccessor acc) throws IOException
    {
        /* Actually quite possible to do, thanks to (optional) "---"
         * indicator we may be getting...
         */
        if (!acc.hasMoreBytes()) {
            return MatchStrength.INCONCLUSIVE;
        }
        byte b = acc.nextByte();
        // Very first thing, a UTF-8 BOM?
        if (b == UTF8_BOM_1) { // yes, looks like UTF-8 BOM
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != UTF8_BOM_2) {
                return MatchStrength.NO_MATCH;
            }
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != UTF8_BOM_3) {
                return MatchStrength.NO_MATCH;
            }
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            b = acc.nextByte();
        }
        // as far as I know, leading space is NOT allowed before "---" marker?
        if (b == '-' && (acc.hasMoreBytes() && acc.nextByte() == '-')
                && (acc.hasMoreBytes() && acc.nextByte() == '-')) {
            return MatchStrength.FULL_MATCH;
        }
        return MatchStrength.INCONCLUSIVE;
    }
    
    /*
    /**********************************************************
    /* Configuration, parser settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified parser feature
     * (check {@link YAMLParser.Feature} for list of features)
     */
    public final YAMLFactory configure(YAMLParser.Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }

    /**
     * Method for enabling specified parser feature
     * (check {@link YAMLParser.Feature} for list of features)
     */
    public YAMLFactory enable(YAMLParser.Feature f) {
        _yamlParserFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified parser features
     * (check {@link YAMLParser.Feature} for list of features)
     */
    public YAMLFactory disable(YAMLParser.Feature f) {
        _yamlParserFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Checked whether specified parser feature is enabled.
     */
    public final boolean isEnabled(YAMLParser.Feature f) {
        return (_yamlParserFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Configuration, generator settings
    /**********************************************************
     */

    /**
     * Method for enabling or disabling specified generator feature
     * (check {@link YAMLGenerator.Feature} for list of features)
     */
    public final YAMLFactory configure(YAMLGenerator.Feature f, boolean state) {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
        return this;
    }


    /**
     * Method for enabling specified generator features
     * (check {@link YAMLGenerator.Feature} for list of features)
     */
    public YAMLFactory enable(YAMLGenerator.Feature f) {
        _yamlGeneratorFeatures |= f.getMask();
        return this;
    }

    /**
     * Method for disabling specified generator feature
     * (check {@link YAMLGenerator.Feature} for list of features)
     */
    public YAMLFactory disable(YAMLGenerator.Feature f) {
        _yamlGeneratorFeatures &= ~f.getMask();
        return this;
    }

    /**
     * Check whether specified generator feature is enabled.
     */
    public final boolean isEnabled(YAMLGenerator.Feature f) {
        return (_yamlGeneratorFeatures & f.getMask()) != 0;
    }

    /*
    /**********************************************************
    /* Overridden parser factory methods (for 2.1)
    /**********************************************************
     */

    @Override
    public YAMLParser createParser(String content)
        throws IOException, JsonParseException
    {
        Reader r = new StringReader(content);
        IOContext ctxt = _createContext(r, true); // true->own, can close
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            r = _inputDecorator.decorate(ctxt, r);
        }
        return _createParser(r, ctxt);
    }

    @Override
    public YAMLParser createParser(File f)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(f, true);
        InputStream in = new FileInputStream(f);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            in = _inputDecorator.decorate(ctxt, in);
        }
        return _createParser(in, ctxt);
    }
    
    @Override
    public YAMLParser createParser(URL url)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(url, true);
        InputStream in = _optimizedStreamFromURL(url);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            in = _inputDecorator.decorate(ctxt, in);
        }
        return _createParser(in, ctxt);
    }

    @Override
    public YAMLParser createParser(InputStream in)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(in, false);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            in = _inputDecorator.decorate(ctxt, in);
        }
        return _createParser(in, ctxt);
    }

    @Override
    public JsonParser createParser(Reader r)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(r, false);
        if (_inputDecorator != null) {
            r = _inputDecorator.decorate(ctxt, r);
        }
        return _createParser(r, ctxt);
    }

    @Override
    public YAMLParser createParser(byte[] data)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(data, true);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, 0, data.length);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, 0, data.length, ctxt);
    }
    
    @Override
    public YAMLParser createParser(byte[] data, int offset, int len)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(data, true);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, offset, len);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, offset, len, ctxt);
    }
    
    /*
    /**********************************************************
    /* Overridden parser factory methods (2.0 and prior)
    /**********************************************************
     */

    @Override
    public YAMLParser createJsonParser(String content)
        throws IOException, JsonParseException
    {
        Reader r = new StringReader(content);
        IOContext ctxt = _createContext(r, true); // true->own, can close
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            r = _inputDecorator.decorate(ctxt, r);
        }
        return _createParser(r, ctxt);
    }

    @Override
    public YAMLParser createJsonParser(File f)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(f, true);
        InputStream in = new FileInputStream(f);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            in = _inputDecorator.decorate(ctxt, in);
        }
        return _createParser(in, ctxt);
    }
    
    @Override
    public YAMLParser createJsonParser(URL url)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(url, true);
        InputStream in = _optimizedStreamFromURL(url);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            in = _inputDecorator.decorate(ctxt, in);
        }
        return _createParser(in, ctxt);
    }

    @Override
    public YAMLParser createJsonParser(InputStream in)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(in, false);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            in = _inputDecorator.decorate(ctxt, in);
        }
        return _createParser(in, ctxt);
    }

    @Override
    public JsonParser createJsonParser(Reader r)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(r, false);
        if (_inputDecorator != null) {
            r = _inputDecorator.decorate(ctxt, r);
        }
        return _createParser(r, ctxt);
    }

    @Override
    public YAMLParser createJsonParser(byte[] data)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(data, true);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, 0, data.length);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, 0, data.length, ctxt);
    }
    
    @Override
    public YAMLParser createJsonParser(byte[] data, int offset, int len)
        throws IOException, JsonParseException
    {
        IOContext ctxt = _createContext(data, true);
        // [JACKSON-512]: allow wrapping with InputDecorator
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, offset, len);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, offset, len, ctxt);
    }

    /*
    /**********************************************************
    /* Overridden generator factory methods (2.1)
    /**********************************************************
     */

    @Override
    public YAMLGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(out, false);
        // [JACKSON-512]: allow wrapping with _outputDecorator
        if (_outputDecorator != null) {
            out = _outputDecorator.decorate(ctxt, out);
        }
        return _createGenerator(_createWriter(out, JsonEncoding.UTF8, ctxt), ctxt);
    }

    @Override
    public YAMLGenerator createGenerator(OutputStream out) throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(out, false);
        // [JACKSON-512]: allow wrapping with _outputDecorator
        if (_outputDecorator != null) {
            out = _outputDecorator.decorate(ctxt, out);
        }
        return _createGenerator(_createWriter(out, JsonEncoding.UTF8, ctxt), ctxt);
    }
    
    @Override
    public YAMLGenerator createGenerator(Writer out) throws IOException
    {
        IOContext ctxt = _createContext(out, false);
        // [JACKSON-512]: allow wrapping with _outputDecorator
        if (_outputDecorator != null) {
            out = _outputDecorator.decorate(ctxt, out);
        }
        return _createGenerator(out, ctxt);
    }
    
    /*
    /**********************************************************
    /* Overridden generator factory methods (2.0 and before)
    /**********************************************************
     */

    @Override
    public YAMLGenerator createJsonGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        return createGenerator(out, enc);
    }

    @Override
    public YAMLGenerator createJsonGenerator(OutputStream out) throws IOException {
        return createGenerator(out);
    }
    
    @Override
    public YAMLGenerator createJsonGenerator(Writer out) throws IOException {
        return createGenerator(out);
    }
    
    /*
    /******************************************************
    /* Overridden internal factory methods
    /******************************************************
     */

    //protected IOContext _createContext(Object srcRef, boolean resourceManaged)

    @Override
    protected YAMLParser _createParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        Reader r = _createReader(in, null, ctxt);
        return new YAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, r);
    }

    @Override
    protected YAMLParser _createParser(Reader r, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return new YAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, r);
    }

    @Override
    protected YAMLParser _createParser(byte[] data, int offset, int len, IOContext ctxt)
        throws IOException, JsonParseException
    {
        Reader r = _createReader(data, offset, len, null, ctxt);
        return new YAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
                _objectCodec, r);
    }

    @Override
    protected YAMLParser _createJsonParser(InputStream in, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return _createParser(in, ctxt);
    }

    @Override
    protected JsonParser _createJsonParser(Reader r, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return _createParser(r, ctxt);
    }

    @Override
    protected YAMLParser _createJsonParser(byte[] data, int offset, int len, IOContext ctxt)
        throws IOException, JsonParseException
    {
        return _createParser(data, offset, len, ctxt);
    }

    @Override
    protected YAMLGenerator _createGenerator(Writer out, IOContext ctxt)
        throws IOException
    {
        int feats = _yamlGeneratorFeatures;
        YAMLGenerator gen = new YAMLGenerator(ctxt, _generatorFeatures, feats,
                _objectCodec, out, _outputOptions, _version);
        // any other initializations? No?
        return gen;
    }

    @Override
    protected YAMLGenerator _createJsonGenerator(Writer out, IOContext ctxt)
        throws IOException
    {
        return _createGenerator(out, ctxt);
    }

    @Override
    protected YAMLGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        throw new IllegalStateException("Method should never get called");
    }

    @Override
    @Deprecated
    protected YAMLGenerator _createUTF8JsonGenerator(OutputStream out, IOContext ctxt) throws IOException {
        return _createUTF8Generator(out, ctxt);
    }
    
    @Override
    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        if (enc == JsonEncoding.UTF8) {
            return new UTF8Writer(out);
        }
        return new OutputStreamWriter(out, enc.getJavaName());
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    protected final Charset UTF8 = Charset.forName("UTF-8");

    protected Reader _createReader(InputStream in, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        if (enc == null) {
            enc = JsonEncoding.UTF8;
        }
        // default to UTF-8 if encoding missing
        if (enc == JsonEncoding.UTF8) {
            boolean autoClose = ctxt.isResourceManaged() || this.isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE);
            return new UTF8Reader(in, autoClose);
//          return new InputStreamReader(in, UTF8);
        }
        return new InputStreamReader(in, enc.getJavaName());
    }

    protected Reader _createReader(byte[] data, int offset, int len,
            JsonEncoding enc, IOContext ctxt) throws IOException
    {
        if (enc == null) {
            enc = JsonEncoding.UTF8;
        }
        // default to UTF-8 if encoding missing
        if (enc == null || enc == JsonEncoding.UTF8) {
            return new UTF8Reader(data, offset, len, true);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(data, offset, len);
        return new InputStreamReader(in, enc.getJavaName());
    }
}
