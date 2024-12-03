package com.example.snaptrail.ui.home.game

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentPlayerCompleteBinding
import com.google.firebase.firestore.FirebaseFirestore

class PlayerCompleteFragment : Fragment() {
    private var _binding: FragmentPlayerCompleteBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private var gameId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlayerCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameId = arguments?.getString("gameId") ?: ""

        // Fetch and display leaderboard
        fetchLeaderboard()

        binding.btnConfirm.setOnClickListener {
            // End game and return to home
            endGame()
        }
    }

    private fun fetchLeaderboard() {
        db.collection("games").document(gameId)
            .get()
            .addOnSuccessListener { document ->
                val gameModel = document.toObject(GameModel::class.java)
                gameModel?.let { model ->
                    // Sort players by points in descending order
                    val sortedPlayers = model.players
                        .mapNotNull { playerId ->
                            val playerName = model.playerNames[playerId] ?: "Unknown"
                            val points = model.playerPoints[playerId] ?: 0
                            PlayerLeaderboardItem(playerName, points)
                        }
                        .sortedByDescending { it.points }

                    // Populate leaderboard
                    val adapter = LeaderboardAdapter(requireContext(), sortedPlayers)
                    binding.lvLeaderboard.adapter = adapter
                }
            }
    }

    private fun endGame() {
        db.collection("games").document(gameId)
            .delete()
            .addOnSuccessListener {
                findNavController().navigate(R.id.nav_home)
            }
    }
}

