package com.github.danielflower.mavenplugins.release.version;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.scm.SCMRepository;

/**
 * @author rolandhauser
 *
 */
public class DefaultVersionTest {
	private static final String ARTIFACT_ID = "anyArtifactId";
	private final SCMRepository gitRepo = mock(SCMRepository.class);
	private final Git git = mock(Git.class);
	private final ListTagCommand cmd = mock(ListTagCommand.class);
	private final MavenProject project = mock(MavenProject.class);

	@Before
	public void setup() throws GitAPIException {
		when(project.getArtifactId()).thenReturn(ARTIFACT_ID);
		final List<Ref> ref = Collections.emptyList();
		when(cmd.call()).thenReturn(ref);
		when(git.tagList()).thenReturn(cmd);
	}

	@Test
	public void versionNamerCaresNotForOrderOfTags() throws Exception {
		when(gitRepo.getRemoteBuildNumbers(ARTIFACT_ID, "1.1.1")).thenReturn(asList(1L, 3L, 2L));
		final Version version = new DefaultVersion(gitRepo, project, "1.1.1", null);
		assertThat(version.releaseVersion(), equalTo("1.1.1.4"));
	}

	@Test
	public void removesTheSnapshotAndSticksTheBuildNumberOnTheEnd() throws Exception {
		when(project.getVersion()).thenReturn("1.0-SNAPSHOT");
		when(gitRepo.getRemoteBuildNumbers(ARTIFACT_ID, "1.0")).thenReturn(Collections.<Long> emptyList());
		final Version version = new DefaultVersion(gitRepo, project, "1.0", 123L);
		assertEquals(version.releaseVersion(), "1.0.123");
	}

	@Test
	public void ifTheBuildNumberIsNullAndThePreviousBuildNumbersIsEmptyListThenZeroIsUsed() throws Exception {
		when(project.getVersion()).thenReturn("1.0-SNAPSHOT");
		when(gitRepo.getRemoteBuildNumbers(ARTIFACT_ID, "1.0")).thenReturn(Collections.<Long> emptyList());
		final Version version = new DefaultVersion(gitRepo, project, "1.0", null);
		assertEquals("1.0.0", version.releaseVersion());
	}

	@Test
	public void ifTheBuildNumberIsNullButThereIsAPreviousBuildNumbersThenThatValueIsIncremented() throws Exception {
		when(project.getVersion()).thenReturn("1.0-SNAPSHOT");
		when(gitRepo.getRemoteBuildNumbers(ARTIFACT_ID, "1.0")).thenReturn(asList(9L, 10L, 8L));
		final Version version = new DefaultVersion(gitRepo, project, "1.0", null);
		assertEquals("1.0.11", version.releaseVersion());
	}

	// FIXME: This test must be re-implemented in the "repository" package
	@Test
	@Ignore
	public void throwsIfTheVersionWouldNotBeAValidGitTag() throws MojoExecutionException, GitAPIException {
		assertThat(errorMessageOf("1.0-A : yeah /-SNAPSHOT", 0), hasItems(
				"Sorry, '1.0-A : yeah /.0' is not a valid version.",
				"Please see https://www.kernel.org/pub/software/scm/git/docs/git-check-ref-format.html for tag naming rules."));
	}

	private List<String> errorMessageOf(final String pomVersion, final long buildNumber)
			throws MojoExecutionException, GitAPIException {
		try {
			when(project.getVersion()).thenReturn(pomVersion);
			new DefaultVersion(gitRepo, project, "1.0", buildNumber);
			throw new AssertionError("Did not throw an error");
		} catch (final ValidationException ex) {
			return ex.getMessages();
		}
	}
}
