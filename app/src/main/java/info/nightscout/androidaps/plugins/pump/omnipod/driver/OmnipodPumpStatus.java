package info.nightscout.androidaps.plugins.pump.omnipod.driver;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.interfaces.PumpDescription;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus;
import info.nightscout.androidaps.plugins.pump.common.data.TempBasalPair;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkConst;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.defs.RileyLinkError;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.defs.RileyLinkServiceState;
import info.nightscout.androidaps.plugins.pump.omnipod.defs.PodDeviceState;
import info.nightscout.androidaps.plugins.pump.omnipod.defs.state.PodSessionState;
import info.nightscout.androidaps.plugins.pump.omnipod.util.OmnipodConst;
import info.nightscout.androidaps.plugins.pump.omnipod.util.OmnipodUtil;
import info.nightscout.androidaps.utils.SP;

/**
 * Created by andy on 4.8.2019
 */
public class OmnipodPumpStatus extends PumpStatus {

    private static Logger LOG = LoggerFactory.getLogger(L.PUMP);

    public String errorDescription = null;
    public String rileyLinkAddress = null;
    public boolean inPreInit = true;

    // statuses
    public RileyLinkServiceState rileyLinkServiceState = RileyLinkServiceState.NotStarted;
    public RileyLinkError rileyLinkError;
    public double currentBasal = 0;
    public long tempBasalStart;
    public long tempBasalEnd;
    public Double tempBasalAmount = 0.0d;
    public Integer tempBasalLength;
    public long tempBasalPumpId;
    public PodSessionState podSessionState;

    private boolean rileyLinkAddressChanged = false;
    private String regexMac = "([\\da-fA-F]{1,2}(?:\\:|$)){6}";


    public String podNumber;
    public PodDeviceState podDeviceState = PodDeviceState.NeverContacted;
    public boolean podAvailable = false;
    public boolean podAvailibityChecked = false;
    public boolean ackAlertsAvailable = false;
    public String ackAlertsText = null;

    public boolean beepBolusEnabled = true;
    public boolean beepBasalEnabled = true;
    public boolean beepSMBEnabled = true;
    public boolean beepTBREnabled = true;
    public boolean podDebuggingOptionsEnabled = false;
    public String podLotNumber = "???";
    public boolean timeChangeEventEnabled = true;

    public OmnipodDriverState driverState = OmnipodDriverState.NotInitalized;

    public OmnipodPumpStatus(PumpDescription pumpDescription) {
        super(pumpDescription);
    }


    @Override
    public void initSettings() {
        this.activeProfileName = "";
        this.reservoirRemainingUnits = 75d;
        this.batteryRemaining = 75;
        this.lastConnection = SP.getLong(OmnipodConst.Statistics.LastGoodPumpCommunicationTime, 0L);
        this.lastDataTime = new LocalDateTime(this.lastConnection);
        this.pumpType = PumpType.Insulet_Omnipod;
        this.podAvailable = false;
    }


    public boolean verifyConfiguration() {
        try {

            this.errorDescription = "-";

            String rileyLinkAddress = SP.getString(RileyLinkConst.Prefs.RileyLinkAddress, null);

            if (rileyLinkAddress == null) {
                if (isLogEnabled())
                    LOG.debug("RileyLink address invalid: null");
                this.errorDescription = MainApp.gs(R.string.medtronic_error_rileylink_address_invalid);
                return false;
            } else {
                if (!rileyLinkAddress.matches(regexMac)) {
                    this.errorDescription = MainApp.gs(R.string.medtronic_error_rileylink_address_invalid);
                    if (isLogEnabled())
                        LOG.debug("RileyLink address invalid: {}", rileyLinkAddress);
                } else {
                    if (!rileyLinkAddress.equals(this.rileyLinkAddress)) {
                        this.rileyLinkAddress = rileyLinkAddress;
                        rileyLinkAddressChanged = true;
                    }
                }
            }

            this.beepBasalEnabled = SP.getBoolean(OmnipodConst.Prefs.BeepBasalEnabled, true);
            this.beepBolusEnabled = SP.getBoolean(OmnipodConst.Prefs.BeepBolusEnabled, true);
            this.beepSMBEnabled = SP.getBoolean(OmnipodConst.Prefs.BeepSMBEnabled, true);
            this.beepTBREnabled = SP.getBoolean(OmnipodConst.Prefs.BeepTBREnabled, true);
            this.podDebuggingOptionsEnabled = SP.getBoolean(OmnipodConst.Prefs.PodDebuggingOptionsEnabled, false);
            this.timeChangeEventEnabled = SP.getBoolean(OmnipodConst.Prefs.TimeChangeEventEnabled, true);

            LOG.debug("Beeps [basal={}, bolus={}, SMB={}, TBR={}]", this.beepBasalEnabled, this.beepBolusEnabled, this.beepSMBEnabled, this.beepTBREnabled);

            reconfigureService();

            return true;

        } catch (Exception ex) {
            this.errorDescription = ex.getMessage();
            LOG.error("Error on Verification: " + ex.getMessage(), ex);
            return false;
        }
    }


