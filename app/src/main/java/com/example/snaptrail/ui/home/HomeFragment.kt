package com.example.snaptrail.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentHomeBinding
import com.example.snaptrail.ui.home.game.GameModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreateTrail.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createTrailFragment)
        }

        binding.btnJoinTrail.setOnClickListener {
            val gameCode = binding.joinCode.text.toString()
            if (gameCode.isEmpty()) {
                binding.joinCode.error = "Please enter the game code"
                return@setOnClickListener
            }
            joinGame(gameCode)
        }
    }

    private fun joinGame(gameCode: String) {
        db.collection("games")
            .document(gameCode)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val gameModel = document.toObject(GameModel::class.java)
                    gameModel?.let { model ->
                        if (!model.players.contains(userId)) {
                            // Get user's email or display name
                            val userName = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"
                            // Update the game model to add this player
                            model.players.add(userId!!)
                            model.playerNames[userId] = userName
                            db.collection("games")
                                .document(gameCode)
                                .set(model)
                                .addOnSuccessListener {
                                    // Navigate to PlayerLobbyFragment
                                    val bundle = Bundle()
                                    bundle.putString("gameId", gameCode)
                                    findNavController().navigate(R.id.action_homeFragment_to_playerLobbyFragment, bundle)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed to join game: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // Player has already joined, navigate to PlayerLobbyFragment
                            val bundle = Bundle()
                            bundle.putString("gameId", gameCode)
                            findNavController().navigate(R.id.action_homeFragment_to_playerLobbyFragment, bundle)
                        }
                    }
                } else {
                    binding.joinCode.error = "Invalid game code"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to join game: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
