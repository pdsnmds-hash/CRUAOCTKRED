# ── Namespace isolation ──
# Repackage ALL obfuscated classes under a product-specific prefix.
# After R8 there is no trace of the platform namespace (com.cruaoctkred.app.*) in the APK.
# Platform modules keep their source namespace on purpose — isolation happens here.
# Change this prefix when you change applicationId; anything but com.cruaoctkred.app works.
-repackageclasses 'com.cruaoctkred.i'
-allowaccessmodification

# ── Keep product entry points (referenced by name from AndroidManifest) ──
# EVERY entry point you add — Service, Receiver, Provider, Activity, Application —
# needs a line here, or R8 renames it and the manifest no longer finds it. This only
# ever fails in release, because debug does not run R8. AGENTS.md section 3.2.
-keep class com.cruaoctkred.app.ProductApp { *; }
-keep class com.cruaoctkred.app.MainActivity { *; }
-keep class com.cruaoctkred.app.data.push.ProductMessagingService { *; }

# ── Firebase ──
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-keep class com.cruaoctkred.app.reminders.PaymentReminderReceiver { *; }
