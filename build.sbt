import android.Keys._

onLoad in Global := ((s: State) => { "updateIdea" :: s}) compose (onLoad in Global).value

lazy val commonSettings = Seq(
  organization := "com.thangiee",
  scalaVersion := "2.11.8"
)

lazy val core = project
  .settings(commonSettings ++ androidBuildAar)
  .settings(
    name := "metadroid",
    version := "0.1.1",
    minSdkVersion := "4",
    platformTarget := "android-24",
    javacOptions in Compile ++= "-source" :: "1.7" :: "-target" :: "1.7" :: Nil,
    typedResources := false,
    libraryDependencies ++= Seq(
      "com.google.android" % "android" % "4.1.1.4" % "provided",
      "org.scala-lang" % "scala-reflect" % "2.11.8",
      "me.chrons" %% "boopickle" % "1.2.4",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0" % "provided",
      "ch.qos.logback" % "logback-classic" % "1.1.7" % "provided"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )
  .settings(
    publishMavenStyle := true,
    bintrayReleaseOnPublish in ThisBuild := false, //  1."sbt core/publish" stage artifacts first 2."sbt core/bintrayRelease" make artifacts public
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    bintrayVcsUrl := Some("https://github.com/Thangiee/Metadroid")
  )

val pluginVer = "0.1.1"
val pluginName = "metadroid-plugin"
lazy val plugin: Project = project
    .enablePlugins(SbtIdeaPlugin)
    .settings(commonSettings)
    .settings(
      name := pluginName,
      version := pluginVer,
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
      ideaInternalPlugins := Seq(),
      ideaExternalPlugins := Seq(IdeaPlugin.Zip("scala-plugin", url("https://plugins.jetbrains.com/plugin/download?pr=idea&updateId=27109"))),
      aggregate in updateIdea := false,
      assemblyExcludedJars in assembly <<= ideaFullJars,
      ideaBuild := "162.1628.6"
    )

lazy val ideaRunner: Project = project.in(file("ideaRunner"))
  .dependsOn(plugin % Provided)
  .settings(commonSettings)
  .settings(
    name := "ideaRunner",
    autoScalaLibrary := false,
    unmanagedJars in Compile <<= ideaMainJars.in(plugin),
    unmanagedJars in Compile += file(System.getProperty("java.home")).getParentFile / "lib" / "tools.jar"
  )

lazy val packagePlugin = TaskKey[File]("package-plugin", "Create plugin's zip file ready to load into IDEA")

packagePlugin in plugin <<= (assembly in plugin, ivyPaths) map { (ideaJar, paths) =>
  val ivyLocal = paths.ivyHome.getOrElse(file(System.getProperty("user.home")) / ".ivy2") / "local"
  val sources = Seq(ideaJar -> s"$pluginName/lib/${ideaJar.getName}")
  val out = plugin.base / "bin" / s"$pluginName-$pluginVer.zip"
  IO.zip(sources, out)
  out
}