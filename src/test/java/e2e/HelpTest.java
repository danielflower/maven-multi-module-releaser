package e2e;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

public class HelpTest {

    @Rule
    public TestProject singleProject = new TestProject(ProjectType.SINGLE);

    private static final String multi_module_release_help = "releaser:help";

    private static Matcher<Iterable<? super String>> containsStrings(String... strings) {
        List<Matcher<Iterable<? super String>>> matchers = new ArrayList<>();
        for (String s : strings) {
            matchers.add(CoreMatchers.hasItem(containsString(s)));
        }

        return allOf((Iterable) matchers);
    }

    @Test
    public void runningTheHelpMojoTellsYouAboutThePlugin() throws IOException {
        assertThat(mvn(multi_module_release_help),
                   containsStrings("This plugin has 3 goals:", "releaser:release", "releaser:help",
                                   multi_module_release_help));
    }

    private List<String> mvn(String... commands) throws IOException {
        return new MvnRunner().runMaven(singleProject.localDir, commands);
    }

    @Test
    public void canShowInformationAboutTheReleaseGoal() throws IOException {
        assertThat(mvn(multi_module_release_help, "-Dgoal=release", "-Ddetail=true"),
                   containsStrings("The goals to run against the project during a release"));
    }
}
