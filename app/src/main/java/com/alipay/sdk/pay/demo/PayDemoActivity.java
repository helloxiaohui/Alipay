package com.alipay.sdk.pay.demo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alipay.sdk.app.AuthTask;
import com.alipay.sdk.app.PayTask;
import com.alipay.sdk.pay.demo.util.OrderInfoUtil2_0;

import java.util.Map;

/**
 * 重要说明:
 * <p>
 * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
 * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
 * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
 */
public class PayDemoActivity extends FragmentActivity {

    /**
     * 支付宝支付业务：入参app_id
     */
    public static final String APPID = "xxxxxxxxxxx";

    /**
     * 支付宝账户登录授权业务：入参pid值
     */
    public static final String PID = "xxxxxxxxxxx";


    /** 商户私钥，pkcs8格式 */
    /** 如下私钥，RSA2_PRIVATE 或者 RSA_PRIVATE 只需要填入一个 */
    /** 如果商户两个都设置了，优先使用 RSA2_PRIVATE */
    /** RSA2_PRIVATE 可以保证商户交易在更加安全的环境下进行，建议使用 RSA2_PRIVATE */
    /** 获取 RSA2_PRIVATE，建议使用支付宝提供的公私钥生成工具生成， */
    /**
     * 工具地址：https://doc.open.alipay.com/docs/doc.htm?treeId=291&articleId=106097&docType=1
     */
    public static final String RSA2_PRIVATE = "xxxxxxxxxxx";
    public static final String RSA_PRIVATE = "xxxxxxxxxxx";

    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_AUTH_FLAG = 2;
    private EditText edInfo;
    private TextView tvResult;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressWarnings("unused")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SDK_PAY_FLAG: {
                    @SuppressWarnings("unchecked")
                    PayResult payResult = new PayResult((Map<String, String>) msg.obj);
                    /**
                     对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                     */
                    String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                    String resultStatus = payResult.getResultStatus();
                    // 判断resultStatus 为9000则代表支付成功
                    if (TextUtils.equals(resultStatus, "9000")) {
                        // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                        Toast.makeText(PayDemoActivity.this, "支付成功", Toast.LENGTH_SHORT).show();
                    } else {
                        // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        Toast.makeText(PayDemoActivity.this, "支付失败", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case SDK_AUTH_FLAG: {
                    @SuppressWarnings("unchecked")
                    AuthResult authResult = new AuthResult((Map<String, String>) msg.obj, true);
                    String resultStatus = authResult.getResultStatus();

                    // 判断resultStatus 为“9000”且result_code
                    // 为“200”则代表授权成功，具体状态码代表含义可参考授权接口文档
                    if (TextUtils.equals(resultStatus, "9000") && TextUtils.equals(authResult.getResultCode(), "200")) {
                        // 获取alipay_open_id，调支付时作为参数extern_token 的value
                        // 传入，则支付账户为该授权账户
                        Toast.makeText(PayDemoActivity.this,
                                "授权成功\n" + String.format("authCode:%s", authResult.getAuthCode()), Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        // 其他状态值则为授权失败
                        Toast.makeText(PayDemoActivity.this,
                                "授权失败" + String.format("authCode:%s", authResult.getAuthCode()), Toast.LENGTH_SHORT).show();

                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pay_main);
        edInfo = (EditText) findViewById(R.id.edInfo);
        tvResult = (TextView) findViewById(R.id.tvResult);
    }

    public void payStart(View v) {
        finalOrderInfo=edInfo.getText().toString();
        Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                PayTask alipay = new PayTask(PayDemoActivity.this);
                final Map<String, String> result = alipay.payV2(finalOrderInfo, true);

                tvResult.post(new Runnable() {
                    @Override
                    public void run() {
                        tvResult.setText(result.toString());
                    }
                });

                Log.i("msp", result.toString());

                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    public void btnClear(View v) {
        finalOrderInfo = "";
        edInfo.setText("");
    }

    String finalOrderInfo = "";

    /**
     * 支付宝支付业务
     *
     * @param v
     */
    public void payV2(View v) {
        tvResult.setText("");
        if (TextUtils.isEmpty(APPID) || (TextUtils.isEmpty(RSA2_PRIVATE) && TextUtils.isEmpty(RSA_PRIVATE))) {
            new AlertDialog.Builder(this).setTitle("警告").setMessage("需要配置APPID | RSA_PRIVATE")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialoginterface, int i) {
                            //
                            finish();
                        }
                    }).show();
            return;
        }

//        *
//         * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
//         * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
//         * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
//         *
//         * orderInfo的获取必须来自服务端

        boolean rsa2 = (RSA2_PRIVATE.length() > 0);
        Map<String, String> params = OrderInfoUtil2_0.buildOrderParamMap(APPID, rsa2);
        String orderParam = OrderInfoUtil2_0.buildOrderParam(params);

        String privateKey = rsa2 ? RSA2_PRIVATE : RSA_PRIVATE;
        String sign = OrderInfoUtil2_0.getSign(params, privateKey, rsa2);
        String orderInfo = orderParam + "&" + sign;
        //String orderInfo = "";

        //orderInfo = "alipay_sdk=alipay-sdk-php-20180705&app_id=2018052160178133&biz_content=%7B%22body%22%3A%22%E4%B8%89%E4%B9%B0%E5%95%86%E5%93%81%E8%AE%A2%E5%8D%95-%E8%AE%A2%E5%8D%95%E7%BC%96%E5%8F%B7%EF%BC%9A1698354379086%22%2C%22subject%22%3A+%22%E4%B8%89%E4%B9%B0%E5%95%86%E5%93%81%E8%AE%A2%E5%8D%95-%E8%AE%A2%E5%8D%95%E7%BC%96%E5%8F%B7%EF%BC%9A1698354379086%22%2C%22out_trade_no%22%3A+%221698354379086%22%2C%22timeout_express%22%3A+%2215m%22%2C%22total_amount%22%3A+%2292.00%22%22product_code%22%3A%22QUICK_MSECURITY_PAY%22%7D&charset=UTF-8&format=json&method=alipay.trade.app.pay&notify_url=http%3A%2F%2Fuser.ypcang.com%2Fuser.php%2FtradeNotice%2FAlipayNotice%2Falipay&sign_type=RSA2&timestamp=2018-07-16+13%3A39%3A03&version=1.0&sign=n7GcYM%2FYe6%2FPgE3MDolIJCR9yZrP2uVxs27s46BELb28D7Qy6EHoLg3ITcFqg1IgcVkHMqu2%2Fvxg66vElGUfxXiqajLRA6Ki3r7MzPfJyvzMP4cIymlWeuV2JN%2BlS87ZwGFeUiJlnsmjwuxp7W%2BN9dkVmo6e260X3ur%2FpBMfMIxTRGOP2nPpLqf0XY1in5mVdRxWf%2FtMPvWRl3V5l%2FAadCjef4MbBui9K5Meii10KtWa3as1G2OrjoSzdfDvmea2PGxZoW8X01rptf%2Bkz%2BV7IoJC5YtvtXoYOqGzAJHVUEJRFLyHgxoy%2FKmAqRaSkcCPXYoHUryuhNJ%2Bozat7EXARg%3D%3D";

        //orderInfo = "app_id=2018052160178133&biz_content=%7B%22body%22%3A%22%E4%B8%89%E4%B9%B0%E5%95%86%E5%93%81%E8%AE%A2%E5%8D%95-%E8%AE%A2%E5%8D%95%E7%BC%96%E5%8F%B7%EF%BC%9A1698354379086%22%2C%22subject%22%3A+%22%E4%B8%89%E4%B9%B0%E5%95%86%E5%93%81%E8%AE%A2%E5%8D%95-%E8%AE%A2%E5%8D%95%E7%BC%96%E5%8F%B7%EF%BC%9A1698354379086%22%2C%22out_trade_no%22%3A+%221698354379086%22%2C%22timeout_express%22%3A+%2215m%22%2C%22total_amount%22%3A+%2292.00%22%22product_code%22%3A%22QUICK_MSECURITY_PAY%22%7D&charset=UTF-8&format=json&method=alipay.trade.app.pay&notify_url=http%3A%2F%2Fuser.ypcang.com%2Fuser.php%2FtradeNotice%2FAlipayNotice%2Falipay&sign_type=RSA2&timestamp=2018-07-16+13%3A39%3A03&version=1.0&sign=n7GcYM%2FYe6%2FPgE3MDolIJCR9yZrP2uVxs27s46BELb28D7Qy6EHoLg3ITcFqg1IgcVkHMqu2%2Fvxg66vElGUfxXiqajLRA6Ki3r7MzPfJyvzMP4cIymlWeuV2JN%2BlS87ZwGFeUiJlnsmjwuxp7W%2BN9dkVmo6e260X3ur%2FpBMfMIxTRGOP2nPpLqf0XY1in5mVdRxWf%2FtMPvWRl3V5l%2FAadCjef4MbBui9K5Meii10KtWa3as1G2OrjoSzdfDvmea2PGxZoW8X01rptf%2Bkz%2BV7IoJC5YtvtXoYOqGzAJHVUEJRFLyHgxoy%2FKmAqRaSkcCPXYoHUryuhNJ%2Bozat7EXARg%3D%3D";

        finalOrderInfo = orderInfo;

        edInfo.setText(finalOrderInfo);

    }

