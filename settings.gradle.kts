import org.gradle.api.JavaVersion
import org.gradle.api.initialization.resolve.RepositoriesMode

if (JavaVersion.current() < JavaVersion.VERSION_11) {
    throw GradleException(
        "ClickFlow Pro requires JDK 11 or newer. AndroidIDE is currently using Java ${JavaVersion.current()}. " +
            "Select or install JDK 11+ in AndroidIDE Gradle/JDK settings, then sync again.",
    )
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ClickFlowPro"
include(":app")