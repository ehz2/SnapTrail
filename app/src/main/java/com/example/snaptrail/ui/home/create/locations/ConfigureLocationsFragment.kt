package com.example.snaptrail.ui.home.create.locations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale
import java.util.UUID
import androidx.appcompat.app.AlertDialog
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentConfigureLocationsBinding

class ConfigureLocationsFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentConfigureLocationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locations = mutableListOf<LocationData>()
    private val markers = mutableListOf<Marker>()

    private companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val DEFAULT_ZOOM = 15f
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigureLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupButtons()
        loadSavedLocations()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setupMap()

        // Set long click listener to add locations
        googleMap.setOnMapLongClickListener { latLng ->
            showAddLocationDialog(latLng)
        }

        // Add markers for saved locations
        locations.forEach { locationData ->
            addMarkerForLocation(locationData)
        }
    }

    private fun loadSavedLocations() {
        val sharedPrefs = requireContext().getSharedPreferences("Locations", Context.MODE_PRIVATE)
        val locationsJson = sharedPrefs.getString("saved_locations", null)

        if (!locationsJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<LocationData>>() {}.type
            val savedLocations = Gson().fromJson<List<LocationData>>(locationsJson, type)
            locations.clear()
            locations.addAll(savedLocations)
        }
    }

    private fun addMarkerForLocation(locationData: LocationData): Marker? {
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(locationData.latitude, locationData.longitude))
                .title(locationData.name)
                .snippet(locationData.address)
        )
        marker?.let { markers.add(it) }
        return marker
    }

    private fun setupMap() {
        if (checkLocationPermission()) {
            googleMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM))
                }
            }
        }
    }

    private fun showAddLocationDialog(latLng: LatLng) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Location")
            .setView(R.layout.dialog_add_location)
            .setPositiveButton("Add") { dialog, _ ->
                val dialogView = (dialog as AlertDialog).findViewById<TextInputEditText>(R.id.locationNameInput)
                val locationName = dialogView?.text.toString()

                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                val address = addresses?.firstOrNull()?.getAddressLine(0) ?: ""

                val locationData = LocationData(
                    placeId = UUID.randomUUID().toString(),
                    name = locationName,
                    address = address,
                    latitude = latLng.latitude,
                    longitude = latLng.longitude
                )

                addLocation(locationData)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun addLocation(locationData: LocationData) {
        locations.add(locationData)
        addMarkerForLocation(locationData)
    }

    private fun setupButtons() {
        binding.buttonSave.setOnClickListener {
            val locationsArray = locations.toTypedArray()
            val action = ConfigureLocationsFragmentDirections
                .actionConfigureLocationsFragmentToReviewLocationsFragment(locationsArray)
            findNavController().navigate(action)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupMap()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload locations and update markers when returning to this fragment
        loadSavedLocations()
        if (::googleMap.isInitialized) {
            // Clear existing markers
            markers.forEach { it.remove() }
            markers.clear()
            // Add markers for current locations
            locations.forEach { addMarkerForLocation(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}