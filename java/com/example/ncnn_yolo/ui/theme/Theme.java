package com.example.ncnn_yolo.ui.theme;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.ncnn_yolo.R;
import com.example.ncnn_yolo.ui.home.HomeViewModel;
import com.google.android.material.navigation.NavigationView;

public class Theme {

    // Pink theme colors
    private static final int DARK_PRIMARY = Color.parseColor("#FFC0CB");
    private static final int DARK_SECONDARY = Color.parseColor("#FFC0CB");
    private static final int DARK_TERTIARY = Color.parseColor("#FFC0CB");
    private static final int DARK_TEXT_COLOR = Color.parseColor("#FFFFFF");

    // Light theme colors
    private static final int LIGHT_PRIMARY = Color.parseColor("#FFFFFF");
    private static final int LIGHT_SECONDARY = Color.parseColor("#F0F0F0");
    private static final int LIGHT_TERTIARY = Color.parseColor("#E0E0E0");
    private static final int LIGHT_TEXT_COLOR = Color.parseColor("#000000");

    public static void applyTheme(Activity activity, boolean darkTheme, boolean dynamicColor) {
        int primaryColor;
        int secondaryColor;
        int tertiaryColor;
        int backgroundColor;
        int textColor;

        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Context context = activity.getApplicationContext();
            if (darkTheme) {
                primaryColor = getDynamicDarkColor(context, "primary");
                secondaryColor = getDynamicDarkColor(context, "secondary");
                tertiaryColor = getDynamicDarkColor(context, "tertiary");
                backgroundColor = DARK_PRIMARY;
                textColor = DARK_TEXT_COLOR;
            } else {
                primaryColor = getDynamicLightColor(context, "primary");
                secondaryColor = getDynamicLightColor(context, "secondary");
                tertiaryColor = getDynamicLightColor(context, "tertiary");
                backgroundColor = LIGHT_PRIMARY;
                textColor = LIGHT_TEXT_COLOR;
            }
        } else if (darkTheme) {
            primaryColor = DARK_PRIMARY;
            secondaryColor = DARK_SECONDARY;
            tertiaryColor = DARK_TERTIARY;
            backgroundColor = DARK_PRIMARY;
            textColor = DARK_TEXT_COLOR;
        } else {
            primaryColor = LIGHT_PRIMARY;
            secondaryColor = LIGHT_SECONDARY;
            tertiaryColor = LIGHT_TERTIARY;
            backgroundColor = LIGHT_PRIMARY;
            textColor = LIGHT_TEXT_COLOR;
        }

        setStatusBarColor(activity, primaryColor, darkTheme);
        applyBackgroundColor(activity, backgroundColor);
        applyViewColors(activity, primaryColor, secondaryColor, tertiaryColor);
        applyTextColor(activity.findViewById(android.R.id.content), textColor);

        HomeViewModel homeViewModel = new HomeViewModel();
        homeViewModel.setTextStateColor(textColor);
    }

    private static void setStatusBarColor(@NonNull Activity activity, int color, boolean darkTheme) {
        Window window = activity.getWindow();
        window.setStatusBarColor(color);
        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = decorView.getSystemUiVisibility();
            if (darkTheme) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }
    }

    private static void applyBackgroundColor(Activity activity, int backgroundColor) {
        View rootView = activity.findViewById(android.R.id.content);
        rootView.setBackgroundColor(backgroundColor);
    }

    private static void applyViewColors(Activity activity, int primaryColor, int secondaryColor, int tertiaryColor) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(primaryColor);
            toolbar.setTitleTextColor(Color.WHITE);

            toolbar.post(() -> {
                for (int i = 0; i < toolbar.getMenu().size(); i++) {
                    MenuItem item = toolbar.getMenu().getItem(i);
                    Drawable icon = item.getIcon();
                    if (icon != null) {
                        icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                    }

                    SpannableString spanString = new SpannableString(item.getTitle());
                    spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0);
                    item.setTitle(spanString);
                }
            });
        }

        NavigationView navigationView = activity.findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setBackgroundColor(secondaryColor);
            for (int i = 0; i < navigationView.getMenu().size(); i++) {
                MenuItem item = navigationView.getMenu().getItem(i);
                SpannableString title = new SpannableString(item.getTitle());
                title.setSpan(new ForegroundColorSpan(Color.WHITE), 0, title.length(), 0);
                item.setTitle(title);

                Drawable icon = item.getIcon();
                if (icon != null) {
                    icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                }
            }
        }

        DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerLayout.setScrimColor(tertiaryColor);
        }
    }

    private static void applyTextColor(View rootView, int textColor) {
        if (rootView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) rootView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childView = viewGroup.getChildAt(i);
                if (childView instanceof ViewGroup) {
                    applyTextColor(childView, textColor);
                } else if (childView instanceof TextView) {
                    ((TextView) childView).setTextColor(textColor);
                } else if (childView instanceof Spinner) {
                    Spinner spinner = (Spinner) childView;
                    ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
                    if (adapter != null) {
                        for (int j = 0; j < adapter.getCount(); j++) {
                            View spinnerItem = adapter.getView(j, null, spinner);
                            if (spinnerItem instanceof TextView) {
                                ((TextView) spinnerItem).setTextColor(textColor);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static int getDynamicDarkColor(Context context, String colorType) {
        switch (colorType) {
            case "primary":
                return DARK_PRIMARY;
            case "secondary":
                return DARK_SECONDARY;
            case "tertiary":
                return DARK_TERTIARY;
            default:
                return DARK_PRIMARY;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static int getDynamicLightColor(Context context, String colorType) {
        switch (colorType) {
            case "primary":
                return LIGHT_PRIMARY;
            case "secondary":
                return LIGHT_SECONDARY;
            case "tertiary":
                return LIGHT_TERTIARY;
            default:
                return LIGHT_PRIMARY;
        }
    }
}
