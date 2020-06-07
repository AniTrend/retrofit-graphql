package co.anitrend.retrofit.graphql.buildSrc.plugin.components

import co.anitrend.retrofit.graphql.buildSrc.plugin.strategy.DependencyStrategy
import org.gradle.api.Project

internal fun Project.configureDependencies() {
    val dependencyStrategy = DependencyStrategy(project.name)
    dependencies.add("implementation",
        fileTree("libs") {
            include("*.jar")
        }
    )
    dependencyStrategy.applyDependenciesOn(dependencies)
}