    private boolean reconfigureService() {

        if (!inPreInit && OmnipodUtil.getOmnipodService() != null) {

            if (rileyLinkAddressChanged) {
                OmnipodUtil.sendBroadcastMessage(RileyLinkConst.Intents.RileyLinkNewAddressSet);
                rileyLinkAddressChanged = false;
            }
        }

        return (!rileyLinkAddressChanged);
    }


    public String getErrorInfo() {
        verifyConfiguration();

        return (this.errorDescription == null) ? "-" : this.errorDescription;
    }


    @Override
    public void refreshConfiguration() {
        verifyConfiguration();
    }


    public boolean setNotInPreInit() {
        this.inPreInit = false;

        return reconfigureService();
    }


    public void clearTemporaryBasal() {
        this.tempBasalStart = 0L;
        this.tempBasalEnd = 0L;
        this.tempBasalAmount = 0.0d;
        this.tempBasalLength = 0;
    }


    private boolean isLogEnabled() {
        return L.isEnabled(L.PUMP);
    }

    public TempBasalPair getTemporaryBasal() {

        TempBasalPair tbr = new TempBasalPair();
        tbr.setDurationMinutes(tempBasalLength);
        tbr.setInsulinRate(tempBasalAmount);
        tbr.setStartTime(tempBasalStart);
        tbr.setEndTime(tempBasalEnd);

        return tbr;
    }

    @Override
    public String toString() {
        return "OmnipodPumpStatus{" +
                "errorDescription='" + errorDescription + '\'' +
                ", rileyLinkAddress='" + rileyLinkAddress + '\'' +
                ", inPreInit=" + inPreInit +
                ", rileyLinkServiceState=" + rileyLinkServiceState +
                ", rileyLinkError=" + rileyLinkError +
                ", currentBasal=" + currentBasal +
                ", tempBasalStart=" + tempBasalStart +
                ", tempBasalEnd=" + tempBasalEnd +
                ", tempBasalAmount=" + tempBasalAmount +
                ", tempBasalLength=" + tempBasalLength +
                ", podSessionState=" + podSessionState +
                ", rileyLinkAddressChanged=" + rileyLinkAddressChanged +
                ", regexMac='" + regexMac + '\'' +
                ", podNumber='" + podNumber + '\'' +
                ", podDeviceState=" + podDeviceState +
                ", podAvailable=" + podAvailable +
                ", ackAlertsAvailable=" + ackAlertsAvailable +
                ", ackAlertsText='" + ackAlertsText + '\'' +
                ", lastDataTime=" + lastDataTime +
                ", lastConnection=" + lastConnection +
                ", previousConnection=" + previousConnection +
                ", lastBolusTime=" + lastBolusTime +
                ", lastBolusAmount=" + lastBolusAmount +
                ", activeProfileName='" + activeProfileName + '\'' +
                ", reservoirRemainingUnits=" + reservoirRemainingUnits +
                ", reservoirFullUnits=" + reservoirFullUnits +
                ", batteryRemaining=" + batteryRemaining +
                ", batteryVoltage=" + batteryVoltage +
                ", iob='" + iob + '\'' +
                ", dailyTotalUnits=" + dailyTotalUnits +
                ", maxDailyTotalUnits='" + maxDailyTotalUnits + '\'' +
                ", validBasalRateProfileSelectedOnPump=" + validBasalRateProfileSelectedOnPump +
                ", pumpType=" + pumpType +
                ", profileStore=" + profileStore +
                ", units='" + units + '\'' +
                ", pumpStatusType=" + pumpStatusType +
                ", basalsByHour=" + Arrays.toString(basalsByHour) +
                ", currentBasal=" + currentBasal +
                ", tempBasalInProgress=" + tempBasalInProgress +
                ", tempBasalRatio=" + tempBasalRatio +
                ", tempBasalRemainMin=" + tempBasalRemainMin +
                ", tempBasalStart=" + tempBasalStart +
                ", pumpDescription=" + pumpDescription +
                "} ";
    }
}
