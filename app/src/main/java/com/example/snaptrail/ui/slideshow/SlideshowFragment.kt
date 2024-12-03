package com.example.snaptrail.ui.slideshow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.snaptrail.databinding.FragmentSlideshowBinding
import com.example.snaptrail.loginpage.LoginActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    private val firestore = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val rootView = binding.root

        val user = Firebase.auth.currentUser

        // Show user name and stats in the textViews
        user?.let {
            loadUsernameFromFirestore(it.uid)
            loadPlayerStatistics(it.uid)
        }

        // Set up logout button
        binding.logoutButton.setOnClickListener {
            Firebase.auth.signOut()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }

        return rootView
    }

    private fun loadUsernameFromFirestore(uid: String) {
        val userDoc = firestore.collection("users").document(uid)
        userDoc.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val savedUsername = document.getString("username")
                    if (!savedUsername.isNullOrEmpty()) {
                        binding.user.text = "Hello $savedUsername"
                    } else {
                        binding.user.text = "Hello"
                        Toast.makeText(requireContext(), "Username not found in Firestore.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    binding.user.text = "Hello"
                    Toast.makeText(requireContext(), "No user data found in Firestore.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                binding.user.text = "Hello"
                Toast.makeText(requireContext(), "Failed to load username: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPlayerStatistics(uid: String) {
        val userDoc = firestore.collection("users").document(uid)
        userDoc.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Fetch values from Firestore
                    val easyTrails = document.getLong("easy") ?: 0
                    val mediumTrails = document.getLong("medium") ?: 0
                    val hardTrails = document.getLong("hard") ?: 0
                    val successfulPlaces = document.getLong("successfulPlacesFound") ?: 0

                    // Update text views
                    binding.easyTrailsTextView.text = "Easy trails: $easyTrails"
                    binding.mediumTrailsTextView.text = "Medium trails: $mediumTrails"
                    binding.hardTrailsTestView.text = "Hard trails: $hardTrails"

                    val totalTrails = easyTrails + mediumTrails + hardTrails
                    binding.totalTrailsCompletedTextView.text = "Total trails completed: $totalTrails"
                    binding.numberOfLocationsFound.text = "Number of locations found: $successfulPlaces"
                } else {
                    Toast.makeText(requireContext(), "No user data found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to load statistics: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
