/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.fkmsoft.sg.board.billing;

import com.android.vending.billing.IMarketBillingService;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import jp.fkmsoft.sg.board.billing.BillingReceiver.MarketAction;
import jp.fkmsoft.sg.board.billing.BillingReceiver.MarketExtra;
import jp.fkmsoft.sg.board.billing.Security.VerifiedPurchase;
import jp.fkmsoft.sg.board.billing.request.CheckBillingSupported;
import jp.fkmsoft.sg.board.billing.request.ConfirmNotifications;
import jp.fkmsoft.sg.board.billing.request.GetPurchaseInformation;
import jp.fkmsoft.sg.board.billing.request.RequestPurchase;
import jp.fkmsoft.sg.board.billing.request.RestoreTransactions;


/**
 * This class sends messages to Android Market on behalf of the application by
 * connecting (binding) to the MarketBillingService. The application
 * creates an instance of this class and invokes billing requests through this service.
 *
 * The {@link BillingReceiver} class starts this service to process commands
 * that it receives from Android Market.
 *
 * You should modify and obfuscate this code before using it.
 */
public class BillingService extends Service implements ServiceConnection {
    private static final String TAG = "BillingService";

    public interface Action {
        public static final String ACTION_CONFIRM_NOTIFICATION =
                "com.example.subscriptions.CONFIRM_NOTIFICATION";
        public static final String ACTION_GET_PURCHASE_INFORMATION =
                "com.example.subscriptions.GET_PURCHASE_INFORMATION";
    }
    
    public static final String MARKET_BILLING_SERVICE_ACTION =
            "com.android.vending.billing.MarketBillingService.BIND";
    /** The service connection to the remote MarketBillingService. */
    private static IMarketBillingService mService;

    /**
     * The list of requests that are pending while we are waiting for the
     * connection to the MarketBillingService to be established.
     */
    private static LinkedList<BillingRequest> mPendingRequests = new LinkedList<BillingRequest>();

    /**
     * The list of requests that we have sent to Android Market but for which we have
     * not yet received a response code. The HashMap is indexed by the
     * request Id that each request receives when it executes.
     */
    private static HashMap<Long, BillingRequest> mSentRequests =
        new HashMap<Long, BillingRequest>();

    
    /**
     * send billing request by marketService
     * @param request
     * @return response
     * @throws RemoteException 
     */
    public Bundle sendBillingRequest(Bundle request) throws RemoteException {
        return mService.sendBillingRequest(request);
    }
    /**
     * add pending request
     * @param request
     */
    public void addPendingRequest(BillingRequest request) {
        mPendingRequests.add(request);
    }
    
    /**
     * add request with requestID
     * @param requestId
     * @param request
     */
    public void addSentRequest(Long requestId, BillingRequest request) {
        mSentRequests.put(requestId, request);
    }
    
    /**
     * return true if this service is connected to marketService
     * @return
     */
    public boolean isConnected() {
        return (mService != null);
    }
    
    public void closeMarketService() {
        mService = null;
    }

    public BillingService() {
        super();
    }

    public void setContext(Context context) {
        attachBaseContext(context);
    }

    /**
     * We don't support binding to this service, only starting the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand(intent, startId);
    }

    /**
     * The {@link BillingReceiver} sends messages to this service using intents.
     * Each intent has an action and some extra arguments specific to that action.
     * @param intent the intent containing one of the supported actions
     * @param startId an identifier for the invocation instance of this service
     */
    private void handleCommand(Intent intent, int startId) {
        String action = intent.getAction();
        if (BillingLogger.isDebugEnabled()) {
            BillingLogger.info(TAG, "handleCommand() action: " + action);
        }
        if (Action.ACTION_CONFIRM_NOTIFICATION.equals(action)) {
            String[] notifyIds = intent.getStringArrayExtra(MarketExtra.NOTIFICATION_ID);
            confirmNotifications(startId, notifyIds);
        } else if (Action.ACTION_GET_PURCHASE_INFORMATION.equals(action)) {
            String notifyId = intent.getStringExtra(MarketExtra.NOTIFICATION_ID);
            getPurchaseInformation(startId, new String[] { notifyId });
        } else if (MarketAction.ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
            String signedData = intent.getStringExtra(MarketExtra.INAPP_SIGNED_DATA);
            String signature = intent.getStringExtra(MarketExtra.INAPP_SIGNATURE);
            purchaseStateChanged(startId, signedData, signature);
        } else if (MarketAction.ACTION_RESPONSE_CODE.equals(action)) {
            long requestId = intent.getLongExtra(MarketExtra.INAPP_REQUEST_ID, -1);
            int responseCodeIndex = intent.getIntExtra(MarketExtra.INAPP_RESPONSE_CODE,
                    ResponseCode.RESULT_ERROR.ordinal());
            ResponseCode responseCode = ResponseCode.valueOf(responseCodeIndex);
            checkResponseCode(requestId, responseCode);
        }
    }

