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
import android.os.Handler
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.prueba.R
import com.example.prueba.databinding.ActivityHomeBinding
import org.json.JSONArray
import org.json.JSONObject
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
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date

class HomeActivity : AppCompatActivity(), SensorEventListener {
    private val MIN_DISTANCE_FOR_UPDATE = 15.0
    private val earthRadius = 6371.0
    private val JSON_FILE_NAME = "locations_records.json"
    private lateinit var binding: ActivityHomeBinding
    lateinit var map: MapView
    private var currentMarker: Marker? = null
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var linearAceleration: Sensor
    private lateinit var orientationSensor: Sensor
    private lateinit var locationManager: LocationManager
    private var userLocationMarker: Marker? = null
    private val REQUEST_LOCATION_PERMISSION = 1
    private lateinit var addresEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var showRouteButton: Button
    private lateinit var userGeoPoint: GeoPoint
    private lateinit var direccion: String
    private var lastLocation: Location? = null
    private var jsonFile: File? = null

    private val geocoder: Geocoder by lazy {
        Geocoder(this)
    }

    private val userLocationMarkers = ArrayList<Marker>()

    /*********************************************/
    private val searchMarkers = ArrayList<Marker>()

    private lateinit var roadManager: RoadManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Cargar la informacion del mapa
        Configuration.getInstance().load(this,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
        //Estableces tipo de fuente
        map = binding.osmMap
        map.setTileSource(
            TileSourceFactory.
            MAPNIK)
        map.setMultiTouchControls(true)

        //AHabilitar Funcion multitouch
        map.overlays.add(createOverlayEvents())


        //Listeners para cuando el usuario presione el boton

         binding.notificaciones.setOnClickListener{
            startActivity(Intent(baseContext, NotificacionesActivity::class.java))
        }

        binding.perfil.setOnClickListener {
            startActivity(Intent(baseContext, PerfilActivity::class.java))
        }

        binding.tiendaPuntos.setOnClickListener {
            startActivity(Intent(baseContext, PuntosActivity::class.java))
        }

        //Inicializacion de los sensors (Luz, Rotacion y ...?

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        linearAceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        //Configuracion de carreteras
      roadManager = OSRMRoadManager(this,"ANDROID")
      val policy = ThreadPolicy.Builder().permitAll().build()
      StrictMode.setThreadPolicy(policy)
        showRouteButton = findViewById(R.id.showRouteButton)

        addresEditText = findViewById(R.id.addressEditText)
        searchButton = findViewById(R.id.searchButton)
        searchButton.setOnClickListener{

            val address = addresEditText.text.toString()
            searchLocation(address)
        }

    }

    val latitude = 4.62
    val longitude = -74.07
    val startPoint = GeoPoint(latitude, longitude)

