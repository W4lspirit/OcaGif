package luigi.wal.walgif;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Rect;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class GIFWallpaperService extends WallpaperService {


    @Override
    public WallpaperService.Engine onCreateEngine() {
        try {
            String[] list = getResources().getAssets().list("gif");
            Log.d("GIF", "pickProfileImage: ");
            Random random = new Random();
            int rand = random.nextInt(list.length);
            InputStream open = getResources().getAssets().open("gif/" + list[rand]);
            Movie movie = Movie.decodeStream(open);

            List<Movie> movies = new ArrayList<>();
            movies.add(movie);
            return new GIFWallpaperEngine(movies);
        } catch (IOException e) {
            Log.d("GIF", "Could not load asset");
            return null;
        }


    }

    private class GIFWallpaperEngine extends WallpaperService.Engine {
        private static final int CAPACITY = 8;//WILL BECOME A CUSTOM PROPERTY
        private final int frameDuration = 100;//ARBITRARY NUMBER
        private final LinkedBlockingDeque<Movie> movies;
        private final Emitter.Listener listener = new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                try {
                    String gifUrl = (String) obj.get("gif");
                    Movie movieFromURL = getMovieFromURL(gifUrl);//TODO move this into a different thread
                    if (movieFromURL != null) {
                        movies.poll();
                        movies.offer(movieFromURL);

                        Log.d("GIF", "call() called with: args = [" + Arrays.toString(args) + "]" + new Date());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        };
        private SurfaceHolder holder;

        private boolean visible;
        private Handler handler;
        /**
         * FIELDS BELLOW
         * THIS WILL BECOME FUNCTIONS
         * OR WILL BE DYNAMICALLY COMPUTED
         */
        private int quarterYMax;
        private int halfXMax;
        private int yMax;
        private int xMax;
        private int cycle;
        private int seconds;
        private Runnable drawGIF = this::draw;

        private GIFWallpaperEngine(List<Movie> pMovies) {
            this.movies = new LinkedBlockingDeque<>(CAPACITY);
            movies.addAll(pMovies);
            handler = new Handler();
            try {
                Socket socket = IO.socket("http://archillect.com/");
                socket.on("gif", listener);
                socket.connect();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }


        private void draw() {

            if (visible) {

                debug();

                Canvas canvas = holder.lockCanvas();
                canvas.save();
                xMax = canvas.getWidth();
                yMax = canvas.getHeight();
                halfXMax = xMax / 2;
                quarterYMax = yMax / 4;
                //bad code... sorry

                //first picture
                int firstX = 0;
                int firstY = 0;
                //second picture
                int secondX = 0;
                int secondY = quarterYMax;
                //third picture
                int thirdX = 0;
                int thirdY = 2 * quarterYMax;
                //fourth picture
                int fourthX = 0;
                int fourthY = 3 * quarterYMax;
                //fifth picture
                int fifthX = halfXMax;
                int fifthY = 0;
                //sixth picture
                int sixthX = halfXMax;
                int sixthY = quarterYMax;
                //seventh picture
                int seventhX = halfXMax;
                int seventhY = 2 * quarterYMax;
                //eighth picture
                int eighthX = halfXMax;
                int eighthY = 3 * quarterYMax;


                Movie movie = movies.peek();
                if (movie != null) {
                    dostuff(canvas, movie, firstX, firstY);
                    //    dostuff(canvas, movie, secondX, secondY);
                    dostuff(canvas, movie, thirdX, thirdY);
                  /*  dostuff(canvas, movie, fourthX, fourthY);
                    dostuff(canvas, movie, fifthX, fifthY);
                    dostuff(canvas, movie, sixthX, sixthY);
                    dostuff(canvas, movie, seventhX, seventhY);
                    dostuff(canvas, movie, eighthX, eighthY);*/
                    if (movie.duration() > 0) {
                        movie.setTime((int) (System.currentTimeMillis() % movie.duration()));
                    }
                } else {
                    Log.i("GIF", "draw: ko");
                    //insert base gif
                }
                canvas.restore();

                holder.unlockCanvasAndPost(canvas);

                handler.removeCallbacks(drawGIF);
                handler.postDelayed(drawGIF, frameDuration);


            }
        }

        private void debug() {
            int prevSecond = new Date().getSeconds();
            if (seconds != prevSecond) {
                Log.i("GIF", "draw:  cycle = " + cycle + " seconds = " + this.seconds);
                this.seconds = prevSecond;
                cycle = 0;
            }
            cycle += 1;
        }

        private void dostuff(Canvas canvas, Movie movie, int left, int top) {
            //TODO width and height must be > 0
            Bitmap movieBitmap = Bitmap.createBitmap(movie.width(), movie.height(),
                    Bitmap.Config.ARGB_8888);
            Canvas movieCanvas = new Canvas(movieBitmap);
            movie.draw(movieCanvas, 0, 0);
            Rect src = new Rect(0, 0, movie.width(), movie.height());
            Rect dst = new Rect(left, top, left + 2 * halfXMax, top + 2 * quarterYMax);
            canvas.drawBitmap(movieBitmap, src, dst, null);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                handler.post(drawGIF);
            } else {
                handler.removeCallbacks(drawGIF);
            }
        }


        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.holder = surfaceHolder;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(drawGIF);
        }


        private Movie getMovieFromURL(String src) {
            try {
                java.net.URL url = new java.net.URL(src);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return Movie.decodeStream(input);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }


    }
}
