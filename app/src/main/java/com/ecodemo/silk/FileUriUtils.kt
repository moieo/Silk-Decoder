package com.ecodemo.silk;

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

public class FileUriUtils {

    companion object {
    
        //判断是否已经获取了Data权限
        fun isGrant(context: Context): Boolean {
            for (persistedUriPermission in context.getContentResolver().getPersistedUriPermissions()) {
                if (persistedUriPermission.isReadPermission() && persistedUriPermission.getUri().toString().equals("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata")) {
                    return true
                }
            }
            return false
        }
    
        //直接返回DocumentFile
        fun getDocumentFilePath(context: Context, path: String, sdCardUri: String): DocumentFile {
            var document = DocumentFile.fromTreeUri(context, Uri.parse(sdCardUri))
            var parts = path.split("/")
            for (i in 3 until parts.size) {
                document = document?.findFile(parts[i])
            }
            return document as DocumentFile
        }
    
        //转换至uriTree的路径
        fun changeToUri(path: String): String {
            /*var path1: String = String()
            if (path.endsWith("/")) {
                path1 = path.substring(0, path.length - 1)
            }*/
            var path2 = path.replace("/storage/emulated/0/", "").replace("/", "%2F")
            return "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A" + path2
        }
    
        //转换至uriTree的路径
        fun getDoucmentFile(context: Context, path: String): DocumentFile {
            var path1: String = String()
            if (path.endsWith("/")) {
                path1 = path.substring(0, path.length - 1)
            }
            var path2 = path1.replace("/storage/emulated/0/", "")
            path2 = path2.replace("/", "%2F")
            
            var document = DocumentFile.fromSingleUri(context, Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A" + path2))
            return document as DocumentFile
        }
    
    
        //转换至uriTree的路径
        fun changeToUri2(path: String): String{
            var paths = path.replace("/storage/emulated/0/Android/data", "").split("/")
            var stringBuilder = StringBuilder("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3AAndroid%2Fdata")
            for (p in paths) {
                if (p.length == 0){
                    continue;
                }
                stringBuilder.append("%2F")
                stringBuilder.append(p)
            }
            return stringBuilder.toString()
        }
    
    
        //转换至uriTree的路径
        fun changeToUri3(path: String): String {
            var path1 = path.replace("/storage/emulated/0/", "")
            path1 = path1.replace("/", "%2F")
            return ("content://com.android.externalstorage.documents/tree/primary%3A" + path1)
        }
    
    //获取指定目录的权限
        fun startFor(path: String, context: Activity, REQUEST_CODE_FOR_DIR: Int) {
            var uri = changeToUri(path)
            var parse = Uri.parse(uri)
            var intent = Intent("android.intent.action.OPEN_DOCUMENT_TREE")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                              Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                              Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                              Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, parse)
            }
            context.startActivityForResult(intent, REQUEST_CODE_FOR_DIR)//开始授权
        }
        
    //直接获取data权限，推荐使用这种方案
        fun startForRoot(context: Activity, REQUEST_CODE_FOR_DIR: Int) {
            var uri1 = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata")
    //        DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri1)
          /*  var uri = changeToUri(Environment.getExternalStorageDirectory().getPath())
            uri = uri + "/document/primary%3A" + Environment.getExternalStorageDirectory().getPath().replace("/storage/emulated/0/", "").replace("/", "%2F")*/
            //var parse = Uri.parse(uri)
           /* 上面的没啥用 */
            var documentFile = DocumentFile.fromTreeUri(context, uri1)
            var intent1 = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent1.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                     Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                     Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                     Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            intent1.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentFile?.getUri())
            context.startActivityForResult(intent1, REQUEST_CODE_FOR_DIR)
        }
    }

}

