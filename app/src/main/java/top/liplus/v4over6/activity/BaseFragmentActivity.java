package top.liplus.v4over6.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class BaseFragmentActivity extends FragmentActivity {

    public void startFragment(@NonNull Fragment fragment, int fragmentContainerId) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        if (getCurrentFragment() != null) {
            transaction.hide(getCurrentFragment());
        }
        transaction.addToBackStack(fragment.getClass().getName())
                .add(fragmentContainerId, fragment)
                .commit();
    }

    @Nullable
    public Fragment getCurrentFragment() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() < 1) {
            return null;
        }
        int index = manager.getBackStackEntryCount() - 1;
        String tag = manager.getBackStackEntryAt(index).getName();
        return manager.findFragmentByTag(tag);
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
