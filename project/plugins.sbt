addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.4.16")
addSbtPlugin("org.typelevel" % "sbt-typelevel-mergify" % "0.4.16")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.2.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.11.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.10")
addSbtPlugin("com.github.tkawachi" % "sbt-doctest" % "0.10.0")
addSbtPlugin("com.47deg" % "sbt-microsites" % "1.4.1")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.7")

ThisBuild / libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
