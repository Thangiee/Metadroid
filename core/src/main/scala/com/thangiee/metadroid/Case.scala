package com.thangiee.metadroid

import com.typesafe.scalalogging.LazyLogging

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class Case extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro CaseImpl.impl
}

object CaseImpl extends LazyLogging {
  val namespace = "com.thangiee.metadroid."

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    // covert type A* -> Seq[A]
    val convertRepeatedIntoSeq: ValDef => ValDef = {
      case ValDef(mod, termName, AppliedTypeTree(Select(_, TypeName("<repeated>")), typeName), rhs) =>
        ValDef(mod, termName, AppliedTypeTree(Ident(TypeName("Seq")), typeName), rhs)
      case good => good // not a type A*, don't need to do anything
    }

    // Unpickle[A*] is a syntax error, convert to Unpickle[Seq[A]] if need be
    val deserializeParam: ValDef => ValDef = convertRepeatedIntoSeq andThen { case ValDef(_, name, typ, _) =>
      val rhs = q"Unpickle[$typ].fromBytes(java.nio.ByteBuffer.wrap(getIntent.getByteArrayExtra(${namespace+name.toString})))"
      typ match {
        case AppliedTypeTree(Ident(TypeName("Option")), _) =>
          ValDef(Modifiers(Flag.LAZY), name, typ, q"scala.util.Try(..$rhs).recover{case _ => None}.toOption.flatten")
        case _ => ValDef(Modifiers(Flag.LAZY), name, typ, rhs)
      }
    }

    val serializeParam: ValDef => Tree = param =>
      q"intent.putExtra(${namespace + param.name.toString}, Pickle.intoBytes(${param.name}).array())"

    val trees = annottees.map(_.tree).toList

    val result: c.universe.Tree = trees.headOption match {
      case Some(q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends $parent with ..$parents { $self => ..$stats }") =>
        val classParams: Seq[ValDef] = paramss.flatten

        val className: TermName = tpname match {
          case name: TypeName => name.toTermName
          case other => c.abort(c.enclosingPosition, s"Fail to extract class name: ${showRaw(other)}")
        }

        val genClass =
          q"""
            $mods class $tpname[..$tparams] $ctorMods() extends $parent with ..$parents { $self =>
              import boopickle.Default._
              ..${classParams.map(deserializeParam) ++ stats}
            }
          """

        val genCompanionObj = trees.tail.headOption match {
          case Some(q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }") =>
            q"""
              object $tname extends { ..$earlydefns } with ..$parents { $self =>
                ..$body
                def apply(...$paramss)(implicit ctx: android.content.Context): android.content.Intent = {
                  import boopickle.Default._
                  val intent = new android.content.Intent(ctx, classOf[$tpname])
                  ..${classParams.map(serializeParam)}
                  intent
                }
              }
            """
          case None => // no companion obj defined
            q"""
              object $className {
                def apply(...$paramss)(implicit ctx: android.content.Context): android.content.Intent = {
                  import boopickle.Default._
                  val intent = new android.content.Intent(ctx, classOf[$tpname])
                  ..${classParams.map(serializeParam)}
                  intent
                }
              }
            """
          case other => c.abort(c.enclosingPosition, s"Expected companion object but got: ${showRaw(other)}")
        }

        q"..${List(genClass, genCompanionObj)}"
      case other => c.abort(c.enclosingPosition, s"Expected class but got: ${showRaw(other)}")
    }

    logger.debug(result.toString())

    c.Expr[Any](result)
  }
}
