
import org.typelevel.Dependencies._

addCommandAlias("gitSnapshots", ";set version in ThisBuild := git.gitDescribedVersion.value.get + \"-SNAPSHOT\"")

/**
 * Project settings
 */
val gh = GitHubSettings(org = "com.kailuowang", proj = "mainecoon", publishOrg = "com.kailuowang", license = apache)
val devs = Seq(Dev("Kailuo Wang", "kauowang"))

val vAll = Versions(versions, libraries, scalacPlugins)

/**
 * Root - This is the root project that aggregates the catalystsJVM and catalystsJS sub projects
 */
lazy val rootSettings = buildSettings ++ commonSettings ++ publishSettings ++ scoverageSettings
lazy val module = mkModuleFactory(gh.proj, mkConfig(rootSettings, commonJvmSettings, commonJsSettings))
lazy val prj = mkPrjFactory(rootSettings)

lazy val rootPrj = project
  .configure(mkRootConfig(rootSettings,rootJVM))
  .aggregate(rootJVM, rootJS, testsJS, macrosJS)
  .dependsOn(rootJVM, rootJS, testsJS, macrosJS)

lazy val rootJVM = project
  .configure(mkRootJvmConfig(gh.proj, rootSettings, commonJvmSettings))
  .aggregate(coreJVM, lawsJVM, testsJVM, macrosJVM, docs)
  .dependsOn(coreJVM, lawsJVM, testsJVM, macrosJVM)

lazy val rootJS = project
  .configure(mkRootJsConfig(gh.proj, rootSettings, commonJsSettings))
  .aggregate(coreJS, lawsJS)


/** core - cross project that provides cross core support.*/
lazy val core    = prj(coreM)
lazy val coreJVM = coreM.jvm
lazy val coreJS  = coreM.js
lazy val coreM   = module("core", CrossType.Pure)
  .settings(addLibs(vAll, "cats-core"))
  .settings(addTestLibs(vAll, "scalatest"))

/** laws - cross project that provides cross laws support.*/
lazy val laws    = prj(lawsM)
lazy val lawsJVM = lawsM.jvm
lazy val lawsJS  = lawsM.js
lazy val lawsM   = module("laws", CrossType.Pure)
  .dependsOn(coreM)
  .settings(addLibs(vAll, "cats-laws"))
  .settings(disciplineDependencies)


/** macros - cross project that provides cross macros support.*/
lazy val macros    = prj(macrosM)
lazy val macrosJVM = macrosM.jvm
lazy val macrosJS  = macrosM.js
lazy val macrosM   = module("macros", CrossType.Pure)
  .dependsOn(coreM)
  .settings(metaMacroSettings,
    libraryDependencies += "org.scalameta" %% "scalameta" % "1.7.0"
  )


lazy val tests    = prj(testsM)
lazy val testsJVM = testsM.jvm
lazy val testsJS  = testsM.js
lazy val testsM   = module("tests", CrossType.Pure)
  .dependsOn(coreM, lawsM, macrosM)
  .settings(disciplineDependencies)
  .settings(metaMacroSettings)
  .settings(noPublishSettings)
  .settings(addTestLibs(vAll, "scalatest" ))


/** Docs - Generates and publishes the scaladoc API documents and the project web site.*/
lazy val docs = project.configure(mkDocConfig(gh, rootSettings, commonJvmSettings,
  coreJVM))

/** Settings.*/
lazy val buildSettings = sharedBuildSettings(gh, vAll)

lazy val commonSettings = sharedCommonSettings ++ Seq(
  addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M8" cross CrossVersion.full),
  scalacOptions ++= scalacAllOptions :+ "-Xplugin-require:macroparadise",
  parallelExecution in Test := false
) ++ warnUnusedImport ++ unidocCommonSettings ++
  addCompilerPlugins(vAll, "kind-projector")

lazy val commonJsSettings = Seq(scalaJSStage in Global := FastOptStage)

lazy val commonJvmSettings = Seq()

lazy val publishSettings = sharedPublishSettings(gh, devs) ++ credentialSettings ++ sharedReleaseProcess

lazy val scoverageSettings = sharedScoverageSettings(60)

lazy val disciplineDependencies = addLibs(vAll, "discipline", "scalacheck")

lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += Resolver.bintrayRepo("scalameta", "maven"),

  scalacOptions in (Compile, console) := Seq(), // macroparadise plugin doesn't work in repl yet.

  sources in (Compile, doc) := Nil // macroparadise doesn't work with scaladoc yet.
)
