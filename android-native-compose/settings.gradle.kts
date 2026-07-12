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
    versionCatalogs {
        create("libs") {
            library("android-gradle-plugin", "com.android.tools.build:gradle:8.4.0")
            library("kotlin-gradle-plugin", "org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
            
            library("androidx-core-ktx", "androidx.core:core-ktx:1.13.1")
            library("androidx-lifecycle-runtime-ktx", "androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
            library("androidx-activity-compose", "androidx.activity:activity-compose:1.9.0")
            
            // Compose BOM
            library("androidx-compose-bom", "androidx.compose:compose-bom:2024.06.00")
            library("androidx-compose-ui", "androidx.compose.ui:ui")
            library("androidx-compose-ui-graphics", "androidx.compose.ui:ui-graphics")
            library("androidx-compose-ui-tooling-preview", "androidx.compose.ui:ui-tooling-preview")
            library("androidx-compose-material3", "androidx.compose.material3:material3")
            library("androidx-compose-material-icons-extended", "androidx.compose.material:material-icons-extended")
            
            // Preferences DataStore & GSON
            library("androidx-datastore-preferences", "androidx.datastore:datastore-preferences:1.1.1")
            library("gson", "com.google.code.gson:gson:2.10.1")
            
            // Navigation
            library("androidx-navigation-compose", "androidx.navigation:navigation-compose:2.7.7")

            plugin("android-application", "com.android.application").version("8.4.0")
            plugin("kotlin-android", "org.jetbrains.kotlin.android").version("1.9.23")
        }
    }
}

rootProject.name = "SmartStudentSchedule"
include(":app")
