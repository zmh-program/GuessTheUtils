package com.aembr.guesstheutils.modules;

import com.aembr.guesstheutils.GTBEvents;
import net.minecraft.client.MinecraftClient;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BuilderNotification extends GTBEvents.Module {
    public BuilderNotification(GTBEvents events) {
        super(events);
        NotificationUtil.platform = NotificationUtil.detectPlatform();
        events.subscribe(GTBEvents.UserBuilderEvent.class, this::onUserBuilder, this);
    }

    private void onUserBuilder(GTBEvents.UserBuilderEvent event) {
        if (!MinecraftClient.getInstance().isWindowFocused()) {
            NotificationUtil.sendNotification("Guess the Build", "Come back! It's your turn to build!");
        }
    }

    @Override
    public ErrorAction getErrorAction() {
        return ErrorAction.RESTART;
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
            TrayIcon trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(""), "GuessTheUtils");

            try {
                tray.add(trayIcon);
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                tray.remove(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }

        private static void sendWindowsPowerShellNotification(String title, String message) {
            try {
                String command = String.format("powershell -Command \"New-BurntToastNotification -Text '%s', '%s'\"", title, message);
                Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static void sendLinuxNotification(String title, String message) {
            try {
                String escapedTitle = title.replace("'", "'\\''");
                String escapedMessage = message.replace("'", "'\\''");
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
