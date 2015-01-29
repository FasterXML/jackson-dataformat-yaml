package com.fasterxml.jackson.dataformat.yaml.failing;

import java.io.StringReader;

import com.esotericsoftware.yamlbeans.parser.*;
import com.esotericsoftware.yamlbeans.tokenizer.*;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;

public class YamlBeansTest extends ModuleTestBase
{
    public void testNestedArray() throws Exception
    {
        String YAML = "---\n"
+"stuff:\n"
+"- first\n"
+"- second\n"
+"more: 3\n"
                ;

        Tokenizer tokenizer = new Tokenizer(new StringReader(YAML));
        Token t;

        while ((t = tokenizer.getNextToken()) != null) {
            if (t instanceof ScalarToken) {
                ScalarToken st = (ScalarToken) t;
                System.out.println(" Scalar/"+((int) st.getStyle())+": ["+st.getValue()+"]");
            } else if (t.getClass() == Token.class) {
                System.out.println(" Std-token: "+t.type.name());
            } else {
                System.out.println(" SPEC-token ("+t.getClass().getName()+"): "+t.type.name());
            }
        }

        /*
        Parser p = new Parser(new StringReader(YAML));
        Event evt;

        while ((evt = p.getNextEvent()) != null) {
            if (evt instanceof ScalarEvent) {
                ScalarEvent se = (ScalarEvent) evt;
                System.out.println("Scalar: ["+se.value+"]");
            } else {
                System.out.println(""+evt.type+": "+evt);
            }
        }
        */
    }
}
