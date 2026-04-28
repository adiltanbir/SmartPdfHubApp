// app/src/main/java/com/smartpdfhub/MainActivity.kt
package com.smartpdfhub

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.snackbar.Snackbar
import com.smartpdfhub.data.model.PDFFile
import com.smartpdfhub.data.model.SortOption
import com.smartpdfhub.data.model.SourceType
import com.smartpdfhub.ui.adapter.PDFAdapter
import com.smartpdfhub.ui.viewmodel.PDFViewModel
import com.smartpdfhub.utils.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: PDFViewModel by viewModels()
    private lateinit var pdfAdapter: PDFAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var chipGroup: ChipGroup
    private lateinit var fabSort: ExtendedFloatingActionButton
    private lateinit var emptyView: View

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            loadPDFs()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupObservers()
        checkPermissions()
    }

    private fun setupViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Smart PDF Hub"

        recyclerView = findViewById(R.id.recyclerView)
        searchBar = findViewById(R.id.searchBar)
        searchView = findViewById(R.id.searchView)
        progressBar = findViewById(R.id.progressBar)
        chipGroup = findViewById(R.id.chipGroup)
        fabSort = findViewById(R.id.fabSort)
        emptyView = findViewById(R.id.emptyView)

        fabSort.setOnClickListener { showSortDialog() }

        // Setup dark mode toggle in toolbar menu
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_dark_mode -> toggleDarkMode()
                R.id.action_refresh -> viewModel.refresh()
            }
            true
        }
    }

    private fun setupRecyclerView() {
        pdfAdapter = PDFAdapter(
            onItemClick = { pdf -> openPDF(pdf) },
            onFavoriteClick = { pdf -> viewModel.toggleFavorite(pdf) },
            onShareClick = { pdf -> sharePDF(pdf) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = pdfAdapter
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
        }
    }

    private fun setupSearch() {
        searchView.editText.setOnEditorActionListener { _, _, _ ->
            searchBar.setText(searchView.text)
            searchView.hide()
            false
        }

        searchView.addTransitionListener { _, _, newState ->
            if (newState == com.google.android.material.search.SearchView.TransitionState.HIDDEN) {
                viewModel.setSearchQuery("")
            }
        }

        // Real-time search
        searchView.editText.doOnTextChanged { text, _, _, _ ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupFilters() {
        val filters = listOf(
            "All" to null,
            "WhatsApp" to SourceType.WHATSAPP,
            "Telegram" to SourceType.TELEGRAM,
            "Downloads" to SourceType.DOWNLOADS,
            "Documents" to SourceType.DOCUMENTS
        )

        filters.forEach { (label, type) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = type == null
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        viewModel.setFilter(type)
                        // Uncheck others
                        for (i in 0 until chipGroup.childCount) {
                            if (chipGroup.getChildAt(i) != this) {
                                (chipGroup.getChildAt(i) as Chip).isChecked = false
                            }
                        }
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pdfList.collect { pdfs ->
                    pdfAdapter.submitList(pdfs)
                    emptyView.isVisible = pdfs.isEmpty()
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.isVisible = isLoading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(recyclerView, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun checkPermissions() {
        if (PermissionManager.hasStoragePermission(this)) {
            loadPDFs()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - Request MANAGE_EXTERNAL_STORAGE
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, PermissionManager.REQUEST_CODE_MANAGE_STORAGE)
            } else {
                storagePermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun loadPDFs() {
        viewModel.refresh()
    }

    private fun openPDF(pdf: PDFFile) {
        viewModel.markAsOpened(pdf)

        try {
            val file = File(pdf.path)
            val uri = if (file.exists()) {
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
            } else {
                pdf.uri
            }

            // Try Google Drive PDF Viewer first
            val driveIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                setPackage("com.google.android.apps.docs")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Fallback to generic PDF viewer with system chooser
            val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            try {
                startActivity(driveIntent)
            } catch (e: ActivityNotFoundException) {
                // Google Drive not installed, show chooser
                val chooser = Intent.createChooser(genericIntent, "Open PDF with")
                startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePDF(pdf: PDFFile) {
        try {
            val file = File(pdf.path)
            val uri = if (file.exists()) {
                FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            } else {
                pdf.uri
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot share PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "Most Recent",
            "Name (A-Z)",
            "Name (Z-A)",
            "Size (Small to Large)",
            "Size (Large to Small)",
            "Date (Oldest First)",
            "Date (Newest First)"
        )

        val sortValues = arrayOf(
            SortOption.RECENT,
            SortOption.NAME_ASC,
            SortOption.NAME_DESC,
            SortOption.SIZE_ASC,
            SortOption.SIZE_DESC,
            SortOption.DATE_ASC,
            SortOption.DATE_DESC
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Sort By")
            .setItems(sortOptions) { _, which ->
                viewModel.setSortOption(sortValues[which])
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Storage Permission Required")
            .setMessage("Smart PDF Hub needs access to your storage to find and manage PDF files.")
            .setPositiveButton("Grant Permission") { _, _ -> checkPermissions() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun toggleDarkMode() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(newMode)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionManager.REQUEST_CODE_MANAGE_STORAGE) {
            if (PermissionManager.hasStoragePermission(this)) {
                loadPDFs()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }
}
