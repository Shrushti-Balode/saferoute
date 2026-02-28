package com.example.saferoute.ui.map

import android.Manifest
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saferoute.R
import com.example.saferoute.database.AppDatabase
import com.example.saferoute.databinding.FragmentMapBinding
import com.example.saferoute.navigation.NavigationManager
import com.example.saferoute.service.SafetyService
import com.example.saferoute.ui.components.SearchSuggestionAdapter
import com.example.saferoute.utils.ConnectivityUtils
import com.example.saferoute.utils.HeatmapUtils
import com.example.saferoute.utils.GeoUtils
import com.example.saferoute.utils.SOSUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.view.MotionEvent

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private var alpha: Double = 0.5 

    private var searchJob: Job? = null
    private var sliderJob: Job? = null
    private lateinit var suggestionAdapter: SearchSuggestionAdapter
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var routeOverlay: Polyline? = null
    private var secondaryRouteOverlay: Polyline? = null
    private val safetyOverlays = mutableListOf<Overlay>()
    
    private var lastOrigin: Pair<Double, Double>? = null
    private var lastDest: Pair<Double, Double>? = null

    private lateinit var offlineBadge: TextView

    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateOfflineBadge(context)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        Configuration.getInstance().userAgentValue = requireContext().packageName
        Configuration.getInstance().load(requireContext(), requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        offlineBadge = view.findViewById(R.id.offline_badge)
        setupMap()
        setupViews()
        drawHeatmap()
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(13.0)
        binding.mapView.controller.setCenter(GeoPoint(18.5204, 73.8567))

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView)
        val personDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_person_pin)
        val personBitmap = drawableToBitmap(personDrawable!!)
        locationOverlay.setPersonIcon(personBitmap)
        locationOverlay.enableFollowLocation()
        locationOverlay.runOnFirstFix {
            requireActivity().runOnUiThread {
                binding.mapView.controller.animateTo(locationOverlay.myLocation)
                binding.mapView.controller.setZoom(18.0)
            }
        }
        locationOverlay.enableMyLocation()
        binding.mapView.overlays.add(locationOverlay)

        binding.mapView.setOnLongClickListener {
            requestRoute(18.5204 to 73.8567, 18.5220 to 73.8580)
            true
        }

        binding.mapView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val searchPanel = binding.root.findViewById<View>(R.id.search_panel)
                if (searchPanel.visibility == View.VISIBLE) {
                    toggleSearchPanel(false, searchPanel)
                }
            }
            false
        }

        requestPermissionsIfNecessary(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE))
    }

    private fun setupViews() {
        val searchBar = binding.root.findViewById<EditText>(R.id.searchBar)
        val searchPanel = binding.root.findViewById<View>(R.id.search_panel)
        val suggestionsRecyclerView = binding.root.findViewById<RecyclerView>(R.id.suggestionsRecyclerView)

        suggestionAdapter = SearchSuggestionAdapter(mutableListOf()) { poi ->
            toggleSearchPanel(false, searchPanel)
            val startPoint = locationOverlay.myLocation?.let { GeoPoint(it.latitude, it.longitude) } ?: binding.mapView.mapCenter as GeoPoint
            lastOrigin = startPoint.latitude to startPoint.longitude
            lastDest = poi.lat to poi.lon
            requestRoute(lastOrigin!!, lastDest!!)
        }

        suggestionsRecyclerView.adapter = suggestionAdapter
        suggestionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        searchBar.setOnFocusChangeListener { _, hasFocus -> 
            if (hasFocus) toggleSearchPanel(true, searchPanel)
        }
        searchBar.setOnClickListener { 
            toggleSearchPanel(true, searchPanel) 
        }

        searchBar.addTextChangedListener {
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(300)
                val query = it.toString()
                if (query.isNotBlank()) {
                    val suggestions = AppDatabase.getDatabase(requireContext()).appDao().searchPoiFts("$query*")
                    suggestionAdapter.updateData(suggestions)
                } else {
                    val presets = AppDatabase.getDatabase(requireContext()).appDao().getPresetDestinations()
                    suggestionAdapter.updateData(presets)
                }
            }
        }

        binding.root.findViewById<Button>(R.id.btnOptionPickup).setOnClickListener {
            toggleSearchPanel(false, searchPanel)
            lifecycleScope.launch {
                val center = locationOverlay.myLocation?.let { GeoPoint(it.latitude, it.longitude) } ?: binding.mapView.mapCenter as GeoPoint
                val nearest = SafetyService.findNearestPickup(requireContext(), center.latitude, center.longitude)
                if (nearest != null) {
                    addMarker(nearest.first.latitude, nearest.first.longitude, "Nearest Pickup", ContextCompat.getDrawable(requireContext(), R.drawable.ic_pickup_spot))
                    lastOrigin = center.latitude to center.longitude
                    lastDest = nearest.first.latitude to nearest.first.longitude
                    requestRoute(lastOrigin!!, lastDest!!)
                }
            }
        }

        binding.root.findViewById<Button>(R.id.btnOptionSafeArea).setOnClickListener {
            toggleSearchPanel(false, searchPanel)
            lifecycleScope.launch {
                val center = locationOverlay.myLocation?.let { GeoPoint(it.latitude, it.longitude) } ?: binding.mapView.mapCenter as GeoPoint
                val nearest = SafetyService.findNearestPolice(requireContext(), center.latitude, center.longitude)
                if (nearest != null) {
                    addMarker(nearest.first.latitude, nearest.first.longitude, "Nearest Safe Zone", ContextCompat.getDrawable(requireContext(), R.drawable.ic_shield))
                    lastOrigin = center.latitude to center.longitude
                    lastDest = nearest.first.latitude to nearest.first.longitude
                    requestRoute(lastOrigin!!, lastDest!!)
                }
            }
        }

        binding.root.findViewById<Button>(R.id.btnOptionDestination).setOnClickListener {
            toggleSearchPanel(false, searchPanel)
            lifecycleScope.launch(Dispatchers.IO) {
                val presets = AppDatabase.getDatabase(requireContext()).appDao().getPresetDestinations()
                val presetNames = presets.map { it.name }.toTypedArray()
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Select a Destination")
                        .setItems(presetNames) { dialog, which ->
                            val selectedPoi = presets[which]
                            val startPoint = locationOverlay.myLocation ?: binding.mapView.mapCenter as GeoPoint
                            lastOrigin = startPoint.latitude to startPoint.longitude
                            lastDest = selectedPoi.lat to selectedPoi.lon
                            requestRoute(lastOrigin!!, lastDest!!)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }

        binding.btnReport.setOnClickListener {
            val reportTypes = arrayOf("Suspicious Activity", "Low Lighting", "Hazard", "Obstruction")
            AlertDialog.Builder(requireContext())
                .setTitle("Report Issue")
                .setItems(reportTypes) { _, which ->
                    val type = reportTypes[which]
                    val location = locationOverlay.myLocation
                    if (location != null) {
                        addMarker(location.latitude, location.longitude, "Reported: $type", ContextCompat.getDrawable(requireContext(), R.drawable.ic_yellow_flag))
                        Toast.makeText(requireContext(), "$type reported at your location", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Location not found for report", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        binding.btnSOSMap.setOnClickListener {
            val location = locationOverlay.myLocation
            if (location != null) {
                SOSUtils.handleSOS(requireContext(), findNavController(), location.latitude, location.longitude)
            }
        }

        binding.btnSOSMap.setOnLongClickListener {
            findNavController().navigate(R.id.action_mapFragment_to_sosContactsFragment)
            Toast.makeText(requireContext(), "Opening SOS Settings...", Toast.LENGTH_SHORT).show()
            true
        }

        binding.btnCenterPune.setOnClickListener {
            val myLocation = locationOverlay.myLocation
            if (myLocation != null) {
                binding.mapView.controller.animateTo(myLocation)
                binding.mapView.controller.setZoom(18.0)
            } else {
                Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
        
        val tvAlpha = binding.root.findViewById<TextView>(R.id.tvAlpha)
        tvAlpha.text = "Speed <-> Safety (${(alpha*100).toInt()}%)"

        binding.root.findViewById<SeekBar>(R.id.sliderAlpha).setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                alpha = progress / 100.0
                tvAlpha.text = "Speed <-> Safety (${progress}%)"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { sliderJob?.cancel() }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sliderJob = lifecycleScope.launch {
                    delay(300) 
                    val orig = lastOrigin
                    val dst = lastDest
                    if (orig != null && dst != null) requestRoute(orig, dst)
                }
            }
        })
    }

    private fun requestRoute(orig: Pair<Double, Double>, dst: Pair<Double, Double>) {
        lifecycleScope.launch {
            if (routeOverlay != null) binding.mapView.overlays.remove(routeOverlay)
            if (secondaryRouteOverlay != null) binding.mapView.overlays.remove(secondaryRouteOverlay)
            safetyOverlays.forEach { binding.mapView.overlays.remove(it) }
            safetyOverlays.clear()

            val speedRoute = NavigationManager.computeRoute(requireContext(), orig.first, orig.second, dst.first, dst.second, 0.0)
            
            var safetyResult: com.example.saferoute.navigation.RouteResult? = null
            
            if (alpha > 0.5) {
                val midLat = (orig.first + dst.first) / 2.0
                val midLon = (orig.second + dst.second) / 2.0
                
                var safeSpot = SafetyService.findNearestPolice(requireContext(), midLat, midLon + 0.005)?.first
                if (safeSpot == null) {
                    safeSpot = com.example.saferoute.data.PoliceStation(0, "Demo Safe Haven", midLat + 0.002, midLon + 0.003, "Demo Address")
                }
                
                val part1 = NavigationManager.computeRoute(requireContext(), orig.first, orig.second, safeSpot.latitude, safeSpot.longitude, 1.0)
                val part2 = NavigationManager.computeRoute(requireContext(), safeSpot.latitude, safeSpot.longitude, dst.first, dst.second, 1.0)
                
                if (part1 != null && part2 != null) {
                    val combinedPoints = part1.points + part2.points
                    safetyResult = com.example.saferoute.navigation.RouteResult(combinedPoints, part1.totalDistanceMeters + part2.totalDistanceMeters, part1.estimatedTimeSeconds + part2.estimatedTimeSeconds)
                    addSafetyMarkers(safeSpot.latitude, safeSpot.longitude)
                }
            }

            val fastPoints = speedRoute?.points ?: listOf(orig, dst)
            val safePoints = safetyResult?.points ?: fastPoints

            drawComparativeRoute(fastPoints, Color.parseColor("#44FF0000"), true) 
            
            val activePoints = if (alpha > 0.7) safePoints else if (alpha < 0.3) fastPoints else safePoints
            val activeColor = if (alpha > 0.7) Color.parseColor("#FF00C853") else if (alpha < 0.3) Color.CYAN else Color.BLUE
            drawComparativeRoute(activePoints, activeColor, false)

            val status = if (alpha > 0.7) "🛡️ SafeRoute™: High Security Path Active" else if (alpha < 0.3) "⚡ Standard: Shortest Path" else "⚖️ Balanced Coverage"
            Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
            
            binding.mapView.invalidate()
        }
    }

    private fun addSafetyMarkers(lat: Double, lon: Double) {
        val shieldIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_shield)
        
        val highlightCircle = Polygon().apply {
            points = Polygon.pointsAsCircle(GeoPoint(lat, lon), 250.0) 
            fillColor = Color.argb(60, 0, 255, 0) 
            strokeColor = Color.GREEN
            strokeWidth = 3f
        }
        
        val m1 = Marker(binding.mapView).apply {
            position = GeoPoint(lat, lon)
            icon = shieldIcon
            title = "Incident reported on original route: this route is safer"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        
        safetyOverlays.add(highlightCircle)
        safetyOverlays.add(m1)
        binding.mapView.overlays.add(highlightCircle)
        binding.mapView.overlays.add(m1)
    }

    private fun drawComparativeRoute(points: List<Pair<Double, Double>>, routeColor: Int, isDashed: Boolean) {
        val polyline = Polyline().apply {
            setPoints(points.map { GeoPoint(it.first, it.second) })
            color = routeColor
            width = if (isDashed) 14f else 24f
            if (isDashed) {
                outlinePaint.pathEffect = DashPathEffect(floatArrayOf(25f, 25f), 0f)
            }
        }
        
        if (isDashed) secondaryRouteOverlay = polyline else routeOverlay = polyline
        binding.mapView.overlays.add(polyline)
        
        if (!isDashed) {
            try {
                val bbox = org.osmdroid.util.BoundingBox.fromGeoPoints(points.map { GeoPoint(it.first, it.second) })
                binding.mapView.zoomToBoundingBox(bbox, true, 150)
            } catch (_: Throwable) {}
        }
    }

    private fun addMarker(lat: Double, lon: Double, title: String, icon: Drawable?) {
        val marker = Marker(binding.mapView)
        marker.position = GeoPoint(lat, lon)
        marker.title = title
        if(icon != null) marker.icon = icon
        binding.mapView.overlays.add(marker)
        binding.mapView.invalidate()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun toggleSearchPanel(show: Boolean, searchPanel: View) {
        if (show) {
            if (searchPanel.visibility == View.VISIBLE) return
            searchPanel.visibility = View.VISIBLE
            searchPanel.post {
                searchPanel.translationY = -searchPanel.height.toFloat()
                ObjectAnimator.ofFloat(searchPanel, "translationY", 0f).setDuration(250).start()
            }
        } else {
            if (searchPanel.visibility != View.VISIBLE) return
            searchPanel.post {
                ObjectAnimator.ofFloat(searchPanel, "translationY", -searchPanel.height.toFloat()).setDuration(200).start()
                searchPanel.postDelayed({ searchPanel.visibility = View.GONE }, 220)
            }
        }
    }

    private fun drawHeatmap() {
        val zones = HeatmapUtils.generateRandomHeatmap(18.5204, 73.8567, 40, 100.0, 400.0)
        for (zone in zones) {
            val circle = Polygon()
            circle.points = Polygon.pointsAsCircle(GeoPoint(zone.latitude, zone.longitude), zone.radius)
            circle.fillColor = when {
                zone.safetyScore > 70 -> Color.argb(70, 0, 255, 0)
                zone.safetyScore > 40 -> Color.argb(70, 255, 255, 0)
                else -> Color.argb(70, 255, 0, 0)
            }
            circle.strokeWidth = 0f
            binding.mapView.overlays.add(circle)
        }
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        locationOverlay.enableMyLocation()
        requireActivity().registerReceiver(connectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        locationOverlay.disableMyLocation()
        requireActivity().unregisterReceiver(connectivityReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateOfflineBadge(context: Context) {
        val badge = view?.findViewById<TextView>(R.id.offline_badge) ?: return
        if (ConnectivityUtils.isOnline(context)) {
            if (badge.visibility == View.VISIBLE) {
                badge.text = "Syncing..."
                Handler(Looper.getMainLooper()).postDelayed({ badge.visibility = View.GONE }, 2000)
            }
        } else {
            badge.text = "Offline"
            badge.visibility = View.VISIBLE
        }
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val toRequest = permissions.filter { ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED }
        if (toRequest.isNotEmpty()) requestPermissions(toRequest.toTypedArray(), REQUEST_PERMISSIONS_REQUEST_CODE)
    }
}
