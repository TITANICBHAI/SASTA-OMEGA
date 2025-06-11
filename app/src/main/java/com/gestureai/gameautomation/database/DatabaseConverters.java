package com.gestureai.gameautomation.database;

import androidx.room.TypeConverter;
import android.graphics.Point;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class DatabaseConverters {
    
    private static final Gson gson = new Gson();
    
    @TypeConverter
    public static String fromPointList(List<Point> points) {
        if (points == null) {
            return null;
        }
        return gson.toJson(points);
    }
    
    @TypeConverter
    public static List<Point> toPointList(String pointsString) {
        if (pointsString == null) {
            return null;
        }
        Type listType = new TypeToken<List<Point>>(){}.getType();
        return gson.fromJson(pointsString, listType);
    }
    
    @TypeConverter
    public static String fromFloatList(List<Float> floats) {
        if (floats == null) {
            return null;
        }
        return gson.toJson(floats);
    }
    
    @TypeConverter
    public static List<Float> toFloatList(String floatsString) {
        if (floatsString == null) {
            return null;
        }
        Type listType = new TypeToken<List<Float>>(){}.getType();
        return gson.fromJson(floatsString, listType);
    }
    
    @TypeConverter
    public static String fromStringList(List<String> strings) {
        if (strings == null) {
            return null;
        }
        return gson.toJson(strings);
    }
    
    @TypeConverter
    public static List<String> toStringList(String stringsString) {
        if (stringsString == null) {
            return null;
        }
        Type listType = new TypeToken<List<String>>(){}.getType();
        return gson.fromJson(stringsString, listType);
    }
}