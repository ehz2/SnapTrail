package com.example.snaptrail.ui.home.game

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentHostGameBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.snaptrail.MainActivity

class HostGameFragment : Fragment() {

    private var _binding: FragmentHostGameBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    private var gameId: String = ""
    private var gameModel: GameModel? = null
    private lateinit var playersAdapter: ArrayAdapter<String>
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Intercept back button press
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing to disable back button
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentHostGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameId = arguments?.getString("gameId") ?: ""

        // Initialize the players list adapter
        playersAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.lvPlayers.adapter = playersAdapter

        // Listen for changes in the game data
        listenerRegistration = db.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HostGameFragment", "Error fetching game data: ${error.message}", error)
                    Toast.makeText(context, "Error fetching game data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    gameModel = snapshot.toObject(GameModel::class.java)
                    updateUI()

                    // Check if game has ended
                    if (gameModel?.gameEnded == true) {
                        navigateToLeaderboard()
                    }
                } else {
                    // Game document has been deleted
                    Toast.makeText(context, "Game has been ended.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.nav_home)
                }
            }

        binding.btnEndGame.setOnClickListener {
            showEndGameConfirmation()
        }
    }

    private fun updateUI() {
        gameModel?.let { model ->
            binding.tvTrailName.text = model.trailName

            // Exclude the host from the players list
            val nonHostPlayers = model.players.filter { it != model.hostId }

            if (model.completedPlayers.size == nonHostPlayers.size && !model.gameEnded) {
                // Update 'gameEnded' in Firestore
                db.collection("games").document(gameId)
                    .update("gameEnded", true)
                    .addOnSuccessListener {
                        Log.d("HostGameFragment", "Game ended successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("HostGameFragment", "Failed to end game: ${e.message}", e)
                    }
            }

            // Include points in player progress list
            val playerProgressList = model.players
                .filter { it != model.hostId }
                .map { playerId ->
                    val playerName = model.playerNames[playerId] ?: "Unknown"
                    val progress = model.playerProgress[playerId] ?: 0
                    val points = model.playerPoints[playerId] ?: 0
                    val totalLocations = model.locations.size
                    "$playerName: $progress/$totalLocations (${points}pts)"
                }

            playersAdapter.clear()
            playersAdapter.addAll(playerProgressList)
            playersAdapter.notifyDataSetChanged()
        }
    }

    private fun showEndGameConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("End Trail")
        builder.setMessage("Are you sure you want to end the trail? This will end the trail for everyone!")
        builder.setPositiveButton("Confirm") { _, _ ->
            endGame()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun endGame() {
        // Delete the game document
        db.collection("games").document(gameId)
            .delete()
            .addOnSuccessListener {
                // Navigate the host back to home
                findNavController().navigate(R.id.nav_home)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to end game: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToLeaderboard() {
        val bundle = Bundle().apply {
            putString("gameId", gameId)
        }
        findNavController().navigate(R.id.action_hostGameFragment_to_playerCompleteFragment, bundle)
    }

    override fun onResume() {
        super.onResume()
        // Hide the up button
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        // Disable the navigation drawer
        (activity as MainActivity).lockDrawer()
    }

    override fun onPause() {
        super.onPause()
        // Re-enable the navigation drawer
        (activity as MainActivity).unlockDrawer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }
}
