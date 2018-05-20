package info.nightscout.androidaps.plugins.PumpCommon.hw.rileylink.service.tasks;

import com.gxwtech.roundtrip2.RoundtripService.Tasks.ServiceTask;

import info.nightscout.androidaps.plugins.PumpCommon.hw.rileylink.RileyLinkUtil;

/**
 * Created by geoff on 7/9/16.
 */
public class DiscoverGattServicesTask extends ServiceTask {

    public DiscoverGattServicesTask() {
    }

    @Override
    public void run() {
        RileyLinkUtil.getRileyLinkBLE().discoverServices();
    }
}