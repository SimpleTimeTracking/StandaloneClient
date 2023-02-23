import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.sonarqube.gradle.SonarQubeTask
import org.javamodularity.moduleplugin.extensions.TestModuleOptions


plugins {
    val kotlinVersion = "1.7.10"
    application

    jacoco
    idea
    antlr
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("org.sonarqube") version "3.1"
    id("com.github.ben-manes.versions") version "0.36.0"

    id("org.openjfx.javafxplugin") version "0.0.13"

    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.beryx.jlink") version "2.25.0"
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/spekframework/spek/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://maven.atlassian.com/content/repositories/atlassian-public/")
}

// -SNAPSHOT is added if the release task is not set
version = "3"
val archivesBaseName = "STT"

application {
    mainModule.set("org.stt")
    mainClass.set("org.stt.StartWithJFX")
    // add-opens, so we can access the file decoration-warning.png within this module
    applicationDefaultJvmArgs =
        listOf("--add-opens=org.controlsfx.controls/impl.org.controlsfx.control.validation=org.stt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

kapt {
    correctErrorTypes = true
}

configurations {
    implementation {
        setExtendsFrom(extendsFrom.filter { it != configurations.antlr })
    }
}

val spek_version = "2.0.4"

dependencies {
    val daggerVersion = "2.43.1"
    antlr(group = "org.antlr", name = "antlr4", version = "4.9.1")
    implementation(group = "org.antlr", name = "antlr4-runtime", version = "4.9.1")

    implementation(group = "org.fxmisc.richtext", name = "richtextfx", version = "0.11.0") {
        exclude(group = "org.openjfx")
    }
    implementation("org.yaml:snakeyaml:1.27")
    implementation("com.google.dagger:dagger:$daggerVersion")
    implementation("javax.inject:javax.inject:1")
    kapt("com.google.dagger:dagger-compiler:$daggerVersion")
    implementation("net.engio:mbassador:1.3.2")
    implementation("org.controlsfx:controlsfx:11.1.2")
    //implementation("net.rcarz:jira-client:0.5")
    implementation("com.jsoniter:jsoniter:0.9.23")
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("commons-io:commons-io:2.8.0")
    testImplementation("org.mockito:mockito-core:3.12.4")
    testImplementation("org.assertj:assertj-core:3.18.1")
    testImplementation("junit:junit-dep:4.11")
}

javafx {
    version = "17.0.1"
    modules("javafx.base", "javafx.controls", "javafx.fxml", "javafx.graphics")
}

distributions.getByName("main") {
    contents {
        include("**/STT*")
    }
}

tasks.compileJava {
    // workaround, to make kapt created classes available to java module source set
    sourceSets {
        main {
            java {
                srcDir("$buildDir/generated/source/kapt/main")
            }
        }
    }
}

tasks.test {
    extensions.configure(TestModuleOptions::class) {
        // disable java-module path for tests
        runOnClasspath = true
    }
}

tasks.withType<KaptTask> {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<ProcessResources> {
    filesMatching("version.info") {
        filter<ReplaceTokens>(
            "tokens" to mapOf(
                "app.version" to project.property("version"),
                "app.hash" to getCheckedOutGitCommitHash()
            )
        )
    }
}

task("release") {
    dependsOn += "jlinkZip"
    doLast {
        println("Built release for $project.version")
    }
}

gradle.taskGraph.whenReady {
    if (!hasTask("release")) {
        version = (version as String) + "-SNAPSHOT"
    }
}

tasks.withType<AntlrTask> {
    maxHeapSize = "64m"
    arguments = arguments + "-visitor" + "-long-messages"
}


fun getCheckedOutGitCommitHash(): String {
    val takeFromHash = 12
    /*
     * '.git/HEAD' contains either
     *      in case of detached head: the currently checked out commit hash
     *      otherwise: a reference to a file containing the current commit hash
     */
    val head = file(".git/HEAD").readText().split(":") // .git/HEAD
    val isCommit = head.size == 1 // e5a7c79edabbf7dd39888442df081b1c9d8e88fd

    if (isCommit) return head[0].trim().take(takeFromHash) // e5a7c79edabb

    val refHead = file(".git/logs/" + head[1].trim()) // .git/refs/heads/master
    return refHead.readText().trim().take(takeFromHash)
}

tasks.withType<SonarQubeTask> {
    properties += "sonar.projectName" to "SimpleTimeTracking"
    properties += "sonar.projectKey" to "org.stt:stt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

//tasks.named("dependencyUpdates", com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class.java).configure {
//    val badVersions = ".*(-rc(-.*)?$|-m.*)".toRegex()
//    rejectVersionIf {
//        candidate.version.toLowerCase().matches(badVersions)
//    }
//}

jlink {
    imageZip.set(File("$buildDir/dist/stt-${javafx.platform.classifier}.zip"))
    addOptions("--bind-services", "--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    mergedModule {
        excludeRequires("javafx.graphics", "javafx.controls", "javafx.base")
    }
    forceMerge("kotlin") // see https://stackoverflow.com/questions/74453018/jlink-package-kotlin-in-both-merged-module-and-kotlin-stdlib
    launcher {
        name = "stt"
        jvmArgs =
            application.applicationDefaultJvmArgs.plus(
                listOf(
                    // some classes in the merged-modules (see jlink plugin( need access to javafx modules
                    "--add-reads=simpleTimeTracking.merged.module=javafx.graphics",
                    "--add-reads=simpleTimeTracking.merged.module=javafx.base",
                    "--add-reads=simpleTimeTracking.merged.module=javafx.controls"
                )
            )
    }
}