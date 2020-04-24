package edu.uw.covidsafe.contact_trace;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.covidsafe.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.covidsafe.comms.NetworkHelper;
import edu.uw.covidsafe.gps.GpsRecord;
import edu.uw.covidsafe.symptoms.SymptomsOpsAsyncTask;
import edu.uw.covidsafe.symptoms.SymptomsRecord;
import edu.uw.covidsafe.ui.notif.NotifRecord;
import edu.uw.covidsafe.ui.notif.NotifRecyclerViewAdapter;
import edu.uw.covidsafe.utils.Constants;
import edu.uw.covidsafe.utils.CryptoUtils;
import edu.uw.covidsafe.utils.TimeUtils;
import edu.uw.covidsafe.utils.Utils;

public class GpsHistoryRecyclerViewAdapter2 extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private Activity av;
    View view;
    List<GpsRecord> records = new LinkedList<>();

    public GpsHistoryRecyclerViewAdapter2(Context mContext, Activity av, View view) {
        this.mContext = mContext;
        this.av = av;
        this.view = view;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view ;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_gps_history, parent, false);
        return new GpsHistoryHolder(view);
    }

    String currentDate = "";
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GpsRecord record = this.records.get(position);
        String address = record.getRawAddress();
        if (address == null || address.isEmpty()) {
            Geocoder gc = new Geocoder(mContext);
            if (gc.isPresent()) {
                try {
                    List<Address> addresses = gc.getFromLocation(record.getLat(mContext),
                            record.getLongi(mContext), 1);
                    address = addresses.get(0).getAddressLine(0);
                } catch (IOException e) {
                    Log.e("err",e.getMessage());
                    ((GpsHistoryHolder)holder).messageView.setText("Unknown address");
                }
            }
        }

        SimpleDateFormat format = new SimpleDateFormat("h:mm aa");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd");
        String time = format.format(record.getTs());
        String date = dateFormat.format(record.getTs());
        if (!date.equals(currentDate)) {
            ((GpsHistoryHolder)holder).date.setVisibility(View.VISIBLE);
            ((GpsHistoryHolder)holder).date.setText(date);
            currentDate = date;
        }
        else {
            ((GpsHistoryHolder)holder).date.setVisibility(View.GONE);
        }

        ((GpsHistoryHolder)holder).messageView.setText(address+"\n"+time);

    }

    public void setRecords(List<GpsRecord> records, Context cxt) {
        Log.e("gpsset","set records");
        List<String> lats = new LinkedList<>();
        List<String> lons = new LinkedList<>();
        List<String> addresses = new LinkedList<>();
        List<Long> ts = new LinkedList<>();
        for(GpsRecord record : records) {
            lats.add(record.getRawLat());
            lons.add(record.getRawLongi());
            addresses.add(record.getRawAddress());
            ts.add(record.getTs());
        }

        String[] decryptedLats = CryptoUtils.decryptBatch(cxt, lats);
        String[] decryptedLons = CryptoUtils.decryptBatch(cxt, lons);
        String[] decryptedAddresses = CryptoUtils.decryptBatch(cxt, addresses);

        Set<String> seenAddresses = new HashSet<>();

        List<GpsRecord> filtRecords = new LinkedList<>();
        if (decryptedAddresses!=null) {
            for (int i = 0; i < decryptedAddresses.length; i++) {
                if (!seenAddresses.contains(decryptedAddresses[i])) {
                    GpsRecord newRec = new GpsRecord();
                    newRec.setRawLat(decryptedLats[i]);
                    newRec.setRawLongi(decryptedLons[i]);
                    newRec.setRawAddress(decryptedAddresses[i]);
                    newRec.setTs(ts.get(i));
                    filtRecords.add(newRec);
                    seenAddresses.add(decryptedAddresses[i]);
                }
            }

            Collections.reverse(filtRecords);
            this.records = filtRecords;

            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return this.records.size();
    }

    public class GpsHistoryHolder extends RecyclerView.ViewHolder {
        TextView messageView;
        Chip date;

        GpsHistoryHolder(@NonNull View itemView) {
            super(itemView);
            this.date = itemView.findViewById(R.id.date);
            this.messageView = itemView.findViewById(R.id.messageView);
        }
    }
}