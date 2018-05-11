package ca.uoguelph.pspenler.maptracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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

        nameEdit = findViewById(R.id.experimentName_edit);
        fileEdit = findViewById(R.id.configurationFile_edit);
        serverEdit = findViewById(R.id.resultsServer_edit);
        labelEdit = findViewById(R.id.beaconLabel_edit);
        heightEdit = findViewById(R.id.beaconHeight_edit);

        nameEdit.setText(configuration.getName());
        fileEdit.setText(configuration.getConfigFile());
        serverEdit.setText(configuration.getResultsServer());
        labelEdit.setText(configuration.getBeaconLabel());
        if(configuration.getBeaconHeight() != 0) {
            heightEdit.setText(Integer.toString(configuration.getBeaconHeight()));
        }
    }

    public void submitConfiguration(View view) {
        try {
            configuration.initConfig(nameEdit.getText().toString(), /*"file:///storage/emulated/0/Documents/config.cfg"*/fileEdit.getText().toString(), serverEdit.getText().toString(), labelEdit.getText().toString(), heightEdit.getText().toString());
            Intent intent = new Intent();
            intent.putExtra("configObject", configuration);
            setResult(RESULT_OK, intent);
            finish();
        }
        catch(Exception e){
            Toast.makeText(ConfigureActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
