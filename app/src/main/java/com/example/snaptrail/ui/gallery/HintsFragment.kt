package com.example.snaptrail.ui.gallery

import android.Manifest
import android.R.attr.bitmap
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.snaptrail.BuildConfig
import com.example.snaptrail.R
import com.example.snaptrail.Util
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.libraries.places.api.model.Place.Field
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Rotation
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.xml.KonfettiView
import okio.IOException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

class HintsFragment : Fragment() {
    private val Tag = "xd:"
    private var position = 0

    private lateinit var autoChallengeViewModel: AutoChallengeViewModel
    private lateinit var hintsTextView: TextView
    private lateinit var placeImageView: ImageView
    private lateinit var titleView: TextView
    private lateinit var confettiView: KonfettiView
    private lateinit var placesClient: PlacesClient
    private lateinit var generativeModel: GenerativeModel
    private lateinit var geminiAPIKey:String
    private lateinit var placeDetails:String
    private lateinit var revealBtn:Button
    private lateinit var clicPictureBtn:Button
    private lateinit var place:Place
    private lateinit var cameraPhotoUri: Uri
    private lateinit var selectedPlaceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoChallengeViewModel = ViewModelProvider(requireActivity()).
        get(AutoChallengeViewModel::class.java)
        placesClient = autoChallengeViewModel.placesClient
        geminiAPIKey = "AIzaSyC_vBpX2PD_e6lYrh-F5HByRE81DnUlNjA"
        generativeModel = GenerativeModel("gemini-1.5-flash",geminiAPIKey)
//        val geminiAPIKey = BuildConfig.GEMINI_API_KEY
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_hints, container, false)
        hintsTextView = view.findViewById(R.id.hints)
        placeImageView = view.findViewById(R.id.placeImage)
        titleView = view.findViewById(R.id.titleHints)
        revealBtn = view.findViewById(R.id.revealButton)
        clicPictureBtn = view.findViewById(R.id.clickPicture)
        confettiView = view.findViewById(R.id.konfettiView)
        confettiView.visibility = View.VISIBLE
        confettiView.bringToFront()

        position = arguments?.getInt(AutoChallengeFragment.POSITION)!!
        place = autoChallengeViewModel.placesList[position]
        placeDetails = "${place.name} - ${place.address}"
        var placeId = place.id !!
        selectedPlaceId = placeId
        var placeName = place.name!!
        val cachedData = autoChallengeViewModel.getHintAndImage(placeId)
        //If data is already cached use that
        if(cachedData!=null){
            val(hints,photoUri) = cachedData
            hintsTextView.text = hints

            val requestOptions = RequestOptions().
            override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
            Glide.with(this@HintsFragment).
            load(photoUri).into(placeImageView)
            Log.e(Tag,"Cached DATA IS CALLED !!!!!!")
        }else{
            //Generate image and hints
            fetchImageAndHints(placeName, placeId)
        }
        revealBtn.setOnClickListener(){
            titleView.text = "$placeName - ${place.address}"
            titleView.textSize = 20F
        }
        clicPictureBtn.setOnClickListener(){
            openCamera()
        }
        return view
    }

    private fun verifyPlace(selectedPlaceId:String) {
        val placeFields: List<Place.Field> = listOf(
            Place.Field.NAME, Place.Field.ID,
            Place.Field.ADDRESS
        )
        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)
        // Call the Current Place API
        if (ContextCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {

            val placeResponse = placesClient.findCurrentPlace(request)
            placeResponse.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val response = task.result
                    var isCorrect = false
                    for (placeLikelihood: PlaceLikelihood in response?.placeLikelihoods
                        ?: emptyList()) {
                        val place = placeLikelihood.place
                        val likelihood = placeLikelihood.likelihood

                        if (place.id == selectedPlaceId && likelihood > 0.5) { // Adjust threshold as needed
                            // User is at the correct location
                            isCorrect = true
                            Log.i(
                                Tag,
                                "Place '${placeLikelihood.place.name}' has likelihood: ${placeLikelihood.likelihood}"
                            )
                            break
                        }
                    }
                    if(isCorrect){
                        Snackbar.make(requireView(), "Correct location! Well done!", Snackbar.LENGTH_LONG)
                            .setAction("Go Back") {
                                parentFragmentManager.popBackStack()
                            }.show()
                        // Show confetti
                        repeat(2){
                            showSuccessConfetti()
                        }
                        val resultBundle = Bundle()
                        resultBundle.putInt(SUCCESSFUL_PLACE_POSITION, position) // Replace `position` with the correct index
                        parentFragmentManager.setFragmentResult(PLACE_SUCCESS_KEY, resultBundle)

                        return@addOnCompleteListener
                    }
                    else{
                        shakeView(requireView())
                        Snackbar.make(requireView(), "Correct location! Well done!", Snackbar.LENGTH_LONG).show()
                        Toast.makeText(context,"Oops! This isn't the correct location. Try again!",Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val exception = task.exception
                    if (exception is ApiException) {
                        Log.e(Tag, "Place not found: ${exception.statusCode}")
                    }
                }
            }
        }else{
            Util.checkAndRequestPermissions(requireActivity())
        }
    }
    private fun showSuccessConfetti() {
        Log.d(Tag,"ConfettiView visibility: ${confettiView.visibility}")
        var party = Party(
            speed = 30f,
            maxSpeed = 50f,
            damping = 0.9f,
            angle = Angle.TOP,
            spread = 45,
            size = listOf(Size.SMALL, Size.LARGE, Size.LARGE),
            shapes = listOf(Shape.Square, Shape.Circle).filterNotNull(),
            timeToLive = 2000L,
            rotation = Rotation(),
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            emitter = Emitter(duration = 2000, TimeUnit.MILLISECONDS).max(30),
            position = Position.Relative(0.5, 1.0)
        )
        confettiView.start(party)
    }
    private fun shakeView(view: View) {
        val shake = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        shake.duration = 2000
        shake.start()
    }
    private fun fetchImageAndHints(placeName:String, placeId:String){
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val photoDeffered = async { fetchImage(placeId) }
                val hintsDeffered = async { generateHints(placeName) }

                val photoResultUri = photoDeffered.await()
                val hints = hintsDeffered.await()

                withContext(Dispatchers.Main) {
                    try{
                        val requestOptions = RequestOptions().
                        override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
                        Glide.with(this@HintsFragment).
                        load(photoResultUri).into(placeImageView)
                        hintsTextView.text = hints
                        autoChallengeViewModel.storeHintAndImage(placeId,hints,photoResultUri!!)
                    }catch (e:Exception){
                        //no Image
                        Log.e(Tag,"No Image available inside the coroutine")
                        placeImageView.setImageResource(R.drawable.placeholder)
                        hintsTextView.text = "Failed to load data."
                    }
                }
            }catch (e:Exception){
                Log.e(Tag, "Error fetching photo or prompt: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    placeImageView.setImageResource(R.drawable.placeholder)
                    hintsTextView.text = "Failed to load data."
                }
            }
        }
    }
    private suspend fun fetchImage(placeId:String):Uri?{
        try {
            val fields = listOf(Place.Field.PHOTO_METADATAS)
            val placeRequest = FetchPlaceRequest.newInstance(placeId, fields)

            val placeResponse = placesClient.fetchPlace(placeRequest).await()

            val place = placeResponse.place

            val photoMetadata = place.photoMetadatas.firstOrNull()
            if (photoMetadata == null) {
                Log.w(Tag, "No photo metadata.")
                return null
            }
            val screenWidth = Resources.getSystem().displayMetrics.widthPixels

            val photoRequest = FetchResolvedPhotoUriRequest.builder(photoMetadata)
                .setMaxWidth(screenWidth) // Set the width to the screen width
                .setMaxHeight((screenWidth * 0.6).toInt()).build()

            val photoResponse = placesClient.fetchResolvedPhotoUri(photoRequest).await()
            val photoResponseUri = photoResponse.uri
            return photoResponseUri
        }catch (e:Exception){
            Log.e(Tag, "Error fetching photo: ${e.localizedMessage}")
            return null
        }
    }

    private suspend fun generateHints(placeName:String): String {
        return try {
            val prompt = "I am making a game where the person has to guess the place" +
                    " based on the hints I give. Can you please generate 5 easy hints about ${placeName}." +
                    "When you give hints make sure to leave more space between lines"
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No hints could be generated."
        } catch (e: Exception) {
            Log.e(Tag, "Error generating hints: ${e.localizedMessage}")
            "Failed to generate hints. Please try again."
        }
    }

    private fun openCamera() {
        // Create an intent to open the camera
        Util.checkAndRequestPermissions(requireActivity())
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            // Retrieve the Bitmap from the Intent
            val imageBitmap = data?.extras?.get("data") as Bitmap?
            imageBitmap?.let {
                saveBitmapToGallery(it) // Save the Bitmap to the gallery
            }
            verifyPlace(selectedPlaceId)
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        // Use the current time as the file name
        val displayName = "Captured_${System.currentTimeMillis()}.jpg"

        // Set the image's metadata
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        // Insert the image into the MediaStore
        val resolver = requireActivity().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        // Write the Bitmap to the output stream
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            Log.d("HintsFragment", "Image saved to gallery: $uri")
        } ?: Log.e("HintsFragment", "Failed to save image to gallery.")
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        var PLACE_SUCCESS_KEY = "place_success_key"
        var SUCCESSFUL_PLACE_POSITION = "successful_place_position"
    }


}