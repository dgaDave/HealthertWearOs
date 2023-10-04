package com.example.healthertwearos

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.example.healthertwearos.databinding.ActivityMedicamentoBinding

class MedicamentoActivity : Activity() {

    private lateinit var binding: ActivityMedicamentoBinding
    private lateinit var horaMedicamentoTextView: TextView
    private lateinit var medicamentoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMedicamentoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        horaMedicamentoTextView = binding.horaMedicamentoTextView
        medicamentoTextView = binding.medicamentoTextView

        horaMedicamentoTextView.text = intent.getStringExtra("horaMedicamento")
        medicamentoTextView.text = "Toma ${intent.getStringExtra("cantidadMedicamento")} de ${intent.getStringExtra("nombreMedicamento")}."



    }
}