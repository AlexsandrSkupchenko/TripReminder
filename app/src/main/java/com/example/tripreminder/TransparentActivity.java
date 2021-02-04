package com.example.tripreminder;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.tripreminder.RoomDataBase.TripTable;
import com.example.tripreminder.RoomDataBase.TripViewModel;
import com.example.tripreminder.serveses.FloatingViewService;

import java.util.Locale;

public class TransparentActivity extends AppCompatActivity {

    public static Ringtone rigntone;
    public static final String START_SERVICE = "com.example.tripreminder.StartService";
    public static final String SNOOZE_SERVICE = "com.example.tripreminder.SnoozeService";
    public static final String CANCEL_SERVICE = "com.example.tripreminder.CancelService";

    private String SOURCE_URL= "http://maps.google.com/maps?saddr=";
    private String DEST_URL= "http://maps.google.com/maps?daddr=";

    Intent myIntent;
    PendingIntent startPendingIntent;
    private TripViewModel tripViewModel;
    NotificationUtils notificationUtils;
    NotificationManager notificationManager;
    int idT;
    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            startFloatingIcon(note);
            return false;
        }
    });
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myIntent = getIntent();
        idT= (int) myIntent.getLongExtra("ID",-1);
        tripViewModel = new ViewModelProvider(TransparentActivity.this, ViewModelProvider.AndroidViewModelFactory.getInstance(TransparentActivity.this.getApplication())).get(TripViewModel.class);
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationUtils=new NotificationUtils(getApplicationContext());
        notificationManager=notificationUtils.getManager();
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage("Are you sure?");
        builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                idT= (int) myIntent.getLongExtra("ID",-1);
                notificationManager.cancel(idT);
                UpdateStatusByID(idT,"Canceled");
                finish();
                //todo:: cancel trip in database

            }
        });
        builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                createNotification(getApplicationContext());
                finish();

            }
        });
        builder.setNeutralButton("Start", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                idT= (int) myIntent.getLongExtra("ID",-1);
                notificationManager.cancel(idT);
                UpdateStatusByID(idT,"Done");
                startTrip();

                finish();

            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(dialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.colorPrim));
        dialog.getButton(dialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.colorPrim));
    }

    private void createNotification(Context context){
        Intent tapNotification = new Intent(getApplicationContext(), com.example.tripreminder.TransparentActivity.class).setAction(START_SERVICE);
        setTripDate(tapNotification);
        int id = myIntent.getIntExtra("ID",0);
        startPendingIntent = PendingIntent.getActivity(getApplicationContext(), id, tapNotification, PendingIntent.FLAG_ONE_SHOT);


         notificationUtils = new NotificationUtils(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationCompat.Builder nb = notificationUtils.getAndroidChannelNotification(myIntent.getStringExtra("tripName"),startPendingIntent);
            notificationUtils.getManager().notify(id, nb.build());

        }

//        try{
//
//            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//            rigntone = RingtoneManager.getRingtone(context, uri);
//            if(!rigntone.isPlaying())
//                rigntone.play();
//        }
//        catch(Exception e){}


    }
    String note;

    private String GetNotes(int id){
        tripViewModel.getNotes(id).observe(TransparentActivity.this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                note =s;
                handler.sendEmptyMessage(0);
            }
        });


        return note;
    }


    private void setTripDate(Intent pass){


        if((myIntent.hasExtra("sourceName"))){ // source exists
            Log.i("log", "not null myrec");
            pass.putExtra("sourceLat",myIntent.getDoubleExtra("sourceLat",0));
            pass.putExtra("sourceLon",myIntent.getDoubleExtra("sourceLon",0));
            pass.putExtra("sourceName",myIntent.getExtras().getString("sourceName","null"));
        }

        pass.putExtra("destinationLat", myIntent.getDoubleExtra("destinationLat",0));
        pass.putExtra("destinationLon",myIntent.getDoubleExtra("destinationLon",0));
        pass.putExtra("destinationName",myIntent.getExtras().getString("destinationName","null"));
        pass.putExtra("ID", myIntent.getIntExtra("ID", 0));
        pass.putExtra("tripName", myIntent.getStringExtra("tripName"));
        pass.putExtra("ways", myIntent.getBooleanExtra("ways",false));
    }

    private void startTrip(){

        Toast.makeText(getApplicationContext(), "start", Toast.LENGTH_SHORT).show();
        double sourceLat,sourceLon,destinationLat,destinationLon;
        String sourceName,destinationName;


        destinationLat = myIntent.getDoubleExtra("destinationLat", 0);
        destinationLon = myIntent.getDoubleExtra("destinationLon", 0);
        destinationName = myIntent.getStringExtra("destinationName");
        idT= (int) myIntent.getLongExtra("ID",-1);
        Intent mapIntent;
        String my_data;

        if(!myIntent.getExtras().getString("sourceName", "null").equals("null")){ // source exists
            Log.i("log", "not null notif");
            sourceLat = myIntent.getDoubleExtra("sourceLat",0);
            sourceLon = myIntent.getDoubleExtra("sourceLon",0);
            sourceName = myIntent.getStringExtra("sourceName");


            my_data= String.format(Locale.ENGLISH, SOURCE_URL+sourceLat+","+sourceLon+"("+sourceName+")&daddr="+destinationLat+","+destinationLon+"("+destinationName+")");
            mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(my_data));
            mapIntent.setPackage("com.google.android.apps.maps");
            mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }else{
            my_data= String.format(Locale.ENGLISH, DEST_URL+destinationLat+","+destinationLon+"("+destinationName+")");
            mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(my_data));
            mapIntent.setPackage("com.google.android.apps.maps");
            mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            System.out.println("null source");
        }
        GetNotes(idT);

        startActivity(mapIntent);

        if(myIntent.hasExtra("notificationID")){
            Log.i("log", "cancel notification ::::"+myIntent.getIntExtra("notificationID", 0)+"");
//            notificationManager.cancel(myIntent.getIntExtra("notificationID", 0));
//            MyReciever.rigntone.stop();
        }

        //Todo: delete this trip
    }

    private void startFloatingIcon(String notes){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startService(new Intent(getApplicationContext(), FloatingViewService.class).putExtra("Notes",notes));
        }else if (Settings.canDrawOverlays(this)) {
            startService(new Intent(getApplicationContext(), FloatingViewService.class).putExtra("Notes",notes));
        }else {
            Toast.makeText(this, "You need System Alert Window Permission to do this", Toast.LENGTH_SHORT).show();
        }
    }

    private void UpdateStatusByID(int idT,String status){
        new Thread(){
            @Override
            public void run() {
                TripTable table1= tripViewModel.getTripRowById((long) idT);
                table1.setStatus(status);
                table1.setId(idT);
                tripViewModel.update(table1);
            }
        }.start();

    }

}