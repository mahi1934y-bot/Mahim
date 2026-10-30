# MAHIM AI SIGNAL PRO — Stage 1

এটি Android Studio-র Kotlin native project। প্রথম ধাপে Dashboard, draggable floating bubble,
Android-এর official MediaProjection consent flow, foreground capture service এবং bubble tap-এ
একটি fresh frame acquire করার ভিত্তি তৈরি হয়েছে।

## প্রয়োজন
- Android Studio (একটি সাম্প্রতিক stable সংস্করণ)
- JDK 17
- Android SDK Platform 35
- Android 14 বা পরের ফোন

## চালানোর নিয়ম
1. ZIP extract করুন।
2. Android Studio → Open → `MAHIM_AI_SIGNAL_PRO` ফোল্ডার খুলুন।
3. Gradle sync শেষ হতে দিন।
4. USB debugging চালু করে ফোন connect করুন।
5. Run চাপুন।
6. অ্যাপে `Floating permission` চাপুন, Android Settings-এ অনুমতি দিন।
7. ফিরে এসে `Floating Bubble ON` চাপুন।
8. `Start Screen Sharing` চাপুন এবং Android-এর system consent dialog-এ Start now দিন।
9. Quotex খুলে AI PRO bubble চাপুন। এই stage-এ capture status internally broadcast হয়।

## গুরুত্বপূর্ণ সীমাবদ্ধতা
- এই stage এখনো Gemini-তে ছবি পাঠায় না এবং UP/DOWN signal তৈরি করে না।
- Frame capture-এর পরে bitmap এখন ইচ্ছাকৃতভাবে recycle করা হয়। পরের stage-এ quality validation,
  Gemini request এবং result panel যুক্ত হবে।
- Android 14+ এ screen capture permission প্রতিটি capture session-এর জন্য user consent-নির্ভর।
- Android 14+ এ screen share শুরু করার আগে app notification permission চাইতে পারে।
- Screen capture-এ notification, password field বা sensitive content-ও থাকতে পারে। Quotex ছাড়া
  অন্য সংবেদনশীল স্ক্রিনে capture চালু করবেন না।
- Floating overlay permission এবং screen capture permission আলাদা।
- API key এই stage-এ নেই। Production-এ Gemini API key সরাসরি APK-তে রাখা নিরাপদ নয়;
  secure backend ব্যবহার করা উচিত।

## Stage 1-এর ফাইল
- `MainActivity.kt`: permission ও service controls
- `FloatingOverlayService.kt`: draggable AI PRO overlay
- `ScreenCaptureService.kt`: MediaProjection foreground service ও tap-triggered frame acquire
- `SignalResult.kt`: পরবর্তী signal model-এর ভিত্তি
