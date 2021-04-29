package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Logs the versions of the modules that the releaser will release on the next release. Does not run the build nor
 * tag the repo.
 * @since 1.4.0
 */
@Mojo(
    name = "next",
    requiresDirectInvocation = true, // this should not be bound to a phase as this plugin starts a phase itself
    inheritByDefault = true, // so you can configure this in a shared parent pom
    requiresProject = true, // this can only run against a maven project
    aggregator = true // the plugin should only run once against the aggregator pom
)
public class NextMojo extends BaseMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();

        try {
            configureJsch(log);

            Set<GitOperations> gitOperations = EnumSet.noneOf(GitOperations.class);
            if (pullTags) {
                gitOperations.add(GitOperations.PULL_TAGS);
            }

            LocalGitRepo repo = new LocalGitRepo.Builder()
                .remoteGitOperationsAllowed(gitOperations)
                .remoteGitUrl(getRemoteUrlOrNullIfNoneSet(project.getOriginalModel().getScm(),
                                                          project.getModel().getScm()))
                .credentialsProvider(getCredentialsProvider(log))
                .buildFromCurrentDir();
            ResolverWrapper resolverWrapper = new ResolverWrapper(factory, artifactResolver, remoteRepositories, localRepository);
            Reactor reactor = Reactor.fromProjects(log, repo, project, projects, buildNumber, modulesToForceRelease, noChangesAction, resolverWrapper, versionNamer, tagNameSeparator);
            if (reactor == null) {
                return;
            }
            ReleaseMojo.figureOutTagNamesAndThrowIfAlreadyExists(reactor.getModulesInBuildOrder(), repo, modulesToRelease);

        } catch (ValidationException e) {
            printBigErrorMessageAndThrow(log, e.getMessage(), e.getMessages());
        } catch (GitAPIException gae) {

            StringWriter sw = new StringWriter();
            gae.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            printBigErrorMessageAndThrow(log, "Could not release due to a Git error",
                asList("There was an error while accessing the Git repository. The error returned from git was:",
                    gae.getMessage(), "Stack trace:", exceptionAsString));
        }
    }

}
