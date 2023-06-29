package com.example.climacast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RelativeLayout relativeLayoutHome;
    private ProgressBar LoadingPage;
    private ImageView BackGroundImage,SearchIcon,WeatherConditionIcon;
    private RecyclerView WeatherRv;
    private TextInputEditText cityEdit;
    private TextView Cityname,TempDisplay,Weathercond;
    private ArrayList<WeatherRVModel> weatherRVModelArrayList;
    private WeatherAdapter weatherAdapter;
    private LocationManager locationManager;
    private int PERMISSION_CODE=1;
    private String cityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        relativeLayoutHome = findViewById(R.id.relativeHome);
        LoadingPage = findViewById(R.id.Loading);
        cityEdit = findViewById(R.id.TextInputUser);
        WeatherRv = findViewById(R.id.RvWeather);
        BackGroundImage = findViewById(R.id.backgroundBlack);
        SearchIcon = findViewById(R.id.search);
        WeatherConditionIcon = findViewById(R.id.WeatherConditionImage);
        Cityname = findViewById(R.id.CityName);
        TempDisplay = findViewById(R.id.TemperatureDisplay);
        Weathercond = findViewById(R.id.WeatherCondition);

        //declares weatherRVmodel array list
        weatherRVModelArrayList = new ArrayList<>();
        //get the values in adapter from weatherrvmodel
        weatherAdapter = new WeatherAdapter(this,weatherRVModelArrayList);
        // set the adapter in recycler view
        WeatherRv.setAdapter(weatherAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                cityName = getCityName(location.getLongitude(), location.getLatitude());
                Log.d("Location", "City Name: " + cityName);
                getWeatherInfo(cityName);
            } else {
                // Use a default city
                cityName = "Delhi";
                Log.d("Location", "Location is null. Using default city: " + cityName);
                Toast.makeText(this, "Unable to retrieve location. Please make sure location services are enabled.", Toast.LENGTH_SHORT).show();
                getWeatherInfo(cityName);
            }
        }

       SearchIcon.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               String city = cityEdit.getText().toString();
               if (city.isEmpty()){
                   Toast.makeText(MainActivity.this, "Please enter city name", Toast.LENGTH_SHORT).show();
               }else {
                   Cityname.setText(cityName);
                   getWeatherInfo(city);
               }
           }
       });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==PERMISSION_CODE){
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Please provide the permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double longitude, double latitude)
    {
        String cityName = "Notfound";
        Geocoder gcd =  new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude,longitude,10);
            for (Address adr : addresses){
                if (adr!=null){
                    String city = adr.getLocality();
                    if (city!=null && !city.equals(" ")){
                        cityName = city;
                    }
                }
            }

        }catch (IOException e){
             e.printStackTrace();
        }
        return  cityName;
    }

    private void getWeatherInfo(String cityName){
        String url ="http://api.weatherapi.com/v1/forecast.json?key= 09e4ca9ab902448ea1f145920232306&q="+cityName+"&days=1&aqi=no&alerts=no";
        Cityname.setText(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest =  new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                LoadingPage.setVisibility(View.GONE);
                relativeLayoutHome.setVisibility(View.VISIBLE);
                weatherRVModelArrayList.clear();
                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    TempDisplay.setText(temperature+"°C");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("http:".concat(conditionIcon)).into(WeatherConditionIcon);
                    Weathercond.setText(condition);
                    if (isDay==1){
                        //morning
                        Picasso.get().load("https://wallpaperaccess.com/full/669543.jpg").into(BackGroundImage);
                    }else{
                        //night
                        Picasso.get().load("https://th.bing.com/th/id/OIP.R9xrZkLFPOVO-ajakPEBiwHaNK?pid=ImgDet&rs=1").into(BackGroundImage);
                    }
                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecastD = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecastD.getJSONArray("hour");
                    for (int i=0;i<hourArray.length();i++){
                            JSONObject hourobj = hourArray.getJSONObject(i);
                            String time = hourobj.getString("time");
                            String temp = hourobj.getString("temp_c");
                            String img = hourobj.getJSONObject("condition").getString("icon");
                            String wind = hourobj.getString("wind_kph");
                            weatherRVModelArrayList.add(new WeatherRVModel(time,temp,img,wind));
                            weatherAdapter.notifyDataSetChanged();
                    }


                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Please enter valid city name ", Toast.LENGTH_SHORT).show();
            }
        });
            requestQueue.add(jsonObjectRequest);

    }
}