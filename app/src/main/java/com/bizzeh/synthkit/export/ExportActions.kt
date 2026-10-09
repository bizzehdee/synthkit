package com.bizzeh.synthkit.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.bizzeh.synthkit.audio.PcmSource
import com.bizzeh.synthkit.project.Project

/** What the export screen can ask for; see ExportViewModel. */
class ExportActions(
    val export: (Project, ExportFormat, Int, (ExportSpec) -> PcmSource?, (Int, Int) -> String) -> Unit,
    val cancel: () -> Unit,
    val reset: () -> Unit,
    val saveTo: (Uri, (Boolean) -> Unit) -> Unit,
)

/** The system "save as" picker for a [mime type, file name] pair. No storage permission is needed. */
class CreateExportDocument : ActivityResultContract<Pair<String, String>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, String>): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.first)
            .putExtra(Intent.EXTRA_TITLE, input.second)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}
