import android.Keys._

onLoad in Global := ((s: State) => { "updateIdea" :: s}) compose (onLoad in Global).value

lazy val commonSettings = Seq(
  version := "0.1.0",
  organization := "com.thangiee",
  scalaVersion := "2.11.8"
)

lazy val core = project
  .settings(commonSettings ++ androidBuildAar)
  .settings(
    name := "metadroid",
    minSdkVersion := "4",
    platformTarget := "android-24",
    typedResources := false,
    libraryDependencies ++= Seq(
      "com.google.android" % "android" % "4.1.1.4" % "provided",
      "org.scala-lang" % "scala-reflect" % "2.11.8",
      "me.chrons" %% "boopickle" % "1.2.4"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

lazy val plugin: Project = project
    .enablePlugins(SbtIdeaPlugin)
    .settings(commonSettings)
    .settings(
      name := "metadroid-plugin",
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

packagePlugin in plugin <<= (assembly in plugin,
  target in plugin,
  ivyPaths) map { (ideaJar, target, paths) =>
  val pluginName = "metadroid-plugin"
  val ivyLocal = paths.ivyHome.getOrElse(file(System.getProperty("user.home")) / ".ivy2") / "local"
  val sources = Seq(
    ideaJar -> s"$pluginName/lib/${ideaJar.getName}"
  )
  val out = target / s"$pluginName-plugin.zip"
  IO.zip(sources, out)
  out
}