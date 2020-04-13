package info.nightscout.androidaps.plugins.pump.omnipod_dash;

import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import info.nightscout.androidaps.BuildConfig;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.interfaces.PluginDescription;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.bus.RxBus;
import info.nightscout.androidaps.plugins.general.actions.defs.CustomAction;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpDriverState;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType;
import info.nightscout.androidaps.plugins.pump.omnipod.OmnipodFragment;
import info.nightscout.androidaps.plugins.pump.omnipod.OmnipodPumpPlugin;
import info.nightscout.androidaps.plugins.pump.omnipod.driver.ui.OmnipodUIComm;
import info.nightscout.androidaps.plugins.pump.omnipod.defs.OmnipodPodType;
import info.nightscout.androidaps.plugins.pump.omnipod.defs.OmnipodPumpPluginInterface;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodPumpValuesChanged;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodRefreshButtonState;
import info.nightscout.androidaps.plugins.pump.omnipod.service.RileyLinkOmnipodService;
import info.nightscout.androidaps.plugins.pump.omnipod.util.OmnipodUtil;
import info.nightscout.androidaps.plugins.pump.omnipod_dash.comm.OmnipodDashCommunicationManager;
import info.nightscout.androidaps.utils.TimeChangeType;

/**
 * Created by andy on 23.04.18.
 *
 * @author Andy Rozman (andy.rozman@gmail.com)
 */
public class OmnipodDashPumpPlugin extends OmnipodPumpPlugin implements OmnipodPumpPluginInterface {

    private static final Logger LOG = LoggerFactory.getLogger(L.PUMP);

    protected static OmnipodDashPumpPlugin plugin = null;
    //private RileyLinkOmnipodService omnipodService;
    //private OmnipodPumpStatus pumpStatusLocal = null;

    // variables for handling statuses and history
    private boolean firstRun = true;
    private boolean isRefresh = false;
    private boolean isBasalProfileInvalid = false;
    private boolean basalProfileChanged = false;
    private boolean isInitialized = false;

    public static boolean isBusy = false;
    //private List<Long> busyTimestamps = new ArrayList<>();
    //private boolean sentIdToFirebase = false;
    //private boolean hasTimeDateOrTimeZoneChanged = false;

    private Profile currentProfile;


    private OmnipodDashPumpPlugin() {
        super(new PluginDescription() //
                        .mainType(PluginType.PUMP) //
                        .fragmentClass(OmnipodFragment.class.getName()) //
                        .pluginName(R.string.omnipod_dash_name) //
                        .shortName(R.string.omnipod_dash_name_short) //
                        .preferencesId(R.xml.pref_omnipod) //
                        .description(R.string.description_pump_omnipod_dash), //
                PumpType.Insulet_Omnipod_Dash
        );

        displayConnectionMessages = false;

        //OmnipodUtil.setOmnipodPodType(OmnipodPodType.Dash);

        if (omnipodCommunicationManager == null) {
            omnipodCommunicationManager = OmnipodDashCommunicationManager.getInstance();
        }

        omnipodUIComm = new OmnipodUIComm(omnipodCommunicationManager, this, this.pumpStatusLocal);

        OmnipodUtil.setPlugin(this);

        // FIXME
//        serviceConnection = new ServiceConnection() {
//
//            public void onServiceDisconnected(ComponentName name) {
//                if (isLoggingEnabled())
//                    LOG.debug("RileyLinkOmnipodService is disconnected");
//                omnipodService = null;
//            }
//
//            public void onServiceConnected(ComponentName name, IBinder service) {
//                if (isLoggingEnabled())
//                    LOG.debug("RileyLinkOmnipodService is connected");
//                RileyLinkOmnipodService.LocalBinder mLocalBinder = (RileyLinkOmnipodService.LocalBinder) service;
//                omnipodService = mLocalBinder.getServiceInstance();
//
//                new Thread(() -> {
//
//                    for (int i = 0; i < 20; i++) {
//                        SystemClock.sleep(5000);
//
//                        if (OmnipodUtil.getPumpStatus() != null) {
//                            if (isLoggingEnabled())
//                                LOG.debug("Starting OmniPod-RileyLink service");
//                            if (OmnipodUtil.getPumpStatus().setNotInPreInit()) {
//                                break;
//                            }
//                        }
//                    }
//                }).start();
//            }
//        };
    }


