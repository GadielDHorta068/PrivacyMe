package com.deneb.org;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private MetadataRemover metadataRemover;
    private InterstitialAd mInterstitialAd;

    // ActivityResultLauncher for selecting media
    private final ActivityResultLauncher<Intent> selectMediaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri selectedMediaUri = data.getData();
                        if (selectedMediaUri != null) {
                            metadataRemover.removeMetadata(selectedMediaUri);
                        } else {
                            Toast.makeText(this, "Failed to get the selected file", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    /**
     * onCreate method
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // Find AdView as defined in XML
        AdView mAdView = findViewById(R.id.adView);

        // Create an ad request
        AdRequest adRequest = new AdRequest.Builder().build();

        mAdView.setAdListener(new AdListener() {

            public void onAdFailedToLoad(@NonNull AdError adError) {
                // El anuncio falló al cargarse
                Toast.makeText(MainActivity.this, "Ad Failed to Load: " + adError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Carga el anuncio
        mAdView.loadAd(adRequest);

        loadInterstitialAd();
        // Verifica los permisos
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            initializeApp();
        } else {
            initializeApp();
        }
        setupButtons();
    }

    /**
     * Carga el anuncio de interstitial en la actividad
     */
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-2575226536979588/4796897340", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Ad dismissed callback
                        shareImage();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        // Ad failed to show callback
                        Toast.makeText(MainActivity.this, "Ad Failed to Show: " + adError.getMessage(), Toast.LENGTH_SHORT).show();
                        shareImage();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Ad showed fullscreen content callback
                        mInterstitialAd = null; // Set the ad to null so we can load a new ad
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null;
                Toast.makeText(MainActivity.this, "Interstitial Ad Failed to Load: " + loadAdError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Muestra el anuncio de interstitial
     */
    void showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
            loadInterstitialAd();
        } else {
            Toast.makeText(this, "Interstitial ad is not ready yet", Toast.LENGTH_SHORT).show();
            shareImage();
        }
    }

    /**
     * Compartir imagen
     * Esto iba a ser usado pero luego la logica fue cambiada.
     * Eliminar proximamente
     */
    private void shareImage() {
        //lógica de compartir imagen aquí.
    }

    /**
     * Boolean que verifica tener todos los permisos necesarios
     * @return Boolean
     */
    private boolean hasPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * This method is called when the user responds to the permission request.
     * @param requestCode The request code passed in ""
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeApp();
            } else {
                Toast.makeText(this, "Permissions are required to use this app", Toast.LENGTH_SHORT).show();
                initializeApp();
            }
        }
    }

    /**
     * Inicializa la aplicacion
     */
    private void initializeApp() {
        metadataRemover = new MetadataRemover(getContentResolver(), this,this);
    }

    /**
     * Abre el selector de archivos para seleccionar una imagen o video
     */
    private void launchMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        selectMediaLauncher.launch(intent);
    }

    /**
     * Configura los botones de la actividad
     */
    private void setupButtons() {
        Button selectMediaButton = findViewById(R.id.button_select_media);
        Button exitButton = findViewById(R.id.button_exit);

        selectMediaButton.setOnClickListener(v -> {
            launchMediaPicker();
            showInterstitialAd();
        });

        exitButton.setOnClickListener(v -> finish());
    }
}