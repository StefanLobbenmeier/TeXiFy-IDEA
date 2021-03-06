package nl.hannahsten.texifyidea.inspections.latex

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import nl.hannahsten.texifyidea.insight.InsightGroup
import nl.hannahsten.texifyidea.inspections.TexifyInspectionBase
import nl.hannahsten.texifyidea.lang.LatexPackage.Companion.AMSMATH
import nl.hannahsten.texifyidea.psi.LatexNormalText
import nl.hannahsten.texifyidea.util.*
import nl.hannahsten.texifyidea.util.magic.PatternMagic

/**
 * @author Sten Wessel
 */
open class LatexEllipsisInspection : TexifyInspectionBase() {

    override val inspectionGroup = InsightGroup.LATEX

    override fun getDisplayName() = "Ellipsis with ... instead of \\ldots or \\dots"

    override val inspectionId = "Ellipsis"

    override fun inspectFile(file: PsiFile, manager: InspectionManager, isOntheFly: Boolean): MutableList<ProblemDescriptor> {
        val descriptors = descriptorList()
        val texts = file.childrenOfType(LatexNormalText::class)

        for (text in texts) {
            ProgressManager.checkCanceled()

            for (match in PatternMagic.ellipsis.findAll(text.text)) {
                // Ignore the inspection when the ellipsis is inside a comment.
                if (file.findElementAt(match.range.first + text.startOffset)?.isComment() == true) {
                    continue
                }

                descriptors.add(
                    manager.createProblemDescriptor(
                        text,
                        match.range.toTextRange(),
                        "Ellipsis with ... instead of command",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        isOntheFly,
                        InsertEllipsisCommandFix(text.inMathContext())
                    )
                )
            }
        }

        return descriptors
    }

    /**
     * @author Sten Wessel
     */
    private class InsertEllipsisCommandFix(val inMathMode: Boolean) : LocalQuickFix {

        override fun getFamilyName() = "Convert to ${if (inMathMode) "\\dots (amsmath package)" else "\\ldots"}"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val file = element.containingFile
            val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
            val range = descriptor.textRangeInElement.shiftRight(element.textOffset)

            document.replaceString(range.startOffset, range.endOffset, if (inMathMode) "\\dots" else "\\ldots")

            if (inMathMode && AMSMATH.name !in file.includedPackages()) {
                file.insertUsepackage(AMSMATH)
            }
        }
    }
}
