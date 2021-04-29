package com.github.danielflower.mavenplugins.release;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;

/**
 * @author Roland Hauser sourcepond@gmail.com
 *
 */
public abstract class BaseMojo extends AbstractMojo {
	/**
	 * The Maven Project.
	 */
	@Parameter(property = "project", required = true, readonly = true, defaultValue = "${project}")
	protected MavenProject project;

	@Parameter(property = "projects", required = true, readonly = true, defaultValue = "${reactorProjects}")
	protected List<MavenProject> projects;

	/**
	 * <p>
	 * The build number to use in the release version. Given a snapshot version
	 * of "1.0-SNAPSHOT" and a buildNumber value of "2", the actual released
	 * version will be "1.0.2".
	 * </p>
	 * <p>
	 * By default, the plugin will automatically find a suitable build number.
	 * It will start at version 0 and increment this with each release.
	 * </p>
	 * <p>
	 * This can be specified using a command line parameter ("-DbuildNumber=2")
	 * or in this plugin's configuration.
	 * </p>
	 */
	@Parameter(property = "buildNumber")
	protected Long buildNumber;


    /**
     * <p>
     * Configures the version naming.
     * </p>
     */
    @Parameter(property = "versionNamer")
    protected VersionNamer versionNamer = new VersionNamer(".");

	/**
	 * The modules to release, or no value to to release the project from the
	 * root pom, which is the default. The selected module plus any other
	 * modules it needs will be built and released also. When run from the
	 * command line, this can be a comma-separated list of module names.
	 */
	@Parameter(alias = "modulesToRelease", property = "modulesToRelease")
	protected List<String> modulesToRelease;

	/**
	 * A module to force release on, even if no changes has been detected.
	 */
	@Parameter(alias = "forceRelease", property = "forceRelease")
	protected List<String> modulesToForceRelease;

    /**
     * Determines the action to take when no module changes are detected. Possible values:
     * {@code ReleaseAll}, {@code ReleaseNone}, {@code FailBuild}
     */
    @Parameter(alias = "noChangesAction", defaultValue="ReleaseAll", property = "noChangesAction")
    protected NoChangesAction noChangesAction;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    protected ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    protected ArtifactResolver artifactResolver;

    /**
     * List of Remote Repositories used by the resolver
     */
    @Parameter(property = "remoteRepositories", required = true, readonly = true, defaultValue = "${project.remoteArtifactRepositories}")
    protected List remoteRepositories;

    /**
     * Location of the local repository.
     *
     */
    @Parameter(property = "localRepository", required = true, readonly = true, defaultValue = "${localRepository}")
    protected ArtifactRepository localRepository;

	@Parameter(property = "disableSshAgent")
	private boolean disableSshAgent;

	@Parameter(defaultValue = "${settings}", readonly = true, required = true)
	private Settings settings;

	/**
	 * <p>If set, the identityFile and passphrase will be read from the Maven settings file.</p>
     * <p>See <a href="https://maven.apache.org/guides/mini/guide-deployment-security-settings.html">https://maven.apache.org/guides/mini/guide-deployment-security-settings.html</a>
     * for more information on configuring servers in Maven.</p>
	 */
	@Parameter(property = "serverId")
	private String serverId;

	/**
	 * If set, this file will be used to specify the known_hosts. This will
	 * override any default value.
	 */
	@Parameter(property = "knownHosts")
	private String knownHosts;

	/**
	 * Specifies the private key to be used for SSH URLs. By default it will use <code>~/.ssh/id_rsa</code>
	 */
	@Parameter(property = "privateKey")
	private String privateKey;

	/**
	 * <p>Specifies the passphrase to be used with the identityFile specified for SSH where the private key requires a pass phrase.</p>
     * <p>To avoid specifying a passphrase in your pom, you could instead specify a <code>server</code> in your
     * maven settings file and then set the <code>serverId</code> property.</p>
	 */
	@Parameter(property = "passphrase")
	private String passphrase;

    /**
     * Fetch tags from remote repository to determine the next build number. If
     * false, then tags from the local repository will be used instead. Make
     * sure they are up to date to avoid problems.
     */
    @Parameter(alias = "pullTags", property = "pull", defaultValue = "true")
    protected boolean pullTags;

    /**
     * <p>Additional arguments to pass to Maven during a release.</p>
     * <p>To pass multiple system properties from the command line,
     * use <code>-Darguments="-Dprop.1=prop1value -Dprop.2=prop2value"</code></p>
     * <p>To configure arguments in your pom, in the <code>&lt;configuration&gt;</code> node add:
     * <code>&lt;arguments&gt;'-Dprop.1=prop 1 value' -Dprop.2=prop2value&lt;/arguments&gt;</code></p>
     */
    @Parameter(property = "arguments")
    public String arguments;

    /**
     * <p>Determines the separator between artifactId and groupId<p>
     * <p>By default, set to '-'</p>
     */
    @Parameter(alias = "tagNameSeparator", required = false, readonly = true, defaultValue = "-", property = "tagNameSeparator")
    protected String tagNameSeparator;
 
    final void setSettings(final Settings settings) {
		this.settings = settings;
	}

    final Settings getSettings() {
        return settings;
    }

	final void setServerId(final String serverId) {
		this.serverId = serverId;
	}

	final void setKnownHosts(final String knownHosts) {
		this.knownHosts = knownHosts;
	}

	final void setPrivateKey(final String privateKey) {
		this.privateKey = privateKey;
	}

	final void setPassphrase(final String passphrase) {
		this.passphrase = passphrase;
	}

	final void disableSshAgent() {
		disableSshAgent = true;
	}

	protected CredentialsProvider getCredentialsProvider(final Log log) throws ValidationException {
        if (serverId != null) {
            Server server = settings.getServer(serverId);
            if (server == null) {
                log.warn(format("No server configuration in Maven settings found with id %s", serverId));
            }
            if (server.getUsername() != null && server.getPassword() != null) {
                return new UsernamePasswordCredentialsProvider(server.getUsername(), server.getPassword());
            }
        }
        return null;
    }

	protected final void configureJsch(final Log log) {
		if (!disableSshAgent) {
			if (serverId != null) {
				final Server server = settings.getServer(serverId);
				if (server != null) {
					privateKey = privateKey == null ? server.getPrivateKey() : privateKey;
					passphrase = passphrase == null ? server.getPassphrase() : passphrase;
				} else {
					log.warn(format("No server configuration in Maven settings found with id %s", serverId));
				}
			}

			JschConfigSessionFactory.setInstance(new SshAgentSessionFactory(log, knownHosts, privateKey, passphrase));
		}
	}

    static void printBigErrorMessageAndThrow(Log log, String terseMessage, List<String> linesToLog) throws MojoExecutionException {
        log.error("");
        log.error("");
        log.error("");
        log.error("************************************");
        log.error("Could not execute the release plugin");
        log.error("************************************");
        log.error("");
        log.error("");
        for (String line : linesToLog) {
            log.error(line);
        }
        log.error("");
        log.error("");
        throw new MojoExecutionException(terseMessage);
    }

    protected static String getRemoteUrlOrNullIfNoneSet(Scm originalScm, Scm actualScm) throws ValidationException {
        if (originalScm == null) {
            // No scm was specified, so don't inherit from any parent poms as they are probably used in different git repos
            return null;
        }

        // There is an SCM specified, so the actual SCM with derived values is used in case (so that variables etc are interpolated)
        String remote = actualScm.getDeveloperConnection();
        if (remote == null) {
            remote = actualScm.getConnection();
        }
        if (remote == null) {
            return null;
        }
        return GitHelper.scmUrlToRemote(remote);
    }

}
