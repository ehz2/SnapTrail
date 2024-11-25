package com.example.snaptrail.loginpage

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snaptrail.MainActivity
import com.example.snaptrail.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore // Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()

                // Check each requirement and update the color accordingly
                binding.requirementLength.setTextColor(
                    if (password.length >= 8) Color.GREEN else Color.RED
                )
                binding.requirementUppercase.setTextColor(
                    if (password.any { it.isUpperCase() }) Color.GREEN else Color.RED
                )
                binding.requirementLowercase.setTextColor(
                    if (password.any { it.isLowerCase() }) Color.GREEN else Color.RED
                )
                binding.requirementNumber.setTextColor(
                    if (password.any { it.isDigit() }) Color.GREEN else Color.RED
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.continueBtn.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val username = binding.username.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, save username to Firestore
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        user?.let {
                            saveUsernameToFirestore(it.uid, username)
                        }
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Registration failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }

        binding.move.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveUsernameToFirestore(uid: String, username: String) {
        val userDoc = firestore.collection("users").document(uid)
        val userData = mapOf("username" to username)

        userDoc.set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "Username saved successfully")
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save username: ${exception.message}")
                Toast.makeText(this, "Failed to save username", Toast.LENGTH_SHORT).show()
            }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
