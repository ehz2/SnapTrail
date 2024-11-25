package com.example.snaptrail.ui.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.snaptrail.BuildConfig
import com.example.snaptrail.R
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Arrays
import java.util.Collections
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.ln
import kotlin.properties.Delegates


class AutoChallengeFragment : Fragment() {

    companion object{
        val POSITION = "PositionKey"
    }
    private lateinit var placesClient: PlacesClient
    private lateinit var listPlaces: List<Place>
    private lateinit var includedTypes: List<String>
    private lateinit var excludedTypes: List<String>
    private lateinit var searchNearbyRequest: SearchNearbyRequest
    private lateinit var difficulty: String
    private var lat by Delegates.notNull<Double>()
    private var lng by Delegates.notNull<Double>()
    private lateinit var autoChallengeViewModel: AutoChallengeViewModel

    private val TAG = "xd:"
    private var displayList = Collections.synchronizedList(mutableListOf<String>())
    private var finalPlaces = Collections.synchronizedList(mutableListOf<Place>())
    private var placeList = Collections.synchronizedList(mutableListOf<Place>())
    private var easyList = mutableListOf("tourist_attraction","cultural_landmark",
        "museum", "park", "restaurant", "beach","stadium","monument","shopping_mall")
    private var mediumList = mutableListOf("stadium","cafe",
        "bar", "art_studio","historical_place","tourist_attraction","park",
        "art_gallery","botanical_garden","ice_skating_rink","ski_resort")
    private var hardList = mutableListOf("planetarium", "wildlife_refuge",
        "dessert_restaurant","arena","roller_coaster","night_club",
        "adventure_sports_center","sculpture","aquarium","amusement_park",
        "monument","tourist_attraction","ski_resort","ice_skating_rink")
    private  var numPlaces = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("xd:Hello from challenge")

        val apiKey = "AIzaSyCemoUw_8sUUxfEecvOceNMtibpoajex3Q"
//        val apiKey = BuildConfig.MAPS_API_KEY
        println("xd:api key is ${apiKey}")
        // Log an error if apiKey is not set.
        if (apiKey.isEmpty() || apiKey == "DEFAULT_API_KEY") {
            Log.e("Places test", "No api key")
            requireActivity().finish()
            return
        }

        //Initialize the SDK
        Places.initializeWithNewPlacesApiEnabled(requireContext(),apiKey)
        // Create a new Places API client
        placesClient = Places.createClient(requireContext())

        //Initialize viewModel
        autoChallengeViewModel = ViewModelProvider(requireActivity()).get(AutoChallengeViewModel::class.java)
        autoChallengeViewModel.placesClient = placesClient
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_auto_challenge, container, false)
        val placesListView = view.findViewById<ListView>(R.id.placesListView)

        //If difficulty is passed it means that user wants to start a new challenge
        //Get the user selected difficulty
        val difficultyArg = arguments?.getString(GalleryFragment.DIFFICULITY_KEY)

        if(difficultyArg != null){
            //Get difficulty and user location from previous activity
            difficulty = difficultyArg
            lng = arguments?.getDouble(GalleryFragment.LONGITUDE_KEY)!!
            lat = arguments?.getDouble(GalleryFragment.LATITUDE_KEY)!!

            //Response list
            var placeFields: List<Place.Field> =
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
            //Radius
            val center = LatLng(lat, lng)
            val circle = CircularBounds.newInstance(center, 50000.0)

            //If the viewModel already has the list
            if (autoChallengeViewModel.displayList.isNotEmpty()) {
                // Use the cached list
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    autoChallengeViewModel.displayList
                )
                placesListView.adapter = adapter
            } else {
                // Make the API call
                when (difficulty) {
                    "Easy" -> {
                        includedTypes = easyList.shuffled()
                        excludedTypes = mutableListOf("american_restaurant")
                        numPlaces = 5
                    }

                    "Medium" -> {
                        includedTypes = mediumList.shuffled()
                        excludedTypes = mutableListOf("american_restaurant")
                        numPlaces = 8
                    }

                    "Hard" -> {
                        includedTypes = hardList.shuffled()
                        excludedTypes = mutableListOf("american_restaurant")
                        numPlaces = 12
                    }
                }

                searchNearbyRequest = SearchNearbyRequest.builder(circle, placeFields).
                setIncludedTypes(includedTypes).setExcludedTypes(excludedTypes).
                setMaxResultCount(20).build()

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val places = getPlaces()
                        finalPlaces = places.shuffled().take(numPlaces).toMutableList()

                        for(place in finalPlaces){
                            val placeName = "${place.name} - ${place.address}"
                            displayList.add(placeName)
                        }

                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            displayList
                        )
                        autoChallengeViewModel.displayList = displayList
                        autoChallengeViewModel.placesList = finalPlaces
                        placesListView.adapter = adapter
                    } catch (e: ApiException) {
                        Log.e(TAG, "Places API Error: ${e.statusCode}, message: ${e.message}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Unexpected error: ${e.message}")
                    }
                }
            }
        }else{
            //Load the list from view model
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                autoChallengeViewModel.displayList
            )
            placesListView.adapter = adapter
        }
        placesListView.setOnItemClickListener{_, _, position, _ ->
            val hintsFragment = HintsFragment()
            var bundle = Bundle()
            bundle.putInt(POSITION, position)
            hintsFragment.arguments = bundle
            val navController = findNavController()
            navController.navigate(R.id.hintsFragment,bundle)
        }


        return view
    }

    private suspend fun getPlaces(): List<Place> = suspendCancellableCoroutine { continuation ->
        var resultList = mutableListOf<Place>()
        // Trigger the Places API call
        placesClient.searchNearby(searchNearbyRequest)
            .addOnSuccessListener { response ->
                // Collect the places into the result list
//                for (place in response.places) {
//                    val placeName = "${place.name} - ${place.address}"
//                    resultList.add(placeName)
//                }
                resultList = response.places
                // Resume the coroutine with the result
                continuation.resume(resultList)
            }
            .addOnFailureListener { e ->
                // Resume the coroutine with an exception
                continuation.resumeWithException(e)
            }
    }

    private fun getLocationPermission() {
        println("xd:It didnt work")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e(TAG,"On destroy view is called")

    }

    override fun onResume() {
        super.onResume()
        Log.e(TAG,"On resume is called")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG,"On resume is called")

    }
}


//        placesClient.searchNearby(searchNearbyRequest).addOnSuccessListener { response->
//            listPlaces = response.places
//            for (place in listPlaces) {
//                val placeInfo = "${place.name} - ${place.address}"
//                displayList.add(placeInfo)
//            }
//            displayList = displayList.shuffled().take(numPlaces).toMutableList()
//            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayList)
//            placesListView.adapter = adapter
//        }.addOnFailureListener {
//            println("xd:It failed")
//        }