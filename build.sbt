import android.Keys._

lazy val commonSettings = Seq(
  version := "0.1.0",
  organization := "com.thangiee",
  scalaVersion := "2.11.8"
)

lazy val core = project
  .settings(commonSettings ++ androidBuildAar)
  .settings(
    name := "Clean Android",
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


