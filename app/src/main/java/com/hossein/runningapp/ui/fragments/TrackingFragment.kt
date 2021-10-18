package com.hossein.runningapp.ui.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.hossein.runningapp.R
import com.hossein.runningapp.databinding.FragmentTrackingBinding
import com.hossein.runningapp.db.Run
import com.hossein.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.hossein.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.hossein.runningapp.other.Constants.MAP_ZOOM
import com.hossein.runningapp.other.Constants.POLYLINE_COLOR
import com.hossein.runningapp.other.Constants.POLYLINE_WIDTH
import com.hossein.runningapp.other.TrackingUtility
import com.hossein.runningapp.services.Polyline
import com.hossein.runningapp.services.TrackingService
import com.hossein.runningapp.ui.viewModels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.round


const val CANCEL_TRACKING_DIALOG_TAG = "CancelDialog"

@AndroidEntryPoint
class TrackingFragment : Fragment(), OnMapReadyCallback {

    private val viewModel: MainViewModel by viewModels()
    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private var map: GoogleMap? = null
    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var curTimeInMillis = 0L
    private var menu: Menu? = null
    private lateinit var mapView: MapView

    @set:Inject
    var weight = 80f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        mapView = binding.mapView
        binding.tvTimer.text = getString(R.string.start_timer)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        mapView.getMapAsync {
            addAllPolylines()
        }

        binding.tvTimer.text = getString(R.string.start_timer)

        if (savedInstanceState != null) {
            val cancelTrackingDialog = parentFragmentManager.findFragmentByTag(
                CANCEL_TRACKING_DIALOG_TAG
            ) as CancelTrackingDialog?

            cancelTrackingDialog?.setYesListener {
                stopRun()
            }
        }

        binding.btnToggleRun.setOnClickListener {
            toggleRun()
        }

        binding.btnFinishRun.setOnClickListener {
            if (pathPoints.last().isNotEmpty()) {
                Timber.d("$pathPoints")
                zoomToSeeWholeTrack()
                endRunSaveToDb()
            } else {
                Snackbar.make(requireView(), "Unregistered Location!", Snackbar.LENGTH_SHORT).show()
            }
        }

        subscribeToObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu, menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        this.menu?.getItem(0)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miCancelTracking -> {
                showCancelTrackingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap
        val latitude = 35.715298
        val longitude = 51.404343

        val homeLatLng = LatLng(latitude, longitude)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, 10f))

        if (mapView.findViewById<View?>("1".toInt()) != null) {
            // Get the button view
            val locationButton: View =
                (mapView.findViewById<View>("1".toInt()).parent as View).findViewById("2".toInt())
            // and next place it, on bottom right (as Google Maps app)
            val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            layoutParams.setMargins(0, 0, 30, 30)
        }

        enableMyLocation()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (TrackingUtility.hasPermission(requireContext())) {
            map?.isMyLocationEnabled = true
        } else {
            Snackbar.make(requireView(), "Please accept permissions", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showCancelTrackingDialog() {
        CancelTrackingDialog().apply {
            setYesListener {
                stopRun()
            }
        }.show(parentFragmentManager, CANCEL_TRACKING_DIALOG_TAG)
    }

    private fun stopRun() {
        sendCommandToService("ACTION_STOP_SERVICE")
        findNavController().navigate(
            R.id.action_trackingFragment_to_runFragment
        )
    }

    private fun zoomToSeeWholeTrack() {
        val bounds = LatLngBounds.Builder()
        for (polyline in pathPoints) {
            for (pos in polyline) {
                bounds.include(pos)
            }
        }

        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                mapView.width,
                mapView.height,
                (mapView.height * 0.05f).toInt()
            )
        )
    }

    private fun endRunSaveToDb() {
        map?.snapshot { bmp ->
            var distanceInMeters = 0
            for (polyline in pathPoints) {
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }
            val avgSpeed =
                round((distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            val dateTimeStamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()
            val run =
                Run(bmp, dateTimeStamp, curTimeInMillis, distanceInMeters, avgSpeed, caloriesBurned)
            viewModel.insertRun(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            stopRun()
        }
    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, {
            updateTracking(it)
        })
        TrackingService.pathPoints.observe(viewLifecycleOwner, {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })
        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, {
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
            binding.tvTimer.text = formattedTime
        })
    }

    private fun toggleRun() {
        if (isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
            menu?.getItem(0)?.isVisible = true
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking && curTimeInMillis > 0L) {
            binding.btnToggleRun.text = getString(R.string.Start)
            binding.btnFinishRun.visibility = View.VISIBLE
        } else if (isTracking) {
            binding.btnToggleRun.text = getString(R.string.Stop)
            binding.btnFinishRun.visibility = View.GONE
            menu?.getItem(0)?.isVisible = true
        }
    }

    private fun moveCameraToUser() {
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun addAllPolylines() {
        for (polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline() {
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
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

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        addAllPolylines()
        Timber.d("onResume")
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        Timber.d("onStart")
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        Timber.d("onStop")
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        Timber.d("onPause")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
        Timber.d("onLowMemory")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}