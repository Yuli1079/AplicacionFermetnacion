package com.example.monitorieodefermentacion;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private TextView textoHum, textoTemp, textoPh, textoFermentacion;
    private ProgressBar progressBar;
    private Handler handler;
    private Runnable runnable;
    private boolean isFirstUpdate = true;
    private static final String CHANNEL_ID = "fermentation_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtener una instancia de la base de datos de Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        textoHum = findViewById(R.id.txtHumedad);
        textoTemp = findViewById(R.id.txtTemp);
        textoPh = findViewById(R.id.txtPh);
        textoFermentacion =  findViewById(R.id.txtFer);
        progressBar = findViewById(R.id.progressBar);

        // Crear el canal de notificación para versiones de Android superiores a Oreo
        createNotificationChannel();

        // Inicializar el handler y el runnable
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                // Verificar si es el primer update
                if (isFirstUpdate) {
                    // Ocultar la ProgressBar después de los primeros 10 segundos
                    progressBar.setVisibility(View.GONE);
                    isFirstUpdate = false;
                }

                // Actualizar los datos de Firebase cada 10 segundos
                actualizarDatosFirebase();
                handler.postDelayed(this, 10000); // Ejecutar de nuevo después de 10 segundos
            }
        };

        // Iniciar la actualización periódica de datos
        handler.postDelayed(runnable, 10000);
    }

    private void actualizarDatosFirebase() {
        // Obtener una referencia a la ubicación de tus datos en la base de datos
        DatabaseReference dataRef = mDatabase.child("Mediciones");
        mostrarNotificacion("Fermentación completada", "Si envio una notificacion.");

        // Leer los datos de Firebase
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Verifica si los datos existen
                if (dataSnapshot.exists()) {
                    // Obtén los valores de temperatura, acidez y humedad
                    Double temperatura = dataSnapshot.child("Temperatura").getValue(Double.class);
                    Double acidez = dataSnapshot.child("Acidez").getValue(Double.class);
                    Double humedad = dataSnapshot.child("Humedad").getValue(Double.class);

                    if (acidez >= 3.5 && acidez <= 4 && temperatura == 15 && humedad >= 45 && humedad <= 55) {
                        textoFermentacion.setText("Fermentación completada");

                        // Mostrar una notificación de alerta en la barra de notificaciones
                        mostrarNotificacion("Fermentación completada", "La fermentación ha finalizado exitosamente.");
                    } else if (acidez > 4 || temperatura != 15) {
                        textoFermentacion.setText("Falta Fermentación");
                    } else {
                        textoFermentacion.setText("Se excedió el tiempo de fermentación");
                    }

                    // Convierte los valores en cadenas de texto
                    String tempString = String.valueOf(temperatura);
                    String acidezString = String.valueOf(acidez);
                    String humedadString = String.valueOf(humedad);

                    // Muestra los valores en los TextView correspondientes
                    textoTemp.setText("Temperatura: " + tempString);
                    textoPh.setText("Acidez: " + acidezString);
                    textoHum.setText("Humedad: " + humedadString);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejar errores de lectura de datos
                Log.e("FirebaseError", "Error al leer los datos: " + databaseError.getMessage());
            }
        });
    }

    private void mostrarNotificacion(String title, String message) {
        // Crear la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background) // Agrega el ícono válido aquí
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Obtener el administrador de notificaciones
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Mostrar la notificación
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }



    private void createNotificationChannel() {
        // Crear el canal de notificación solo para versiones de Android superiores a Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "Fermentation Channel";
            String channelDescription = "Channel for fermentation notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, importance);
            channel.setDescription(channelDescription);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Detener la actualización periódica de datos al salir de la actividad
        handler.removeCallbacks(runnable);
    }
}
