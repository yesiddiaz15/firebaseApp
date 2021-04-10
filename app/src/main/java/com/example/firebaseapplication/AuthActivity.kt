package com.example.firebaseapplication

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.android.synthetic.main.activity_auth.*
import kotlinx.android.synthetic.main.activity_home.*
import java.security.Provider

class AuthActivity : AppCompatActivity() {

    private val GOOGLE_SIGN_IN = 100
    private val callBackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {

        Thread.sleep(2000)
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        //Eventos personalizados a google analytics
        val analytics:FirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        bundle.putString("message", "Integracion de firebase completa")
        analytics.logEvent("InitScreen", bundle)

        //Remote config
        val configSetting = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 60
        }
        val firebaseConfig : FirebaseRemoteConfig = Firebase.remoteConfig
        firebaseConfig.setConfigSettingsAsync(configSetting)
        firebaseConfig.setDefaultsAsync(mapOf("show_error_button" to false, "error_button_text" to "Forzar error"))

        //Setup
        notification()
        setup()
        session()
    }

    private fun notification() {

        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener{
            it.result?.token?.let {
                println("Este es el token del dispositivo: ${it}")
            }
        }

        //Temas(Topics)
        FirebaseMessaging.getInstance().subscribeToTopic("tutorial")

        //Recuperar informacion
        val url = intent.getStringExtra("url")
        url?.let {
            println("Ha llegado informacion en una push: ${it}")
        }
    }

    override fun onStart() {
        super.onStart()

        authLayout.visibility = View.VISIBLE
    }

    private fun session() {

        val prefs = getSharedPreferences("prefs_file", Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        if(email != null && provider != null){
            authLayout.visibility = View.INVISIBLE
            showHome(email, ProviderType.valueOf(provider))
        }
    }

    private fun setup() {

        title = "Autenticación"

        buttonSignUp.setOnClickListener {
            if (editTextEmail.text.isNotEmpty() && editTextPassword.text.isNotEmpty()){
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(editTextEmail.text.toString(),
                editTextPassword.text.toString()).addOnCompleteListener{
                    if(it.isSuccessful){
                        showHome(it.result?.user?.email?:"", ProviderType.BASIC)
                    }else{
                        showAlert()
                    }
                }
            }
        }

        buttonLogin.setOnClickListener {
            if (editTextEmail.text.isNotEmpty() && editTextPassword.text.isNotEmpty()){
                FirebaseAuth.getInstance().signInWithEmailAndPassword(editTextEmail.text.toString(),
                    editTextPassword.text.toString()).addOnCompleteListener{
                    if(it.isSuccessful){
                        showHome(it.result?.user?.email?:"", ProviderType.BASIC)
                    }else{
                        showAlert()
                    }
                }
            }
        }

        buttonGoogle.setOnClickListener {

            //Configuracion

            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()

            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }

        buttonFacebook.setOnClickListener {

            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))

            LoginManager.getInstance().registerCallback(callBackManager,
                    object : FacebookCallback<LoginResult> {

                        override fun onSuccess(result: LoginResult?) {
                            result?.let {
                                val token = it.accessToken

                                val credential = FacebookAuthProvider.getCredential(token.token)
                                FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener{
                                    if(it.isSuccessful){
                                        showHome(it.result?.user?.email ?: "", ProviderType.FACEBOOK)
                                    }else{
                                        showAlert()
                                        LoginManager.getInstance().logOut()
                                    }
                                }
                            }
                        }

                        override fun onCancel() {

                        }

                        override fun onError(error: FacebookException?) {
                            showAlert()
                        }
                    })
        }
    }

    private fun showAlert(){

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error en la autenticacion")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String, provider: ProviderType){

        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        callBackManager.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GOOGLE_SIGN_IN){

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            try{
                if(account != null){
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener{
                        if(it.isSuccessful){
                            showHome(account.email ?: "", ProviderType.GOOGLE)
                        }else{
                            showAlert()
                        }
                    }
                }
            }catch (e: ApiException){
                showAlert()
            }

        }
    }
}