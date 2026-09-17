package com.multiroom.ui.util

import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path

/** Native file chooser, backed by the system dialog.
 */
object FilePicker {

    /**
     * @param title dialog caption
     * @param extension filter such as "exe", or null for any file
     * @param startIn directory to open in
     * @return the chosen file, or null when dismissed
     */
    fun chooseFile(
        owner: Frame? = null,
        title: String,
        extension: String? = null,
        startIn: Path? = null,
    ): Path? {
        val dialog = FileDialog(owner, title, FileDialog.LOAD)
        if (extension != null) {
            dialog.file = "*.$extension"
            // The filter is advisory on Windows.
            dialog.setFilenameFilter { _, name -> name.endsWith(".$extension", true) }
        }
        startIn?.let { dialog.directory = it.toString() }

        dialog.isVisible = true

        val directory = dialog.directory ?: return null
        val file = dialog.file ?: return null
        val chosen = Path.of(directory, file)

        return if (extension == null || chosen.fileName.toString().endsWith(".$extension", true)) {
            chosen
        } else {
            null
        }
    }
}
