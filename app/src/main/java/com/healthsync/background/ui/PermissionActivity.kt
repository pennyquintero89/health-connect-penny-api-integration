package com.healthsync.background.ui

import android.content.Intent
import android.net.Uri
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
import androidx.health.connect.client.records.*

@AndroidEntryPoint
class PermissionActivity : AppCompatActivity() {

    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var requestPermissionActivityLauncher: ActivityResultLauncher<Set<String>>

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
        Log.d("PermissionActivity", "Health Connect status: $availabilityStatus")

        when (availabilityStatus) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                Log.e("PermissionActivity", "Health Connect SDK not available")
                showInstallHealthConnectDialog()
                return
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Log.e("PermissionActivity", "Health Connect needs update")
                showUpdateHealthConnectDialog()
                return
            }
            HealthConnectClient.SDK_AVAILABLE -> {
                Log.i("PermissionActivity", "Health Connect available")
            }
        }

        try {
            healthConnectClient = HealthConnectClient.getOrCreate(this)
            Log.d("PermissionActivity", "HealthConnectClient created successfully")
        } catch (e: Exception) {
            Log.e("PermissionActivity", "Error creating HealthConnectClient", e)
            showErrorDialog("Error connecting to Health Connect: ${e.message}")
            return
        }

        val requestPermissionActivityContract =
            PermissionController.createRequestPermissionResultContract()

        requestPermissionActivityLauncher = registerForActivityResult(
            requestPermissionActivityContract
        ) { grantedPermissions ->
            Log.d("PermissionActivity", "=== CALLBACK RECEIVED ===")
            Log.d("PermissionActivity", "Granted permissions: $grantedPermissions")
            Log.d("PermissionActivity", "Required permissions: $readPermissions")
            Log.d("PermissionActivity", "Thread: ${Thread.currentThread().name}")
            handlePermissionResult(grantedPermissions)
        }

        checkPermissionsOnStart()

        findViewById<Button>(R.id.button_request_permissions).setOnClickListener {
            Log.d("PermissionActivity", "Manual permission request button clicked")
            requestPermissions()
        }

        findViewById<Button>(R.id.button_open_health_connect)?.setOnClickListener {
            Log.d("PermissionActivity", "Opening Health Connect directly")
            openHealthConnectDirectly()
        }
    }

    private fun diagnosticHealthConnect() {
        Log.d("PermissionActivity", "=== DIAGNOSTIC INFO ===")
        Log.d("PermissionActivity", "Package name: $packageName")
        Log.d("PermissionActivity", "SDK version: ${Build.VERSION.SDK_INT}")

        // Verificar si Health Connect está instalado
        try {
            val pm = packageManager
            val healthConnectInfo = pm.getApplicationInfo("com.google.android.apps.healthdata", 0)
            Log.d("PermissionActivity", "Health Connect installed: ${healthConnectInfo.enabled}")
            Log.d("PermissionActivity", "Health Connect version: ${pm.getPackageInfo("com.google.android.apps.healthdata", 0).versionName}")
        } catch (e: Exception) {
            Log.e("PermissionActivity", "Health Connect not found: $e")
        }

        // Verificar permisos en manifest
        try {
            val pm = packageManager
            val permissions = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
            Log.d("PermissionActivity", "Manifest permissions: ${permissions?.toList()}")
        } catch (e: Exception) {
            Log.e("PermissionActivity", "Error reading manifest permissions: $e")
        }
    }

    private fun checkPermissionsOnStart() {
        lifecycleScope.launch {
            try {
                Log.d("PermissionActivity", "Checking current permissions...")
                val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
                Log.d("PermissionActivity", "Current granted permissions: $grantedPermissions")
                Log.d("PermissionActivity", "Required permissions: $readPermissions")

                val missingPermissions = readPermissions - grantedPermissions
                if (missingPermissions.isEmpty()) {
                    Log.d("PermissionActivity", "All permissions already granted")
                    updateUIForAllPermissionsGranted()
                    setResult(RESULT_OK)
                    scheduleWorker()
                    finish()
                } else {
                    Log.d("PermissionActivity", "Missing permissions: $missingPermissions")
                    updateUIForPermissionRequest()
                }
            } catch (e: Exception) {
                Log.e("PermissionActivity", "Error checking permissions", e)
                updateUIForPermissionRequest()
            }
        }
    }

    private fun requestPermissions() {
        Log.d("PermissionActivity", "=== REQUESTING PERMISSIONS ===")
        Log.d("PermissionActivity", "Permissions to request: $readPermissions")

        try {
            // Verificar que el launcher esté listo
            if (!::requestPermissionActivityLauncher.isInitialized) {
                Log.e("PermissionActivity", "Permission launcher not initialized")
                showErrorDialog("Internal Error: launcher not initialized")
                return
            }

            // Verificar el estado actual de la activity
            Log.d("PermissionActivity", "Activity state - isFinishing: $isFinishing, isDestroyed: $isDestroyed")

            // Lanzar la solicitud de permisos
            Log.d("PermissionActivity", "Launching permission request...")
            requestPermissionActivityLauncher.launch(readPermissions)
            Log.d("PermissionActivity", "Permission request launched successfully")

        } catch (e: Exception) {
            Log.e("PermissionActivity", "Error launching permission request", e)
            showErrorDialog("Error requesting permissions: ${e.message}")
        }
    }

    private fun openHealthConnectDirectly() {
        try {
            // Intentar abrir Health Connect directamente
            val intent = Intent().apply {
                setClassName(
                    "com.google.android.apps.healthdata",
                    "com.google.android.apps.healthdata.permissions.PermissionsActivity"
                )
                putExtra("package_name", packageName)
            }

            Log.d("PermissionActivity", "Trying to open Health Connect directly...")
            startActivityForResult(intent, REQUEST_CODE_HEALTH_CONNECT)

        } catch (e: Exception) {
            Log.e("PermissionActivity", "Error opening Health Connect directly", e)

            // Fallback: abrir Health Connect en general
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    showErrorDialog("Health Connect its not installed")
                }
            } catch (e2: Exception) {
                Log.e("PermissionActivity", "Error with fallback", e2)
                showErrorDialog("Health Connect cannot be opened: ${e2.message}")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("PermissionActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == REQUEST_CODE_HEALTH_CONNECT) {
            // Verificar permisos nuevamente después de regresar de Health Connect
            checkPermissionsOnStart()
        }
    }

    private fun handlePermissionResult(grantedPermissions: Set<String>) {
        Log.d("PermissionActivity", "=== PERMISSION RESULT ===")
        Log.d("PermissionActivity", "Granted: $grantedPermissions")
        Log.d("PermissionActivity", "Required: $readPermissions")

        val missingPermissions = readPermissions - grantedPermissions

        if (missingPermissions.isEmpty()) {
            Log.d("PermissionActivity", "✅ All permissions needed granted successfully")
            updateUIForAllPermissionsGranted()
            setResult(RESULT_OK)
            scheduleWorker()
        } else {
            Log.w("PermissionActivity", "❌ Missing permissions: $missingPermissions")
            updateUIForPermissionDenied()
            setResult(RESULT_CANCELED)
        }

        // Pequeño delay para que el usuario vea el resultado
        lifecycleScope.launch {
            kotlinx.coroutines.delay(2000)
            finish()
        }
    }

    private fun updateUIForPermissionRequest() {
        findViewById<TextView>(R.id.text_status)?.text =
            "Health Connect permissions are needed in order to read Health Data"
        findViewById<Button>(R.id.button_request_permissions)?.apply {
            isEnabled = true
            text = "Grant Permissions (Launcher)"
        }
        findViewById<Button>(R.id.button_open_health_connect)?.apply {
            isEnabled = true
            text = "Open Health Connect app directly"
        }
    }

    private fun updateUIForAllPermissionsGranted() {
        findViewById<TextView>(R.id.text_status)?.text =
            "✅ All needed permissions granted."
        findViewById<Button>(R.id.button_request_permissions)?.apply {
            isEnabled = false
            text = "Permissions granted"
        }
        findViewById<Button>(R.id.button_open_health_connect)?.isEnabled = false
    }

    private fun updateUIForPermissionDenied() {
        findViewById<TextView>(R.id.text_status)?.text =
            "❌ Some permissions were denied. Try opening the Health Connect directly."
        findViewById<Button>(R.id.button_request_permissions)?.apply {
            isEnabled = true
            text = "Retry Permissions Launcher"
        }
        findViewById<Button>(R.id.button_open_health_connect)?.apply {
            isEnabled = true
            text = "Open Health Connect app"
        }
    }

    private fun showInstallHealthConnectDialog() {
        AlertDialog.Builder(this)
            .setTitle("Health Connect needed")
            .setMessage("This app needs Health Connect to work properly. Would you like to install it from the Play Store first?")
            .setPositiveButton("Install") { _, _ ->
                openHealthConnectInPlayStore()
            }
            .setNegativeButton("Cancel") { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
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
                setResult(RESULT_CANCELED)
                finish()
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
                data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            }
            startActivity(intent)
        }
    }

    private fun scheduleWorker() {
        try {
            val scheduler: WorkScheduler = if (AppConfig.testMode) TestScheduler() else DailyScheduler()
            scheduler.scheduleWork(this)
            Log.d("PermissionActivity", "Worker scheduled successfully")
        } catch (e: Exception) {
            Log.e("PermissionActivity", "Error scheduling worker", e)
        }
    }

    companion object {
        private const val REQUEST_CODE_HEALTH_CONNECT = 1001
    }
}
