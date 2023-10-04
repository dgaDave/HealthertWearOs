package com.example.healthertwearos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class MyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Toast.makeText(context,"Notificacion",Toast.LENGTH_LONG).show()
        Log.e("x","di")
        val intento = Intent(context,MainActivity2::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intento)


            }

}