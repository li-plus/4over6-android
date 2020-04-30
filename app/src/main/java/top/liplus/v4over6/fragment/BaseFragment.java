package top.liplus.v4over6.fragment;

import androidx.fragment.app.Fragment;

import top.liplus.v4over6.R;
import top.liplus.v4over6.activity.BaseFragmentActivity;

public class BaseFragment extends Fragment {
    public BaseFragmentActivity getBaseFragmentActivity() {
        return (BaseFragmentActivity) getActivity();
    }

    public TransitionConfig getTransitionConfig() {
        return SLIDE_TRANSITION_CONFIG;
    }

    public static class TransitionConfig {
        public int enter;
        public int exit;
        public int popEnter;
        public int popExit;

        public TransitionConfig(int enter, int exit, int popEnter, int popExit) {
            this.enter = enter;
            this.exit = exit;
            this.popEnter = popEnter;
            this.popExit = popExit;
        }
    }

    protected static final TransitionConfig SLIDE_TRANSITION_CONFIG = new TransitionConfig(
            R.anim.slide_in_right, R.anim.slide_out_left,
            R.anim.slide_in_left, R.anim.slide_out_right
    );

    protected static final TransitionConfig SCALE_TRANSITION_CONFIG = new TransitionConfig(
            R.anim.scale_enter, R.anim.slide_still,
            R.anim.slide_still, R.anim.scale_exit
    );

    protected static final TransitionConfig FADE_TRANSITION_CONFIG = new TransitionConfig(
            R.anim.fade_in, R.anim.fade_out,
            R.anim.fade_in, R.anim.fade_out
    );

    protected static final TransitionConfig FADE_IN_SLIDE_OUT_TRANSITION_CONFIG = new TransitionConfig(
            R.anim.slide_in_right, R.anim.slide_out_left,
            R.anim.fade_in_exit, R.anim.fade_out
    );
}
