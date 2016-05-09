package scaffolding;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.version.Version;

public class ReleasableModuleBuilder {

	MavenProject project = new MavenProject();
	private long buildNumber = 123;
	private String equivalentVersion = null;
	private String relativePathToModule = ".";

	public ReleasableModuleBuilder withBuildNumber(final long buildNumber) {
		this.buildNumber = buildNumber;
		return this;
	}

	public ReleasableModuleBuilder withEquivalentVersion(final String equivalentVersion) {
		this.equivalentVersion = equivalentVersion;
		return this;
	}

	public ReleasableModuleBuilder withRelativePathToModule(final String relativePathToModule) {
		this.relativePathToModule = relativePathToModule;
		return this;
	}

	public ReleasableModuleBuilder withGroupId(final String groupId) {
		project.setGroupId(groupId);
		return this;
	}

	public ReleasableModuleBuilder withArtifactId(final String artifactId) {
		project.setArtifactId(artifactId);
		return this;
	}

	public ReleasableModuleBuilder withSnapshotVersion(final String snapshotVersion) {
		project.setVersion(snapshotVersion);
		return this;
	}

	public ReleasableModule build() throws ValidationException {
		final Version version = mock(Version.class);
		when(version.buildNumber()).thenReturn(buildNumber);
		when(version.developmentVersion()).thenReturn(project.getVersion());
		when(version.releaseVersion()).thenReturn(project.getVersion().replace("-SNAPSHOT", ".") + buildNumber);
		return new ReleasableModule(project, version, equivalentVersion, relativePathToModule);
	}

	public static ReleasableModuleBuilder aModule() {
		return new ReleasableModuleBuilder().withGroupId("com.github.danielflower.somegroup")
				.withArtifactId("some-artifact");
	}
}
