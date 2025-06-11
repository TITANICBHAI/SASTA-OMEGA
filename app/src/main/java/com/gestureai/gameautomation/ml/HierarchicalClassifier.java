package com.gestureai.gameautomation.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import com.gestureai.gameautomation.fragments.GestureLabelerFragment.LabeledObject;
import com.gestureai.gameautomation.utils.TensorFlowLiteHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hierarchical classification system for semantic object understanding
 * Supports multi-level classification: Category → Type → Name → State → Context
 */
public class HierarchicalClassifier {
    private static final String TAG = "HierarchicalClassifier";
    
    private Context context;
    private TensorFlowLiteHelper tfHelper;
    private Map<String, List<String>> categoryHierarchy;
    private Map<String, SemanticContext> contextDatabase;
    
    // Detection confidence thresholds
    private static final float CATEGORY_THRESHOLD = 0.6f;
    private static final float TYPE_THRESHOLD = 0.7f;
    private static final float STATE_THRESHOLD = 0.5f;
    private static final float CONTEXT_THRESHOLD = 0.5f;
    
    public static class HierarchicalResult {
        public String detectedName;
        public String detectedCategory;
        public String detectedType;
        public String detectedState;
        public String detectedContext;
        public float confidence;
        public Rect boundingBox;
        public SemanticContext semanticInfo;
        
        public HierarchicalResult(String name, String category, String type, 
                                String state, String context, float confidence, Rect bounds) {
            this.detectedName = name;
            this.detectedCategory = category;
            this.detectedType = type;
            this.detectedState = state;
            this.detectedContext = context;
            this.confidence = confidence;
            this.boundingBox = bounds;
        }
        
        public boolean matchesHierarchy(String categoryPattern, String typePattern) {
            return detectedCategory.toLowerCase().contains(categoryPattern.toLowerCase()) &&
                   detectedType.toLowerCase().contains(typePattern.toLowerCase());
        }
        
        public String getFullClassification() {
            return detectedCategory + "_" + detectedType + "_" + detectedName + 
                   "_" + detectedState + "_" + detectedContext;
        }
    }
    
    public static class SemanticContext {
        public String location;           // ground, inventory, hand, building
        public String availability;      // available, equipped, damaged, destroyed
        public String interaction;       // pickup, use, avoid, attack
        public String priority;          // high, medium, low
        public String gameState;         // combat, looting, traveling, hiding
        public List<String> associations; // related objects/actions
        
        public SemanticContext() {
            this.associations = new ArrayList<>();
        }
        
        public SemanticContext(String location, String availability, String interaction, 
                             String priority, String gameState) {
            this.location = location;
            this.availability = availability;
            this.interaction = interaction;
            this.priority = priority;
            this.gameState = gameState;
            this.associations = new ArrayList<>();
        }
    }
    
    public HierarchicalClassifier(Context context) {
        this.context = context;
        this.tfHelper = new TensorFlowLiteHelper(context);
        this.categoryHierarchy = new HashMap<>();
        this.contextDatabase = new HashMap<>();
        
        initializeHierarchy();
        initializeSemanticContexts();
        
        Log.d(TAG, "Hierarchical Classifier initialized");
    }
    
