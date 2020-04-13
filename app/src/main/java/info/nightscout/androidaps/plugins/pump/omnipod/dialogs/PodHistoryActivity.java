package info.nightscout.androidaps.plugins.pump.omnipod.dialogs;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.activities.NoSplashAppCompatActivity;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.common.data.TempBasalPair;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpHistoryEntryGroup;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType;
import info.nightscout.androidaps.plugins.pump.common.utils.ProfileUtil;
import info.nightscout.androidaps.plugins.pump.omnipod.driver.db.PodHistory;
import info.nightscout.androidaps.plugins.pump.omnipod.util.OmnipodUtil;

public class PodHistoryActivity extends NoSplashAppCompatActivity {

    private static Logger LOG = LoggerFactory.getLogger(L.PUMP);

    Spinner historyTypeSpinner;
    TextView statusView;
    RecyclerView recyclerView;
    LinearLayoutManager llm;

    static TypeList showingType = null;
    static PumpHistoryEntryGroup selectedGroup = PumpHistoryEntryGroup.All;
    List<PodHistory> fullHistoryList = new ArrayList<>();
    List<PodHistory> filteredHistoryList = new ArrayList<>();

    RecyclerViewAdapter recyclerViewAdapter;
    boolean manualChange = false;

    List<TypeList> typeListFull;


    public PodHistoryActivity() {
        super();
    }


    private void prepareData() {
        GregorianCalendar gc = new GregorianCalendar();
        gc.add(Calendar.HOUR_OF_DAY, -24);

        MainApp.getDbHelper().getPodHistoryFromTime(gc.getTimeInMillis(), false);

        fullHistoryList.addAll(MainApp.getDbHelper().getPodHistoryFromTime(gc.getTimeInMillis(), true));
    }


    private void filterHistory(PumpHistoryEntryGroup group) {

        this.filteredHistoryList.clear();

        LOG.debug("Items on full list: {}", fullHistoryList.size());

        if (group == PumpHistoryEntryGroup.All) {
            this.filteredHistoryList.addAll(fullHistoryList);
        } else {
            for (PodHistory pumpHistoryEntry : fullHistoryList) {
                if (pumpHistoryEntry.getPodDbEntryType().getGroup() == group) {
                    this.filteredHistoryList.add(pumpHistoryEntry);
                }
            }
        }

        if (this.recyclerViewAdapter != null) {
            this.recyclerViewAdapter.setHistoryList(this.filteredHistoryList);
            this.recyclerViewAdapter.notifyDataSetChanged();
        }

        LOG.debug("Items on filtered list: {}", filteredHistoryList.size());
    }


    @Override
    protected void onResume() {
        super.onResume();
        filterHistory(selectedGroup);
        setHistoryTypeSpinner();
    }


    private void setHistoryTypeSpinner() {
        this.manualChange = true;

        for (int i = 0; i < typeListFull.size(); i++) {
            if (typeListFull.get(i).entryGroup == selectedGroup) {
                historyTypeSpinner.setSelection(i);
                break;
            }
        }

        SystemClock.sleep(200);
        this.manualChange = false;
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.omnipod_pod_history_activity);

        historyTypeSpinner = (Spinner) findViewById(R.id.omnipod_historytype);
        statusView = (TextView) findViewById(R.id.omnipod_historystatus);
        recyclerView = (RecyclerView) findViewById(R.id.omnipod_history_recyclerview);

