organization := "com.oradian.util"
name := "exit-denied"

libraryDependencies += "org.ow2.asm" % "asm" % "9.0"

crossPaths := false
autoScalaLibrary := false

doc / javacOptions := Seq(
  "-encoding", "UTF-8",
  "-source", "8",
)
javacOptions := (doc / javacOptions).value ++ (Seq(
  "-deprecation",
  "-parameters",
  "-target", "8",
  "-Xlint",
) ++ sys.env.get("JAVA8_HOME").map { jdk8 =>
  Seq("-bootclasspath", jdk8 + "/jre/lib/rt.jar")
}.getOrElse(Nil))

Global / onChangedBuildSource := ReloadOnSourceChanges

Compile / packageBin / packageOptions +=
  Package.ManifestAttributes(
    "Premain-Class" -> "com.oradian.util.exitdenied.Agent",
    "Boot-Class-Path" -> (assembly / assemblyJarName).value,
    "Can-Retransform-Classes" -> "true",
  )

assembly / assemblyShadeRules := Seq(
  ShadeRule.rename("org.objectweb.asm.**" -> "com.oradian.util.exitdenied.shaded.@0").inAll
)
Compile / packageBin := (assembly in Compile).value

assembly / assemblyJarName := s"${name.value}-${version.value}.jar"
