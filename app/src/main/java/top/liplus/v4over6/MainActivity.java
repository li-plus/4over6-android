package top.liplus.v4over6;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Button btnConnect;

    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btn_connect);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isConnected = !isConnected;
                btnConnect.setText(isConnected ? R.string.disconnect : R.string.connect);
                Toast.makeText(view.getContext(), "Connected " + isConnected, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
