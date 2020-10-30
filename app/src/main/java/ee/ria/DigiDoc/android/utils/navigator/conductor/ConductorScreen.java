package ee.ria.DigiDoc.android.utils.navigator.conductor;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Controller;

import ee.ria.DigiDoc.android.utils.navigator.Screen;

public abstract class ConductorScreen extends Controller implements Screen {

    private final int id;

    protected ConductorScreen(@IdRes int id) {
        this(id, null);
    }

    protected ConductorScreen(@IdRes int id, @Nullable Bundle args) {
        super(args);
        this.id = id;
    }

    protected abstract View view(Context context);

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = view(container.getContext());
        view.setId(id);
        return view;
    }
}
