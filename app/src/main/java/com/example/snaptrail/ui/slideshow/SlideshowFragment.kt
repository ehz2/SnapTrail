package com.example.snaptrail.ui.slideshow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.snaptrail.databinding.FragmentSlideshowBinding
import com.example.snaptrail.loginpage.LoginActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)

        // Show user currently logged in
        val user = Firebase.auth.currentUser
        user?.let {
            val email = it.email
            val displayUser: TextView = binding.user
            displayUser.text = "Hello $email"
        }

        // Set up the logout button to sign out the user and redirect to LoginActivity
        binding.logoutButton.setOnClickListener {
            Firebase.auth.signOut() // Sign out the user from Firebase
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()

            // Redirect to LoginActivity after signing out
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            activity?.finish() // Close MainActivity so the user cannot go back
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
