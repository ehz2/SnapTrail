package com.example.snaptrail.ui.home.create

import android.content.Context
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
import com.example.snaptrail.ui.home.create.locations.LocationData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CreateTrailFragment : Fragment() {

    private var _binding: FragmentCreateTrailBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var selectedLocations: List<LocationData> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateTrailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load selected locations
        loadSelectedLocations()

        binding.btnConfigureLocations.setOnClickListener {
            findNavController().navigate(R.id.action_createTrailFragment_to_configureLocationsFragment)
        }

        binding.btnSaveTrail.setOnClickListener {
            createGame()
        }
    }

    private fun loadSelectedLocations() {
        val sharedPrefs = requireContext().getSharedPreferences("Locations", Context.MODE_PRIVATE)
        val locationsJson = sharedPrefs.getString("saved_locations", null)

        if (!locationsJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<LocationData>>() {}.type
            selectedLocations = Gson().fromJson(locationsJson, type)
        }
    }

    private fun createGame() {
        val trailName = binding.editTrailName.text.toString().trim()
        val maxPlayersText = binding.editNumberOfPlayers.text.toString().trim()
        val comments = binding.editComment.text.toString().trim()

        // Validate inputs
        if (trailName.isEmpty()) {
            binding.editTrailName.error = "Please enter a trail name"
            return
        }

        val maxPlayers = if (maxPlayersText.isNotEmpty()) {
            maxPlayersText.toIntOrNull() ?: run {
                binding.editNumberOfPlayers.error = "Please enter a valid number"
                return
            }
        } else {
            null
        }

        if (selectedLocations.isEmpty()) {
            Toast.makeText(context, "Please select at least one location", Toast.LENGTH_SHORT).show()
            return
        }

        // Generate a game code
        val gameId = (1000..9999).random().toString()
        // Get user's email or display name
        val userName = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"

        // Create a GameModel
        val gameModel = GameModel(
            gameId = gameId,
            gameStatus = "CREATED",
            hostId = userId!!,
            trailName = trailName,
            maxPlayers = maxPlayers,
            comments = comments,
            locations = selectedLocations,
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

    override fun onResume() {
        super.onResume()
        // Reload selected locations in case they were updated
        loadSelectedLocations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}