plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.20.6" /* [SC] DO NOT EDIT */

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "build"
    ofTask("build")
}

stonecutter registerChiseled tasks.register("chiseledModrinth", stonecutter.chiseled) {
    versions = stonecutter.versions.reversed()
    group = "publishing"
    ofTask("modrinth")
}
