package com.github.danielflower.mavenplugins.release.pom;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.github.danielflower.mavenplugins.release.ValidationException;
import com.github.danielflower.mavenplugins.release.reactor.Reactor;

public interface Updater {

	/**
	 * Updates all necessary POMs and returns the changed files.
	 * 
	 * @param reactor
	 *            Reactor instance, must not be {@code null}
	 * @return List of updated POM files.
	 * @throws IOException
	 * @throws ValidationException
	 */
	List<File> updatePoms(Reactor reactor) throws IOException, ValidationException;

}
