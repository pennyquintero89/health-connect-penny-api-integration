package com.healthsync.background.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import com.healthsync.background.R
import com.healthsync.background.config.AppConfig
import com.healthsync.background.config.WorkScheduler
import com.healthsync.background.scheduler.DailyScheduler
import com.healthsync.background.scheduler.TestScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.records.*
import androidx.core.net.toUri
import kotlinx.coroutines.delay

@AndroidEntryPoint
class PermissionActivity : AppCompatActivity() {

    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var requestPermissionActivityLauncher: ActivityResultLauncher<Set<String>>

    private lateinit var healthConnectLauncher: ActivityResultLauncher<Intent>


    // UI State variables
    private var isSyncing = false
    private var lastSyncTime: String? = null

    private val readPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
        HealthPermission.getReadPermission(FloorsClimbedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(BodyWaterMassRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
        HealthPermission.getReadPermission(BoneMassRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
        HealthPermission.getReadPermission(SexualActivityRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        diagnosticHealthConnect()

        val availabilityStatus = HealthConnectClient.getSdkStatus(this)
        Log.d(PERMISSION_ACTIVITY_NAME, "Health Connect status: $availabilityStatus")

        when (availabilityStatus) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                Log.e(PERMISSION_ACTIVITY_NAME, "Health Connect SDK not available")
                showInstallHealthConnectDialog()
                return
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Log.e(PERMISSION_ACTIVITY_NAME, "Health Connect needs update")
                showUpdateHealthConnectDialog()
                return
            }
            HealthConnectClient.SDK_AVAILABLE -> {
                Log.i(PERMISSION_ACTIVITY_NAME, "Health Connect available")
            }
        }

        try {
            healthConnectClient = HealthConnectClient.getOrCreate(this)
            Log.d(PERMISSION_ACTIVITY_NAME, "HealthConnectClient created successfully")
        } catch (e: Exception) {
            Log.e(PERMISSION_ACTIVITY_NAME, "Error creating HealthConnectClient", e)
            showErrorDialog(getString(R.string.error_connecting_health_connect, e.message))
            return
        }

        val requestPermissionActivityContract =
            PermissionController.createRequestPermissionResultContract()

        requestPermissionActivityLauncher = registerForActivityResult(
            requestPermissionActivityContract
        ) { grantedPermissions ->
            Log.d(PERMISSION_ACTIVITY_NAME, "=== CALLBACK RECEIVED ===")
            Log.d(PERMISSION_ACTIVITY_NAME, "Granted permissions: $grantedPermissions")
            Log.d(PERMISSION_ACTIVITY_NAME, "Required permissions: $readPermissions")
            Log.d(PERMISSION_ACTIVITY_NAME, "Thread: ${Thread.currentThread().name}")
            handlePermissionResult(grantedPermissions)
        }

        healthConnectLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d(PERMISSION_ACTIVITY_NAME, "Health Connect result: ${result.resultCode}")
            checkPermissionsOnStart()
        }

        checkPermissionsOnStart()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.button_request_permissions).setOnClickListener {
            Log.d(PERMISSION_ACTIVITY_NAME, "Manual permission request button clicked")
            requestPermissions()
        }

        findViewById<Button>(R.id.button_open_health_connect)?.setOnClickListener {
            Log.d(PERMISSION_ACTIVITY_NAME, "Opening Health Connect directly")
            openHealthConnectDirectly()
        }

