package com.example.snaptrail.ui.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.ViewModelProvider
import com.example.snaptrail.R
import com.example.snaptrail.databinding.FragmentGalleryBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity.LOCATION_SERVICE
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import org.xml.sax.ext.Locator2Impl

class GalleryFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentGalleryBinding? = null
    private lateinit var mMap:GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var locationRequest:LocationRequest
    private val PERMISSION_REQUEST_CODE = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
                val latLng = LatLng(location.latitude,location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
            }
        }

    }
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT < 23) return
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
        else{
//            initLocationManager()
            startLocationUpdates()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        // Set an initial camera position (for testing purposes)
//        val testLocation = LatLng(37.7749, -122.4194) // San Francisco coordinates
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(testLocation, 12f))
        println("xd:Hello here on mapReady")
//        checkPermission()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        println("xd:Hello hi from request permissions")
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        fusedLocationClient.removeLocationUpdates(locationCallback())
    }
}

//    private fun initLocationManager() {
//        try {
//            locationManager = requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager
//
//            if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return
//
//            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
//            if (location != null){
//                onLocationChanged(location)
//            }
//
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0L,0f,activity)
//
//        } catch (e: SecurityException) {
//        }
//    }


//    override fun onLocationChanged(location: Location) {
//        val lat = location.latitude
//        val long = location.longitude
//        val latLng = LatLng(lat,long)
//
//        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,17f)
//        mMap.animateCamera(cameraUpdate)
//        println("xd:Hello here")
//    }