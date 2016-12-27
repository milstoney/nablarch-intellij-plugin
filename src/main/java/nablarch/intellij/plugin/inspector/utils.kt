package nablarch.intellij.plugin.inspector

import com.intellij.codeInsight.*
import com.intellij.codeInspection.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import java.io.*
import java.util.*

val publishAnnotationName = "nablarch.core.util.annotation.Published"

val blacklist = HashSet<String>()

val defaultBlacklist = setOf(
    "java.lang.Exception",
    "java.lang.RuntimeException",
    "java.lang.NullPointerException",
    "java.applet.Applet",
    "java.net.HttpCookie.getName()",
    "java.net.PasswordAuthentication.PasswordAuthentication(java.lang.String, char[])",
    "java.awt"
)

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

fun addUnpermittedProblem(holder: ProblemsHolder, element: PsiElement) {
  holder.registerProblem(element, "使用不許可APIです。")
}

fun isJavaOpenApi(psiMethod: PsiMethod): Boolean {
  val name = PsiUtil.getMemberQualifiedName(psiMethod) ?: return true
  val sb = StringBuilder()
  sb.append(name).append('(')
  val paramTypes = psiMethod.getSignature(PsiSubstitutor.EMPTY).parameterTypes.map { it.canonicalText }
  paramTypes.joinTo(sb)
  sb.append(')')
  val fqcn = sb.toString()
  return !blacklist.any {
    fqcn.startsWith(it)
  }
}

fun isJavaOpenApi(psiClass: PsiClass): Boolean {
  val name = PsiUtil.getMemberQualifiedName(psiClass) ?: return true
  return !blacklist.any {
    name.startsWith(it)
  }
}

fun refreshBlacklist(blacklistFile: String) {
  blacklist.clear()
  if (blacklistFile.isEmpty()) {
    blacklist.addAll(defaultBlacklist)
  } else {
    val file = File(blacklistFile).absoluteFile
    file.forEachLine {
      blacklist.add(it)
    }
  }
}
