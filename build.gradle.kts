plugins { java }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

group = "com.inpuzah"
version = "0.1.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/") // Geyser/Floodgate (optional)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    // JDA used for compile only; Paper will download at runtime via plugin.yml libraries
    compileOnly("net.dv8tion:JDA:5.0.0-beta.24") { exclude(module = "opus-java") }

    // Log4j is provided by Paper at runtime; we only need it to compile the appender
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.1")
    compileOnly("org.apache.logging.log4j:log4j-core:2.24.1")
}

tasks.processResources {
    filesMatching("plugin.yml") { expand("version" to project.version) }
}
