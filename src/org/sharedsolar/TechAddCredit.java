package org.sharedsolar;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sharedsolar.R;
import org.sharedsolar.adapter.TechAddCreditAdapter;
import org.sharedsolar.db.DatabaseAdapter;
import org.sharedsolar.model.CreditSummaryModel;
import org.sharedsolar.tool.Connector;
import org.sharedsolar.tool.MyUI;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import android.util.Log;

public class TechAddCredit extends ListActivity {
	// the next two lines are for logging, if "debugMode" in config.xml is true
	private final String TAG = this.getClass().getSimpleName();
	private static boolean DEBUG;

	private ArrayList<CreditSummaryModel> modelList;
	private ArrayList<CreditSummaryModel> addedModelList;
	private TechAddCreditAdapter techAddCreditAdapter;
	private DatabaseAdapter dbAdapter;
	private ProgressDialog progressDialog;
	private String info;
	private String requestTokenJson;
	private JSONObject tokenAtMeterJson;
	private int uploadTokenStatus;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tech_add_credit);
		
		DEBUG = getResources().getBoolean(R.bool.debugMode);

		// get model list and token at meter from db
		dbAdapter = new DatabaseAdapter(this);
		dbAdapter.open();
		modelList = dbAdapter.getCreditSummaryModelList();
		tokenAtMeterJson = dbAdapter.getTokenAtMeter();
		dbAdapter.close();

		// list adapter
		techAddCreditAdapter = new TechAddCreditAdapter(this,
				R.layout.tech_add_credit_item, modelList,
				(TextView) findViewById(R.id.techAddCreditAddedTV),
				(Button) findViewById(R.id.techAddCreditSubmitBtn));
		setListAdapter(techAddCreditAdapter);

		// submit
		Button submitBtn = (Button) findViewById(R.id.techAddCreditSubmitBtn);
		submitBtn.setOnClickListener(submitBtnClickListener);
		submitBtn.setEnabled(false);
		
		// upload tokens to gateway
		try {
			if (tokenAtMeterJson.getJSONArray("tokens").length() > 0)
			{
				progressDialog = ProgressDialog.show(this, "", getString(R.string.uploadingTokens));
				new Thread() {
					public void run() {
						uploadTokenStatus = new Connector(TechAddCredit.this).uploadToken(
										getString(R.string.uploadTokenUrl), tokenAtMeterJson);
						uploadHandler.sendEmptyMessage(0);
					}
				}.start();
			}
		} catch (JSONException e) {
			if (DEBUG) Log.d(TAG, e.toString());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	View.OnClickListener submitBtnClickListener = new OnClickListener() {
		public void onClick(View view) {
			info = "";
			// build new model list
			ListView list = getListView();
			addedModelList = new ArrayList<CreditSummaryModel>();
			for (int i = 0; i < list.getChildCount(); i++) {
				LinearLayout row = (LinearLayout) list.getChildAt(i);
				TextView denominationTV = (TextView) row.getChildAt(1);
				TextView addedCountTV = (TextView) row.getChildAt(2);
				int denomination = Integer.parseInt(denominationTV.getText()
						.toString());
				int addedCount = Integer.parseInt(addedCountTV.getText()
						.toString());
				if (addedCount != 0) {
					addedModelList.add(new CreditSummaryModel(denomination,
							addedCount));
					info += getString(R.string.denomination) + " "
							+ denomination + ": " + addedCount + " "
							+ getString(R.string.added) + "\n";
				}
			}
			// update info string
			String creditAdded = ((TextView) TechAddCredit.this
					.findViewById(R.id.techAddCreditAddedTV)).getText()
					.toString();
			int newCr = Integer.parseInt(creditAdded);
			if (newCr > 0) {
				info += "\n" + getString(R.string.creditAddedLabel) + " "
						+ creditAdded;
				// dialog
				String message = info + "\n\n"
						+ getString(R.string.addCreditConfirm);
				MyUI.showlDialog(view.getContext(), R.string.addCredit,
						message, R.string.yes, R.string.no,
						submitDialoguePositiveClickListener);
			}
		}
	};

	DialogInterface.OnClickListener submitDialoguePositiveClickListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			dialog.cancel();
			progressDialog = ProgressDialog.show(TechAddCredit.this, "",
					getString(R.string.downloadingTokens));
			new Thread() {
				public void run() {
					Connector connector = new Connector(TechAddCredit.this);
					requestTokenJson = connector.requestToken(
							getString(R.string.requestTokenUrl),
							TechAddCredit.this, addedModelList);
					submitHandler.sendEmptyMessage(0);
				}
			}.start();
		}
	};
	
	private Handler uploadHandler = new Handler() {
		public void handleMessage(Message msg) {
			progressDialog.dismiss();
			if (uploadTokenStatus == Connector.CONNECTION_FAILURE) {
				MyUI.showNeutralDialog(TechAddCredit.this,
						R.string.uploadError,
						R.string.uploadTokensErrorMsg, R.string.ok);
				return;
			} else if (uploadTokenStatus == Connector.CONNECTION_TIMEOUT) {
				MyUI.showNeutralDialog(TechAddCredit.this,
						R.string.uploadError,
						R.string.uploadTokensTimeoutMsg, R.string.ok);
				return;
			} 
			dbAdapter.open();
			dbAdapter.expireTokenAtMeter();
			dbAdapter.close();
			MyUI.showNeutralDialog(TechAddCredit.this,
					R.string.syncWithGateway,
					R.string.syncCompleted, R.string.ok);
		}
	};

	private Handler submitHandler = new Handler() {
		public void handleMessage(Message msg) {
			progressDialog.dismiss();
			if (requestTokenJson == null) {
				MyUI.showNeutralDialog(TechAddCredit.this,
						R.string.downloadError,
						R.string.downloadTokensErrorMsg, R.string.ok);
				return;
			}
			// timeout
			if (requestTokenJson.equals(String.valueOf(Connector.CONNECTION_TIMEOUT))) {
				MyUI.showNeutralDialog(TechAddCredit.this,
						R.string.downloadError, R.string.downloadTokensTimeoutMsg,
						R.string.ok);
				return;
			}
			// check device validity
			if (requestTokenJson.equals("\"Not a valid device\"")) {
				MyUI.showNeutralDialog(TechAddCredit.this,
						R.string.invalidDevice, R.string.invalidDeviceMsg,
						R.string.ok);
				return;
			}
			
			// insert tokens to db
			dbAdapter.open();
			try {
				JSONArray arr = new JSONArray(requestTokenJson);
				for (int i = 0; i < arr.length(); i++) {
					JSONObject ele = arr.getJSONObject(i);
					dbAdapter.insertTokenAtVendor(ele.getLong("token_id"),
							ele.getInt("denomination"));
				}
			} catch (JSONException e) {
				MyUI.showNeutralDialog(TechAddCredit.this,
						R.string.invalidTokens, R.string.invalidTokensMsg,
						R.string.ok);
				dbAdapter.close();
				return;
			}
			dbAdapter.close();
			Intent intent = new Intent(TechAddCredit.this,
					TechAddCreditReceipt.class);
			intent.putExtra("info", info);
			startActivity(intent);
		}
	};
}