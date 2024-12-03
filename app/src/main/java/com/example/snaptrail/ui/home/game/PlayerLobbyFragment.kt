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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.snaptrail.MainActivity
import com.example.snaptrail.databinding.FragmentPlayerLobbyBinding
import com.google.firebase.firestore.FieldValue

class PlayerLobbyFragment : Fragment() {

    private var _binding: FragmentPlayerLobbyBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var gameId: String = ""
    private var gameModel: GameModel? = null
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var gameViewModel: GameViewModel
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
        _binding = FragmentPlayerLobbyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gameViewModel = ViewModelProvider(requireActivity()).get(GameViewModel::class.java)
        if(gameViewModel.gameId!=null){
            gameId = gameViewModel.gameId!!
        }
        gameId = arguments?.getString("gameId") ?: ""

        // Set the game code
        binding.tvGameCode.text = "Joined Trail: $gameId"

        // Fetch the player's username and add them to the game
        if (userId != null) {
            val userDocRef = db.collection("users").document(userId)
            userDocRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val username = documentSnapshot.getString("username") ?: "Unknown"

                    // Add player to the game with username
                    addPlayerToGame(gameId, userId, username)
                } else {
                    Toast.makeText(context, "User data not found.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.nav_home)
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_home)
            }
        } else {
            Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_home)
        }

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
                        try {
                            gameModel = snapshot.toObject(GameModel::class.java)
                            if (gameModel != null) {
                                updateUI()

                                // Check if game has started
                                if (gameModel!!.gameStatus == "ONGOING") {
                                    // Navigate to PlayerGameFragment
                                    val action = PlayerLobbyFragmentDirections
                                        .actionPlayerLobbyFragmentToPlayerGameFragment(gameId)
                                    findNavController().navigate(action)
                                }

                            } else {
                                Log.e("PlayerLobbyFragment", "Failed to parse GameModel.")
                                Toast.makeText(context, "Error parsing game data.", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.nav_home)
                            }
                        } catch (e: Exception) {
                            Log.e("PlayerLobbyFragment", "Exception during deserialization: ${e.message}", e)
                            Toast.makeText(context, "Error parsing game data: ${e.message}", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.nav_home)
                        }
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

    private fun addPlayerToGame(gameId: String, userId: String, username: String) {
        val gameDocRef = db.collection("games").document(gameId)

        gameDocRef.update(
            "players", FieldValue.arrayUnion(userId),
            "playerNames.$userId", username
        ).addOnSuccessListener {
            // Player added successfully
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to join game: ${e.message}", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_home)
        }
    }

    private fun updateUI() {
        gameModel?.let { model ->
            // Update the trail name
            binding.tvTrailName.text = model.trailName
            // Update comments
            binding.tvComments.text = model.comments
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
        // Remove the snapshot listener
        listenerRegistration?.remove()
        listenerRegistration = null
        _binding = null
    }
}
