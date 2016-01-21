package quartzo.com.dublinbusalarm;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import adapters.ListAlarmsAdapter;
import adapters.ListBusAdapter;
import entities.Alarm;
import entities.Bus;
import utils.AlarmPersistence;
import utils.Constants;

public class MainActivity extends AppCompatActivity {

    ProgressDialog mProgressDialog;
    private Context mContext;

    private RecyclerView listBuses;

    private RecyclerView listAlarms;

    private TextView txtBusStop;

    private TextView txtBusNumber;

    AlertDialog listDialog;

    private LocalTime timeBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //txttitle = (TextView) findViewById(R.id.txtLastBus);

        //txttitle.setText(readDataShared());

        mContext = this;

        //pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, alarmIntent, 0);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                AlertDialog.Builder builder =
                        new AlertDialog.Builder(mContext, R.style.AppCompatAlertDialogStyle);
                builder.setTitle("Almost there");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Dialog f = (Dialog) dialog;

                        txtBusStop = (TextView) f.findViewById(R.id.editTextBusStop);

                        txtBusNumber = (TextView) f.findViewById(R.id.editTextBusNumber);

                        new LastBusAsync().execute(txtBusStop.getText().toString(), txtBusNumber.getText().toString());

                    }
                });
                builder.setNegativeButton("Cancel", null);
                builder.setView(R.layout.customdialogform);


                builder.show();
            }
        });

        final LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(GridLayoutManager.VERTICAL);

        final LinearLayoutManager layoutManager1 = new LinearLayoutManager(mContext);
        layoutManager1.setOrientation(LinearLayoutManager.VERTICAL);

        listBuses = new RecyclerView(mContext);

        //listBuses.setOnItemClickListener(evtClickBus);

        listAlarms = (RecyclerView) findViewById(R.id.listViewAlarms);

        listBuses.setLayoutManager(layoutManager);

        listAlarms.setLayoutManager(layoutManager1);

        listAlarms.setHasFixedSize(true);

        listBuses.setHasFixedSize(true);

        updateListViewAlarms();

        AlertDialog.Builder builderListDialog;

        builderListDialog =
                new AlertDialog.Builder(mContext, R.style.AppCompatAlertDialogStyle);

        builderListDialog.setTitle("List bus");

        builderListDialog.setNegativeButton("Cancel", null);

        listDialog = builderListDialog.create();


    }

    private void updateListViewAlarms(){

        List<Alarm> alarms = AlarmPersistence.readStoredAlarms(mContext);

        if(alarms != null) {
            listAlarms.setAdapter(new ListAlarmsAdapter(mContext, alarms, evtSwitcherAlarm));
        }
    }

    private View.OnClickListener evtSwitcherAlarm = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //Dialog f = (Dialog) dialog;

            //txtBusStop = (TextView) f.findViewById(R.id.editTextBusStop);

            //txtBusNumber = (TextView) f.findViewById(R.id.editTextBusNumber);
            Switch swt = (Switch) v;

            if(swt.isChecked()) {
                Alarm alarm = (Alarm) v.getTag();

                new LastBusAsync().execute(alarm.getBusStop(), alarm.getBus());
            }else{
                AlarmManager manager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

                //Alarm alarm =  alarms.get(taggedPosition);
                Alarm alarm = (Alarm) v.getTag();

                Intent alarmIntent = new Intent("EXECUTE_ALARM_BUS");

                manager.cancel(PendingIntent.getBroadcast(mContext, alarm.getId(), alarmIntent, PendingIntent.FLAG_ONE_SHOT));

                alarm.setIsActive(false);

                AlarmPersistence.saveAlarm(alarm, mContext);

                Toast toast = Toast.makeText(mContext, "Deleted " + alarm.toString(), Toast.LENGTH_SHORT);
                toast.show();

                //updateListViewAlarms();
            }
        }
    };

    private AdapterView.OnClickListener evtClickBus = new AdapterView.OnClickListener() {

        @Override
        public void onClick(final View view) {

//            Bus bus = (Bus) listBuses.getAdapter().getItem(position);
            final Bus bus = (Bus) view.getTag();

            listDialog.cancel();

            AlertDialog.Builder builder =
                    new AlertDialog.Builder(mContext, R.style.AppCompatAlertDialogStyle);

            builder.setTitle("Set alert options");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    Dialog f = (Dialog) dialog;

                    NumberPicker number = (NumberPicker) f.findViewById(R.id.numberPicker2);

                    AlarmManager manager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

                    DateTime date = new DateTime();

                    LocalTime busTime = timeBus.minusHours(LocalTime.now().getHourOfDay()).minusMinutes(LocalTime.now().getMinuteOfHour()).minusSeconds(LocalTime.now().getSecondOfMinute());

                    int diff = busTime.getMinuteOfHour() - number.getValue();

                    date = date.plusMinutes(diff);
                    //date = date.plusSeconds(10);
                    //TODO Check it out another way to control the id
                    int alarmId = LocalTime.now().getMillisOfDay();

                    //Alarm myData = new Alarm(alarmId ,date.toString(), txtBusNumber.getText().toString(), txtBusStop.getText().toString(), String.valueOf(number.getValue()), true);
                    Alarm myData = new Alarm(alarmId ,date.toString(), txtBusNumber.getText().toString(), txtBusStop.getText().toString(), String.valueOf(number.getValue()), true);
                    Intent alarmIntent = new Intent("EXECUTE_ALARM_BUS");

                    alarmIntent.putExtra("myDataSerialized", myData.serialize());

                    PendingIntent appIntent = PendingIntent.getBroadcast(mContext, alarmId, alarmIntent, PendingIntent.FLAG_ONE_SHOT);

                    manager.set(AlarmManager.RTC_WAKEUP, date.getMillis(), appIntent);

                    AlarmPersistence.saveAlarm(myData, mContext);

                    updateListViewAlarms();

                }
            });
            builder.setNegativeButton("Cancel", null);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.customdialogtimepicker, (ViewGroup) view, false);

            NumberPicker time = (NumberPicker) v.findViewById(R.id.numberPicker2);

            timeBus = LocalTime.parse(bus.getTime());

            LocalTime difference = timeBus.minusHours(LocalTime.now().getHourOfDay()).minusMinutes(LocalTime.now().getMinuteOfHour()).minusSeconds(LocalTime.now().getSecondOfMinute());

            int val = difference.getMinuteOfHour();

            time.setMinValue(1);
            time.setMaxValue(val - 1);

            time.setValue(val - 1);

            builder.setView(v);

            builder.show();

        }
    };

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

    public class LastBusAsync extends AsyncTask<String, List<Bus>, List<Bus>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setTitle("Dublin Bus");
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected List<Bus> doInBackground(String... params) {

            List<Bus> buses = new ArrayList<>();

            try {

                // Connect to the web site
                Document document = Jsoup.connect(Constants.URL_DUBLIN_BUS + params[0]).get();
                //Document document = Jsoup.connect(Constants.URL_DUBLIN_BUS).get();


                // Get the html document title
                Iterator<Element> table = document.select("table[id=rtpi-results]").select("tr:contains(" + params[1] + " )").iterator();

                if(table != null) {

                    //Iterator<Element> ite = table.select("tr:contains(" + params[1] + " )").iterator();

                    while (table.hasNext()) {
                        Element ele = table.next();
                        String curr = ele.select("td").get(Constants.DESTINATION).text();


                        if(!ele.select("td").get(Constants.TIME).text().equalsIgnoreCase("Due")) {

                            Bus newBus = new Bus();

                            newBus.setTime(ele.select("td").get(Constants.TIME).text());
                            newBus.setRoute(ele.select("td").get(Constants.ROUTE).text());
                            newBus.setDestination(ele.select("td").get(Constants.DESTINATION).text());


                            buses.add(newBus);
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


            return buses;
        }

        @Override
        protected void onPostExecute(List<Bus> result) {

            mProgressDialog.dismiss();

            listBuses.setAdapter(new ListBusAdapter(result, mContext, listDialog, evtClickBus));

            listDialog.setView(listBuses);

            listDialog.show();

        }
    }
}