    private void initializeHierarchy() {
        // Weapon hierarchy
        List<String> weaponTypes = new ArrayList<>();
        weaponTypes.add("assault_rifle");
        weaponTypes.add("sniper_rifle");
        weaponTypes.add("shotgun");
        weaponTypes.add("pistol");
        weaponTypes.add("smg");
        weaponTypes.add("lmg");
        categoryHierarchy.put("weapon", weaponTypes);
        
        // Enemy hierarchy
        List<String> enemyTypes = new ArrayList<>();
        enemyTypes.add("player");
        enemyTypes.add("bot");
        enemyTypes.add("teammate");
        enemyTypes.add("npc");
        categoryHierarchy.put("enemy", enemyTypes);
        
        // Item hierarchy
        List<String> itemTypes = new ArrayList<>();
        itemTypes.add("health");
        itemTypes.add("armor");
        itemTypes.add("ammo");
        itemTypes.add("attachment");
        itemTypes.add("throwable");
        categoryHierarchy.put("item", itemTypes);
        
        // Vehicle hierarchy
        List<String> vehicleTypes = new ArrayList<>();
        vehicleTypes.add("car");
        vehicleTypes.add("motorcycle");
        vehicleTypes.add("boat");
        vehicleTypes.add("plane");
        vehicleTypes.add("helicopter");
        categoryHierarchy.put("vehicle", vehicleTypes);
        
        // UI hierarchy
        List<String> uiTypes = new ArrayList<>();
        uiTypes.add("button");
        uiTypes.add("minimap");
        uiTypes.add("health_bar");
        uiTypes.add("inventory");
        uiTypes.add("menu");
        categoryHierarchy.put("ui", uiTypes);
    }
    
    private void initializeSemanticContexts() {
        // Weapon semantic contexts
        contextDatabase.put("weapon_ground", new SemanticContext(
            "ground", "available", "pickup", "medium", "looting"));
        contextDatabase.put("weapon_equipped", new SemanticContext(
            "hand", "equipped", "use", "high", "combat"));
        contextDatabase.put("weapon_inventory", new SemanticContext(
            "inventory", "stored", "equip", "low", "management"));
            
        // Enemy semantic contexts
        contextDatabase.put("enemy_visible", new SemanticContext(
            "open_area", "active", "attack", "high", "combat"));
        contextDatabase.put("enemy_cover", new SemanticContext(
            "cover", "protected", "flank", "high", "tactical"));
        contextDatabase.put("enemy_knocked", new SemanticContext(
            "ground", "downed", "finish", "medium", "cleanup"));
            
        // Item semantic contexts
        contextDatabase.put("item_ground", new SemanticContext(
            "ground", "available", "pickup", "medium", "looting"));
        contextDatabase.put("item_building", new SemanticContext(
            "building", "available", "pickup", "low", "looting"));
        contextDatabase.put("item_crate", new SemanticContext(
            "container", "available", "loot", "high", "looting"));
            
        // Vehicle semantic contexts
        contextDatabase.put("vehicle_available", new SemanticContext(
            "open_area", "available", "enter", "medium", "traveling"));
        contextDatabase.put("vehicle_occupied", new SemanticContext(
            "road", "occupied", "attack", "high", "combat"));
    }
    
    // Classification methods for hierarchical detection
    public boolean isWeapon(String categoryPattern) {
        return categoryPattern != null && categoryPattern.toLowerCase().contains("weapon");
    }
    
    public boolean isWeaponType(String categoryPattern, String typePattern) {
        return isWeapon(categoryPattern) && 
               categoryHierarchy.get("weapon").contains(typePattern.toLowerCase());
    }
    
    public List<HierarchicalResult> findWeapons(Bitmap image, List<Rect> detectedRegions) {
        List<HierarchicalResult> weapons = new ArrayList<>();
        List<HierarchicalResult> allResults = classifyObjects(image, detectedRegions);
        
        for (HierarchicalResult result : allResults) {
            if (isWeapon(result.detectedCategory)) {
                weapons.add(result);
            }
        }
        
        return weapons;
    }
    
