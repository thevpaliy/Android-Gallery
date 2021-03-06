package com.vpaliy.studioq.model;


import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.parceler.ParcelConverter;
import org.parceler.Parcels;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.support.annotation.NonNull;

@SuppressWarnings("WeakerAccess")
public class MediaFile implements Parcelable{

    private static final String TAG=MediaFile.class.getSimpleName();

    protected String  mediaFile;
    protected String mimeType;
    protected long Id;
    protected Type type;

    private String referencePath;
    private boolean isReference;

    public MediaFile(@NonNull Cursor cursor, Type type) {
        this.type=type;
        this.mediaFile=cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
        this.mimeType=cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));
        this.Id=cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
        this.isReference=false;
    }

    public MediaFile(MediaFile mediaFile, File file) {
        this.mediaFile=file.getAbsolutePath();
        this.mimeType=mediaFile.mimeType;
        this.Id=mediaFile.Id;
        this.type=mediaFile.type;
        this.isReference=mediaFile.isReference();
        this.referencePath=mediaFile.referencePath;
    }

    public MediaFile(Parcel in) {
        this.mediaFile=in.readString();
        this.mimeType=in.readString();
        this.Id=in.readLong();
        this.type=Type.valueOf(in.readString());
        this.isReference=in.readInt()==1;
        if(isReference) {
            this.referencePath=in.readString();
        }

    }

    private MediaFile(@NonNull String referencePath, @NonNull MediaFile model) {
        this.referencePath=referencePath;
        this.mediaFile=model.pathToMediaFile();
        this.mimeType=model.mimeType;
        this.Id=model.Id;
        this.type=model.type;
        this.isReference=true;
    }

    public Uri uri() {
        return Uri.fromFile(mediaFile());
    }

    public File mediaFile() {
        return new File(mediaFile);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mediaFile);
        out.writeString(mimeType);
        out.writeLong(Id);
        out.writeString(type.name());
        out.writeInt(isReference?1:0);
        if(isReference) {
            out.writeString(referencePath);
        }
    }

    public String pathToMediaFile() {
        return mediaFile;
    }

    public String parentPath() {
        if(isReference) {
            return referencePath;
        }
        return mediaFile().getParentFile().getAbsolutePath();
    }

    public String getDate() {
        Date date=new Date(new File(mediaFile).lastModified());
        SimpleDateFormat dateFormat=new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return dateFormat.format(date).intern();
    }

    public final static Creator<MediaFile> CREATOR =new Creator<MediaFile>() {
        @Override
        public MediaFile createFromParcel(Parcel parcel) {
            return new MediaFile(parcel);
        }

        @Override
        public MediaFile[] newArray(int i) {
            return new MediaFile[0];
        }
    };

    public String getMimeType() {
        return mimeType;
    }

    public static class MediaListConverter
            implements ParcelConverter<List<MediaFile>> {

        @Override
        public List<MediaFile> fromParcel(Parcel parcel) {
            int size = parcel.readInt();
            if (size < 0)
                 return null;
            List<MediaFile> items = new ArrayList<>();
            for (int i = 0; i < size; ++i) {
                items.add((MediaFile) Parcels.unwrap(parcel.readParcelable(MediaFile.class.getClassLoader())));
            }
            return items;
        }

        @Override
        public void toParcel(List<MediaFile> input, Parcel parcel) {
            if(input==null) {
                parcel.writeInt(-1);
            }else {
                parcel.writeInt(input.size());
                for(MediaFile mediaFile:input) {
                    parcel.writeParcelable(Parcels.wrap(mediaFile), 0);
                }
            }
        }
    }

    public static class MediaConverter
            implements ParcelConverter<MediaFile> {

        @Override
        public void toParcel(MediaFile input, Parcel parcel) {
            parcel.writeParcelable(Parcels.wrap(input),0);
        }

        @Override
        public MediaFile fromParcel(Parcel parcel) {
            return (MediaFile)Parcels.unwrap(parcel.readParcelable(MediaFile.class.getClassLoader()));
        }
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder=new HashCodeBuilder()
                .append(mediaFile)
                .append(mimeType)
                .append(Id);
        return builder.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof MediaFile)) {
            return false;
        }
        MediaFile file=(MediaFile)(obj);
        return new EqualsBuilder()
                .append(mediaFile,file.mediaFile)
                .append(mimeType,file.mimeType)
                .append(Id,file.Id).isEquals();
    }

    public boolean isReference() {
        return isReference;
    }

    public MediaFile realFile() {
        if(referencePath==null) {
            return this;
        }
        mediaFile=referencePath+File.separator+mediaFile().getName();
        return this;
    }

    public static MediaFile createFrom(@NonNull Uri uri, @NonNull MediaFile copy) {
        return new MediaFile(copy,new File(uri.getPath()));
    }

    public static MediaFile createReference(@NonNull String referencePath, @NonNull MediaFile model) {
       return new MediaFile(referencePath,model);
    }

    public static ArrayList<MediaFile> createReferenceList(@NonNull String referencePath,
                         @NonNull ArrayList<MediaFile> modelList) {
        ArrayList<MediaFile> list=new ArrayList<>(modelList.size());
        for(MediaFile file:modelList) {
            list.add(createReference(referencePath,file));
        }
        return list;
    }


    public long getId() {
        return Id;
    }

    public Type getType() {
        return type;
    }

    public static final  Comparator<MediaFile> BY_SIZE=new Comparator<MediaFile>() {
        @Override
        public int compare(MediaFile o1, MediaFile o2) {
            if(o1.mediaFile().length()>o2.mediaFile().length()) {
                return 1;
            }else if(o1.mediaFile().length()<o2.mediaFile().length()) {
                return -1;
            }
            return 0;
        }
    };

    public static final  Comparator<MediaFile> BY_DATE=new Comparator<MediaFile>() {
        @Override
        public int compare(MediaFile o1, MediaFile o2) {
            if(o1.mediaFile().lastModified()>o2.mediaFile().lastModified()) {
                return 1;
            }else if(o1.mediaFile().lastModified()<o2.mediaFile().lastModified()) {
                return -1;
            }
            return 0;
        }
    };


    public static final  Comparator<MediaFile> BY_NAME=new Comparator<MediaFile>() {
        @Override
        public int compare(MediaFile o1, MediaFile o2) {
            return o1.mediaFile.compareTo(o2.mediaFile);
        }
    };

    public enum Type {

        IMAGE (1),
        GIF   (2),
        VIDEO (0);

        Type(int navI){
            this.nativeId = navI;
        }

        final int nativeId;
    }

}
