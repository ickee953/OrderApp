/**
 * © Panov Vitaly 2023 - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package ru.fl.marketplace.app.ui

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.MapKitFactory
import ru.fl.marketplace.app.BuildConfig
import ru.fl.marketplace.app.R
import ru.fl.marketplace.app.data.model.AppVersion
import ru.fl.marketplace.app.data.viewmodel.AppVersionViewModel
import ru.fl.marketplace.app.databinding.ActivityMainBinding
import ru.fl.marketplace.app.utils.Refreshable
import ru.fl.marketplace.app.utils.Status
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    private var _binding: ActivityMainBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    val binding get() = _binding!!

    private var versionCode = 0
    private var remoteAppVersion: AppVersion? = null
    private var downloadedApkFile: File? = null
    private var downloadedApkDestination: String? = null

    companion object {
        const val PERMISSION_REQUEST_STORAGE = 0
        const val PERMISSION_REQUEST_INSTALL = 1

        internal const val FILE_NAME        = "demo_app.apk"
        private const val FILE_BASE_PATH    = "file://"
        private const val MIME_TYPE         = "application/vnd.android.package-archive"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarMainActivity)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_item_detail) as NavHostFragment

        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)

        setupActionBarWithNavController(navController, appBarConfiguration)

        //if is release build then check for updates
        if( !BuildConfig.DEBUG){
            checkForUpdates()
        }

        MapKitFactory.setApiKey("6961d5ad-482b-4184-a65f-f13d1e09f70e");
        MapKitFactory.initialize(this)

    }

    fun setLiftOnScroll( enabled: Boolean ){
        binding.appbarMainActivity.isLiftOnScroll = enabled
    }

    fun showUpToolbar(){
        binding.appbarMainActivity.setExpanded(true, true)
    }

    /*private fun refresh(){
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_item_detail) as NavHostFragment

        if( navHostFragment.childFragmentManager.fragments[0] is Refreshable){
            val refreshableFragment = navHostFragment.childFragmentManager.fragments[0] as Refreshable

            refreshableFragment.refresh()

            binding.networkStatusView.visibility = View.GONE
            binding.navHostFragmentItemDetail.visibility = View.VISIBLE

        }
    }*/

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_item_detail)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun onNetworkError(message: String?) {
        binding.connectionMessage.text = message
        binding.networkStatusView.visibility = View.VISIBLE
        binding.navHostFragmentItemDetail.visibility = View.GONE
    }

    private fun checkForUpdates(){
        //check version
        var pInfo: PackageInfo? = null
        try {
            pInfo = packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        //get the app version Code for checking
        versionCode = pInfo!!.versionCode

        val viewModel = ViewModelProviders.of(this).get(AppVersionViewModel::class.java)
        viewModel.checkAppVersion().observe(this) { resource ->
            resource?.let {
                when(it.status){
                    Status.LOADING -> {

                    }
                    Status.ERROR -> {

                    }
                    Status.SUCCESS_REMOTE -> {
                        remoteAppVersion = it.data
                        if (remoteAppVersion != null && versionCode < remoteAppVersion!!.versionCode ) {

                            val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
                            builder.setMessage("There is newer version of this application available, click OK to upgrade now?")
                                .setPositiveButton(
                                    "OK",
                                    DialogInterface.OnClickListener { dialog, id ->
                                        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                            // start downloading
                                            //DownloadController(this@MainActivity, remoteAppVersion!!.appUrl).enqueueDownload()
                                            enqueueDownload(remoteAppVersion!!.appUrl)
                                        } else {
                                            // Permission is missing and must be requested.
                                            requestStoragePermission()
                                        }
                                    })
                                .setNegativeButton(
                                    "Remind Later",
                                    DialogInterface.OnClickListener { dialog, id ->
                                        // User cancelled the dialog
                                    })
                            //show the alert message
                            builder.create().show()

                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun enqueueDownload(url : String? ) {
        var destination =
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"

        downloadedApkFile = File(destination, FILE_NAME)

        destination += FILE_NAME
        val uri = Uri.parse("$FILE_BASE_PATH$destination")
        val file = File(destination)
        if (file.exists()) file.delete()
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        request.setMimeType(MIME_TYPE)
        request.setTitle(getString(R.string.title_file_download))
        request.setDescription(getString(R.string.downloading))
        // set destination
        request.setDestinationUri(uri)
        showInstallOption(destination, uri)
        // Enqueue a new download and same the referenceId
        downloadManager.enqueue(request)
        Toast.makeText(this, getString(R.string.downloading), Toast.LENGTH_LONG)
            .show()
    }

    private fun showInstallOption(
        destination: String,
        uri: Uri
    ) {
        // set BroadcastReceiver to install app when .apk is downloaded
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                downloadedApkDestination = destination

                //if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //    (context as MainActivity).requestInstallPermission()
                //}
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && !packageManager.canRequestPackageInstalls()
                ) {
                    val unknownAppSourceIntent = Intent()
                        .setAction(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse(String.format("package:%s", packageName)))
                    unknownAppSourceDialog.launch(unknownAppSourceIntent)
                } else {
                    // App already have the permission to install so launch the APK installation.
                    startActivity(intent)
                }*/

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val apkUri = downloadedApkFile?.let {
                        FileProvider.getUriForFile(
                            this@MainActivity,
                            BuildConfig.APPLICATION_ID + ".provider",
                            it
                        )
                    }
                    val install = Intent(Intent.ACTION_VIEW)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    install.data = apkUri
                    startActivity(install)
                    //finish()
                } else {
                    val install = Intent(Intent.ACTION_VIEW)
                    install.setDataAndType(
                        Uri.fromFile(downloadedApkFile),
                        MIME_TYPE
                    )
                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                    startActivity(install)
                    //finish()
                }

                unregisterReceiver(this)

            }
        }
        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    /*var unknownAppSourceDialog: ActivityResultLauncher<Intent> =
        registerForActivityResult<Intent, ActivityResult>(
            StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                // User has allowed app to install APKs
                // so we can now launch APK installation.
                startActivity(intent)
            }
        }*/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            // Request for camera permission.
            if (grantResults.size == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // start downloading
                //DownloadController(this@MainActivity, remoteAppVersion!!.appUrl).enqueueDownload()
                enqueueDownload(remoteAppVersion!!.appUrl)
            } else {
                // Permission request was denied.
                Snackbar.make(binding.mainContent, R.string.storage_permission_denied, Snackbar.LENGTH_SHORT)
            }
        } else if(requestCode == PERMISSION_REQUEST_INSTALL) {

            if (grantResults.size == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && !packageManager.canRequestPackageInstalls()
                ) {
                    val unknownAppSourceIntent = Intent()
                        .setAction(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse(String.format("package:%s", packageName)))
                    unknownAppSourceDialog.launch(unknownAppSourceIntent)
                } else {
                    // App already have the permission to install so launch the APK installation.
                    startActivity(intent)
                }*/

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    /*val apkUri = downloadedApkFile?.let {
                        FileProvider.getUriForFile(
                            this,
                            BuildConfig.APPLICATION_ID + ".provider",
                            it
                        )
                    }*/
                    val install = Intent(Intent.ACTION_VIEW)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    install.data = Uri.fromFile(downloadedApkFile)
                    startActivity(install)
                    //finish()
                } else {
                    val install = Intent(Intent.ACTION_VIEW)
                    install.setDataAndType(
                        Uri.fromFile(downloadedApkFile),
                        MIME_TYPE
                    )
                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                    startActivity(install)
                    //finish()
                }

            } else {
                // Permission request was denied.
                Snackbar.make(binding.mainContent, R.string.storage_permission_denied, Snackbar.LENGTH_SHORT)
            }
        }
    }

    private fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            val snackbar = Snackbar.make(binding.mainContent,
                R.string.storage_access_required, Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction(R.string.storage_access_required) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_STORAGE
                )
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_STORAGE
            )
        }
    }

    /*private fun showPopUpMessage(message: String?){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setVisibleActionItem(i: Int, isVisible: Boolean) {
        val menuItem = binding.toolbarMainActivity.menu.getItem(i)
        menuItem.isVisible = isVisible
    }*/

    /*fun downloadAllItems() {
        setVisibleActionItem(0, false)

        val downloadAllItemsWork = OneTimeWorkRequestBuilder<DownloadAllItemsWorker>()
            .build()

        val workManager = WorkManager.getInstance(this)

        workManager.enqueue(downloadAllItemsWork)

        workManager.getWorkInfoByIdLiveData(downloadAllItemsWork.id)
            .observe(this) { info ->
                if (info != null) {
                    when (info.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = info.progress.getInt(DownloadAllItemsWorker.PROGRESS, 0)
                            if (progress == 0) {
                                //init progress bar
                                binding.progressIndicator.visibility = View.GONE
                                binding.progressIndicator.isIndeterminate = true
                                binding.progressIndicator.progress = 0
                                binding.progressIndicator.visibility = View.VISIBLE
                            } else {
                                binding.progressIndicator.isIndeterminate = false
                            }
                            binding.progressIndicator.progress = progress
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            binding.progressIndicator.visibility = View.GONE
                            setVisibleActionItem(0, true)

                            refresh()
                        }
                        WorkInfo.State.FAILED -> {
                            val message =
                                info.outputData.getString(DownloadAllItemsWorker.MESSAGE_PARAM)

                            binding.progressIndicator.visibility = View.GONE
                            setVisibleActionItem(0, true)
                            showPopUpMessage(message)
                        }
                        else -> {}
                    }
                }
            }
    }*/
}
