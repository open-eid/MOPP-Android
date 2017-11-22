package ee.ria.DigiDoc.android.main.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import ee.ria.DigiDoc.R;
import ee.ria.DigiDoc.android.Application;
import ee.ria.DigiDoc.android.main.settings.SettingsScreen;
import ee.ria.DigiDoc.android.utils.ViewDisposables;
import ee.ria.DigiDoc.android.utils.navigation.Navigator;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.jakewharton.rxbinding2.view.RxView.clicks;

public final class HomeToolbar extends Toolbar {

    private final ImageButton overflowButton;
    private final PopupWindow popupWindow;
    private final RecyclerView recyclerView;
    private final MenuAdapter adapter;

    private final Navigator navigator;

    private final ViewDisposables disposables;

    public HomeToolbar(Context context) {
        this(context, null);
    }

    public HomeToolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.toolbarStyle);
    }

    public HomeToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.main_home_toolbar, this);

        overflowButton = findViewById(R.id.mainHomeToolbarOverflow);
        TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
        overflowButton.setImageTintList(ColorStateList.valueOf(a.getColor(0, Color.BLACK)));
        a.recycle();

        Context popupContext = new ContextThemeWrapper(context, getPopupTheme());

        popupWindow = new PopupWindow(popupContext, null, R.attr.listPopupWindowStyle);
        popupWindow.setFocusable(true);
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);

        recyclerView = new RecyclerView(popupContext);
        recyclerView.setLayoutManager(new LinearLayoutManager(popupContext));
        recyclerView.setAdapter(adapter = new MenuAdapter());
        popupWindow.setContentView(recyclerView);

        navigator = Application.component(context).navigator();
        disposables = new ViewDisposables();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        disposables.attach();
        disposables.add(clicks(overflowButton).subscribe(o -> {
            popupWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
            popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
            popupWindow.showAsDropDown(overflowButton);
        }));
        disposables.add(adapter.itemClicks().subscribe(menuItem -> {
            switch (menuItem.id()) {
                case R.id.mainHomeToolbarSettings:
                    navigator.pushScreen(SettingsScreen.create());
                    break;
            }
        }));
    }

    @Override
    protected void onDetachedFromWindow() {
        popupWindow.dismiss();
        disposables.detach();
        super.onDetachedFromWindow();
    }

    static final ImmutableList<MenuItem> MENU_ITEMS = ImmutableList.<MenuItem>builder()
            .add(MenuItem.create(R.id.mainHomeToolbarDocuments,
                    R.string.main_home_toolbar_documents, R.drawable.ic_folder_open))
            .add(MenuItem.create(R.id.mainHomeToolbarFirstRun,
                    R.string.main_home_toolbar_first_run, R.drawable.ic_first_run))
            .add(MenuItem.create(R.id.mainHomeToolbarSettings,
                    R.string.main_home_toolbar_settings, R.drawable.ic_settings))
            .add(MenuItem.create(R.id.mainHomeToolbarHelp,
                    R.string.main_home_toolbar_help, R.drawable.ic_help_outline))
            .add(MenuItem.create(R.id.mainHomeToolbarAbout,
                    R.string.main_home_toolbar_about, R.drawable.ic_info_outline))
            .build();

    @AutoValue
    static abstract class MenuItem {

        @IdRes abstract int id();
        @StringRes abstract int text();
        @DrawableRes abstract int icon();

        static MenuItem create(@IdRes int id, @StringRes int text, @DrawableRes int icon) {
            return new AutoValue_HomeToolbar_MenuItem(id, text, icon);
        }
    }

    static final class MenuAdapter extends RecyclerView.Adapter<MenuViewHolder> {

        private final Subject<MenuItem> itemClicksSubject = PublishSubject.create();

        Observable<MenuItem> itemClicks() {
            return itemClicksSubject;
        }

        @Override
        public MenuViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MenuViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.main_home_toolbar_menu_item, parent, false));
        }

        @Override
        public void onBindViewHolder(MenuViewHolder holder, int position) {
            holder.bind(MENU_ITEMS.get(position));
            holder.itemView.setOnClickListener(v ->
                    itemClicksSubject.onNext(MENU_ITEMS.get(holder.getAdapterPosition())));
        }

        @Override
        public int getItemCount() {
            return MENU_ITEMS.size();
        }
    }

    static final class MenuViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;

        MenuViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }

        void bind(MenuItem item) {
            textView.setText(item.text());
            Drawable icon = itemView.getResources().getDrawable(item.icon(), null);
            icon.setTint(textView.getCurrentTextColor());
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
        }
    }
}
