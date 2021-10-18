package com.hossein.runningapp.ui.fragments

import android.Manifest
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.hossein.runningapp.R
import com.hossein.runningapp.adapters.RunAdapter
import com.hossein.runningapp.databinding.FragmentRunBinding
import com.hossein.runningapp.other.Constants
import com.hossein.runningapp.other.SortType
import com.hossein.runningapp.other.TrackingUtility.hasPermission
import com.hossein.runningapp.ui.viewModels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class RunFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    private var _binding: FragmentRunBinding? = null
    private val binding get() = _binding!!
    private lateinit var runAdapter: RunAdapter
    private lateinit var spFilter: Spinner

    private var googleApiClient: GoogleApiClient? = null
    private val REQUEST_LOCATION = 199

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunBinding.inflate(inflater, container, false)
        spFilter = binding.spFilter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        enableLoc()

        super.onViewCreated(view, savedInstanceState)
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_runFragment_to_trackingFragment)
        }

        if (!hasPermission(requireContext())) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setupRecyclerView()

        spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                id: Long
            ) {
                when (pos) {
                    0 -> viewModel.sortRuns(SortType.DATE)
                    1 -> viewModel.sortRuns(SortType.RUNNING_TIME)
                    2 -> viewModel.sortRuns(SortType.AVG_SPEED)
                    3 -> viewModel.sortRuns(SortType.DISTANCE)
                    4 -> viewModel.sortRuns(SortType.CALORIES_BURNED)
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        viewModel.runs.observe(viewLifecycleOwner, {
            runAdapter.submitList(it)
        })

        when (viewModel.sortType) {
            SortType.DATE -> spFilter.setSelection(0)
            SortType.RUNNING_TIME -> spFilter.setSelection(1)
            SortType.AVG_SPEED -> spFilter.setSelection(2)
            SortType.DISTANCE -> spFilter.setSelection(3)
            SortType.CALORIES_BURNED -> spFilter.setSelection(4)
        }
    }

    private fun enableLoc() {
        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(requireContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(object : ConnectionCallbacks {
                    override fun onConnected(bundle: Bundle?) {}
                    override fun onConnectionSuspended(i: Int) {
                        googleApiClient!!.connect()
                    }
                })
                .addOnConnectionFailedListener {}.build()
            googleApiClient.run { this!!.connect() }
            val locationRequest: LocationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            //locationRequest.interval = 30 * 1000
            //locationRequest.fastestInterval = 5 * 1000
            locationRequest.interval = Constants.LOCATION_UPDATE_INTERVAL
            locationRequest.fastestInterval = Constants.FASTEST_LOCATION_INTERVAL
            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
            builder.setAlwaysShow(true)
            val result: PendingResult<LocationSettingsResult> =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
            result.setResultCallback { results ->
                val status: Status = results.status
                when (status.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        status.startResolutionForResult(requireActivity(), REQUEST_LOCATION)
                        // finish();
                    } catch (e: SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() = binding.rvRuns.apply {
        runAdapter = RunAdapter()
        adapter = runAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}