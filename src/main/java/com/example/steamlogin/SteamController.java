package com.example.steamlogin;

import Database.PostgresJDBC;
import com.example.steamlogin.service.SteamApiService;
import com.example.steamlogin.service.SteamApiService.SteamUserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openid4java.consumer.*;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.UUID;

@RestController
public class SteamController {

    private static final String STEAM_OPENID = "https://steamcommunity.com/openid";

    @Autowired
    private PostgresJDBC database;

    @Autowired
    private SteamApiService steamApiService;

    // USER DATA TRANSFER OBJECT
    public record UserSessionDto(
            int userId,
            String steamId,
            String username,
            String avatar,
            int coins,
            int xp
    ) {}

    @GetMapping("/auth/steam")
    public void login(HttpServletRequest request, HttpServletResponse response) throws Exception {

        ConsumerManager manager = new ConsumerManager();

        manager.setMaxAssocAttempts(0);

        List<DiscoveryInformation> discoveries = manager.discover(STEAM_OPENID);

        DiscoveryInformation discovered = discoveries.get(0);

        request.getSession().setAttribute("steam-disc", discovered);

        String returnToUrl = request.getScheme() + "://" +
                request.getServerName() +
                (request.getServerPort() == 80 ? "" : ":" + request.getServerPort()) +
                request.getContextPath() +
                "/auth/steam/return";

        AuthRequest authReq = manager.authenticate(discovered, returnToUrl);

        response.sendRedirect(authReq.getDestinationUrl(true));
    }


    @GetMapping("/auth/steam/return")
    public void verify(HttpServletRequest request, HttpServletResponse httpResponse) throws Exception {

        ParameterList openidResponse = new ParameterList(request.getParameterMap());

        DiscoveryInformation discovered =
                (DiscoveryInformation) request.getSession().getAttribute("steam-disc");

        StringBuffer receivingURL = request.getRequestURL();
        String queryString = request.getQueryString();

        if (queryString != null && !queryString.isEmpty()) {
            receivingURL.append("?").append(queryString);
        }

        ConsumerManager manager = new ConsumerManager();
        VerificationResult verification = manager.verify(
                receivingURL.toString(),
                openidResponse,
                discovered
        );

        Identifier verified = verification.getVerifiedId();

        if (verified == null) {
            httpResponse.sendRedirect("http://localhost:5173//login?error=steam_failed");
            return;
        }

        String steamId = verified.getIdentifier()
                .replace("https://steamcommunity.com/openid/id/", "");

        try {
            SteamUserProfile profile = steamApiService.getUserProfile(steamId);

            int userId = database.findOrCreateUser(
                    steamId,
                    profile.getUsername(),
                    profile.getAvatarFullUrl()
            );

            request.getSession().setAttribute("userId", userId);
            request.getSession().setAttribute("steamId", steamId);
            request.getSession().setAttribute("username", profile.getUsername());
            request.getSession().setAttribute("avatar", profile.getAvatarMediumUrl());

            httpResponse.sendRedirect("http://localhost:5173/profile");

        } catch (Exception e) {
            e.printStackTrace();
            httpResponse.sendRedirect("http://localhost:5173//login?error=server_error");
        }
    }

    @GetMapping("/api/me")
    public Object me(HttpServletRequest request) {

        Integer userId = (Integer) request.getSession().getAttribute("userId");

        if (userId == null) {
            return null;
        }

        return new UserSessionDto(
                userId,
                (String) request.getSession().getAttribute("steamId"),
                (String) request.getSession().getAttribute("username"),
                (String) request.getSession().getAttribute("avatar"),
                request.getSession().getAttribute("coins") == null ? 0 : (Integer) request.getSession().getAttribute("coins"),
                request.getSession().getAttribute("xp") == null ? 0 : (Integer) request.getSession().getAttribute("xp")

        );
    }

    @Configuration
    public class SessionConfig {

        @Bean
        public WebMvcConfigurer corsConfigurer() {
            return new WebMvcConfigurer() {
                @Override
                public void addCorsMappings(CorsRegistry registry) {
                    registry.addMapping("/**")
                            .allowedOrigins("http://localhost:5173")
                            .allowCredentials(true)
                            .allowedMethods("*");
                }
            };
        }
    }



}