package com.microsoft.xbox.idp.model;

import com.google.gson.GsonBuilder;
import com.microsoft.xbox.idp.model.serialization.UTCDateConverter;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class UserAccount {
    public String administeredConsoles;
    public Date dateOfBirth;
    public String email;
    public String firstName;
    public String gamerTag;
    public String gamerTagChangeReason;
    public Address homeAddressInfo;
    public String homeConsole;
    public String imageUrl;
    public boolean isAdult;
    public String lastName;
    public String legalCountry;
    public String locale;
    public String midasConsole;
    public boolean msftOptin;
    public String ownerHash;
    public String ownerXuid;
    public boolean partnerOptin;
    public Date touAcceptanceDate;
    public String userHash;
    public String userKey;
    public String userXuid;

    public static @NotNull GsonBuilder registerAdapters(@NotNull GsonBuilder gsonBuilder) {
        return gsonBuilder.registerTypeAdapter(Date.class, new UTCDateConverter.UTCDateConverterJSONDeserializer());
    }

    public static class Address {
        public String city;
        public String country;
        public String postalCode;
        public String state;
        public String street1;
        public String street2;
    }
}
