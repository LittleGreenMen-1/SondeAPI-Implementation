package com.foni;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

public class Request {

    private HttpsURLConnection _Connection;
    private final StringJoiner _FinalQuery;
    private boolean _Connected;

    private Request() {
        _FinalQuery = new StringJoiner("&");
        _Connected = false;
    }

    private Request(HttpsURLConnection con) {
        _Connection = con;

        _FinalQuery = new StringJoiner("&");
        _Connected = false;
    }

    public static Request MakeRequest(String method, String url) {
        try {
            URL connection_url = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) connection_url.openConnection();

            connection.setRequestMethod(method);
            connection.setDoOutput(true);

            return new Request(connection);
        } catch (Exception e) {
            e.printStackTrace();

            return new Request();
        }
    }

    public Request addHeader(String key, String value) {
        _Connection.addRequestProperty(key, value);

        return this;
    }

    public Request addQuery(String key, String value) {
        _FinalQuery.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" +
                URLEncoder.encode(value, StandardCharsets.UTF_8));

        return this;
    }

    public Request connect() {
        try {
            if (!_Connected) {
                _Connection.connect();
                _Connected = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public void disconnect() {
        _Connection.disconnect();
    }

    public Request send(byte[] message) {
        try {
            // _Connection.setFixedLengthStreamingMode(message.length);

            if(!_Connected) {
                _Connection.connect();
                _Connected = true;
            }

            _Connection.getOutputStream().write(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public Request send() { return send(_FinalQuery.toString()); }
    public Request send(String message) { return send(message.getBytes(StandardCharsets.UTF_8)); }
    public Request sendEmpty() { return send(""); }

    public String getMessage() {
        try {
            if(!_Connected) {
                _Connection.connect();
                _Connected = true;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(_Connection.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();

            while ((inputLine = in.readLine()) != null) content.append(inputLine);

            in.close();
            return content.toString();
        } catch (Exception e) {
            e.printStackTrace();

            return "";
        }
    }
}
