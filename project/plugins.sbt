addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.7.4")
addSbtPlugin("org.typelevel" % "sbt-typelevel-mergify" % "0.7.4")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.16.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.5")
addSbtPlugin("io.github.sbt-doctest" % "sbt-doctest" % "0.11.0")
addSbtPlugin("com.47deg" % "sbt-microsites" % "1.4.4")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.6.1")

ThisBuild / libraryDependencySchemes ++= List(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
  "com.lihaoyi" %% "geny" % VersionScheme.Always
)
