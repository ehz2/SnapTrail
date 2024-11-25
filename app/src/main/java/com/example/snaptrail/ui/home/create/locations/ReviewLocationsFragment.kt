// ReviewLocationsFragment.kt
package com.example.snaptrail.ui.home.create.locations

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snaptrail.R
import com.google.gson.Gson
import com.example.snaptrail.databinding.FragmentReviewLocationsBinding

class ReviewLocationsFragment : Fragment() {
    private var _binding: FragmentReviewLocationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LocationsAdapter
    private var locations: ArrayList<LocationData> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            locations = ArrayList(ReviewLocationsFragmentArgs.fromBundle(it).locations.toList())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = LocationsAdapter(
            locations = locations,
            onDeleteClick = { position ->
                if (position in 0 until locations.size) {
                    locations.removeAt(position)
                    adapter.notifyDataSetChanged()

                    // Save immediately after deletion
                    saveLocations()

                    if (locations.isEmpty()) {
                        binding.buttonConfirm.isEnabled = false
                    }
                }
            }
        )

        binding.recyclerViewLocations.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ReviewLocationsFragment.adapter
            itemAnimator?.changeDuration = 0
        }
    }

    private fun setupButtons() {
        binding.buttonConfirm.setOnClickListener {
            if (locations.isNotEmpty()) {
                saveLocations()
                // Navigate back to CreateTrailFragment
                findNavController().popBackStack(R.id.createTrailFragment, false)
            }
        }
    }

    private fun saveLocations() {
        val sharedPrefs = requireContext().getSharedPreferences("Locations", Context.MODE_PRIVATE)
        val locationsJson = Gson().toJson(locations)
        sharedPrefs.edit().putString("saved_locations", locationsJson).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}