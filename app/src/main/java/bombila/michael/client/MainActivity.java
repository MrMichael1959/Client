package bombila.michael.client;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

//*************************************************************************************
public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        TextToSpeech.OnInitListener {
    //*************************************************************************************
    String _get_order_by_phoneUrl = "http://185.25.119.3/BombilaClient/get_order_by_phone.php";
    String _get_city_dataUrl = "http://185.25.119.3/BombilaClient/get_city_data.php";
    String _get_order_by_idUrl = "http://185.25.119.3/BombilaClient/get_order_by_id.php";
    String _set_orderUrl = "http://185.25.119.3/BombilaClient/set_order.php";
    String _upd_orderUrl = "http://185.25.119.3/BombilaClient/upd_order.php";
    String _directionsUrl = "https://maps.googleapis.com/maps/api/directions/json?";
    //    String _geocodeUrl    =         "https://maps.googleapis.com/maps/api/geocode/json?";
    String _key = "key=AIzaSyA0FZ6BTNZVDgcB44U7bNXOYKlh_AfCnV4";

    double _tarif = 1.0;
    double _cost_per_km = 5.0;
    double _cost_out_per_km = 8.0;
    int _min_km = 2;
    int _min_cost = 35;

    int _cost_total;
    String _order_id;
    String _on_time = "";
    String _PS = "";
    //String    _order_time;
    String _order_data;
    String _order_status = "none";
    String _city;
    String _driver_info;
    String _number_phone;

    int SIZE = 5;
    int PLACE_PICKER_REQUEST = 1;
    String STATUS = "none";
    boolean DAEMON = true;

    LinearLayout llMain, llPS, llOnTime;
    TextView tvCity, tvInfo, tvPS, tvTextPS, tvOnTime, tvTextOnTime, tvCallTaxi,
            tvOrderClose, tvOrderDelete;
    ImageView ivMenu, ivStatus, ivPS, ivOnTime;
    ProgressBar pb;

    LinearLayout[] ll = new LinearLayout[SIZE];
    TextView[] tv = new TextView[SIZE];
    ImageView[] iv = new ImageView[SIZE];
    ArrayList<MyPlace> places = new ArrayList<>();

    int placeId = 0;
    MyPlace place;

    TextToSpeech mTTS;
    Daemon daemon;


    Context context;

    //-------------------------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//-------------------------------------------------------------------------------------
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        _number_phone = getSharedPreferences("bombila_client_pref", MODE_PRIVATE)
                .getString("number_phone", "");

        if (_number_phone.equals("")) dialogPhone();

        mTTS = new TextToSpeech(this, this);
        daemon = new Daemon();

        initView();
        review();

//        init();

        new HttpPost().execute(_get_order_by_phoneUrl, _number_phone);

//        STATUS = _order_status;

    }
//--------------------------------------------------------------------------------------------------
    void init() {
//--------------------------------------------------------------------------------------------------
        try {
            getOrderInfo(new HttpPost().execute(_get_order_by_phoneUrl, _number_phone).get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (_order_status.equals("free")   ||
                _order_status.equals("accept") ||
                _order_status.equals("place")
                ) dialogStatus();
    }
//--------------------------------------------------------------------------------------------------
    @Override
    protected void onResume() {
//--------------------------------------------------------------------------------------------------
        super.onResume();
    }
    //--------------------------------------------------------------------------------------------------
    @Override
    protected void onStart() {
//--------------------------------------------------------------------------------------------------
        super.onStart();
    }
    //--------------------------------------------------------------------------------------------------
    @Override
    public void onInit(int status) {
//--------------------------------------------------------------------------------------------------
        // TODO Auto-generated method stub
        if (status == TextToSpeech.SUCCESS) {
            int result = mTTS.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Извините, этот язык не поддерживается");
            }
        } else {
            Log.e("TTS", "Ошибка!");
        }
    }
//--------------------------------------------------------------------------------------------------
    @Override
    public void onDestroy() {
//--------------------------------------------------------------------------------------------------
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
            mTTS = null;
        }
        daemon.cancel(false);
        super.onDestroy();
    }
//--------------------------------------------------------------------------------------------------
    @Override
    public void onBackPressed() {
//--------------------------------------------------------------------------------------------------
        super.onBackPressed();
    }
//--------------------------------------------------------------------------------------------------
    @Override
    public void onStop() {
//--------------------------------------------------------------------------------------------------
        super.onStop();
    }
