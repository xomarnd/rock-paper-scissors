plugins {
}

allprojects {
    group = "com.github.xomarnd"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withType<JavaPlugin> {
        the<JavaPluginExtension>().toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    plugins.withType<JacocoPlugin> {
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}