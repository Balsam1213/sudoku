# Add project specific ProGuard rules here.

# kotlinx-serialization：保留生成的序列化器（GameSave 等存档模型经 DataStore JSON 持久化）
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.balsam.sudoku.**$$serializer { *; }
-keepclassmembers class com.balsam.sudoku.** {
    *** Companion;
}
-keepclasseswithmembers class com.balsam.sudoku.** {
    kotlinx.serialization.KSerializer serializer(...);
}
