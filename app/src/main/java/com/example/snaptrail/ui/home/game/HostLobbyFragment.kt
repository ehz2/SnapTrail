package com.example.snaptrail.ui.home.game

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentHostLobbyBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.util.Log

class HostLobbyFragment : Fragment() {

    private var _binding: FragmentHostLobbyBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    private var gameId: String = ""
    private var gameModel: GameModel? = null
    private lateinit var playersAdapter: ArrayAdapter<String>
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentHostLobbyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameId = arguments?.getString("gameId") ?: ""

        binding.tvGameCode.text = "Join Code: $gameId"

        // Initialize the players list adapter
        playersAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.lvPlayers.adapter = playersAdapter

        // Listen for changes in the game data
        listenerRegistration = db.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HostLobbyFragment", "Error fetching game data: ${error.message}", error)
                    Toast.makeText(context, "Error fetching game data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (snapshot.exists()) {
                        gameModel = snapshot.toObject(GameModel::class.java)
                        updateUI()
                    } else {
                        Log.d("HostLobbyFragment", "Game document does not exist. Navigating back to home.")
                        Toast.makeText(context, "Game has been deleted.", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.nav_home)
                    }
                }
            }

        binding.btnDeleteGame.setOnClickListener {
            showDeleteGameConfirmation()
        }
    }

    private fun showDeleteGameConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Trail")
        builder.setMessage("Are you sure you want to delete this trail?")
        builder.setPositiveButton("Confirm") { dialog, which ->
            deleteGame()
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun updateUI() {
        gameModel?.let { model ->
            // Update the list of players
            val playerNames = model.playerNames.values.toList()
            playersAdapter.clear()
            playersAdapter.addAll(playerNames)
            playersAdapter.notifyDataSetChanged()
        }
    }

    private fun deleteGame() {
        Log.d("HostLobbyFragment", "Attempting to delete game document: $gameId")
        db.collection("games").document(gameId)
            .delete()
            .addOnSuccessListener {
                Log.d("HostLobbyFragment", "Game document deleted successfully.")
                Toast.makeText(context, "Trail deleted.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_home)
            }
            .addOnFailureListener { e ->
                Log.e("HostLobbyFragment", "Failed to delete game document: ${e.message}", e)
                Toast.makeText(context, "Failed to delete game: ${e.message}", Toast.LENGTH_SHORT).show()
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
