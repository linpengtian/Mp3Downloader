package com.example.mp3downloader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import androidx.documentfile.provider.DocumentFile;

import com.example.mp3downloader.databinding.ActivityMainBinding;

import android.widget.EditText;
import android.widget.Toast;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    File youtubeDLDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private boolean downloading = false;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String processId = "MyDlProcess";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            YoutubeDL.getInstance().init(this);
            FFmpeg.getInstance().init(this);
        } catch (YoutubeDLException e) {
            Log.e("TAG", "failed to initialize youtubedl-android", e);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                download();
            }
        });

        binding.textDestinationFolder.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFolder();
            }
        });

    }

    private void selectFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        activityResultLauncher.launch(intent);
    }

    private final Function3<Float, Long, String, Unit> callback = new Function3<Float, Long, String, Unit>() {
        @Override
        public Unit invoke(Float progress, Long o2, String line) {
            runOnUiThread(() -> {
                Log.i("======", String.valueOf(progress) + ", " + String.valueOf(o2) + ", " + line);
                        binding.progressBar.setProgress((int) progress.floatValue());
                        binding.txtProgress.setText(String.valueOf(progress) + "%");
                    }
            );
            return Unit.INSTANCE;
        }
    };

    private void download() {
        YoutubeDLRequest request = new YoutubeDLRequest("https://www.youtube.com/watch?v=Ke90Tje7VS0");
        request.addOption("-x");
        request.addOption("--audio-format", "mp3");
//        request.addOption("--write-thumbnail");
        request.addOption("-o", youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");


        showStart();

        downloading = true;
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, processId, callback))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
                    binding.progressBar.setProgress(100);
                    binding.txtStatus.setText(getString(R.string.success));
//                    tvCommandOutput.setText(youtubeDLResponse.getOut());
                    Toast.makeText(MainActivity.this, "download successful", Toast.LENGTH_LONG).show();
                    downloading = false;
                }, e -> {
                    if (BuildConfig.DEBUG) Log.e("=============", "failed to download", e);
                    binding.txtStatus.setText(getString(R.string.failed));
//                    tvCommandOutput.setText(e.getMessage());
                    Toast.makeText(MainActivity.this, "download failed", Toast.LENGTH_LONG).show();
                    downloading = false;
                });
        compositeDisposable.add(disposable);
    }

    private void showStart() {
        binding.selectView.setVisibility(View.GONE);
        binding.downloadView.setVisibility(View.VISIBLE);
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                Uri uri = intent.getData();
                Log.i("Directory Tree: ", String.valueOf(uri));
                Log.i("Directory Path: ", uri.getPath());
                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));

                String path = ASFUriHelper.getPath(getApplicationContext(), docUri);
                Log.i("Real path: ", path);


                youtubeDLDir = new File(path);
                String absolutePath = youtubeDLDir.getAbsolutePath();
                Log.i("full Path: ", absolutePath);

                DocumentFile df = DocumentFile.fromTreeUri(getApplicationContext(), uri);

                EditText folderNameEditText = binding.textDestinationFolder.getEditText();
                if (folderNameEditText != null) {
                    folderNameEditText.setText(df.getName());
                }
            }
        }
    });

}