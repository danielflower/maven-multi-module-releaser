package com.github.danielflower.mavenplugins.release.log;

import org.apache.maven.plugin.logging.Log;

public interface LogHolder {

	Log getLog();

	void setLog(Log log);
}
