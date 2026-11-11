# MuPDF's fitz bindings are reached from JNI code (native callbacks and
# constructors are looked up by name at runtime), so they must not be
# renamed or removed.
-keep class com.artifex.mupdf.** { *; }
