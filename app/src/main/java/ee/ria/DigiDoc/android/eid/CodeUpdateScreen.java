package ee.ria.DigiDoc.android.eid;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bluelinelabs.conductor.Controller;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.utils.navigator.Screen;
import timber.log.Timber;

public final class CodeUpdateScreen extends Controller implements Screen {

    private static final String KEY_ACTION = "action";

    public static CodeUpdateScreen create(CodeUpdateAction action) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_ACTION, action);
        return new CodeUpdateScreen(args);
    }

    private final CodeUpdateAction action;

    private TextView textView;
    private TextInputLayout currentLabelView;
    private EditText currentView;
    private TextInputLayout newLabelView;
    private EditText newView;
    private TextInputLayout repeatLabelView;
    private EditText repeatView;
    private Button negativeButton;
    private Button positiveButton;

    @SuppressWarnings("WeakerAccess")
    public CodeUpdateScreen(Bundle args) {
        super(args);
        action = args.getParcelable(KEY_ACTION);
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = inflater.inflate(R.layout.eid_home_code_update_screen, container, false);
        textView = view.findViewById(R.id.eidHomeCodeUpdateText);
        currentLabelView = view.findViewById(R.id.eidHomeCodeUpdateCurrentLabel);
        currentView = view.findViewById(R.id.eidHomeCodeUpdateCurrent);
        newLabelView = view.findViewById(R.id.eidHomeCodeUpdateNewLabel);
        newView = view.findViewById(R.id.eidHomeCodeUpdateNew);
        repeatLabelView = view.findViewById(R.id.eidHomeCodeUpdateRepeatLabel);
        repeatView = view.findViewById(R.id.eidHomeCodeUpdateRepeat);
        negativeButton = view.findViewById(R.id.eidHomeCodeUpdateNegativeButton);
        positiveButton = view.findViewById(R.id.eidHomeCodeUpdatePositiveButton);

        Timber.e("ACTION: %s", action);

        return view;
    }
}
