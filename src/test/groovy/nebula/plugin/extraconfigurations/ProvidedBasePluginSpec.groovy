package nebula.plugin.extraconfigurations

import nebula.test.ProjectSpec
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

class ProvidedBasePluginSpec extends ProjectSpec {
    @Override
    String getPluginName() {
        'nebula-provided-base'
    }

    def "Does not create provided configuration if Java plugin is not applied"() {
        when:
        project.apply plugin: pluginName

        then:
        !project.configurations.findByName(ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME)
    }

    def 'Creates provided configuration if Java plugin is applied'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: pluginName

        then: 'Compile configuration extends from provided configuration'
        Configuration compileConfiguration = project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)
        compileConfiguration.extendsFrom.collect { it.name } as Set<String> == [ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME] as Set<String>
        !compileConfiguration.visible
        compileConfiguration.transitive

        and: 'Provided configuration exists and does not extend other configurations'
        Configuration providedConfiguration = project.configurations.getByName(ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME)
        providedConfiguration.extendsFrom == Collections.emptySet()
        providedConfiguration.visible
        providedConfiguration.transitive
    }

    def 'order independent does provided conf exist'() {
        when:
        project.apply plugin: pluginName
        project.apply plugin: 'java'

        then:
        project.configurations.getByName(ProvidedBasePlugin.PROVIDED_CONFIGURATION_NAME)
    }

    def 'check versions'() {
        when:
        project.apply plugin: 'java'
        project.apply plugin: pluginName

        project.repositories {
            mavenCentral()
        }

        project.dependencies {
            compile 'com.google.guava:guava:12.0'
            provided 'commons-io:commons-io:2.4'
        }

        then:
        ResolvedConfiguration resolved = project.configurations.compile.resolvedConfiguration
        resolved.getResolvedArtifacts().any { it.name == 'guava' }
        resolved.getResolvedArtifacts().any { it.name == 'commons-io' }

        ResolvedConfiguration resolvedProvided = project.configurations.provided.resolvedConfiguration
        !resolvedProvided.getResolvedArtifacts().any { it.name == 'guava' }
        resolvedProvided.getResolvedArtifacts().any { it.name == 'commons-io' }

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention)
        SourceSet mainSourceSet = javaConvention.sourceSets.main
        mainSourceSet.compileClasspath.any { it.name.contains 'guava'}
        mainSourceSet.compileClasspath.any { it.name.contains 'commons-io'}

        SourceSet testSourceSet = javaConvention.sourceSets.test
        testSourceSet.compileClasspath.any { it.name.contains 'guava'}
        testSourceSet.compileClasspath.any { it.name.contains 'commons-io'}
    }
}
