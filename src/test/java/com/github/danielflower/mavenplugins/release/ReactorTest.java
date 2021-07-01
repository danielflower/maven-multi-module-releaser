package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.ReleasableModuleBuilder.aModule;

public class ReactorTest {

    @Test
    public void canFindModulesByGroupAndArtifactName() throws Exception {
        ReleasableModule arty = aModule().withGroupId("my.great.group").withArtifactId("some-arty").build();
        Reactor reactor = new Reactor(asList(
            aModule().build(), arty, aModule().build()
        ));
        assertThat(reactor.find("my.great.group", "some-arty", "1.0-SNAPSHOT"), is(arty));
        assertThat(reactor.findByLabel("my.great.group:some-arty"), is(arty));
    }

    @Test
    public void findOrReturnNullReturnsNullIfNotFound() throws Exception {
        Reactor reactor = new Reactor(asList(
            aModule().build(), aModule().build()
        ));
        assertThat(reactor.findByLabel("my.great.group:some-arty"), is(nullValue()));
    }

    @Test
    public void ifNotFoundThenAUnresolvedSnapshotDependencyExceptionIsThrown() throws Exception {
        Reactor reactor = new Reactor(asList(
            aModule().build(), aModule().build()
        ));
        try {
            reactor.find("my.great.group", "some-arty", "1.0-SNAPSHOT");
            Assert.fail("Should have thrown");
        } catch (UnresolvedSnapshotDependencyException e) {
            assertThat(e.getMessage(), equalTo("Could not find my.great.group:some-arty:1.0-SNAPSHOT"));
        }
    }

    @Test
    public void returnsTheLatestTagIfThereAreChanges() throws MojoExecutionException {
        AnnotatedTag onePointNine = AnnotatedTag.create("whatever-1.1.9", "1.1", 9);
        AnnotatedTag onePointTen = AnnotatedTag.create("whatever-1.1.10", "1.1", 10);
        assertThat(Reactor.hasChangedSinceLastRelease(asList(onePointNine, onePointTen), new NeverChanged(), new MavenProject(), "whatever"), is(onePointTen));
        assertThat(Reactor.hasChangedSinceLastRelease(asList(onePointTen, onePointNine), new NeverChanged(), new MavenProject(), "whatever"), is(onePointTen));
    }

    private static class NeverChanged implements DiffDetector {
        @Override
        public boolean hasChangedSince(String modulePath, List<String> childModules, Collection<AnnotatedTag> tags) throws IOException {
            return false;
        }
    }
}
