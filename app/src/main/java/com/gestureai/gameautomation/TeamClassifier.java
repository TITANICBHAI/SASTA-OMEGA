package com.gestureai.gameautomation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.*;
import com.gestureai.gameautomation.utils.NLPProcessor;

/**
 * Advanced team classification for multiplayer games
 */
public class TeamClassifier {
    private static final String TAG = "TeamClassifier";

    private Context context;

    private TensorFlowLiteHelper tfliteHelper;
    private Map<TeamAffiliation, TeamProfile> teamProfiles;
    private NLPProcessor nlpProcessor;
    private boolean isInitialized = false;

    public enum TeamAffiliation {
        FRIENDLY, ENEMY, NEUTRAL, UNKNOWN
    }

    public static class TeamProfile {
        public float[] primaryColor; // RGB values 0-1
        public float[] secondaryColor;
        public List<String> visualIndicators;
        public float[] nametagColor;
        public boolean hasOutline;
        public float outlineThickness;
        public Map<String, Float> uiElements;

        public TeamProfile() {
            this.primaryColor = new float[3];
            this.secondaryColor = new float[3];
            this.nametagColor = new float[3];
            this.visualIndicators = new ArrayList<>();
            this.uiElements = new HashMap<>();
        }
    }

    public static class PlayerClassification {
        public int playerId;
        public TeamAffiliation team;
        public float confidence;
        public Rect playerRegion;
        public float[] dominantColors;
        public List<String> detectedIndicators;
        public boolean hasNametag;
        public String nametagText;

        public float threatLevel;

        public PlayerClassification(int playerId, Rect region) {
            this.playerId = playerId;
            this.playerRegion = region;
            this.dominantColors = new float[3];
            this.detectedIndicators = new ArrayList<>();
        }
    }

    public static class TeamAnalysisResult {
        public List<PlayerClassification> players;
        public int friendlyCount;
        public int enemyCount;
        public int neutralCount;
        public int unknownCount;
        public float overallConfidence;
        public GameMode detectedMode;

        public TeamAnalysisResult() {
            this.players = new ArrayList<>();
        }
    }

    public enum GameMode {
        TEAM_VS_TEAM, FREE_FOR_ALL, BATTLE_ROYALE, COOP_VS_AI, UNKNOWN
    }

    public TeamClassifier(Context context, TensorFlowLiteHelper tfliteHelper) {
        this.context = context;
        this.tfliteHelper = tfliteHelper;
        this.nlpProcessor = new NLPProcessor(context);
        this.teamProfiles = new HashMap<>();

        initialize();
    }

    private void initialize() {
        try {
            // Initialize team profiles for common game types
            initializeTeamProfiles();

            isInitialized = true;
            Log.d(TAG, "Team Classifier initialized");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Team Classifier", e);
        }
    }

    private void initializeTeamProfiles() {
        // Friendly team profile (commonly blue)
        TeamProfile friendlyProfile = new TeamProfile();
        friendlyProfile.primaryColor = new float[]{0.2f, 0.4f, 0.8f}; // Blue
        friendlyProfile.secondaryColor = new float[]{0.6f, 0.8f, 1.0f}; // Light blue
        friendlyProfile.nametagColor = new float[]{0.0f, 0.8f, 0.0f}; // Green nametag
        friendlyProfile.hasOutline = true;
        friendlyProfile.outlineThickness = 2.0f;
        friendlyProfile.visualIndicators.addAll(Arrays.asList("blue_marker", "friendly_icon", "team_badge"));
        friendlyProfile.uiElements.put("health_bar", 0.8f);
        friendlyProfile.uiElements.put("status_indicator", 0.9f);
        teamProfiles.put(TeamAffiliation.FRIENDLY, friendlyProfile);

        // Enemy team profile (commonly red)
        TeamProfile enemyProfile = new TeamProfile();
        enemyProfile.primaryColor = new float[]{0.8f, 0.2f, 0.2f}; // Red
        enemyProfile.secondaryColor = new float[]{1.0f, 0.4f, 0.4f}; // Light red
        enemyProfile.nametagColor = new float[]{0.8f, 0.0f, 0.0f}; // Red nametag
        enemyProfile.hasOutline = true;
        enemyProfile.outlineThickness = 2.5f;
        enemyProfile.visualIndicators.addAll(Arrays.asList("red_marker", "enemy_icon", "hostile_indicator"));
        enemyProfile.uiElements.put("crosshair_highlight", 0.9f);
        enemyProfile.uiElements.put("threat_indicator", 0.8f);
        teamProfiles.put(TeamAffiliation.ENEMY, enemyProfile);

        // Neutral profile (commonly yellow/orange)
        TeamProfile neutralProfile = new TeamProfile();
        neutralProfile.primaryColor = new float[]{0.8f, 0.8f, 0.2f}; // Yellow
        neutralProfile.secondaryColor = new float[]{1.0f, 0.6f, 0.0f}; // Orange
        neutralProfile.nametagColor = new float[]{0.8f, 0.8f, 0.0f}; // Yellow nametag
        neutralProfile.hasOutline = false;
        neutralProfile.visualIndicators.addAll(Arrays.asList("neutral_marker", "civilian_icon"));
        teamProfiles.put(TeamAffiliation.NEUTRAL, neutralProfile);
    }

