package nablarch.intellij.plugin.inspector

import com.intellij.codeInsight.*
import com.intellij.codeInspection.*
import com.intellij.psi.*

val publishAnnotationName = "nablarch.core.util.annotation.Published"

fun isPublishedApi(element: PsiModifierListOwner, categories: List<String>): Boolean {
  return AnnotationUtil.findAnnotation(element, publishAnnotationName)?.let {
    val tags = it.findAttributeValue("tag")

    when (tags) {
      is PsiLiteralExpression -> tags.value == null || categories.contains(tags.value)
      is PsiArrayInitializerMemberValue -> {
        val tagsString: List<String> = tags.initializers.mapNotNull { if (it is PsiLiteralExpression) it.value.toString() else null }.filterNot(String::isNullOrBlank)
        if (tagsString.isEmpty()) {
          true
        } else {
          categories.any { tagsString.contains(it) }
        }
      }
      else -> false
    }
  } ?: false
}

fun isNablarchClass(psiClass: PsiClass?): Boolean = psiClass?.qualifiedName?.startsWith("nablarch.") ?: false

fun addProblem(holder: ProblemsHolder, element: PsiElement, tags: List<String>) {
  val option = if (tags.filter(String::isNotBlank).isNotEmpty()) {
    "(許可タグリスト:" + tags.joinToString(",") + ")"
  } else {
    "(許可タグリストなし)"
  }
  holder.registerProblem(element, "非公開APIです。$option")
}

fun addBlacklistProblem(holder: ProblemsHolder, element: PsiElement) {
  holder.registerProblem(element, "使用不許可APIです。")
}