//-------------------------------------------------------------------------------------
    @Override
    public void onClick(View v) {
//-------------------------------------------------------------------------------------
        boolean b = _order_status.equals("free")    ||
                _order_status.equals("accept")  ||
                _order_status.equals("place");

        switch (v.getId()) {
            case R.id.tvCallTaxi:
                setOrder("free");
                daemon.execute();
                DAEMON = false;
                break;
            case R.id.tvOrderDelete:
                daemon.cancel(false);
                updateOrder("delete");
                finish();
                break;
            case R.id.tvOrderClose:
                daemon.cancel(false);
                updateOrder("close");
                finish();
                break;
            case R.id.tv0:                      if (b) break;
                placeId = 0;
                MyPlacePicker();
                break;
            case R.id.tv1:                      if (b) break;
                placeId = 1;
                MyPlacePicker();
                break;
            case R.id.tv2:                      if (b) break;
                placeId = 2;
                MyPlacePicker();
                break;
            case R.id.tv3:                      if (b) break;
                placeId = 3;
                MyPlacePicker();
                break;
            case R.id.tv4:                      if (b) break;
                placeId = 4;
                MyPlacePicker();
                break;
            case R.id.tvOnTime:                 if (b) break;
                dialogTime();
                break;
            case R.id.tvPS:                     if (b) break;
                dialogPS();
                break;
            case R.id.iv1:
                deletePlace(1);
                break;
            case R.id.iv2:
                deletePlace(2);
                break;
            case R.id.iv3:
                deletePlace(3);
                break;
            case R.id.iv4:
                deletePlace(4);
                break;
            case R.id.ivPS:
                tvTextPS.setText("");
                llPS.setVisibility(View.GONE);
                break;
            case R.id.ivOnTime:
                tvTextOnTime.setText("");
                llOnTime.setVisibility(View.GONE);
                break;
            case R.id.ivMenu:                   if (b) break;
                dialogMenu();
                break;
            case R.id.llPhone:
                dialogPhone();
                break;
            case R.id.ivStatus:
                dialogStatus();
                break;
            default:
                break;
        }
    }
//--------------------------------------------------------------------------------------------------
    LatLng getLatLng(String addr){
//--------------------------------------------------------------------------------------------------
        Geocoder coder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        LatLng latlng = null;
        try {
            addresses = coder.getFromLocationName(addr, 1);
            if (addresses==null || addresses.size()==0) { return null; }
            latlng = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());

/*
            _city = new Geocoder(this, Locale.getDefault())
                    .getFromLocation(latlng.latitude,latlng.longitude,1)
                    .get(0)
                    .getLocality();
*/

        } catch (IOException e) {
            e.printStackTrace();
        }
        return latlng;
    }
//-------------------------------------------------------------------------------------
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//-------------------------------------------------------------------------------------
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place pp = PlacePicker.getPlace(this, data);
                String address = pp.getAddress().toString();

                String[] arr = address.split(", ");
                if (arr.length < 2) {
                    Toast.makeText(this, "Адрес не определен.", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    List<Address> addresses = new Geocoder(this, Locale.getDefault())
                    .getFromLocation(pp.getLatLng().latitude, pp.getLatLng().longitude,1);
                    if (addresses.size() == 0) {
                        Toast.makeText(this, "Адрес не определен.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    place = new MyPlace(pp.getLatLng(), address, addresses.get(0).getLocality());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (arr.length < 6) {
                    dialogHouse();
                    return;
                }

                if (placeId == 0) {
                    dialogParadnoe();
                }

                addPlace();
                review();
            }
        }
    }
//-------------------------------------------------------------------------------------
    void dialogPS() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.dialog_ps, null);
        final EditText et = (EditText) view.findViewById(R.id.etPS);

        final AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setCancelable(false)
                .setTitle("Примечание")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        _PS = et.getText().toString();
                        tvTextPS.setText(_PS);
                        if (_PS.equals("")) {
                            llPS.setVisibility(View.GONE);
                            return;
                        }
                        llPS.setVisibility(View.VISIBLE);
                    }
                });
        dialog = builder.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogMenu() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.dialog_menu, null);
        final LinearLayout llPhone = (LinearLayout) view.findViewById(R.id.llPhone);
        final LinearLayout llTaxi  = (LinearLayout) view.findViewById(R.id.llTaxi);
        final LinearLayout llExit  = (LinearLayout) view.findViewById(R.id.llExit);
        final TextView tvPhone     = (TextView) view.findViewById(R.id.tvPhone);
        tvPhone.setText(_number_phone);

        final AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view).setTitle("Меню").setCancelable(false);
        dialog = builder.create();
        dialog.show();

        llPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogPhone();
                dialog.cancel();
            }
        });
        llTaxi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        llExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                finish();
            }
        });
    }
