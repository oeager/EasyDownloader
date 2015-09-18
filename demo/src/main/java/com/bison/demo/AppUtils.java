package com.bison.demo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

public class AppUtils {

    public static void install(Context context,String mUrl) {
        // 核心是下面几句代码
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(mUrl)),
                "application/vnd.android.package-archive");
        context.startActivity(intent);
    }
}
