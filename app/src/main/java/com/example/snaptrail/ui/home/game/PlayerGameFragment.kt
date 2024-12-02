package com.example.snaptrail.ui.home.game

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.snaptrail.MainActivity

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameId = arguments?.getString("gameId") ?: ""

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnTakePhoto.setOnClickListener {
            // Camera stuff later
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
        // Update the map if needed
        if (::googleMap.isInitialized) {
            googleMap.clear()
            setupMap()
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }
}