//-------------------------------------------------------------------------------------
    void dialogStatus() {
//-------------------------------------------------------------------------------------
        String msg = "";
        final AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder .setTitle("Статус заказа");
        switch (_order_status) {
            case "none":
                msg = "Заказ не оформлен.";
                builder.setIcon(R.drawable.ic_status_white);
                break;
            case "free":
                msg = "Заказ обрабатывается.";
                builder.setIcon(R.drawable.ic_status_orange);
                break;
            case "accept":
                msg = "Заказ назначен.";
                builder.setIcon(R.drawable.ic_status_green);
                break;
            case "place":
                String[] arr = _driver_info.split("!");
                msg = "Машина на месте.";
                msg += "\nмарка: " + arr[0];
                msg += "\nцвет: "  + arr[1];
                msg += "\nномер: " + arr[2];
                msg += "\nтел.: "  + arr[3].replace(" ", "+");
                builder.setIcon(R.drawable.ic_status_red);
                break;
            case "delete":
                break;
            case "close":
                msg = "Заказ закрыт.";
                builder.setIcon(R.drawable.ic_status_grey);
                break;
            default:
                break;
        }
        builder.setMessage(msg);
        builder.setCancelable(false);

        if (_order_status.equals("place")) {
            builder.setNeutralButton("Позвонить", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    String number = String.format("tel:%s", _driver_info.split("!")[3]
                            .replace(" ", "+"));
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(number)));
                }
            });
        }
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                if (DAEMON && places.size()>1) {
                    daemon.execute();
                    DAEMON = false;
                }
            }
        });
        dialog = builder.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void statusSpeek() {
//-------------------------------------------------------------------------------------
        String msg = "";
        switch (_order_status) {
            case "none":
                msg = "Заказ не оформлен.";
                break;
            case "free":
                msg = "Заказ обрабатывается.";
                break;
            case "accept":
                msg = "Заказ назначен.";
                break;
            case "place":
                String[] arr = _driver_info.split("!");
                msg = "Машина на месте.";
                msg += "\nмарка: " + arr[0];
                msg += "\nцвет: " + arr[1];
                msg += "\nномер: ";
                if (arr[2].length() < 4) msg += arr[2];
                else msg += arr[2].substring(0,2) + " " + arr[2].substring(2);
                break;
            case "delete":
                break;
            case "close":
                msg = "Заказ закрыт.";
                break;
            default:
                break;
        }
        mTTS.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
    }
//-------------------------------------------------------------------------------------
    void dialogTime() {
//-------------------------------------------------------------------------------------
        TimePickerDialog tpd = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        String h = String.valueOf(hour);
                        String m = String.valueOf(minute);
                        if (hour < 10)   h = "0" + h;
                        if (minute < 10) m = "0" + m;
                        _on_time = h + ":" + m;
                        tvTextOnTime.setText(_on_time);
                        if (h.equals("")) {
                            llOnTime.setVisibility(View.GONE);
                            return;
                        }
                        llOnTime.setVisibility(View.VISIBLE);
                    }
                },
                java.util.Calendar.getInstance().getTime().getHours(),
                java.util.Calendar.getInstance().getTime().getMinutes(),
                true);
        tpd.show();
    }
//-------------------------------------------------------------------------------------
    void dialogParadnoe() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.dialog_paradnoe, null);
        final EditText et = (EditText) view.findViewById(R.id.et);

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Введите номер подъезда")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String txt = tv[0].getText() + " п." + et.getText().toString();
                        tv[0].setText(txt);
                        MyPlace p = places.get(0);
                        p.setAddress(txt + ", " + place.getAddress().split(", ", 3)[2]);
                        places.set(0, p);
                        et.setText("");
                        dialog.cancel();
                    }
                });
            add.setNeutralButton("Пропустить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                et.setText("");
                dialog.cancel();
            }
        });
        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogPhone() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.dialog_phone, null);
        final EditText et = (EditText) view.findViewById(R.id.et);
        et.setText(_number_phone);

        final AlertDialog dialog;
        AlertDialog.Builder add = new AlertDialog.Builder(this);
        add.setView(view)
                .setCancelable(false)
                .setTitle("Введите номер телефона")
                .setNegativeButton("Очистить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        et.setText(_number_phone = "");
                        dialogPhone();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        _number_phone = et.getText().toString()
                                .replace("+","")
                                .replace("-","")
                                .replace("(","")
                                .replace(")","")
                                .replace(" ","");
                        _number_phone = "+" + _number_phone;
                        if (_number_phone.length() != 13) {
                            dialogPhone();
                            return;
                        }
                        getSharedPreferences("bombila_client_pref", MODE_PRIVATE)
                                .edit()
                                .putString("number_phone", _number_phone)
                                .apply();
//                        dialog.cancel();
                    }
                });

        dialog = add.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void dialogHouse() {
//-------------------------------------------------------------------------------------
        final View view = getLayoutInflater().inflate(R.layout.dialog_house, null);
        final EditText et = (EditText) view.findViewById(R.id.et);
        et.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder .setView(view)
                .setTitle("Введите номер дома")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String address = place.getAddress();
                        String[] addr = address.split(", ", 2);

                        String house = et.getText().toString();
                        String text = addr[0] + ", " + house;
                        if (!house.equals("")) {
                            address =  text  +  ", " + addr[1];
                            LatLng ll = getLatLng(address);
                            place.setLatlng(ll);
                            place.setAddress(address);
                            if (placeId == 0) dialogParadnoe();
                            et.setText("");
                        } else {

                            tv[placeId].setText(addr[0]);
//                        place.setAddress(address);
//                            tv[placeId].setText(text);
//                        et.setText("");

//                        LatLng ll = getLatLng(address);
//                        place.setLatlng(ll);
//                            if (placeId == 0) dialogParadnoe();
                        }
                        addPlace();
                        review();

                        dialog.cancel();
                    }
                });
        dialog = builder.create();
        dialog.show();
    }
