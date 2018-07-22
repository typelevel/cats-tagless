import com.typesafe.sbt.SbtGit.git
import org.typelevel.Dependencies._

addCommandAlias("gitSnapshots", ";set version in ThisBuild := git.gitDescribedVersion.value.get + \"-SNAPSHOT\"")

addCommandAlias("validateJVM", ";testsJVM/test ; docs/makeMicrosite")


val apache2 = "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")
val gh = GitHubSettings(org = "typelevel", proj = "cats-tagless", publishOrg = "org.typelevel", license = apache2)

val vAll = Versions(versions, libraries, scalacPlugins)

lazy val rootSettings = buildSettings ++ commonSettings ++ publishSettings ++ scoverageSettings
lazy val module = mkModuleFactory(gh.proj, mkConfig(rootSettings, commonJvmSettings, commonJsSettings))
lazy val prj = mkPrjFactory(rootSettings)

lazy val rootPrj = project
  .configure(mkRootConfig(rootSettings,rootJVM))
  .aggregate(rootJVM, rootJS, testsJS, macrosJS)
  .dependsOn(rootJVM, rootJS, testsJS, macrosJS)
  .settings(noPublishSettings)


lazy val rootJVM = project
  .configure(mkRootJvmConfig(gh.proj, rootSettings, commonJvmSettings))
  .aggregate(coreJVM, optimizeJVM, lawsJVM, testsJVM, macrosJVM, docs)
  .dependsOn(coreJVM, optimizeJVM, lawsJVM, testsJVM, macrosJVM)
  .settings(noPublishSettings)


lazy val rootJS = project
  .configure(mkRootJsConfig(gh.proj, rootSettings, commonJsSettings))
  .aggregate(coreJS, optimizeJS, lawsJS)
  .settings(noPublishSettings)


lazy val core    = prj(coreM)
lazy val coreJVM = coreM.jvm
lazy val coreJS  = coreM.js
lazy val coreM   = module("core", CrossType.Pure)
  .settings(addLibs(vAll, "cats-core"))
  .settings(simulacrumSettings(vAll))
  .enablePlugins(AutomateHeaderPlugin)


lazy val optimize    = prj(optimizeM)
lazy val optimizeJVM = optimizeM.jvm
lazy val optimizeJS  = optimizeM.js
lazy val optimizeM   = module("optimize", CrossType.Pure)
  .dependsOn(coreM)
  .enablePlugins(AutomateHeaderPlugin)


lazy val laws    = prj(lawsM)
lazy val lawsJVM = lawsM.jvm
lazy val lawsJS  = lawsM.js
lazy val lawsM   = module("laws", CrossType.Pure)
  .dependsOn(coreM)
  .settings(addLibs(vAll, "cats-laws"))
  .settings(disciplineDependencies)
  .enablePlugins(AutomateHeaderPlugin)


lazy val macros    = prj(macrosM)
lazy val macrosJVM = macrosM.jvm
lazy val macrosJS  = macrosM.js
lazy val macrosM   = module("macros", CrossType.Pure)
  .dependsOn(coreM)
  .settings(metaMacroSettings)
  .settings(copyrightHeader)
  .enablePlugins(AutomateHeaderPlugin)


lazy val tests    = prj(testsM)
lazy val testsJVM = testsM.jvm
lazy val testsJS  = testsM.js
lazy val testsM   = module("tests", CrossType.Pure)
  .settings(addLibs(vAll, "shapeless"))
  .dependsOn(coreM, lawsM, macrosM, optimizeM)
  .settings(disciplineDependencies)
  .settings(metaMacroSettings)
  .settings(noPublishSettings)
  .settings(addTestLibs(vAll, "scalatest", "cats-free", "cats-effect"))
  .enablePlugins(AutomateHeaderPlugin)


/** Docs - Generates and publishes the scaladoc API documents and the project web site.*/
lazy val docs = project
  .settings(rootSettings)
  .settings(moduleName := gh.proj + "-docs")
  .settings(noPublishSettings)
  .settings(unidocCommonSettings)
  .settings(simulacrumSettings(vAll))
  .settings(commonJvmSettings)
  .settings(metaMacroSettings)
  .settings(addLibs(vAll, "cats-free"))
  .dependsOn(List(coreJVM, macrosJVM).map( ClasspathDependency(_, Some("compile;test->test"))):_*)
  .enablePlugins(MicrositesPlugin)
  .settings(
    organization  := gh.organisation,
    autoAPIMappings := true,
    micrositeName := "Cats-tagless",
    micrositeDescription := "A library of utilities for tagless final algebras",
    micrositeBaseUrl := "cats-tagless",
    micrositeGithubOwner := "typelevel",
    micrositeGithubRepo := "cats-tagless",
    micrositeHighlightTheme := "atom-one-light",
    fork in tut := true,
    micrositePalette := Map(
      "brand-primary"     -> "#51839A",
      "brand-secondary"   -> "#EDAF79",
      "brand-tertiary"    -> "#96A694",
      "gray-dark"         -> "#192946",
      "gray"              -> "#424F67",
      "gray-light"        -> "#E3E2E3",
      "gray-lighter"      -> "#F4F3F4",
      "white-color"       -> "#FFFFFF"),
    ghpagesNoJekyll := false,
    micrositeAuthor := "cats-tagless Contributors",
    scalacOptions in Tut ~= (_.filterNot(Set("-Ywarn-unused-import", "-Ywarn-dead-code"))),
    git.remoteRepo := gh.repo,
    includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md")



lazy val buildSettings = sharedBuildSettings(gh, vAll)

lazy val commonSettings = sharedCommonSettings ++ Seq(
  parallelExecution in Test := false,
  scalaVersion := vAll.vers("scalac_2.12"),
  crossScalaVersions := Seq(vAll.vers("scalac_2.11"), scalaVersion.value),
  developers := List(Developer("Kailuo Wang", "@kailuowang", "kailuo.wang@gmail.com", new java.net.URL("http://kailuowang.com")))
) ++ scalacAllSettings ++ unidocCommonSettings ++
  addCompilerPlugins(vAll, "kind-projector") ++ copyrightHeader

lazy val commonJsSettings = Seq(scalaJSStage in Global := FastOptStage)

lazy val commonJvmSettings = Seq()

lazy val publishSettings = sharedPublishSettings(gh) ++ credentialSettings ++ sharedReleaseProcess

lazy val scoverageSettings = sharedScoverageSettings(60)

lazy val disciplineDependencies = addLibs(vAll, "discipline", "scalacheck")

lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += Resolver.bintrayRepo("scalameta", "maven"),
  libraryDependencies += "org.scalameta" %% "scalameta" % "1.8.0",
  scalacOptions in (Compile, console) := Seq(), // macroparadise plugin doesn't work in repl yet.
  addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M11" cross CrossVersion.full),
  scalacOptions += "-Xplugin-require:macroparadise",
  sources in (Compile, doc) := Nil // macroparadise doesn't work with scaladoc yet.
)

lazy val copyrightHeader = Seq(
  startYear := Some(2017),
  organizationName := "Kailuo Wang"

)