    override fun onResume() {
        super.onResume()

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000,
                10.0f,
                locationListener
            )
        }

        map.onResume()
        map.controller.setZoom(10.0)
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if(location != null){
            val userGeoPoint = GeoPoint(location.latitude, location.longitude)
            map.controller.animateTo(userGeoPoint)
        }else{
            //No se cargara el mapa
        }

        showUserLocation()

        //Existe un destino en la barra de busqueda o se ha presionado en el mapa
        if(searchMarkers.isNotEmpty()){
            val destination = searchMarkers.firstOrNull()?.position
            if(destination != null){
                val userLocation = userLocationMarkers.firstOrNull()?.position
                if(userLocation != null){
                    drawRoute(userLocation, destination)
                }

            }
        }else{

            map.onResume()
            map.controller.setZoom(10.0)
            requestLocationPermission()
        }

        //Aqui se empiezan el listeners de los sensoneres
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, linearAceleration, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL)



    }
    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    //Comprueba si el usuario otorgo o denego el permiso de ubicacion y actua en consecuencia, cada vez que hay una respuesta a solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){

            REQUEST_LOCATION_PERMISSION->{

                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    //El usuario acepto conceder el permiso y se ysa su ubicacion
                }else{

                    //El usuario denego el permiso
                }
            }
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

        currentMarker?.title = ""
        val addressText = findAddress(p)
        val titleText: String = addressText?:""
       //Crear un marcador o actualizar el marcador existente

        if(currentMarker == null){
            currentMarker = createMarker(p, titleText, R.drawable.puntero2,"Unknown")
            searchMarkers.add(currentMarker!!)
            map.overlays.add(currentMarker)
        }else{
            currentMarker?.title = titleText
            currentMarker?.position = p
        }

        val userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if(userLocation != null){
            val userGeoPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
            val distance = calculateDistance(userGeoPoint, p)
            val distanceMessage = "Distancia total entre puntos: $distance km"
            Toast.makeText(this, distanceMessage,Toast.LENGTH_SHORT).show()
        }

        val address = findAddress(p)
        val snippet: String = address ?: ""
        searchMarkers.forEach{map.overlays.remove(it)}
        searchMarkers.clear()
        val marker = createMarker(p,snippet,R.drawable.puntero2,"Unknown")
        searchMarkers.add(marker)
        map.overlays.add(marker)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if(location != null){
                userGeoPoint = GeoPoint(location.latitude, location.longitude)
                updateRoute(userGeoPoint,p)
            }
        }else{
            requestLocationPermission()
        }

    }

    private fun calculateDistance(start: GeoPoint, finish: GeoPoint): Double{
        val dLat = Math.toRadians(finish.latitude - start.latitude)
        val dLng = Math.toRadians(finish.longitude - start.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(finish.latitude)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c

    }

    //Actualiza la ruta en el mapa para dos puntos geograficos

    private fun updateRoute(start: GeoPoint, finish: GeoPoint){

        var routePoints = ArrayList<GeoPoint>()
        routePoints.add(start)
        routePoints.add(finish)
        val road = roadManager.getRoad(routePoints)
        Log.i("Maps app","Route_Lenght: " + road.mLength + "klm")
        Log.i("Maps app","Duration: " + road.mDuration/60 + "min")

        if(map != null ){
            if(roadOverlay!=null){

                map.overlays.remove(roadOverlay)
            }
        }
        roadOverlay = RoadManager.buildRoadOverlay(road)
        roadOverlay!!.outlinePaint.color = Color.CYAN
        roadOverlay!!.outlinePaint.strokeWidth = 10f
        map.overlays.add(roadOverlay)
        map.invalidate()
    }

    private fun findAddress(latLng: GeoPoint): String? {

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

                userGeoPoint = GeoPoint(location.latitude,location.longitude)
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

        return marker

    }

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++//

    private val locationListener: LocationListener = object: LocationListener {
        override fun onLocationChanged(location: Location) {
            val latitude = location.latitude
            val longitude = location.longitude
            userGeoPoint = GeoPoint(latitude, longitude)

            userLocationMarkers.forEach { map.overlays.remove(it) }
            userLocationMarkers.clear()

            val address = "user"
            val userLocationMarker = createMarker(userGeoPoint, address, R.drawable.arrowofuser, direccion)

            userLocationMarkers.add(userLocationMarker)
            map.overlays.add(userLocationMarker)

            //Comprobar si existe un movimiento significativo
            if(lastLocation != null){
                val distance = lastLocation!!.distanceTo(location)
                if(distance > MIN_DISTANCE_FOR_UPDATE){

                    saveLocationRecord(location)
                }
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }
    }

    //Se guardan los registros de ubicacion en un archivo JSON
    private fun saveLocationRecord(location: Location){

        try{
            val locationRecord = JSONObject()
            locationRecord.put("latitude",location.latitude)
            locationRecord.put("longitude",location.longitude)
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            locationRecord.put("timestamp", currentTime)
            var jsonArray = JSONArray()
            var file = File(filesDir, JSON_FILE_NAME)
            if(file.exists()){
                val jsonStr = FileReader(file).readText()
                jsonArray = JSONArray(jsonStr)
            }
            jsonArray.put(locationRecord)

            FileWriter(file).use{it.write(jsonArray.toString())}
        }catch (e: Exception){
            e.printStackTrace()
        }

    }

    //Se muestra la ruta en el mapa basada en los registros de ubicacion guardados en el archivo .json
    private fun showLocationRoute(){

        if(jsonFile?.exists() == true) {
            val jsonStr = FileReader(jsonFile).readText()

            try {

                val jsonArray = JSONArray(jsonStr)
                val routePoints = ArrayList<GeoPoint>()
                for(i in 0 until jsonArray.length()){

                    val locationRecord = jsonArray.getJSONObject(i)
                    val latitude = locationRecord.getDouble("latitude")
                    val longitude = locationRecord.getDouble("longitude")
                    //Agrego la ubicacion a la lista de puntos de la ruta
                    routePoints.add(GeoPoint(latitude, longitude))
                }

                if(routePoints.size >= 2){
                    //Si es una linea que conecte a todos los puntos de una linea
                    val routePolyline = Polyline()
                    routePolyline.setPoints(routePoints)
                    routePolyline.color = Color.YELLOW
                    routePolyline.width = 5.0f
                    //Polilinea a mapa
                    map.overlays.add(routePolyline)
                    map.invalidate()
                    //Zoom para mostrar toda la ruta
                    map.zoomToBoundingBox(routePolyline.bounds, true)
                    //funcionalidad todp aca programar desaparicion despues de 5 segundos
                    Handler().postDelayed({

                        if(map.overlays.contains(routePolyline)){
                            map.overlays.remove(routePolyline)
                            map.invalidate()
                        }
                    },5000)

                }else{
                    Toast.makeText(this, "No hay suficientes registros de ubicacion para mostrar una ruta",Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }else{
            //Si no se encuentra el archivo .json, muestra un mensaje apropiado
            showAlertDialog("Archivo no encontrado","No se encontro el archivo JSON. ")

        }
    }

    private fun showAlertDialog(title: String, message: String){

        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Ok"){dialog, _-> dialog.dismiss()}
        builder.show()
    }
        private fun searchLocation(address: String) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val userLocation =
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                if (userLocation != null) {

                    val userGeoPoint = GeoPoint(userLocation.latitude, userLocation.longitude)

                    val geocodeResults = geocoder.getFromLocationName(address, 1)

                    if (geocodeResults != null && geocodeResults.isNotEmpty() && geocodeResults[0] != null) {

                        val foundAddress = geocodeResults[0]!!
                        val latitude = foundAddress.latitude
                        val longitud = foundAddress.longitude
                        val geoPoint = GeoPoint(latitude, longitud)
                        //Asigno la direccion como titulo del marcado

                        val addressAsTitle = foundAddress.getAddressLine(0)
                        //Llamada a la drawRoute antes de agregar el nuevo marcador.
                        drawRoute(userGeoPoint, geoPoint)
                        searchMarkers.forEach { map.overlays.remove(it) }
                        searchMarkers.clear()
                        val marker =
                            createMarker(geoPoint, addressAsTitle, R.drawable.puntero2, "Unknown")
                        searchMarkers.add(marker)
                        map.overlays.add(marker)
                        map.controller.animateTo(geoPoint)
                    } else {

                        Toast.makeText(this, "Direccion no encontrada", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "No se puede obtener la ubicacion actual",
                        Toast.LENGTH_SHORT
                    ).show()

                }}
            else{

                requestLocationPermission()

                }
            }

    private fun requestLocationPermission(){

        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){

            AlertDialog.Builder(this)
                .setTitle("Permiso de ubicacion necesaria")
                .setMessage("La aplicacion necesita accede a su ubicacion en el mapa")
                .setPositiveButton("Ok"){_,_ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION

                    )
                }
                .setNegativeButton("Cancelar"){_,_ ->

                }
                .show()
        }else{

            ActivityCompat.requestPermissions(
                this,
                      arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                      REQUEST_LOCATION_PERMISSION
            )
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

            userLocationMarker?.let {
                map.overlays.remove(it)
            }
            val rotationMatrix = FloatArray(9)
            val orientationValues = FloatArray(3)

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationValues)

            val azimuthRadians = orientationValues[0]

            val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()

            val adjustedAzimuth = if (azimuthDegrees < 0) azimuthDegrees + 360 else azimuthDegrees

            direccion = mapHeadingToDirection(adjustedAzimuth)

            val direccionTextView = findViewById<TextView>(R.id.direccion)

            direccionTextView.text = direccion

            createMarker(userGeoPoint, "User", R.drawable.arrowofuser,direccion)


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


