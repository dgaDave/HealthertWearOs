package com.example.healthertwearos

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.*
import com.example.healthertwearos.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.firebase.Timestamp
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.lang.reflect.Field
import java.sql.Time
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.random.Random

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val db = Firebase.database.reference.child("medicionTr")

    private var dispositivo: String = ""
    private var usuarioCuidador: String = ""
    private lateinit var avisarButton: Button
    private val alertas = Firebase.firestore
    private var paciente: String = ""
    private lateinit var medicamentoTextView: TextView
    private var contador = 0
    private var medicamentos = mutableListOf<Medicamento>()
    private var latitud : Double = 0.0
    private var longitud : Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE)

        //Referencia en la cual registraremos los datos
        dispositivo = sharedPreferences.getString("dispositivo", "").toString()
        usuarioCuidador = sharedPreferences.getString("cuidador", "").toString()
        paciente = sharedPreferences.getString("nombre", "").toString()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        avisarButton = binding.alertaButton
        medicamentoTextView = binding.medicamentoTextView

        alertas.collection("medicamentos").whereEqualTo("paciente", dispositivo)
            .addSnapshotListener { snapshots, error ->

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            Log.e("a", "Nuevo medicamento: ${dc.document.data}")
                            val medicamento = dc.document.toObject(Medicamento::class.java)
                            medicamentos.add(medicamento)
                            medicamentos.sortBy { it.fechaLong }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            Log.e("m", "Modified data: ${dc.document.data}")
                            medicamentos.sortBy { it.fechaLong }

                        }
                        DocumentChange.Type.REMOVED -> {
                            Log.e("r", "Removed data: ${dc.document.data}")
                            val medicamentoRemover =
                                medicamentos.find { (it.nombreMedicamento == dc.document.data["nombreMedicamento"]) && (it.fechaLong == dc.document.data["fechaLong"]) }
                            medicamentos.remove(medicamentoRemover)
                            medicamentos.sortBy { it.fechaLong }
                        }
                    }
                }
                if (medicamentos.isNotEmpty()) {
                    val Hora = SimpleDateFormat("HH:mm").format(medicamentos.first().fechaLong)
                    medicamentoTextView.text = Hora.toString()
                }

            }


        //Intento de pulso
        val heartRateCallback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>, availability: Availability
            ) {
                if (availability is DataTypeAvailability) {
                    //Handle availability
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val hearRateBPM = data.getData(DataType.HEART_RATE_BPM).last().value
                db.child(dispositivo).child("bpm").setValue(hearRateBPM)
                // hearRateBPM = Random.nextInt(140, 171)
                //Log.e("pulso", "$hearRateBPM")
                contador++


                if (contador == 100) {

                    val data = hashMapOf(
                        "timestamp" to Timestamp.now(),
                        "bpm" to hearRateBPM,
                        "paciente" to dispositivo,
                        "fechaLong" to Timestamp.now().toDate().time
                    )
                    //Log.d("s", "Se subio historial")
                    alertas.collection("historial").add(data)
                    contador = 0

                    synchronized(medicamentos) {
                        val iterador = medicamentos.iterator()
                        while (iterador.hasNext()) {
                            val medicamento = iterador.next()
                            if (medicamento.fechaLong <= Timestamp.now().toDate().time) {
                                Log.e("Paso", "${medicamento.nombreMedicamento} a las ${medicamento.timestamp?.toDate()}")
                                iterador.remove()
                                val intento = Intent(this@MainActivity, MedicamentoActivity::class.java)
                                intento.putExtra("nombreMedicamento",medicamento.nombreMedicamento)
                                intento.putExtra("cantidadMedicamento",medicamento.cantidad.toString())
                                intento.putExtra("horaMedicamento",SimpleDateFormat("HH:mm").format(medicamento.timestamp?.toDate()))
                                startActivity(intento)
                            }
                        }
                        medicamentos.sortBy { it.fechaLong }
                        if (medicamentos.isNotEmpty()) {
                            val Hora = SimpleDateFormat("HH:mm").format(medicamentos.first().fechaLong)
                            medicamentoTextView.text = Hora.toString()
                        }
                    }
                }
                if (hearRateBPM > 150) {
                    val data = hashMapOf(
                        "visto" to false,
                        "timestamp" to Timestamp.now(),
                        "usuarioCuidador" to usuarioCuidador,
                        "nombrePaciente" to paciente,
                        "paciente" to dispositivo,
                        "tipo" to "automatica",
                        "latitud" to latitud,
                        "longitud" to longitud,
                        "fechaLong" to Timestamp.now().toDate().time
                    )

                    alertas.collection("alertas").add(data)
                }

            }
        }

        val vo2Callback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>, availability: Availability
            ) {
                if (availability is DataTypeAvailability) {
                    //Handle availability
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val vo2 = data.getData(DataType.VO2_MAX).first().value
                db.child(dispositivo).child("vo2").setValue(vo2)
                // hearRateBPM = Random.nextInt(140, 171)
                //Log.e("pulso", "$hearRateBPM")
                contador++


                /*if (contador == 100) {

                    val data = hashMapOf(
                        "timestamp" to Timestamp.now(),
                        "bpm" to hearRateBPM,
                        "paciente" to dispositivo,
                        "fechaLong" to Timestamp.now().toDate().time
                    )
                    //Log.d("s", "Se subio historial")
                    alertas.collection("historial").add(data)
                    contador = 0

                }
                if (hearRateBPM > 150) {
                    val data = hashMapOf(
                        "visto" to false,
                        "timestamp" to Timestamp.now(),
                        "usuarioCuidador" to usuarioCuidador,
                        "nombrePaciente" to paciente,
                        "paciente" to dispositivo,
                        "tipo" to "automatica",
                        "latitud" to latitud,
                        "longitud" to longitud,
                        "fechaLong" to Timestamp.now().toDate().time
                    )

                    alertas.collection("alertas").add(data)
                }*/

            }
        }

        val healthClient = HealthServices.getClient(applicationContext)
        val measureClient = healthClient.measureClient

