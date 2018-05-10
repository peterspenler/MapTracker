package ca.uoguelph.pspenler.maptracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ConfigureActivity extends AppCompatActivity {

    private Configuration configuration;

    private EditText nameEdit;// = (EditText) findViewById(R.id.experimentName_edit);
    private EditText fileEdit;// = (EditText) findViewById(R.id.configurationFile_edit);
    private EditText serverEdit;// = (EditText) findViewById(R.id.resultsServer_edit);
    private EditText labelEdit;// = (EditText) findViewById(R.id.beaconLabel_edit);
    private EditText heightEdit;// = (EditText) findViewById(R.id.beaconHeight_edit);

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
        heightEdit.setText(Integer.toString(configuration.getBeaconHeight()));
    }

    public void submitConfiguration(View view) {
        try {
            configuration.initConfig(/*nameEdit.getText().toString()*/ "EXP NAME", "file:///storage/emulated/0/Documents/config.cfg"/*fileEdit.getText().toString()*/, "file:///storage/emulated/0/Documents/landmarkData.csv", /*labelEdit.getText().toString()*/ "LABELNAME", heightEdit.getText().toString());
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
