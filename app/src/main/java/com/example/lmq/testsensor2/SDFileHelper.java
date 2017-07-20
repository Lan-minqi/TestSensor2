package com.example.lmq.testsensor2;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by lmq on 2017/2/28.
 */

public class SDFileHelper {
    private Context context;
    FileOutputStream output;
    FileInputStream input;
    public SDFileHelper() {
    }
    public SDFileHelper(Context context, String filename, String typeName) throws Exception {
        super();
        this.context = context;
        //如果手机已插入sd卡,且app具有读写sd卡的权限
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filename = Environment.getExternalStorageDirectory().getCanonicalPath() + "/"+"testsensors"+"/" + filename;
            //这里就不要用openFileOutput了,那个是往手机内存中写数据的
            if(typeName.toLowerCase() == "write")
                output = new FileOutputStream(filename);
            else if(typeName.toLowerCase() == "read")
                input = new FileInputStream(filename);
            else Toast.makeText(context, "参数错误", Toast.LENGTH_SHORT).show();
        } else Toast.makeText(context, "SD卡不存在或者不可读写", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if(output != null)
            output.close();
        if(input != null)
            input.close();
    }
    //往SD卡写入文件的方法
    public void savaFileToSD(String filecontent) throws IOException {
        output.write(filecontent.getBytes());
        //将String字符串以字节流的形式写入到输出流中
    }
    public StringBuffer readFileFromSD() throws IOException {
        StringBuffer fileContent = new StringBuffer("");
        byte[] buffer = new byte[1024];
        int n;
        while ((n = input.read(buffer)) != -1)
        {
            fileContent.append(new String(buffer, 0, n));
        }
        fileContent.append("END");
        return fileContent;
    }
}