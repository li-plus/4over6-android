package top.liplus.v4over6.activity;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import top.liplus.v4over6.R;
import top.liplus.v4over6.fragment.ConfigListFragment;

public class MainActivity extends BaseFragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // replace fragment
        Fragment fragment = new ConfigListFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(fragment.getClass().getName())
                .replace(R.id.fl_fragment_container, fragment, fragment.getClass().getName())
                .commit();
    }
}
