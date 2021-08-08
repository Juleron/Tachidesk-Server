package suwayomi.tachidesk.server.util

/*
 * Copyright (C) Contributors to the Suwayomi project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

import mu.KotlinLogging
import net.lingala.zip4j.ZipFile
import org.kodein.di.DI
import org.kodein.di.conf.global
import org.kodein.di.instance
import suwayomi.tachidesk.server.ApplicationDirs
import suwayomi.tachidesk.server.BuildConfig
import java.io.BufferedInputStream
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

private val logger = KotlinLogging.logger {}
private val applicationDirs by DI.global.instance<ApplicationDirs>()
private val tmpDir = System.getProperty("java.io.tmpdir")

private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

private fun directoryMD5(fileDir: String): String {
    var sum = ""
    File(fileDir).walk().toList().sortedBy { it.path }.forEach { file ->
        if (file.isFile) {
            val md5 = MessageDigest.getInstance("MD5")
            md5.update(file.readBytes())
            val digest = md5.digest()
            sum += digest.toHex()
        }
    }

    val md5 = MessageDigest.getInstance("MD5")
    md5.update(sum.toByteArray(StandardCharsets.UTF_8))
    val digest = md5.digest()
    return digest.toHex()
}

fun setupWebUI() {
    // check if we have webUI installed and is correct version
    val webUIRevisionFile = File(applicationDirs.webUIRoot + "/revision")
    if (webUIRevisionFile.exists() && webUIRevisionFile.readText().trim() == BuildConfig.WEBUI_TAG) {
        logger.info { "WebUI Static files exists and is the correct revision" }
        logger.info { "Verifying WebUI Static files..." }
        logger.info { "md5: " + directoryMD5(applicationDirs.webUIRoot) }
    } else {
        File(applicationDirs.webUIRoot).deleteRecursively()

        // download webUI zip
        val webUIZip = "Tachidesk-WebUI-${BuildConfig.WEBUI_TAG}.zip"
        val webUIZipPath = "$tmpDir/$webUIZip"
        val webUIZipURL = "${BuildConfig.WEBUI_REPO}/releases/download/${BuildConfig.WEBUI_TAG}/$webUIZip"
        val webUIZipFile = File(webUIZipPath)
        webUIZipFile.delete()

        logger.info { "Downloading WebUI zip from the Internet..." }
        val data = ByteArray(1024)

        webUIZipFile.outputStream().use { webUIZipFileOut ->
            BufferedInputStream(URL(webUIZipURL).openStream()).use { inp ->
                var totalCount = 0
                var tresh = 0
                while (true) {
                    val count = inp.read(data, 0, 1024)
                    totalCount += count
                    if (totalCount > tresh + 10 * 1024) {
                        tresh = totalCount
                        print(" *")
                    }
                    if (count == -1)
                        break
                    webUIZipFileOut.write(data, 0, count)
                }
                println()
                logger.info { "Downloading WebUI Done." }
            }
        }

        // extract webUI zip
        logger.info { "Extracting downloaded WebUI zip..." }
        File(applicationDirs.webUIRoot).mkdirs()
        ZipFile(webUIZipPath).extractAll(applicationDirs.webUIRoot)
        logger.info { "Extracting downloaded WebUI zip Done." }
    }
}
