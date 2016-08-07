package compiling

import android.app.Activity
import com.thangiee.metadroid.Case

@Case class NoParamTest() extends Activity

@Case class OneParamTest(name: String) extends Activity

case class Person(name: String, age: Int)
@Case class ManyParamsTest(s: String, person: Person) extends Activity

