package com.github.danielflower.mavenplugins.release;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.github.danielflower.mavenplugins.release.MavenVersionResolver.resolveVersionsDefinedThroughProperties;

public class Reactor {

    private final List<ReleasableModule> modulesInBuildOrder;

    public Reactor(List<ReleasableModule> modulesInBuildOrder) {
        this.modulesInBuildOrder = modulesInBuildOrder;
    }

    public List<ReleasableModule> getModulesInBuildOrder() {
        return modulesInBuildOrder;
    }

    public static Reactor fromProjects(Log log, LocalGitRepo gitRepo, MavenProject rootProject, List<MavenProject> projects, Long buildNumber, List<String> modulesToForceRelease, NoChangesAction actionWhenNoChangesDetected, ResolverWrapper resolverWrapper, VersionNamer versionNamer, String tagNameSeparator) throws ValidationException, GitAPIException, MojoExecutionException {
        DiffDetector detector = new TreeWalkingDiffDetector(gitRepo.git.getRepository());
        List<ReleasableModule> modules = new ArrayList<ReleasableModule>();

        resolveVersionsDefinedThroughProperties(projects);

        AnnotatedTagFinder annotatedTagFinder = new AnnotatedTagFinder(versionNamer);
        for (MavenProject project : projects) {
            String relativePathToModule = calculateModulePath(rootProject, project);
            String artifactId = project.getArtifactId();
            String versionWithoutBuildNumber = project.getVersion().replace("-SNAPSHOT", "");
            List<AnnotatedTag> previousTagsForThisModule = annotatedTagFinder.tagsForVersion(gitRepo.git, artifactId, versionWithoutBuildNumber, tagNameSeparator);


            Collection<Long> previousBuildNumbers = new ArrayList<Long>();
            if (previousTagsForThisModule != null) {
                for (AnnotatedTag previousTag : previousTagsForThisModule) {
                    previousBuildNumbers.add(previousTag.buildNumber());
                }
            }

            Collection<Long> remoteBuildNumbers = getRemoteBuildNumbers(gitRepo, artifactId, versionWithoutBuildNumber, versionNamer);
            previousBuildNumbers.addAll(remoteBuildNumbers);

            VersionName newVersion = versionNamer.name(project.getVersion(), buildNumber, previousBuildNumbers);

            boolean oneOfTheDependenciesHasChanged = false;
            String changedDependency = null;
            for (ReleasableModule module : modules) {
                if (module.willBeReleased()) {
                    for (Dependency dependency : project.getModel().getDependencies()) {
                        if (hasSameMavenGA(module, dependency)) {
                            oneOfTheDependenciesHasChanged = true;
                            changedDependency = dependency.getArtifactId();
                            break;
                        }
                    }

                    if (project.getModel().getDependencyManagement() != null) {
                        for (Dependency dependency : project.getModel().getDependencyManagement().getDependencies()) {
                            if (hasSameMavenGAByDependencyLocation(module, dependency)) {
                                oneOfTheDependenciesHasChanged = true;
                                changedDependency = module.getArtifactId();
                                break;
                            }
                        }
                    }

                    if (project.getParent() != null && hasSameMavenGA(module, project.getParent())) {
                        oneOfTheDependenciesHasChanged = true;
                        changedDependency = project.getParent().getArtifactId();
                        break;
                    }
                }
                if (oneOfTheDependenciesHasChanged) {
                    break;
                }
            }

            String equivalentVersion = null;

            if(modulesToForceRelease != null && modulesToForceRelease.contains(artifactId)) {
                log.info("Releasing " + artifactId + " " + newVersion.releaseVersion() + " as we was asked to forced release.");
            } else if (oneOfTheDependenciesHasChanged) {
                log.info("Releasing " + artifactId + " " + newVersion.releaseVersion() + " as " + changedDependency + " has changed.");
            } else {
                AnnotatedTag previousTagThatIsTheSameAsHEADForThisModule = hasChangedSinceLastRelease(previousTagsForThisModule, detector, project, relativePathToModule);
                if (previousTagThatIsTheSameAsHEADForThisModule != null) {
                    equivalentVersion = previousTagThatIsTheSameAsHEADForThisModule.version() + versionNamer.getDelimiter() + previousTagThatIsTheSameAsHEADForThisModule.buildNumber();
                    // attempt to resolve
                    if (resolverWrapper.isResolvable(project.getGroupId(), project.getArtifactId(), equivalentVersion, project.getPackaging(), log)) {
                        log.info("Will use version " + equivalentVersion + " for " + artifactId + " as it has not been changed since that release.");
                    } else {
                        log.info("Will use version " + newVersion.releaseVersion() + " for " + artifactId + " as although no change was detected, the artifact cannot be resolved!");
                        equivalentVersion = null;
                    }
                } else {
                    log.info("Will use version " + newVersion.releaseVersion() + " for " + artifactId + " as it has changed since the last release.");
                }
            }
            ReleasableModule module = new ReleasableModule(project, newVersion, equivalentVersion, relativePathToModule, tagNameSeparator);
            modules.add(module);
        }

        if (!atLeastOneBeingReleased(modules)) {
            switch (actionWhenNoChangesDetected) {
                case ReleaseNone:
                    log.warn("No changes have been detected in any modules so will not perform release");
                    return null;
                case FailBuild:
                    throw new MojoExecutionException("No module changes have been detected");
                default:
                    log.warn("No changes have been detected in any modules so will re-release them all");
                    List<ReleasableModule> newList = new ArrayList<ReleasableModule>();
                    for (ReleasableModule module : modules) {
                        newList.add(module.createReleasableVersion());
                    }
                    modules = newList;
            }
        }

        return new Reactor(modules);
    }

