package com.example.lmq.testsensor2;

/**
 * Created by lmq on 2017/7/6.
 */

public class ButterWorth {
    int n;
    float Wn;
    float [] B, A;
    /*
    *   B numerator 分子
    *   A denominator 分母
     */
    public ButterWorth(int nn, float Wnn, String typeName)
    {
        this.n = nn;
        this.Wn = Wnn;
        if(n > 500) return;
        B = new float[n+1];
        A = new float[n+1];
        int fs = 2;
        float u = 2*fs*(float)Math.tan(Math.PI*Wn/fs);
        Wn = u;
    }

}
