package com.vpaliy.studioq.common.dataUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Random;

import com.vpaliy.studioq.activities.MediaUtilCreatorScreen;
import com.vpaliy.studioq.model.MediaFile;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class FileUtils {

    private final static String TAG=FileUtils.class.getSimpleName();

    private FileUtils() {
        throw new UnsupportedOperationException();
    }


    private static void makeFileCopy(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            try {
                if (inputChannel != null)
                    inputChannel.close();
                if (outputChannel != null)
                    outputChannel.close();
            }catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void deleteFile(Context context, MediaFile mediaFile) {
        String pathTo=mediaFile.mediaFile().getAbsolutePath();

        if(!mediaFile.mediaFile().delete()) {
            Log.d(TAG, "1:Failed to delete file2:" + pathTo);
            pathTo=uniqueNameFor(pathTo);
            File file=new File(pathTo);
            if(!file.delete()) {
                Log.d(TAG, "2:Failed to delete file:" + pathTo);
            }
           return;
        }

        if(mediaFile.getType()== MediaFile.Type.VIDEO) {
            deleteVideo(context, pathTo);
        }else {
            deleteImage(context, pathTo);
        }
    }

    private static void deleteVideo(@NonNull Context context, @NonNull String pathTo) {
        String[] projection = { MediaStore.Video.Media._ID };

        String selection = MediaStore.Video.Media.DATA + " = ?";
        String[] selectionArgs = new String[] { pathTo };

        Uri queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                Uri deleteUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                contentResolver.delete(deleteUri, null, null);
            }
            cursor.close();
        }
    }

    private static void deleteImage(@NonNull Context context, @NonNull String pathTo) {
        String[] projection = { MediaStore.Images.Media._ID };

        String selection = MediaStore.Images.Media.DATA + " = ?";
        String[] selectionArgs = new String[] { pathTo };

        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
        if(cursor!=null) {
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                contentResolver.delete(deleteUri, null, null);
            }
            cursor.close();
        }
    }


    public static void deleteFileList(Context context, List<? extends MediaFile> deleteMediaFileList) {
        for (MediaFile mediaFile : deleteMediaFileList) {
            deleteFile(context,mediaFile);
        }


    }

    public static void copyFileList(Context context, List<MediaFile> contentList, File mediaFolder) {
        if (contentList != null) {
            ContentValues values=new ContentValues();
            for (int index=0;index<contentList.size();index++) {
                MediaFile mediaFile=contentList.get(index);
                String fileName=mediaFile.mediaFile().getName();

                boolean isVideo=mediaFile.getType()== MediaFile.Type.VIDEO;
                File file = new File(mediaFolder, fileName);
                //let the user to decide whether to create a copy of already existing files
                if(file.exists()) {
                    file=new File(mediaFolder,uniqueNameFor(fileName));
                    Log.d(TAG,"Unique name:"+file.getAbsolutePath());
                }

                if(!file.exists()) {
                    try {
                        FileUtils.makeFileCopy(mediaFile.mediaFile().getAbsoluteFile(), file);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        continue;
                    }

                    if (isVideo) {
                        values.put(MediaStore.Video.VideoColumns.DATA, file.getAbsolutePath());
                        values.put(MediaStore.Video.VideoColumns.MIME_TYPE,mediaFile.getMimeType());
                        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                    } else {
                        values.put(MediaStore.Images.ImageColumns.DATA, file.getAbsolutePath());
                        values.put(MediaStore.Images.ImageColumns.MIME_TYPE,mediaFile.getMimeType());
                        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    }

                }
                values.clear();
            }
        }
    }

    public static void updateWithImage(@NonNull Context context, @NonNull File target, @NonNull String mimeType) {
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.ImageColumns.DATA,target.getAbsolutePath());
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE,mimeType);
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static void updateWithVideo(@NonNull Context context, @NonNull File target, @NonNull String mimeType) {
        ContentValues values=new ContentValues();
        values.put(MediaStore.Video.VideoColumns.DATA, target.getAbsolutePath());
        values.put(MediaStore.Video.VideoColumns.MIME_TYPE,mimeType);
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    private static String uniqueNameFor(@NonNull String fileName) {
        Random random=new Random();
        fileName+=Integer.toString(random.nextInt(100));
        return fileName;
    }

    public static File createFolderInExternalStorage(@NonNull Context context, @NonNull String folderName) {
        String pathTo = Environment.getExternalStorageDirectory() + File.separator + folderName+File.separator;
        File mediaFolder = new File(pathTo);
        if (!mediaFolder.mkdir()) {
            Toast.makeText(context, "Failed to create the folder", Toast.LENGTH_SHORT).show();
            return null;
        }
        return mediaFolder;
    }

    private static MediaFile convertToCopy(File to, MediaFile from) {
        return new MediaFile(from,to);
    }

}
