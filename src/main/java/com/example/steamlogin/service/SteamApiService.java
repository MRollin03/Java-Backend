package com.example.steamlogin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.github.cdimascio.dotenv.Dotenv;

@Service
public class SteamApiService {

    private String steamApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetch Steam user profile information
     * @param steamId The user's 64-bit Steam ID
     * @return SteamUserProfile object with user details
     */
    public SteamUserProfile getUserProfile(String steamId) {

        steamApiKey = Dotenv.load().get("STEAM_API_KEY");
        String url = String.format(
                "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s",
                steamApiKey,
                steamId
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode player = root.path("response").path("players").get(0);

            if (player == null) {
                throw new RuntimeException("Player not found");
            }

            SteamUserProfile profile = new SteamUserProfile();
            profile.setSteamId(player.path("steamid").asText());
            profile.setUsername(player.path("personaname").asText());
            profile.setProfileUrl(player.path("profileurl").asText());
            profile.setAvatarUrl(player.path("avatar").asText());
            profile.setAvatarMediumUrl(player.path("avatarmedium").asText());
            profile.setAvatarFullUrl(player.path("avatarfull").asText());
            profile.setRealName(player.path("realname").asText(null));
            profile.setCountryCode(player.path("loccountrycode").asText(null));

            return profile;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Steam profile: " + e.getMessage(), e);
        }
    }

    /**
     * Inner class to hold Steam user profile data
     */
    public static class SteamUserProfile {
        private String steamId;
        private String username;
        private String profileUrl;
        private String avatarUrl;
        private String avatarMediumUrl;
        private String avatarFullUrl;
        private String realName;
        private String countryCode;

        // Getters and Setters
        public String getSteamId() { return steamId; }
        public void setSteamId(String steamId) { this.steamId = steamId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getProfileUrl() { return profileUrl; }
        public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }

        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

        public String getAvatarMediumUrl() { return avatarMediumUrl; }
        public void setAvatarMediumUrl(String avatarMediumUrl) { this.avatarMediumUrl = avatarMediumUrl; }

        public String getAvatarFullUrl() { return avatarFullUrl; }
        public void setAvatarFullUrl(String avatarFullUrl) { this.avatarFullUrl = avatarFullUrl; }

        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        @Override
        public String toString() {
            return "SteamUserProfile{" +
                    "steamId='" + steamId + '\'' +
                    ", username='" + username + '\'' +
                    ", avatarUrl='" + avatarUrl + '\'' +
                    ", realName='" + realName + '\'' +
                    ", countryCode='" + countryCode + '\'' +
                    '}';
        }
    }
}