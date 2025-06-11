package com.gestureai.gameautomation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.gestureai.gameautomation.utils.NLPProcessor;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.util.*;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;

/**
 * Advanced weapon recognition and classification for FPS games
 */
public class WeaponRecognizer {
    private static final String TAG = "WeaponRecognizer";

    private Context context;
    private TensorFlowLiteHelper tfliteHelper;
    private OCREngine ocrEngine;
    private NLPProcessor nlpProcessor;
    private Map<String, WeaponProfile> weaponDatabase;
    private NLPProcessor.StrategyAnalysis nlpClassification;
    private String strategicUse;
    private boolean isInitialized = false;

    public static class WeaponInfo {
        public String weaponName;
        public WeaponType type;
        public WeaponRarity rarity;
        public int currentAmmo;
        public int maxAmmo;
        public int reserveAmmo;
        public float damage;
        public float range;
        public float fireRate;
        public float accuracy;
        public List<String> attachments;
        public Rect weaponUIRegion;
        public float confidence;

        public WeaponInfo() {
            this.attachments = new ArrayList<>();
        }
    }

    public static class WeaponProfile {
        public String name;
        public WeaponType type;
        public float[] typicalDamage; // [min, max]
        public float[] typicalRange; // [min, max]
        public float fireRate; // rounds per minute
        public String[] visualFeatures; // Key visual identifiers
        public Map<String, Float> colorSignature; // RGB patterns
        public int[] ammoCapacity; // [magazine, reserve]

        public WeaponProfile(String name, WeaponType type) {
            this.name = name;
            this.type = type;
            this.colorSignature = new HashMap<>();
            this.visualFeatures = new String[0];
        }
    }

    public enum WeaponType {
        ASSAULT_RIFLE, SMG, SHOTGUN, SNIPER_RIFLE, PISTOL, LMG,
        GRENADE, MELEE, LAUNCHER, UNKNOWN
    }

