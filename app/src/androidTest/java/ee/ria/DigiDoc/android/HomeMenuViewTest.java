package ee.ria.DigiDoc.android;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;

import ee.ria.DigiDoc.R;

public final class HomeMenuViewTest {

    @Rule
    public ActivityScenarioRule<Activity> activityScenarioRule = new ActivityScenarioRule<>(Activity.class);

    @Test
    public void visibility_isVisible_ReturnsTrue() {
        activityScenarioRule.getScenario().onActivity(activity -> {
            LayoutInflater inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.main_home_menu, null);

            RadioButton estonianButton = layout.findViewById(R.id.mainHomeMenuLocaleEt);
            RadioButton englishButton = layout.findViewById(R.id.mainHomeMenuLocaleEn);
            RadioButton russianButton = layout.findViewById(R.id.mainHomeMenuLocaleRu);

            assertEquals(VISIBLE, estonianButton.getVisibility());
            assertEquals(VISIBLE, englishButton.getVisibility());
            assertEquals(VISIBLE, russianButton.getVisibility());
        });
    }

    @Test
    public void visibility_isHidden_ReturnsTrue() {
        activityScenarioRule.getScenario().onActivity(activity -> {
            LayoutInflater inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.main_home_menu, null);

            RadioButton estonianButton = layout.findViewById(R.id.mainHomeMenuLocaleEt);

            estonianButton.setVisibility(GONE);

            assertEquals(GONE, estonianButton.getVisibility());
        });
    }
}