    public static OmnipodDashPumpPlugin getPlugin() {
        if (plugin == null)
            plugin = new OmnipodDashPumpPlugin();
        return plugin;
    }


    private String getLogPrefix() {
        return "OmnipodPlugin::";
    }


    // FIXME
    public Class getServiceClass() {
        return RileyLinkOmnipodService.class;
    }


    @Override
    public String deviceID() {
        return "Omnipod Dash";
    }


    // Pump Plugin

    private boolean isServiceSet() {
        return true; //omnipodService != null;
    }

    private boolean isServiceInitialized() {
        return true;
    }


    @Override
    public boolean isInitialized() {
        if (isLoggingEnabled() && displayConnectionMessages)
            LOG.debug(getLogPrefix() + "isInitialized");
        return isServiceSet() && isInitialized;
    }


    @Override
    public boolean isConnected() {
        if (isLoggingEnabled() && displayConnectionMessages)
            LOG.debug(getLogPrefix() + "isConnected");
        return isServiceSet() && isServiceInitialized();
    }


    @Override
    public boolean isConnecting() {
        if (isLoggingEnabled() && displayConnectionMessages)
            LOG.debug(getLogPrefix() + "isConnecting");
        return !isServiceSet() || !isServiceInitialized();
    }


    @Override
    public void getPumpStatus() {

        if (firstRun) {
            initializePump(!isRefresh);
        }

//        getPodPumpStatus();
//
//        if (firstRun) {
//            initializePump(!isRefresh);
//        } else {
//            refreshAnyStatusThatNeedsToBeRefreshed();
//        }
//
//        MainApp.bus().post(new EventMedtronicPumpValuesChanged());
    }


    public void resetStatusState() {
        firstRun = true;
        isRefresh = true;
    }


    private void setRefreshButtonEnabled(boolean enabled) {
        RxBus.INSTANCE.send(new EventOmnipodRefreshButtonState(enabled));
    }


    private void initializePump(boolean realInit) {

        if (isLoggingEnabled())
            LOG.info(getLogPrefix() + "initializePump - start");

        if (omnipodCommunicationManager == null) {
            omnipodCommunicationManager = OmnipodDashCommunicationManager.getInstance();
        }

//        setRefreshButtonEnabled(false);
//
//        getPodPumpStatus();
//
//        if (isRefresh) {
//            if (isPumpNotReachable()) {
//                if (isLoggingEnabled())
//                    LOG.error(getLogPrefix() + "initializePump::Pump unreachable.");
//                MedtronicUtil.sendNotification(MedtronicNotificationType.PumpUnreachable);
//
//                setRefreshButtonEnabled(true);
//
//                return;
//            }
//
//            MedtronicUtil.dismissNotification(MedtronicNotificationType.PumpUnreachable);
//        }
//
//        this.pumpState = PumpDriverState.Connected;
//
//        pumpStatusLocal.setLastCommunicationToNow();
//        setRefreshButtonEnabled(true);

        // TODO need to read status and BasalProfile if pod inited and pod status and set correct commands enabled

        if (!isRefresh) {
            pumpState = PumpDriverState.Initialized;
        }

        if (!sentIdToFirebase) {
            Bundle params = new Bundle();
            params.putString("version", BuildConfig.VERSION);
            MainApp.getFirebaseAnalytics().logEvent("OmnipodPumpInit", params);

            sentIdToFirebase = true;
        }

        isInitialized = true;
        // this.pumpState = PumpDriverState.Initialized;

        this.firstRun = false;
    }


    protected void triggerUIChange() {
        RxBus.INSTANCE.send(new EventOmnipodPumpValuesChanged());
    }


    // OPERATIONS not supported by Pump or Plugin

    //private List<CustomAction> customActions = null;


    @Override
    public List<CustomAction> getCustomActions() {

        if (customActions == null) {
            this.customActions = Arrays.asList(
//                    customActionPairAndPrime, //
//                    customActionFillCanullaSetBasalProfile, //
//                    customActionDeactivatePod, //
//                    customActionResetPod
            );
        }

        return this.customActions;
    }


    @Override
    public void timezoneOrDSTChanged(TimeChangeType timeChangeType) {

//        if (isLoggingEnabled())
//            LOG.warn(getLogPrefix() + "Time, Date and/or TimeZone changed. ");
//
//        this.hasTimeDateOrTimeZoneChanged = true;
    }


}
