package compilation

import android.app.Activity
import com.thangiee.metadroid.{Case, Case2}

//@Case class NoParamTest() extends Activity
//
//@Case class OneParamTest(name: String = "Alice") extends Activity
//
case class Person(name: String, age: Int)
@Case class ManyParamsTest(s: Seq[String], people: Person*) extends Activity
//
//@Case class SupportCompanionObjTest(a: String) extends Activity
//object SupportCompanionObjTest {
//  val foo = "hello"
//  def bar(i: Int) = 0
//  def apply(b: String): SupportCompanionObjTest = ???
//}
//
//@Case class OptionalParamTest(opt: Option[Int]) extends Activity

@Case2 class Metaz(name: Option[String], people: Seq[Int]) extends Activity {
  people.foreach(println)
}

object AA {
//  new Metaz(???, ???).name
//  Metaz.apply()
}