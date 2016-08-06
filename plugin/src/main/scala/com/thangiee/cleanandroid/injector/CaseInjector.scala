package com.thangiee.cleanandroid.injector

import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.{ScClassImpl, SyntheticMembersInjector}


class CaseInjector extends SyntheticMembersInjector {

  override def injectFunctions(source: ScTypeDefinition): Seq[String] = {
    source match {
      case obj: ScObject =>
        obj.fakeCompanionClassOrCompanionClass match {
          case clazz: ScClassImpl if clazz.findAnnotationNoAliases("com.thangiee.cleanandroid.Case") != null =>
            val params = clazz.parameters.map(param => s"${param.name}: ${param.getRealParameterType().getOrAny.toString()}").mkString(", ")
            Seq(s"def apply($params)(implicit ctx: android.content.Context): android.content.Intent = ???")
          case _ => Seq.empty
        }
      case _ => Seq.empty
    }
  }

  override def needsCompanionObject(source: ScTypeDefinition): Boolean = {
    source.findAnnotationNoAliases("com.thangiee.cleanandroid.Case") != null
  }

}
