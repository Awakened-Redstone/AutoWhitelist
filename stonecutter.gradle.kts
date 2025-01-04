plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21.4" /* [SC] DO NOT EDIT */

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "build"
    ofTask("build")
}

stonecutter registerChiseled tasks.register("chiseledPublishMods", stonecutter.chiseled) {
    group = "publishing"
    ofTask("publishMods")
}
