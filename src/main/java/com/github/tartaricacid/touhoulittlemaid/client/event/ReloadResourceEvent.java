package com.github.tartaricacid.touhoulittlemaid.client.event;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.animation.inner.InnerAnimation;
import com.github.tartaricacid.touhoulittlemaid.client.resource.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.client.resource.GeckoModelLoader;
import com.github.tartaricacid.touhoulittlemaid.client.resource.models.PlayerMaidModels;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;


@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ReloadResourceEvent extends SimplePreparableReloadListener<Void> {
    @SubscribeEvent
    public static void onRegister(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ReloadResourceEvent());
    }

    public static void reloadAllPack() {
        StopWatch watch = StopWatch.createStarted();
        {
            GeckoModelLoader.reload();
            InnerAnimation.init();
            CustomPackLoader.reloadPacks();
            PlayerMaidModels.reload();
        }
        watch.stop();
        TouhouLittleMaid.LOGGER.info("Model loading time: {} ms", watch.getTime(TimeUnit.MICROSECONDS) / 1000.0);
    }

    @Override
    protected Void prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        reloadAllPack();
        return null;
    }

    @Override
    protected void apply(Void pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
    }
}
