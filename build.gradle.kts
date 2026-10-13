// Root project: only orchestrates the backend build.
// Scalingo's Gradle buildpack runs `./gradlew stage` when this task exists.
tasks.register("stage") {
    group = "build"
    description = "Builds the deployable jar (frontend must be built first, see package.json)."
    dependsOn(":backend:bootJar")
}
