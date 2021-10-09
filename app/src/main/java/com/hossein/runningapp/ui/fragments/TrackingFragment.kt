package com.hossein.runningapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.PolylineOptions
import com.hossein.runningapp.databinding.FragmentTrackingBinding
import com.hossein.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.hossein.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.hossein.runningapp.other.Constants.MAP_ZOOM
import com.hossein.runningapp.other.Constants.POLYLINE_COLOR
import com.hossein.runningapp.other.Constants.POLYLINE_WIDTH
import com.hossein.runningapp.services.Polyline
import com.hossein.runningapp.services.TrackingService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackingFragment: Fragment() {

    private val viewModel: ViewModel by viewModels()
    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private var map:GoogleMap? = null
    private var mapView:MapView? = null
    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        mapView = binding.mapView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync {
            map = it
            addAllPolylines()
        }

        binding.btnToggleRun.setOnClickListener {
            toggleRun()
        }

        subscribeToObservers()

    }

    private fun subscribeToObservers(){
        TrackingService.isTracking.observe(viewLifecycleOwner, {
            updateTracking(it)
        })
        TrackingService.pathPoints.observe(viewLifecycleOwner, {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })
    }

    private fun toggleRun(){
        if (isTracking){
            sendCommandToService(ACTION_PAUSE_SERVICE)
        }else{
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTracking(isTracking: Boolean){
        this.isTracking = isTracking
        if (!isTracking){
            binding.btnToggleRun.text = "Start"
            binding.btnFinishRun.visibility = View.VISIBLE
        }else{
            binding.btnToggleRun.text = "Stop"
            binding.btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser(){
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()){
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM)
            )
        }
    }

    private fun addAllPolylines(){
        for (polyline in pathPoints){
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline(){
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1){
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action:String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}