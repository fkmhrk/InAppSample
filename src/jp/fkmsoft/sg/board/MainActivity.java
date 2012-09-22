package jp.fkmsoft.sg.board;

import java.lang.ref.WeakReference;

import jp.fkmsoft.sg.board.billing.BillingService;
import jp.fkmsoft.sg.board.billing.PurchaseObserver;
import jp.fkmsoft.sg.board.billing.ResponseHandler;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Handler mHandler;
    private BillingService mBillingService;
    private PurchaseObserver mObserver;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setListener();
        
        // billing service
        // observer 
        mHandler = new Handler();
        mObserver = new MarketObserver(this, mHandler);
        ResponseHandler.register(mObserver);
        mBillingService = new BillingService();
        mBillingService.setContext(this);
        
        if (!mBillingService.checkBillingSupported("subs")) {
            // show error dialog
            Toast.makeText(this, "Subscription is not supported", Toast.LENGTH_LONG).show();
        }
        
    }

    private void setListener() {
        ButtonListener listener = new ButtonListener(this);
        int[] ids = { 
                R.id.func_button,
                R.id.money_button,
                R.id.vip_button
        };
        for (int id : ids) {
            View button = findViewById(id);
            button.setOnClickListener(listener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        ResponseHandler.register(mObserver);
    }

    /**
     * Called when this activity is no longer visible.
     */
    @Override
    protected void onStop() {
        super.onStop();
        ResponseHandler.unregister(mObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBillingService.unbind();
    }    
    
    private static class ButtonListener implements OnClickListener {

        private WeakReference<MainActivity> activityRef;
        public ButtonListener(MainActivity activity) {
            this.activityRef = new WeakReference<MainActivity>(activity);
        }
        @Override
        public void onClick(View v) {
            MainActivity activity = activityRef.get();
            if (activity == null) { return; }
            
            switch (v.getId()) {
            case R.id.func_button:
                buyFunction(activity);
                break;
            }
        }
        private void buyFunction(MainActivity activity) {
            activity.mBillingService.requestPurchase("sg_board_stage_1", "inapp", "");
//            activity.mBillingService.requestPurchase("android.test.purchased", "inapp", "");
        }
        
    }
}
