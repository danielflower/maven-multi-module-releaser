package com.github.danielflower.mavenplugins.release;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.WriterFactory;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PomUpdater {

    private final ArtifactFactory artifactFactory;
    private final ArtifactMetadataSource artifactMetadataSource;
    private final ArtifactRepository localRepository;
    private final List<ArtifactRepository> remoteArtifactRepositories;
    private final Log log;
    private final Reactor reactor;
    private final List<String> resolveSnapshots;

    public PomUpdater(Log log, Reactor reactor, ArtifactFactory artifactFactory,
                      ArtifactMetadataSource artifactMetadataSource, ArtifactRepository localRepository,
                      List<ArtifactRepository> remoteArtifactRepositories,
                      List<String> resolveSnapshots) {
        this.log = log;
        this.reactor = reactor;
        this.artifactFactory = artifactFactory;
        this.artifactMetadataSource = artifactMetadataSource;
        this.localRepository = localRepository;
        this.remoteArtifactRepositories = remoteArtifactRepositories;
        this.resolveSnapshots = resolveSnapshots;
    }

    public UpdateResult updateVersion() {
        List<File> changedPoms = new ArrayList<File>();
        List<String> errors = new ArrayList<String>();
        for (ReleasableModule module : reactor.getModulesInBuildOrder()) {
            try {
                MavenProject project = module.getProject();
                if (module.willBeReleased()) {
                    log.info("Going to release " + module.getArtifactId() + " " + module.getNewVersion());
                }

                List<String> errorsForCurrentPom = alterModel(project, module.getNewVersion());
                errors.addAll(errorsForCurrentPom);

                File pom = project.getFile().getCanonicalFile();
                changedPoms.add(pom);
                Writer fileWriter = WriterFactory.newXmlWriter(pom);

                Model originalModel = project.getOriginalModel();
                try {
                    MavenXpp3Writer pomWriter = new MavenXpp3Writer();
                    pomWriter.write(fileWriter, originalModel);
                } finally {
                    fileWriter.close();
                }
            } catch (Exception e) {
                return new UpdateResult(changedPoms, errors, e);
            }
        }
        return new UpdateResult(changedPoms, errors, null);
    }

    public static class UpdateResult {
        public final List<File> alteredPoms;
        public final List<String> dependencyErrors;
        public final Exception unexpectedException;

        public UpdateResult(List<File> alteredPoms, List<String> dependencyErrors, Exception unexpectedException) {
            this.alteredPoms = alteredPoms;
            this.dependencyErrors = dependencyErrors;
            this.unexpectedException = unexpectedException;
        }
        public boolean success() {
            return (dependencyErrors.size() == 0) && (unexpectedException == null);
        }
    }

    /**
     * Check whether the given dependencySnapshot is matched by the resolveSnapshotPattern (regular expression).
     *
     * @param dependencySnapshot a dependency string in a gradle like notation, i.e., groupId:artifactId:version
     * @param resolveSnapshotPattern a regular expression pattern which is used in the plugin configuration, e.g.,
     *                               ^org\.slf4j:slf4j-api:1\.7\..*
     * @return if the dependency string matches the regex
     */
    protected static boolean snapshotResolves(String dependencySnapshot, String resolveSnapshotPattern) {
        Pattern resolveSnapshotPatternAsPattern = Pattern.compile(resolveSnapshotPattern);
        Matcher matcher = resolveSnapshotPatternAsPattern.matcher(dependencySnapshot);

        return matcher.matches();
    }

    private List<String> alterModel(MavenProject project, String newVersion) {
        Model originalModel = project.getOriginalModel();
        originalModel.setVersion(newVersion);

        List<String> errors = new ArrayList<String>();

        String searchingFrom = project.getArtifactId();
        MavenProject parent = project.getParent();
        if (parent != null && isSnapshot(parent.getVersion())) {
            try {
                ReleasableModule parentBeingReleased = reactor.find(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
                originalModel.getParent().setVersion(parentBeingReleased.getVersionToDependOn());
                log.debug(" Parent " + parentBeingReleased.getArtifactId() + " rewritten to version " + parentBeingReleased.getVersionToDependOn());
            } catch (UnresolvedSnapshotDependencyException e) {
                errors.add("The parent of " + searchingFrom + " is " + e.artifactId + " " + e.version);
            }
        }

        Properties projectProperties = project.getProperties();
        for (Dependency dependency : originalModel.getDependencies()) {
            String version = dependency.getVersion();
            if (isSnapshot(resolveVersion(version, projectProperties))) {
                try {
                    ReleasableModule dependencyBeingReleased = reactor.find(dependency.getGroupId(), dependency.getArtifactId(), version);
                    dependency.setVersion(dependencyBeingReleased.getVersionToDependOn());
                    log.debug(" Dependency on " + dependencyBeingReleased.getArtifactId() + " rewritten to version " + dependencyBeingReleased.getVersionToDependOn());
                } catch (UnresolvedSnapshotDependencyException e) {
                    boolean resolveSnapshotFound = false;
                    if (null != resolveSnapshots) {
                        String dependencySnapshot = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + version;
                        log.debug(" Trying to resolve snapshot: '" + dependencySnapshot + "' with ...");
                        for (String resolveSnapshot : resolveSnapshots) {
                            log.debug(" ...: '" + resolveSnapshot + "'");
                            if (snapshotResolves(dependencySnapshot, resolveSnapshot)) {
                                String resolvedVersion = resolveSnapshotDependency (dependency);
                                log.debug(" Resolving snapshot dependency: '" + dependencySnapshot + "' to '" + resolvedVersion + "'");
                                if (null != resolvedVersion) {
                                    resolveSnapshotFound = true;
                                    dependency.setVersion(resolvedVersion);
                                }
                                // Break loop anyways: If release was not found, it is an error ...
                                break;
                            }
                        }
                    }
                    if (!resolveSnapshotFound) {
                        errors.add(searchingFrom + " references dependency " + e.artifactId + " " + e.version);
                    }
                }
            }else
                log.debug(" Dependency on " + dependency.getArtifactId() + " kept at version " + dependency.getVersion());
        }
        for (Plugin plugin : project.getModel().getBuild().getPlugins()) {
            String version = plugin.getVersion();
            if (isSnapshot(resolveVersion(version, projectProperties))) {
                if (!isMultiModuleReleasePlugin(plugin)) {
                    errors.add(searchingFrom + " references plugin " + plugin.getArtifactId() + " " + version);
                }
            }
        }
        return errors;
    }

	private String resolveVersion(String version, Properties projectProperties) {
		if (version != null && version.startsWith("${")) {
			return projectProperties.getProperty(version.replace("${", "").replace("}", ""), version);
		}
		return version;
	}

    private String resolveSnapshotDependency (Dependency dependency)
    {
        Artifact artifact = createDependencyArtifact(dependency);
        log.debug ("Retrieving versions for artifact: " + artifact);
        try {
            List<ArtifactVersion> versions =
                artifactMetadataSource.retrieveAvailableVersions( artifact, localRepository, remoteArtifactRepositories);
            if (versions.size() == 0) {
                log.error("Could not retrieve versions for artifact: " + artifact);
                return null;
            }
            Collections.sort(versions, new Comparator<ArtifactVersion>() {
                @Override
                public int compare(ArtifactVersion o1, ArtifactVersion o2) {
                    return o1.compareTo(o2);
                }
            });
            ArtifactVersion latestVersion = versions.get(versions.size() - 1);
            log.debug("Using version '" + latestVersion + "' for '" + artifact + "'");
            return latestVersion.toString();
        } catch (ArtifactMetadataRetrievalException e) {
            log.error("Could not retrieve versions for artifact: " + artifact, e);
            return null;
        }
    }

    // Stolen from Versions Maven Plugin
    private Artifact createDependencyArtifact(String groupId, String artifactId, VersionRange versionRange, String type,
                                              String classifier, String scope )
    {
        return artifactFactory.createDependencyArtifact( groupId, artifactId, versionRange, type, classifier, scope );
    }

    private Artifact createDependencyArtifact( Dependency dependency )
    {
        String strippedVersion = dependency.getVersion().replace("-SNAPSHOT", ".0");
        String versionRangeSpec = "[" + strippedVersion + ",]";
        VersionRange versionRange = null;
        try {
            versionRange = VersionRange.createFromVersionSpec(versionRangeSpec);
        } catch (InvalidVersionSpecificationException e) {
            log.error ("Could not resolve version range '" + versionRangeSpec + "'");
            return null;
        }

        return createDependencyArtifact(
            dependency.getGroupId(),
            dependency.getArtifactId(),
            versionRange,
            dependency.getType(),
            dependency.getClassifier(),
            dependency.getScope()
        );
    }


    private static boolean isMultiModuleReleasePlugin(Plugin plugin) {
        return plugin.getGroupId().equals("com.github.danielflower.mavenplugins") && plugin.getArtifactId().equals("multi-module-maven-release-plugin");
    }

    private boolean isSnapshot(String version) {
        return (version != null && version.endsWith("-SNAPSHOT"));
    }

}
