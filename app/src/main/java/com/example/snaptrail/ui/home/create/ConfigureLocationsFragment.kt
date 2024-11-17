package com.example.snaptrail.ui.home.create

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snaptrail.databinding.FragmentConfigureLocationsBinding

class ConfigureLocationsFragment : Fragment() {

    private var _binding: FragmentConfigureLocationsBinding? = null
    private val binding get() = _binding!!

    private val locations = mutableListOf<String>()
    private lateinit var adapter: LocationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentConfigureLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LocationsAdapter(locations)
        binding.recyclerViewLocations.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewLocations.adapter = adapter

        binding.btnAddLocation.setOnClickListener {
            val location = binding.editLocation.text.toString()
            if (location.isNotEmpty()) {
                locations.add(location)
                adapter.notifyDataSetChanged()
                binding.editLocation.text.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}