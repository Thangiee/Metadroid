package com.thangiee.metadroid

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class Case extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro CaseImpl.impl
}

object CaseImpl {
  val namespace = "com.thangiee.metadroid."

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val result = {
      annottees.map(_.tree).toList match {
        case q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends $parent with ..$parents { $self => ..$stats }" :: Nil =>

          val classParams: Seq[ValDef] = paramss.flatten

          val serializeParams = classParams.map(param =>
            q"intent.putExtra(${namespace+param.name.toString}, Pickle.intoBytes(${param.name}).array())"
          )

          val deserializeParams = classParams.map { case ValDef(_, name, typ, _) =>
            val rhs = q"Unpickle[$typ].fromBytes(java.nio.ByteBuffer.wrap(getIntent.getByteArrayExtra(${namespace+name.toString})))"
            ValDef(Modifiers(Flag.LAZY), name, typ, rhs)
          }

          val className: c.universe.TermName = tpname match {
            case name: TypeName => name.toTermName
            case other => c.abort(c.enclosingPosition, s"Fail to extract class name: ${showRaw(other)}")
          }

          val out = q"""
            class $tpname[..$tparams] $ctorMods() extends $parent with ..$parents { $self =>
              import boopickle.Default._
              ..${deserializeParams ++ stats}
            }

            object $className {
              def apply(...$paramss)(implicit ctx: android.content.Context): android.content.Intent = {
                import boopickle.Default._
                val intent = new android.content.Intent(ctx, classOf[$tpname])
                ..$serializeParams
                intent
              }
            }
          """
          println(out)
          out

        case other => c.abort(c.enclosingPosition, showRaw(other))
      }
    }

    c.Expr[Any](result)
  }
}
