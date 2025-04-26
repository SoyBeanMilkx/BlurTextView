# BlurTextView



<img title="" src="./art/show.png" alt="show.png" width="391">



给文字添加了毛玻璃的效果



## 如何使用

```xml
<com.yuuki.blurtextview.blur.BlurTextView
        android:id="@+id/blurText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="center" />
```

```java
BlurTextView blurTextView = findViewById(R.id.blurText);
        blurTextView.setText("Yuuki");
        blurTextView.setTextSize(350); // px
        blurTextView.setBlurRadius(45);

```


