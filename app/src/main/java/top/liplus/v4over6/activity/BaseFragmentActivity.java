package top.liplus.v4over6.activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import top.liplus.v4over6.fragment.BaseFragment;

public abstract class BaseFragmentActivity extends FragmentActivity {
    private static final String TAG = BaseFragmentActivity.class.getSimpleName();

    public abstract int getContextViewId();

    public int startFragment(@NonNull BaseFragment fragment) {
        String tagName = fragment.getClass().getName();
        BaseFragment.TransitionConfig trans = fragment.getTransitionConfig();
        return getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(trans.enter, trans.exit, trans.popEnter, trans.popExit)
                .replace(getContextViewId(), fragment, tagName)
                .addToBackStack(tagName)
                .commit();
    }

    @Override
    public void onBackPressed() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 1) {
            manager.popBackStackImmediate();
        } else {
            finish();
        }
    }
}
