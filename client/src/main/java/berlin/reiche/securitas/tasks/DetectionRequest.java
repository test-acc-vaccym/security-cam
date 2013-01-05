package berlin.reiche.securitas.tasks;

import static berlin.reiche.securitas.tasks.DetectionRequest.DetectionCommand.START;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import berlin.reiche.securitas.Client;
import berlin.reiche.securitas.MainActivity;
import berlin.reiche.securitas.util.HttpUtilities;

import com.google.android.gcm.GCMRegistrar;

public class DetectionRequest extends AsyncTask<String, Void, HttpResponse> {

	public enum DetectionCommand {
		START, STOP
	};

	IOException exception;

	Activity activity;

	DetectionCommand command;

	private static String TAG = DetectionRequest.class.getSimpleName();

	public DetectionRequest(Activity activity, DetectionCommand command) {
		this.activity = activity;
		this.command = command;
	}

	@Override
	protected HttpResponse doInBackground(String... uri) {

		HttpClient client = HttpUtilities.newHttpClient();
		HttpGet get = new HttpGet(uri[0]);
		HttpUtilities.setAuthorization(get, Client.getSettings());

		try {
			return client.execute(get);
		} catch (IOException e) {
			exception = e;
		} finally {
			HttpUtilities.closeClient(client);
		}
		return null;
	}

	protected void onPostExecute(HttpResponse response) {

		MainActivity activity = (MainActivity) this.activity;
		if (exception != null && response == null) {
			activity.errors.setText(exception.getMessage());
		} else if (response == null) {
			Log.e(TAG, "Response is null without an exception. "
					+ "The endpoint probably ran into a problem.");
		} else {
			switch (response.getStatusLine().getStatusCode()) {
			case SC_OK:
				Client.toggleDetectionActive();
				activity.toggleButtonText();
				if (command == START) {
					Client.downloadLatestSnapshot(activity, activity.snapshot);
					return;
				} else {
					activity.unlockUI();
					activity.snapshot.setVisibility(ImageView.INVISIBLE);
					return;
				}
			case SC_UNAUTHORIZED:
				activity.errors.setText("Unauthorized request, check "
						+ "authentication data");
				break;
			case SC_CONFLICT:
				GCMRegistrar.setRegisteredOnServer(activity, false);
				activity.manageDeviceRegistration();
				Client.toggleMotionDetection(activity);
				return;
			default:
				StatusLine status = response.getStatusLine();
				activity.errors.setText(String.valueOf(status.getStatusCode())
						+ " " + status.getReasonPhrase());
			}
		}
		activity.unlockUI();
	}
}