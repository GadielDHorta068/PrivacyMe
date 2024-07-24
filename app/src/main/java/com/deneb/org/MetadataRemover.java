package com.deneb.org;

import static java.io.File.createTempFile;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import androidx.exifinterface.media.ExifInterface;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MetaBox;
import com.googlecode.mp4parser.FileDataSourceImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class MetadataRemover {

    private final ContentResolver contentResolver;
    private final Context context;

    public MetadataRemover(ContentResolver contentResolver, Context context) {
        this.contentResolver = contentResolver;
        this.context = context;
    }

    public void removeMetadata(Uri mediaUri) {
        String mimeType = contentResolver.getType(mediaUri);
        if (mimeType != null) {
            try {
                if (mimeType.startsWith("image/")) {
                    removeImageMetadata(mediaUri);
                } else if (mimeType.startsWith("video/")) {
                    //removeVideoMetadata(mediaUri);
                } else {
                    Toast.makeText(context, "Unsupported media type: " + mimeType, Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(context, "Failed to process media metadata", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(context, "Failed to get MIME type", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeImageMetadata(Uri imageUri) throws IOException {
        Toast.makeText(context, "Processing image metadata", Toast.LENGTH_SHORT).show();

        // Crear un archivo temporal para almacenar la imagen modificada
        File tempFile = createTempFile();
        try (InputStream inputStream = contentResolver.openInputStream(imageUri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {
            if (inputStream == null) {
                Toast.makeText(context, "Failed to open image input stream", Toast.LENGTH_SHORT).show();
                return;
            }

            // Copiar el archivo original al archivo temporal
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            Toast.makeText(context, "Failed to copy image to temporary file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            throw e;
        }

        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(tempFile.getAbsolutePath());
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null);
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null);
            exifInterface.setAttribute(ExifInterface.TAG_DATETIME, null);
            exifInterface.setAttribute(ExifInterface.TAG_MAKE, null);
            exifInterface.setAttribute(ExifInterface.TAG_MODEL, null);
            exifInterface.saveAttributes();
            Toast.makeText(context, "Metadata removed from image", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, "Failed to process image metadata", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            throw e;
        }

        // Guardar el archivo modificado en una nueva ubicación
        File newFile = saveFileToDCIM(tempFile, "image");
        if (newFile != null) {
            Toast.makeText(context, "Metadata removed from image. New file saved at: " + newFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Failed to save modified image", Toast.LENGTH_SHORT).show();
        }

        // Eliminar el archivo temporal
        tempFile.delete();
    }

    private String getPathFromUri(Uri uri) {
        // Implementa la lógica para obtener la ruta del archivo desde el URI
        return uri.getPath();
    }

    private File createTempFile() throws IOException {
        File tempDir = context.getCacheDir(); // Usar el directorio de caché de la aplicación
        return File.createTempFile("temp_image", ".jpg", tempDir);
    }


    private File saveFileToDCIM(File sourceFile, String mediaType) {
        File dcimDirectory = new File(context.getExternalFilesDir(null), "DCIM");
        File appDirectory = new File(dcimDirectory, "PrivacyMe");
        if (!appDirectory.exists()) {
            appDirectory.mkdirs();
        }

        String fileName = "processed_" + System.currentTimeMillis();
        if (mediaType.equals("image")) {
            fileName += ".jpg";
        } else if (mediaType.equals("video")) {
            fileName += ".mp4";
        }

        File newFile = new File(appDirectory, fileName);
        try (InputStream inputStream = new FileInputStream(sourceFile);
             OutputStream outputStream = new FileOutputStream(newFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return newFile;
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // Ignorar excepciones durante el cierre
                e.printStackTrace();
            }
        }
    }
}