    private static boolean hasSameMavenGA(ReleasableModule module, Dependency dependency) {
        return dependency.getGroupId().equals(module.getGroupId()) && dependency.getArtifactId().equals(module.getArtifactId());
    }

    private static boolean hasSameMavenGA(ReleasableModule module, MavenProject project) {
        return project.getGroupId().equals(module.getGroupId()) && project.getArtifactId().equals(module.getArtifactId());
    }

    private static boolean hasSameMavenGAByDependencyLocation(ReleasableModule module, Dependency dependency) {
        String[] modelId = dependency.getLocation("").getSource().getModelId().split(":");
        return modelId[0].equals(module.getGroupId()) && modelId[1].equals(module.getArtifactId());
    }

    private static Collection<Long> getRemoteBuildNumbers(LocalGitRepo gitRepo, String artifactId, String versionWithoutBuildNumber, VersionNamer versionNamer) throws GitAPIException {
        Collection<Ref> remoteTagRefs = gitRepo.allTags();
        Collection<Long> remoteBuildNumbers = new ArrayList<Long>();
        String tagWithoutBuildNumber = artifactId + "-" + versionWithoutBuildNumber;
        AnnotatedTagFinder annotatedTagFinder = new AnnotatedTagFinder(versionNamer);
        for (Ref remoteTagRef : remoteTagRefs) {
            String remoteTagName = remoteTagRef.getName();
            Long buildNumber = annotatedTagFinder.buildNumberOf(tagWithoutBuildNumber, remoteTagName);
            if (buildNumber != null) {
                remoteBuildNumbers.add(buildNumber);
            }
        }
        return remoteBuildNumbers;
    }

    private static boolean atLeastOneBeingReleased(List<ReleasableModule> modules) {
        for (ReleasableModule module : modules) {
            if (module.willBeReleased()) {
                return true;
            }
        }
        return false;
    }

    private static String calculateModulePath(MavenProject rootProject, MavenProject project) throws MojoExecutionException {
        // Getting canonical files because on Windows, it's possible one returns "C:\..." and the other "c:\..." which is rather amazing
        File projectRoot;
        File moduleRoot;
        try {
            projectRoot = rootProject.getBasedir().getCanonicalFile();
            moduleRoot = project.getBasedir().getCanonicalFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not find directory paths for maven project", e);
        }
        String relativePathToModule = Repository.stripWorkDir(projectRoot, moduleRoot);
        if (relativePathToModule.length() == 0) {
            relativePathToModule = ".";
        }
        return relativePathToModule;
    }

    static AnnotatedTag hasChangedSinceLastRelease(List<AnnotatedTag> previousTagsForThisModule, DiffDetector detector, MavenProject project, String relativePathToModule) throws MojoExecutionException {
        try {
            if (previousTagsForThisModule.size() == 0) return null;
            boolean hasChanged = detector.hasChangedSince(relativePathToModule, project.getModel().getModules(), previousTagsForThisModule);
            return hasChanged ? null : tagWithHighestBuildNumber(previousTagsForThisModule);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while detecting whether or not " + project.getArtifactId() + " has changed since the last release", e);
        }
    }

    private static AnnotatedTag tagWithHighestBuildNumber(List<AnnotatedTag> tags) {
        AnnotatedTag cur = null;
        for (AnnotatedTag tag : tags) {
            if (cur == null || tag.buildNumber() > cur.buildNumber()) {
                cur = tag;
            }
        }
        return cur;
    }

    public ReleasableModule findByLabel(String label) {
        for (ReleasableModule module : modulesInBuildOrder) {
            String currentLabel = module.getGroupId() + ":" + module.getArtifactId();
            if (currentLabel.equals(label)) {
                return module;
            }
        }
        return null;
    }

    public ReleasableModule find(String groupId, String artifactId, String version) throws UnresolvedSnapshotDependencyException {
        ReleasableModule value = findByLabel(groupId + ":" + artifactId);
        if (value == null) {
            throw new UnresolvedSnapshotDependencyException(groupId, artifactId, version);
        }
        return value;
    }
}