//-------------------------------------------------------------------------------------
    void MyPlacePicker() {
//-------------------------------------------------------------------------------------
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }
//-------------------------------------------------------------------------------------
    void review() {
//-------------------------------------------------------------------------------------
        int size = places.size();
        if (size < 2) {
            tvInfo.setText("");
            tvCallTaxi.setVisibility(View.GONE);
//            return;
        }
        calculate();
        switch (places.size()) {
            case 0:
                iv[0].setVisibility(View.GONE);
                iv[1].setVisibility(View.GONE);

                ll[0].setVisibility(View.VISIBLE);
                ll[1].setVisibility(View.VISIBLE);
                ll[2].setVisibility(View.GONE);
                ll[3].setVisibility(View.GONE);
                ll[4].setVisibility(View.GONE);
                break;
            case 1:
                iv[0].setVisibility(View.GONE);
                iv[1].setVisibility(View.GONE);

                ll[0].setVisibility(View.VISIBLE);
                ll[1].setVisibility(View.VISIBLE);
                ll[2].setVisibility(View.GONE);
                ll[3].setVisibility(View.GONE);
                ll[4].setVisibility(View.GONE);
                break;
            case 2:
                iv[0].setVisibility(View.GONE);
                iv[1].setVisibility(View.VISIBLE);
                iv[2].setVisibility(View.GONE);

                ll[0].setVisibility(View.VISIBLE);
                ll[1].setVisibility(View.VISIBLE);
                ll[2].setVisibility(View.VISIBLE);
                ll[3].setVisibility(View.GONE);
                ll[4].setVisibility(View.GONE);
                break;
            case 3:
                iv[0].setVisibility(View.GONE);
                iv[1].setVisibility(View.VISIBLE);
                iv[2].setVisibility(View.VISIBLE);
                iv[3].setVisibility(View.GONE);

                ll[0].setVisibility(View.VISIBLE);
                ll[1].setVisibility(View.VISIBLE);
                ll[2].setVisibility(View.VISIBLE);
                ll[3].setVisibility(View.VISIBLE);
                ll[4].setVisibility(View.GONE);
                break;
            case 4:
                iv[0].setVisibility(View.GONE);
                iv[1].setVisibility(View.VISIBLE);
                iv[2].setVisibility(View.VISIBLE);
                iv[3].setVisibility(View.VISIBLE);
                iv[4].setVisibility(View.GONE);

                ll[0].setVisibility(View.VISIBLE);
                ll[1].setVisibility(View.VISIBLE);
                ll[2].setVisibility(View.VISIBLE);
                ll[3].setVisibility(View.VISIBLE);
                ll[4].setVisibility(View.VISIBLE);
                break;
            case 5:
                iv[0].setVisibility(View.GONE);
                iv[1].setVisibility(View.VISIBLE);
                iv[2].setVisibility(View.VISIBLE);
                iv[3].setVisibility(View.VISIBLE);
                iv[4].setVisibility(View.VISIBLE);

                ll[0].setVisibility(View.VISIBLE);
                ll[1].setVisibility(View.VISIBLE);
                ll[2].setVisibility(View.VISIBLE);
                ll[3].setVisibility(View.VISIBLE);
                ll[4].setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }

    }
//-------------------------------------------------------------------------------------
    String httpPost(String... args) {
//-------------------------------------------------------------------------------------
        String resultString;
        String pars = "";
        for (int i=1; i<args.length; i++) {
            if (i == args.length-1) {
                pars += "par" + String.valueOf(i) + "=" + args[i];
            } else {
                pars += "par" + String.valueOf(i) + "=" + args[i] + "&";
            }
        }
        try {
            URL url = new URL(args[0]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            byte[] data = pars.getBytes("UTF-8");
            os.write(data); os.flush(); os.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }
            data = baos.toByteArray();
            baos.flush(); baos.close(); is.close();
            resultString = new String(data, "UTF-8");
            conn.disconnect();
        } catch (MalformedURLException e) { resultString = "MalformedURLException:" + e.getMessage();
        } catch (IOException e) { resultString = "IOException:" + e.getMessage();
        } catch (Exception e) { resultString = "Exception:" + e.getMessage();
        }
        return resultString;
    }
