addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.8.0")
addSbtPlugin("org.typelevel" % "sbt-typelevel-mergify" % "0.8.0")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.18.2")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.17")
addSbtPlugin("io.github.sbt-doctest" % "sbt-doctest" % "0.11.2")
addSbtPlugin("com.47deg" % "sbt-microsites" % "1.4.4")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.7.1")

ThisBuild / libraryDependencySchemes ++= List(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
  "com.lihaoyi" %% "geny" % VersionScheme.Always
)
