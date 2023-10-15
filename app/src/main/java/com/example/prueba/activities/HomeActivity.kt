package com.example.prueba.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.prueba.R
import com.example.prueba.databinding.ActivityHomeBinding
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay

class HomeActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityHomeBinding
    lateinit var map: MapView
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var linearAceleration: Sensor
    private lateinit var orientationSensor: Sensor
    private lateinit var locationManager: LocationManager
    private val REQUEST_LOCATION_PERMISSION = 1

    private val geocoder: Geocoder by lazy {
        Geocoder(this)
    }

    private val userLocationMarkers = ArrayList<Marker>()

    /*********************************************/
    private val searchMarkers = ArrayList<Marker>()

    private lateinit var roadManager: RoadManager
    private var userGeoPoint: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().load(this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        map = binding.osmMap
        map.setTileSource(
            TileSourceFactory.
            MAPNIK)
        map.setMultiTouchControls(true)
        map.overlays.add(createOverlayEvents())


         binding.notificaciones.setOnClickListener{
            startActivity(Intent(baseContext, NotificacionesActivity::class.java))
        }

        binding.perfil.setOnClickListener {
            startActivity(Intent(baseContext, PerfilActivity::class.java))
        }

        binding.tiendaPuntos.setOnClickListener {
            startActivity(Intent(baseContext, PuntosActivity::class.java))
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        linearAceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

      roadManager = OSRMRoadManager(this,"ANDROID")

    }

    val latitude = 4.62
    val longitude = -74.07
    val startPoint = GeoPoint(latitude, longitude)

    override fun onResume() {
        super.onResume()
        map.onResume()
        map.
        controller.setZoom(18.0)
        map.
        controller.animateTo(startPoint)

        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, linearAceleration, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL)

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),REQUEST_LOCATION_PERMISSION)
        }else{
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if(location != null){
                val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                map.controller.animateTo(userGeoPoint)
            }else{

            }



            showUserLocation()
        }
        /*
        if (userGeoPoint == null) {
            userGeoPoint = getUserLocation()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        } else {
            showUserLocation()
        } */

    }
    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun getUserLocation(): GeoPoint? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (location != null) {
                val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                map.controller.animateTo(userGeoPoint)
                return userGeoPoint
            } else {
                // Handle the case where location is not available
                return null
            }
        } else {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
            return null
        }
    }


    private fun createOverlayEvents(): MapEventsOverlay{

        return MapEventsOverlay(object: MapEventsReceiver{
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                if(p!= null){
                    longPressOnMap(p)

                    Log.d("YourTag", "p: Latitude=${p?.latitude}, p=${p?.longitude}")

                }
                return true;
            }

        })
    }

    private fun longPressOnMap(p: GeoPoint) {

        val address = findAddress(LatLng(p.latitude, p.longitude))
        val snippet: String = address ?: ""

        searchMarkers.forEach{map.overlays.remove(it)}
        searchMarkers.clear()

        val marker = createMarker(p, snippet, R.drawable.arrowofuser,"Unknown")
        searchMarkers.add(marker)
        map.overlays.add(marker)

    }

    private fun findAddress(latLng: LatLng): String? {

        val addresses:List<Address> = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) ?: emptyList()
        if(addresses.isNotEmpty()){
            val address: Address = addresses[0]
            return address.getAddressLine(0)
        }
    return null
    }

    private fun showUserLocation(){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if(location != null){

                val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                val userLocationMarker = createMarker(userGeoPoint, "Mi ubicacion", R.drawable.puntero2,"North")
                userLocationMarkers.add(userLocationMarker)
                map.overlays.add(userLocationMarker)

            }
        }
    }

    private fun createMarker(p: GeoPoint, title: String, iconID: Int, direccion: String): Marker {

        val marker = Marker(map)
        marker.title = title
        val myIcon = ContextCompat.getDrawable(this,  iconID)
        marker.icon = myIcon
        marker.position = p
        val setAnchorByDirection: Marker.() -> Unit = {
            when (direccion) {
                "North" -> setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                "South" -> setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP)
                "East" -> setAnchor(Marker.ANCHOR_LEFT, Marker.ANCHOR_CENTER)
                "West" -> setAnchor(Marker.ANCHOR_RIGHT, Marker.ANCHOR_CENTER)
                "Northeast" -> setAnchor(Marker.ANCHOR_LEFT, Marker.ANCHOR_TOP)
                "Southeast" -> setAnchor(Marker.ANCHOR_LEFT, Marker.ANCHOR_BOTTOM)
                "Southwest" -> setAnchor(Marker.ANCHOR_RIGHT, Marker.ANCHOR_BOTTOM)
                "Northwest" -> setAnchor(Marker.ANCHOR_RIGHT, Marker.ANCHOR_TOP)

                else -> setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }

        }
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        return marker

    }

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//

    private val locationListener: LocationListener = object: LocationListener {
        override fun onLocationChanged(location: Location) {
            val latitude = location.latitude
            val longitude = location.longitude
            val geoPoint = GeoPoint(latitude, longitude)

            userLocationMarkers.forEach { map.overlays.remove(it) }
            userLocationMarkers.clear()

            val address = "Ubicacion Actual"
            val userLocationMarker = createMarker(geoPoint, address, R.drawable.puntero2, "North")
            userLocationMarkers.add(userLocationMarker)
            map.overlays.add(userLocationMarker)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }
    }
        private fun searchLocation(address: String){

            val addresses: List<Address> = geocoder.getFromLocationName(address, 1) as? List<Address> ?: emptyList()
            if(addresses.isNotEmpty()){

                val foundAddress = addresses[0]
                val latitude = foundAddress.latitude
                val longitude = foundAddress.longitude
                val geoPoint = GeoPoint(latitude, longitude)

                searchMarkers.forEach{map.overlays.remove(it)}
                searchMarkers.clear()

                val marker = createMarker(geoPoint, address, R.drawable.puntero2,"North")
                searchMarkers.add(marker)
                map.overlays.add(marker)

                map.controller.animateTo(geoPoint)
            }else{
                Toast.makeText(baseContext, "Direccion no encontrada", Toast.LENGTH_SHORT).show()
            }
        }

        private var roadOverlay: Polyline? = null

        fun drawRoute(start: GeoPoint, finish: GeoPoint){
            var routePoints = ArrayList<GeoPoint>()
            routePoints.add(start)
            routePoints.add(finish)
            val road = roadManager.getRoad(routePoints)
            Log.i("MapsApp", "Route length: "+road.mLength+" klm")
            Log.i("MapsApp", "Duration: "+road.mDuration/60+" min")
            if(map!=null){
                if(roadOverlay != null){
                    map.getOverlays().remove(roadOverlay);
                }
                roadOverlay = RoadManager.buildRoadOverlay(road)
                roadOverlay!!.getOutlinePaint().setColor(
                    Color.
                RED)
                roadOverlay!!.getOutlinePaint().setStrokeWidth(10F)
                map.getOverlays().add(roadOverlay)
            }

        }

    override fun onSensorChanged(event: SensorEvent?) {
      if(event?.sensor?.type == Sensor.TYPE_LIGHT){
            val lightValue = event.values[0]
            val threshold = 80.0
            if(lightValue < threshold){
                map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
            }else{
                map.overlayManager.tilesOverlay.setColorFilter(null)
            }
        }

        if(event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {

            val rotationMatrix = FloatArray(9)
            val orientationValues = FloatArray(3)

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationValues)

            val azimuthRadians = orientationValues[0]

            val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()

            val adjustedAzimuth = if (azimuthDegrees < 0) azimuthDegrees + 360 else azimuthDegrees

            val direction = mapHeadingToDirection(adjustedAzimuth)

            val direccionTextView = findViewById<TextView>(R.id.direccion)

            direccionTextView.text = direction

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            } else {
                locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                if (location != null) {
                    val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                    val userLocationMarker = createMarker(userGeoPoint,"user",R.drawable.arrowofuser,direction)
                    userLocationMarkers.add(userLocationMarker)
                    map.overlays.add(userLocationMarker)
                } else {

                }
            }

            R.id.direccion
        }
        if(event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION){

            val accelerationX = event.values[0]
            val accelerationY = event.values[1]
            val accelerationZ = event.values[2]


        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    fun mapHeadingToDirection(heading: Float): String {
        return when (heading) {
            in 337.5..22.5, in 0.0..22.5 -> "North"
            in 22.5..67.5 -> "Northeast"
            in 67.5..112.5 -> "East"
            in 112.5..157.5 -> "Southeast"
            in 157.5..202.5 -> "South"
            in 202.5..247.5 -> "Southwest"
            in 247.5..292.5 -> "West"
            in 292.5..337.5 -> "Northwest"
            else -> "Unknown"
        }
    }
}