    public enum WeaponRarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
    }

    public static class WeaponDetectionResult {
        public WeaponInfo primaryWeapon;
        public WeaponInfo secondaryWeapon;
        public WeaponInfo meleeWeapon;
        public List<WeaponInfo> grenades;
        public WeaponInfo currentWeapon;
        public float overallConfidence;

        public WeaponDetectionResult() {
            this.grenades = new ArrayList<>();
        }
    }

    public WeaponRecognizer(Context context, TensorFlowLiteHelper tfliteHelper, OCREngine ocrEngine) {
        this.context = context;
        this.tfliteHelper = tfliteHelper;
        this.ocrEngine = ocrEngine;
        this.weaponDatabase = new HashMap<>();
        this.nlpProcessor = new NLPProcessor(context);  // Add this line

        initialize();
    }

    private void initialize() {
        try {
            // Load weapon classification model
            tfliteHelper.loadModel("weapon_classifier");

            // Initialize weapon database
            initializeWeaponDatabase();

            isInitialized = true;
            Log.d(TAG, "Weapon Recognizer initialized with " + weaponDatabase.size() + " weapon profiles");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Weapon Recognizer", e);
        }
    }

    private void initializeWeaponDatabase() {
        // Assault Rifles
        WeaponProfile ak47 = new WeaponProfile("AK-47", WeaponType.ASSAULT_RIFLE);
        ak47.typicalDamage = new float[]{36, 42};
        ak47.typicalRange = new float[]{300, 600};
        ak47.fireRate = 600;
        ak47.visualFeatures = new String[]{"curved_magazine", "wooden_stock", "distinctive_shape"};
        ak47.colorSignature.put("wood_brown", 0.3f);
        ak47.colorSignature.put("metal_black", 0.6f);
        ak47.ammoCapacity = new int[]{30, 120};
        weaponDatabase.put("ak47", ak47);

        WeaponProfile m4a1 = new WeaponProfile("M4A1", WeaponType.ASSAULT_RIFLE);
        m4a1.typicalDamage = new float[]{33, 38};
        m4a1.typicalRange = new float[]{400, 700};
        m4a1.fireRate = 650;
        m4a1.visualFeatures = new String[]{"straight_magazine", "adjustable_stock", "rail_system"};
        m4a1.colorSignature.put("military_green", 0.4f);
        m4a1.colorSignature.put("metal_black", 0.6f);
        m4a1.ammoCapacity = new int[]{30, 90};
        weaponDatabase.put("m4a1", m4a1);

        // SMGs
        WeaponProfile mp5 = new WeaponProfile("MP5", WeaponType.SMG);
        mp5.typicalDamage = new float[]{25, 30};
        mp5.typicalRange = new float[]{100, 300};
        mp5.fireRate = 800;
        mp5.visualFeatures = new String[]{"compact_size", "side_folding_stock", "curved_magazine"};
        mp5.colorSignature.put("metal_black", 0.8f);
        mp5.ammoCapacity = new int[]{30, 120};
        weaponDatabase.put("mp5", mp5);

        // Sniper Rifles
        WeaponProfile awp = new WeaponProfile("AWP", WeaponType.SNIPER_RIFLE);
        awp.typicalDamage = new float[]{100, 115};
        awp.typicalRange = new float[]{800, 1500};
        awp.fireRate = 40;
        awp.visualFeatures = new String[]{"long_barrel", "large_scope", "bolt_action"};
        awp.colorSignature.put("dark_green", 0.5f);
        awp.colorSignature.put("metal_black", 0.5f);
        awp.ammoCapacity = new int[]{10, 30};
        weaponDatabase.put("awp", awp);

        // Shotguns
        WeaponProfile shotgun = new WeaponProfile("Pump Shotgun", WeaponType.SHOTGUN);
        shotgun.typicalDamage = new float[]{80, 120};
        shotgun.typicalRange = new float[]{50, 150};
        shotgun.fireRate = 60;
        shotgun.visualFeatures = new String[]{"wide_barrel", "pump_action", "shell_tube"};
        shotgun.colorSignature.put("metal_black", 0.7f);
        shotgun.colorSignature.put("wood_brown", 0.3f);
        shotgun.ammoCapacity = new int[]{8, 32};
        weaponDatabase.put("shotgun", shotgun);

        // Pistols
        WeaponProfile glock = new WeaponProfile("Glock", WeaponType.PISTOL);
        glock.typicalDamage = new float[]{20, 28};
        glock.typicalRange = new float[]{100, 250};
        glock.fireRate = 400;
        glock.visualFeatures = new String[]{"compact_frame", "polymer_body", "straight_magazine"};
        glock.colorSignature.put("polymer_black", 0.8f);
        glock.ammoCapacity = new int[]{17, 68};
        weaponDatabase.put("glock", glock);
    }

    public WeaponDetectionResult analyzeWeapons(Bitmap gameScreen) {
        WeaponDetectionResult result = new WeaponDetectionResult();

        if (!isInitialized || gameScreen == null) {
            return result;
        }

        try {
            // Step 1: Locate weapon UI regions
            List<Rect> weaponRegions = findWeaponUIRegions(gameScreen);

            // Step 2: Analyze each weapon slot
            for (int i = 0; i < weaponRegions.size(); i++) {
                Rect region = weaponRegions.get(i);
                WeaponInfo weapon = analyzeWeaponInRegion(gameScreen, region);

                if (weapon != null) {
                    assignWeaponToSlot(result, weapon, i);
                }
            }

            // Step 3: Determine current active weapon
            result.currentWeapon = detectActiveWeapon(gameScreen, result);

            // Step 4: Extract ammo information
            extractAmmoData(gameScreen, result);

            // Step 5: Calculate overall confidence
            result.overallConfidence = calculateOverallConfidence(result);

            Log.d(TAG, "Weapon analysis complete - Current: " +
                    (result.currentWeapon != null ? result.currentWeapon.weaponName : "none"));

        } catch (Exception e) {
            Log.e(TAG, "Error analyzing weapons", e);
        }

        return result;
    }

    private List<Rect> findWeaponUIRegions(Bitmap screen) {
        List<Rect> regions = new ArrayList<>();

        try {
            // Use ML detection for weapon UI elements
            List<TensorFlowLiteHelper.DetectionResult> uiResults =
                    tfliteHelper.runInference("ui_detector", screen);

            // Look for weapon slots
            for (TensorFlowLiteHelper.DetectionResult result : uiResults) {
                if (result.className.contains("weapon") || result.className.contains("inventory")) {
                    float[] box = result.boundingBox;
                    Rect region = new Rect(
                            (int)(box[0] * screen.getWidth()),
                            (int)(box[1] * screen.getHeight()),
                            (int)((box[0] + box[2]) * screen.getWidth()),
                            (int)((box[1] + box[3]) * screen.getHeight())
                    );
                    regions.add(region);
                }
            }

            // Fallback to common weapon UI locations
            if (regions.isEmpty()) {
                regions.addAll(getDefaultWeaponRegions(screen));
            }

        } catch (Exception e) {
            Log.w(TAG, "UI region detection failed, using defaults", e);
            regions.addAll(getDefaultWeaponRegions(screen));
        }

        return regions;
    }

    private List<Rect> getDefaultWeaponRegions(Bitmap screen) {
        List<Rect> regions = new ArrayList<>();
        int width = screen.getWidth();
        int height = screen.getHeight();

        // Bottom-right weapon wheel/slots (common in mobile FPS)
        regions.add(new Rect(width - 200, height - 300, width, height - 100));

        // Bottom-center weapon display
        regions.add(new Rect(width/2 - 150, height - 150, width/2 + 150, height));

        // Right side weapon inventory
        regions.add(new Rect(width - 150, height/2 - 100, width, height/2 + 100));

        return regions;
    }

    private WeaponInfo analyzeWeaponInRegion(Bitmap screen, Rect region) {
        try {
            // Extract weapon region
            Bitmap weaponBitmap = Bitmap.createBitmap(screen,
                    region.left, region.top, region.width(), region.height());

            // ML classification
            List<TensorFlowLiteHelper.DetectionResult> mlResults =
                    tfliteHelper.runInference("weapon_classifier", weaponBitmap);

            WeaponInfo weapon = new WeaponInfo();
            weapon.weaponUIRegion = region;

            // Process ML results
            if (!mlResults.isEmpty()) {
                TensorFlowLiteHelper.DetectionResult bestResult = mlResults.get(0);
                weapon.weaponName = bestResult.className;
                weapon.confidence = bestResult.confidence;
                weapon.type = mapClassNameToType(bestResult.className);
            }

            // Enhanced analysis with visual features
            enhanceWeaponAnalysis(weaponBitmap, weapon);

            // OCR for weapon name and stats
            extractWeaponTextInfo(weaponBitmap, weapon);

            return weapon.confidence > 0.3f ? weapon : null;

        } catch (Exception e) {
            Log.w(TAG, "Weapon region analysis failed", e);
            return null;
        }
    }

    private void enhanceWeaponAnalysis(Bitmap weaponBitmap, WeaponInfo weapon) {
        try {
            // Convert to OpenCV for advanced analysis
            Mat weaponMat = new Mat();
            Utils.bitmapToMat(weaponBitmap, weaponMat);

            // Analyze weapon shape and features
            analyzeWeaponShape(weaponMat, weapon);

            // Color analysis for rarity detection
            analyzeWeaponRarity(weaponMat, weapon);

            // Attachment detection
            detectAttachments(weaponMat, weapon);

        } catch (Exception e) {
            Log.w(TAG, "Enhanced weapon analysis failed", e);
        }
    }

    private void analyzeWeaponShape(Mat weaponMat, WeaponInfo weapon) {
        try {
            // Convert to grayscale
            Mat grayMat = new Mat();
            Imgproc.cvtColor(weaponMat, grayMat, Imgproc.COLOR_RGB2GRAY);

            // Edge detection
            Mat edges = new Mat();
            Imgproc.Canny(grayMat, edges, 50, 150);

            // Find contours
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Analyze largest contour for weapon shape
            if (!contours.isEmpty()) {
                MatOfPoint largestContour = contours.get(0);
                double maxArea = Imgproc.contourArea(largestContour);

                for (MatOfPoint contour : contours) {
                    double area = Imgproc.contourArea(contour);
                    if (area > maxArea) {
                        maxArea = area;
                        largestContour = contour;
                    }
                }

                // Calculate shape properties using OpenCV Rect
                org.opencv.core.Rect cvBoundingRect = Imgproc.boundingRect(largestContour);
                float aspectRatio = (float) cvBoundingRect.width / cvBoundingRect.height;

                // Classify weapon type based on aspect ratio
                if (aspectRatio > 3.0f) {
                    weapon.type = WeaponType.SNIPER_RIFLE;
                } else if (aspectRatio > 2.0f) {
                    weapon.type = WeaponType.ASSAULT_RIFLE;
                } else if (aspectRatio > 1.5f) {
                    weapon.type = WeaponType.SMG;
                } else if (aspectRatio > 0.8f) {
                    weapon.type = WeaponType.SHOTGUN;
                } else if (aspectRatio > 0.3f) {
                    weapon.type = WeaponType.PISTOL;
                } else {
                    // Very compact or elongated shapes = melee weapons
                    weapon.type = WeaponType.MELEE;

                    // Additional melee subtype detection based on shape analysis
                    detectMeleeSubtype(cvBoundingRect, weapon, largestContour);
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Weapon shape analysis failed", e);
        }
    }

    private void analyzeWeaponRarity(Mat weaponMat, WeaponInfo weapon) {
        try {
            // Calculate dominant colors using ND4J for speed
            INDArray colorData = matToNDArray(weaponMat);
            INDArray meanColors = colorData.mean(0, 1); // Average across height and width

            float r = meanColors.getFloat(0);
            float g = meanColors.getFloat(1);
            float b = meanColors.getFloat(2);

            // Rarity based on color schemes (common in many games)
            if (r > 0.8f && g > 0.6f && b < 0.3f) {
                weapon.rarity = WeaponRarity.LEGENDARY; // Gold
            } else if (r > 0.6f && g < 0.3f && b > 0.6f) {
                weapon.rarity = WeaponRarity.EPIC; // Purple
            } else if (r < 0.3f && g < 0.3f && b > 0.8f) {
                weapon.rarity = WeaponRarity.RARE; // Blue
            } else if (r < 0.3f && g > 0.6f && b < 0.3f) {
                weapon.rarity = WeaponRarity.UNCOMMON; // Green
            } else {
                weapon.rarity = WeaponRarity.COMMON; // Gray/White
            }

        } catch (Exception e) {
            Log.w(TAG, "Rarity analysis failed", e);
            weapon.rarity = WeaponRarity.COMMON;
        }
    }

    private void detectAttachments(Mat weaponMat, WeaponInfo weapon) {
        try {
            // Look for common attachment indicators
            String[] attachmentPatterns = {"scope", "silencer", "grip", "laser", "stock", "barrel"};

            // Use template matching or feature detection
            // This is simplified - real implementation would use trained models

            // Analyze weapon profile for attachment points
            if (weapon.type == WeaponType.ASSAULT_RIFLE || weapon.type == WeaponType.SNIPER_RIFLE) {
                // Check for scope (bright reflection or distinctive shape)
                if (hasScope(weaponMat)) {
                    weapon.attachments.add("scope");
                }

                // Check for suppressor (extended barrel)
                if (hasSuppressor(weaponMat)) {
                    weapon.attachments.add("suppressor");
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Attachment detection failed", e);
        }
    }

    private boolean hasScope(Mat weaponMat) {
        // Look for circular patterns (scope lens) or rectangular shapes (scope body)
        Mat grayMat = new Mat();
        Imgproc.cvtColor(weaponMat, grayMat, Imgproc.COLOR_RGB2GRAY);

        Mat circles = new Mat();
        Imgproc.HoughCircles(grayMat, circles, Imgproc.HOUGH_GRADIENT, 1,
                grayMat.rows()/8f, 200, 100, 5, 30);

        return circles.cols() > 0;
    }

    private boolean hasSuppressor(Mat weaponMat) {
        // Look for extended cylindrical shape at barrel end
        // Simplified check based on aspect ratio
        return weaponMat.width() / (float)weaponMat.height() > 4.0f;
    }

    private void extractWeaponTextInfo(Bitmap weaponBitmap, WeaponInfo weapon) {
        try {
            // Use OCR to extract weapon name and stats
            List<OCREngine.DetectedText> texts = ocrEngine.processScreenText(weaponBitmap).get();

            for (OCREngine.DetectedText text : texts) {
                String textLower = text.text.toLowerCase();

                // Look for weapon names
                for (String weaponName : weaponDatabase.keySet()) {
                    if (textLower.contains(weaponName.toLowerCase())) {
                        weapon.weaponName = weaponName;
                        weapon.confidence = Math.max(weapon.confidence, text.confidence);
                        break;
                    }
                }

                // Extract damage numbers
                if (textLower.contains("damage") || textLower.contains("dmg")) {
                    String numericText = text.text.replaceAll("[^0-9.]", "");
                    if (!numericText.isEmpty()) {
                        weapon.damage = Float.parseFloat(numericText);
                    }
                }

                // Extract fire rate
                if (textLower.contains("rpm") || textLower.contains("rate")) {
                    String numericText = text.text.replaceAll("[^0-9.]", "");
                    if (!numericText.isEmpty()) {
                        weapon.fireRate = Float.parseFloat(numericText);
                    }
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Weapon text extraction failed", e);
        }
    }

    private WeaponType mapClassNameToType(String className) {
        String lower = className.toLowerCase();

        if (lower.contains("rifle") && !lower.contains("sniper")) {
            return WeaponType.ASSAULT_RIFLE;
        } else if (lower.contains("sniper")) {
            return WeaponType.SNIPER_RIFLE;
        } else if (lower.contains("smg") || lower.contains("submachine")) {
            return WeaponType.SMG;
        } else if (lower.contains("shotgun")) {
            return WeaponType.SHOTGUN;
        } else if (lower.contains("pistol") || lower.contains("handgun")) {
            return WeaponType.PISTOL;
        } else if (lower.contains("lmg") || lower.contains("machine")) {
            return WeaponType.LMG;
        } else if (lower.contains("grenade")) {
            return WeaponType.GRENADE;
        } else if (lower.contains("knife") || lower.contains("melee")) {
            return WeaponType.MELEE;
        }

        return WeaponType.UNKNOWN;
    }

    private void assignWeaponToSlot(WeaponDetectionResult result, WeaponInfo weapon, int slotIndex) {
        switch (slotIndex) {
            case 0:
                result.primaryWeapon = weapon;
                break;
            case 1:
                result.secondaryWeapon = weapon;
                break;
            case 2:
                if (weapon.type == WeaponType.MELEE) {
                    result.meleeWeapon = weapon;
                } else if (weapon.type == WeaponType.GRENADE) {
                    result.grenades.add(weapon);
                }
                break;
            default:
                if (weapon.type == WeaponType.GRENADE) {
                    result.grenades.add(weapon);
                }
                break;
        }
    }

    private WeaponInfo detectActiveWeapon(Bitmap screen, WeaponDetectionResult result) {
        try {
            // Look for active weapon indicators (highlighted UI, crosshair, etc.)

            // Check ammo counter region for active weapon
            Rect ammoRegion = new Rect(screen.getWidth() - 200, screen.getHeight() - 150,
                    screen.getWidth(), screen.getHeight());

            List<OCREngine.DetectedText> ammoTexts =
                    ocrEngine.processScreenRegion(screen, ammoRegion).get();

            // Match ammo count to weapon
            for (OCREngine.DetectedText text : ammoTexts) {
                if (text.text.matches("\\d+/\\d+")) {
                    // Parse current/max ammo
                    String[] parts = text.text.split("/");
                    int currentAmmo = Integer.parseInt(parts[0]);
                    int maxAmmo = Integer.parseInt(parts[1]);

                    // Find weapon with matching ammo capacity
                    if (result.primaryWeapon != null &&
                            maxAmmo >= result.primaryWeapon.maxAmmo * 0.8f &&
                            maxAmmo <= result.primaryWeapon.maxAmmo * 1.2f) {
                        result.primaryWeapon.currentAmmo = currentAmmo;
                        return result.primaryWeapon;
                    }

                    if (result.secondaryWeapon != null &&
                            maxAmmo >= result.secondaryWeapon.maxAmmo * 0.8f &&
                            maxAmmo <= result.secondaryWeapon.maxAmmo * 1.2f) {
                        result.secondaryWeapon.currentAmmo = currentAmmo;
                        return result.secondaryWeapon;
                    }
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Active weapon detection failed", e);
        }

        // Default to primary weapon
        return result.primaryWeapon;
    }

    private void extractAmmoData(Bitmap screen, WeaponDetectionResult result) {
        try {
            // Extract ammo information from UI
            Rect ammoRegion = new Rect(screen.getWidth() - 250, screen.getHeight() - 200,
                    screen.getWidth(), screen.getHeight());

            List<OCREngine.DetectedText> ammoTexts =
                    ocrEngine.processScreenRegion(screen, ammoRegion).get();

            for (OCREngine.DetectedText text : ammoTexts) {
                if (text.text.matches("\\d+/\\d+")) {
                    String[] parts = text.text.split("/");
                    int current = Integer.parseInt(parts[0]);
                    int max = Integer.parseInt(parts[1]);

                    if (result.currentWeapon != null) {
                        result.currentWeapon.currentAmmo = current;
                        result.currentWeapon.maxAmmo = max;
                    }
                } else if (text.text.matches("\\d+")) {
                    int reserveAmmo = Integer.parseInt(text.text);
                    if (result.currentWeapon != null) {
                        result.currentWeapon.reserveAmmo = reserveAmmo;
                    }
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Ammo data extraction failed", e);
        }
    }

    private float calculateOverallConfidence(WeaponDetectionResult result) {
        float totalConfidence = 0f;
        int weaponCount = 0;

        if (result.primaryWeapon != null) {
            totalConfidence += result.primaryWeapon.confidence;
            weaponCount++;
        }

        if (result.secondaryWeapon != null) {
            totalConfidence += result.secondaryWeapon.confidence;
            weaponCount++;
        }

        if (result.meleeWeapon != null) {
            totalConfidence += result.meleeWeapon.confidence;
            weaponCount++;
        }

        return weaponCount > 0 ? totalConfidence / weaponCount : 0f;
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

    public WeaponProfile getWeaponProfile(String weaponName) {
        return weaponDatabase.get(weaponName.toLowerCase());
    }

    public boolean shouldReload(WeaponInfo weapon) {
        if (weapon == null) return false;

        float ammoPercentage = (float) weapon.currentAmmo / weapon.maxAmmo;

        // Reload thresholds based on weapon type
        switch (weapon.type) {
            case SNIPER_RIFLE:
                return ammoPercentage < 0.3f; // Reload earlier for precision weapons
            case SHOTGUN:
                return ammoPercentage < 0.5f; // Reload frequently for burst weapons
            case ASSAULT_RIFLE:
            case SMG:
                return ammoPercentage < 0.2f; // Reload when getting low
            default:
                return ammoPercentage < 0.1f; // Emergency reload
        }
    }

    public float calculateWeaponEffectiveness(WeaponInfo weapon, float targetDistance) {
        if (weapon == null) return 0f;

        WeaponProfile profile = getWeaponProfile(weapon.weaponName);
        if (profile == null) return 0.5f; // Unknown weapon, assume moderate effectiveness

        // Calculate effectiveness based on range
        float optimalRange = (profile.typicalRange[0] + profile.typicalRange[1]) / 2f;
        float rangeFactor = 1.0f - Math.abs(targetDistance - optimalRange) / optimalRange;
        rangeFactor = Math.max(0.1f, Math.min(1.0f, rangeFactor));

        // Factor in ammo availability
        float ammoFactor = weapon.currentAmmo > 0 ? 1.0f : 0.1f;

        // Factor in weapon condition/rarity
        float rarityFactor = weapon.rarity.ordinal() * 0.1f + 0.5f;

        return rangeFactor * ammoFactor * rarityFactor;
    }
    public static class WeaponData {
        public String name;
        public WeaponType type;
        public float confidence;
        public android.graphics.Rect boundingBox;
        public int width;
        public int height;
        public String nlpClassification;  // Add this
        public String strategicUse;       // Add this

        public WeaponData() {
            this.boundingBox = new android.graphics.Rect();
            this.nlpClassification = "";
            this.strategicUse = "";
        }
    }
    public void addNLPWeaponData(String description, WeaponData data) {
        if (nlpProcessor != null) {
            NLPProcessor.ActionIntent intent = nlpProcessor.processNaturalLanguageCommand(description);
            if (intent != null) {
                data.nlpClassification = intent.getAction();
                data.strategicUse = extractWeaponStrategy(description);
            }
        }
    }
    private String extractWeaponStrategy(String description) {
        if (nlpProcessor == null) return "standard_use";

        String weaponDesc = description + " combat usage";
        NLPProcessor.ActionIntent intent = nlpProcessor.processNaturalLanguageCommand(
                "use " + weaponDesc + " effectively in combat"
        );

        return intent != null ? intent.getAction() : "standard_use";
    }
    private void detectMeleeSubtype(org.opencv.core.Rect boundingRect, WeaponInfo weapon, MatOfPoint contour) {
        double area = Imgproc.contourArea(contour);
        double perimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
        double compactness = (perimeter * perimeter) / (4 * Math.PI * area);

        // Classify melee weapons by shape characteristics
        if (boundingRect.height > boundingRect.width * 4) {
            // Long thin weapons
            if (compactness < 2.0) {
                weapon.weaponName = "Katana";
            } else {
                weapon.weaponName = "Knife";
            }
        } else if (boundingRect.width > boundingRect.height * 2) {
            // Wide weapons
            if (area > 1000) {
                weapon.weaponName = "Pan";
            } else {
                weapon.weaponName = "Bat";
            }
        } else if (compactness > 3.0) {
            // Irregular shaped weapons
            weapon.weaponName = "Scythe";
        } else if (area < 500) {
            // Small compact weapons
            weapon.weaponName = "Dagger";
        } else {
            // Default to fists for very small/unclear shapes
            weapon.weaponName = "Fists";
        }
    }
}

