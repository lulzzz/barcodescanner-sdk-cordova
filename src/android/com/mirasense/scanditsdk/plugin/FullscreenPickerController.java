package com.mirasense.scanditsdk.plugin;


import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import com.scandit.barcodepicker.ScanSettings;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class FullscreenPickerController extends PickerControllerBase implements ResultRelay.Callback {
    FullscreenPickerController(CordovaPlugin plugin, CallbackContext callbacks) {
        super(plugin, callbacks);
    }

    @Override
    public void show(JSONObject settings, Bundle options, Bundle overlayOptions, boolean legacyMode) {
        int flags = mPlugin.cordova.getActivity().getWindow().getAttributes().flags;
        if ((flags & WindowManager.LayoutParams.FLAG_SECURE) != 0) {
            options.putBoolean("secure", true);
        }
        mContinuousMode = PhonegapParamParser.shouldRunInContinuousMode(options);
        // The activity sends the result to the result relay, which will then invoke onRelayedResult.
        ResultRelay.setCallback(this);
        Intent intent = new Intent(mPlugin.cordova.getActivity(), FullScreenPickerActivity.class);
        if (settings != null) {
            intent.putExtra("settings", settings.toString());
        }
        intent.putExtra("options", options);
        if (overlayOptions != null) {
            intent.putExtra("overlayOptions", overlayOptions);
        }
        mPlugin.cordova.startActivityForResult(mPlugin, intent, 1);
    }

    @Override
    public void setState(int state) {
        FullScreenPickerActivity.setState(state);
    }

    @Override
    public void applyScanSettings(ScanSettings scanSettings) {
        FullScreenPickerActivity.applyScanSettings(scanSettings);
    }

    @Override
    public void updateUI(Bundle overlayOptions) {
        FullScreenPickerActivity.updateUI(overlayOptions);
    }

    @Override
    public void setTorchEnabled(boolean enabled) {
        FullScreenPickerActivity.setTorchEnabled(enabled);
    }

    @Override
    public void close() {
        FullScreenPickerActivity.close();
    }

    @Override
    public void onActivityPause() {
        // nothing to be done. The full screen picker activity has its own pause/resume events.
    }

    @Override
    public void onActivityResume() {
        // nothing to be done. The full screen picker activity has its own pause/resume events.
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == FullScreenPickerActivity.SCAN ||
            resultCode == FullScreenPickerActivity.MANUAL) {
            PluginResult result = resultForBundle(data.getExtras());
            sendPluginResultBlocking(result);
        } else if (resultCode == FullScreenPickerActivity.CANCEL) {
            mCallbackContext.sendPluginResult(Marshal.createCancel());
        }
    }

    @Override
    public int onRelayedResult(Bundle bundle) {
        PluginResult result = resultForBundle(bundle);
        if (bundle.getBoolean("waitForResult", true)) {
            return sendPluginResultBlocking(result);
        }
        mCallbackContext.sendPluginResult(resultForBundle(bundle));
        return 0;

    }

    PluginResult createOkResult(JSONArray args) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, args);
        result.setKeepCallback(true);
        return result;
    }

    private PluginResult resultForBundle(Bundle bundle) {
        PluginResult result;
        if (bundle.containsKey("jsonString")) {
            String jsonString = bundle.getString("jsonString");
            try {
                JSONArray json = new JSONArray(jsonString);
                return createOkResult(json);
            } catch (JSONException e) {
                e.printStackTrace();
                result = new PluginResult(PluginResult.Status.OK, "error");
            }
        } else {
            assert mLegacyMode;
            // Legacy
            String barcode = bundle.getString("barcode");
            String symbology = bundle.getString("symbology");
            JSONArray args = new JSONArray();
            args.put(barcode);
            args.put(symbology);
            return createOkResult(args);
        }
        return result;
    }
}