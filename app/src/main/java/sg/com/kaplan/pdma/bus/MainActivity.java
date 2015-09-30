package sg.com.kaplan.pdma.bus;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    //please obtain your account key from http://www.mytransport.sg/content/mytransport/home/dataMall.html 
    private final String AccountKey = "";
    private final String UniqueUserID = "";

    private Context context = this;
    EditText editText;
    TextView textView;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Toast.makeText(context, "Not connected to Internet", Toast.LENGTH_SHORT).show();
            return;
        }
        //Log.v("Bus", "connection: "+ isConnected);

        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String busStopNumberText = v.getText().toString().trim();
                String regexStr = "^[0-9]*$";
                if (busStopNumberText.matches(regexStr) && busStopNumberText.length() > 0) {
                    progressBar.setVisibility(View.VISIBLE);
                    textView.setText(busStopNumberText);
                    String urlString = "http://datamall2.mytransport.sg/ltaodataservice/BusArrival?BusStopID=" + busStopNumberText;
                    new HttpAsyncTask().execute(urlString);
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(context, "Invalid bus stop number", Toast.LENGTH_SHORT).show();
                }

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return GET(urls[0]);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_SHORT).show();
            try {
                JSONObject jsonRootObject = new JSONObject(result);
                JSONArray jsonArray = jsonRootObject.optJSONArray("Services");
                String output = "";
                Date now = new Date();
                //output += "\nNow: " + getArrivalTimeInMins(now.toString());
                for(int i=0; i < jsonArray.length(); i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String serviceNumber = jsonObject.getString("ServiceNo");
                    String status = jsonObject.getString("Status");
                    JSONObject nextBus = jsonObject.getJSONObject("NextBus");
                    String estimatedArrivalString = nextBus.getString("EstimatedArrival");
                    String estimatedArrival = getArrivalTimeInMins(estimatedArrivalString);
                    output += "\n" + serviceNumber + " : " + estimatedArrival;
                    JSONObject subsequentBus = jsonObject.getJSONObject("SubsequentBus");
                    estimatedArrivalString = subsequentBus.getString("EstimatedArrival");
                    estimatedArrival = getArrivalTimeInMins(estimatedArrivalString);
                    output += ", " + estimatedArrival;
                }
                textView.setText(output);
            } catch (JSONException e) {
                e.printStackTrace();
                textView.setText(e.toString());
            }

            progressBar.setVisibility(View.GONE);
        }
    }

    public String GET(String urlString) {
        InputStream inputStream = null;
        String result = "";
        try {
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("AccountKey", AccountKey);
            con.setRequestProperty("UniqueUserID", UniqueUserID);
            result = readStream(con.getInputStream());
        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private String readStream(InputStream in) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in));) {

            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String getArrivalTimeInMins(String dateString) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date value = null;
        try {
            value = formatter.parse(dateString);
            Date current = new Date();
            long diff = value.getTime() - current.getTime();

//            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy hh:mmaa");
//            dateFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));
//            String dt = dateFormatter.format(value);

            int min = (int) diff/1000/60;


            return min < 1 ? "Arr": (min +"m");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}
