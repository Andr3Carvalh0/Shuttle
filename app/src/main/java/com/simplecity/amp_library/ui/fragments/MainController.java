package com.simplecity.amp_library.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.Playlist;
import com.simplecity.amp_library.ui.detail.PlaylistDetailFragment;
import com.simplecity.amp_library.ui.drawer.DrawerEventRelay;
import com.simplecity.amp_library.ui.drawer.DrawerLockController;
import com.simplecity.amp_library.ui.drawer.DrawerLockManager;
import com.simplecity.amp_library.ui.drawer.DrawerProvider;
import com.simplecity.amp_library.ui.settings.SettingsParentFragment;
import com.simplecity.amp_library.ui.views.UpNextView;
import com.simplecity.amp_library.ui.views.multisheet.CustomMultiSheetView;
import com.simplecity.amp_library.ui.views.multisheet.MultiSheetEventRelay;
import com.simplecity.amp_library.utils.SleepTimer;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import test.com.androidnavigation.fragment.BackPressHandler;
import test.com.androidnavigation.fragment.BaseNavigationController;
import test.com.androidnavigation.fragment.FragmentInfo;
import test.com.multisheetview.ui.view.MultiSheetView;

public class MainController extends BaseNavigationController implements BackPressHandler, DrawerLockController {

    private static final String TAG = "MainController";

    public static final String STATE_CURRENT_SHEET = "current_sheet";

    @Inject DrawerEventRelay drawerEventRelay;

    @Inject MultiSheetEventRelay multiSheetEventRelay;

    private CompositeSubscription subscriptions = new CompositeSubscription();

    private Handler delayHandler;

    @BindView(R.id.multiSheetView)
    CustomMultiSheetView multiSheetView;

    public static MainController newInstance() {
        Bundle args = new Bundle();
        MainController fragment = new MainController();
        fragment.setArguments(args);
        return fragment;
    }

    public MainController() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ButterKnife.bind(this, rootView);

        ShuttleApplication.getInstance().getAppComponent().inject(this);

        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .add(multiSheetView.getSheetContainerViewResId(MultiSheetView.Sheet.FIRST), PlayerFragment.newInstance())
                    .add(multiSheetView.getSheetPeekViewResId(MultiSheetView.Sheet.FIRST), MiniPlayerFragment.newInstance())
                    .add(multiSheetView.getSheetContainerViewResId(MultiSheetView.Sheet.SECOND), QueueFragment.newInstance())
                    .commit();

        } else {
            multiSheetView.goToSheet(savedInstanceState.getInt(STATE_CURRENT_SHEET));
        }

        ((ViewGroup) multiSheetView.findViewById(multiSheetView.getSheetPeekViewResId(MultiSheetView.Sheet.SECOND))).addView(new UpNextView(getContext()));

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (delayHandler != null) {
            delayHandler.removeCallbacksAndMessages(null);
        }
        delayHandler = new Handler();

        subscriptions.add(drawerEventRelay.getEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .filter(drawerEvent -> drawerEvent.isActionable)
                .subscribe(drawerEvent -> {
                    switch (drawerEvent.type) {
                        case DrawerEventRelay.DrawerEvent.Type.LIBRARY_SELECTED:
                            popToRootViewController();
                            break;
                        case DrawerEventRelay.DrawerEvent.Type.FOLDERS_SELECTED:
                            delayHandler.postDelayed(() -> pushViewController(FolderFragment.newInstance("PageTitle"), "FolderFragment"), 250);
                            break;
                        case DrawerEventRelay.DrawerEvent.Type.SLEEP_TIMER_SELECTED:
                            Action0 showToast = () -> Toast.makeText(getContext(), R.string.sleep_timer_started, Toast.LENGTH_SHORT).show();
                            SleepTimer.getInstance().getDialog(
                                    getContext(),
                                    () -> SleepTimer.getInstance().showHmsPicker(getContext(), getFragmentManager(), showToast),
                                    showToast
                            ).show();
                            break;
                        case DrawerEventRelay.DrawerEvent.Type.EQUALIZER_SELECTED:
                            delayHandler.postDelayed(() -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(EqualizerFragment.newInstance(), "EqualizerFragment"), 250);
                            break;
                        case DrawerEventRelay.DrawerEvent.Type.SETTINGS_SELECTED:
                            delayHandler.postDelayed(() -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(SettingsParentFragment.newInstance(R.xml.settings_headers, R.string.settings), "Settings Fragment"), 250);
                            break;
                        case DrawerEventRelay.DrawerEvent.Type.SUPPORT_SELECTED:
                            delayHandler.postDelayed(() -> multiSheetEventRelay.sendEvent(new MultiSheetEventRelay.MultiSheetEvent(MultiSheetEventRelay.MultiSheetEvent.Action.HIDE, MultiSheetView.Sheet.FIRST)), 100);
                            delayHandler.postDelayed(() -> pushViewController(SettingsParentFragment.newInstance(R.xml.settings_support, R.string.pref_title_support), "Support Fragment"), 250);
                            break;
                        case DrawerEventRelay.DrawerEvent.Type.PLAYLIST_SELECTED:
                            delayHandler.postDelayed(() -> pushViewController(PlaylistDetailFragment.newInstance((Playlist) drawerEvent.data), "PlaylistDetailFragment"), 250);
                            break;
                    }
                }));

        DrawerLockManager.getInstance().setDrawerLockController(this);
    }

    @Override
    public void onPause() {
        delayHandler.removeCallbacksAndMessages(null);
        delayHandler = null;

        subscriptions.clear();

        DrawerLockManager.getInstance().setDrawerLockController(null);

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_SHEET, multiSheetView.getCurrentSheet());
        super.onSaveInstanceState(outState);
    }

    @Override
    public FragmentInfo getRootViewControllerInfo() {
        return LibraryController.fragmentInfo();
    }

    @Override
    public boolean consumeBackPress() {
        if (multiSheetView.consumeBackPress()) {
            return true;
        }

        return super.consumeBackPress();
    }

    @Override
    public void lockDrawer() {
        ((DrawerProvider) getActivity()).getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    public void unlockDrawer() {
        // Don't unlock the drawer if one of the sheets is expanded
        if (multiSheetView.getCurrentSheet() == MultiSheetView.Sheet.FIRST || multiSheetView.getCurrentSheet() == MultiSheetView.Sheet.SECOND) {
            return;
        }

        ((DrawerProvider) getActivity()).getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }
}