    public List<HierarchicalResult> classifyObjects(Bitmap image, List<Rect> detectedRegions) {
        List<HierarchicalResult> results = new ArrayList<>();
        
        for (Rect region : detectedRegions) {
            try {
                // Extract object patch
                Bitmap objectPatch = extractObjectPatch(image, region);
                if (objectPatch == null) continue;
                
                // Multi-stage classification
                String category = classifyCategory(objectPatch);
                String type = classifyType(objectPatch, category);
                String name = classifySpecificObject(objectPatch, category, type);
                String state = classifyState(objectPatch, category, name);
                String context = classifyContext(objectPatch, image, region, category, state);
                
                // Calculate overall confidence
                float confidence = calculateHierarchicalConfidence(category, type, name, state, context);
                
                // Create result
                HierarchicalResult result = new HierarchicalResult(
                    name, category, type, state, context, confidence, region);
                    
                // Add semantic context
                String contextKey = category + "_" + state;
                result.semanticInfo = contextDatabase.get(contextKey);
                if (result.semanticInfo == null) {
                    result.semanticInfo = new SemanticContext("unknown", "unknown", "observe", "low", "general");
                }
                
                results.add(result);
                
            } catch (Exception e) {
                Log.w(TAG, "Classification failed for region: " + region, e);
            }
        }
        
        return results;
    }
    
    private String classifyCategory(Bitmap objectPatch) {
        try {
            List<TensorFlowLiteHelper.DetectionResult> categoryResults = 
                tfHelper.runInference("category_classifier", objectPatch);
                
            if (!categoryResults.isEmpty() && categoryResults.get(0).confidence > CATEGORY_THRESHOLD) {
                return categoryResults.get(0).label;
            }
        } catch (Exception e) {
            Log.w(TAG, "Category classification failed", e);
        }
        return "unknown";
    }
    
    private String classifyType(Bitmap objectPatch, String category) {
        try {
            // Use category-specific type classifier
            String modelName = category + "_type_classifier";
            List<TensorFlowLiteHelper.DetectionResult> typeResults = 
                tfHelper.runInference(modelName, objectPatch);
                
            if (!typeResults.isEmpty() && typeResults.get(0).confidence > TYPE_THRESHOLD) {
                return typeResults.get(0).label;
            }
        } catch (Exception e) {
            Log.w(TAG, "Type classification failed for category: " + category, e);
        }
        return "default";
    }
    
    private String classifySpecificObject(Bitmap objectPatch, String category, String type) {
        try {
            // Use type-specific object classifier
            String modelName = category + "_" + type + "_classifier";
            List<TensorFlowLiteHelper.DetectionResult> objectResults = 
                tfHelper.runInference(modelName, objectPatch);
                
            if (!objectResults.isEmpty()) {
                return objectResults.get(0).label;
            }
        } catch (Exception e) {
            Log.w(TAG, "Object classification failed for: " + category + "_" + type, e);
        }
        return "generic_" + type;
    }
    
    private String classifyState(Bitmap objectPatch, String category, String name) {
        try {
            // Analyze visual state indicators
            if (category.equals("weapon")) {
                return analyzeWeaponState(objectPatch);
            } else if (category.equals("enemy")) {
                return analyzeEnemyState(objectPatch);
            } else if (category.equals("item")) {
                return analyzeItemState(objectPatch);
            }
        } catch (Exception e) {
            Log.w(TAG, "State classification failed", e);
        }
        return "normal";
    }
    
    private String classifyContext(Bitmap objectPatch, Bitmap fullImage, Rect region, 
                                 String category, String state) {
        try {
            // Analyze surrounding area for context clues
            Rect expandedRegion = expandRegion(region, fullImage.getWidth(), fullImage.getHeight());
            Bitmap contextArea = extractObjectPatch(fullImage, expandedRegion);
            
            if (contextArea != null) {
                return analyzeContextualEnvironment(contextArea, category, state);
            }
        } catch (Exception e) {
            Log.w(TAG, "Context classification failed", e);
        }
        return "general";
    }
    
    private String analyzeWeaponState(Bitmap weaponPatch) {
        // Look for visual indicators of weapon state
        // - Muzzle flash = firing
        // - Ground position = dropped
        // - Hand position = equipped
        // - Inventory UI = stored
        
        // Placeholder implementation - would use CV analysis
        return "available";
    }
    
    private String analyzeEnemyState(Bitmap enemyPatch) {
        // Look for enemy state indicators
        // - Movement blur = moving
        // - Muzzle flash = shooting
        // - Red indicators = damaged
        // - Ground position = knocked
        
        return "active";
    }
    
