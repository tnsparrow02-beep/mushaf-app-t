package __PKG__;

import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "MediaCtl")
public class MediaCtlPlugin extends Plugin {
    static MediaCtlPlugin inst;

    @Override
    public void load() {
        inst = this;
    }

    @PluginMethod
    public void update(PluginCall call) {
        Intent i = new Intent(getContext(), MediaService.class);
        i.putExtra("title", call.getString("title", ""));
        i.putExtra("text", call.getString("text", ""));
        i.putExtra("playing", Boolean.TRUE.equals(call.getBoolean("playing", false)));
        try {
            ContextCompat.startForegroundService(getContext(), i);
        } catch (Exception e) {
            try {
                getContext().startService(i);
            } catch (Exception e2) {
                // ignore: app not allowed to start the service right now
            }
        }
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        try {
            getContext().stopService(new Intent(getContext(), MediaService.class));
        } catch (Exception e) {
            // ignore
        }
        call.resolve();
    }

    static void emit(String action) {
        if (inst != null) {
            JSObject o = new JSObject();
            o.put("action", action);
            inst.notifyListeners("action", o);
        }
    }
}
