import com.typesafe.sbt.SbtGit.git

addCommandAlias("validateJVM", "all scalafmtCheckAll scalafmtSbtCheck testsJVM/test")
addCommandAlias("validateJS", "all testsJS/test")
addCommandAlias("validateNative", "all testsNative/test")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")
addCommandAlias("gitSnapshots", ";set version in ThisBuild := git.gitDescribedVersion.value.get + \"-SNAPSHOT\"")

val Scala212 = "2.12.13"
val Scala213 = "2.13.5"

val gitRepo = "git@github.com:typelevel/cats-tagless.git"
val homePage = "https://typelevel.org/cats-tagless"

// sbt-spiewak settings
ThisBuild / baseVersion := "0.12"
ThisBuild / publishGithubUser := "joroKr21"
ThisBuild / publishFullName := "Georgi Krastev"
enablePlugins(SonatypeCiReleasePlugin)

// update to scala 3 requires swapping from scalatest to munit and reimplementing all macros
ThisBuild / crossScalaVersions := Seq(Scala212, Scala213 /*, "3.0.0-M1", "3.0.0-M2"*/ )
ThisBuild / scalaVersion := Scala213
ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowArtifactUpload := false
ThisBuild / githubWorkflowBuildMatrixAdditions += "ci" -> List("validateJVM", "validateJS", "validateNative")
ThisBuild / githubWorkflowBuild := List(WorkflowStep.Sbt(List("${{ matrix.ci }}"), name = Some("Validation")))
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
    scalas = List(Scala213)
  )
)

val catsVersion = "2.4.2"
val circeVersion = "0.13.0"
val disciplineVersion = "1.1.4"
val disciplineMunitVersion = "1.0.6"
val paradiseVersion = "2.1.1"
val scalaCheckVersion = "1.15.3"

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

lazy val catsTagless = project
  .aggregate(rootJVM, rootJS, rootNative, docs)
  .dependsOn(rootJVM, rootJS, rootNative)
  .settings(rootSettings, noPublishSettings)
  .settings(moduleName := "cats-tagless")

lazy val rootJVM = project
  .aggregate(coreJVM, lawsJVM, testsJVM, macrosJVM)
  .dependsOn(coreJVM, lawsJVM, testsJVM, macrosJVM)
  .settings(rootSettings, noPublishSettings)

lazy val rootJS = project
  .aggregate(coreJS, lawsJS, testsJS, macrosJS)
  .dependsOn(coreJS, lawsJS, testsJS, macrosJS)
  .settings(rootSettings, noPublishSettings, commonJsSettings)

lazy val rootNative = project
  .aggregate(coreNative, lawsNative, testsNative, macrosNative)
  .dependsOn(coreNative, lawsNative, testsNative, macrosNative)
  .settings(rootSettings, noPublishSettings, commonNativeSettings)

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
  .settings(rootSettings, macroSettings, copyrightHeader)
  .settings(
    moduleName := "cats-tagless-macros",
    scalacOptions := scalacOptions.value.filterNot(_.startsWith("-Wunused")).filterNot(_.startsWith("-Ywarn-unused")),
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test
  )

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js
lazy val testsNative = tests.native
lazy val tests = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .dependsOn(macros, laws)
  .enablePlugins(AutomateHeaderPlugin)
  .jvmSettings(libraryDependencies += "io.circe" %% "circe-core" % circeVersion % Test)
  .jsSettings(commonJsSettings)
  .jsSettings(scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)))
  .nativeSettings(commonNativeSettings)
  .settings(rootSettings, macroSettings, noPublishSettings)
  .settings(
    moduleName := "cats-tagless-tests",
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++= List(
      "org.typelevel" %%% "cats-free" % catsVersion,
      "org.typelevel" %%% "cats-testkit" % catsVersion,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion
    ).map(_ % Test)
  )

/** Docs - Generates and publishes the scaladoc API documents and the project web site. */
lazy val docs = project
  .dependsOn(macrosJVM)
  .enablePlugins(MicrositesPlugin, SiteScaladocPlugin)
  .settings(rootSettings, macroSettings, noPublishSettings)
  .settings(
    moduleName := "cats-tagless-docs",
    libraryDependencies += "org.typelevel" %%% "cats-free" % catsVersion,
    docsMappingsAPIDir := "api",
    addMappingsToSiteDir(mappings in packageDoc in Compile in coreJVM, docsMappingsAPIDir),
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
    includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md"
  )

lazy val docsMappingsAPIDir = settingKey[String]("Name of subdirectory in site target directory for api docs")
lazy val rootSettings = (organization := "org.typelevel") :: commonSettings ::: publishSettings

lazy val commonSettings = copyrightHeader ::: List(
  scalaVersion := (ThisBuild / scalaVersion).value,
  crossScalaVersions := (ThisBuild / crossScalaVersions).value,
  parallelExecution in Test := false,
  resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots"))
)

lazy val commonJsSettings = List(
  scalaJSStage in Global := FastOptStage,
  // currently sbt-doctest doesn't work in JS builds
  // https://github.com/tkawachi/sbt-doctest/issues/52
  doctestGenTests := Nil
)

lazy val commonNativeSettings = List(
  doctestGenTests := Nil
)

lazy val noPublishSettings = List(
  skip in publish := true
)

lazy val publishSettings = List(
  homepage := Some(url(homePage)),
  scmInfo := Some(ScmInfo(url(homePage), gitRepo)),
  licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  developers := List(
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
)

lazy val copyrightHeader = List(
  startYear := Some(2019),
  organizationName := "cats-tagless maintainers"
)
