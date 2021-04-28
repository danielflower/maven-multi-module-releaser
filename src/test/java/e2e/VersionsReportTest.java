package e2e;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static scaffolding.ExactCountMatcher.oneOf;
import static scaffolding.ExactCountMatcher.twoOf;

public class VersionsReportTest {

    final String buildNumber = String.valueOf(System.currentTimeMillis());
    final String expectedParentVersion = "1.0." + buildNumber;
    final String expectedCoreVersion = "2.0." + buildNumber;
    final String expectedAppVersion = "3.2." + buildNumber;
    final String releasedVersionsReportFlatFileName = "released-report.txt";
    final String releasedVersionsReportJsonFileName = "released-report.json";
    final String allVersionsReportJsonFileName = "version-report.json";
    final List<String> reportedVersions = Arrays.asList(
        "independent-versions:" + expectedParentVersion,
        "core-utils:" + expectedCoreVersion,
        "console-app:" + expectedAppVersion);
    final TestProject testProject = TestProject.versionReportProject();

    @BeforeClass
    public static void installPluginToLocalRepo() throws MavenInvocationException {
        MvnRunner.installReleasePluginToLocalRepo();
    }

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        buildsEachProjectOnceAndOnlyOnce(testProject.mvnRelease(buildNumber));
        reportsFilesGeneratedWithCorrectVersionsAndFormat();
    }

    private void reportsFilesGeneratedWithCorrectVersionsAndFormat() throws IOException {
        List<String> fileLines = Files.readAllLines(new File(testProject.localDir, releasedVersionsReportFlatFileName).toPath(), Charset.defaultCharset());
        assertTrue(fileLines.size() == reportedVersions.size() && fileLines.containsAll(reportedVersions) && reportedVersions.containsAll(fileLines));
    }

    private void buildsEachProjectOnceAndOnlyOnce(List<String> commandOutput) {
        assertThat(
            commandOutput,
            allOf(
                oneOf(containsString("Going to release independent-versions " + expectedParentVersion)),
                twoOf(containsString("Building independent-versions")), // once for initial build; once for release build
                oneOf(containsString("Building core-utils")),
                oneOf(containsString("Building console-app")),
                oneOf(containsString("The Calculator Test has run")),
                oneOf(containsString("Successfully written report file - " + releasedVersionsReportFlatFileName)),
                oneOf(containsString("Successfully written report file - " + releasedVersionsReportJsonFileName)),
                oneOf(containsString("Successfully written report file - " + allVersionsReportJsonFileName))
            )
        );
    }
}
