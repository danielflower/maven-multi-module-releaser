Changelog
---------

### 3.3.0

* Latest tagged version from current branch is used for unchanged modules [issue #118 for details](https://github.com/danielflower/multi-module-maven-release-plugin/issues/118)

### 3.2.0

* Added support for processing version properties e.g. `<version>${foo.version}</version>`
* Added support for processing dependencies in `<dependencyManagement>` sections e.g. for BOMs
* Fix bugs with custom delimiters (joins build number to version)  

### 3.1.2

* Updated JGit to 5.3.0
* If run with `-X` (debug mode) the release invocation will write debug logs too.

### 3.0.0

* Update JGit to 4.7.0 to ensure changed files are detected for projects
  with sub-projects defined.
* Changed minimum version of java to Java 8 (required by JGit)

### 2.2.0

* Can set `pullTags=false` to prevent tags being pulled, in environments where (e.g.) your build lacks
remote access credentials. Ensure your local repository is already up to date with tags!

### 2.1.3

* Modules will be released if they cannot be resolved, even if it looks like it hasn't been changed before. See
[this pull request for details](https://github.com/danielflower/multi-module-maven-release-plugin/pull/52)

### 2.1.2

* Adding ability to control the behavior when no changes are detected. New property noChangesAction can be set
 to one of ReleaseAll, ReleaseNone, FailBuild; default is ReleaseAll.

### 2.1.0

* When custom user settings or global settings are passed in to the command line (via `-s` or `-gs` arguments)
 these are now inherited by the release portion of the build. This should make the need for `userSettings` and
 `globalSettings` options in the plugin to be unnecessary, and so these have been marked as deprecated.

#### 2.0.11

* Reverted jgit version change from 2.0.10 to support java 7

#### 2.0.10

Warning: inadvertently requires java 8 (rather than java 7). Use 2.0.11 for java 7.

* Errors if dependencies specified with properties are snapshots.
* Another jgit update for better git stuff.

#### 2.0.9

* Updated jgit version for more reliable git actions support.

#### 2.0.8

* If SCM details are specified, these are now resolved correctly.

#### 2.0.6

* Can set `pushTags=false` to prevent tags being pushed.

#### 2.0.4

* The SCM details are no longer inherited from parent POMs.
* Fixed bug [#31](https://github.com/danielflower/multi-module-maven-release-plugin/issues/31)

## 2.0.0

* Breaking change: now requires Java 7 or later to run.
* More bug fixes where it thought there were changes, even though the repo is clean.

#### 1.4.2

* Fixed some bugs around change detection in modules where sometimes changes were not being detected.

#### 1.4.1

* Added `forceRelease` option to allow forcing modules to be released even if no changes are detected.

### 1.4.0

* New feature: run `releaser:next` to see which versions will be used in the next release.

#### 1.3.4

* Fixed bug where a partial build failure where a single commit has multiple tags could result in subsequent releases
failing due to the plugin picking the older tag to use when it is detected that the module hadn't changed. 

### 1.3.0

* Added ssh-agent support thanks to [pull request 7](https://github.com/danielflower/multi-module-maven-release-plugin/pull/7)

#### 1.2.4

* Temporarily reverted version 1.2.2 as it broke compatibility with JDK 6

#### 1.2.2

* Fixed bug where the plugin would complain about symlinks (by upgrading the jgit version).

### 1.2.0

* If a parent module changes, then all child modules are updated. This covers cases where upgrading a dependency in a parent
should force all children to be updated.

### 1.1.0

* Bug fix: tags are now pushed before building so that in the event of failure, the next build will use an incremented build number. 
This is needed for cases where part of the build succeeded and some module(s) were uploaded to Nexus - re-uploading would cause an 
error if the build number is not incremented. 

#### 1.0.2

* Bug fix: When a git repository is partially checked out and the report repo has tags that the local repo does not, it was possible that the
generated version number would clash with an existing tag.

#### 1.0.1

* Feature: A list of `releaseProfiles` can now be set in the plugin config.
* Bug fix: Support cases where Windows would return `C:\` and `c:\` for different calls in some situations, causing no sub-modules to build.
* Bug fix: When one commit has multiple tags, the wrong one would sometimes be seen as the latest and cause the release to fail.

## 1.0.0

First stable release.
