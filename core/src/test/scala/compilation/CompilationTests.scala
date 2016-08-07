package compilation

import android.app.Activity
import com.thangiee.metadroid.Case

@Case class NoParamTest() extends Activity

@Case class OneParamTest(name: String = "hello") extends Activity

case class Person(name: String, age: Int)
@Case class ManyParamsTest(s: String, person: Person) extends Activity

@Case class SupportCompanionObjTest(a: String) extends Activity
object SupportCompanionObjTest {
  val foo = "hello"
  def bar(i: Int) = 0
  def apply(b: String): SupportCompanionObjTest = ???
}
