package com.example.firebaseapplication

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.facebook.login.LoginManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.android.synthetic.main.activity_home.*

enum class ProviderType{
    BASIC,
    GOOGLE,
    FACEBOOK
}

private lateinit var firebaseAnalytics: FirebaseAnalytics

class HomeActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        firebaseAnalytics = Firebase.analytics

        //Setup

        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")
        setup(email?: "", provider?: "")

        //Guardado de datos

        val prefs = getSharedPreferences("prefs_file", Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()

        // Remote Config
        buttonError.visibility = View.INVISIBLE
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if(task.isSuccessful){
                val showErrorButton = Firebase.remoteConfig.getBoolean("show_error_button")
                val errorButtonText = Firebase.remoteConfig.getString("error_button_text")

                if(showErrorButton){
                    buttonError.visibility = View.VISIBLE
                }

                buttonError.text = errorButtonText
            }
        }
    }

    private fun setup(email: String, provider: String) {

        title = "Inicio"
        textViewEmail.text = email
        textViewProvider.text = provider

        buttonLogOut.setOnClickListener {
            val prefs = getSharedPreferences("prefs_file", Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            if(provider == ProviderType.FACEBOOK.name){
                LoginManager.getInstance().logOut()
            }

            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }

        buttonError.setOnClickListener {

            FirebaseCrashlytics.getInstance().setUserId(email)
            FirebaseCrashlytics.getInstance().setCustomKey("provider", provider)

            //Enviar log de contexto
            FirebaseCrashlytics.getInstance().log("Se ha pulsado el boton forazar error")

            //Forzado de error
            //throw RuntimeException("Forzado de error")
        }

        buttonSave.setOnClickListener {

            db.collection("users").document(email).set(
                hashMapOf("provider" to provider,
                "address" to textViewAddress.text.toString(),
                "phone" to textViewPhone.text.toString())
            )
        }

        buttonGet.setOnClickListener {

            db.collection("users").document(email).get().addOnSuccessListener {
                textViewAddress.setText(it.get("address") as String?)
                textViewPhone.setText(it.get("phone") as String?)
            }
        }

        buttonDelete.setOnClickListener {

            db.collection("users").document(email).delete()
        }

        buttonAnimations.setOnClickListener {

            val animationsIntent = Intent(this, LottieAnimations::class.java)
            startActivity(animationsIntent)
        }
    }
}