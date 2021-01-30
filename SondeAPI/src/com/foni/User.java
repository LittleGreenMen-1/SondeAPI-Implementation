package com.foni;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class User {
    public String _userID;
    private final String _gender;
    private final int _yearOfBirth;
    private final String _language;

    public enum Genders {
        MALE, FEMALE, OTHER
    }

    public enum Codes {
        US, IN, DE
    }

    public User(Genders gender, int yearOfBirth, String language) {
        _gender = String.valueOf(gender);
        _yearOfBirth = yearOfBirth;
        _language = language;
    }

    public String createS3String(Codes countryCode) {
        return String.format("{\"fileType\": \"wav\", \"countryCode\": \"%s\", \"userIdentifier\": \"%s\"}",
                countryCode, _userID);
    }

    public String toString() {
        // {"gender":"MALE", "yearOfBirth":"1985", "language": "ENGLISH"}
        return String.format("{\"gender\":\"%s\", \"yearOfBirth\":\"%d\", \"language\": \"%s\"}",
                _gender, _yearOfBirth, _language);
    }

    public void Save(String filename, String uniqueID) throws IOException {
        FileOutputStream out = new FileOutputStream(filename);
        String stringToWrite = String.format("%s: %s%s\n", uniqueID, toString(), _userID);

        out.write(stringToWrite.getBytes(StandardCharsets.UTF_8));
    }

//    public static User Load(String filename, String uniqueID) {
//
//    }
}
