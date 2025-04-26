package com.yuuki.blurtextview;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.yuuki.blurtextview.blur.BlurTextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setImmersiveStatusBar(this);
        setContentView(R.layout.activity_main);

        BlurTextView blurTextView = findViewById(R.id.blurText);
        blurTextView.setText("Yuuki");

        //Typeface customFont = Typeface.createFromAsset(getAssets(), "fonts/HELVETI1.ttf"); // 不知道为啥没用，我用TextView试了也不行
        //blurTextView.setTypeface(customFont);

        blurTextView.setTextSize(350); // px
        blurTextView.setBlurRadius(45);

    }

    public static void setImmersiveStatusBar(Activity activity) {
        Window window = activity.getWindow();

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        window.setStatusBarColor(Color.TRANSPARENT);

        int flags = window.getDecorView().getSystemUiVisibility();
        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        window.getDecorView().setSystemUiVisibility(flags);
    }

}