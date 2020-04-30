package top.liplus.v4over6.activity;

import android.os.Bundle;

import top.liplus.v4over6.R;
import top.liplus.v4over6.fragment.ConfigListFragment;

public class MainActivity extends BaseFragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startFragment(new ConfigListFragment());
    }

    @Override
    public int getContextViewId() {
        return R.id.cl_main;
    }
}