    public TeamAnalysisResult classifyPlayers(Bitmap gameScreen, List<PlayerTracker.PlayerData> players) {
        TeamAnalysisResult result = new TeamAnalysisResult();

        if (!isInitialized || gameScreen == null || players == null) {
            return result;
        }

        try {
            // Convert to OpenCV for advanced processing
            Mat screenMat = new Mat();
            Utils.bitmapToMat(gameScreen, screenMat);

            // Analyze each player
            for (PlayerTracker.PlayerData player : players) {
                PlayerClassification classification = classifyPlayer(screenMat, player);
                result.players.add(classification);

                // Update counts
                updateTeamCounts(result, classification.team);
            }

            // Determine game mode
            result.detectedMode = determineGameMode(result);

            // Calculate overall confidence
            result.overallConfidence = calculateOverallConfidence(result);

            // Apply contextual corrections
            applyContextualCorrections(result);

            Log.d(TAG, "Team classification complete - Friendly: " + result.friendlyCount +
                    ", Enemy: " + result.enemyCount + ", Mode: " + result.detectedMode);

        } catch (Exception e) {
            Log.e(TAG, "Error classifying players", e);
        }

        return result;
    }

    private PlayerClassification classifyPlayer(Mat screen, PlayerTracker.PlayerData player) {
        PlayerClassification classification = new PlayerClassification(
                player.playerId, player.boundingBox);

        try {
            // Extract player region
            Mat playerRegion = extractPlayerRegion(screen, player.boundingBox);

            // Multi-stage classification
            classifyByColor(playerRegion, classification);
            classifyByOutline(playerRegion, classification);
            classifyByNametag(playerRegion, classification);
            classifyByUIElements(screen, player.boundingBox, classification);

            // ML-based classification
            classifyWithML(playerRegion, classification);

            // Combine all classification methods
            finalizeClassification(classification);

        } catch (Exception e) {
            Log.w(TAG, "Player classification failed for player " + player.playerId, e);
            classification.team = TeamAffiliation.UNKNOWN;
            classification.confidence = 0f;
        }

        return classification;
    }

    private Mat extractPlayerRegion(Mat screen, Rect boundingBox) {
        // Expand region slightly to capture surrounding elements
        int expandX = Math.max(20, boundingBox.width() / 4);
        int expandY = Math.max(20, boundingBox.height() / 4);

        int x = Math.max(0, boundingBox.left - expandX);
        int y = Math.max(0, boundingBox.top - expandY);
        int width = Math.min(screen.width() - x, boundingBox.width() + 2 * expandX);
        int height = Math.min(screen.height() - y, boundingBox.height() + 2 * expandY);

        org.opencv.core.Rect expandedRect = new org.opencv.core.Rect(x, y, width, height);
        return new Mat(screen, expandedRect);
    }

