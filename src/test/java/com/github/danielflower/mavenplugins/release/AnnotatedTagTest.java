package com.github.danielflower.mavenplugins.release;

import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.Test;

public class AnnotatedTagTest {
    @Test
    public void gettersReturnValuesPassedIn() throws Exception {
        // yep, testing getters... but only because it isn't a simple POJO
        AnnotatedTag tag = AnnotatedTag.create("my-name", "the-version", new VersionInfoImpl(2134L));
        assertThat(tag.name(), equalTo("my-name"));
        assertThat(tag.version(), equalTo("the-version"));
        assertThat(tag.versionInfo().getBuildNumber(), equalTo(2134L));
    }

    @Test
    public void aTagCanBeCreatedFromAGitTag() throws GitAPIException, IOException {
        TestProject project = TestProject.singleModuleProject();
        AnnotatedTag tag = AnnotatedTag.create("my-name", "the-version", new VersionInfoImpl(2134L));
        tag.saveAtHEAD(project.local);

        Ref ref = project.local.tagList().call().get(0);
        AnnotatedTag inflatedTag = AnnotatedTag.fromRef(project.local.getRepository(), ref);
        assertThat(inflatedTag.name(), equalTo("my-name"));
        assertThat(inflatedTag.version(), equalTo("the-version"));
        assertThat(inflatedTag.versionInfo().getBuildNumber(), equalTo(2134L));
    }

    @Test
    public void ifATagIsSavedWithoutJsonThenTheVersionIsSetTo0Dot0() throws GitAPIException, IOException {
        TestProject project = TestProject.singleModuleProject();
        project.local.tag().setName("my-name-1.0.2").setAnnotated(true).setMessage("This is not json").call();

        Ref ref = project.local.tagList().call().get(0);
        AnnotatedTag inflatedTag = AnnotatedTag.fromRef(project.local.getRepository(), ref);
        assertThat(inflatedTag.name(), equalTo("my-name-1.0.2"));
        assertThat(inflatedTag.version(), equalTo("0"));
        assertThat(inflatedTag.versionInfo().getBuildNumber(), equalTo(0L));
    }

}
