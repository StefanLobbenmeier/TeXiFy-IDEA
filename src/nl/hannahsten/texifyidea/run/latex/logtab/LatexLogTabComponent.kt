package nl.hannahsten.texifyidea.run.latex.logtab

import com.intellij.diagnostic.logging.AdditionalTabComponent
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import nl.hannahsten.texifyidea.run.latex.ui.LatexCompileMessageTreeView
import java.awt.BorderLayout
import javax.swing.JComponent

/**
 * Runner tab component displaying LaTeX log messages in a more readable and navigatable format.
 *
 * @param startedProcess The LaTeX compile process.
 *
 * @author Sten Wessel
 */
class LatexLogTabComponent(val project: Project, val mainFile: VirtualFile?, startedProcess: ProcessHandler) : AdditionalTabComponent(BorderLayout()) {

    private val latexMessageList = mutableListOf<LatexLogMessage>()
    private val bibtexMessageList = mutableListOf<LatexLogMessage>()
    private val treeView = LatexCompileMessageTreeView(project)

    init {
        add(treeView, BorderLayout.CENTER)
        startedProcess.addProcessListener(LatexOutputListener(project, mainFile, latexMessageList, bibtexMessageList, treeView), this)
    }

    override fun getTabTitle() = "Log messages"

    override fun dispose() {
    }

    override fun getPreferredFocusableComponent() = component

    override fun getToolbarActions(): ActionGroup? = null

    override fun getToolbarContextComponent(): JComponent? = null

    override fun getToolbarPlace(): String? = null

    override fun getSearchComponent(): JComponent? = null

    override fun isContentBuiltIn() = false
}