    private void classifyByColor(Mat playerRegion, PlayerClassification classification) {
        try {
            // Convert to ND4J for fast color analysis
            INDArray colorData = matToNDArray(playerRegion);
            INDArray meanColors = colorData.mean(0, 1); // Average across spatial dimensions

            classification.dominantColors[0] = meanColors.getFloat(0);
            classification.dominantColors[1] = meanColors.getFloat(1);
            classification.dominantColors[2] = meanColors.getFloat(2);

            // Compare against team profiles
            float bestColorMatch = 0f;
            TeamAffiliation bestTeam = TeamAffiliation.UNKNOWN;

            for (Map.Entry<TeamAffiliation, TeamProfile> entry : teamProfiles.entrySet()) {
                TeamProfile profile = entry.getValue();

                // Calculate color distance using ND4J
                INDArray profileColor = Nd4j.create(profile.primaryColor);
                INDArray playerColor = meanColors;

                double colorDistance = profileColor.distance2(playerColor);
                float colorMatch = Math.max(0f, 1f - (float)(colorDistance / Math.sqrt(3))); // Normalize by max RGB distance

                // Also check secondary color
                INDArray secondaryColor = Nd4j.create(profile.secondaryColor);
                double secondaryDistance = secondaryColor.distance2(playerColor);
                float secondaryMatch = Math.max(0f, 1f - (float)(secondaryDistance / Math.sqrt(3)));

                float combinedMatch = Math.max(colorMatch, secondaryMatch * 0.8f);

                if (combinedMatch > bestColorMatch && combinedMatch > 0.3f) {
                    bestColorMatch = combinedMatch;
                    bestTeam = entry.getKey();
                }
            }

            if (bestTeam != TeamAffiliation.UNKNOWN) {
                classification.team = bestTeam;
                classification.confidence = bestColorMatch;
                classification.detectedIndicators.add("color_match_" + bestTeam.name().toLowerCase());
            }

        } catch (Exception e) {
            Log.w(TAG, "Color classification failed", e);
        }
    }

