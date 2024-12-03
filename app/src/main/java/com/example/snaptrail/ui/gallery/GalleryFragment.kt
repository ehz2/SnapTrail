package com.example.snaptrail.ui.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentGalleryBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.properties.Delegates

class GalleryFragment : Fragment(), OnMapReadyCallback {
    companion object{
        var DIFFICULITY_KEY = "DifficultyKey"
        var LONGITUDE_KEY = "LongitudeKey"
        var LATITUDE_KEY = "LatitudeKey"
    }

    private val TAG = "xd:"
    private var locationObtained = false
    private var _binding: FragmentGalleryBinding? = null
    private var userMarkerLocation: Marker? = null

    private val PERMISSION_REQUEST_CODE = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude by Delegates.notNull<Double>()
    private var longitude by Delegates.notNull<Double>()
    private val firestore = Firebase.firestore

    //Use Activity Result Launcher for permission
    private lateinit var requestPermissionLauncher:ActivityResultLauncher<String>
    private lateinit var mMap:GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var locationRequest:LocationRequest
    private lateinit var challengeBtn:Button
    private lateinit var autoChallengeViewModel: AutoChallengeViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ){isGranted: Boolean ->
            if (isGranted) {
                startLocationUpdates()
            } else {
                // Handle permission denial if needed
                println("xd: Permission denied")
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)
        autoChallengeViewModel = ViewModelProvider(requireActivity()).get(AutoChallengeViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initializing map
        val mapFragment = childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Initialize location of client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        checkPermission()

        //Difficulity
        var difficulitySpinner:Spinner = binding.difficulty

        ArrayAdapter.createFromResource(
            requireActivity(),
            R.array.difficulity,
            android.R.layout.simple_spinner_item
        ).also{adapter->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            difficulitySpinner.adapter = adapter
        }
        //Join Challenge
        challengeBtn = binding.challenge

        challengeBtn.setOnClickListener(){
            val difficulty = difficulitySpinner.getSelectedItem().toString()

            //Launch New Fragment
            val challengeFragment = AutoChallengeFragment()

            //Share difficulty
            val bundle = Bundle()
            bundle.putString(DIFFICULITY_KEY,difficulty)
            bundle.putDouble(LATITUDE_KEY,latitude)
            bundle.putDouble(LONGITUDE_KEY,longitude)
            challengeFragment.arguments = bundle

            //Clear the entries in the view Model because the user wants to start a new trial
            autoChallengeViewModel.displayList.clear()
            autoChallengeViewModel.placesList.clear()
            autoChallengeViewModel.successfulPlaces.clear()

            val navController = findNavController()
            navController.navigate(R.id.nav_autoChallenge, bundle)
            Toast.makeText(context,"JOIN CHALLENGE WITH DIFFICULTY - ${difficulty}",Toast.LENGTH_SHORT).show()
        }
        Log.e(TAG,"Value of locationObtained is ${locationObtained}")
        if(!locationObtained) {
            challengeBtn.visibility = View.GONE
        }

        //Check for number of places found and update database
        parentFragmentManager.setFragmentResultListener(AutoChallengeFragment.TOTAL_PLACES_BUNDLE_KEY,
            viewLifecycleOwner){_, bundle ->
            val totalPlaces = bundle.getInt(AutoChallengeFragment.TOTAL_PLACES_KEY)
            //Update the Firestore
            val user = Firebase.auth.currentUser
            user?.let{
                updateSuccesfulPlacesStat(it.uid,totalPlaces.toLong())
            }
        }
        //Check for difficulty mode completed and update database
        parentFragmentManager.setFragmentResultListener(AutoChallengeFragment.DIFFICULTY_MODE_BUNDLE,
            viewLifecycleOwner){_,bundle->
            val numPlaces = bundle.getInt(AutoChallengeFragment.DIFFICULTY_MODE)
            val user = Firebase.auth.currentUser
            user?.let{
                updateDifficultyStat(it.uid,numPlaces)
            }
        }

        return root
    }


    private fun updateDifficultyStat(uid: String,numPlaces:Int){
        val userStatsDoc = firestore.collection("users").document(uid)
        when(numPlaces){
            5 ->{
                userStatsDoc.update("easy",
                    FieldValue.increment(1.toLong()))
            }
            8->{
                userStatsDoc.update("medium",
                    FieldValue.increment(1.toLong()))
            }
            12->{
                userStatsDoc.update("hard",
                    FieldValue.increment(1.toLong()))
            }
        }
    }

    private fun updateSuccesfulPlacesStat(uid:String,totalPlaces:Long){
        val userStatsDoc = firestore.collection("users").document(uid)
        userStatsDoc.update("successfulPlacesFound",
            FieldValue.increment(totalPlaces)).addOnSuccessListener {
            Log.d(TAG, "SuccessfulPlaces updated successfully by $totalPlaces")
        }.addOnFailureListener {exception->
            Log.e(TAG, "Failed to update SuccessfulPlaces: ${exception.message}")
        }
    }

    private fun startLocationUpdates(){
        //If permission granted request current location
        println("xd:Hello location Updates")

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback(),
                requireActivity().mainLooper
            )
        }
    }
    private fun locationCallback()= object :LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations){
                latitude = location.latitude
                longitude = location.longitude
//                println("xd:${location.latitude}, ${location.longitude}")
                val latLng = LatLng(location.latitude,location.longitude)
                if(userMarkerLocation == null){
                    //create a new marker
                    userMarkerLocation = mMap.addMarker(MarkerOptions().position(latLng)
                        .title("You are here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                }
                else{
                    userMarkerLocation?.position = latLng
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                if (!locationObtained) {
                    locationObtained = true
                    challengeBtn.visibility = View.VISIBLE
                }
            }
        }
    }
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT < 23) return
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startLocationUpdates()
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        fusedLocationClient.removeLocationUpdates(locationCallback())
        Log.e(TAG,"On destroy view is called")
    }

}