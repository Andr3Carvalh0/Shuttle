package com.simplecity.amp_library.ui.widgets;

import android.support.v7.app.AppCompatActivity;
import com.simplecity.amp_library.di.app.activity.ActivityModule;
import com.simplecity.amp_library.di.app.activity.ActivityScope;
import com.simplecity.amp_library.di.app.activity.fragment.FragmentScope;
import com.simplecity.amp_library.ui.screens.widgets.WidgetFragment;
import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module(includes = ActivityModule.class)
public abstract class WidgetConfigureActivityExtraLargeModule {

    @Binds
    @ActivityScope
    abstract AppCompatActivity appCompatActivity(WidgetConfigureActivityExtraLarge activity);

    @FragmentScope
    @ContributesAndroidInjector(modules = WidgetFragmentModule.class)
    abstract WidgetFragment widgetFragmentInjector();
}