addCommandAlias("validateJVM", "all scalafmtCheckAll scalafmtSbtCheck testsJVM/test")
addCommandAlias("validateJS", "all testsJS/test")
addCommandAlias("validateNative", "all testsNative/test")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")

val Scala212 = "2.12.19"
val Scala213 = "2.13.13"
val Scala3 = "3.3.3"

val gitRepo = "git@github.com:typelevel/cats-tagless.git"
val homePage = "https://typelevel.org/cats-tagless"

// GitHub actions configuration
ThisBuild / organizationName := "cats-tagless maintainers"
// Note: we use early SemVer so the base version should be bumped only on binary-breaking changes.
// Additions to the API are allowed in a patch version while the major version remains zero.
ThisBuild / tlBaseVersion := "0.16"
ThisBuild / crossScalaVersions := Seq(Scala212, Scala213, Scala3)
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
        params = Map("ruby-version" -> "3.3", "bundler-cache" -> "true")
      ),
      WorkflowStep.Run(List("gem install jekyll -v 3.9.4"), name = Some("Install Jekyll")),
      WorkflowStep.Run(List("gem install kramdown-parser-gfm -v 1.1.0"), name = Some("Install Kramdown")),
      WorkflowStep.Sbt(List("docs/makeMicrosite"), name = Some("Build microsite"))
    ),
    scalas = List("2.13"),
    javas = List(JavaSpec.temurin("11"))
  )
)

val catsVersion = "2.10.0"
val circeVersion = "0.14.7"
val disciplineVersion = "1.6.0"
val disciplineMunitVersion = "2.0.0-M3"
val fs2Version = "3.10.2"
val kindProjectorVersion = "0.13.3"
val paradiseVersion = "2.1.1"
val scalaCheckVersion = "1.17.1"

lazy val root = tlCrossRootProject.aggregate(core, fs2, laws, tests, macros, examples)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native
lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .enablePlugins(AutomateHeaderPlugin)
  .jsSettings(commonJsSettings)
  .nativeSettings(commonNativeSettings)
  .settings(rootSettings, mimaSettings)
  .settings(
    moduleName := "cats-tagless-core",
    libraryDependencies += "org.typelevel" %%% "cats-core" % catsVersion
  )

lazy val fs2JVM = fs2.jvm
lazy val fs2JS = fs2.js
lazy val fs2Native = fs2.native
lazy val fs2 = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .dependsOn(core)
  .enablePlugins(AutomateHeaderPlugin)
  .jsSettings(commonJsSettings)
  .nativeSettings(commonNativeSettings)
  .settings(rootSettings)
  .settings(
    moduleName := "cats-tagless-fs2",
    libraryDependencies += "co.fs2" %%% "fs2-core" % fs2Version,
    tlVersionIntroduced := Map("2.12" -> "0.15.1", "2.13" -> "0.15.1", "3" -> "0.15.1")
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
  .settings(rootSettings, mimaSettings)
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
    scalacOptions ~= (_.filterNot(opt => opt.startsWith("-Wunused") || opt.startsWith("-Ywarn-unused"))),
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test,
    publish / skip := scalaBinaryVersion.value.startsWith("3"),
    tlMimaPreviousVersions := when(scalaBinaryVersion.value.startsWith("2"))(
      "0.16.0",
      "0.15.0",
      "0.14.0",
      "0.13.0"
    ).toSet
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
    scalacOptions ++= when(scalaBinaryVersion.value == "3")("-Xcheck-macros"),
    libraryDependencies ++= List(
      "org.typelevel" %%% "cats-free" % catsVersion,
      "org.typelevel" %%% "cats-testkit" % catsVersion,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion,
      "io.circe" %%% "circe-core" % circeVersion
    ).map(_ % Test)
  )

lazy val examples = project
  .dependsOn(macros.jvm, laws.jvm)
  .enablePlugins(AutomateHeaderPlugin, NoPublishPlugin)
  .settings(rootSettings, macroSettings)
  .settings(
    moduleName := "cats-tagless-examples",
    libraryDependencies += "org.typelevel" %%% "cats-free" % catsVersion
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
lazy val rootSettings = commonSettings
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
  autoAPIMappings := true,
  // sbt-typelevel sets -source:3.0-migration, we'd like to replace it with -source:future
  scalacOptions ~= (_.filterNot(_ == "-source:3.0-migration")),
  scalacOptions ++= (scalaBinaryVersion.value match {
    case "3" => List("-language:adhocExtensions", "-explain")
    case "2.13" => List("-Xsource:3", "-P:kind-projector:underscore-placeholders", "-Xlint:-pattern-shadow")
    case _ => List("-Xsource:3", "-P:kind-projector:underscore-placeholders")
  })
)

lazy val commonJsSettings = List(
  // currently sbt-doctest doesn't work in JS builds
  // https://github.com/tkawachi/sbt-doctest/issues/52
  doctestGenTests := Nil
)

lazy val commonNativeSettings = List(
  doctestGenTests := Nil
)

lazy val macroSettings = List[Def.Setting[?]](
  scalacOptions ++= when(scalaBinaryVersion.value == "2.13")("-Ymacro-annotations"),
  libraryDependencies ++= when(scalaBinaryVersion.value.startsWith("2"))("scala-compiler", "scala-reflect")
    .map("org.scala-lang" % _ % scalaVersion.value % Provided),
  libraryDependencies ++= when(scalaBinaryVersion.value == "2.12")(
    compilerPlugin(("org.scalamacros" %% "paradise" % paradiseVersion).cross(CrossVersion.full))
  )
)

lazy val mimaSettings = List[Def.Setting[?]](
  tlVersionIntroduced := Map("3" -> "0.15.0"),
  tlMimaPreviousVersions += "0.15.0",
  tlMimaPreviousVersions ++= when(scalaBinaryVersion.value.startsWith("2"))("0.14.0", "0.13.0").toSet
)

def when[A](condition: Boolean)(values: A*): Seq[A] =
  if (condition) values else Nil

ThisBuild / homepage := Some(url(homePage))
ThisBuild / startYear := Some(2017)
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