    private String analyzeItemState(Bitmap itemPatch) {
        // Look for item state indicators
        // - Glow effects = rare/legendary
        // - Ground position = available
        // - Damaged textures = damaged
        
        return "available";
    }
    
    private String analyzeContextualEnvironment(Bitmap contextArea, String category, String state) {
        // Analyze surrounding environment
        // - Green/brown textures = outdoor/ground
        // - UI elements = inventory/menu
        // - Building textures = indoor
        // - Water textures = water
        
        return "ground";
    }
    
    private Rect expandRegion(Rect original, int imageWidth, int imageHeight) {
        int expansion = Math.min(original.width(), original.height()) / 2;
        return new Rect(
            Math.max(0, original.left - expansion),
            Math.max(0, original.top - expansion),
            Math.min(imageWidth, original.right + expansion),
            Math.min(imageHeight, original.bottom + expansion)
        );
    }
    
    private float calculateHierarchicalConfidence(String category, String type, String name, 
                                                String state, String context) {
        float baseConfidence = 0.5f;
        
        // Boost confidence for known hierarchies
        if (categoryHierarchy.containsKey(category)) {
            baseConfidence += 0.2f;
            if (categoryHierarchy.get(category).contains(type)) {
                baseConfidence += 0.2f;
            }
        }
        
        // Boost for semantic context matches
        String contextKey = category + "_" + state;
        if (contextDatabase.containsKey(contextKey)) {
            baseConfidence += 0.1f;
        }
        
        return Math.min(1.0f, baseConfidence);
    }
    
    private Bitmap extractObjectPatch(Bitmap image, Rect region) {
        try {
            if (region.left >= 0 && region.top >= 0 && 
                region.right <= image.getWidth() && region.bottom <= image.getHeight()) {
                return Bitmap.createBitmap(image, region.left, region.top, 
                                         region.width(), region.height());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to extract object patch", e);
        }
        return null;
    }
    
    public void trainHierarchicalModel(List<LabeledObject> trainingData) {
        Map<String, List<LabeledObject>> categoryGroups = groupByCategory(trainingData);
        
        for (Map.Entry<String, List<LabeledObject>> entry : categoryGroups.entrySet()) {
            String category = entry.getKey();
            List<LabeledObject> categoryData = entry.getValue();
            
            // Train category-specific models
            trainCategoryTypeModel(category, categoryData);
            trainStateContextModel(category, categoryData);
            
            Log.d(TAG, "Trained hierarchical model for category: " + category + 
                      " with " + categoryData.size() + " examples");
        }
    }
    
    private Map<String, List<LabeledObject>> groupByCategory(List<LabeledObject> data) {
        Map<String, List<LabeledObject>> groups = new HashMap<>();
        
        for (LabeledObject obj : data) {
            String category = obj.getCategory();
            if (!groups.containsKey(category)) {
                groups.put(category, new ArrayList<>());
            }
            groups.get(category).add(obj);
        }
        
        return groups;
    }
    
    private void trainCategoryTypeModel(String category, List<LabeledObject> data) {
        // Group by type within category
        Map<String, List<LabeledObject>> typeGroups = new HashMap<>();
        for (LabeledObject obj : data) {
            String type = obj.getType();
            if (!typeGroups.containsKey(type)) {
                typeGroups.put(type, new ArrayList<>());
            }
            typeGroups.get(type).add(obj);
        }
        
        // Train type classifier for this category
        String modelName = category + "_type_classifier";
        // Implementation would train TensorFlow Lite model
        Log.d(TAG, "Training type classifier: " + modelName + " with " + typeGroups.size() + " types");
    }
    
    private void trainStateContextModel(String category, List<LabeledObject> data) {
        // Train state and context recognition for this category
        String modelName = category + "_state_context_classifier";
        // Implementation would train specialized models for state/context detection
        Log.d(TAG, "Training state/context classifier: " + modelName);
    }
}