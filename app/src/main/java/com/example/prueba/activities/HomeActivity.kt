package com.example.prueba.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.prueba.R
import com.example.prueba.databinding.ActivityHomeBinding
import com.google.android.gms.maps.model.LatLng
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    lateinit var map: MapView
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private val REQUEST_LOCATION_PERMISSION = 1

    private val geocoder: Geocoder by lazy {
        Geocoder(this)
    }

    private val userLocationMarkers = ArrayList<Marker>()
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
    }
    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun createOverlayEvents(): MapEventsOverlay{

        return MapEventsOverlay(object: MapEventsReceiver{
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                if(p!= null){
                    longPressOnMap(p)
                }
                return true;
            }

        })
    }

    private fun longPressOnMap(p: GeoPoint) {

        val address = findAddress(LatLng(p.latitude, p.longitude))

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
                val userLocationMarker = createMarker(userGeoPoint, "Mi ubicacion", R.drawable.puntero2)
                userLocationMarkers.add(userLocationMarker)
                map.overlays.add(userLocationMarker)

            }
        }
    }

    private fun createMarker(p: GeoPoint, title: String, iconID: Int): Marker {

        val marker = Marker(map)
        marker.title = title
        val myIcon = ContextCompat.getDrawable(this,  iconID)
        marker.icon = myIcon
        marker.position = p
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        return marker

    }


}