// Register the callback.

        measureClient.registerMeasureCallback(DataType.Companion.HEART_RATE_BPM, heartRateCallback)
        measureClient.registerMeasureCallback(DataType.Companion.VO2_MAX,vo2Callback)

        //permisos
        val permiso1 = android.Manifest.permission.ACCESS_FINE_LOCATION
        val permiso2 = android.Manifest.permission.BODY_SENSORS
        val grant = PackageManager.PERMISSION_GRANTED


        //evento cuando se pide que de una localizacion
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                db.child(dispositivo).child("coordenadas").setValue(
                    mapOf(
                        "latitud" to locationResult.lastLocation?.latitude,
                        "longitud" to locationResult.lastLocation?.longitude
                    )
                )
                latitud= locationResult.lastLocation?.latitude!!
                longitud = locationResult.lastLocation?.longitude!!

            }
        }

        //Se piden permisos
        if (ContextCompat.checkSelfPermission(
                this, permiso1
            ) != grant || ContextCompat.checkSelfPermission(this, permiso2) != grant
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permiso1, permiso2), 1)
        } else {
            startLocationUpdates(
                LocationRequest.Builder(5000).setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setGranularity(
                        Granularity.GRANULARITY_FINE
                    ).build()
            )
        }
        //Alerta del boton
        avisarButton.setOnClickListener {

            val data = hashMapOf(
                "visto" to false,
                "timestamp" to Timestamp.now(),
                "usuarioCuidador" to usuarioCuidador,
                "nombrePaciente" to paciente,
                "paciente" to paciente,
                "tipo" to "manual",
                "latitud" to latitud,
                "longitud" to longitud,
                "fechaLong" to Timestamp.now().toDate().time
            )

            alertas.collection("alertas").add(data)


        }
    }

    //Al recibir los permisos, reinicia la actividad
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            requestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    Toast.makeText(this, "No puede funcionar la aplicacion", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    //empieza las actualizaciones de ubicacion
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(locationRequest: LocationRequest) {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }
}

data class Medicamento(
    val cantidad: Int = 0,
    val fechaLong: Long = 0,
    val nombreMedicamento: String = "",
    val paciente: String = "",
    val timestamp: Timestamp? = null
)