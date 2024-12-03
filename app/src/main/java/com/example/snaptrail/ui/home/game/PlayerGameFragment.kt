package com.example.snaptrail.ui.home.game

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentPlayerGameBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.util.Log
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.snaptrail.MainActivity
import com.example.snaptrail.ui.home.create.locations.LocationData
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.maps.model.LatLng
import android.location.Location
import android.widget.LinearLayout
import com.google.firebase.firestore.FieldValue

class PlayerGameFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentPlayerGameBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private var gameId: String = ""
    private var gameModel: GameModel? = null
    private var listenerRegistration: ListenerRegistration? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap

    private val locationPoints = mutableMapOf<String, Int>()

    private val completedLocations = mutableSetOf<String>()

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
        _binding = FragmentPlayerGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // Get current location when photo is taken
            getCurrentLocation { location ->
                checkLocationProximity(location)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameId = arguments?.getString("gameId") ?: ""

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnTakePhoto.setOnClickListener {
            // Request camera permission if not granted
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            } else {
                cameraLauncher.launch(null)
            }
        }

        binding.btnLeaveGame.setOnClickListener {
            showLeaveGameConfirmation()
        }

        // Listen for changes in the game data
        listenerRegistration = db.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PlayerGameFragment", "Error fetching game data: ${error.message}", error)
                    Toast.makeText(context, "Error fetching game data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (snapshot.exists()) {
                        gameModel = snapshot.toObject(GameModel::class.java)
                        if (gameModel != null) {
                            updateUI()

                            // Check if game has ended
                            if (gameModel?.gameEnded == true) {
                                navigateToLeaderboard()
                            }
                        }
                    } else {
                        // Game document has been deleted
                        Log.d("PlayerGameFragment", "Game document does not exist. Navigating back to home.")
                        Toast.makeText(context, "Game has been ended by the host.", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.nav_home)
                    }
                }
            }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setupMap()
    }

    private fun setupMap() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        googleMap.isMyLocationEnabled = true

        // Display the player's current location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }

        // Display the locations with 250m radius circles
        gameModel?.let { model ->
            model.locations.forEach { locationData ->
                val latLng = LatLng(locationData.latitude, locationData.longitude)
                googleMap.addCircle(
                    CircleOptions()
                        .center(latLng)
                        .radius(250.0) // 250 meters
                        .strokeColor(0x5500ff00)
                        .fillColor(0x2200ff00)
                )
            }
        }
    }

    private fun updateUI() {
        // Update comments
        gameModel?.comments?.let { comments ->
            binding.tvComments.text = comments
        }

        // Populate location entries
        binding.locationsContainer.removeAllViews()
        gameModel?.locations?.forEach { location ->
            addLocationEntry(location)
        }

        // Update the map if needed
        if (::googleMap.isInitialized) {
            googleMap.clear()
            setupMap()
        }
    }

    private fun addLocationEntry(location: LocationData) {
        val locationEntryLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Points TextView
        val pointsTextView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
            text = locationPoints[location.placeId]?.let { "$it pts" } ?: ""
            setTextColor(Color.BLUE)
        }

        // Location Name TextView
        val locationTextView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = location.name
            setPadding(16, 16, 16, 16)
            setTextColor(if (completedLocations.contains(location.placeId))
                Color.GREEN else Color.BLACK)
            setBackgroundResource(R.drawable.location_entry_background)
        }

        locationEntryLayout.addView(pointsTextView)
        locationEntryLayout.addView(locationTextView)

        binding.locationsContainer.addView(locationEntryLayout)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMap()
        } else {
            Toast.makeText(context, "Location permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLeaveGameConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Leave Trail")
        builder.setMessage("Are you sure you want to leave the trail? You will not be able to rejoin")
        builder.setPositiveButton("Confirm") { _, _ ->
            leaveGame()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
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
                        model.playerProgress.remove(userId)
                        db.collection("games").document(gameId)
                            .set(model)
                            .addOnSuccessListener {
                                Toast.makeText(context, "You have left the game.", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.nav_home)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to leave game: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(context, "Game does not exist.", Toast.LENGTH_SHORT).show()
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

    private fun navigateToLeaderboard() {
        val bundle = Bundle().apply {
            putString("gameId", gameId)
        }
        findNavController().navigate(R.id.action_playerGameFragment_to_playerCompleteFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }

    private fun getCurrentLocation(callback: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { callback(it) }
        }
    }

    private fun checkLocationProximity(currentLocation: Location) {
        gameModel?.locations?.forEach { savedLocation ->
            val savedLatLng = LatLng(savedLocation.latitude, savedLocation.longitude)
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                savedLatLng.latitude,
                savedLatLng.longitude,
                results
            )

            if (results[0] <= 200) {
                val points = calculateLocationPoints(results[0])

                // Only award points if not already scored for this location
                if (!locationPoints.containsKey(savedLocation.placeId)) {
                    locationPoints[savedLocation.placeId] = points
                    Toast.makeText(context, "Found location! Earned $points points", Toast.LENGTH_SHORT).show()
                    completedLocations.add(savedLocation.placeId)
                    updatePlayerProgress(savedLocation.placeId, points)
                    updateLocationEntryColor(savedLocation.placeId)

                    if (completedLocations.size == gameModel?.locations?.size) {
                        userId?.let { currentUserId ->
                            db.collection("games").document(gameId)
                                .update("completedPlayers", FieldValue.arrayUnion(currentUserId))
                                .addOnSuccessListener {
                                    Log.d("PlayerGameFragment", "Player added to completedPlayers")
                                    // Navigate to leaderboard
                                    val bundle = Bundle().apply {
                                        putString("gameId", gameId)
                                    }
                                    findNavController().navigate(R.id.action_playerGameFragment_to_playerCompleteFragment, bundle)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("PlayerGameFragment", "Failed to update completedPlayers", e)
                                }
                        }
                    }
                    return
                }
            }
        }

        // No location within 200 meters
        Toast.makeText(context, "Nope! Not quite the location!", Toast.LENGTH_SHORT).show()
    }

    private fun updatePlayerProgress(placeId: String, points: Int) {
        userId?.let { currentUserId ->
            gameModel?.let { model ->
                // Increment player's progress and total points
                val currentProgress = model.playerProgress.getOrDefault(currentUserId, 0)
                val currentPoints = model.playerPoints.getOrDefault(currentUserId, 0)

                model.playerProgress[currentUserId] = currentProgress + 1
                model.playerPoints[currentUserId] = currentPoints + points

                // Update the game document in Firestore
                db.collection("games").document(gameId)
                    .update(
                        mapOf(
                            "playerProgress" to model.playerProgress,
                            "playerPoints" to model.playerPoints
                        )
                    )
                    .addOnSuccessListener {
                        Log.d("PlayerGameFragment", "Player progress and points updated")
                        // After updating the game, update the user's successfulPlacesFound
                        incrementSuccessfulPlacesFound(currentUserId)
                    }
                    .addOnFailureListener { e ->
                        Log.e("PlayerGameFragment", "Failed to update progress", e)
                    }
            }
        }
    }

    private fun incrementSuccessfulPlacesFound(userId: String) {
        val userDocRef = db.collection("users").document(userId)

        userDocRef.update("successfulPlacesFound", FieldValue.increment(1))
            .addOnSuccessListener {
                Log.d("PlayerGameFragment", "successfulPlacesFound incremented")
            }
            .addOnFailureListener { e ->
                Log.e("PlayerGameFragment", "Failed to increment successfulPlacesFound", e)
            }
    }

    private fun updateLocationEntryColor(placeId: String) {
        for (i in 0 until binding.locationsContainer.childCount) {
            val entryView = binding.locationsContainer.getChildAt(i) as? TextView
            val location = gameModel?.locations?.get(i)
            if (location?.placeId == placeId) {
                entryView?.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                break
            }
        }
    }

    private fun calculateLocationPoints(distance: Float): Int {
        return when {
            distance <= 50 -> 1000
            distance <= 200 -> {
                // Linear interpolation between 500-1000 points from 50-200 meters
                val interpolatedPoints = 1000 - ((distance - 50) / 150 * 500).toInt()
                interpolatedPoints.coerceIn(500, 1000)
            }
            else -> 0
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }
}