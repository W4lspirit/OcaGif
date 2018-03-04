package luigi.wal.walgif

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie
import android.graphics.Rect
import android.os.Handler
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import luigi.wal.walgif.utils.RectangleCalculator
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.*


class GIFWallpaperService : WallpaperService() {
    private var gifAssets: Array<String>? = null

    private val mRandom = Random()

    override fun onCreateEngine(): WallpaperService.Engine? {
        return try {
            gifAssets = resources.assets.list("gif")
            val movies = pickRandomDefaultMovies(4)
            GIFWallpaperEngine(movies)
        } catch (e: IOException) {
            Log.d(GIF_TAG, "Could not load asset")
            null
        }
    }

    @Throws(IOException::class)
    private fun pickRandomDefaultMovies(numberOfMovie: Int): List<Movie> {
        val movies = ArrayList<Movie>()
        for (i in 0 until numberOfMovie) {
            Log.d(GIF_TAG, "pickProfileImage: ")
            movies.add(pickRandomMovie())
        }
        return movies
    }

    @Throws(IOException::class)
    private fun pickRandomMovie(): Movie {
        val rand = mRandom.nextInt(gifAssets?.size!!)
        val open = resources.assets.open("gif/" + gifAssets!![rand])
        return Movie.decodeStream(open)
    }

