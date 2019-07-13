package com.cursoandroid;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {

    private final String TAG = WebServer.class.getCanonicalName();
    Context ctx;

    public interface WebserverListener { //Boolean ledStatus = false;
        Boolean getLedStatus();

        void switchLEDon();

        void switchLEDoff();

        int getNewValueFotoresistor();
    }

    private WebserverListener listener;

    public WebServer(int port, Context ctx, WebserverListener listener) {
        super(port);
        this.ctx = ctx;
        this.listener = listener;
        try {
            start();
            Log.i(TAG, "Webserver iniciado");
        } catch (IOException ioe) {
            Log.e(TAG, "No ha sido posible iniciar el webserver", ioe);
        }
    }

    private StringBuffer readFile() {
        BufferedReader reader = null;
        StringBuffer buffer = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(ctx.getAssets().open("home.html"), "UTF-8"));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                buffer.append(mLine);
                buffer.append("\n");
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error leyendo la página home", ioe);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error cerrando el reader", e);
                } finally {
                    reader = null;
                }
            }
        }
        return buffer;
    }

    private String lastValue = "0";

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, List<String>> parms = session.getParameters();
        // Analizamos los parámetros que ha modificado el usuario
        // Según estos parámetros, ejecutamos acciones en la RP3
        if (parms.get("on") != null) {
            listener.switchLEDon();
        } else if (parms.get("off") != null) {
            listener.switchLEDoff();
        }

        // Obtenemos la web original
        String preweb = readFile().toString();
        // Si queremos mostrar algún valor de salida, la modificamos
        // En este caso, sustituimos palabras clave por strings
        String postweb;
        if (listener.getLedStatus()) {
            postweb = preweb.replaceAll("#keytext", "ENCENDIDO");
            postweb = postweb.replaceAll("#keycolor", "MediumSeaGreen");
            postweb = postweb.replaceAll("#colorA", "#F2994A");
            postweb = postweb.replaceAll("#colorB", "#F2C94C");
        } else {
            postweb = preweb.replaceAll("#keytext", "APAGADO");
            postweb = postweb.replaceAll("#keycolor", "Tomato");
            postweb = postweb.replaceAll("#colorA", "#3e5151");
            postweb = postweb.replaceAll("#colorB", "#decba4");
        }

        if (parms.get("refreshvalue") != null) {
            DecimalFormat df = new DecimalFormat("0.00");
            String formatValueFotoresistor = df.format(listener.getNewValueFotoresistor()*0.01);
            lastValue = formatValueFotoresistor;
            postweb = postweb.replaceAll("#fotoresistor", formatValueFotoresistor);
        } else {
            postweb = postweb.replaceAll("#fotoresistor", lastValue);
        }

        return newFixedLengthResponse(postweb);
    }

}
