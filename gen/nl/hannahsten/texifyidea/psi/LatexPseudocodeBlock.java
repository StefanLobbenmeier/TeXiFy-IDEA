// This is a generated file. Not intended for manual editing.
package nl.hannahsten.texifyidea.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface LatexPseudocodeBlock extends PsiElement {

  @Nullable
  LatexEnvironmentContent getEnvironmentContent();

  @NotNull
  List<LatexParameter> getParameterList();

}
