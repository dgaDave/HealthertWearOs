package com.example.healthertwearos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.example.healthertwearos.databinding.ActivityVinculacionBinding
import com.google.firebase.firestore.FieldValue
import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class vinculacionActivity : Activity() {

    private lateinit var binding: ActivityVinculacionBinding
    private lateinit var vincularButton: Button
    private lateinit var codigoEditText: EditText
    private var db = Firebase.firestore
    private var dbTr = Firebase.database.reference.child("medicionTr")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        if (sharedPreferences.getString("dispositivo", null)!=null){
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
        binding = ActivityVinculacionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vincularButton=binding.vincularButton
        codigoEditText=binding.codigoEditText

        vincularButton.setOnClickListener {
            if (codigoEditText.text.length==6){
                Log.e("codigo", "bien")
                val codigo=codigoEditText.text.toString().uppercase()
                db.collection("users").whereEqualTo("codigo",codigo).get().addOnSuccessListener {documents ->

                    Log.d("Se entro","Se entro")
                    var dispositivo :String =""
                    for (document in documents){
                        Log.e("hola", "${document.id} => ${document.data}")
                        dispositivo = document.id
                        val nombrecMap = document.data["nombrec"] as HashMap<String,String>
                        val nombreC = nombrecMap["nombres"] + " " + nombrecMap["apellidoP"] + " " +nombrecMap["apellidoM"]
                        sharedPreferences.edit().putString("nombre",nombreC).apply()
                        sharedPreferences.edit().putString("curp", document.data["curp"].toString()).apply()
                        sharedPreferences.edit().putString("edad", document.data["edad"].toString()).apply()
                        sharedPreferences.edit().putString("altura", document.data["altura"].toString()).apply()
                        sharedPreferences.edit().putString("peso", document.data["peso"].toString()).apply()
                        sharedPreferences.edit().putString("alergias", document.data["alergias"].toString()).apply()
                        sharedPreferences.edit().putString("padecimientos", document.data["padecimientos"].toString()).apply()
                        sharedPreferences.edit().putString("cuidador",document.data["usuarioCuidador"].toString()).apply()
                        sharedPreferences.edit().putString("dispositivo",dispositivo.replace(".","")).apply()
                    }
                    db.collection("users").document(dispositivo).update("codigo",FieldValue.delete()).addOnCompleteListener {
                        startActivity(Intent(this,MainActivity::class.java))
                        finishAffinity()
                    }
                }.addOnFailureListener {
                    Log.e("e", "error")
                }
            }else{

            }
        }

    }
}