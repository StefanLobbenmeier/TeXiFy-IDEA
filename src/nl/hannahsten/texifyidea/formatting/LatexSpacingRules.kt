package nl.hannahsten.texifyidea.formatting

import com.intellij.formatting.Spacing
import com.intellij.psi.codeStyle.CodeStyleSettings
import nl.hannahsten.texifyidea.LatexLanguage
import nl.hannahsten.texifyidea.psi.LatexTypes.*
import nl.hannahsten.texifyidea.settings.codestyle.LatexCodeStyleSettings

/**
 *
 * @author Sten Wessel
 */
fun createSpacingBuilder(settings: CodeStyleSettings): LatexSpacingBuilder {
    fun createSpacing(minSpaces: Int, maxSpaces: Int, minLineFeeds: Int, keepLineBreaks: Boolean, keepBlankLines: Int) : Spacing =
            Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines)

    val latexSettings = settings.getCustomSettings(LatexCodeStyleSettings::class.java)
    val latexCommonSettings = settings.getCommonSettings(LatexLanguage.INSTANCE)

    return rules(latexCommonSettings) {

        simple {
            between(NORMAL_TEXT_WORD, NORMAL_TEXT_WORD).spaces(1)
            around(ENVIRONMENT_CONTENT).lineBreakInCode()
        }

        custom {
            fun commentSpacing(minSpaces: Int): Spacing {
                if (latexCommonSettings.KEEP_FIRST_COLUMN_COMMENT) {
                    return Spacing.createKeepingFirstColumnSpacing(minSpaces, Int.MAX_VALUE, latexCommonSettings.KEEP_LINE_BREAKS, latexCommonSettings.KEEP_BLANK_LINES_IN_CODE)
                }
                return createSpacing(minSpaces, Int.MAX_VALUE, 0, latexCommonSettings.KEEP_LINE_BREAKS, latexCommonSettings.KEEP_BLANK_LINES_IN_CODE)
            }

            inPosition(right = COMMENT_TOKEN).spacing(commentSpacing(0))

            customRule { parent, left, right ->
                if (right.node?.text?.matches(Regex("\\\\section\\{.*\\}")) == true) {
                    return@customRule createSpacing(
                            minSpaces = 0,
                            maxSpaces = Int.MAX_VALUE,
                            minLineFeeds = latexSettings.BLANK_LINES_BEFORE_SECTIONING + 1,
                            keepLineBreaks = latexCommonSettings.KEEP_LINE_BREAKS,
                            keepBlankLines = latexCommonSettings.KEEP_BLANK_LINES_IN_CODE)
                }
                return@customRule Spacing.getReadOnlySpacing()
            }


        }
    }
}