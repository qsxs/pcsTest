package com.six.test;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.oauth.BaiduOAuth;
import com.baidu.pcs.BaiduPCSActionInfo;
import com.baidu.pcs.BaiduPCSClient;
import com.baidu.pcs.BaiduPCSStatusListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView textView;
    private Button btnLogin;
    private Button btnLogout;
    private Button btnUpload;
    private Button btnDownload;
    private Button btnFour;

    private String mbApiKey = "0vR3zycmFEVGQuUa4NOg8t8W";
    private final static String mbRootPath = "/apps/测试备份文件夹";//上传到网盘的路径
    private String mbOauth;
    private Handler mbUiThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mbUiThreadHandler = new Handler();
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        mbOauth = sp.getString("mbOauth", null);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
        btnLogin = (Button) findViewById(R.id.button);
        btnLogout = (Button) findViewById(R.id.button5);
        btnUpload = (Button) findViewById(R.id.button2);
        btnDownload = (Button) findViewById(R.id.button3);
        btnFour = (Button) findViewById(R.id.button4);

        btnLogin.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnUpload.setOnClickListener(this);
        btnDownload.setOnClickListener(this);
        btnFour.setOnClickListener(this);

        if(mbOauth !=null){
            textView.setText(mbOauth);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                login();
                break;
            case R.id.button2:
                upload("");
                break;
            case R.id.button3:
                download("", "");
                break;
            case R.id.button4:
                list();
                break;
            case R.id.button5:
                logout();
                break;
        }
    }

    private void login() {
        textView.setText("login");
        BaiduOAuth oauthClient = new BaiduOAuth();
        //第三个参数应该是请求的权限，basic:基础信息  netdisk:网盘
        oauthClient.startOAuth(this, mbApiKey, new String[]{"basic", "netdisk"}, new BaiduOAuth.OAuthListener() {
            @Override
            public void onException(String msg) {
                Toast.makeText(getApplicationContext(), "Login failed " + msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete(BaiduOAuth.BaiduOAuthResponse response) {
                if (null != response) {
                    mbOauth = response.getAccessToken();
                    SharedPreferences sp = getPreferences(MODE_PRIVATE);
                    sp.edit().putString("mbOauth", mbOauth).apply();

                    textView.setText("Token: " + mbOauth + "\n" + "User name:" + response.getUserName());
                    Toast.makeText(getApplicationContext(), "Token: " + mbOauth + "    User name:" + response.getUserName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancel() {
                Toast.makeText(getApplicationContext(), "Login cancelled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void logout() {
        textView.setText("logout");
        if (null != mbOauth) {
            /**
             * you can call this method to logout in Android 4.0.3
             */
//    		BaiduOAuth oauth = new BaiduOAuth();
//    	    oauth.logout(mbOauth, new BaiduOAuth.ILogoutListener(){
//
//				@Override
//				public void onResult(boolean success) {
//
//					Toast.makeText(getApplicationContext(), "Logout: " + success, Toast.LENGTH_SHORT).show();
//				}
//
//    		});

            /**
             * you can call this method to logout in Android 2.X
             */
            Thread workThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    BaiduOAuth oauth = new BaiduOAuth();
                    final boolean ret = oauth.logout(mbOauth);
                    mbUiThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("Logout " + ret);
                            SharedPreferences sp = getPreferences(MODE_PRIVATE);
                            sp.edit().remove("mbOauth").apply();
                            Toast.makeText(getApplicationContext(), "Logout " + ret, Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            });

            workThread.start();
        }

    }

    /**
     * 上传
     *
     * @param tmpFile 上传文件路径
     */
    public void upload(final String tmpFile) {
        textView.setText("upload");
        if (null != mbOauth) {

            Thread workThread = new Thread(new Runnable() {
                public void run() {
                    BaiduPCSClient api = new BaiduPCSClient();
                    api.setAccessToken(mbOauth);
                    String fileName = tmpFile.substring(tmpFile.lastIndexOf("/") - 1);
                    final BaiduPCSActionInfo.PCSFileInfoResponse response = api.uploadFile(tmpFile, mbRootPath + fileName, new BaiduPCSStatusListener() {
                        @Override
                        public void onProgress(long bytes, long total) {
                            // TODO Auto-generated method stub

                            final long bs = bytes;
                            final long tl = total;

                            mbUiThreadHandler.post(new Runnable() {
                                public void run() {
                                    textView.setText("upload" + bs + "/" + tl);
                                    Toast.makeText(getApplicationContext(), "total: " + tl + "    sent:" + bs, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public long progressInterval() {
                            return 1000;
                        }
                    });

                    mbUiThreadHandler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), response.status.errorCode + "  " + response.status.message + "  " + response.commonFileInfo.blockList, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            workThread.start();
        }
    }

    /**
     * 下载
     *
     * @param source 云端路径
     * @param target 本地保存的路径
     */
    private void download(String source, String target) {
        textView.setText("download");
        if (null != mbOauth) {

            Thread workThread = new Thread(new Runnable() {
                public void run() {

                    BaiduPCSClient api = new BaiduPCSClient();
                    api.setAccessToken(mbOauth);
                    String source = mbRootPath + "/189.jpg";
                    String target = "/mnt/sdcard/DCIM/100MEDIA/yytest0801.mp4";
                    final BaiduPCSActionInfo.PCSSimplefiedResponse ret = api.downloadFileFromStream(source, target, new BaiduPCSStatusListener() {
                        //yangyangdd
                        @Override
                        public void onProgress(long bytes, long total) {
                            // TODO Auto-generated method stub
                            final long bs = bytes;
                            final long tl = total;

                            mbUiThreadHandler.post(new Runnable() {
                                public void run() {
                                    textView.setText("download" + bs + "/" + tl);
                                    Toast.makeText(getApplicationContext(), "total: " + tl + "    downloaded:" + bs, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public long progressInterval() {
                            return 500;
                        }

                    });

                    mbUiThreadHandler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Download files:  " + ret.errorCode + "   " + ret.message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            workThread.start();
        }
    }

    /**
     * 文件列表
     */
    private void list() {
        textView.setText("list");
        if (null != mbOauth) {

            Thread workThread = new Thread(new Runnable() {
                public void run() {

                    BaiduPCSClient api = new BaiduPCSClient();
                    api.setAccessToken(mbOauth);
                    String path = mbRootPath;

                    final BaiduPCSActionInfo.PCSListInfoResponse ret = api.list(path, "name", "asc");
                    //final BaiduPCSActionInfo.PCSListInfoResponse ret = api.imageStream();


                    mbUiThreadHandler.post(new Runnable() {
                        public void run() {
                            if (ret.list == null) {
                                textView.setText("没有文件");
                            } else {
                                for (BaiduPCSActionInfo.PCSCommonFileInfo fileInfo : ret.list) {
                                    textView.setText(textView.getText() + fileInfo.path + "\n");
                                }
                            }
                            Toast.makeText(getApplicationContext(), "List:  " + ret.status.errorCode + "    " + ret.status.message, Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            });

            workThread.start();
        }
    }
}
