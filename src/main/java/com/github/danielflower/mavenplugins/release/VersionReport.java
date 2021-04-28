package com.github.danielflower.mavenplugins.release;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

public class VersionReport {

    /**
     * File format to use for @versionsReportFilePath
     */
    @Parameter
    private VersionReportFormat versionsReportFormat;

    /**
     * If true then a report with all versions will be generated.
     */
    @Parameter
    private String versionsReportFilePath;

    /**
     * If report should contain only released modules or all modules.
     */
    @Parameter(alias = "releasedModulesOnly", defaultValue="false")
    private boolean releasedModulesOnly;

    public void generateVersionReport(Log log, List<ReleasableModule> releasableModules) throws IOException {
        String res = "";
        switch (versionsReportFormat) {
            case FLAT:
                for (ReleasableModule module : releasableModules) {
                    if (skipModuleInReport(module)) continue;
                    res += String.format("%s:%s%n",
                        module.getArtifactId(), module.getVersionToDependOn());
                }
                break;
            default: //JSON as default
                JSONObject jsonObject = new JSONObject();
                for (ReleasableModule module : releasableModules) {
                    if (skipModuleInReport(module)) continue;
                    jsonObject.put(module.getArtifactId(), module.getVersionToDependOn());
                }
                res = jsonObject.toJSONString();
                break;
        }

        if (!res.equals("")) {
            try (FileWriter file = new FileWriter(versionsReportFilePath, true)) {
                file.write(res);
                log.info(format("Successfully written report file - %s", versionsReportFilePath));
            } catch (IOException e) {
                log.warn(format("Failed to write report to file %nversionsReportFilePath=%s%ncontent=%s",
                    versionsReportFilePath, res));
                throw e;
            }
        } else {
            log.info(format("Nothing to write in report, versionsReportFilePath=%s", versionsReportFilePath));
        }
    }

    private boolean skipModuleInReport(ReleasableModule module) {
        return (releasedModulesOnly && !module.willBeReleased());
    }
}
