package com.example.snaptrail.ui.home.create

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentCreateTrailBinding
import com.example.snaptrail.ui.home.game.GameModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateTrailFragment : Fragment() {

    private var _binding: FragmentCreateTrailBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateTrailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfigureLocations.setOnClickListener {
            findNavController().navigate(R.id.action_createTrailFragment_to_configureLocationsFragment)
        }

        binding.btnSaveTrail.setOnClickListener {
            createGame()
        }
    }

    private fun createGame() {
        // Generate a game code
        val gameId = (1000..9999).random().toString()
        // Get user's email or display name
        val userName = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"

        // Create a GameModel
        val gameModel = GameModel(
            gameId = gameId,
            gameStatus = "CREATED",
            hostId = userId!!,
            players = mutableListOf(userId),
            playerNames = mutableMapOf(userId to userName)
        )
        // Save the game model to Firebase
        db.collection("games")
            .document(gameId)
            .set(gameModel)
            .addOnSuccessListener {
                // Navigate to HostLobbyFragment
                val bundle = Bundle()
                bundle.putString("gameId", gameId)
                findNavController().navigate(R.id.action_createTrailFragment_to_hostLobbyFragment, bundle)
            }
            .addOnFailureListener { e ->
                // Handle the error
                Toast.makeText(context, "Failed to create game: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}