addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.5.4")
addSbtPlugin("org.typelevel" % "sbt-typelevel-mergify" % "0.5.4")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.14.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.15")
addSbtPlugin("com.github.tkawachi" % "sbt-doctest" % "0.10.0")
addSbtPlugin("com.47deg" % "sbt-microsites" % "1.4.3")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.7")

ThisBuild / libraryDependencySchemes ++= List(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
  "com.lihaoyi" %% "geny" % VersionScheme.Always
)
