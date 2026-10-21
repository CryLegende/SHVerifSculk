package org.Cry_Legende.sHVerifSculk;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SHVerifSculk extends JavaPlugin implements Listener {

    // Map pour suivre les joueurs ayant besoin de confirmer le cassage
    private final HashMap<UUID, Long> confirmationMap = new HashMap<>();

    // Pattern pour la gestion des couleurs hexadécimales
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    @Override
    public void onEnable() {
        // Sauvegarde la configuration par défaut si elle n'existe pas
        saveDefaultConfig();

        // Enregistre cette classe comme un écouteur d'événements
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("SHVerifSculk a été activé !");
    }

    @Override
    public void onDisable() {
        getLogger().info("SHVerifSculk a été désactivé !");
    }

    /**
     * Gère l'événement de cassage de bloc pour le catalyseur de sculk.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // On ne traite que les catalyseurs de sculk
        if (event.getBlock().getType() != Material.SCULK_CATALYST) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Si le joueur a Silk Touch, on ne fait rien
        if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) {
            // On s'assure de retirer le joueur de la map au cas où il aurait changé d'outil
            confirmationMap.remove(playerUUID);
            return;
        }

        long currentTime = System.currentTimeMillis();
        // On récupère le délai depuis la config (en millisecondes)
        long confirmationDelay = getConfig().getLong("confirmation-delay", 10) * 1000;

        // Si le joueur n'est pas dans la map ou que le délai est dépassé
        if (!confirmationMap.containsKey(playerUUID) || (currentTime - confirmationMap.get(playerUUID)) > confirmationDelay) {
            // On annule l'événement
            event.setCancelled(true);
            // On ajoute le joueur à la map pour la prochaine vérification
            confirmationMap.put(playerUUID, currentTime);

            String message = getConfig().getString("verification-message");
            if (message != null && !message.isEmpty()) {
                player.sendMessage(translateColorCodes(message));
            }
        } else {
            // Le joueur a confirmé en cassant le bloc à nouveau dans le temps imparti
            // On le retire de la map pour que le prochain cassage demande à nouveau une confirmation
            confirmationMap.remove(playerUUID);
        }
    }

    /**
     * Traduit les codes couleurs standards (&) et hexadécimaux (&#RRGGBB) dans une chaîne de caractères.
     *
     * @param message Le message à traduire.
     * @return Le message avec les couleurs formatées.
     */
    private static String translateColorCodes(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }

        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }
}
