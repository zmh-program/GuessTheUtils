package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class BuilderNotification extends GTBEvents.Module {
    public BuilderNotification(GTBEvents events) {
        super(events);
        NotificationUtil.platform = NotificationUtil.detectPlatform();
        events.subscribe(GTBEvents.UserBuilderEvent.class, this::onUserBuilder, this);
    }

    private void onUserBuilder(GTBEvents.UserBuilderEvent event) {
        if (!Minecraft.getMinecraft().inGameHasFocus && GuessTheUtilsConfig.enableBuilderNotification) {
            NotificationUtil.sendNotification("Guess the Build", "Come back! It is your turn to build!");
        }
    }

    @Override
    public GTBEvents.Module.ErrorAction getErrorAction() {
        return GTBEvents.Module.ErrorAction.RESTART;
    }

    public static class NotificationUtil {
        public enum Platform {
            LINUX,
            WINDOWS,
            UNSUPPORTED
        }

        static Platform platform;

        private static Platform detectPlatform() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("nix") || os.contains("nux")) {
                return Platform.LINUX;
            } else if (os.contains("win")) {
                    return Platform.WINDOWS;
            } else {
                return Platform.UNSUPPORTED;
            }
        }

        public static void sendNotification(String title, String message) {
            switch (platform) {
                case WINDOWS:
                    if (SystemTray.isSupported()) {
                        sendWindowsNotification(title, message);
                    } else {
                        sendWindowsPowerShellNotification(title, message);
                    }
                    break;
                case LINUX:
                    sendLinuxNotification(title, message);
                    break;
                case UNSUPPORTED:
                default:
                    System.out.println("Unsupported OS for notifications.");
                    break;
            }
        }

        private static void sendWindowsNotification(String title, String message) {
            if (!SystemTray.isSupported()) {
                System.out.println("System tray is not supported!");
                return;
            }

            SystemTray tray = SystemTray.getSystemTray();
            TrayIcon trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(""), "Guess The Utils Toast");

            try {
                tray.add(trayIcon);
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                tray.remove(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }

        private static String escapeForShell(String input) {
            if (input == null) {
                return null;
            }
            return input.replace("'", "'\\''");
        }

        public static void sendWindowsPowerShellNotification(String title, String message) {
            try {
                GuessTheUtils.LOGGER.info("[GuessTheUtils] Sending Powershell toast.");
                final String COMMAND_TEMPLATE = "powershell.exe -ExecutionPolicy Bypass -Command \""
                        + "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] > $null;"
                        + "$template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02);"
                        + "$xml = New-Object Windows.Data.Xml.Dom.XmlDocument;"
                        + "$xml.LoadXml($template.GetXml());"
                        + "$texts = $xml.GetElementsByTagName('text');"
                        + "$texts.Item(0).AppendChild($xml.CreateTextNode('%s')) > $null;"
                        + "$texts.Item(1).AppendChild($xml.CreateTextNode('%s')) > $null;"
                        + "$toast = [Windows.UI.Notifications.ToastNotification]::new($xml);"
                        + "$notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('%s');"
                        + "$notifier.Show($toast);\"";
                String command = String.format(COMMAND_TEMPLATE, title, message.replace("\n", "'+\\\"`r`n\\\"+'"), "Guess The Utils");
                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
                builder.redirectErrorStream(true);
                Process process = builder.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        GuessTheUtils.LOGGER.info(line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static void sendLinuxNotification(String title, String message) {
            try {
                GuessTheUtils.LOGGER.info("[GuessTheUtils] Sending Linux toast.");
                String escapedTitle = escapeForShell(title);
                String escapedMessage = escapeForShell(message);

                String command = String.format("notify-send '%s' '%s'", escapedTitle, escapedMessage);
                Process process = Runtime.getRuntime().exec(new String[] { "bash", "-c", command });

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Output: " + line);
                }
                while ((line = errorReader.readLine()) != null) {
                    System.err.println("Error: " + line);
                }

                process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}