    /**
     * 支付宝账户授权业务
     *
     * @param v
     */
    public void authV2(View v) {
        /*if (TextUtils.isEmpty(PID) || TextUtils.isEmpty(APPID)
                || (TextUtils.isEmpty(RSA2_PRIVATE) && TextUtils.isEmpty(RSA_PRIVATE))
				|| TextUtils.isEmpty(TARGET_ID)) {
			new AlertDialog.Builder(this).setTitle("警告").setMessage("需要配置PARTNER |APP_ID| RSA_PRIVATE| TARGET_ID")
					.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialoginterface, int i) {
						}
					}).show();
			return;
		}

		*//**
         * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
         * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
         * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
         *
         * authInfo的获取必须来自服务端；
         *//*
        boolean rsa2 = (RSA2_PRIVATE.length() > 0);
		Map<String, String> authInfoMap = OrderInfoUtil2_0.buildAuthInfoMap(PID, APPID, TARGET_ID, rsa2);
		String info = OrderInfoUtil2_0.buildOrderParam(authInfoMap);
		
		String privateKey = rsa2 ? RSA2_PRIVATE : RSA_PRIVATE;
		String sign = OrderInfoUtil2_0.getSign(authInfoMap, privateKey, rsa2);*/
        //String authInfo = info + "&" + sign;
        String authInfo = "";

        authInfo = "alipay_sdk=alipay-sdk-php-20180705&app_id=2018052160178133&biz_content=%7B%22body%22%3A%22%E4%B8%89%E4%B9%B0%E5%95%86%E5%93%81%E8%AE%A2%E5%8D%95-%E8%AE%A2%E5%8D%95%E7%BC%96%E5%8F%B7%EF%BC%9A1698354379086%22%2C%22subject%22%3A+%22%E4%B8%89%E4%B9%B0%E5%95%86%E5%93%81%E8%AE%A2%E5%8D%95-%E8%AE%A2%E5%8D%95%E7%BC%96%E5%8F%B7%EF%BC%9A1698354379086%22%2C%22out_trade_no%22%3A+%221698354379086%22%2C%22timeout_express%22%3A+%2215m%22%2C%22total_amount%22%3A+%2292.00%22%22product_code%22%3A%22QUICK_MSECURITY_PAY%22%7D&charset=UTF-8&format=json&method=alipay.trade.app.pay&notify_url=http%3A%2F%2Fuser.ypcang.com%2Fuser.php%2FtradeNotice%2FAlipayNotice%2Falipay&sign_type=RSA2&timestamp=2018-07-16+13%3A39%3A03&version=1.0&sign=n7GcYM%2FYe6%2FPgE3MDolIJCR9yZrP2uVxs27s46BELb28D7Qy6EHoLg3ITcFqg1IgcVkHMqu2%2Fvxg66vElGUfxXiqajLRA6Ki3r7MzPfJyvzMP4cIymlWeuV2JN%2BlS87ZwGFeUiJlnsmjwuxp7W%2BN9dkVmo6e260X3ur%2FpBMfMIxTRGOP2nPpLqf0XY1in5mVdRxWf%2FtMPvWRl3V5l%2FAadCjef4MbBui9K5Meii10KtWa3as1G2OrjoSzdfDvmea2PGxZoW8X01rptf%2Bkz%2BV7IoJC5YtvtXoYOqGzAJHVUEJRFLyHgxoy%2FKmAqRaSkcCPXYoHUryuhNJ%2Bozat7EXARg%3D%3D";

        final String finalAuthInfo = authInfo;
        Runnable authRunnable = new Runnable() {

            @Override
            public void run() {
                // 构造AuthTask 对象
                AuthTask authTask = new AuthTask(PayDemoActivity.this);
                // 调用授权接口，获取授权结果
                Map<String, String> result = authTask.authV2(finalAuthInfo, true);

                Message msg = new Message();
                msg.what = SDK_AUTH_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread authThread = new Thread(authRunnable);
        authThread.start();
    }

    /**
     * get the sdk version. 获取SDK版本号
     */
    public void getSDKVersion() {
        PayTask payTask = new PayTask(this);
        String version = payTask.getVersion();
        Toast.makeText(this, version, Toast.LENGTH_SHORT).show();
    }

    /**
     * 原生的H5（手机网页版支付切natvie支付） 【对应页面网页支付按钮】
     *
     * @param v
     */
    public void h5Pay(View v) {
        Intent intent = new Intent(this, H5PayDemoActivity.class);
        Bundle extras = new Bundle();
        /**
         * url 是要测试的网站，在 Demo App 中会使用 H5PayDemoActivity 内的 WebView 打开。
         *
         * 可以填写任一支持支付宝支付的网站（如淘宝或一号店），在网站中下订单并唤起支付宝；
         * 或者直接填写由支付宝文档提供的“网站 Demo”生成的订单地址
         * （如 https://mclient.alipay.com/h5Continue.htm?h5_route_token=303ff0894cd4dccf591b089761dexxxx）
         * 进行测试。
         *
         * H5PayDemoActivity 中的 MyWebViewClient.shouldOverrideUrlLoading() 实现了拦截 URL 唤起支付宝，
         * 可以参考它实现自定义的 URL 拦截逻辑。
         */
        String url = "http://m.taobao.com";
        extras.putString("url", url);
        intent.putExtras(extras);
        startActivity(intent);
    }

}
