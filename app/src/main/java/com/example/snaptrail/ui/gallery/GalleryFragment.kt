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
import android.widget.ArrayAdapter
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

class GalleryFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentGalleryBinding? = null
    private var userMarkerLocation: Marker? = null

    private lateinit var mMap:GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var locationRequest:LocationRequest
    private val PERMISSION_REQUEST_CODE = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //Use Activity Result Launcher for permission
    private lateinit var requestPermissionLauncher:ActivityResultLauncher<String>

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
        var challenge = binding.challenge
        challenge.setOnClickListener(){
            val difficulty = difficulitySpinner.getSelectedItem().toString()

            //Launch New Fragment
            val challengeFragment = AutoChallengeFragment()

            //Share difficulty
            val bundle = Bundle()
            bundle.putString("DifficultyKey",difficulty)
            challengeFragment.arguments = bundle
            val navController = findNavController()
            navController.navigate(R.id.nav_autoChallenge, bundle)
            Toast.makeText(context,"JOIN CHALLENGE WITH DIFFICULTY - ${difficulty}",Toast.LENGTH_SHORT).show()
        }
        return root
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
                println("xd:${location.latitude}, ${location.longitude}")
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
    }
}