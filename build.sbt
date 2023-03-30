addCommandAlias("validateJVM", "all scalafmtCheckAll scalafmtSbtCheck testsJVM/test")
addCommandAlias("validateJS", "all testsJS/test")
addCommandAlias("validateNative", "all testsNative/test")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")

val Scala212 = "2.12.17"
val Scala213 = "2.13.10"
val Java8 = JavaSpec.temurin("8")

val gitRepo = "git@github.com:typelevel/cats-tagless.git"
val homePage = "https://typelevel.org/cats-tagless"

// GitHub actions configuration
ThisBuild / organizationName := "cats-tagless maintainers"
ThisBuild / tlBaseVersion := "0.14"

ThisBuild / crossScalaVersions := Seq(Scala212, Scala213)
ThisBuild / tlCiReleaseBranches := Seq("master")
ThisBuild / mergifyStewardConfig := Some(
  MergifyStewardConfig(
    author = "typelevel-steward[bot]",
    mergeMinors = true
  )
)
ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    "microsite",
    "Microsite",
    githubWorkflowJobSetup.value.toList ::: List(
      WorkflowStep.Use(
        UseRef.Public("ruby", "setup-ruby", "v1"),
        name = Some("Setup Ruby"),
        params = Map("ruby-version" -> "2.6", "bundler-cache" -> "true")
      ),
      WorkflowStep.Run(List("gem install jekyll -v 2.5"), name = Some("Install Jekyll")),
      WorkflowStep.Sbt(List("docs/makeMicrosite"), name = Some("Build microsite"))
    ),
    scalas = List(Scala213),
    javas = List(Java8)
  )
)

val catsVersion = "2.9.0"
val circeVersion = "0.14.5"
val disciplineVersion = "1.5.1"
val disciplineMunitVersion = "1.0.9"
val kindProjectorVersion = "0.13.2"
val paradiseVersion = "2.1.1"
val scalaCheckVersion = "1.17.0"

val macroSettings = List(
  libraryDependencies ++=
    List("scala-compiler", "scala-reflect").map("org.scala-lang" % _ % scalaVersion.value % Provided),
  scalacOptions ++= (scalaBinaryVersion.value match {
    case "2.13" => List("-Ymacro-annotations")
    case _ => Nil
  }),
  libraryDependencies ++= (scalaBinaryVersion.value match {
    case "2.13" => Nil
    case _ => List(compilerPlugin(("org.scalamacros" %% "paradise" % paradiseVersion).cross(CrossVersion.full)))
  })
)

lazy val root = tlCrossRootProject.aggregate(core, laws, tests, macros)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native
lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .enablePlugins(AutomateHeaderPlugin)
  .jsSettings(commonJsSettings)
  .nativeSettings(commonNativeSettings)
  .settings(rootSettings)
  .settings(
    moduleName := "cats-tagless-core",
    libraryDependencies += "org.typelevel" %%% "cats-core" % catsVersion
  )

lazy val lawsJVM = laws.jvm
lazy val lawsJS = laws.js
lazy val lawsNative = laws.native
lazy val laws = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .dependsOn(core)
  .enablePlugins(AutomateHeaderPlugin)
  .jsSettings(commonJsSettings)
  .nativeSettings(commonNativeSettings)
  .settings(rootSettings)
  .settings(
    moduleName := "cats-tagless-laws",
    libraryDependencies ++= List(
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline-core" % disciplineVersion
    )
  )