    /**
     * Binds to the MarketBillingService and returns true if the bind
     * succeeded.
     * @return true if the bind succeeded; false otherwise
     */
    boolean bindToMarketBillingService() {
        try {
            if (BillingLogger.isDebugEnabled()) {
                Log.i(TAG, "binding to Market billing service");
            }
            boolean bindResult = bindService(
                    new Intent(MARKET_BILLING_SERVICE_ACTION),
                    this,  // ServiceConnection.
                    Context.BIND_AUTO_CREATE);

            if (bindResult) {
                return true;
            } else {
                Log.e(TAG, "Could not bind to service.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e);
        }
        return false;
    }

    /**
     * Checks if in-app billing is supported.
     * @pram itemType Either Consts.ITEM_TYPE_INAPP or Consts.ITEM_TYPE_SUBSCRIPTION, indicating the
     *                type of item support is being checked for.
     * @return true if supported; false otherwise
     */
    public boolean checkBillingSupported(String itemType) {
        return new CheckBillingSupported(this, itemType).runRequest();
    }

    /**
     * Requests that the given item be offered to the user for purchase. When
     * the purchase succeeds (or is canceled) the {@link BillingReceiver}
     * receives an intent with the action {@link Consts#ACTION_NOTIFY}.
     * Returns false if there was an error trying to connect to Android Market.
     * @param productId an identifier for the item being offered for purchase
     * @param itemType  Either Consts.ITEM_TYPE_INAPP or Consts.ITEM_TYPE_SUBSCRIPTION, indicating
     *                  the type of item type support is being checked for.
     * @param developerPayload a payload that is associated with a given
     * purchase, if null, no payload is sent
     * @return false if there was an error connecting to Android Market
     */
    public boolean requestPurchase(String productId, String itemType, String developerPayload) {
        return new RequestPurchase(this, productId, itemType, developerPayload).runRequest();
    }

    /**
     * Requests transaction information for all managed items. Call this only when the
     * application is first installed or after a database wipe. Do NOT call this
     * every time the application starts up.
     * @return false if there was an error connecting to Android Market
     */
    public boolean restoreTransactions() {
        return new RestoreTransactions(this).runRequest();
    }

    /**
     * Confirms receipt of a purchase state change. Each {@code notifyId} is
     * an opaque identifier that came from the server. This method sends those
     * identifiers back to the MarketBillingService, which ACKs them to the
     * server. Returns false if there was an error trying to connect to the
     * MarketBillingService.
     * @param startId an identifier for the invocation instance of this service
     * @param notifyIds a list of opaque identifiers associated with purchase
     * state changes.
     * @return false if there was an error connecting to Market
     */
    private boolean confirmNotifications(int startId, String[] notifyIds) {
        return new ConfirmNotifications(this, startId, notifyIds).runRequest();
    }

    /**
     * Gets the purchase information. This message includes a list of
     * notification IDs sent to us by Android Market, which we include in
     * our request. The server responds with the purchase information,
     * encoded as a JSON string, and sends that to the {@link BillingReceiver}
     * in an intent with the action {@link Consts#ACTION_PURCHASE_STATE_CHANGED}.
     * Returns false if there was an error trying to connect to the MarketBillingService.
     *
     * @param startId an identifier for the invocation instance of this service
     * @param notifyIds a list of opaque identifiers associated with purchase
     * state changes
     * @return false if there was an error connecting to Android Market
     */
    private boolean getPurchaseInformation(int startId, String[] notifyIds) {
        return new GetPurchaseInformation(this, startId, notifyIds).runRequest();
    }

    /**
     * Verifies that the data was signed with the given signature, and calls
     * {@link ResponseHandler#purchaseResponse(Context, PurchaseState, String, String, long)}
     * for each verified purchase.
     * @param startId an identifier for the invocation instance of this service
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature the signature for the data, signed with the private key
     */
    private void purchaseStateChanged(int startId, String signedData, String signature) {
        ArrayList<Security.VerifiedPurchase> purchases;
        purchases = Security.verifyPurchase(signedData, signature);
        if (purchases == null) {
            return;
        }

        ArrayList<String> notifyList = new ArrayList<String>();
        for (VerifiedPurchase vp : purchases) {
            if (vp.notificationId != null) {
                notifyList.add(vp.notificationId);
            }
            ResponseHandler.purchaseResponse(this, vp.purchaseState, vp.productId,
                    vp.orderId, vp.purchaseTime, vp.developerPayload);
        }
        if (!notifyList.isEmpty()) {
            String[] notifyIds = notifyList.toArray(new String[notifyList.size()]);
            confirmNotifications(startId, notifyIds);
        }
    }

    /**
     * This is called when we receive a response code from Android Market for a request
     * that we made. This is used for reporting various errors and for
     * acknowledging that an order was sent to the server. This is NOT used
     * for any purchase state changes.  All purchase state changes are received
     * in the {@link BillingReceiver} and passed to this service, where they are
     * handled in {@link #purchaseStateChanged(int, String, String)}.
     * @param requestId a number that identifies a request, assigned at the
     * time the request was made to Android Market
     * @param responseCode a response code from Android Market to indicate the state
     * of the request
     */
    private void checkResponseCode(long requestId, ResponseCode responseCode) {
        BillingRequest request = mSentRequests.get(requestId);
        if (request != null) {
            if (BillingLogger.isDebugEnabled()) {
                BillingLogger.debug(TAG, request.getClass().getSimpleName() + ": " + responseCode);
            }
            request.responseCodeReceived(responseCode);
        }
        mSentRequests.remove(requestId);
    }

    /**
     * Runs any pending requests that are waiting for a connection to the
     * service to be established.  This runs in the main UI thread.
     */
    private void runPendingRequests() {
        int maxStartId = -1;
        BillingRequest request;
        while ((request = mPendingRequests.peek()) != null) {
            if (request.runIfConnected()) {
                // Remove the request
                mPendingRequests.remove();

                // Remember the largest startId, which is the most recent
                // request to start this service.
                if (maxStartId < request.getStartId()) {
                    maxStartId = request.getStartId();
                }
            } else {
                // The service crashed, so restart it. Note that this leaves
                // the current request on the queue.
                bindToMarketBillingService();
                return;
            }
        }

        // If we get here then all the requests ran successfully.  If maxStartId
        // is not -1, then one of the requests started the service, so we can
        // stop it now.
        if (maxStartId >= 0) {
            if (BillingLogger.isDebugEnabled()) {
                BillingLogger.info(TAG, "stopping service, startId: " + maxStartId);
            }
            stopSelf(maxStartId);
        }
    }

    /**
     * This is called when we are connected to the MarketBillingService.
     * This runs in the main UI thread.
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (BillingLogger.isDebugEnabled()) {
            BillingLogger.debug(TAG, "Billing service connected");
        }
        mService = IMarketBillingService.Stub.asInterface(service);
        runPendingRequests();
    }

    /**
     * This is called when we are disconnected from the MarketBillingService.
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.w(TAG, "Billing service disconnected");
        mService = null;
    }

    /**
     * Unbinds from the MarketBillingService. Call this when the application
     * terminates to avoid leaking a ServiceConnection.
     */
    public void unbind() {
        try {
            unbindService(this);
        } catch (IllegalArgumentException e) {
            // This might happen if the service was disconnected
        }
    }
}
