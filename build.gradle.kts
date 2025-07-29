plugins {
	`maven-publish`
	id("fabric-loom")
	// id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${stonecutter.current.version}"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

repositories {
	/**
	 * Restricts dependency search of the given [groups] to the [maven URL][url],
	 * improving the setup speed.
	 */
	fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
		forRepository { maven(url) { name = alias } }
		filter { groups.forEach(::includeGroup) }
	}
	strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
	strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")

	mavenCentral()

	// YACL
	maven("https://maven.isxander.dev/releases") {
		name = "Xander Maven"
	}

	// Mod Menu
	maven("https://maven.terraformersmc.com/") {
		name = "Terraformers"
	}
}

dependencies {
	/**
	 * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
	 * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
	 */
	fun fapi(vararg modules: String) {
		for (it in modules) modImplementation(fabricApi.module(it, property("deps.fabric_api") as String))
	}

	minecraft("com.mojang:minecraft:${stonecutter.current.version}")
	mappings("net.fabricmc:yarn:${property("deps.yarn")}:v2")
	modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

	testImplementation("net.fabricmc:fabric-loader-junit:${property("deps.fabric_loader")}")

	modImplementation("dev.isxander:yet-another-config-lib:${property("deps.yacl")}")
	modImplementation("com.terraformersmc:modmenu:${property("deps.modmenu")}")

	implementation("org.yaml:snakeyaml:2.4")
	include("org.yaml:snakeyaml:2.4")

	fapi("fabric-lifecycle-events-v1",)
}

tasks.test {
	useJUnitPlatform()
}

loom {
	decompilerOptions.named("vineflower") {
		options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
	}

	runConfigs.all {
		ideConfigGenerated(true)
		vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
		runDir = "../../run" // Shares the run directory between versions
	}
}

java {
	withSourcesJar()
	val requiresJava21: Boolean = stonecutter.eval(stonecutter.current.version, ">=1.20.6")
	val javaVersion: JavaVersion =
		if (requiresJava21) JavaVersion.VERSION_21
		else JavaVersion.VERSION_17
	targetCompatibility = javaVersion
	sourceCompatibility = javaVersion
}

tasks {
	processResources {
		inputs.property("id", project.property("mod.id"))
		inputs.property("name", project.property("mod.name"))
		inputs.property("version", project.property("mod.version"))
		inputs.property("minecraft", project.property("mod.mc_dep"))
		inputs.property("fabric_api", project.property("deps.fabric_api"))
		inputs.property("yacl", project.property("deps.yacl"))
		inputs.property("modmenu", project.property("deps.modmenu"))

		val props = mapOf(
			"id" to project.property("mod.id"),
			"name" to project.property("mod.name"),
			"version" to project.property("mod.version"),
			"minecraft" to project.property("mod.mc_dep"),
			"fabric_api" to project.property("deps.fabric_api"),
			"yacl" to project.property("deps.yacl"),
			"modmenu" to project.property("deps.modmenu")
		)

		filesMatching("fabric.mod.json") { expand(props) }
	}

	// Builds the version into a shared folder in `build/libs/${mod version}/`
	register<Copy>("buildAndCollect") {
		group = "build"
		from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
		into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
		dependsOn("build")
	}
}

/*
publishMods {
    file = tasks.remapJar.get().archiveFile
    additionalFiles.from(tasks.remapSourcesJar.get().archiveFile)
    displayName = "${mod.name} ${mod.version} for $mcVersion"
    version = mod.version
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = providers.environmentVariable("MODRINTH_TOKEN")
        .getOrNull() == null || providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add(mcVersion)
        requires {
            slug = "fabric-api"
        }
    }

    curseforge {
        projectId = property("publish.curseforge").toString()
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.add(mcVersion)
        requires {
            slug = "fabric-api"
        }
    }
}
*/
/*
publishing {
    repositories {
        maven("...") {
            name = "..."
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${mod.id}"
            artifactId = mod.version
            version = mcVersion

            from(components["java"])
        }
    }
}
*/