package com.gestureai.gameautomation.utils;
 public class TimeUtils {
     public static String formatTime(long milliseconds) {
         long seconds = milliseconds / 1000;
         long minutes = seconds / 60;
         long hours = minutes / 60;
         return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
     }

     public static String formatTimeShort(long milliseconds) {
         long seconds = milliseconds / 1000;
         long minutes = seconds / 60;
         long hours = minutes / 60;

         if (hours > 0) {
             return String.format("%dh %dm", hours, minutes % 60);
         } else {
             return String.format("%dm %ds", minutes, seconds % 60);
         }
     }
 }