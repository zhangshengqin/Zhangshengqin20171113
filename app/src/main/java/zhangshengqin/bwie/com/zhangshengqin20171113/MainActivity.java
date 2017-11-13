package zhangshengqin.bwie.com.zhangshengqin20171113;

import java.io.File;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import butterknife.BindView;
import butterknife.ButterKnife;



public class MainActivity extends AppCompatActivity {
    private static final int PROCESSING = 1;
    private static final int FAILURE = -1;
    @BindView(R.id.downloadbutton)
    Button downloadbutton;
    @BindView(R.id.stopbutton)
    Button stopbutton;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.resultView)
    TextView resultView;

    private Handler handler = new UIHandler();

    private final class UIHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PROCESSING: // 更新进度
                    progressBar.setProgress(msg.getData().getInt("size"));
                    float num = (float) progressBar.getProgress()
                            / (float) progressBar.getMax();
                    int result = (int) (num * 100); // 计算进度
                    resultView.setText(result + "%");
                    if (progressBar.getProgress() == progressBar.getMax()) { // 下载完成
                        Toast.makeText(getApplicationContext(), "下载成功",
                                Toast.LENGTH_LONG).show();
                    }
                    break;
                case FAILURE: // 下载失败
                    Toast.makeText(getApplicationContext(), "下载失败",
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        ButtonClickListener listener = new ButtonClickListener();
        downloadbutton.setOnClickListener(listener);
        stopbutton.setOnClickListener(listener);
    }

    private final class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.downloadbutton: // 开始下载
                    // 其他文件下载的链接
                    String path = "http://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4";
                    String filename = path.substring(path.lastIndexOf('/') + 1);

                    try {
                        // URL编码（这里是为了将中文进行URL编码）
                        filename = URLEncoder.encode(filename, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    path = path.substring(0, path.lastIndexOf("/") + 1) + filename;
                    if (Environment.getExternalStorageState().equals(
                            Environment.MEDIA_MOUNTED)) {
                        // File savDir =
                        // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                        // 保存路径
                        File savDir = Environment.getExternalStorageDirectory();
                        download(path, savDir);
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "吐", Toast.LENGTH_LONG).show();
                    }
                    downloadbutton.setEnabled(false);
                    stopbutton.setEnabled(true);
                    break;
                case R.id.stopbutton: // 暂停下载
                    exit();
                    Toast.makeText(getApplicationContext(),
                            "Now thread is Stopping!!", Toast.LENGTH_LONG).show();
                    downloadbutton.setEnabled(true);
                    stopbutton.setEnabled(false);
                    break;
            }
        }

        /*
         * 由于用户的输入事件(点击button, 触摸屏幕....)是由主线程负责处理的，如果主线程处于工作状态，
         * 此时用户产生的输入事件如果没能在5秒内得到处理，系统就会报“应用无响应”错误。
         * 所以在主线程里不能执行一件比较耗时的工作，否则会因主线程阻塞而无法处理用户的输入事件，
         * 导致“应用无响应”错误的出现。耗时的工作应该在子线程里执行。
         */
        private DownloadTask task;

        private void exit() {
            if (task != null)
                task.exit();
        }

        private void download(String path, File savDir) {
            task = new DownloadTask(path, savDir);
            new Thread(task).start();
        }

        /**
         * UI控件画面的重绘(更新)是由主线程负责处理的，如果在子线程中更新UI控件的值，更新后的值不会重绘到屏幕上
         * 一定要在主线程里更新UI控件的值，这样才能在屏幕上显示出来，不能在子线程中更新UI控件的值
         */
        private final class DownloadTask implements Runnable {
            private String path;
            private File saveDir;
            private FileDownloader loader;

            public DownloadTask(String path, File saveDir) {
                this.path = path;
                this.saveDir = saveDir;
            }

            /**
             * 退出下载
             */
            public void exit() {
                if (loader != null)
                    loader.exit();
            }

            DownloadProgressListener downloadProgressListener = new DownloadProgressListener() {
                @Override
                public void onDownloadSize(int size) {
                    Message msg = new Message();
                    msg.what = PROCESSING;
                    msg.getData().putInt("size", size);
                    handler.sendMessage(msg);
                }
            };

            public void run() {
                try {
                    // 实例化一个文件下载器
                    loader = new FileDownloader(getApplicationContext(), path,
                            saveDir, 4);
                    // 设置进度条最大值
                    progressBar.setMax(loader.getFileSize());
                    loader.download(downloadProgressListener);
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendMessage(handler.obtainMessage(FAILURE)); // 发送一条空消息对象
                }
            }
        }
    }
}
