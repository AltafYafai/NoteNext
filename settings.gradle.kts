
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
        maven { url = uri("https://repo1.maven.org/maven2") }
        maven { url = uri("https://jitpack.io") }
    }
}

// Metadata.kt source of truth parser
fun getMetadataValue(key: String): String {
    val metadataFile = settingsDir.resolve("metadata.kt")
    if (!metadataFile.exists()) return ""
    val regex = "const val $key = \"([^\"]*)\"".toRegex()
    val regexInt = "const val $key = ([0-9]*)".toRegex()
    val content = metadataFile.readText()
    
    return regex.find(content)?.groupValues?.get(1) 
        ?: regexInt.find(content)?.groupValues?.get(1) 
        ?: ""
}

gradle.beforeProject {
    extra["APP_NAME"] = getMetadataValue("APP_NAME")
    extra["APPLICATION_ID"] = getMetadataValue("APPLICATION_ID")
    extra["VERSION_CODE"] = getMetadataValue("VERSION_CODE").toIntOrNull() ?: 28
    extra["VERSION_NAME"] = getMetadataValue("VERSION_NAME")
    extra["MIN_SDK"] = getMetadataValue("MIN_SDK").toIntOrNull() ?: 24
    extra["TARGET_SDK"] = getMetadataValue("TARGET_SDK").toIntOrNull() ?: 36
    extra["COMPILE_SDK"] = getMetadataValue("COMPILE_SDK").toIntOrNull() ?: 36
    extra["NAMESPACE"] = getMetadataValue("NAMESPACE")
}

rootProject.name = getMetadataValue("APP_NAME")
include(":app")
include(":core")
include(":data")
include(":credits")
include(":changelog")
include(":feature:todo")
