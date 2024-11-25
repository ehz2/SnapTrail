package com.example.snaptrail.ui.home.game

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.util.Log
import com.example.snaptrail.databinding.FragmentPlayerLobbyBinding

class PlayerLobbyFragment : Fragment() {

    private var _binding: FragmentPlayerLobbyBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var gameId: String = ""
    private var gameModel: GameModel? = null
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentPlayerLobbyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameId = arguments?.getString("gameId") ?: ""

        binding.tvGameCode.text = "Joined Game: $gameId"

        // Listen for changes in the game data
        listenerRegistration = db.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PlayerLobbyFragment", "Error fetching game data: ${error.message}", error)
                    Toast.makeText(context, "Error fetching game data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (snapshot.exists()) {
                        Log.d("PlayerLobbyFragment", "Game document exists.")
                        gameModel = snapshot.toObject(GameModel::class.java)
                        // You can update UI based on gameModel if needed
                    } else {
                        Log.d("PlayerLobbyFragment", "Game document does not exist. Navigating back to home.")
                        Toast.makeText(context, "Game has been deleted by the host.", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.nav_home)
                    }
                } else {
                    Log.d("PlayerLobbyFragment", "Snapshot is null.")
                }
            }

        binding.btnLeaveGame.setOnClickListener {
            showLeaveGameConfirmation()
        }
    }

    private fun showLeaveGameConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Leave Trail")
        builder.setMessage("Are you sure you want to leave this trail?")
        builder.setPositiveButton("Confirm") { dialog, which ->
            leaveGame()
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun leaveGame() {
        if (userId == null) return

        db.collection("games").document(gameId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val gameModel = document.toObject(GameModel::class.java)
                    gameModel?.let { model ->
                        model.players.remove(userId)
                        model.playerNames.remove(userId)
                        db.collection("games").document(gameId)
                            .set(model)
                            .addOnSuccessListener {
                                Toast.makeText(context, "You have left the trail.", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.nav_home)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to leave game: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(context, "Trail does not exist.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.nav_home)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to leave game: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the snapshot listener
        listenerRegistration?.remove()
        listenerRegistration = null
        _binding = null
    }
}