        recyclerView.setHasFixedSize(true);
        llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);

        prepareData();

        recyclerViewAdapter = new RecyclerViewAdapter(filteredHistoryList);
        recyclerView.setAdapter(recyclerViewAdapter);

        statusView.setVisibility(View.GONE);

        typeListFull = getTypeList(PumpHistoryEntryGroup.getList());

        ArrayAdapter<TypeList> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_centered, typeListFull);
        historyTypeSpinner.setAdapter(spinnerAdapter);

        historyTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (manualChange)
                    return;
                TypeList selected = (TypeList) historyTypeSpinner.getSelectedItem();
                showingType = selected;
                selectedGroup = selected.entryGroup;
                filterHistory(selectedGroup);
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (manualChange)
                    return;
                filterHistory(PumpHistoryEntryGroup.All);
            }
        });

    }


    private List<TypeList> getTypeList(List<PumpHistoryEntryGroup> list) {

        ArrayList<TypeList> typeList = new ArrayList<>();

        for (PumpHistoryEntryGroup pumpHistoryEntryGroup : list) {
            typeList.add(new TypeList(pumpHistoryEntryGroup));
        }

        return typeList;
    }

    public static class TypeList {

        PumpHistoryEntryGroup entryGroup;
        String name;


        TypeList(PumpHistoryEntryGroup entryGroup) {
            this.entryGroup = entryGroup;
            this.name = entryGroup.getTranslated();
        }


        @Override
        public String toString() {
            return name;
        }
    }

    public static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.HistoryViewHolder> {

        List<PodHistory> historyList;


        RecyclerViewAdapter(List<PodHistory> historyList) {
            this.historyList = historyList;
        }


        public void setHistoryList(List<PodHistory> historyList) {
            // this.historyList.clear();
            // this.historyList.addAll(historyList);

            this.historyList = historyList;

            Collections.sort(this.historyList);

            // this.notifyDataSetChanged();
        }


        @Override
        public HistoryViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.omnipod_pod_history_item, //
                    viewGroup, false);
            return new HistoryViewHolder(v);
        }


        @Override
        public void onBindViewHolder(HistoryViewHolder holder, int position) {
            PodHistory record = historyList.get(position);

            if (record != null) {
                holder.timeView.setText(record.getDateTimeString());
                holder.typeView.setText(record.getPodDbEntryType().getResourceId());
                setValue(record, holder.valueView);
            }
        }


        private void setValue(PodHistory historyEntry, TextView valueView) {
            //valueView.setText("");

            if (historyEntry.isSuccess()) {
                switch (historyEntry.getPodDbEntryType()) {

                    case SetTemporaryBasal: {
                        TempBasalPair tempBasalPair = OmnipodUtil.getGsonInstance().fromJson(historyEntry.getData(), TempBasalPair.class);
                        valueView.setText(MainApp.gs(R.string.omnipod_cmd_tbr_value, tempBasalPair.getInsulinRate(), tempBasalPair.getDurationMinutes()));
                    }
                    break;

                    case FillCannulaSetBasalProfile:
                    case SetBasalSchedule: {
                        if (historyEntry.getData() != null) {
                            setProfileValue(historyEntry.getData(), valueView);
                        }
                    }
                    break;

                    case SetBolus: {
                        if (historyEntry.getData().contains(";")) {
                            String[] splitVal = historyEntry.getData().split(";");
                            valueView.setText(MainApp.gs(R.string.omnipod_cmd_bolus_value_with_carbs, Double.valueOf(splitVal[0]), Double.valueOf(splitVal[1])));
                        } else {
                            valueView.setText(MainApp.gs(R.string.omnipod_cmd_bolus_value, Double.valueOf(historyEntry.getData())));
                        }
                    }
                    break;

                    case GetPodStatus:
                    case GetPodInfo:
                    case SetTime:
                    case PairAndPrime:
                    case CancelTemporaryBasal:
                    case CancelTemporaryBasalForce:
                    case ConfigureAlerts:
                    case CancelBolus:
                    case DeactivatePod:
                    case ResetPodState:
                    case AcknowledgeAlerts:
                    case SuspendDelivery:
                    case ResumeDelivery:
                    case UnknownEntryType:
                    default:
                        valueView.setText("");
                        break;

                }
            } else {
                valueView.setText(historyEntry.getData());
            }

        }

        private void setProfileValue(String data, TextView valueView) {
            LOG.debug("Profile json:\n" + data);

            try {
                Profile.ProfileValue[] profileValuesArray = OmnipodUtil.getGsonInstance().fromJson(data, Profile.ProfileValue[].class);

                //profile = new Profile(new JSONObject(data), Constants.MGDL);
                valueView.setText(ProfileUtil.getBasalProfilesDisplayable(profileValuesArray, PumpType.Insulet_Omnipod));
            } catch (Exception e) {
                LOG.error("Problem parsing Profile json. Ex: {}, Data:\n{}", e.getMessage(), data);
                valueView.setText("");
            }
            //Profile profile = OmnipodUtil.getGsonInstance().fromJson(data, Profile.class);

        }


        @Override
        public int getItemCount() {
            return historyList.size();
        }


        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }


        static class HistoryViewHolder extends RecyclerView.ViewHolder {

            TextView timeView;
            TextView typeView;
            TextView valueView;

            HistoryViewHolder(View itemView) {
                super(itemView);
                timeView = itemView.findViewById(R.id.omnipod_history_time);
                typeView = itemView.findViewById(R.id.omnipod_history_source);
                valueView = itemView.findViewById(R.id.omnipod_history_description);
            }
        }
    }

}