    private inner class GIFWallpaperEngine internal constructor(pMovies: List<Movie>) : WallpaperService.Engine() {
        private val frameDuration = 100//Practical NUMBER
        private var movies: LinkedBlockingDeque<Movie> = LinkedBlockingDeque(CAPACITY)
        private val listener = Emitter.Listener { args ->
            val obj = args[0] as JSONObject
            try {
                val gifUrl = obj.get("gif") as String
                val movieFromURL = getMovieFromURL(gifUrl)
                if (movieFromURL != null) {
                    movies.poll()
                    movies.offer(movieFromURL)

                    Log.d(GIF_TAG, "call() called with: args = [" + Arrays.toString(args) + "]" + Date())
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        private var holder: SurfaceHolder? = null
        private var visible: Boolean = false
        private val handler: Handler
        /**
         * FIELDS BELLOW
         * THIS WILL BECOME FUNCTIONS
         * OR WILL BE DYNAMICALLY COMPUTED
         */


        private var cycle: Int = 0
        private var seconds: Int = 0
        private var rectWidth: Int = 0
        private var rectHeigth: Int = 0
        private var mRectanglePoints: List<Pair<Int, Int>>? = null
        private var settingsHasChanged: Boolean = false
        private val drawGIF = Runnable { this.draw() }
        private var mSocket: Socket? = null
        private val mScheduler: ScheduledExecutorService? = Executors.newSingleThreadScheduledExecutor()
        private var mScheduledFutureStartOn: ScheduledFuture<*>? = null
        private var mScheduledFutureStartOff: ScheduledFuture<*>? = null

        init {
            settingsHasChanged = true
            movies.addAll(pMovies)
            handler = Handler()
            try {
                mSocket = IO.socket("http://archillect.com/")
                startListeningArchillect()
            } catch (e: URISyntaxException) {
                Log.e(GIF_TAG, "GIFWallpaperEngine: ", e)
            }

        }

        private fun startListeningArchillect() {

            val startOn = Runnable {
                mSocket?.on(SOCKET_EVENT_NAME, listener)
                Log.i(GIF_TAG, "  mSocket.on(\"gif\", listener);: on")
            }

            mScheduledFutureStartOn = mScheduler?.scheduleWithFixedDelay(startOn, 0, 15, TimeUnit.SECONDS)
            val startOff = Runnable {
                mSocket?.off(SOCKET_EVENT_NAME)
                Log.i(GIF_TAG, "mSocket.off(\"gif\");")
            }
            mScheduledFutureStartOff = mScheduler?.scheduleWithFixedDelay(startOff, 5, 15, TimeUnit.SECONDS)
            mSocket?.connect()
        }


        private fun draw() {

            if (visible) {
                // debug();
                val canvas = holder?.lockCanvas()
                if (canvas != null) {
                    canvas.save()


                    checkSettingsHasChanged(canvas)
                    val moviesClone = LinkedList(movies)
                    mRectanglePoints?.forEach { (first, second) -> newMovie(moviesClone, canvas, first, second) }
                    moviesClone.clear()

                    canvas.restore()

                    holder?.unlockCanvasAndPost(canvas)
                }
                handler.removeCallbacks(drawGIF)
                handler.postDelayed(drawGIF, frameDuration.toLong())


            }
        }

        private fun checkSettingsHasChanged(pCanvas: Canvas) {
            if (settingsHasChanged) {
                val xMax = pCanvas.width
                val yMax = pCanvas.height
                mRectanglePoints = RectangleCalculator.getRectanglePoints(xMax, yMax, DIMENSION)
                val (first, second) = RectangleCalculator.getRectDimension(xMax, yMax, DIMENSION)
                rectWidth = first
                rectHeigth = second
                settingsHasChanged = false
            }
        }

        private fun newMovie(pMoviesClone: LinkedList<Movie>, pCanvas: Canvas, x: Int, y: Int) {
            try {
                var movie: Movie? = pMoviesClone.poll()
                if (movie == null) {
                    Log.i(GIF_TAG, "draw: no movie available")
                    movie = pickRandomMovie()
                }
                drawMovie(pCanvas, x, y, movie)
            } catch (pE: IOException) {
                Log.e(GIF_TAG, "newMovie: ", pE)
            }

        }


        private fun drawMovie(pCanvas: Canvas, pPosX: Int, pPosY: Int, pMovie: Movie) {
            drawBitmapMovie(pCanvas, pMovie, pPosX, pPosY)

            if (pMovie.duration() > 0) {
                pMovie.setTime((System.currentTimeMillis() % pMovie.duration()).toInt())
            }
        }

        private fun drawBitmapMovie(canvas: Canvas, movie: Movie, left: Int, top: Int) {

            val movieBitmap = Bitmap.createBitmap(movie.width(), movie.height(),
                    Bitmap.Config.ARGB_8888)
            val movieCanvas = Canvas(movieBitmap)
            movie.draw(movieCanvas, 0f, 0f)
            val src = Rect(0, 0, movie.width(), movie.height())
            val dst = Rect(left, top, left + rectWidth, top + rectHeigth)
            canvas.drawBitmap(movieBitmap, src, dst, null)
        }

        private fun debug() {
            val prevSecond = Date().seconds
            if (seconds != prevSecond) {
                Log.i(GIF_TAG, "draw:  cycle = " + cycle + " seconds = " + this.seconds)
                this.seconds = prevSecond
                cycle = 0
            }
            cycle += 1
        }

        private fun getMovieFromURL(src: String): Movie? {
            return try {
                val url = java.net.URL(src)
                val connection = url
                        .openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                Movie.decodeStream(input)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }

        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            this.holder = surfaceHolder
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawGIF)
            stopScheduledTask()
            stopIOSocket()
        }

        private fun stopScheduledTask() {
            if (mScheduledFutureStartOff != null) mScheduledFutureStartOff?.cancel(true)
            if (mScheduledFutureStartOn != null) mScheduledFutureStartOn?.cancel(true)
            mScheduler?.shutdown()
        }

        private fun stopIOSocket() {
            mSocket?.disconnect()
            mSocket?.off(SOCKET_EVENT_NAME)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                handler.post(drawGIF)
            } else {
                handler.removeCallbacks(drawGIF)
            }
        }

    }

    companion object {
        val SOCKET_EVENT_NAME = "gif"
        val CAPACITY = 8//WILL BECOME A CUSTOM PROPERTY
        val GIF_TAG = "SOCKET_EVENT_NAME"
        val DIMENSION = Pair(1, 3)
    }
}




