package com.example.mojar.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mojar.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/** Activity that handles User Authentication using Google Sign-In and Firebase.*/
class LoginActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Configure Google Sign-In to request the user's ID, email, and basic profile.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("779867640167-65lbogf0l970rffpf25hmgc2ktff86i7.apps.googleusercontent.com")
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val btnGoogleSignIn = findViewById<SignInButton>(R.id.btnGoogleSignIn)
        btnGoogleSignIn.setOnClickListener {
            signIn()
        }
    }


    //Triggers the Google Sign-In Intent.
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!

            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Log.w("MojarLogin", "Google sign in failed", e)
        }
    }


    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        val auth = FirebaseAuth.getInstance()

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Create or update the user's profile in Firestore
                    if (user != null) {
                        val userData = hashMapOf(
                            "uid" to user.uid,
                            "displayName" to user.displayName,
                            "email" to user.email
                        )

                        db.collection("users").document(user.uid)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener {
                                Log.d("MojarLogin", "User profile synced to Firestore")
                                checkUserHousehold()
                            }
                    }
                } else {
                    Toast.makeText(this, "Firebase login failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserHousehold() {
        val intent = Intent(this, HouseholdSelectionActivity::class.java)
        startActivity(intent)
        finish() // Close LoginActivity so the user cannot navigate back to it
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}