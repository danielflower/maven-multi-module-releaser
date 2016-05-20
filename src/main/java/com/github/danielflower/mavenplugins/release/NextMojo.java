package com.github.danielflower.mavenplugins.release;

import static java.util.Arrays.asList;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.danielflower.mavenplugins.release.reactor.Reactor;

/**
 * Logs the versions of the modules that the releaser will release on the next
 * release. Does not run the build nor tag the repo.
 * 
 * @since 1.4.0
 */
@Mojo(name = "next", requiresDirectInvocation = true, // this should not be
														// bound to a phase as
														// this plugin starts a
														// phase itself
inheritByDefault = true, // so you can configure this in a shared parent pom
requiresProject = true, // this can only run against a maven project
aggregator = true // the plugin should only run once against the aggregator pom
)
public class NextMojo extends BaseMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();
		try {
			configureJsch();
			final String remoteUrl = getRemoteUrlOrNullIfNoneSet(project.getScm());
			final Reactor reactor = newReactor(remoteUrl);
			figureOutTagNamesAndThrowIfAlreadyExists(reactor, remoteUrl);

		} catch (final ValidationException e) {
			printBigErrorMessageAndThrow(e.getMessage(), e.getMessages());
		} catch (final GitAPIException gae) {

			final StringWriter sw = new StringWriter();
			gae.printStackTrace(new PrintWriter(sw));
			final String exceptionAsString = sw.toString();

			printBigErrorMessageAndThrow("Could not release due to a Git error",
					asList("There was an error while accessing the Git repository. The error returned from git was:",
							gae.getMessage(), "Stack trace:", exceptionAsString));
		}
	}

}
