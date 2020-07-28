/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.afollestad.assent.rationale.createDialogRationale
import com.alexvt.weathergraph.R
import com.alexvt.weathergraph.viewmodel.DataViewModel
import com.alexvt.weathergraph.viewmodel.EventObserver
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_data.*
import kotlinx.android.synthetic.main.view_data_source.view.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject


class DataActivity : BaseAppCompatActivity(R.layout.activity_data) {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val viewModel by lazy {
        ViewModelProvider(this, vmFactory)[DataViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        bindWeatherSource()
        bindAirQualitySource()
        bindKnownLocationsSource()
        bindNavigation()
    }

    private fun bindWeatherSource() = bindSource(
        kind = "Weather",
        view = vWeatherData,
        source = viewModel.getWeatherDataProvider()
    )

    private fun bindAirQualitySource() = bindSource(
        kind = "Air Quality",
        view = vAirQuality,
        source = viewModel.getAirQualityProvider()
    )

    private fun bindKnownLocationsSource() = bindSource(
        kind = "Location Names",
        view = vLocationNames,
        source = viewModel.getKnownLocationsProvider(),
        exportListener = { exportKnownLocations() }
    )

    private fun bindSource(
        kind: String, view: View, source: DataViewModel.DataSource,
        exportListener: (() -> Unit)? = null
    ) {
        view.mtvTitle.text = getString(R.string.data_source_title, kind, source.name)
        view.mbLink.text = source.shortLink
        view.mbLink.setOnClickListener { viewModel.clickLink(source.link) }
        view.mbExport.visibility = if (source.rawDataPath != null) View.VISIBLE else View.GONE
        view.mbExport.setOnClickListener { exportListener?.invoke() }
    }

    private val exportDbRequestCode = 0

    private fun getWriteStoragePermission(resultListener: (Boolean) -> Unit) {
        val permission = Permission.WRITE_EXTERNAL_STORAGE
        askForPermissions(
            permission,
            rationaleHandler = createDialogRationale(R.string.storage_write_permission) {
                onPermission(permission, R.string.export_needs_write)
            }) {
            resultListener(it.isAllGranted(permission))
        }
    }

    private fun exportKnownLocations() = getWriteStoragePermission { granted ->
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "KnownLocations.db")
        }.takeIf { granted }?.let { startActivityForResult(it, exportDbRequestCode) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK && requestCode == exportDbRequestCode) {
            val dbContent = readFileContent(viewModel.getKnownLocationsProvider().rawDataPath!!)
            writeFileContent(dbContent, resultData!!.data!!)
            Toast.makeText(this, "File is saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeFileContent(content: ByteArray, uri: Uri) {
        val pfd = this.contentResolver.openFileDescriptor(uri, "w")
        val fileOutputStream = FileOutputStream(pfd!!.fileDescriptor)
        fileOutputStream.write(content)
        fileOutputStream.close()
        pfd.close()
    }

    private fun readFileContent(path: String): ByteArray {
        val file = File(path)
        val bytes = ByteArray(file.length().toInt())
        val buf = BufferedInputStream(FileInputStream(file))
        buf.read(bytes, 0, bytes.size)
        buf.close()
        return bytes
    }

    private fun bindNavigation() {
        viewModel.backNavigationLiveData.observe(this, EventObserver { finish() })
        viewModel.linkNavigationLiveData.observe(this, EventObserver { openLink(it) })
        fabOk.setOnClickListener { viewModel.clickBack() }
    }

    override fun onSupportNavigateUp() = viewModel.clickBack().let { true }

    override fun onBackPressed() = viewModel.clickBack()
}
