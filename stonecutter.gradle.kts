plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21-pre2" /* [SC] DO NOT EDIT */

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "build"
    ofTask("build")
}

stonecutter registerChiseled tasks.register("chiseledModrinth", stonecutter.chiseled) {
    group = "publishing"
    ofTask("modrinth")
}
