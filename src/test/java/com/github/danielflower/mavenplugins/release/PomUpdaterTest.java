package com.github.danielflower.mavenplugins.release;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/*
  In fact, this is currently just a test for Java pattern matching ... Hence, you could easily add much more patterns,
  but would hardly increase the module quality. In the end the plugin user has to provide the right pattern
  configuration.
 */
public class PomUpdaterTest {

    @Test
    public void testGroupIdPrefix () {
        assertTrue ("groupIdPrefix",
            PomUpdater.snapshotResolves("net.aschemann.test:test-module:1.3-SNAPSHOT", "^net\\.aschemann.*"));
    }

    @Test
    public void testArtifactIdInfix () {
        assertTrue ("artifactIdInfix",
            PomUpdater.snapshotResolves("net.aschemann.test:test-module:1.3-SNAPSHOT", "^net\\.aschemann.*:.*st-modu.*:.*"));
    }

}
