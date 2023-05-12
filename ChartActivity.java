package com.example.test02;
import org.json.*;

import android.os.Bundle;
import org.json.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Iterator;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import java.net.URI;
import java.net.URISyntaxException;
import tech.gusavila92.websocketclient.WebSocketClient;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import android.graphics.Color;


public class ChartActivity extends AppCompatActivity {
    private WebSocketClient webSocketClient;
    private LineChart lineChart1;
    private LineChart lineChart2;
    private LineDataSet lineDataSet1;
    private LineDataSet lineDataSet2;
    private LineData lineData1;
    private LineData lineData2;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        lineChart1 = findViewById(R.id.lineChart1);
        lineChart2 = findViewById(R.id.lineChart2);

        setupLineChart(lineChart1);
        setupLineChart(lineChart2);

        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());

        createWebSocketClient();
    }

    private void setupLineChart(LineChart lineChart) {
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);

        // Utwórz puste listy danych dla wykresu
        List<Entry> entries1 = new ArrayList<>();
        List<Entry> entries2 = new ArrayList<>();

        // Utwórz zestaw danych dla wykresu 1
        lineDataSet1 = new LineDataSet(entries1, "Device 1");
        lineDataSet1.setColor(Color.BLUE);
        lineDataSet1.setLineWidth(2f);
        lineDataSet1.setDrawCircles(false);
        lineDataSet1.setDrawValues(false);

        // Utwórz zestaw danych dla wykresu 2
        lineDataSet2 = new LineDataSet(entries2, "Device 2");
        lineDataSet2.setColor(Color.RED);
        lineDataSet2.setLineWidth(2f);
        lineDataSet2.setDrawCircles(false);
        lineDataSet2.setDrawValues(false);

        // Utwórz obiekt LineData i dodaj zestawy danych do niego
        lineData1 = new LineData(lineDataSet1);
        lineData2 = new LineData(lineDataSet2);

        // Przypisz LineData do wykresów
        lineChart1.setData(lineData1);
        lineChart2.setData(lineData2);
    }

    private void createWebSocketClient() {
        URI uri;
        try {
            uri = new URI("ws://192.168.88.252:8000/data");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                Log.i("WebSocket", "Session is starting");
                webSocketClient.send("Hello World!");
            }
            public String convertStandardJSONString(String data_json) {
                data_json = data_json.replaceAll("\\\\r\\\\n", "");
                data_json = data_json.replace("\\\"", "\"");

                data_json = data_json.replace("\"{", "{");
                data_json = data_json.replace("}\",", "},");
                data_json = data_json.replace("}\"", "}");
                return data_json;
            }

            @Override
            public void onTextReceived(String s) {
                Log.i("WebSocket", "Message received");
                List<Device> devices = new ArrayList<>();
                try {
                    JSONObject json = new JSONObject(convertStandardJSONString(s));
                    Iterator<String> keys = json.keys();
                    while (keys.hasNext()) {
                        String id = keys.next();
                        JSONObject deviceObject = json.getJSONObject(id);
                        Device device = new Device(id);
                        Iterator<String> sensorKeys = deviceObject.keys();
                        while (sensorKeys.hasNext()) {
                            String sensorId = sensorKeys.next();
                            JSONObject sensorObject = deviceObject.getJSONObject(sensorId);
                            Sensor sensor = new Sensor(sensorId, sensorObject.getString("timestamp"), sensorObject.getDouble("value"));
                            device.getSensors().add(sensor);
                        }
                        devices.add(device);
                    }

                    List<Entry> entries1 = new ArrayList<>();
                    List<Entry> entries2 = new ArrayList<>();

                    for (Device device : devices) {
                        List<Sensor> sensors = device.getSensors();
                        for (Sensor sensor : sensors) {
                            double value = sensor.getValue();
                            Date timestamp = dateFormat.parse(sensor.getTimestamp());
                            long timestampInSeconds = timestamp.getTime() / 1000;

                            if (device.getId().equals("1")) {
                                lineDataSet1.addEntry(new Entry(timestampInSeconds, (float) value));
                            } else if (device.getId().equals("2")) {
                                lineDataSet2.addEntry(new Entry(timestampInSeconds, (float) value));
                            }
                        }
                    }

                    lineDataSet1 = new LineDataSet(entries1, "Device 1");
                    lineDataSet1.setColor(Color.BLUE);
                    lineDataSet1.setLineWidth(2f);
                    lineDataSet1.setDrawCircles(false);
                    lineDataSet1.setDrawValues(false);

                    lineDataSet2 = new LineDataSet(entries2, "Device 2");
                    lineDataSet2.setColor(Color.RED);
                    lineDataSet2.setLineWidth(2f);
                    lineDataSet2.setDrawCircles(false);
                    lineDataSet2.setDrawValues(false);

                    lineData1 = new LineData(lineDataSet1);
                    lineData2 = new LineData(lineDataSet2);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lineChart1.setData(lineData1);
                            lineChart1.notifyDataSetChanged();
                            lineChart1.invalidate();

                            lineChart2.setData(lineData2);
                            lineChart2.notifyDataSetChanged();
                            lineChart2.invalidate();
                        }
                    });

                } catch (JSONException e) {
                    Log.e("WebSocket", "Error parsing JSON", e);
                } catch (ParseException e) {
                    Log.e("WebSocket", "Error parsing timestamp", e);
                }
            }


            class Sensor {
                private String id;
                private String timestamp;
                private double value;

                public Sensor(String id, String timestamp, double value) {
                    this.id = id;
                    this.timestamp = timestamp;
                    this.value = value;
                }

                public String getId() {
                    return id;
                }

                public String getTimestamp() {
                    return timestamp;
                }

                public double getValue() {
                    return value;
                }
            }

            class Device {
                private String id;
                private List<Sensor> sensors;

                public Device(String id) {
                    this.id = id;
                    this.sensors = new ArrayList<>();
                }

                public String getId() {
                    return id;
                }

                public List<Sensor> getSensors() {
                    return sensors;
                }
            }

            @Override
            public void onBinaryReceived(byte[] data) {}

            @Override
            public void onPingReceived(byte[] data) {}

            @Override
            public void onPongReceived(byte[] data) {}

            @Override
            public void onException(Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onCloseReceived() {
                Log.i("WebSocket", "Closed ");
            }
        };

        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }
}