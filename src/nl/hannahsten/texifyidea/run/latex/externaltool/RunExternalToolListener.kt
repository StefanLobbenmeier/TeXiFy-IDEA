package nl.hannahsten.texifyidea.run.latex.externaltool

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.impl.RunConfigurationBeforeRunProvider
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import nl.hannahsten.texifyidea.run.compiler.ExternalTool
import nl.hannahsten.texifyidea.run.latex.LatexConfigurationFactory
import nl.hannahsten.texifyidea.run.latex.LatexRunConfiguration
import nl.hannahsten.texifyidea.util.LatexPackage
import nl.hannahsten.texifyidea.util.files.psiFile
import nl.hannahsten.texifyidea.util.includedPackages

/**
 * Run the external tool.
 */
class RunExternalToolListener(
    private val latexRunConfig: LatexRunConfiguration,
    private val environment: ExecutionEnvironment,
) : ProcessListener {

    override fun processTerminated(event: ProcessEvent) {
        try {

            // Only create new one if there is none yet
            // todo isn't it expensive to check this every time?
            val runConfigSettingsList =
                if (latexRunConfig.externalToolRunConfigs.isEmpty()) {
                    generateExternalToolConfigs()
                }
                else {
                    latexRunConfig.externalToolRunConfigs
                }

            // Run all run configurations
            for (runConfigSettings in runConfigSettingsList) {
                RunConfigurationBeforeRunProvider.doExecuteTask(environment, runConfigSettings, null)
            }

            scheduleLatexRuns()
        }
        finally {
            latexRunConfig.isLastRunConfig = false
            latexRunConfig.isFirstRunConfig = true
        }
    }

    private fun scheduleLatexRuns() {
        // Don't schedule more latex runs if bibtex is used, because that will already schedule the extra runs
        if (latexRunConfig.bibRunConfigs.isEmpty() && latexRunConfig.makeindexRunConfigs.isEmpty()) {
            // LaTeX twice
            latexRunConfig.isFirstRunConfig = false
            val latexSettings = RunManagerImpl.getInstanceImpl(environment.project).getSettings(latexRunConfig)
                ?: return
            latexRunConfig.isLastRunConfig = false
            RunConfigurationBeforeRunProvider.doExecuteTask(environment, latexSettings, null)
            latexRunConfig.isLastRunConfig = true
            RunConfigurationBeforeRunProvider.doExecuteTask(environment, latexSettings, null)
        }
    }

    /**
     * Generate the extra run configs if needed.
     */
    private fun generateExternalToolConfigs(): Set<RunnerAndConfigurationSettings> {
        val runManager = RunManagerImpl.getInstanceImpl(environment.project)
        val tools = getRequiredExternalTools(latexRunConfig.mainFile, environment.project)

        val runConfigs = mutableSetOf<RunnerAndConfigurationSettings>()

        for (tool in tools) {
            val runConfigSettings = runManager.createConfiguration(
                "",
                LatexConfigurationFactory(ExternalToolRunConfigurationType())
            )

            val runConfig = runConfigSettings.configuration as ExternalToolRunConfiguration

            runConfig.mainFile = latexRunConfig.mainFile
            runConfig.program = tool
            runConfig.setSuggestedName()

            // If any external tool needs a different working directory, it can be changed here

            runManager.addConfiguration(runConfigSettings)
            runConfigs.add(runConfigSettings)
        }

        latexRunConfig.externalToolRunConfigs = runConfigs
        return runConfigs
    }

    /**
     * Check the contents of the LaTeX fileset to find out if any external tools are needed.
     */
    private fun getRequiredExternalTools(mainFile: VirtualFile?, project: Project): Set<ExternalTool> {
        val usedPackages = runReadAction {
            mainFile?.psiFile(project)?.includedPackages() ?: emptySet()
        }

        val externalTools = mutableSetOf<ExternalTool>()

        if (LatexPackage.PYTHONTEX.name in usedPackages) {
            externalTools.add(ExternalTool.PYTHONTEX)
        }

        return externalTools
    }

    override fun onTextAvailable(p0: ProcessEvent, p1: Key<*>) {}

    override fun processWillTerminate(p0: ProcessEvent, p1: Boolean) {}

    override fun startNotified(p0: ProcessEvent) {}
}