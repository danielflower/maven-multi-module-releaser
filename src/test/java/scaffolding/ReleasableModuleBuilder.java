package scaffolding;

import com.github.danielflower.mavenplugins.release.ReleasableModule;
import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.VersionNamer;
import org.apache.maven.project.MavenProject;

public class ReleasableModuleBuilder {

    private final VersionNamer versionNamer = new VersionNamer();
    MavenProject project = new MavenProject();
    private long buildNumber = 123;
    private String equivalentVersion = null;
    private String relativePathToModule = ".";

    public ReleasableModuleBuilder withBuildNumber(long buildNumber) {
        this.buildNumber = buildNumber;
        return this;
    }

    public ReleasableModuleBuilder withEquivalentVersion(String equivalentVersion) {
        this.equivalentVersion = equivalentVersion;
        return this;
    }

    public ReleasableModuleBuilder withRelativePathToModule(String relativePathToModule) {
        this.relativePathToModule = relativePathToModule;
        return this;
    }

    public ReleasableModuleBuilder withGroupId(String groupId) {
        project.setGroupId(groupId);
        return this;
    }

    public ReleasableModuleBuilder withArtifactId(String artifactId) {
        project.setArtifactId(artifactId);
        return this;
    }

    public ReleasableModuleBuilder withSnapshotVersion(String snapshotVersion) {
        project.setVersion(snapshotVersion);
        return this;
    }

    public ReleasableModule build() throws ValidationException {
        return new ReleasableModule(project, versionNamer.name(project.getVersion(), buildNumber, null), equivalentVersion, relativePathToModule, "-");
    }

    public static ReleasableModuleBuilder aModule() {
        return new ReleasableModuleBuilder()
            .withGroupId("com.github.danielflower.somegroup")
            .withArtifactId("some-artifact");
    }
}
