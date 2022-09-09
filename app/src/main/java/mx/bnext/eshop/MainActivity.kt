package mx.bnext.eshop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.evergage.android.ClientConfiguration
import com.evergage.android.Context
import com.evergage.android.Evergage
import com.evergage.android.Screen
import com.evergage.android.promote.Product
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.salesforce.marketingcloud.MCLogListener
import com.salesforce.marketingcloud.MarketingCloudConfig
import com.salesforce.marketingcloud.MarketingCloudSdk
import com.salesforce.marketingcloud.notifications.NotificationCustomizationOptions
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.button)

        Evergage.initialize(application)
        val evergage = Evergage.getInstance()
        evergage.start(
            ClientConfiguration.Builder()
                .account("heinekenintlamer")
                .dataset("development")
                .usePushNotifications(true)
                .build()
        )

        btn.setOnClickListener {
            val screen: Screen? = evergage.getScreenForActivity(this)
            val contextEvergage: Context? = evergage.globalContext
            val product = Product("1998968")
            product.name = "Coca-Cola"
            if (screen != null)
                screen.viewItem(product)
            else
                contextEvergage?.viewItem(product)
        }

        // TODO: Don't do that, we are not using Evergage SDK for push messaging.
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task: Task<String?> ->
                if (!task.isSuccessful) {
                    Log.w("TokenPush", "getInstanceId failed", task.exception)
                    return@addOnCompleteListener
                }
                Log.d("Token", task.result!!)
                evergage.setFirebaseToken(task.result!!)
            }

        // TODO: Set to the GLUP USER ID as soon as its known.
        evergage.userId = "bartektesting3"

        if (BuildConfig.DEBUG) {
            MarketingCloudSdk.setLogLevel(MCLogListener.VERBOSE)
            MarketingCloudSdk.setLogListener(MCLogListener.AndroidLogListener())
        }

        SFMCSdk.configure(applicationContext, SFMCSdkModuleConfig.build {

            pushModuleConfig = MarketingCloudConfig.builder().apply {
                setApplicationId("843cdf42-c2ee-41d1-a92c-59228b584fa1")
                setAccessToken("e8Qe39PtBHMfJWOunCQdpY9X")
                setMarketingCloudServerUrl("https://mc9q6tzd8n0mybmv2n07qygb9fk8.device.marketingcloudapis.com/")
                // TODO: Do not set the senderId if there are multiple push services used eg. Marketing Cloud and Firebase.
                // TODO: Set the delay of Contact Key registration, so once the GLUP USER ID is known, only then it should be set as ContactKey.
                setDelayRegistrationUntilContactKeyIsSet(true)
                setMid("534002103")
                setNotificationCustomizationOptions(
                    NotificationCustomizationOptions.create(R.mipmap.ic_launcher)
                )
            }.build(applicationContext)

        }) { initStatus ->
            Log.d("Alfredo", initStatus.toString())
        }

        // TODO: After the SFMCSdk is initiated device token needs to be set. First set the device token then the Contact Key.
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task: Task<String?> ->
                if (!task.isSuccessful) {
                    Log.w("TokenPush", "getInstanceId failed", task.exception)
                    return@addOnCompleteListener
                }
                Log.d("Token", task.result!!)
                SFMCSdk.requestSdk { sdk ->
                    sdk.mp {
                        it.pushMessageManager.setPushToken(task.result!!)
                    }
                }
            }

        // TODO: After the token is set, only then SET THE contactKey with GLUP USER ID
        SFMCSdk.requestSdk { sdk ->
            // Set Contact Key
            sdk.identity.setProfileId(evergage.userId!!)

            // Get Contact Key
            sdk.mp {
                val contactKey = it.moduleIdentity.profileId
                if (contactKey != null) {
                    Log.d("Contact Key", contactKey)
                }
            }
        }
    }


    // [START ask_post_notifications]
    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher =
        registerForActivityResult<String, Boolean>(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->

        }

    // [START_EXCLUDE]
    @RequiresApi(33) // [END_EXCLUDE]
    private fun askNotificationPermission() {
        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {

        } else */if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            // TODO: display an educational UI explaining to the user the features that will be enabled
            //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
            //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
            //       If the user selects "No thanks," allow the user to continue without notifications.
        } else {
            // Directly ask for the permission
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}