        // Add sync button listener if it exists in the layout
        findViewById<Button>(R.id.button_sync_data)?.setOnClickListener {
            Log.d(PERMISSION_ACTIVITY_NAME, "Manual sync requested")
            startDataSync()
        }
    }

    private fun diagnosticHealthConnect() {
        Log.d(PERMISSION_ACTIVITY_NAME, "=== DIAGNOSTIC INFO ===")
        Log.d(PERMISSION_ACTIVITY_NAME, "Package name: $packageName")
        Log.d(PERMISSION_ACTIVITY_NAME, "SDK version: ${Build.VERSION.SDK_INT}")

        // Verificar si Health Connect esta instalado
        try {
            val pm = packageManager
            val healthConnectInfo = pm.getApplicationInfo("com.google.android.apps.healthdata", 0)
            Log.d(PERMISSION_ACTIVITY_NAME, "Health Connect installed: ${healthConnectInfo.enabled}")
            Log.d(PERMISSION_ACTIVITY_NAME, "Health Connect version: ${pm.getPackageInfo("com.google.android.apps.healthdata", 0).versionName}")
        } catch (e: Exception) {
            Log.e(PERMISSION_ACTIVITY_NAME, "Health Connect not found: $e")
        }

        // Verificar permisos en manifest
        try {
            val pm = packageManager
            val permissions = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
            Log.d(PERMISSION_ACTIVITY_NAME, "Manifest permissions: ${permissions?.toList()}")
        } catch (e: Exception) {
            Log.e(PERMISSION_ACTIVITY_NAME, "Error reading manifest permissions: $e")
        }
    }

    private fun checkPermissionsOnStart() {
        lifecycleScope.launch {
            try {
                Log.d(PERMISSION_ACTIVITY_NAME, "Checking current permissions...")
                val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
                Log.d(PERMISSION_ACTIVITY_NAME, "Current granted permissions: $grantedPermissions")
                Log.d(PERMISSION_ACTIVITY_NAME, "Required permissions: $readPermissions")

                val missingPermissions = readPermissions - grantedPermissions
                if (missingPermissions.isEmpty()) {
                    Log.d(PERMISSION_ACTIVITY_NAME, "All permissions already granted")
                    updateUIForAllPermissionsGranted()
                    // Don't finish the activity, let user stay to use sync and Health Connect access
                    scheduleWorker()
                } else {
                    Log.d(PERMISSION_ACTIVITY_NAME, "Missing permissions: $missingPermissions")
                    updateUIForPermissionRequest()
                }
            } catch (e: Exception) {
                Log.e(PERMISSION_ACTIVITY_NAME, "Error checking permissions", e)
                updateUIForPermissionRequest()
            }
        }
    }

    private fun requestPermissions() {
        Log.d(PERMISSION_ACTIVITY_NAME, "=== REQUESTING PERMISSIONS ===")
        Log.d(PERMISSION_ACTIVITY_NAME, "Permissions to request: $readPermissions")

        try {
            // Verificar que el launcher estÃ© listo
            if (!::requestPermissionActivityLauncher.isInitialized) {
                Log.e(PERMISSION_ACTIVITY_NAME, "Permission launcher not initialized")
                showErrorDialog(getString(R.string.internal_error_launcher))
                return
            }

            // Verificar el estado actual de la activity
            Log.d(PERMISSION_ACTIVITY_NAME, "Activity state - isFinishing: $isFinishing, isDestroyed: $isDestroyed")

            // Lanzar la solicitud de permisos
            Log.d(PERMISSION_ACTIVITY_NAME, "Launching permission request...")
            requestPermissionActivityLauncher.launch(readPermissions)
            Log.d(PERMISSION_ACTIVITY_NAME, "Permission request launched successfully")

        } catch (e: Exception) {
            Log.e(PERMISSION_ACTIVITY_NAME, "Error launching permission request", e)
            showErrorDialog(getString(R.string.error_requesting_permissions, e.message))
        }
    }

    private fun openHealthConnectDirectly() {
        try {
            val intent = Intent().apply {
                setClassName(
                    "com.google.android.apps.healthdata",
                    "com.google.android.apps.healthdata.permissions.PermissionsActivity"
                )
                putExtra("package_name", packageName)
            }

            Log.d(PERMISSION_ACTIVITY_NAME, "Trying to open Health Connect directly...")
            healthConnectLauncher.launch(intent)

        } catch (e: Exception) {
            Log.e(PERMISSION_ACTIVITY_NAME, "Error opening Health Connect directly", e)

            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    showErrorDialog(getString(R.string.health_connect_not_installed))
                }
            } catch (e2: Exception) {
                Log.e(PERMISSION_ACTIVITY_NAME, "Error with fallback", e2)
                showErrorDialog(getString(R.string.health_connect_cannot_open, e2.message))
            }
        }
    }

    private fun startDataSync() {
        if (isSyncing) {
            Log.d(PERMISSION_ACTIVITY_NAME, "Sync already in progress, ignoring request")
            return
        }

        lifecycleScope.launch {
            try {
                isSyncing = true
                updateUIForSyncing()

                Log.d(PERMISSION_ACTIVITY_NAME, "Starting data synchronization...")

                // Schedule the worker for immediate execution
                scheduleWorker()

                // Simulate sync process (replace this with actual sync logic if needed)
                // In a real scenario, you might want to listen to WorkManager status
                delay(3000) // Simulate network/sync delay

                // Update last sync time
                lastSyncTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())

                Log.d(PERMISSION_ACTIVITY_NAME, "Data synchronization completed")
                updateUIForSyncCompleted()

            } catch (e: Exception) {
                Log.e(PERMISSION_ACTIVITY_NAME, "Error during sync", e)
                updateUIForSyncError(e.message ?: "Unknown error")
            } finally {
                isSyncing = false
            }
        }
    }

    private fun handlePermissionResult(grantedPermissions: Set<String>) {
        Log.d(PERMISSION_ACTIVITY_NAME, "=== PERMISSION RESULT ===")
        Log.d(PERMISSION_ACTIVITY_NAME, "Granted: $grantedPermissions")
        Log.d(PERMISSION_ACTIVITY_NAME, "Required: $readPermissions")

        val missingPermissions = readPermissions - grantedPermissions

        if (missingPermissions.isEmpty()) {
            Log.d(PERMISSION_ACTIVITY_NAME, "âœ… All permissions needed granted successfully")
            updateUIForAllPermissionsGranted()
            scheduleWorker()
            // Don't finish the activity anymore
        } else {
            Log.w("PermissionActivity", "âŒ Missing permissions: $missingPermissions")
            updateUIForPermissionDenied()
        }
    }

    private fun updateUIForPermissionRequest() {
        findViewById<TextView>(R.id.text_status)?.text = getString(R.string.health_connect_permissions_needed)
        findViewById<Button>(R.id.button_request_permissions)?.apply {
            isEnabled = true
            text = getString(R.string.grant_permissions)
        }
        findViewById<Button>(R.id.button_open_health_connect)?.apply {
            isEnabled = true
            text = getString(R.string.open_health_connect_settings)
        }
        findViewById<Button>(R.id.button_sync_data)?.apply {
            isEnabled = false
            text = getString(R.string.sync_data_permissions_required)
        }
    }

    private fun updateUIForAllPermissionsGranted() {
        findViewById<TextView>(R.id.text_status)?.text =
            if (lastSyncTime != null)
                getString(R.string.permissions_granted_last_sync, lastSyncTime)
            else
                getString(R.string.all_permissions_granted_ready)

        findViewById<Button>(R.id.button_request_permissions)?.apply {
            isEnabled = false
            text = getString(R.string.permissions_granted)
        }
        findViewById<Button>(R.id.button_open_health_connect)?.apply {
            isEnabled = true
            text = getString(R.string.open_health_connect_settings)
        }
        findViewById<Button>(R.id.button_sync_data)?.apply {
            isEnabled = !isSyncing
            text = if (isSyncing) getString(R.string.syncing_ellipsis) else getString(R.string.sync_health_data)
        }
    }

    private fun updateUIForPermissionDenied() {
        findViewById<TextView>(R.id.text_status)?.text = getString(R.string.some_permissions_denied)
        findViewById<Button>(R.id.button_request_permissions)?.apply {
            isEnabled = true
            text = getString(R.string.retry_permission_request)
        }
        findViewById<Button>(R.id.button_open_health_connect)?.apply {
            isEnabled = true
            text = getString(R.string.open_health_connect_settings)
        }
        findViewById<Button>(R.id.button_sync_data)?.apply {
            isEnabled = false
            text = getString(R.string.sync_data_permissions_required)
        }
    }

    private fun updateUIForSyncing() {
        findViewById<TextView>(R.id.text_status)?.text = getString(R.string.synchronizing_data)

        findViewById<Button>(R.id.button_sync_data)?.apply {
            isEnabled = false
            text = getString(R.string.syncing_ellipsis)
        }
        findViewById<Button>(R.id.button_request_permissions)?.isEnabled = false
    }

    private fun updateUIForSyncCompleted() {
        findViewById<TextView>(R.id.text_status)?.text =
            getString(R.string.data_synchronized_at, lastSyncTime)

        findViewById<Button>(R.id.button_sync_data)?.apply {
            isEnabled = true
            text = getString(R.string.sync_health_data)
        }
        findViewById<Button>(R.id.button_request_permissions)?.isEnabled = false
    }

    private fun updateUIForSyncError(error: String) {
        findViewById<TextView>(R.id.text_status)?.text = getString(R.string.sync_failed_error, error)

        findViewById<Button>(R.id.button_sync_data)?.apply {
            isEnabled = true
            text = getString(R.string.retry_sync)
        }
        findViewById<Button>(R.id.button_request_permissions)?.isEnabled = false
    }

    private fun showInstallHealthConnectDialog() {
        AlertDialog.Builder(this)
            .setTitle("Health Connect needed")
            .setMessage("This app needs Health Connect to work properly. Would you like to install it from the Play Store first?")
            .setPositiveButton("Install") { _, _ ->
                openHealthConnectInPlayStore()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Don't finish, let user stay in the app
            }
            .setCancelable(false)
            .show()
    }

    private fun showUpdateHealthConnectDialog() {
        AlertDialog.Builder(this)
            .setTitle("Updated required")
            .setMessage("Health Connect needs to be updated in order for this app to work properly")
            .setPositiveButton("Update") { _, _ ->
                openHealthConnectInPlayStore()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Don't finish, let user stay in the app
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                // No cerrar automáticamente para permitir reintentos
            }
            .show()
    }

    private fun openHealthConnectInPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "market://details?id=com.google.android.apps.healthdata".toUri()
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data =
                    "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata".toUri()
            }
            startActivity(intent)
        }
    }

    private fun scheduleWorker() {
        try {
            val scheduler: WorkScheduler = if (AppConfig.testMode) TestScheduler() else DailyScheduler()
            scheduler.scheduleWork(this)
            Log.d(PERMISSION_ACTIVITY_NAME, "Worker scheduled successfully")
        } catch (e: Exception) {
            Log.e(PERMISSION_ACTIVITY_NAME, "Error scheduling worker", e)
        }
    }

    companion object {
        private const val PERMISSION_ACTIVITY_NAME: String = "PermissionActivity"
    }
}