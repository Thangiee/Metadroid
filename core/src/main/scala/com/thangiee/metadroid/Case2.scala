package com.thangiee.metadroid

import scala.annotation.StaticAnnotation
import scala.collection.immutable.Seq
import scala.meta.Defn.Val
import scala.meta.Term.{Apply, Block, Param}
import scala.meta._

class Case2 extends StaticAnnotation {

  inline def apply(defn: Any): Any = meta {
    defn match {
      case cls @ Defn.Class(_, _, _, ctor @ Ctor.Primary(_, _, paramss), template) =>
        val classParams: Seq[Param] = paramss.flatten
//        val params = classParams.map(CaseUtils.convertRepeatedIntoSeq)
//        params.map(CaseUtils.deserializeParamAsVal).foreach(println)
//        val template"{ ..$stats1 } with ..$ctorcalls { $param => ..$stats2 }" = template
//        println(stats1, ctorcalls, param, stats2)

//        val genClass =
//          q"""
//            $mods class $tpname[..$tparams] $ctorMods() extends $parent with ..$parents { $self =>
//              import boopickle.Default._
//              ..${classParams.map(deserializeParam) ++ stats}
//            }
//          """
//        q"..$mods class $tname[..$tparams] ..$ctorMods (..$params) extends $template {}"
//        val t = template.stats.map(seq => seq :+ q"val bar = 0")
//        val u = q"{  }"
//        val cls = q"..$mods class $tname[..$tparams] ..$ctorMods (..${params.tail}) extends ${template.copy(stats = t)}"
//        val main = q"def main(args: Array[String]): Unit = { ..$stats2}"
//        val companion   = q"object ${Term.Name(tname.value)} { def apply(..$params): $tname = ??? }"
//        val res = Term.Block(Seq(cls, companion))
//        println(res.syntax)
        val q: Block = q"..${classParams.map(CaseUtils.deserializeParamAsVal)}"
        val res = (q"import boopickle.Default._" +: q.stats) ++ template.stats.getOrElse(Nil)
//        val res = q"""
//           import boopickle.Default._
//           ..${classParams.map(CaseUtils.deserializeParamAsVal)}
//           ..${template.stats.getOrElse(Nil)}
//         """

        val genClass = cls.copy(ctor = ctor.copy(paramss = Nil), templ = template.copy(stats = Some(res)))

        println(genClass.syntax)
        genClass
      case other =>
        println("FAIL")
        abort(s"Expected class but got: ${other.syntax}")
    }
  }
}

object CaseUtils {
  val namespace = "com.thangiee.metadroid."

  // covert type A* -> Seq[A]
  def convertRepeatedIntoSeq(param: Param): Param = param match {
    case p @ Param(_, name, Some(t"$tpe[..$tpesnel]"), _) if tpe.syntax.contains("<repeated>") =>
      p.copy(decltpe = Some(t"Seq[..$tpesnel]"))
    case good => good // not a type A*, don't need to do anything
  }

  def deserializeParamAsVal: Param => Val = convertRepeatedIntoSeq _ andThen { case p @ Param(_, name, Some(tpe), _) =>
    val returnType = Type.Name(tpe.syntax)
    val rhs = q"Unpickle[$returnType].fromBytes(java.nio.ByteBuffer.wrap(getIntent.getByteArrayExtra(${namespace+name.syntax})))"
    def valExpr(expr: Term): Val = q"lazy val ${Pat.Var.Term(Term.Name(name.syntax))}: $returnType = $expr"

    tpe match {
      case t"Option[..$_]" => valExpr(q"scala.util.Try($rhs).recover{case _ => None}.toOption.flatten")
      case _               => valExpr(rhs)
    }
  }

  def serializeParam(param: Param): Term =
    q"intent.putExtra(${Term.Name(namespace + param.name)}, Pickle.intoBytes(${Term.Name(param.name.syntax)}).array())"

}
