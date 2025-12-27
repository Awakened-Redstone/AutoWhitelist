@file:OptIn(StonecutterExperimentalFilesAPI::class)

import dev.kikugie.stonecutter.controller.file.Presets
import dev.kikugie.stonecutter.controller.file.StonecutterExperimentalFilesAPI

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11" /* [SC] DO NOT EDIT */

stonecutter handlers {
    configure("accesswidener") {
        scanner { from(Presets.Scanner.Hash) }
        comment(Presets.Commenter.Hash)
    }
}
