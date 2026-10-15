package com.chuckerteam.chucker.internal.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import com.chuckerteam.chucker.R
import com.chuckerteam.chucker.databinding.ChuckerActivityMainBinding
import com.chuckerteam.chucker.internal.data.model.DialogData
import com.chuckerteam.chucker.internal.support.Logger
import com.chuckerteam.chucker.internal.support.shareAsFile
import com.chuckerteam.chucker.internal.support.showDialog
import com.chuckerteam.chucker.internal.ui.ExportType.HAR
import com.chuckerteam.chucker.internal.ui.ExportType.TEXT
import com.chuckerteam.chucker.internal.ui.transaction.MainTransactionAdapter
import com.chuckerteam.chucker.internal.ui.transaction.TransactionActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MainActivity2 :
    BaseChuckerActivity(),
    SearchView.OnQueryTextListener {
    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var mainBinding: ChuckerActivityMainBinding
    private lateinit var transactionsAdapter: MainTransactionAdapter
    private lateinit var selectionBackCallback: OnBackPressedCallback

    private val applicationName: CharSequence
        get() = applicationInfo.loadLabel(packageManager)

    private val permissionRequest =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isPermissionGranted: Boolean ->
            if (!isPermissionGranted) {
                showToast(
                    applicationContext.getString(R.string.chucker_notifications_permission_not_granted),
                    Toast.LENGTH_LONG,
                )
                Logger.error("Notification permission denied. Can't show transactions info")
            }
        }

    private val saveTextToFile =
        registerForActivityResult(ActivityResultContracts.CreateDocument(TEXT.mimeType)) { uri ->
            viewModel.onSaveDocumentResult(uri, TEXT)
        }

    private val saveHarToFile =
        registerForActivityResult(ActivityResultContracts.CreateDocument(HAR.mimeType)) { uri ->
            viewModel.onSaveDocumentResult(uri, HAR)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupView()
        selectionBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                viewModel.clearSelection()
            }
        }
        onBackPressedDispatcher.addCallback(this, selectionBackCallback)
        observeState()
        observeEvents()

        val selectedIds =
            savedInstanceState?.getLongArray(KEY_SELECTED_TRANSACTION_IDS)?.toList().orEmpty()
        val query = savedInstanceState?.getString(KEY_SEARCH_QUERY).orEmpty()
        viewModel.onCreate(selectedIds, query)
    }

    private fun setupView() {
        mainBinding = ChuckerActivityMainBinding.inflate(layoutInflater)
        transactionsAdapter = MainTransactionAdapter(
            context = this,
            onTransactionClick = viewModel::onTransactionClick,
            onTransactionLongClick = viewModel::onTransactionLongClick,
        )

        with(mainBinding) {
            setContentView(root)
            applyInsets()
            setSupportActionBar(toolbar)
            toolbar.subtitle = applicationName

            tutorialLink.movementMethod = LinkMovementMethod.getInstance()
            transactionsRecyclerView.apply {
                setHasFixedSize(true)
                addItemDecoration(
                    DividerItemDecoration(
                        this@MainActivity2,
                        DividerItemDecoration.VERTICAL,
                    ),
                )
                adapter = transactionsAdapter
            }
        }
    }

    private fun applyInsets() {
        // Set up window insets to properly handle the UI around system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Apply insets to the main content to avoid overlap with system bars
        ViewCompat.setOnApplyWindowInsetsListener(mainBinding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainBinding.appBarLayout.updatePadding(top = insets.top)
            view.updatePadding(bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun observeState() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.state.collect { state ->
                val transactions = state.transactions
                transactionsAdapter.submitList(transactions)
                selectionBackCallback.isEnabled = state.isSelectionMode
                mainBinding.tutorialGroup.isVisible = transactions.isEmpty()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun observeEvents() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    MainActivityEvents.RequestNotificationPermissions -> handleNotificationsPermission()
                    is MainActivityEvents.NavigateToTransaction ->
                        TransactionActivity.start(this@MainActivity2, event.transactionId)

                    MainActivityEvents.ShowClearTransactionsDialog -> showDialog(
                        dialogData = getClearDialogData(),
                        onPositiveClick = viewModel::onConfirmClearTransactions,
                        onNegativeClick = null,
                    )

                    MainActivityEvents.ShowShareTextAllDialog -> showDialog(
                        dialogData = getExportDialogData(R.string.chucker_export_text_http_confirmation),
                        onPositiveClick = viewModel::onConfirmExportText,
                    )

                    MainActivityEvents.ShowShareTextSelectedDialog -> showDialog(
                        dialogData = getExportDialogData(R.string.chucker_export_text_selected_http_confirmation),
                        onPositiveClick = viewModel::onConfirmExportText,
                    )

                    MainActivityEvents.ShowShareHarAllDialog -> showDialog(
                        dialogData = getExportDialogData(R.string.chucker_export_har_http_confirmation),
                        onPositiveClick = viewModel::onConfirmExportHar,
                    )

                    MainActivityEvents.ShowShareHarSelectedDialog -> showDialog(
                        dialogData = getExportDialogData(R.string.chucker_export_har_selected_http_confirmation),
                        onPositiveClick = viewModel::onConfirmExportHar,
                    )

                    MainActivityEvents.ShowSaveTextDialog -> showSaveDialog(
                        TEXT,
                        R.string.chucker_save_text_http_confirmation,
                    )

                    MainActivityEvents.ShowSaveHarDialog -> showSaveDialog(
                        HAR,
                        R.string.chucker_save_har_http_confirmation,
                    )

                    is MainActivityEvents.LaunchSaveDocument -> when (event.type) {
                        TEXT -> saveTextToFile.launch(event.type.fileName)
                        HAR -> saveHarToFile.launch(event.type.fileName)
                    }

                    is MainActivityEvents.Share -> lifecycleScope.launch {
                        val shareIntent = withContext(Dispatchers.IO) {
                            event.data.shareAsFile(
                                activity = this@MainActivity2,
                                fileName = event.fileName,
                                intentTitle = getString(R.string.chucker_share_all_transactions_title),
                                intentSubject = getString(R.string.chucker_share_all_transactions_subject),
                                clipDataLabel = "transactions",
                            )
                        }
                        if (shareIntent != null) startActivity(shareIntent)
                        else showToast(getString(R.string.chucker_export_no_file))
                    }

                    is MainActivityEvents.ShowToast -> showToast(getString(event.stringId))
                }
            }
        }
    }

    private fun handleNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED -> {
                // We have permission, all good
            }

            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                Snackbar
                    .make(
                        mainBinding.root,
                        applicationContext.getString(R.string.chucker_notifications_permission_not_granted),
                        Snackbar.LENGTH_LONG,
                    ).setAction(applicationContext.getString(R.string.chucker_change)) {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                data = Uri.fromParts("package", packageName, null)
                            }.also { intent ->
                                startActivity(intent)
                            }
                    }.show()
            }

            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.chucker_transactions_list, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        setUpSearch(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setUpSearch(menu: Menu) {
        val searchMenuItem = menu.findItem(R.id.search)
        val searchView = searchMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.setIconifiedByDefault(true)
    }

    @Suppress("LongMethod")
    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.clear -> {
                viewModel.onClearTransactionsClick()
                true
            }

            R.id.share_text -> {
                viewModel.onShareTextClick()
                true
            }

            R.id.share_har -> {
                viewModel.onShareHarClick()
                true
            }

            R.id.save_text -> {
                viewModel.onSaveTextClick()
                true
            }

            R.id.save_har -> {
                viewModel.onSaveHarClick()
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }

    override fun onQueryTextSubmit(query: String): Boolean = true

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel.onQueryChanged(newText)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLongArray(
            KEY_SELECTED_TRANSACTION_IDS,
            viewModel.selectedTransactionIds.toLongArray()
        )
        outState.putString(KEY_SEARCH_QUERY, viewModel.state.value.query)
        super.onSaveInstanceState(outState)
    }

    private fun getClearDialogData(): DialogData =
        DialogData(
            title = getString(R.string.chucker_clear),
            message =
                getString(
                    if (viewModel.state.value.isSelectionMode) {
                        R.string.chucker_clear_selected_http_confirmation
                    } else {
                        R.string.chucker_clear_http_confirmation
                    },
                ),
            positiveButtonText = getString(R.string.chucker_clear),
            negativeButtonText = getString(R.string.chucker_cancel),
        )

    private fun getExportDialogData(
        @StringRes dialogMessage: Int,
    ): DialogData =
        DialogData(
            title = getString(R.string.chucker_export),
            message = getString(dialogMessage),
            positiveButtonText = getString(R.string.chucker_export),
            negativeButtonText = getString(R.string.chucker_cancel),
        )

    private fun getSaveDialogData(
        @StringRes dialogMessage: Int,
    ): DialogData =
        DialogData(
            title = getString(R.string.chucker_save),
            message = getString(dialogMessage),
            positiveButtonText = getString(R.string.chucker_save),
            negativeButtonText = getString(R.string.chucker_cancel),
        )

    private fun showSaveDialog(exportType: ExportType, @StringRes messageId: Int) {
        showDialog(
            getSaveDialogData(messageId),
            onPositiveClick = {
                viewModel.onConfirmSave(exportType)
            },
        )
    }

    companion object {
        private const val KEY_SELECTED_TRANSACTION_IDS = "selectedTransactionIds"
        private const val KEY_SEARCH_QUERY = "searchQuery"
    }
}
