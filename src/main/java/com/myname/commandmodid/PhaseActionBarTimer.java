package com.myname.commandmodid;

import java.util.Timer;
import java.util.TimerTask;

import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class PhaseActionBarTimer {

    private static Timer timer;

    /**
     * Запуск таймера только на сервере!
     */
    public static void start() {
        // Останавливаем предыдущий таймер, если был
        stop();

        int totalSeconds;
        String phaseName;

        if (FlagPointCommand.preparationPhase) {
            totalSeconds = FlagPointCommand.preparationTimeMinutes * 60 + FlagPointCommand.preparationTimeSeconds;
            phaseName = "Подготовка";
        } else if (FlagPointCommand.isFlagPointSet()) {
            totalSeconds = FlagPointCommand.flagHoldTimeMinutes * 60 + FlagPointCommand.flagHoldTimeSeconds;
            phaseName = "Захват флага";
        } else {
            return;
        }

        // Запускаем только на сервере!
        if (FMLCommonHandler.instance()
                .getEffectiveSide() != Side.SERVER)
            return;

        final String phaseNameFinal = phaseName;
        final int[] secondsLeft = { totalSeconds };

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                if (secondsLeft[0] < 0) {
                    stop();
                    if (FlagPointCommand.preparationPhase) {
                        FlagPointCommand.preparationPhase = false;
                        CommandMod.network.sendToAll(
                                new PacketAnnouncement(0xFFFFFF + "Подготовка окончена"));
                    } else {
                        FlagVictoryHandler.checkVictory();
                    }
                    // Отправляем пустой таймер для очистки у всех клиентов
                    if (!MinecraftServer.getServer()
                            .getConfigurationManager().playerEntityList.isEmpty()) {
                        CommandMod.network.sendToAll(new PacketTimerText(""));
                    }
                    return;
                }

                // Отправляем обновление таймера всем клиентам
                if (!MinecraftServer.getServer()
                        .getConfigurationManager().playerEntityList.isEmpty()) {
                    String text = phaseNameFinal + ": " + formatTime(secondsLeft[0]);
                    CommandMod.network.sendToAll(new PacketTimerText(text));
                }
                secondsLeft[0]--;
            }
        }, 0, 1000);
    }

    /**
     * Остановка таймера (без очистки overlay на клиенте — это делает сервер через
     * пакет)
     */
    public static void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        // Очищаем текст таймера на клиенте
    }

    private static String formatTime(int totalSeconds) {
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format("%02d:%02d", min, sec);
    }
}
