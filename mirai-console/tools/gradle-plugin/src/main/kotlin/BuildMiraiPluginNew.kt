/*
 * Copyright 2019-2022 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/dev/LICENSE
 */

package net.mamoe.mirai.console.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import javax.inject.Inject

public open class BuildMiraiPluginNew : Jar() {
    // @get:Internal
    private lateinit var metadataTask: GenMetadataTask

    public open class GenMetadataTask
    @Inject internal constructor(
        @JvmField internal val orgTask: BuildMiraiPluginNew,
    ) : DefaultTask() {
        @TaskAction
        internal fun run() {
            val runtime = mutableSetOf<String>()
            val api = mutableSetOf<String>()
            project.configurations["runtimeClasspath"].resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                if (artifact.id.componentIdentifier is ModuleComponentIdentifier) {
                    runtime.add(artifact.id.componentIdentifier.displayName)
                } else {
                    val file = artifact.file
                    if (file.isDirectory) {
                        orgTask.from(file)
                    } else if (file.extension == "jar") {
                        orgTask.from(project.zipTree(file))
                    } else {
                        orgTask.from(file)
                    }
                }
            }
            project.configurations.findByName("apiElements")?.allDependencies?.forEach { dep ->
                if (dep is ExternalModuleDependency) {
                    api.add("${dep.group}:${dep.name}:${dep.version}")
                }
            }
            temporaryDir.also {
                it.mkdirs()
            }.let { tmpDir ->
                tmpDir.resolve("api.txt").writeText(api.sorted().joinToString("\n"))
                tmpDir.resolve("runtime.txt").writeText(runtime.sorted().joinToString("\n"))
                orgTask.from(tmpDir.resolve("api.txt")) { copy ->
                    copy.into("META-INF/mirai-console-plugin")
                    copy.rename { "dependencies-shared.txt" }
                }
                orgTask.from(tmpDir.resolve("runtime.txt")) { copy ->
                    copy.into("META-INF/mirai-console-plugin")
                    copy.rename { "dependencies-private.txt" }
                }
            }
        }
    }

    @Suppress("RedundantLambdaArrow", "RemoveExplicitTypeArguments")
    internal fun init() {
        metadataTask = project.tasks.create<GenMetadataTask>(name + "GenMetadata", this)
        dependsOn(metadataTask)
        archiveExtension.set("mirai.jar")
    }
}