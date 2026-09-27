# Car API classes are provided by the platform on AAOS; never strip references to them.
-dontwarn android.car.**
-keep class android.car.** { *; }
