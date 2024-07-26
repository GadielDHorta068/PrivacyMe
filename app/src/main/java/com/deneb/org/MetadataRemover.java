package com.deneb.org;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.content.FileProvider;
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
import java.nio.file.Files;

/**
 * Clase para remover metadatos de archivos multimedia.
 */
public class MetadataRemover {

    private final ContentResolver contentResolver;
    private final Context context;
    private final MainActivity mainActivity;

    /**
     * Constructor
     *
     * @param contentResolver
     * @param context
     * @param mainActivity
     */
    public MetadataRemover(ContentResolver contentResolver, Context context, MainActivity mainActivity) {
        this.contentResolver = contentResolver;
        this.context = context;
        this.mainActivity = mainActivity;
    }

    /**
     * Esta clase recibe una URI de un archivo multimedia y ejecuta el metodo corrrespondiente
     * dependiendo de su tipo.
     * @param mediaUri  Direccion URI del archivo a trabajar
     */
    public void removeMetadata(Uri mediaUri) {
        String mimeType = contentResolver.getType(mediaUri);
        if (mimeType != null) {
            try {
                if (mimeType.startsWith("image/")) {
                    removeImageMetadata(mediaUri);
                } else if (mimeType.startsWith("video/")) {
                    removeVideoMetadata(mediaUri);
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

    /**
     * Este metodo remueve los metadatos de una imagen.
     * @param imageUri Direccion URI de la imagen a trabajar
     * @throws IOException  Excepcion en caso de que ocurra un error
     */
    private void removeImageMetadata(Uri imageUri) throws IOException {

        // Crear un archivo temporal para almacenar la imagen modificada
        File tempFile = createTempFile();
        try (InputStream inputStream = contentResolver.openInputStream(imageUri);
             OutputStream outputStream = Files.newOutputStream(tempFile.toPath())) {
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

        ExifInterface exifInterface;
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
        if (newFile == null) {
            Toast.makeText(context, "Failed to save modified image", Toast.LENGTH_SHORT).show();
        }
        tempFile.delete();
    }

    /**
     * De una URI devuelve la direccion del archivo(?
     * @param uri
     * @return
     */
    private String getPathFromUri(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        try (Cursor cursor = contentResolver.query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Crea un archivo temporal para almacenar la imagen modificada.
     * @return Archivo temporal y su direccion
     * @throws IOException
     */
    private File createTempFile() throws IOException {
        File tempDir = context.getCacheDir(); // Usar el directorio de caché de la aplicación
        return File.createTempFile("temp_image", ".jpg", tempDir);
    }

    /**
     * Guarda un archivo en el directorio DCIM de la aplicación. (Arreglar esto, lo guarda en matusalem)
     * @param sourceFile Archivo a guardar
     * @param mediaType Tipo de archivo (imagen o video)
     * @return
     */
    private File saveFileToDCIM(File sourceFile, String mediaType) {
        File dcimDirectory = new File(context.getExternalFilesDir(null), "DCIM");
        File appDirectory = new File(dcimDirectory, "PrivacyMe");
        if (!appDirectory.exists() && !appDirectory.mkdirs()) {
            Toast.makeText(context, "Failed to create directory", Toast.LENGTH_SHORT).show();
            return null;
        }

        String fileName = "processed_" + System.currentTimeMillis();
        if (mediaType.equals("image")) {
            fileName += ".jpg";
        } else if (mediaType.equals("video")) {
            fileName += ".mp4";
        }

        File newFile = new File(appDirectory, fileName);
        try (InputStream inputStream = Files.newInputStream(sourceFile.toPath());
             OutputStream outputStream = Files.newOutputStream(newFile.toPath())) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // Verificar el tamaño del archivo
            if (newFile.length() == 0) {
                Toast.makeText(context, "File is empty", Toast.LENGTH_SHORT).show();
                return null;
            }

            // Compartir el archivo
            shareImage(newFile);
            mainActivity.showInterstitialAd();


            return newFile;
        } catch (IOException e) {
            Toast.makeText(context, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Compartir imagen
     * @param imageFile imagen a ser compartida
     */
    private void shareImage(File imageFile) {
        Uri imageUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", imageFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(context, Intent.createChooser(shareIntent, "Share image using"), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeApp();
    }

    /**
     * Remover los metadatos de un video
     * @param videoUri video a ser procesado
     */
    private void removeVideoMetadata(Uri videoUri) throws IOException {
        FileDataSourceImpl dataSource = null;
        IsoFile isoFile = null;

        try {
            String filePath = getPathFromUri(videoUri);
            if (filePath == null) {
                Toast.makeText(context, "Failed to get file path from URI", Toast.LENGTH_SHORT).show();
                return;
            }

            File videoFile = new File(filePath);
            if (!videoFile.exists()) {
                Toast.makeText(context, "Video file does not exist: " + filePath, Toast.LENGTH_SHORT).show();
                return;
            }

            dataSource = new FileDataSourceImpl(videoFile);
            isoFile = new IsoFile(dataSource);

            if (isoFile.getBoxes() != null) {
                isoFile.getBoxes().removeIf(box -> box instanceof MetaBox);
            } else {
                Toast.makeText(context, "No metadata found in video", Toast.LENGTH_SHORT).show();
                return;
            }

            File processedFile = saveFileToSameDirectory(videoFile, "video");
            if (processedFile != null) {
                try (FileChannel outputChannel = new FileOutputStream(processedFile).getChannel()) {
                    isoFile.getBox(outputChannel);
                    shareMedia(processedFile, "video");
                } catch (IOException e) {
                    Toast.makeText(context, "Failed to save modified video", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    throw e;
                }
            }
        } catch (IOException e) {
            Toast.makeText(context, "Failed to process video metadata", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            throw e;
        } finally {
            if (dataSource != null) {
                try {
                    dataSource.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (isoFile != null) {
                try {
                    isoFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Guarda un archivo en el mismo directorio que el original
     * @param sourceFile Archivo a guardar
     * @param mediaType Tipo de archivo (imagen o video)
     * @return
     */
    private File saveFileToSameDirectory(File sourceFile, String mediaType) {
        File sourceDirectory = sourceFile.getParentFile();
        if (sourceDirectory == null) {
            Toast.makeText(context, "Failed to get source directory", Toast.LENGTH_SHORT).show();
            return null;
        }

        String fileName = "processed_" + System.currentTimeMillis();
        if (mediaType.equals("image")) {
            fileName += ".jpg";
        } else if (mediaType.equals("video")) {
            fileName += ".mp4";
        }

        File newFile = new File(sourceDirectory, fileName);
        try (InputStream inputStream = new FileInputStream(sourceFile);
             OutputStream outputStream = new FileOutputStream(newFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            if (newFile.length() == 0) {
                Toast.makeText(context, "File is empty", Toast.LENGTH_SHORT).show();
                return null;
            }

            shareImage(newFile);
            return newFile;
        } catch (IOException e) {
            Toast.makeText(context, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return null;
        }
    }

    private void closeApp() {
        System.exit(0);
    }

    /**
     * Otro metodo para compartir el archivo, usando en video
     * @param mediaFile archivo
     * @param mediaType tipo de archivo
     */
    private void shareMedia(File mediaFile, String mediaType) {
        Uri mediaUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", mediaFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mediaType.equals("image") ? "image/*" : "video/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, mediaUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(context, Intent.createChooser(shareIntent, "Share media using"), null);
        } catch (Exception e) {
            Toast.makeText(context, "No app found to share media", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}