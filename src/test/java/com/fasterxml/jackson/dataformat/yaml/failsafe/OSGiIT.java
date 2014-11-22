/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2014 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.fasterxml.jackson.dataformat.yaml.failsafe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.url;

/**
 * PAX-EXAM test that proves that the OSGi bundle works. This test is magically transformed into
 * an OSGi bundle, and additional bundles are loaded into the container as specified in
 * {@link #config()}.
 *<p>
 * NOTE: named specifically so as NOT to run as a unit test, as this is an integration test
 * to run after build
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OSGiIT
{
    @Configuration
    public Option[] config() {
        return options(
                mavenBundle("com.fasterxml.jackson.core", "jackson-core").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.core", "jackson-annotations").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.core", "jackson-databind").versionAsInProject(),
                /*
                 * mavenBundle talks to aether, not the reactor, so we cannot use it to access the
                 * thing built just now. Instead, we load the file.
                 */
                url(String.format("file:%s/jackson-dataformat-yaml-%s.jar", System.getProperty("project.build.directory"), System.getProperty("project.version"))),
                systemPackages(
                        "javax.annotation"),
                junitBundles(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN")
        );
    }

    @Test
    public void onceOver() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        String yaml = mapper.writeValueAsString("Hello");
        System.out.println("OSGi verification successful, result: '"+yaml+"'");
    }
}