    private void classifyByOutline(Mat playerRegion, PlayerClassification classification) {
        try {
            // Detect colored outlines around players
            Mat edges = new Mat();
            Imgproc.Canny(playerRegion, edges, 50, 150);

            // Dilate to make outlines more prominent
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
            Mat dilated = new Mat();
            Imgproc.dilate(edges, dilated, kernel);

            // Analyze edge colors
            Mat edgeColors = new Mat();
            Core.bitwise_and(playerRegion, playerRegion, edgeColors, dilated);

            // Calculate mean color of edges
            Scalar meanEdgeColor = Core.mean(edgeColors, dilated);
            float[] edgeRGB = {
                    (float)(meanEdgeColor.val[0] / 255.0),
                    (float)(meanEdgeColor.val[1] / 255.0),
                    (float)(meanEdgeColor.val[2] / 255.0)
            };

            // Compare edge colors to team outline colors
            for (Map.Entry<TeamAffiliation, TeamProfile> entry : teamProfiles.entrySet()) {
                TeamProfile profile = entry.getValue();

                if (profile.hasOutline) {
                    float colorDistance = calculateColorDistance(edgeRGB, profile.primaryColor);

                    if (colorDistance < 0.3f) { // Close color match
                        float outlineConfidence = 0.7f * (1f - colorDistance);

                        if (outlineConfidence > classification.confidence) {
                            classification.team = entry.getKey();
                            classification.confidence = outlineConfidence;
                            classification.detectedIndicators.add("outline_" + entry.getKey().name().toLowerCase());
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Outline classification failed", e);
        }
    }

    private void classifyByNametag(Mat playerRegion, PlayerClassification classification) {
        try {
            // Look for nametag regions (usually above player)
            int nametagHeight = playerRegion.height() / 6;
            org.opencv.core.Rect nametagRect = new org.opencv.core.Rect(
                    0, 0, playerRegion.width(), nametagHeight);

            Mat nametagRegion = new Mat(playerRegion, nametagRect);

            // Analyze nametag colors
            Scalar meanNametagColor = Core.mean(nametagRegion);
            float[] nametagRGB = {
                    (float)(meanNametagColor.val[0] / 255.0),
                    (float)(meanNametagColor.val[1] / 255.0),
                    (float)(meanNametagColor.val[2] / 255.0)
            };

            // Check for specific nametag colors
            for (Map.Entry<TeamAffiliation, TeamProfile> entry : teamProfiles.entrySet()) {
                TeamProfile profile = entry.getValue();
                float colorDistance = calculateColorDistance(nametagRGB, profile.nametagColor);

                if (colorDistance < 0.25f) {
                    classification.hasNametag = true;
                    float nametagConfidence = 0.8f * (1f - colorDistance);

                    if (nametagConfidence > classification.confidence) {
                        classification.team = entry.getKey();
                        classification.confidence = nametagConfidence;
                        classification.detectedIndicators.add("nametag_" + entry.getKey().name().toLowerCase());
                    }
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Nametag classification failed", e);
        }
    }

    private void classifyByUIElements(Mat screen, Rect playerBounds, PlayerClassification classification) {
        try {
            // Look for UI elements around the player (health bars, markers, etc.)
            int searchRadius = Math.max(playerBounds.width(), playerBounds.height());

            // Expand search area
            org.opencv.core.Rect searchRect = new org.opencv.core.Rect(
                    Math.max(0, playerBounds.left - searchRadius),
                    Math.max(0, playerBounds.top - searchRadius),
                    Math.min(screen.width(), searchRadius * 2),
                    Math.min(screen.height(), searchRadius * 2)
            );

            Mat searchRegion = new Mat(screen, searchRect);

            // Look for common UI indicators
            detectHealthBars(searchRegion, classification);
            detectMarkerIcons(searchRegion, classification);
            detectStatusIndicators(searchRegion, classification);

        } catch (Exception e) {
            Log.w(TAG, "UI element classification failed", e);
        }
    }

    private void detectHealthBars(Mat region, PlayerClassification classification) {
        // Look for horizontal colored bars (health/shield)
        Mat grayRegion = new Mat();
        Imgproc.cvtColor(region, grayRegion, Imgproc.COLOR_RGB2GRAY);

        // Detect horizontal lines
        Mat horizontal = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25, 1));
        Mat morphed = new Mat();
        Imgproc.morphologyEx(grayRegion, morphed, Imgproc.MORPH_OPEN, horizontal);

        // Find contours of health bars
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(morphed, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {
            org.opencv.core.Rect boundingRect = Imgproc.boundingRect(contour);

            // Health bar characteristics: wider than tall, reasonable size
            if (boundingRect.width > boundingRect.height * 3 &&
                    boundingRect.width > 20 && boundingRect.height > 3) {

                // Analyze color of the health bar
                Mat healthBarRegion = new Mat(region, boundingRect);
                Scalar meanColor = Core.mean(healthBarRegion);

                // Green health bars often indicate friendlies
                if (meanColor.val[1] > meanColor.val[0] && meanColor.val[1] > meanColor.val[2]) {
                    classification.detectedIndicators.add("green_health_bar");
                    if (classification.team == TeamAffiliation.UNKNOWN) {
                        classification.team = TeamAffiliation.FRIENDLY;
                        classification.confidence = 0.6f;
                    }
                }
            }
        }
    }

    private void detectMarkerIcons(Mat region, PlayerClassification classification) {
        // Look for small circular or triangular markers
        Mat grayRegion = new Mat();
        Imgproc.cvtColor(region, grayRegion, Imgproc.COLOR_RGB2GRAY);

        // Detect circles (common for player markers)
        Mat circles = new Mat();
        Imgproc.HoughCircles(grayRegion, circles, Imgproc.HOUGH_GRADIENT, 1,
                20, 100, 30, 5, 25);

        if (circles.cols() > 0) {
            // Analyze color of detected circles
            for (int i = 0; i < circles.cols(); i++) {
                double[] circleData = circles.get(0, i);
                float[] circle = new float[]{(float)circleData[0], (float)circleData[1], (float)circleData[2]};
                Point center = new Point(circle[0], circle[1]);
                int radius = (int) circle[2];

                // Extract circle region
                org.opencv.core.Rect circleRect = new org.opencv.core.Rect(
                        (int) Math.max(0, center.x - radius),
                        (int) Math.max(0, center.y - radius),
                        Math.min(region.width(), radius * 2),
                        Math.min(region.height(), radius * 2)
                );

                Mat circleRegion = new Mat(region, circleRect);
                Scalar meanColor = Core.mean(circleRegion);

                float[] markerRGB = {
                        (float)(meanColor.val[0] / 255.0),
                        (float)(meanColor.val[1] / 255.0),
                        (float)(meanColor.val[2] / 255.0)
                };

                // Compare to team colors
                for (Map.Entry<TeamAffiliation, TeamProfile> entry : teamProfiles.entrySet()) {
                    TeamProfile profile = entry.getValue();
                    float distance = calculateColorDistance(markerRGB, profile.primaryColor);

                    if (distance < 0.3f) {
                        classification.detectedIndicators.add("marker_" + entry.getKey().name().toLowerCase());
                        if (classification.confidence < 0.7f) {
                            classification.team = entry.getKey();
                            classification.confidence = 0.7f;
                        }
                    }
                }
            }
        }
    }

    private void detectStatusIndicators(Mat region, PlayerClassification classification) {
        // Look for status icons (shields, buffs, etc.)
        // This is a simplified implementation - real version would use trained models

        // Look for bright spots that might be status indicators
        Mat grayRegion = new Mat();
        Imgproc.cvtColor(region, grayRegion, Imgproc.COLOR_RGB2GRAY);

        // Find bright regions
        Mat thresh = new Mat();
        Imgproc.threshold(grayRegion, thresh, 200, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > 10 && area < 200) { // Small icons
                classification.detectedIndicators.add("status_indicator");
            }
        }
    }

    private void classifyWithML(Mat playerRegion, PlayerClassification classification) {
        try {
            // Convert Mat to Bitmap for TensorFlow Lite
            Bitmap playerBitmap = Bitmap.createBitmap(
                    playerRegion.width(), playerRegion.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(playerRegion, playerBitmap);

            // Run ML classification if available
            if (tfliteHelper.isModelLoaded("player_classifier")) {
                List<TensorFlowLiteHelper.DetectionResult> results =
                        tfliteHelper.runInference("player_classifier", playerBitmap);

                for (TensorFlowLiteHelper.DetectionResult result : results) {
                    TeamAffiliation mlTeam = mapMLResultToTeam(result.className);
                    if (mlTeam != TeamAffiliation.UNKNOWN && result.confidence > 0.6f) {
                        classification.detectedIndicators.add("ml_" + result.className);

                        if (result.confidence > classification.confidence) {
                            classification.team = mlTeam;
                            classification.confidence = result.confidence;
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "ML classification failed", e);
        }
    }

    private TeamAffiliation mapMLResultToTeam(String className) {
        String lower = className.toLowerCase();

        if (lower.contains("friendly") || lower.contains("ally") || lower.contains("teammate")) {
            return TeamAffiliation.FRIENDLY;
        } else if (lower.contains("enemy") || lower.contains("hostile") || lower.contains("opponent")) {
            return TeamAffiliation.ENEMY;
        } else if (lower.contains("neutral") || lower.contains("civilian")) {
            return TeamAffiliation.NEUTRAL;
        }

        return TeamAffiliation.UNKNOWN;
    }

    private void finalizeClassification(PlayerClassification classification) {
        // Apply confidence thresholds
        if (classification.confidence < 0.3f) {
            classification.team = TeamAffiliation.UNKNOWN;
        }

        // Calculate threat level based on team affiliation
        switch (classification.team) {
            case ENEMY:
                classification.threatLevel = 0.8f + (classification.confidence * 0.2f);
                break;
            case NEUTRAL:
                classification.threatLevel = 0.3f;
                break;
            case FRIENDLY:
                classification.threatLevel = 0.1f;
                break;
            default:
                classification.threatLevel = 0.5f; // Unknown players are medium threat
                break;
        }
    }

    private float calculateColorDistance(float[] color1, float[] color2) {
        float dr = color1[0] - color2[0];
        float dg = color1[1] - color2[1];
        float db = color1[2] - color2[2];
        return (float) Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private INDArray matToNDArray(Mat mat) {
        int height = mat.height();
        int width = mat.width();
        int channels = mat.channels();

        byte[] data = new byte[height * width * channels];
        mat.get(0, 0, data);

        float[] floatData = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            floatData[i] = (data[i] & 0xFF) / 255.0f;
        }

        return Nd4j.create(floatData).reshape(height, width, channels);
    }

    private void updateTeamCounts(TeamAnalysisResult result, TeamAffiliation team) {
        switch (team) {
            case FRIENDLY:
                result.friendlyCount++;
                break;
            case ENEMY:
                result.enemyCount++;
                break;
            case NEUTRAL:
                result.neutralCount++;
                break;
            default:
                result.unknownCount++;
                break;
        }
    }

    private GameMode determineGameMode(TeamAnalysisResult result) {
        int totalPlayers = result.friendlyCount + result.enemyCount + result.neutralCount;

        if (totalPlayers > 50) {
            return GameMode.BATTLE_ROYALE;
        } else if (result.friendlyCount > 0 && result.enemyCount > 0 && result.neutralCount == 0) {
            return GameMode.TEAM_VS_TEAM;
        } else if (result.friendlyCount == 0 && result.enemyCount > 1) {
            return GameMode.FREE_FOR_ALL;
        } else if (result.friendlyCount > 0 && result.enemyCount == 0) {
            return GameMode.COOP_VS_AI;
        }

        return GameMode.UNKNOWN;
    }

    private float calculateOverallConfidence(TeamAnalysisResult result) {
        if (result.players.isEmpty()) return 0f;

        float totalConfidence = 0f;
        for (PlayerClassification player : result.players) {
            totalConfidence += player.confidence;
        }

        return totalConfidence / result.players.size();
    }

    private void applyContextualCorrections(TeamAnalysisResult result) {
        // Apply game mode specific corrections
        switch (result.detectedMode) {
            case BATTLE_ROYALE:
                // In battle royale, most players should be enemies
                for (PlayerClassification player : result.players) {
                    if (player.team == TeamAffiliation.UNKNOWN && player.confidence < 0.5f) {
                        player.team = TeamAffiliation.ENEMY;
                        player.confidence = 0.6f;
                        player.detectedIndicators.add("battle_royale_assumption");
                    }
                }
                break;

            case FREE_FOR_ALL:
                // In FFA, all other players are enemies
                for (PlayerClassification player : result.players) {
                    if (player.team != TeamAffiliation.FRIENDLY) {
                        player.team = TeamAffiliation.ENEMY;
                        player.confidence = Math.max(player.confidence, 0.7f);
                        player.detectedIndicators.add("ffa_assumption");
                    }
                }
                break;
        }
    }

    public List<PlayerClassification> getEnemyPlayers(TeamAnalysisResult result) {
        List<PlayerClassification> enemies = new ArrayList<>();
        for (PlayerClassification player : result.players) {
            if (player.team == TeamAffiliation.ENEMY) {
                enemies.add(player);
            }
        }

        // Sort by threat level
        enemies.sort((p1, p2) -> Float.compare(p2.threatLevel, p1.threatLevel));
        return enemies;
    }

    public List<PlayerClassification> getFriendlyPlayers(TeamAnalysisResult result) {
        List<PlayerClassification> friendlies = new ArrayList<>();
        for (PlayerClassification player : result.players) {
            if (player.team == TeamAffiliation.FRIENDLY) {
                friendlies.add(player);
            }
        }
        return friendlies;
    }

    public PlayerClassification getNearestEnemy(TeamAnalysisResult result, float[] playerPosition) {
        List<PlayerClassification> enemies = getEnemyPlayers(result);

        PlayerClassification nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (PlayerClassification enemy : enemies) {
            float dx = enemy.playerRegion.centerX() - playerPosition[0];
            float dy = enemy.playerRegion.centerY() - playerPosition[1];
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < minDistance) {
                minDistance = distance;
                nearest = enemy;
            }
        }

        return nearest;
    }
    public void addNLPTeamData(String teamDescription, String classification) {
        if (nlpProcessor != null) {
            List<String> teamIndicators = nlpProcessor.extractTeamIndicators(teamDescription);
            updateTeamClassificationRules(teamIndicators, classification);
        }
    }
    private void updateTeamClassificationRules(List<String> teamIndicators, String classification) {
        try {
            TeamAffiliation affiliation = TeamAffiliation.valueOf(classification.toUpperCase());
            TeamProfile profile = teamProfiles.get(affiliation);

            if (profile != null) {
                // Add new indicators to the profile
                for (String indicator : teamIndicators) {
                    if (!profile.visualIndicators.contains(indicator)) {
                        profile.visualIndicators.add(indicator);
                    }
                }
                Log.d(TAG, "Updated team classification rules for " + affiliation + " with " + teamIndicators.size() + " indicators");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to update team classification rules", e);
        }
    }

}
