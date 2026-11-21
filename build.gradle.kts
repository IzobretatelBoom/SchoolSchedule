plugins {
    java
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    // Проект собран под JDK 17 (см. README); на более новых JDK тоже скомпилируется.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Единственная внешняя зависимость — драйвер SQLite из папки lib/.
    implementation(files("lib/sqlite-jdbc-3.53.4.0.jar"))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Исходники лежат в src/ (а не в стандартном src/main/java), как задумано в проекте.
// resources указывает сюда же — так файлы вроде db/schema.sql попадают в classpath.
sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("src"))
        resources.include("**/*.sql", "**/*.properties", "**/*.json", "**/*.xml")
    }
    test {
        java.setSrcDirs(listOf("src/test/java"))
        resources.setSrcDirs(listOf("src/test/resources"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

application {
    mainClass.set("Main")
    // JVM печатает кириллицу в UTF-8. Консоль Windows по умолчанию работает в CP866,
    // поэтому её нужно переключить на UTF-8: в терминале выполнить chcp 65001 один раз
    // (или запускать из Windows Terminal). Подробнее — раздел «Кириллица в консоли» в README.
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform()
}

// Консольный интерфейс читает ввод с клавиатуры и работает с school.db
// в корне проекта — поэтому запуск идёт из корня, а stdin отдаётся напрямую.
tasks.named<JavaExec>("run") {
    workingDir = projectDir
    standardInput = System.`in`
}

// Исполняемый jar со всеми зависимостями: build/libs/SchoolSchedule-1.0-SNAPSHOT-all.jar
tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Собирает исполняемый jar со всеми зависимостями."
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to "Main",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
}

tasks.named("build") {
    dependsOn("fatJar")
}
