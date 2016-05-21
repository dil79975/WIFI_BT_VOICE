/**
 * 
 */
package adhoc.voip;


import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

// TODO: Auto-generated Javadoc
/**
 * The Class PopupsManager.
 *
 * @author Raz and Elad
 */
public class PopupsManager {

	/** The talk dialog. */
	public Dialog talkDialog;	
	
	/** The enable wifi dialog. */
	public Dialog enableWifiDialog;


	/**
	 * Instantiates a new popups manager.
	 *
	 * @param context the application context
	 */
	public PopupsManager(final Context context) {

		LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View menuLayout = li.inflate(R.layout.talk_dialog, null,false);
		talkDialog = new Dialog(context);
		talkDialog.setTitle("Talking...");
		talkDialog.setContentView(menuLayout);
		talkDialog.setCanceledOnTouchOutside(false);
		talkDialog.setCancelable(false);

		menuLayout = li.inflate(R.layout.enable_wifi_direct_dialog, null,false);
		enableWifiDialog = new Dialog(context);
		enableWifiDialog.setTitle("Wifi Direct is disabled");
		enableWifiDialog.setContentView(menuLayout);
		enableWifiDialog.setCanceledOnTouchOutside(false);
		enableWifiDialog.setCancelable(false);
	}

}
