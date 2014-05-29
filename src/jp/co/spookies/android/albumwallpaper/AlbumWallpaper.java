package jp.co.spookies.android.albumwallpaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public class AlbumWallpaper extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new WallpaperEngine();
    }

    class WallpaperEngine extends Engine {
        private final Handler handler = new Handler();
        private final File imageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        private final int INTERVAL = 3000;
        private List<File> images = new ArrayList<File>();
        private int imageIndex = 0;
        private int height = 0;
        private int width = 0;

        private final Runnable drawPicture = new Runnable() {
            public void run() {
                draw();
            }
        };

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                images = getFiles(imageDirectory);
                draw();
            } else {
                handler.removeCallbacks(drawPicture);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            this.width = width;
            this.height = height;
            draw();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            handler.removeCallbacks(drawPicture);
        }

        public void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                if (!images.isEmpty()) {
                    drawImage(canvas);
                }
                holder.unlockCanvasAndPost(canvas);
            }
            handler.removeCallbacks(drawPicture);
            handler.postDelayed(drawPicture, INTERVAL);
        }

        public void drawImage(Canvas canvas) {
            try {
                byte[] bytes = getNextImage();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                options.inSampleSize = getScale(options.outWidth, options.outHeight);
                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                int blankWidth = this.width - options.outWidth; // 横余白の長さ
                int blankHeight = this.height - options.outHeight; // 縦余白の長さ
                canvas.drawColor(Color.BLACK);
                // 縮小した画像を画面中心に描画
                canvas.drawBitmap(bitmap, blankWidth / 2, blankHeight / 2, new Paint());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private int getScale(int imageWidth, int imageHeight) {
            if (imageWidth > imageHeight) {
                return (int) Math.ceil((float) imageWidth / this.width);
            } else {
                return (int) Math.ceil((float) imageHeight / this.height);
            }
        }

        private byte[] getNextImage() throws IOException {
            if (imageIndex >= images.size()) {
                // 最後の画像に達したので最初の画像に戻す
                imageIndex = 0;
            }
            InputStream stream = new FileInputStream(images.get(imageIndex++));
            int length = stream.available();
            byte[] bytes = new byte[length];
            stream.read(bytes, 0, length);
            stream.close();
            return bytes;
        }

        private List<File> getFiles(File directory) {
            if (!directory.exists()) {
                return new ArrayList<File>();
            }
            List<File> files = Arrays.asList(directory.listFiles());
            List<File> result = new ArrayList<File>();
            for (File file : files) {
                if (file.isDirectory()) {
                    // 対象がディレクトリだったら再帰的にファイルを追加
                    result.addAll(getFiles(file));
                } else if (file.getPath().endsWith(".jpg")) {
                    result.add(file);
                }
            }
            return result;
        }
    }
}
