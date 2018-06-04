package ca.uoguelph.pspenler.maptracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

public class ConfigureActivity extends AppCompatActivity {

    private Configuration configuration;

    private EditText nameEdit;
    private EditText fileEdit;
    private EditText serverEdit;
    private EditText labelEdit;
    private EditText heightEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        configuration = getIntent().getParcelableExtra("configObject");

        SharedPreferences mPref = getPreferences(MODE_PRIVATE);
        String json = mPref.getString("config", "");
        if (!json.equals("")) {
            configuration = new Gson().fromJson(json, Configuration.class);
        }

        nameEdit = findViewById(R.id.experimentName_edit);
        fileEdit = findViewById(R.id.configurationFile_edit);
        serverEdit = findViewById(R.id.resultsServer_edit);
        labelEdit = findViewById(R.id.beaconLabel_edit);
        heightEdit = findViewById(R.id.beaconHeight_edit);

        nameEdit.setText(configuration.getName());
        fileEdit.setText(configuration.getConfigFile());
        serverEdit.setText(configuration.getResultsServer());
        labelEdit.setText(configuration.getBeaconLabel());
        if (configuration.getBeaconHeight() != 0) {
            heightEdit.setText(Float.toString(configuration.getBeaconHeight()));
        }
    }

    public void submitConfiguration(View view) {
        try {
            saveState();
            Intent intent = new Intent();
            intent.putExtra("configObject", configuration);
            setResult(RESULT_OK, intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(ConfigureActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveState() {
        try {
            configuration.initConfig(nameEdit.getText().toString(), fileEdit.getText().toString(), serverEdit.getText().toString(), labelEdit.getText().toString(), heightEdit.getText().toString());
        } catch (Exception e) {
            Log.d("Config", "Configuration failed to save with error but we will save anyway", e);
        }
        SharedPreferences mPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPref.edit();
        prefsEditor.putString("config", new Gson().toJson(configuration));
        prefsEditor.commit();
    }

    protected void onDestroy() {
        super.onDestroy();
        try {
            saveState();
        } catch (Exception e) {
            Toast.makeText(ConfigureActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }


    }

}
