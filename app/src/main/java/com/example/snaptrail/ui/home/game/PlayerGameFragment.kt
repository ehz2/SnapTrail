package com.example.snaptrail.ui.home.game

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.MainActivity
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentPlayerGameBinding
import com.example.snaptrail.ui.home.create.locations.LocationData
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

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
    private val circleCenters = mutableMapOf<String, LatLng>()

    private var gameStartTime: Long = 0L

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var userLocationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore circle centers if available
        savedInstanceState?.let { bundle ->
            val circleCentersList = bundle.getParcelableArrayList<Bundle>("circleCenters")
            circleCentersList?.forEach { item ->
                val placeId = item.getString("placeId") ?: return@forEach
                val latitude = item.getDouble("latitude")
                val longitude = item.getDouble("longitude")
                circleCenters[placeId] = LatLng(latitude, longitude)
            }
        }

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

        // Initialize location request
        locationRequest = LocationRequest.create().apply {
            interval = 5000 // Update interval in milliseconds
            fastestInterval = 2000 // Fastest update interval in milliseconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                if (location != null) {
                    // Update user's location on the map
                    updateUserLocationOnMap(location)
                }
            }
        }

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

        // Initialize game start time
        gameStartTime = System.currentTimeMillis()

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

        googleMap.isMyLocationEnabled = false

        // Display the locations with circles
        gameModel?.let { model ->
            val boundsBuilder = LatLngBounds.Builder()

            model.locations.forEach { locationData ->
                val locationLatLng = LatLng(locationData.latitude, locationData.longitude)

                // Generate or retrieve the circle center for this location
                val circleCenter = circleCenters.getOrPut(locationData.placeId) {
                    generateRandomPointAround(locationLatLng, 300.0)
                }

                googleMap.addCircle(
                    CircleOptions()
                        .center(circleCenter)
                        .radius(300.0) // 300 meters
                        .strokeColor(Color.argb(85, 0, 255, 0))
                        .fillColor(Color.argb(34, 0, 255, 0))
                )

                // Include the circle bounds
                boundsBuilder.include(circleCenter)
            }

            // Adjust the camera to include all circles
            val bounds = boundsBuilder.build()
            val padding = 100 // Adjust as needed
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            googleMap.moveCamera(cameraUpdate)
        }
    }

    private fun generateRandomPointAround(center: LatLng, radiusInMeters: Double): LatLng {
        val random = Random()

        // Convert radius from meters to degrees
        val radiusInDegrees = radiusInMeters / 111000f

        val u = random.nextDouble()
        val v = random.nextDouble()
        val w = radiusInDegrees * Math.sqrt(u)
        val t = 2 * Math.PI * v
        val x = w * Math.cos(t)
        val y = w * Math.sin(t)

        // Adjust the x-coordinate for the shrinking of the east-west distances
        val newX = x / Math.cos(Math.toRadians(center.latitude))

        val foundLatitude = center.latitude + y
        val foundLongitude = center.longitude + newX

        return LatLng(foundLatitude, foundLongitude)
    }

    private fun updateUserLocationOnMap(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)

        if (userLocationMarker == null) {
            // Add a marker for the user's location
            userLocationMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(currentLatLng)
                    .title("You")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        } else {
            // Move the marker to the new location
            userLocationMarker?.position = currentLatLng
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
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setupMap()
            startLocationUpdates()
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permission is required.", Toast.LENGTH_SHORT).show()
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
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        // Re-enable the navigation drawer
        (activity as MainActivity).unlockDrawer()
        stopLocationUpdates()
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
        stopLocationUpdates()
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
                // Only award points if not already scored for this location
                if (!locationPoints.containsKey(savedLocation.placeId)) {
                    val currentTime = System.currentTimeMillis()
                    val timeTakenMillis = currentTime - gameStartTime

                    // Calculate points based on time taken
                    val points = calculatePointsBasedOnTime(timeTakenMillis)

                    locationPoints[savedLocation.placeId] = points
                    Toast.makeText(context, "Found location! Earned $points points", Toast.LENGTH_SHORT).show()
                    completedLocations.add(savedLocation.placeId)
                    updatePlayerProgress(savedLocation.placeId, timeTakenMillis)
                    updateLocationEntryColor(savedLocation.placeId)

                    if (completedLocations.size == gameModel?.locations?.size) {
                        userId?.let { currentUserId ->
                            db.collection("games").document(gameId)
                                .update("completedPlayers", FieldValue.arrayUnion(currentUserId))
                                .addOnSuccessListener {
                                    Log.d("PlayerGameFragment", "Player added to completedPlayers")
                                    // Navigate to leaderboard
                                    navigateToLeaderboard()
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

    private fun updatePlayerProgress(placeId: String, timeTakenMillis: Long) {
        userId?.let { currentUserId ->
            gameModel?.let { model ->
                // Points have already been calculated based on time
                val points = locationPoints[placeId] ?: 0

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
            val locationEntryLayout = binding.locationsContainer.getChildAt(i) as? LinearLayout
            val locationTextView = locationEntryLayout?.getChildAt(1) as? TextView
            val location = gameModel?.locations?.get(i)
            if (location?.placeId == placeId) {
                locationTextView?.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                break
            }
        }
    }

    private fun calculatePointsBasedOnTime(timeTakenMillis: Long): Int {
        val minTime = 5 * 60 * 1000L       // 5 minutes in milliseconds
        val maxTime = 2 * 60 * 60 * 1000L  // 2 hours in milliseconds

        return when {
            timeTakenMillis <= minTime -> 1000
            timeTakenMillis >= maxTime -> 0
            else -> {
                val score = 1000 - ((timeTakenMillis - minTime).toDouble() / (maxTime - minTime) * 1000).toInt()
                score.coerceIn(0, 1000)
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val circleCentersList = circleCenters.map { (placeId, latLng) ->
            Bundle().apply {
                putString("placeId", placeId)
                putDouble("latitude", latLng.latitude)
                putDouble("longitude", latLng.longitude)
            }
        }
        outState.putParcelableArrayList("circleCenters", ArrayList(circleCentersList))
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }
}