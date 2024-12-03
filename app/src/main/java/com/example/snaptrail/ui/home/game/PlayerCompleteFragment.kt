package com.example.snaptrail.ui.home.game

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentPlayerCompleteBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PlayerCompleteFragment : Fragment() {
    private var _binding: FragmentPlayerCompleteBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private var gameId: String = ""

    private var leaderboardListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlayerCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfirm.visibility = View.GONE

        gameId = arguments?.getString("gameId") ?: ""

        // Fetch and display leaderboard
        fetchLeaderboard()

        binding.btnConfirm.setOnClickListener {
            // End game and return to home
            endGame()
        }
    }

    private fun fetchLeaderboard() {
        leaderboardListener = db.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PlayerCompleteFragment", "Error fetching leaderboard: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val gameModel = snapshot.toObject(GameModel::class.java)
                    gameModel?.let { model ->
                        // Exclude the host from the players list
                        val nonHostPlayers = model.players.filter { it != model.hostId }

                        // Sort players by points in descending order
                        val sortedPlayers = nonHostPlayers
                            .mapNotNull { playerId ->
                                val playerName = model.playerNames[playerId] ?: "Unknown"
                                val points = model.playerPoints[playerId] ?: 0
                                PlayerLeaderboardItem(playerName, points)
                            }
                            .sortedByDescending { it.points }

                        // Populate leaderboard
                        val adapter = LeaderboardAdapter(requireContext(), sortedPlayers)
                        binding.lvLeaderboard.adapter = adapter

                        // Control visibility of "Confirm" button
                        if (model.gameEnded) {
                            binding.btnConfirm.visibility = View.VISIBLE
                        } else {
                            binding.btnConfirm.visibility = View.GONE
                        }
                    }
                }
            }
    }

    private fun endGame() {
        db.collection("games").document(gameId)
            .get()
            .addOnSuccessListener { document ->
                val gameModel = document.toObject(GameModel::class.java)
                if (gameModel != null && gameModel.gameEnded) {
                    // Delete the game document
                    db.collection("games").document(gameId)
                        .delete()
                        .addOnSuccessListener {
                            findNavController().navigate(R.id.nav_home)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to delete game: ${e.message}", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.nav_home)
                        }
                } else {
                    Toast.makeText(context, "Cannot end game until all players have finished.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching game data: ${e.message}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_home)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        leaderboardListener?.remove()
        _binding = null
    }
}