//-------------------------------------------------------------------------------------
    String httpGet(String... args) {
//-------------------------------------------------------------------------------------
        String resultString;
        StringBuilder pars = new StringBuilder();
        for (int i=0; i<args.length; i++) {
            if ( (i==0) || (i==args.length-1) ) {
                pars.append(args[i]);
            } else {
                pars.append(args[i]).append("&");
            }
        }
        try {
            URL url = new URL(pars.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }
            byte[] data = baos.toByteArray();
            baos.flush(); baos.close(); is.close();
            resultString = new String(data, "UTF-8");
            conn.disconnect();
        } catch (MalformedURLException e) { resultString = "MalformedURLException:" + e.getMessage();
        } catch (IOException e) { resultString = "IOException:" + e.getMessage();
        } catch (Exception e) { resultString = "Exception:" + e.getMessage();
        }
        return resultString;
    }
//-------------------------------------------------------------------------------------
    void addPlace() {
//-------------------------------------------------------------------------------------
        int count = places.size();
        if (count == 0) placeId = 0;
        if (placeId == 0) {
            _city = place.getLocality();
            tvCity.setText(_city);
            try {
                String str = new HttpPost().execute(_get_city_dataUrl, _city).get();
                if (!str.equals("error")) {
                    JSONObject res = new JSONObject(str);
                    _tarif = res.getDouble("tarif");
                    _cost_per_km = res.getDouble("cost_per_km");
                    _cost_out_per_km = res.getDouble("cost_out_per_km");
                    _min_km = res.getInt("min_km");
                    _min_cost = res.getInt("min_cost");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String address = place.getAddress();
        String[] ss = address.split(", ");
        String s = ss[0] + ", " + ss[1];
        if (!place.getLocality().equals(_city)) s += " (" + place.getLocality() + ")";

        if (placeId < count) {
            places.set(placeId, place);
            for (int i=0; i<SIZE; i++) {
                if (i > count) {
                    tv[i].setText("");
                    break;
                }
                if (i >= count) continue;
                String arr = places.get(i).getAddress();
                String[] split = arr.split(", ");
                String str = split[0] + ", " + split[1];
                if (!places.get(i).getLocality().equals(_city))
                    str += " (" + places.get(i).getLocality() + ")";
                tv[i].setText(str);

            }
            return;
        }
        places.add(count, place);
        for (int i=0; i<SIZE; i++) {
            if (i > count) {
                tv[i].setText("");
                break;
            }
            String arr = places.get(i).getAddress();
            String[] split = arr.split(", ");
            String str = split[0] + ", " + split[1];
            if (!places.get(i).getLocality().equals(_city))
                str += " (" + places.get(i).getLocality() + ")";
            tv[i].setText(str);

        }
    }
//-------------------------------------------------------------------------------------
    void deletePlace(int del) {
//-------------------------------------------------------------------------------------
        places.remove(del);
        for(int i=1; i<SIZE; i++) {
            if (i < places.size()) {
                String[] s = places.get(i).getAddress().split(", ");
                String s01 = s[0] + ", " + s[1];
                if (!places.get(i).getLocality().equals(_city))
                    s01 += " (" + places.get(i).getLocality() + ")";
                tv[i].setText(s01);
            }
            else tv[i].setText("");
        }
        review();
    }
//-------------------------------------------------------------------------------------
    void sleep(int milliseconds) {
//-------------------------------------------------------------------------------------
        try { TimeUnit.MILLISECONDS.sleep(milliseconds); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }
//-------------------------------------------------------------------------------------
    void showOrder() {
//-------------------------------------------------------------------------------------
        int n = places.size();
        for (int i=0; i<SIZE; i++) {
            iv[i].setVisibility(View.GONE);
            if (i < n) {
                String[] arr = places.get(i).getAddress().split(", ");
                String s = arr[0] + ", " + arr[1];
                if (!arr[2].equals(_city)) s += " (" + arr[2] + ")";
                if (arr.length < 6) s = arr[0];
                tv[i].setText(s);
                tv[i].setVisibility(View.VISIBLE);
            } else { ll[i].setVisibility(View.GONE); }
        }
        ivPS.setVisibility(View.GONE);
        ivOnTime.setVisibility(View.GONE);
        tvCallTaxi.setVisibility(View.GONE);
        tvOrderDelete.setVisibility(View.GONE);
        tvOrderClose.setVisibility(View.GONE);
        if (_order_status.equals("free")) {
            tvOrderDelete.setVisibility(View.VISIBLE);
            ivMenu.setImageResource(R.drawable.ic_menu_black);
            ivStatus.setImageResource(R.drawable.ic_status_orange);
        }
        if (_order_status.equals("accept")) {
            tvOrderDelete.setVisibility(View.VISIBLE);
            ivMenu.setImageResource(R.drawable.ic_menu_black);
            ivStatus.setImageResource(R.drawable.ic_status_green);
        }
        if (_order_status.equals("place")) {
            tvOrderClose.setVisibility(View.VISIBLE);
            ivMenu.setImageResource(R.drawable.ic_menu_black);
            ivStatus.setImageResource(R.drawable.ic_status_red);
        }
        if (_order_status.equals("close")) {
            tvOrderClose.setVisibility(View.VISIBLE);
            ivMenu.setImageResource(R.drawable.ic_menu_black);
            ivStatus.setImageResource(R.drawable.ic_status_grey);
        }
        tvCity.setText(_city);
        if (!_on_time.equals("")) {
            llOnTime.setVisibility(View.VISIBLE);
            tvTextOnTime.setText(_on_time);
        }
        if (!_PS.equals("")) {
            llPS.setVisibility(View.VISIBLE);
            tvTextPS.setText(_PS);
        }
        String s = "Сумма: " + Long.toString(_cost_total) + " грн.";
        tvInfo.setText(s);
    }
//-------------------------------------------------------------------------------------
    void updateOrder(String status) {
//-------------------------------------------------------------------------------------
        new HttpPost().execute(_upd_orderUrl, _order_id, status);
    }
//-------------------------------------------------------------------------------------
    void setOrder(String status) {
//-------------------------------------------------------------------------------------
        order_encode();
        try {
            new HttpPost().execute(
                    _set_orderUrl,
                    _number_phone,
                    _order_data,
                    status).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
//-------------------------------------------------------------------------------------
    void order_encode() {
//-------------------------------------------------------------------------------------
        JSONArray jlocalities = new JSONArray();
        JSONArray jaddresses  = new JSONArray();
        JSONArray jcoordes    = new JSONArray();
        JSONArray jcoord = null;
        for (MyPlace p : places) {
            try {
                double lat = p.getLatlng().latitude;
                double lon = p.getLatlng().longitude;
                jcoord = new JSONArray();
                jcoord.put(lat).put(lon);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jlocalities.put(p.getLocality());
            jaddresses.put(p.getAddress());
            jcoordes.put(jcoord);
        }
        JSONObject data = new JSONObject();
        try {
            data.put("city", _city);
            data.put("order_time", String.valueOf(Calendar.getInstance().getTimeInMillis()));
            data.put("cost_total", _cost_total);
            data.put("on_time", _on_time);
            data.put("PS", _PS);
            data.put("localities", jlocalities);
            data.put("addresses", jaddresses);
            data.put("coordes", jcoordes);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        _order_data = data.toString();
    }
//-------------------------------------------------------------------------------------
    void getOrderInfo(String response) {
//-------------------------------------------------------------------------------------
        try {
            JSONObject object = new JSONObject(response);
            _order_id = object.getString("id");
            _order_status = object.getString("status");
            _driver_info = object.getString("driver_info");

            String sdata   = object.getString("data");
            JSONObject data = new JSONObject(sdata);
            _city = data.getString("city");
            _on_time = data.getString("on_time");
            _PS = data.getString("PS");
            _cost_total = data.getInt("cost_total");
            JSONArray addresses = data.getJSONArray("addresses");
            JSONArray coordes   = data.getJSONArray("coordes");
            places.clear();
            int size = addresses.length();
            for (int i=0; i<size; i++) {
                MyPlace p = new MyPlace();
                p.setAddress(addresses.getString(i));
                JSONArray arr = coordes.getJSONArray(i);
                LatLng latLng = new LatLng(arr.getDouble(0), arr.getDouble(1));
                p.setLatlng(latLng);
                places.add(p);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
//-------------------------------------------------------------------------------------
    void calculate() {
//-------------------------------------------------------------------------------------
        int size = places.size();
        if (size < 2) {
            tvInfo.setText("");
            tvCallTaxi.setVisibility(View.GONE);
            return;
        }
/*
        String origin = "origin=" + addressEncode(places.get(0).getAddress());
        StringBuilder waypoints = new StringBuilder("waypoints=");
        for (int i=1; i<size-1; i++) {
            if (i != 1) waypoints.append("|");
            waypoints.append(addressEncode(places.get(i).getAddress()));
        }
        String destination = "destination=" + addressEncode(places.get(size-1).getAddress());
*/
        String origin = "origin=" +
                places.get(0).getLatlng().latitude + "," +
                places.get(0).getLatlng().longitude;

        StringBuilder waypoints = new StringBuilder("waypoints=");
        for (int i=1; i<size-1; i++) {
            if (i != 1) waypoints.append("|");
            waypoints.append(places.get(i).getLatlng().latitude + "," +
                             places.get(i).getLatlng().longitude);
        }

        String destination = "destination=" +
                places.get(size-1).getLatlng().latitude + "," +
                places.get(size-1).getLatlng().longitude;
        new HttpGet().execute(_directionsUrl,origin,destination, waypoints.toString(),_key);
    }
//-------------------------------------------------------------------------------------
void initView() {
//-------------------------------------------------------------------------------------
        ll[0] = (LinearLayout) findViewById(R.id.ll0);
        ll[1] = (LinearLayout) findViewById(R.id.ll1);
        ll[2] = (LinearLayout) findViewById(R.id.ll2);
        ll[3] = (LinearLayout) findViewById(R.id.ll3);
        ll[4] = (LinearLayout) findViewById(R.id.ll4);

        tv[0] = (TextView) findViewById(R.id.tv0);
        tv[1] = (TextView) findViewById(R.id.tv1);
        tv[2] = (TextView) findViewById(R.id.tv2);
        tv[3] = (TextView) findViewById(R.id.tv3);
        tv[4] = (TextView) findViewById(R.id.tv4);

        llMain       = (LinearLayout) findViewById(R.id.llMain);
        llPS         = (LinearLayout) findViewById(R.id.llPS);
        llOnTime     = (LinearLayout) findViewById(R.id.llOnTime);
        tvCity       = (TextView) findViewById(R.id.tvCity);
        tvInfo       = (TextView) findViewById(R.id.tvInfo);
        tvTextOnTime = (TextView) findViewById(R.id.tvTextOnTime);
        tvTextPS     = (TextView) findViewById(R.id.tvTextPS);
        tvOnTime     = (TextView) findViewById(R.id.tvOnTime);       tvOnTime.setOnClickListener(this);
        tvPS         = (TextView) findViewById(R.id.tvPS);           tvPS.setOnClickListener(this);
        tvCallTaxi   = (TextView) findViewById(R.id.tvCallTaxi);     tvCallTaxi.setOnClickListener(this);
        tvOrderClose = (TextView) findViewById(R.id.tvOrderClose);   tvOrderClose.setOnClickListener(this);
        tvOrderDelete= (TextView) findViewById(R.id.tvOrderDelete);  tvOrderDelete.setOnClickListener(this);
        llMain.setVisibility(View.VISIBLE);
        llPS.setVisibility(View.GONE);
        llOnTime.setVisibility(View.GONE);

        ivMenu   = (ImageView) findViewById(R.id.ivMenu);       ivMenu.setOnClickListener(this);
        ivStatus = (ImageView) findViewById(R.id.ivStatus);     ivStatus.setOnClickListener(this);
        ivPS     = (ImageView) findViewById(R.id.ivPS);         ivPS.setOnClickListener(this);
        ivOnTime = (ImageView) findViewById(R.id.ivOnTime);     ivOnTime.setOnClickListener(this);

        iv[0] = (ImageView) findViewById(R.id.iv0);
        iv[1] = (ImageView) findViewById(R.id.iv1);
        iv[2] = (ImageView) findViewById(R.id.iv2);
        iv[3] = (ImageView) findViewById(R.id.iv3);
        iv[4] = (ImageView) findViewById(R.id.iv4);

        pb = (ProgressBar) findViewById(R.id.pb);

        for(int i=0; i<SIZE; i++) {
            tv[i].setOnClickListener(this);
            iv[i].setOnClickListener(this);
        }

        placeId = 0;
    }
//-------------------------------------------------------------------------------------
    String addressEncode(String address) {
//-------------------------------------------------------------------------------------
        String[] arr = address.split(", ");
        int size = arr.length - 2;
        StringBuilder result = new StringBuilder();
        for (int i=0; i<size; i++) {
            if (i != 0) result.append("+");
            result.append(arr[i]);
        }
        return result.toString().replace(" ", "+");
    }
//*************************************************************************************
    public class HttpGet extends AsyncTask<String,Void,String> {
//*************************************************************************************
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... args) {
            String resultString;
            StringBuilder pars = new StringBuilder();
            for (int i=0; i<args.length; i++) {
                if ( (i==0) || (i==args.length-1) ) {
                    pars.append(args[i]);
                } else {
                    pars.append(args[i]).append("&");
                }
            }
            try {
                URL url = new URL(pars.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }
                byte[] data = baos.toByteArray();
                baos.flush(); baos.close(); is.close();
                resultString = new String(data, "UTF-8");
                conn.disconnect();
            } catch (MalformedURLException e) { resultString = "MalformedURLException:" + e.getMessage();
            } catch (IOException e) { resultString = "IOException:" + e.getMessage();
            } catch (Exception e) { resultString = "Exception:" + e.getMessage();
            }
            return resultString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                JSONObject jobj = new JSONObject(result);
/*                if (jobj.has("results")) {
                    jobj = jobj
                            .getJSONArray("results")
                            .getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location");
                    place.setLatlng(
                            new LatLng(jobj.getDouble("lat"), jobj.getDouble("lng"))
                    );

                    if (placeId == 0) dialogParadnoe();
                    addPlace();
                    review();
                }
*/
                if (jobj.has("routes")) {
                    JSONArray legs = jobj
                            .getJSONArray("routes")
                            .getJSONObject(0)
                            .getJSONArray("legs");
                    int l = 0;
                    for (int i=0; i<legs.length(); i++){
                        JSONObject j = legs.getJSONObject(i).getJSONObject("distance");
//                        String text = j.getString("text");
                        l += j.getLong("value");
                    }
                    if (l < 1000) l = 1000;

                    double per_km;
//                    String[] arr = places.get(places.size()-1).getAddress().split(", ");
//                    String end_address = arr[2];
                    if (_city.equals(places.get(places.size()-1).getLocality())) {
                        per_km = _cost_per_km;
                    }
                    else per_km = _cost_out_per_km;
                    _cost_total = _min_cost + (int) (per_km * (l / 1000 - _min_km + 1));

                    String s = "Сумма: " + Long.toString(_cost_total) + " грн.";
                    tvInfo.setText(s);
                    tvCallTaxi.setVisibility(View.VISIBLE);
                }
                pb.setVisibility(View.GONE);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
//*************************************************************************************
    public class Daemon extends AsyncTask<Void,String,Void> {
//*************************************************************************************
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected Void doInBackground(Void... values) {

            while (true) {

                if (isCancelled()) {
                    break;
                }

                String order = httpPost(_get_order_by_idUrl, _order_id);
                getOrderInfo(order);
                publishProgress("order");
                sleep(5000);
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            if(values[0].equals("order")) {
                showOrder();

                if (!STATUS.equals(_order_status)) {
                    STATUS = _order_status;
                    statusSpeek();
                }
            }
        }
    }
//*************************************************************************************
    public class HttpPost extends AsyncTask<String,Void,String> {
//*************************************************************************************
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pb.setVisibility(View.VISIBLE);
        }
        @Override
        protected String doInBackground(String... args) {
            String resultString;
            String pars = "";
            for (int i=1; i<args.length; i++) {
                if (i == args.length-1) {
                    pars += "par" + String.valueOf(i) + "=" + args[i];
                } else {
                    pars += "par" + String.valueOf(i) + "=" + args[i] + "&";
                }
            }
            try {
                URL url = new URL(args[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                byte[] data = pars.getBytes("UTF-8");
                os.write(data); os.flush(); os.close();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }
                data = baos.toByteArray();
                baos.flush(); baos.close(); is.close();
                resultString = new String(data, "UTF-8");
                conn.disconnect();
            } catch (MalformedURLException e) { resultString = "MalformedURLException:" + e.getMessage();
            } catch (IOException e) { resultString = "IOException:" + e.getMessage();
            } catch (Exception e) { resultString = "Exception:" + e.getMessage();
            }
            return resultString;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            pb.setVisibility(View.GONE);
            if (result.equals("error")) {
                return;
            }
            try {
                if (new JSONObject(result).has("tarif")) return;
            } catch (JSONException e) {
                e.printStackTrace();
            }

            getOrderInfo(result);
            showOrder();
            if (_order_status.equals("free")   ||
                    _order_status.equals("accept") ||
                    _order_status.equals("place")
                    ) dialogStatus();
            STATUS = _order_status;
        }
    }
//*************************************************************************************
    public class MyPlace {
//*************************************************************************************
        private LatLng latlng;
        private String address;
        private String locality;

        MyPlace(LatLng latlng, String address, String locality) {
            this.latlng = latlng;
            this.address = address;
            this.locality = locality;
        }
        MyPlace() {}
        String getLocality() { return locality; }
        String getAddress() {
            return address;
        }
        LatLng getLatlng() {
            return latlng;
        }

        void setLatlng(LatLng latlng) {
            this.latlng = latlng;
        }
        void setAddress(String address) {
            this.address = address;
        }
        void setLocality(String locality) { this.locality = locality; }
    }
//*************************************************************************************
}
//*********** End ActivityMain class **************************************************
