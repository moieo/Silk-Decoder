package com.xmoieo.silk;

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.documentfile.provider.DocumentFile

/**
 * Android 11+ (API 30+) 访问 Android/data 目录的工具类
 * 支持 Android 11 (R) 到 Android 16 (API 35+)
 */
public class FileUriUtils {

    companion object {
        
        // Android/data 目录的 URI
        private const val ANDROID_DATA_URI = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata"
        
        /**
         * 判断是否已经获取了 Android/data 权限
         */
        fun isGrant(context: Context): Boolean {
            for (persistedUriPermission in context.contentResolver.persistedUriPermissions) {
                if (persistedUriPermission.isReadPermission) {
                    val uriStr = persistedUriPermission.uri.toString()
                    // 检查是否包含 Android/data 路径
                    if (uriStr.contains("Android%2Fdata") || uriStr == ANDROID_DATA_URI) {
                        return true
                    }
                }
            }
            return false
        }
        
        /**
         * 判断是否已经获取了指定目录的权限
         */
        fun isGrantForPath(context: Context, path: String): Boolean {
            val targetUri = changeToUri(path)
            for (persistedUriPermission in context.contentResolver.persistedUriPermissions) {
                if (persistedUriPermission.isReadPermission) {
                    val uriStr = persistedUriPermission.uri.toString()
                    if (uriStr == ANDROID_DATA_URI || targetUri.startsWith(uriStr)) {
                        return true
                    }
                }
            }
            return false
        }
        
        /**
         * 检查是否需要特殊权限访问 Android/data
         * Android 11+ 需要通过 SAF 获取权限
         */
        fun needsSpecialPermission(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        }
        
        /**
         * 检查是否有 MANAGE_EXTERNAL_STORAGE 权限（Android 11+）
         */
        fun hasManageExternalStoragePermission(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        }
        
        /**
         * 请求 MANAGE_EXTERNAL_STORAGE 权限
         */
        fun requestManageExternalStoragePermission(activity: Activity, requestCode: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivityForResult(intent, requestCode)
            }
        }
    
        /**
         * 直接返回 DocumentFile
         */
        fun getDocumentFilePath(context: Context, path: String, sdCardUri: String): DocumentFile? {
            var document = DocumentFile.fromTreeUri(context, Uri.parse(sdCardUri))
            val parts = path.split("/")
            for (i in 3 until parts.size) {
                document = document?.findFile(parts[i])
                if (document == null) break
            }
            return document
        }
    
        /**
         * 转换至 uriTree 的路径
         */
        fun changeToUri(path: String): String {
            val path2 = path.replace("/storage/emulated/0/", "").replace("/", "%2F")
            return "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A$path2"
        }
    
        /**
         * 转换至 uriTree 的路径并获取 DocumentFile
         */
        fun getDocumentFile(context: Context, path: String): DocumentFile? {
            var path1 = path
            if (path1.endsWith("/")) {
                path1 = path1.substring(0, path1.length - 1)
            }
            var path2 = path1.replace("/storage/emulated/0/", "")
            path2 = path2.replace("/", "%2F")
            
            val uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A$path2")
            return DocumentFile.fromSingleUri(context, uri)
        }
    
        /**
         * 转换至 uriTree 的路径（用于 Android/data 子目录）
         */
        fun changeToUri2(path: String): String {
            val paths = path.replace("/storage/emulated/0/Android/data", "").split("/")
            val stringBuilder = StringBuilder("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3AAndroid%2Fdata")
            for (p in paths) {
                if (p.isEmpty()) {
                    continue
                }
                stringBuilder.append("%2F")
                stringBuilder.append(p)
            }
            return stringBuilder.toString()
        }
    
        /**
         * 转换至 uriTree 的路径（通用）
         */
        fun changeToUri3(path: String): String {
            var path1 = path.replace("/storage/emulated/0/", "")
            path1 = path1.replace("/", "%2F")
            return "content://com.android.externalstorage.documents/tree/primary%3A$path1"
        }

        /**
         * 获取指定目录的权限
         * 适用于 Android 11+ (API 30+)
         */
        fun startFor(path: String, context: Activity, requestCode: Int) {
            val uri = changeToUri(path)
            val parse = Uri.parse(uri)
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, parse)
            }
            context.startActivityForResult(intent, requestCode)
        }
        
        /**
         * 直接获取 Android/data 权限
         * 这是推荐的方案，一次授权可访问所有 Android/data 子目录
         * 
         * 注意：
         * - Android 11 (API 30): 需要用户手动选择 Android/data 目录
         * - Android 12 (API 31): 同上
         * - Android 13+ (API 33+): Google 进一步限制了对 Android/data 的访问
         *   部分设备可能无法通过 SAF 访问，需要使用 Shizuku 或 Root 方案
         */
        fun startForRoot(context: Activity, requestCode: Int) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            
            // Android 8.0+ 可以设置初始目录，引导用户到 Android/data
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 构建指向 Android/data 的 URI
                val dataUri = Uri.Builder()
                    .scheme("content")
                    .authority("com.android.externalstorage.documents")
                    .appendPath("document")
                    .appendPath("primary:Android/data")
                    .build()
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, dataUri)
            }
            
            context.startActivityForResult(intent, requestCode)
        }
        
        /**
         * 处理权限请求结果
         * 在 onActivityResult 中调用
         */
        fun handleActivityResult(context: Context, requestCode: Int, resultCode: Int, data: Intent?, expectedRequestCode: Int): Boolean {
            if (requestCode != expectedRequestCode || resultCode != Activity.RESULT_OK) {
                return false
            }
            
            val uri = data?.data ?: return false
            
            // 持久化权限
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, data.flags and flags)
            
            return true
        }
        
        /**
         * 获取 Android/data 目录的 DocumentFile
         * 需要先调用 startForRoot 获取权限
         */
        fun getAndroidDataDocumentFile(context: Context): DocumentFile? {
            // 查找包含 Android/data 的已授权 URI
            for (persistedUriPermission in context.contentResolver.persistedUriPermissions) {
                if (persistedUriPermission.isReadPermission) {
                    val uriStr = persistedUriPermission.uri.toString()
                    if (uriStr.contains("Android%2Fdata") || uriStr == ANDROID_DATA_URI) {
                        return DocumentFile.fromTreeUri(context, persistedUriPermission.uri)
                    }
                }
            }
            return null
        }
        
        /**
         * 获取指定应用的 Android/data 目录
         */
        fun getAppDataDocumentFile(context: Context, packageName: String): DocumentFile? {
            val androidData = getAndroidDataDocumentFile(context) ?: return null
            return androidData.findFile(packageName)
        }
        
        /**
         * 释放已持久化的权限
         */
        fun releasePermission(context: Context) {
            for (permission in context.contentResolver.persistedUriPermissions) {
                if (permission.uri.toString() == ANDROID_DATA_URI) {
                    context.contentResolver.releasePersistableUriPermission(
                        permission.uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    break
                }
            }
        }
    }
}

