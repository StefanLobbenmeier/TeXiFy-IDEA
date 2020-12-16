package nl.hannahsten.texifyidea.psi

import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import nl.hannahsten.texifyidea.BibtexLanguage
import nl.hannahsten.texifyidea.lang.magic.DefaultMagicKeys
import nl.hannahsten.texifyidea.lang.magic.magicComment
import nl.hannahsten.texifyidea.psi.impl.LatexCommandsImpl
import nl.hannahsten.texifyidea.util.Magic
import nl.hannahsten.texifyidea.util.camelCase
import nl.hannahsten.texifyidea.util.parentOfType

/**
 * Inject language based on magic comments.
 *
 * @author Sten Wessel
 */
class LatexLanguageInjector : LanguageInjector {
    override fun getLanguagesToInject(host: PsiLanguageInjectionHost, registrar: InjectedLanguagePlaces) {
        if (host is LatexEnvironment) {

            val magicComment = host.magicComment()
            val hasMagicCommentKey = magicComment.containsKey(DefaultMagicKeys.INJECT_LANGUAGE)

            val languageId = when {
                hasMagicCommentKey -> {
                    magicComment.value(DefaultMagicKeys.INJECT_LANGUAGE)
                }
                host.environmentName == "lstlisting" -> {
                    host.beginCommand.optionalParameters.getOrDefault("language", null)
                }
                else -> {
                    Magic.Environment.languageInjections[host.environmentName]
                }
            }

            val language = findLanguage(languageId) ?: return

            val range = host.environmentContent?.textRange?.shiftRight(-host.textOffset) ?: TextRange.EMPTY_RANGE

            return registrar.addPlace(language, range, null, null)
        }

        if (host is LatexParameter) {
            val parent = host.parentOfType(LatexCommands::class) ?: return

            val languageId = Magic.Command.languageInjections[parent.commandToken.text.substring(1)]
            val language = findLanguage(languageId) ?: return
            val range = host.textRange
                .shiftRight(-host.textOffset)
                .let { TextRange(it.startOffset + 1, it.endOffset - 1) }

            return registrar.addPlace(language, range, null, null)
        }
    }

    private fun findLanguage(id: String?): Language? {
        return if (id.isNullOrBlank()) null
        else {
            Language.findLanguageByID(id)
                ?: Language.findLanguageByID(id.toLowerCase())
                ?: Language.findLanguageByID(id.toUpperCase())
                ?: Language.findLanguageByID(id.camelCase())
        }
    }
}