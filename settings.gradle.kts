pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/")
		maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.7-beta.7"
}

stonecutter {
	create(rootProject) {
		versions("1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.6")
		vcsVersion = "1.21.6"
	}
}

rootProject.name = "Template"