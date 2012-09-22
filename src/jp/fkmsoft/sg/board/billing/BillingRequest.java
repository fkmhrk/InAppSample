package jp.fkmsoft.sg.board.billing;

import android.os.Bundle;
import android.os.RemoteException;

/**
 * The base class for all requests that use the MarketBillingService.
 * Each derived class overrides the run() method to call the appropriate
 * service interface.  If we are already connected to the MarketBillingService,
 * then we call the run() method directly. Otherwise, we bind
 * to the service and save the request on a queue to be run later when
 * the service is connected.
 */
public abstract class BillingRequest {
    private static final String TAG = "Billing Request";
    public interface Field {
        // These are the names of the fields in the request bundle.
        public static final String BILLING_REQUEST_METHOD = "BILLING_REQUEST";
        public static final String BILLING_REQUEST_API_VERSION = "API_VERSION";
        public static final String BILLING_REQUEST_PACKAGE_NAME = "PACKAGE_NAME";
        public static final String BILLING_REQUEST_ITEM_ID = "ITEM_ID";
        public static final String BILLING_REQUEST_ITEM_TYPE = "ITEM_TYPE";
        public static final String BILLING_REQUEST_DEVELOPER_PAYLOAD = "DEVELOPER_PAYLOAD";
        public static final String BILLING_REQUEST_NOTIFY_IDS = "NOTIFY_IDS";
        public static final String BILLING_REQUEST_NONCE = "NONCE";

        public static final String BILLING_RESPONSE_RESPONSE_CODE = "RESPONSE_CODE";
        public static final String BILLING_RESPONSE_PURCHASE_INTENT = "PURCHASE_INTENT";
        public static final String BILLING_RESPONSE_REQUEST_ID = "REQUEST_ID";
        public static long BILLING_RESPONSE_INVALID_REQUEST_ID = -1;
    }
    
    protected BillingService mBillingService;
    private final int mStartId;
    protected long mRequestId;

    public BillingRequest(BillingService service, int startId) {
        this.mBillingService = service;
        mStartId = startId;
    }

    public int getStartId() {
        return mStartId;
    }

    /**
     * Run the request, starting the connection if necessary.
     * @return true if the request was executed or queued; false if there
     * was an error starting the connection
     */
    public boolean runRequest() {
        if (runIfConnected()) {
            return true;
        }

        if (mBillingService.bindToMarketBillingService()) {
            // Add a pending request to run when the service is connected.
            mBillingService.addPendingRequest(this);
            return true;
        }
        return false;
    }

    /**
     * Try running the request directly if the service is already connected.
     * @return true if the request ran successfully; false if the service
     * is not connected or there was an error when trying to use it
     */
    public boolean runIfConnected() {
        if (BillingLogger.isDebugEnabled()) {
            BillingLogger.debug(TAG, getClass().getSimpleName());
        }
        if (!mBillingService.isConnected()) {
            return false;
        }
        try {
            mRequestId = run();
            if (BillingLogger.isDebugEnabled()) {
                BillingLogger.debug(TAG, "request id: " + mRequestId);
            }
            if (mRequestId >= 0) {
                mBillingService.addSentRequest(mRequestId, this);
            }
            return true;
        } catch (RemoteException e) {
            onRemoteException(e);
            return false;
        }
    }

    /**
     * Called when a remote exception occurs while trying to execute the
     * {@link #run()} method.  The derived class can override this to
     * execute exception-handling code.
     * @param e the exception
     */
    protected void onRemoteException(RemoteException e) {
        BillingLogger.warn(TAG, "remote billing service crashed");
        mBillingService.closeMarketService();
    }

    /**
     * The derived class must implement this method.
     * @throws RemoteException
     */
    abstract protected long run() throws RemoteException;

    /**
     * This is called when Android Market sends a response code for this
     * request.
     * @param responseCode the response code
     */
    protected void responseCodeReceived(ResponseCode responseCode) {
    }

    protected Bundle makeRequestBundle(String method) {
        Bundle request = new Bundle();
        request.putString(Field.BILLING_REQUEST_METHOD, method);
        request.putInt(Field.BILLING_REQUEST_API_VERSION, 2);
        request.putString(Field.BILLING_REQUEST_PACKAGE_NAME, mBillingService.getPackageName());
        return request;
    }

    protected void logResponseCode(String method, Bundle response) {
        ResponseCode responseCode = ResponseCode.valueOf(
                response.getInt(Field.BILLING_RESPONSE_RESPONSE_CODE));
        if (BillingLogger.isDebugEnabled()) {
            BillingLogger.error(TAG, method + " received " + responseCode.toString());
        }
    }
}