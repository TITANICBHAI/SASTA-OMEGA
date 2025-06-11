package com.gestureai.gameautomation;

import android.graphics.Rect;
import android.util.Log;
import java.util.*;
import com.gestureai.gameautomation.utils.NLPProcessor;
import android.content.Context;

/**
 * Advanced multi-player tracking for battle royale and multiplayer games
 */
public class PlayerTracker {
    private static final String TAG = "PlayerTracker";
    private Context context;
    private NLPProcessor nlpProcessor;
    public PlayerTracker(Context context) {
        this.context = context;
        this.trackedPlayers = new HashMap<>();
        this.nextPlayerId = 1;
        // Initialize NLPProcessor
        this.nlpProcessor = new NLPProcessor(context);
    }

    public static class PlayerData {
        public int playerId;
        public Rect boundingBox;
        public float[] position;
        public float[] velocity;
        public String teamStatus; // "enemy", "teammate", "unknown"
        public float threatLevel;
        public float confidence;
        public long lastSeen;
        public List<float[]> movementHistory;
        public List<String> nlpTags;
        public String actionRecommendation;
        public int health;
        public int armor;
        public String playerName;
        public boolean isEnemy;
        public WeaponRecognizer.WeaponType currentWeapon;



        public PlayerData(int id, Rect box, float confidence) {
            this.playerId = id;
            this.boundingBox = box;
            this.confidence = confidence;
            this.position = new float[]{box.centerX(), box.centerY()};
            this.velocity = new float[]{0f, 0f};
            this.movementHistory = new ArrayList<>();
            this.lastSeen = System.currentTimeMillis();
        }

        public void updatePosition(Rect newBox) {
            float[] newPos = new float[]{newBox.centerX(), newBox.centerY()};

            // Calculate velocity
            long timeDiff = System.currentTimeMillis() - lastSeen;
            if (timeDiff > 0) {
                velocity[0] = (newPos[0] - position[0]) / timeDiff;
                velocity[1] = (newPos[1] - position[1]) / timeDiff;
            }

            // Update movement history
            movementHistory.add(Arrays.copyOf(position, 2));
            if (movementHistory.size() > 10) {
                movementHistory.remove(0);
            }

            position = newPos;
            boundingBox = newBox;
            lastSeen = System.currentTimeMillis();
        }

        public float[] predictNextPosition(long timeAhead) {
            return new float[]{
                    position[0] + velocity[0] * timeAhead,
                    position[1] + velocity[1] * timeAhead
            };
        }
    }

    private Map<Integer, PlayerData> trackedPlayers;
    private int nextPlayerId;
    private float trackingThreshold = 0.3f;
    private long maxTrackingAge = 5000; // 5 seconds

    /**
     * Update player tracking with new detections
     */
    public List<PlayerData> updateTracking(List<ObjectDetectionEngine.DetectedObject> detectedObjects) {
        List<ObjectDetectionEngine.DetectedObject> playerObjects = filterPlayerObjects(detectedObjects);

        // Update existing tracks
        for (PlayerData player : trackedPlayers.values()) {
            ObjectDetectionEngine.DetectedObject matchedObject = findBestMatch(player, playerObjects);
            if (matchedObject != null) {
                player.updatePosition(new android.graphics.Rect(
                        matchedObject.boundingRect.x,
                        matchedObject.boundingRect.y,
                        matchedObject.boundingRect.x + matchedObject.boundingRect.width,
                        matchedObject.boundingRect.y + matchedObject.boundingRect.height
                ));
                playerObjects.remove(matchedObject);
            }
        }

        // Create new tracks for unmatched detections
        for (ObjectDetectionEngine.DetectedObject obj : playerObjects) {
            PlayerData newPlayer = new PlayerData(nextPlayerId++, new android.graphics.Rect(
                    obj.boundingRect.x,
                    obj.boundingRect.y,
                    obj.boundingRect.x + obj.boundingRect.width,
                    obj.boundingRect.y + obj.boundingRect.height
            ), obj.confidence);
            newPlayer.teamStatus = classifyTeamStatus(obj);
            trackedPlayers.put(newPlayer.playerId, newPlayer);
        }

        // Remove old tracks
        removeStaleTracking();

        // Calculate threat levels
        updateThreatLevels();

        return new ArrayList<>(trackedPlayers.values());
    }

    private List<ObjectDetectionEngine.DetectedObject> filterPlayerObjects(List<ObjectDetectionEngine.DetectedObject> objects) {
        List<ObjectDetectionEngine.DetectedObject> players = new ArrayList<>();
        for (ObjectDetectionEngine.DetectedObject obj : objects) {
            if (obj.name.toLowerCase().contains("player") ||
                    obj.name.toLowerCase().contains("enemy") ||
                    obj.name.toLowerCase().contains("character")) {
                players.add(obj);
            }
        }
        return players;
    }

