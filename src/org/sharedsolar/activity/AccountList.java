package org.sharedsolar.activity;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sharedsolar.R;
import org.sharedsolar.adapter.AccountListAdapter;
import org.sharedsolar.model.AccountListModel;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;

public class AccountList extends ListActivity {
	
	private ArrayList<AccountListModel> modelList;
	private AccountListAdapter accountListAdapter;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_list);
        
        modelList = new ArrayList<AccountListModel>();
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	String jsonString = extras.getString("accountList");
        	Log.d("d", jsonString);
        	try {
				JSONArray jsonArray =new JSONArray(jsonString);
				for (int i=0; i<jsonArray.length(); i++) {
					JSONObject jsonObject = (JSONObject)jsonArray.get(i);
					AccountListModel model = new AccountListModel(jsonObject.getString("aid"), 
							jsonObject.getDouble("cr"));
					modelList.add(model);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			accountListAdapter = new AccountListAdapter(this, R.layout.account_list_item, modelList);			
			setListAdapter(accountListAdapter);
			accountListAdapter.notifyDataSetChanged();
        }
        	
    }
}