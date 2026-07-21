plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter handlers {
    // Copy the accesswidener configuration to classtweaker
    inherit("aw", "classtweaker")
}

stonecutter active "26.2" /* [SC] DO NOT EDIT */