    private ObjectDetectionEngine.DetectedObject findBestMatch(PlayerData player,
                                                               List<ObjectDetectionEngine.DetectedObject> candidates) {

        ObjectDetectionEngine.DetectedObject bestMatch = null;
        float bestScore = Float.MAX_VALUE;

        for (ObjectDetectionEngine.DetectedObject candidate : candidates) {
            float distance = calculateDistance(player.position,
                    new float[]{
                            candidate.boundingRect.x + candidate.boundingRect.width/2f,
                            candidate.boundingRect.y + candidate.boundingRect.height/2f
                    });

            if (distance < bestScore && distance < 100) { // Max distance threshold
                bestScore = distance;
                bestMatch = candidate;
            }
        }

        return bestMatch;
    }

    private float calculateDistance(float[] pos1, float[] pos2) {
        float dx = pos1[0] - pos2[0];
        float dy = pos1[1] - pos2[1];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private String classifyTeamStatus(ObjectDetectionEngine.DetectedObject obj) {
        // Analyze color patterns, UI indicators, etc.
        // This would use advanced computer vision techniques

        if (obj.name.toLowerCase().contains("enemy")) {
            return "enemy";
        } else if (obj.name.toLowerCase().contains("teammate")) {
            return "teammate";
        }

        return "unknown";
    }

    private void removeStaleTracking() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, PlayerData>> iterator = trackedPlayers.entrySet().iterator();

        while (iterator.hasNext()) {
            PlayerData player = iterator.next().getValue();
            if (currentTime - player.lastSeen > maxTrackingAge) {
                iterator.remove();
                Log.d(TAG, "Removed stale player track: " + player.playerId);
            }
        }
    }

    private void updateThreatLevels() {
        for (PlayerData player : trackedPlayers.values()) {
            player.threatLevel = calculateThreatLevel(player);
        }
    }

    private float calculateThreatLevel(PlayerData player) {
        float threat = 0f;

        // Distance-based threat (closer = more threatening)
        float distance = calculateDistance(player.position, new float[]{540, 960}); // Screen center
        threat += Math.max(0, 1.0f - (distance / 500f));

        // Movement-based threat (fast movement = more threatening)
        float speed = (float) Math.sqrt(player.velocity[0] * player.velocity[0] +
                player.velocity[1] * player.velocity[1]);
        threat += Math.min(0.5f, speed / 100f);

        // Team status modifier
        if ("enemy".equals(player.teamStatus)) {
            threat *= 1.5f;
        } else if ("teammate".equals(player.teamStatus)) {
            threat *= 0.3f;
        }

        return Math.min(1.0f, threat);
    }

    /**
     * Get players sorted by threat level
     */
    public List<PlayerData> getPlayersByThreat() {
        List<PlayerData> players = new ArrayList<>(trackedPlayers.values());
        players.sort((p1, p2) -> Float.compare(p2.threatLevel, p1.threatLevel));
        return players;
    }

    /**
     * Get the most threatening enemy player
     */
    public PlayerData getMostThreateningEnemy() {
        return trackedPlayers.values().stream()
                .filter(p -> "enemy".equals(p.teamStatus))
                .max(Comparator.comparingDouble(p -> p.threatLevel))
                .orElse(null);
    }

    /**
     * Predict player positions after specified time
     */
    public Map<Integer, float[]> predictPlayerPositions(long timeAheadMs) {
        Map<Integer, float[]> predictions = new HashMap<>();
        for (PlayerData player : trackedPlayers.values()) {
            predictions.put(player.playerId, player.predictNextPosition(timeAheadMs));
        }
        return predictions;
    }

    public int getPlayerCount() {
        return trackedPlayers.size();
    }

    public int getEnemyCount() {
        return (int) trackedPlayers.values().stream()
                .filter(p -> "enemy".equals(p.teamStatus))
                .count();
    }

    public int getTeammateCount() {
        return (int) trackedPlayers.values().stream()
                .filter(p -> "teammate".equals(p.teamStatus))
                .count();
    }
    public void addNLPTrainingData(String playerDescription, PlayerData data) {
        if (nlpProcessor != null) {
            NLPProcessor.ActionIntent intent = nlpProcessor.processNaturalLanguageCommand(playerDescription);
            if (intent != null) {
                // Store NLP-processed player data
                data.nlpTags = intent.getEntityTypes();
                data.actionRecommendation = intent.getAction();
            }
        }
    }
}
