package de.tubs.ibr.dtn.sharebox.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import de.tubs.ibr.dtn.api.Node;
import de.tubs.ibr.dtn.api.SingletonEndpoint;
import de.tubs.ibr.dtn.sharebox.R;

public class SelectDestinationActivity extends Activity {
    Button button;
    EditText text;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_destination);

        button = (Button)findViewById(R.id.select_destination_button);
        text = (EditText) findViewById(R.id.select_destination_edittext);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent();
                // Create destination Node
                Node n = new Node();
                n.endpoint = new SingletonEndpoint(text.getText().toString());
                n.type = "NODE_DISCOVERED";
                // Put Node into Intent
                intent.putExtra(de.tubs.ibr.dtn.Intent.EXTRA_NODE, n);
                // Return to ShareWithActivity
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
