plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21.6" /* [SC] DO NOT EDIT */

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "build"
    ofTask("build")
}

stonecutter registerChiseled tasks.register("chiseledClean", stonecutter.chiseled) {
    group = "build"
    ofTask("clean")
}

stonecutter registerChiseled tasks.register("chiseledPublishMods", stonecutter.chiseled) {
    group = "publishing"
    ofTask("publishMods")
    dependsOn(tasks.getByName("chiseledBuild"))
}
