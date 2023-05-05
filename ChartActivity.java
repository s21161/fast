package com.example.test02;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ChartActivity extends AppCompatActivity {

    private LineChart lineChart;
    private Socket socket;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        lineChart = findViewById(R.id.line_chart);

        // konfiguracja osi X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // odstęp między punktami na osi X

        // konfiguracja wykresu
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(Color.WHITE);

        // połączenie z serwerem websocket
        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            socket = IO.socket("http://0.0.0.0:8000/data", options);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                isConnected = true;
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                isConnected = false;
            }
        }).on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args.length > 0) {
                    String data = (String) args[0];
                    try {
                        JSONObject jsonObject = new JSONObject(data);
                        Iterator<String> keys = jsonObject.keys();

                        ArrayList<Entry> entries = new ArrayList<>();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            JSONObject values = jsonObject.getJSONObject(key);
                            Iterator<String> valuesKeys = values.keys();
                            while (valuesKeys.hasNext()) {
                                String time = valuesKeys.next();
                                float value = (float) values.getDouble(time);
                                entries.add(new Entry(Float.parseFloat(time), value));
                            }

                            LineDataSet dataSet = new LineDataSet(entries, key);
                            dataSet.setColor(Color.BLUE);
                            dataSet.setCircleColor(Color.BLUE);
                            dataSet.setLineWidth(2f);
                            dataSet.setCircleRadius(3f);
                            dataSet.setDrawCircleHole(false);
                            dataSet.setValueTextSize(10f);
                            dataSet.setValueTextColor(Color.BLUE);

                            LineData lineData = new LineData(dataSet);
                            lineChart.setData(lineData);
                            lineChart.invalidate();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        System.out.println("Błąd parsowania JSON: " + e.getMessage());
                    } finally {
                        System.out.println("Otrzymane dane: " + data);
                    }

                }
            }
        });

        socket.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.disconnect();
    }
}