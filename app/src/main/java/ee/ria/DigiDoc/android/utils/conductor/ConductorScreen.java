package ee.ria.DigiDoc.android.utils.conductor;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.utils.mvi.MviViewModelProvider;
import ee.ria.DigiDoc.android.utils.navigation.Screen;

import static android.support.v4.content.res.ResourcesCompat.getColor;

public abstract class ConductorScreen extends Controller implements Screen {

    @IdRes private final int id;

    private MviViewModelProvider viewModelProvider;

    protected ConductorScreen(@IdRes int id) {
        this(id, null);
    }

    protected ConductorScreen(int id, @Nullable Bundle args) {
        super(args);
        this.id = id;
    }

    protected abstract View createView(Context context);

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = createView(container.getContext());
        view.setId(id);
        view.setBackgroundColor(getColor(container.getResources(), R.color.windowBackground, null));
        return view;
    }

    @Override
    protected void onContextAvailable(@NonNull Context context) {
        super.onContextAvailable(context);
        if (viewModelProvider == null) {
            viewModelProvider = Application.component(context).viewModelProvider();
        }
    }

    MviViewModelProvider getViewModelProvider() {
        return viewModelProvider;
    }
}
