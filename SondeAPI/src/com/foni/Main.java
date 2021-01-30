package com.foni;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Main {

    private static final String ClientID = "<Your SONDE Client ID>";
    private static final String ClientSecret = "<Your SONDE Client Secret>";
    private static final String ApiURL = "<Your SONDE API url>";
    private static final int EOT = 4;
    private static final int Port = 11111;

    public static void main(String[] args) {
        // Pas 1:
        String access_token = "";
        String JSON_Response = "";
        long expiresIn = 0;

        Request req;

        req = Request.MakeRequest("POST", ApiURL + "/platform/v1/oauth2/token")
                .addQuery("grant_type", "client_credentials")
                .addQuery("scope", "sonde-platform/users.write sonde-platform/scores.write sonde-platform/questionnaires.read sonde-platform/questionnaire-responses.write sonde-platform/storage.write")
                .addHeader("Authorization", "Basic " + EncodeCredentials())
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .send();

        JSON_Response = req.getMessage();
        req.disconnect();

        try {
            JSONObject obj = (JSONObject) new JSONParser().parse(JSON_Response);

            access_token = (String) obj.get("access_token");
            expiresIn = (long) obj.get("expires_in");
            String token_type = (String) obj.get("token_type");

            if(!token_type.equals("Bearer"))
                System.out.printf("Token type is not Bearer: actual value is %s\n", token_type);
        } catch (Exception e) {
            e.printStackTrace();

            System.out.println("ERROR: Could not get access key");
            return;
        }

        System.out.printf("STEP 1:\n\tAccess token: %s\n\tWill expire in: %d seconds\n", access_token, expiresIn);

        // Pas 2:
        String userID = "", requestID = "";
        User user = new User(User.Genders.MALE, 1985, "ENGLISH");

        req = Request.MakeRequest("POST", ApiURL + "/platform/v1/users")
                .addHeader("Authorization", access_token)
                .addHeader("Content-Type", "application/json")
                .send(user.toString());

        JSON_Response = req.getMessage();
        req.disconnect();

        try {
            JSONObject obj = (JSONObject) new JSONParser().parse(JSON_Response);

            requestID = (String) obj.get("requestId");
            user._userID = (String) obj.get("userIdentifier");
        } catch (Exception e) {
            e.printStackTrace();

            System.out.println("ERROR: Could not generate user");
            return;
        }

        System.out.printf("STEP 2:\n\tUser ID: %s\n\tRequest ID: %s\n", user._userID, requestID);

        // Pas 3:
        String signedURL = "", filePath = "";

        req = Request.MakeRequest("POST", ApiURL + "/platform/v1/storage/files")
                .addHeader("Authorization", access_token)
                .addHeader("Content-Type", "application/json")
                .send(user.createS3String(User.Codes.US));

        JSON_Response = req.getMessage();
        req.disconnect();

        try {
            JSONObject obj = (JSONObject) new JSONParser().parse(JSON_Response);

            requestID = (String) obj.get("requestId");
            signedURL = (String) obj.get("signedURL");
            filePath = (String) obj.get("filePath");
        } catch (Exception e) {
            e.printStackTrace();

            System.out.println("ERROR: Could not get S3 bucket upload link");
            return;
        }

        System.out.printf("STEP 3:\n\tSigned URL: %s\n\tFile Path: %s\n\tRequest ID: %s\n", signedURL, filePath, requestID);

        // Pas 4:

        try (Socket server = new Socket("[::1]", Port)) {

            OutputStream out = server.getOutputStream();
            out.write(signedURL.getBytes(StandardCharsets.UTF_8));

            InputStreamReader in = new InputStreamReader(server.getInputStream());

            int readChar;
            StringBuilder builder = new StringBuilder();

            while((readChar = in.read()) != EOT){
                builder.append((char) readChar);
            }

            if(builder.toString().equals("OK"))
                System.out.println("Ready to move on!");
            else {
                System.out.printf("ERROR: %s\n", builder.toString());
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();

            System.out.printf("ERROR: Could not connect to C# server on port %s\n", Port);
            return;
        }

        System.out.print("STEP 4:\n\tFile uploaded. Ready to continue.\n");

        // Pas 5:
        String requestName = "", questID = "", questTitle = "";
        List<String> questLang = new ArrayList<>();
        JSONObject questionnaire;

        req = Request.MakeRequest("GET", ApiURL + "/platform/v1/measures/name/respiratory-symptoms-risk")
                .addHeader("Authorization", access_token)
                //.sendEmpty()
                .connect();

        JSON_Response = req.getMessage();
        req.disconnect();

        try {
            JSONObject obj = (JSONObject) new JSONParser().parse(JSON_Response);

            requestID = (String) obj.get("id");
            requestName = (String) obj.get("name");
            questionnaire = (JSONObject) obj.get("questionnaire");

            questID = (String) questionnaire.get("id");
            questTitle = (String) questionnaire.get("title");
            questLang = (List<String>) questionnaire.get("languages");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(JSON_Response);

        // Pas 6:
        JSON_Response = Request.MakeRequest("GET", ApiURL + "/platform/v1/questionnaires/" + "qnr_e23er432w" + "?language=" + "en")
                .addHeader("Authorization", access_token)
                .connect()
                .getMessage();

        System.out.println(JSON_Response);

        // Pas 7:
//        H.clear();
//
//        H.replace("Authorization", access_token);
//        H.replace("Content-Type", "application/json");
    }

    private static String EncodeCredentials() {
        String credsText = ClientID + ":" + ClientSecret;

        return new String(new Base64().encode(credsText.getBytes()));
    }
}
