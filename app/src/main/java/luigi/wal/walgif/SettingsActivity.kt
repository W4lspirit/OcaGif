package luigi.wal.walgif

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.view.MenuItem

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || DarkGeneralPreferenceFragment::class.java.name == fragmentName

    }


    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class DarkGeneralPreferenceFragment : PreferenceFragment() {
        lateinit var mSeek1: SeekBarPreference
        private lateinit var SEEK_1: String
        val DEFAULT_1 = 10

        /*override fun onResume() {
            super.onResume()
            val prefs = preferenceManager.sharedPreferences
            onSharedPreferenceChanged(prefs, getString(R.string.seek_1))
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            val prefs = preferenceManager.sharedPreferences
            prefs.unregisterOnSharedPreferenceChangeListener(this)
        }
*/
/*
        override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
            if (SEEK_1.equals(key)) {
                val i = prefs?.getInt(key, DEFAULT_1)
                mSeek1.summary = "$i minutes"
            }
        }*/

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general_dark)
            setHasOptionsMenu(true)
            SEEK_1 = getString(R.string.seek_1)
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            mSeek1 = findPreference(SEEK_1) as SeekBarPreference

        }


        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }

    }


}
