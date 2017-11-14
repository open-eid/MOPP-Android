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

import ee.ria.DigiDoc.android.utils.navigation.Screen;

public abstract class ConductorScreen extends Controller implements Screen {

    @IdRes private final int id;

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
        return view;
    }
}
