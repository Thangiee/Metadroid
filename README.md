#Metadroid
Remove boilerplate code for Android with Scala macros.

##Getting started
Add the following to your build.sbt: 
```scala
resolvers += Resolver.jcenterRepo
libraryDependencies += "com.thangiee" %% "metadroid" % "0.1.0"
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
```

##`@Case` macro
This macro enable passing data between activities a breeze.

```scala
import com.thangiee.metadroid.Case

case class Person(name: String, age: Int)

@Case class ExampleAct(s: String, person: Person) extends Activity {
  ...
}

class OtherAct extends Activity {
  implicit val ctx: Context = ...
  startActivity(ExampleAct("hello", Person("bob", 20))) // needs an implicit Context
}
```

During compile time, `ExampleAct` is expanded to something similar to:
```scala
class ExampleAct extends Activity {
  import boopickle.Default._
  lazy val s: String = Unpickle[String].fromBytes(java.nio.ByteBuffer.wrap(getIntent.getByteArrayExtra("s")))
  lazy val person: Person = Unpickle[Person].fromBytes(java.nio.ByteBuffer.wrap(getIntent.getByteArrayExtra("person")))
  ...
}

object ExampleAct {
  def apply(s: String, person: Person)(implicit ctx: android.content.Context): android.content.Intent = {
    import boopickle.Default._
    val intent = new android.content.Intent(ctx, classOf[ExampleAct])
    intent.putExtra("s", Pickle.intoBytes(s).array())
    intent.putExtra("person", Pickle.intoBytes(person).array())
    intent
  }
}
```

###Some important things to note from the expanded version:

[Boopickle](https://github.com/ochrons/boopickle) is used to de/serialize data. 
If you use a type that is not [supported](https://github.com/ochrons/boopickle#supported-types),
you will need to provide a [custom pickler](https://github.com/ochrons/boopickle#custom-picklers). 
For example, if you need to pass a `java.util.Date`, your custom pickler would be:
```scala
implicit val datePickler = transformPickler((t: Long) => new java.util.Date(t))(_.getTime)
```
 

Class parameters are converted to lazy vals as they are not available before the `onCreate` activity
lifecycle. Therefore, any instance variable that reference a class parameter needs to be declared as lazy. 
```scala
@Case class ExampleAct(s: String, person: Person) extends Activity {
  // INCORRECT, null pointer exception 
  // val greeting = s"Hello, my name is ${person.name} and I am ${person.age} years old." 
  
  // CORRECT
  lazy val greeting = s"Hello, my name is ${person.name} and I am ${person.age} years old."
  
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    println(greeting)
  }
}
```

### IntelliJ support
The generated apply method in the companion object will be invisible to Intellij and will be
marked as an error although it will still compile successfully. To fix this, install the 
**Metadroid plugin** to get proper highlighting.

1) _(Coming soon)_ Search and Install the plugin from JetBrains plugin repository. 

  --**OR**--

1) Download the plugin from https://github.com/Thangiee/Metadroid/tree/master/plugin/bin

2) Go to settings, Plugins section, then click on install plugin from disc, and choose this plugin. 

### Todo
* Currently, if a companion object already exists, the `@Case` macro won't work. Macro needs to 
combine the existing companion object with the generated companion object.

* Support fragment 