lazy val macrosJVM = macros.jvm
lazy val macrosJS = macros.js
lazy val macrosNative = macros.native
lazy val macros = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .dependsOn(core)
  .aggregate(core)
  .enablePlugins(AutomateHeaderPlugin)
  .jsSettings(commonJsSettings)
  .nativeSettings(commonNativeSettings)
  .settings(rootSettings, macroSettings)
  .settings(
    moduleName := "cats-tagless-macros",
    scalacOptions := scalacOptions.value.filterNot(_.startsWith("-Wunused")).filterNot(_.startsWith("-Ywarn-unused")),
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "3.3.0" % Optional,
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test
    )
  )

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js
lazy val testsNative = tests.native
lazy val tests = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .dependsOn(macros, laws)
  .enablePlugins(AutomateHeaderPlugin, NoPublishPlugin)
  .jsSettings(commonJsSettings)
  .jsSettings(scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)))
  .nativeSettings(commonNativeSettings)
  .settings(rootSettings, macroSettings)
  .settings(
    moduleName := "cats-tagless-tests",
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++= List(
      "org.typelevel" %%% "cats-free" % catsVersion,
      "org.typelevel" %%% "cats-testkit" % catsVersion,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion,
      "io.circe" %%% "circe-core" % circeVersion
    ).map(_ % Test)
  )

/** Docs - Generates and publishes the scaladoc API documents and the project web site. */
lazy val docs = project
  .dependsOn(macrosJVM)
  .enablePlugins(MicrositesPlugin, SiteScaladocPlugin, NoPublishPlugin)
  .settings(docSettings, macroSettings)
  .settings(
    moduleName := "cats-tagless-docs",
    libraryDependencies += "org.typelevel" %%% "cats-free" % catsVersion
  )

lazy val docsMappingsAPIDir = settingKey[String]("Name of subdirectory in site target directory for api docs")
lazy val rootSettings = (scalacOptions += "-Xsource:3") :: commonSettings
lazy val docSettings = commonSettings ::: List(
  docsMappingsAPIDir := "api",
  addMappingsToSiteDir(coreJVM / Compile / packageDoc / mappings, docsMappingsAPIDir),
  organization := "org.typelevel",
  autoAPIMappings := true,
  micrositeName := "Cats-tagless",
  micrositeDescription := "A library of utilities for tagless final algebras",
  micrositeBaseUrl := "cats-tagless",
  micrositeGithubOwner := "typelevel",
  micrositeGithubRepo := "cats-tagless",
  micrositeHighlightTheme := "atom-one-light",
  micrositeTheme := "pattern",
  micrositePalette := Map(
    "brand-primary" -> "#51839A",
    "brand-secondary" -> "#EDAF79",
    "brand-tertiary" -> "#96A694",
    "gray-dark" -> "#192946",
    "gray" -> "#424F67",
    "gray-light" -> "#E3E2E3",
    "gray-lighter" -> "#F4F3F4",
    "white-color" -> "#FFFFFF"
  ),
  ghpagesNoJekyll := false,
  micrositeAuthor := "cats-tagless Contributors",
  scalacOptions -= "-Xfatal-warnings",
  git.remoteRepo := gitRepo,
  makeSite / includeFilter := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

lazy val commonSettings = List(
  scalaVersion := (ThisBuild / scalaVersion).value,
  crossScalaVersions := (ThisBuild / crossScalaVersions).value,
  Test / parallelExecution := false,
  resolvers ++= Resolver.sonatypeOssRepos("releases"),
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  startYear := Some(2019),
  apiURL := Some(url("https://typelevel.org/cats-tagless/api/")),
  autoAPIMappings := true
)

lazy val commonJsSettings = List(
  // currently sbt-doctest doesn't work in JS builds
  // https://github.com/tkawachi/sbt-doctest/issues/52
  doctestGenTests := Nil
)

lazy val commonNativeSettings = List(
  doctestGenTests := Nil
)

ThisBuild / homepage := Some(url(homePage))
ThisBuild / developers := List(
  Developer(
    "Georgi Krastev",
    "@joroKr21",
    "joro.kr.21@gmail.com",
    url("https://www.linkedin.com/in/georgykr")
  ),
  Developer(
    "Kailuo Wang",
    "@kailuowang",
    "kailuo.wang@gmail.com",
    url("http://kailuowang.com")
  ),
  Developer(
    "Luka Jacobowitz",
    "@LukaJCB",
    "luka.jacobowitz@fh-duesseldorf.de",
    url("http://stackoverflow.com/users/3795501/luka-jacobowitz")
  )
)
